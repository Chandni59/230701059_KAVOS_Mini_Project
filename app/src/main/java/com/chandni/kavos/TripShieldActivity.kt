package com.chandni.kavos

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class TripShieldActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            renderActiveBanner()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_shield)
        supportActionBar?.hide()

        // Gradient hero
        val hero = findViewById<View>(R.id.heroHeader)
        hero.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                ContextCompat.getColor(this, R.color.grad_purple_a),
                ContextCompat.getColor(this, R.color.grad_purple_b)
            )
        )

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Defaults: Walking + 30 min
        findViewById<MaterialButtonToggleGroup>(R.id.togType).check(R.id.btnTypeWalk)
        findViewById<MaterialButtonToggleGroup>(R.id.togDuration).check(R.id.btnDur30)

        findViewById<MaterialButton>(R.id.btnStartTrip).setOnClickListener { onStartTrip() }
        findViewById<MaterialButton>(R.id.btnImSafeBig).setOnClickListener { onMarkSafe() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    override fun onResume() {
        super.onResume()
        renderActiveBanner()
        if (TripShieldService.isActive) handler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickRunnable)
    }

    private fun renderActiveBanner() {
        val activeCard = findViewById<View>(R.id.cardActiveTrip)
        val setup = findViewById<View>(R.id.setupForm)

        if (TripShieldService.isActive) {
            activeCard.visibility = View.VISIBLE
            setup.visibility = View.GONE
            findViewById<TextView>(R.id.tvActiveLabel).text = TripShieldService.tripLabel
            val msLeft = (TripShieldService.tripEndsAt - System.currentTimeMillis())
                .coerceAtLeast(0L)
            val mins = msLeft / 60_000L
            val secs = (msLeft / 1000L) % 60
            findViewById<TextView>(R.id.tvActiveCountdown).text =
                String.format("Auto-SOS in %02d:%02d if you don't tap I'm Safe", mins, secs)
        } else {
            activeCard.visibility = View.GONE
            setup.visibility = View.VISIBLE
        }
    }

    private fun onStartTrip() {
        // Need at least one guardian to alert
        Thread {
            val count = AppDatabase.getDatabase(this).contactDao().getAllContactsForUser(UserScope.uid()).size
            runOnUiThread {
                if (count == 0) {
                    Toast.makeText(
                        this,
                        "Add at least one guardian first — Trip Shield alerts your guardians.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }
                launchTrip()
            }
        }.start()
    }

    private fun launchTrip() {
        val type = when (findViewById<MaterialButtonToggleGroup>(R.id.togType).checkedButtonId) {
            R.id.btnTypeCab -> "In a cab"
            R.id.btnTypeOther -> "Trip"
            else -> "Walking"
        }
        val duration = when (findViewById<MaterialButtonToggleGroup>(R.id.togDuration).checkedButtonId) {
            R.id.btnDur10 -> 10
            R.id.btnDur60 -> 60
            else -> 30
        }

        val intent = Intent(this, TripShieldService::class.java)
            .setAction(TripShieldService.ACTION_START)
            .putExtra(TripShieldService.EXTRA_LABEL, type)
            .putExtra(TripShieldService.EXTRA_DURATION_MIN, duration)
        startForegroundService(intent)

        Toast.makeText(this, "Trip Shield active · I'll guard you for $duration min", Toast.LENGTH_LONG)
            .show()
        renderActiveBanner()
        handler.post(tickRunnable)
    }

    private fun onMarkSafe() {
        val intent = Intent(this, TripShieldService::class.java)
            .setAction(TripShieldService.ACTION_STOP_SAFE)
        startService(intent)
        Toast.makeText(this, "Trip ended · guardians notified you're safe", Toast.LENGTH_SHORT).show()
        handler.removeCallbacks(tickRunnable)
        // Wait a beat for the service to flip its state, then re-render.
        handler.postDelayed({ renderActiveBanner() }, 300)
    }
}
