package com.chandni.kavos

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service that "guards" a trip end-to-end.
 *
 * While running it:
 *  - sends a starting SMS to every guardian with the user's live Maps link,
 *  - re-sends a fresh location every PING_INTERVAL_MS,
 *  - shows a sticky notification with an "I'M SAFE" action,
 *  - auto-fires a full SOS if the trip's max duration elapses without
 *    the user marking themselves safe.
 *
 * This is what makes Trip Shield different from plain Share Location:
 * the *absence* of an "I'm safe" tap is itself a signal.
 */
class TripShieldService : Service() {

    companion object {
        const val ACTION_START = "com.chandni.kavos.TRIP_START"
        const val ACTION_STOP_SAFE = "com.chandni.kavos.TRIP_STOP_SAFE"
        const val EXTRA_LABEL = "trip_label"
        const val EXTRA_DURATION_MIN = "trip_duration_min"

        private const val CHANNEL_ID = "trip_shield_channel"
        private const val NOTIF_ID = 4221
        private const val PING_INTERVAL_MS = 2 * 60 * 1000L      // 2 min
        private const val DEFAULT_DURATION_MIN = 30

        // Static state so the launcher activity can show "Trip in progress"
        @Volatile var isActive: Boolean = false; private set
        @Volatile var tripLabel: String = ""; private set
        @Volatile var tripStartedAt: Long = 0L; private set
        @Volatile var tripEndsAt: Long = 0L; private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private var pingRunnable: Runnable? = null
    private var endRunnable: Runnable? = null
    private var pingsSent = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SAFE -> { endTrip(safe = true); return START_NOT_STICKY }
        }

        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "Trip"
        val durationMin = intent?.getIntExtra(EXTRA_DURATION_MIN, DEFAULT_DURATION_MIN)
            ?: DEFAULT_DURATION_MIN

        tripLabel = label
        tripStartedAt = System.currentTimeMillis()
        tripEndsAt = tripStartedAt + durationMin * 60_000L
        isActive = true
        pingsSent = 0

        startForegroundCompat(buildNotification(durationMin))

        // Initial alert SMS so guardians know to expect updates.
        sendGuardianMessage(
            "🛡 KAVOS Trip Shield started — \"$label\". I'll auto-share my location " +
                "for the next $durationMin minutes. Live now: __MAPS__"
        )

        // Periodic re-share.
        pingRunnable = object : Runnable {
            override fun run() {
                if (!isActive) return
                pingsSent++
                sendGuardianMessage("🛡 KAVOS Trip update #$pingsSent — \"$label\": __MAPS__")
                refreshNotification(durationMin)
                handler.postDelayed(this, PING_INTERVAL_MS)
            }
        }
        handler.postDelayed(pingRunnable!!, PING_INTERVAL_MS)

        // Hard stop: if no "I'm Safe" by deadline, fire full SOS.
        endRunnable = Runnable {
            if (isActive) {
                SOSHelper.sendSOS(this, "Trip Shield · Missed Check-in")
                endTrip(safe = false)
            }
        }
        handler.postDelayed(endRunnable!!, durationMin * 60_000L)

        return START_STICKY
    }

    private fun endTrip(safe: Boolean) {
        isActive = false
        pingRunnable?.let { handler.removeCallbacks(it) }
        endRunnable?.let { handler.removeCallbacks(it) }

        if (safe) {
            sendGuardianMessage(
                "✅ KAVOS Trip Shield: \"$tripLabel\" — I'm safe. Final location: __MAPS__"
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isActive = false
        pingRunnable?.let { handler.removeCallbacks(it) }
        endRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun startForegroundCompat(notif: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun buildNotification(durationMin: Int): Notification {
        ensureChannel()

        val safeIntent = Intent(this, TripShieldService::class.java)
            .setAction(ACTION_STOP_SAFE)
        val safePending = PendingIntent.getService(
            this, 0, safeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, TripShieldActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openPending = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_security_shield)
            .setContentTitle("Trip Shield active · $tripLabel")
            .setContentText("Auto-checking in every 2 min · ends in $durationMin min")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(0, "I'M SAFE", safePending)
            .build()
    }

    private fun refreshNotification(durationMin: Int) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIF_ID, buildNotification(durationMin))
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Trip Shield",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live trip-tracking notifications"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun sendGuardianMessage(template: String) {
        Thread {
            try {
                val contacts = AppDatabase.getDatabase(this).contactDao().getAllContactsForUser(UserScope.uid())
                if (contacts.isEmpty()) return@Thread
                val loc = currentLocation()
                val mapsLink = "https://www.google.com/maps?q=$loc"
                val body = template.replace("__MAPS__", mapsLink)
                val sms = getSystemService(SmsManager::class.java)
                for (c in contacts) {
                    val parts = sms.divideMessage(body)
                    sms.sendMultipartTextMessage(c.phoneNumber, null, parts, null, null)
                }
            } catch (_: Exception) {
                // SMS failures are silent here — service keeps trying next interval.
            }
        }.start()
    }

    private fun currentLocation(): String {
        return try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) "${loc.latitude},${loc.longitude}"
                else "12.9152,80.0000"
            } else "12.9152,80.0000"
        } catch (_: Exception) {
            "12.9152,80.0000"
        }
    }

}
