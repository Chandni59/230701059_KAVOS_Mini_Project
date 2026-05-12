package com.chandni.kavos

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class KnowYourRightsActivity : AppCompatActivity() {

    private val categories = listOf("All", "Public", "Workplace", "Domestic", "Cyber")
    private var selectedCategory = "All"
    private lateinit var adapter: LegalRightAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_know_your_rights)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { goHome() }

        adapter = LegalRightAdapter(LegalRightsRepo.ALL) { right ->
            val intent = Intent(this, LegalRightDetailActivity::class.java)
                .putExtra("right_id", right.id)
            startActivity(intent)
        }

        val rv = findViewById<RecyclerView>(R.id.rvRights)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        buildChips()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
    }

    private fun buildChips() {
        val row = findViewById<LinearLayout>(R.id.chipRow)
        row.removeAllViews()
        val density = resources.displayMetrics.density

        categories.forEach { cat ->
            val chip = TextView(this).apply {
                text = cat
                textSize = 13f
                setTextColor(getColorStateList(R.color.chip_filter_text))
                setBackgroundResource(R.drawable.chip_filter_bg)
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setPadding((18 * density).toInt(), (10 * density).toInt(), (18 * density).toInt(), (10 * density).toInt())
                isSelected = (cat == selectedCategory)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = (8 * density).toInt()
            chip.layoutParams = lp

            chip.setOnClickListener {
                selectedCategory = cat
                applyFilter()
                refreshChipStates(row)
            }
            row.addView(chip)
        }
    }

    private fun refreshChipStates(row: LinearLayout) {
        for (i in 0 until row.childCount) {
            val v = row.getChildAt(i)
            if (v is TextView) v.isSelected = (v.text.toString() == selectedCategory)
        }
    }

    private fun applyFilter() {
        val filtered = if (selectedCategory == "All") LegalRightsRepo.ALL
        else LegalRightsRepo.ALL.filter { it.category == selectedCategory }
        adapter.submit(filtered)
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }
}
