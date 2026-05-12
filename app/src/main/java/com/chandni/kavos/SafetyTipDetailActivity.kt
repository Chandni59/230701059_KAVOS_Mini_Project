package com.chandni.kavos

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SafetyTipDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_tip_detail)
        supportActionBar?.hide()

        val tipId = intent.getStringExtra("tip_id") ?: run { finish(); return }
        val tip = SafetyTipsRepo.ALL.firstOrNull { it.id == tipId } ?: run { finish(); return }

        // Gradient hero background
        val hero = findViewById<View>(R.id.heroHeader)
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(getColor(tip.gradientStart), getColor(tip.gradientEnd))
        )
        hero.background = gradient

        findViewById<ImageView>(R.id.ivTipIcon).setImageResource(tip.iconRes)
        findViewById<TextView>(R.id.tvTipTagline).text = tip.tagline.uppercase()
        findViewById<TextView>(R.id.tvTipTitle).text = tip.title
        findViewById<TextView>(R.id.tvTipSummary).text = tip.summary

        findViewById<View>(R.id.cardCall112).setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
        }

        // Helplines: every tip page falls back to the universal trio so
        // the user never has to scroll/search for a number in distress.
        val helplines = if (tip.helplines.isNotEmpty()) tip.helplines else DEFAULT_HELPLINES
        val helplinesContainer = findViewById<LinearLayout>(R.id.helplinesContainer)
        findViewById<TextView>(R.id.tvHelplinesHeading).visibility = View.VISIBLE
        helplines.forEach { line -> helplinesContainer.addView(buildHelplineRow(line)) }

        val sections = findViewById<LinearLayout>(R.id.sectionsContainer)
        val accent = ContextCompat.getColor(this, tip.accentColor)
        tip.sections.forEachIndexed { idx, section ->
            sections.addView(buildSectionCard(section, accent, idx))
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun buildSectionCard(section: SafetyTipSection, accent: Int, index: Int): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_tip_section_card, null)
        view.findViewById<TextView>(R.id.tvSectionHeading).text = section.heading
        view.findViewById<View>(R.id.sectionAccent).setBackgroundColor(accent)

        val bullets = view.findViewById<LinearLayout>(R.id.bulletsContainer)
        section.bullets.forEachIndexed { i, bullet ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_tip_bullet, bullets, false)
            row.findViewById<TextView>(R.id.tvBullet).text = bullet
            val dotBg = row.findViewById<View>(R.id.bulletDot).background?.mutate()
            if (dotBg is GradientDrawable) dotBg.setColor(accent)
            if (i > 0) {
                (row.layoutParams as LinearLayout.LayoutParams).topMargin =
                    (8 * resources.displayMetrics.density).toInt()
            }
            bullets.addView(row)
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = (12 * resources.displayMetrics.density).toInt()
        if (index == 0) lp.topMargin = (4 * resources.displayMetrics.density).toInt()
        view.layoutParams = lp
        return view
    }

    private fun buildHelplineRow(text: String): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_helpline_row, null)
        view.findViewById<TextView>(R.id.tvHelplineText).text = text

        val numberMatch = Regex("(\\d{3,})").find(text)
        val number = numberMatch?.value
        view.setOnClickListener {
            if (number != null) {
                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                startActivity(dial)
            }
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = (8 * resources.displayMetrics.density).toInt()
        view.layoutParams = lp
        return view
    }

    companion object {
        private val DEFAULT_HELPLINES = listOf(
            "All-in-one emergency — 112",
            "Women's helpline — 1091",
            "Ambulance — 108"
        )
    }
}
