package com.chandni.kavos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Listens for incoming SMS. If the sender's number matches one of the saved guardians
 * AND there's a recent SOS alert that hasn't been acknowledged yet, mark it as
 * acknowledged and post a high-priority notification. The reply is classified by
 * Gemini (when available) so the notification can say "X is on the way" vs
 * "X got your alert" vs "X can't help" — falls back to UNCLEAR if AI is offline.
 */
class SmsReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: return
        val body = messages.joinToString(separator = "") { it.displayMessageBody ?: "" }

        Thread {
            val db = AppDatabase.getDatabase(context)
            val uid = UserScope.uid()
            val guardian = findMatchingGuardian(db.contactDao().getAllContactsForUser(uid), sender)
                ?: return@Thread

            val since = System.currentTimeMillis() - 60 * 60 * 1000L
            val pending = db.sosLogDao().getLatestPendingForUser(uid, since) ?: return@Thread

            val cleaned = body.trim()
            val cls = classifyWithTimeout(context, cleaned)
            val now = System.currentTimeMillis()
            db.sosLogDao().acknowledge(pending.id, guardian.name, now, cleaned, cls?.name)

            postAckNotification(context, guardian.name, cleaned, cls)
        }.start()
    }

    private fun classifyWithTimeout(context: Context, replyText: String): ReplyClass? {
        val key = GeminiClient.apiKey(context) ?: return null
        if (!GeminiClient.isOnline(context)) return null
        val executor = Executors.newSingleThreadExecutor()
        return try {
            executor.submit<ReplyClass?> { GeminiClient.classifyReply(key, replyText) }
                .get(4, TimeUnit.SECONDS)
        } catch (_: Exception) {
            null
        } finally {
            executor.shutdownNow()
        }
    }

    private fun findMatchingGuardian(contacts: List<Contact>, sender: String): Contact? {
        val incomingDigits = lastTenDigits(sender) ?: return null
        return contacts.firstOrNull { lastTenDigits(it.phoneNumber) == incomingDigits }
    }

    /** Strip non-digits and take the last 10 — robust to "+91", spaces, "0" prefix, etc. */
    private fun lastTenDigits(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else null
    }

    private fun postAckNotification(
        context: Context,
        guardianName: String,
        replyText: String,
        cls: ReplyClass?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Guardian acknowledgements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Plays when a guardian replies to your SOS"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val openIntent = Intent(context, SOSHistoryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (cls) {
            ReplyClass.COMING_NOW -> "$guardianName is on the way"
            ReplyClass.ACKNOWLEDGED -> "$guardianName got your alert"
            ReplyClass.CANT_HELP -> "$guardianName can't help right now"
            ReplyClass.UNCLEAR, null -> "$guardianName replied"
        }

        val display = if (replyText.length > 80) replyText.take(80) + "…" else replyText

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Reply: $display")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Reply: $display"))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "guardian_ack_channel"
        private const val NOTIF_ID_BASE = 5000
    }
}
