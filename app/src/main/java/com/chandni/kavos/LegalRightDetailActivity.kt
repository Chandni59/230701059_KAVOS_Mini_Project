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

class LegalRightDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal_right_detail)
        supportActionBar?.hide()

        val rightId = intent.getIntExtra("right_id", -1)
        val right = LegalRightsRepo.ALL.firstOrNull { it.id == rightId }
        if (right == null) { finish(); return }

        val hero = findViewById<View>(R.id.heroHeader)
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(getColor(right.gradientStart), getColor(right.gradientEnd))
        )
        hero.background = gradient

        findViewById<ImageView>(R.id.ivDetailIcon).setImageResource(right.iconRes)
        findViewById<TextView>(R.id.tvDetailCategory).text = right.category.uppercase()
        findViewById<TextView>(R.id.tvDetailTitle).text = right.title
        findViewById<TextView>(R.id.tvDetailShort).text = right.shortLine
        findViewById<TextView>(R.id.tvWhatMeans).text = right.whatItMeans

        val stepsContainer = findViewById<LinearLayout>(R.id.stepsContainer)
        right.whatToDo.forEachIndexed { idx, step ->
            stepsContainer.addView(buildStepRow(idx + 1, step))
        }

        val helplines = findViewById<LinearLayout>(R.id.helplinesContainer)
        right.helplines.forEach { line ->
            helplines.addView(buildHelplineRow(line))
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun buildStepRow(num: Int, text: String): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_step_row, null)
        view.findViewById<TextView>(R.id.tvStepNum).text = num.toString()
        view.findViewById<TextView>(R.id.tvStepText).text = text
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = (10 * resources.displayMetrics.density).toInt()
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
}
