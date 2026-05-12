package com.chandni.kavos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object SOSHelper {

    fun sendSOS(context: Context, triggerMethod: String, spokenMessage: String? = null) {
        val db = AppDatabase.getDatabase(context)
        val uid = UserScope.uid()
        Thread {
            val contacts = db.contactDao().getAllContactsForUser(uid)
            if (contacts.isEmpty()) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, context.getString(R.string.no_guardians), Toast.LENGTH_SHORT).show()
                }
                return@Thread
            }

            val locationStr = getLiveLocation(context)
            val mapsLink = "https://www.google.com/maps?q=$locationStr"

            // Best-effort smart opener — bounded to ~3s. SOS still fires if AI is
            // slow / offline / unkeyed; we just use the static text instead.
            val smartOpener = tryComposeSmartOpener(context, triggerMethod, spokenMessage)

            try {
                val smsManager = context.getSystemService(SmsManager::class.java)

                for (contact in contacts) {
                    val opener = smartOpener ?: when {
                        !spokenMessage.isNullOrBlank() ->
                            "KAVOS ALERT (Voice): \"${spokenMessage.trim()}\""
                        contact.customMessage.isNotEmpty() -> contact.customMessage
                        else -> "KAVOS ALERT: I need help!"
                    }

                    val voiceLine =
                        if (smartOpener != null && !spokenMessage.isNullOrBlank())
                            "\nVoice: \"${spokenMessage.trim()}\""
                        else ""

                    val message = "$opener$voiceLine\nLocation: $mapsLink\n\nReply OK to confirm you're on the way."

                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                }

                db.sosLogDao().insertLog(
                    SOSLog(triggerMethod = triggerMethod, location = locationStr, userId = uid)
                )

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "SOS sent to ${contacts.size} guardians!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "SMS Failed: Check balance/permissions.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun tryComposeSmartOpener(
        context: Context,
        triggerMethod: String,
        spokenPhrase: String?
    ): String? {
        val key = GeminiClient.apiKey(context) ?: return null
        if (!GeminiClient.isOnline(context)) return null

        val firstName = context.getSharedPreferences("KAVOS_PREFS", Context.MODE_PRIVATE)
            .getString("user_name", null)?.split(" ")?.firstOrNull()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val battery = batteryPercent(context)

        val executor = Executors.newSingleThreadExecutor()
        return try {
            val future = executor.submit<String?> {
                GeminiClient.composeSosOpener(
                    apiKey = key,
                    firstName = firstName,
                    triggerMethod = triggerMethod,
                    hourOfDay = hour,
                    batteryPct = battery,
                    spokenPhrase = spokenPhrase
                )
            }
            future.get(3, TimeUnit.SECONDS)?.let { line ->
                if (line.startsWith("KAVOS", ignoreCase = true)) line
                else "KAVOS ALERT: $line"
            }
        } catch (_: Exception) {
            null
        } finally {
            executor.shutdownNow()
        }
    }

    private fun batteryPercent(context: Context): Int {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        } catch (_: Exception) { 100 }
    }

    private fun getLiveLocation(context: Context): String {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if (loc != null) "${loc.latitude},${loc.longitude}" else "12.9152,80.0000"
            } else {
                "12.9152,80.0000"
            }
        } catch (e: Exception) {
            "12.9152,80.0000"
        }
    }
}
