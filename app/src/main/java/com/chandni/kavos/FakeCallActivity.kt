package com.chandni.kavos

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FakeCallActivity : AppCompatActivity() {

    // IMPORTANT: ringtone must be a class property — NOT a local variable.
    // If it's local, it gets garbage-collected and silently stops playing.
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call)

        // Caller name from prefs — fall back to "Amma" as a warm default
        val prefs = getSharedPreferences("KAVOS_PREFS", Context.MODE_PRIVATE)
        val callerName = prefs.getString("fake_caller_name", null)
            ?.takeIf { it.isNotBlank() }
            ?: prefs.getString("user_name", null)
                ?.takeIf { it.isNotBlank() }
            ?: "Amma"

        findViewById<TextView>(R.id.tvCallerName).text = callerName
        findViewById<TextView>(R.id.tvCallerStatus).text = "Incoming Call…"

        // Max ring volume
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVol, 0)

        // Start ringtone
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
        } catch (e: Exception) {
            // Ringtone unavailable on some emulators — safe to ignore
        }

        // Decline — stop and close
        findViewById<ImageButton>(R.id.btnDecline).setOnClickListener {
            stopAndFinish()
        }

        // Accept — stop ring, show "Connected", animate "in call" state
        findViewById<ImageButton>(R.id.btnAccept).setOnClickListener {
            ringtone?.stop()
            val tvStatus = findViewById<TextView>(R.id.tvCallerStatus)
            tvStatus.text = "Connected"

            // Show call timer counting up — makes it feel real
            var seconds = 0
            val timerRunnable = object : Runnable {
                override fun run() {
                    seconds++
                    val mins = seconds / 60
                    val secs = seconds % 60
                    tvStatus.text = "%d:%02d".format(mins, secs)
                    handler.postDelayed(this, 1000)
                }
            }
            handler.postDelayed(timerRunnable, 1000)

            // Change decline button to "End Call"
            val btnDecline = findViewById<ImageButton>(R.id.btnDecline)
            btnDecline.setOnClickListener {
                handler.removeCallbacksAndMessages(null)
                stopAndFinish()
            }
        }
    }

    private fun stopAndFinish() {
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
        handler.removeCallbacksAndMessages(null)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndFinish()
    }
}