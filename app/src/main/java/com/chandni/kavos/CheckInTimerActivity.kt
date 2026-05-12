package com.chandni.kavos

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CheckInTimerActivity : AppCompatActivity() {

    private var timer: CountDownTimer? = null
    private var timeLeftMs: Long = 5 * 60 * 1000L
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkin_timer)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Check-in Timer"

        val tvTimer = findViewById<TextView>(R.id.tvTimer)
        val btnSafe = findViewById<Button>(R.id.btnImSafe)
        val btnAdd = findViewById<Button>(R.id.btnAddTime)
        val btnStart = findViewById<Button>(R.id.btnStartTimer)

        updateDisplay(tvTimer, timeLeftMs)

        btnStart.setOnClickListener {
            if (!isRunning) startTimer(tvTimer, btnStart)
            else stopTimer(tvTimer, btnStart)
        }

        btnSafe.setOnClickListener {
            stopTimer(tvTimer, btnStart)
            Toast.makeText(this, "Great! You are safe. Timer cancelled.", Toast.LENGTH_SHORT).show()
        }

        btnAdd.setOnClickListener {
            timeLeftMs += 5 * 60 * 1000L
            if (isRunning) { timer?.cancel(); startTimer(tvTimer, btnStart) }
            else updateDisplay(tvTimer, timeLeftMs)
            Toast.makeText(this, "+5 minutes added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer(tv: TextView, btn: Button) {
        isRunning = true
        btn.text = "Cancel"
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.red_danger))
        timer = object : CountDownTimer(timeLeftMs, 1000) {
            override fun onTick(ms: Long) { timeLeftMs = ms; updateDisplay(tv, ms) }
            override fun onFinish() {
                isRunning = false
                tv.text = "00:00"
                btn.text = "Start Timer"
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.purple_light))
                Toast.makeText(this@CheckInTimerActivity, "Time up! Sending SOS…", Toast.LENGTH_LONG).show()
                SOSHelper.sendSOS(this@CheckInTimerActivity, "Check-in Timer")
            }
        }.start()
    }

    private fun stopTimer(tv: TextView, btn: Button) {
        timer?.cancel()
        isRunning = false
        timeLeftMs = 5 * 60 * 1000L
        updateDisplay(tv, timeLeftMs)
        btn.text = "Start Timer"
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.purple_light))
    }

    private fun updateDisplay(tv: TextView, ms: Long) {
        val m = (ms / 1000) / 60
        val s = (ms / 1000) % 60
        tv.text = String.format("%02d:%02d", m, s)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
    override fun onDestroy() { super.onDestroy(); timer?.cancel() }
}