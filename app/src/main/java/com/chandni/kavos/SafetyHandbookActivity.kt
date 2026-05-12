package com.chandni.kavos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class SafetyHandbookActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_handbook)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { goHome() }

        findViewById<View>(R.id.cardEmergencyCall).setOnClickListener { dial("112") }
        findViewById<MaterialButton>(R.id.btnDialWomen).setOnClickListener { dial("1091") }
        findViewById<MaterialButton>(R.id.btnDialAmb).setOnClickListener { dial("108") }

        val rv = findViewById<RecyclerView>(R.id.rvSafetyTips)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SafetyTipAdapter(SafetyTipsRepo.ALL) { tip -> openDetail(tip) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })
    }

    private fun dial(number: String) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun openDetail(tip: SafetyTip) {
        startActivity(
            Intent(this, SafetyTipDetailActivity::class.java)
                .putExtra("tip_id", tip.id)
        )
    }
}
