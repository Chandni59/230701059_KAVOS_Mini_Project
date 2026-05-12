package com.chandni.kavos

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class NearestHelpActivity : AppCompatActivity() {

    private data class Helpline(
        val name: String,
        val number: String,
        val iconRes: Int,
        val tint: Int
    )

    private val helplines = listOf(
        Helpline("National Emergency", "112", R.drawable.ic_emergency_call, Color.parseColor("#E53935")),
        Helpline("Police", "100", R.drawable.ic_zone_police, Color.parseColor("#1E88E5")),
        Helpline("Ambulance / Medical", "108", R.drawable.ic_zone_hospital, Color.parseColor("#43A047")),
        Helpline("Fire", "101", R.drawable.ic_emergency_call, Color.parseColor("#FB8C00")),
        Helpline("Women's Helpline", "1091", R.drawable.ic_security_shield, Color.parseColor("#8E24AA")),
        Helpline("Women in Distress", "181", R.drawable.ic_security_shield, Color.parseColor("#D81B60")),
        Helpline("Childline", "1098", R.drawable.ic_self_defense, Color.parseColor("#7E57C2")),
        Helpline("Cyber Crime", "1930", R.drawable.ic_law_cyber, Color.parseColor("#00897B"))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearest_help)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnMapPolice).setOnClickListener {
            openMapsSearch("police station")
        }
        findViewById<MaterialButton>(R.id.btnMapHospital).setOnClickListener {
            openMapsSearch("hospital")
        }
        findViewById<MaterialButton>(R.id.btnMapPharmacy).setOnClickListener {
            openMapsSearch("24 hour pharmacy")
        }

        renderHelplines()
    }

    private fun renderHelplines() {
        val container = findViewById<LinearLayout>(R.id.helplineContainer)
        for (h in helplines) {
            val row = layoutInflater.inflate(R.layout.item_helpline, container, false)
            row.findViewById<TextView>(R.id.tvHelplineName).text = h.name
            row.findViewById<TextView>(R.id.tvHelplineNumber).text = h.number

            val iconBg = row.findViewById<View>(R.id.helplineIconBg)
            val tinted = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(adjustAlpha(h.tint, 0.18f))
            }
            iconBg.background = tinted

            val iv = row.findViewById<ImageView>(R.id.ivHelplineIcon)
            iv.setImageResource(h.iconRes)
            iv.setColorFilter(h.tint)

            row.findViewById<MaterialButton>(R.id.btnHelplineCall).setOnClickListener {
                dialNumber(h.number)
            }
            container.addView(row)
        }
    }

    private fun dialNumber(number: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMapsSearch(query: String) {
        try {
            // geo:0,0?q= asks the maps app to search and use current location.
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to any maps app
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}")))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open Maps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val a = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }
}
