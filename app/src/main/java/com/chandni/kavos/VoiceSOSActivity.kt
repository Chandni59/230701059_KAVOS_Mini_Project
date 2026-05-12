package com.chandni.kavos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class VoiceSOSActivity : AppCompatActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val RECORD_AUDIO_REQUEST = 201
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "voice_sos_channel"

    // Exponential backoff for retries
    private var retryDelayMs = 1500L
    private val MAX_RETRY_DELAY = 5000L
    private val retryHandler = Handler(Looper.getMainLooper())
    private var retryScheduled = false

    // Pulse animation
    private val pulseHandler = Handler(Looper.getMainLooper())
    private var pulseVisible = true
    private lateinit var pulseDot: View
    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button

    private val pulseRunnable = object : Runnable {
        override fun run() {
            if (isListening) {
                pulseDot.visibility = if (pulseVisible) View.VISIBLE else View.INVISIBLE
                pulseVisible = !pulseVisible
                pulseHandler.postDelayed(this, 600)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_sos)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Voice SOS"

        tvStatus = findViewById(R.id.tvVoiceStatus)
        btnToggle = findViewById(R.id.btnToggleVoice)
        pulseDot = findViewById(R.id.pulseDot)
        pulseDot.visibility = View.INVISIBLE

        createNotificationChannel()

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvStatus.text = "Voice recognition not available on this device."
            btnToggle.isEnabled = false
            return
        }

        btnToggle.setOnClickListener {
            if (!isListening) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST
                    )
                } else {
                    startVoiceSession()
                }
            } else {
                stopVoiceSession()
            }
        }
    }

    private fun startVoiceSession() {
        isListening = true
        retryDelayMs = 1500L
        btnToggle.text = "Stop Listening"
        btnToggle.backgroundTintList =
            android.content.res.ColorStateList.valueOf(getColor(R.color.red_danger))
        pulseDot.visibility = View.VISIBLE
        pulseHandler.post(pulseRunnable)
        tvStatus.text = "Starting voice listener…"
        showListeningNotification()
        initRecognizerAndListen()
    }

    private fun stopVoiceSession() {
        isListening = false
        retryHandler.removeCallbacksAndMessages(null)
        pulseHandler.removeCallbacks(pulseRunnable)
        pulseDot.visibility = View.INVISIBLE
        pulseDot.scaleX = 1f
        pulseDot.scaleY = 1f
        speechRecognizer?.stopListening()
        destroyRecognizer()
        btnToggle.text = "Start Listening"
        btnToggle.backgroundTintList =
            android.content.res.ColorStateList.valueOf(getColor(R.color.purple_primary))
        tvStatus.text = "Voice SOS is off. Tap to activate."
        dismissNotification()
    }

    private fun initRecognizerAndListen() {
        destroyRecognizer()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(p: Bundle?) {
                retryScheduled = false
                retryDelayMs = 1500L
                tvStatus.text = "Listening… say \"help me\", \"bachao\", or \"madad\""
            }

            override fun onResults(bundle: Bundle?) {
                if (!isListening) return
                val results = bundle
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: return

                if (isTriggerPhrase(results)) {
                    fireTrigger(results.firstOrNull().orEmpty())
                } else {
                    tvStatus.text = "Heard: \"${results.firstOrNull() ?: ""}\"\nNot a trigger — still listening…"
                    scheduleRetry()
                }
            }

            override fun onPartialResults(p: Bundle?) {
                if (!isListening) return
                val partial = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                if (isTriggerPhrase(partial)) {
                    fireTrigger(partial.firstOrNull().orEmpty())
                }
            }

            override fun onError(errorCode: Int) {
                if (!isListening) return

                if (errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    tvStatus.text = "Recognizer busy — restarting in 3s…"
                    retryHandler.postDelayed({
                        if (isListening) {
                            destroyRecognizer()
                            initRecognizerAndListen()
                        }
                    }, 3000)
                    return
                }

                val msg = when (errorCode) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout — retrying"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error — retrying"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
                    else -> "Error $errorCode — retrying"
                }
                tvStatus.text = "$msg"
                scheduleRetry()
            }

            override fun onBeginningOfSpeech() {
                tvStatus.text = "Hearing you…"
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (!isListening) return
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                val scale = 1f + (normalized * 0.5f)
                pulseDot.scaleX = scale
                pulseDot.scaleY = scale
            }

            override fun onEndOfSpeech() {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })

        startListening()
    }

    private fun fireTrigger(spoken: String) {
        if (!isListening) return
        tvStatus.text = "Trigger heard! Sending SOS…"
        isListening = false
        stopVoiceSession()
        SOSHelper.sendSOS(this@VoiceSOSActivity, "Voice", spoken)
    }

    private fun isTriggerPhrase(results: List<String>): Boolean {
        // Broad emergency vocabulary — English + common Indian-language words (transliterated).
        // Any of these in any candidate result fires the trigger.
        val keywords = listOf(
            "help me", "save me", "help help",
            "bachao", "bachaao", "bachavo",
            "madad", "madat",
            "rakshinchu", "kapadu",
            "kavalee", "kaapaadu", "kaapathu",
            "emergency", "danger"
        )
        return results.any { result ->
            val lower = result.lowercase()
            keywords.any { lower.contains(it) } ||
                // standalone "help" said twice in quick recognition is also a trigger
                lower.split(Regex("\\s+")).count { it == "help" } >= 2
        }
    }

    private fun scheduleRetry() {
        if (!isListening || retryScheduled) return
        retryScheduled = true
        retryHandler.postDelayed({
            retryScheduled = false
            if (isListening) initRecognizerAndListen()
        }, retryDelayMs)
        retryDelayMs = minOf(retryDelayMs * 2, MAX_RETRY_DELAY)
    }

    private fun startListening() {
        val lang = Locale.getDefault().toLanguageTag()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            tvStatus.text = "Failed to start listener. Retrying…"
            scheduleRetry()
        }
    }

    private fun destroyRecognizer() {
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = null
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice SOS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active while Voice SOS is listening"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showListeningNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(this, VoiceSOSActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice SOS Active")
            .setContentText("Listening for \"help me\"… Tap to open")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPi)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun dismissNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
    }

    // ── Permissions / lifecycle ───────────────────────────────────────────────

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceSession()
        } else {
            tvStatus.text = "Microphone permission is required for Voice SOS."
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() {
        super.onDestroy()
        retryHandler.removeCallbacksAndMessages(null)
        pulseHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        dismissNotification()
    }
}
