package com.chandni.kavos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private data class Page(
        val icon: Int,
        val bg: Int,
        val title: String,
        val summary: String,
        val whenToUse: String
    )

    private val pages = listOf(
        Page(
            icon = R.drawable.ic_security_shield,
            bg = R.drawable.circle_onboarding_purple,
            title = "Welcome to KAVOS",
            summary = "Your invisible guardian. KAVOS sends your location and a custom message to people you trust — instantly, in any emergency.",
            whenToUse = "First, add at least one Guardian. They'll get an SMS with your live location whenever any SOS trigger fires."
        ),
        Page(
            icon = R.drawable.ic_volume_up,
            bg = R.drawable.circle_onboarding_red,
            title = "Hold for SOS",
            summary = "Long-press the big SOS button to start a 5-second countdown, then SMS goes out to every guardian.",
            whenToUse = "When you feel unsafe and need help fast. You can shake the phone hard 3× as a backup, or use Siren to scare someone off."
        ),
        Page(
            icon = R.drawable.ic_mic_sos,
            bg = R.drawable.circle_onboarding_teal,
            title = "Voice SOS",
            summary = "Say \"help me\", \"save me\", \"bachao\", or \"madad\" out loud — KAVOS hears it and triggers SOS automatically.",
            whenToUse = "When your hands aren't free — being followed, grabbed, or in a moving vehicle. Tap Start once, then keep the screen on."
        ),
        Page(
            icon = R.drawable.ic_timer_sos,
            bg = R.drawable.circle_onboarding_orange,
            title = "Check-in Timer",
            summary = "Set a timer before risky activities. If you don't tap \"I'm Safe\" before it ends, SOS fires automatically to all your guardians.",
            whenToUse = "Walking home alone at night, meeting a stranger, going on a date. The timer is your safety net."
        ),
        Page(
            icon = R.drawable.ic_grid_apps,
            bg = R.drawable.circle_onboarding_purple,
            title = "Disguise Mode",
            summary = "Opens a real working calculator. To unlock the real KAVOS app, type your secret code (default: 111) and tap =.",
            whenToUse = "If someone is forcing you to unlock your phone or you don't want anyone to see KAVOS in your app drawer."
        )
    )

    private lateinit var pager: ViewPager2
    private lateinit var dots: LinearLayout
    private lateinit var btnNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        supportActionBar?.hide()

        pager = findViewById(R.id.onboardingPager)
        dots = findViewById(R.id.dotsContainer)
        btnNext = findViewById(R.id.btnOnboardingNext)

        pager.adapter = PageAdapter(pages)

        buildDots()
        updateForPage(0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateForPage(position)
            }
        })

        btnNext.setOnClickListener {
            if (pager.currentItem < pages.size - 1) {
                pager.currentItem = pager.currentItem + 1
            } else {
                finishOnboarding()
            }
        }

        findViewById<TextView>(R.id.btnSkip).setOnClickListener { finishOnboarding() }
    }

    private fun buildDots() {
        dots.removeAllViews()
        for (i in pages.indices) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size).coerceAtLeast(20),
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size).coerceAtLeast(8)
            )
            params.setMargins(8, 0, 8, 0)
            params.width = (resources.displayMetrics.density * if (i == 0) 20 else 8).toInt()
            params.height = (resources.displayMetrics.density * 6).toInt()
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(this, R.drawable.dot_indicator_inactive)
            dots.addView(dot)
        }
    }

    private fun updateForPage(position: Int) {
        // Update dots
        for (i in 0 until dots.childCount) {
            val dot = dots.getChildAt(i)
            val active = i == position
            dot.background = ContextCompat.getDrawable(
                this,
                if (active) R.drawable.dot_indicator_active else R.drawable.dot_indicator_inactive
            )
            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = (resources.displayMetrics.density * if (active) 20 else 6).toInt()
            params.height = (resources.displayMetrics.density * 6).toInt()
            dot.layoutParams = params
        }
        btnNext.text = if (position == pages.size - 1) "Get Started" else "Next"
    }

    private fun finishOnboarding() {
        getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()

        // If we entered from MainActivity (replay), just finish.
        if (intent.getBooleanExtra("from_replay", false)) {
            finish()
        } else {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }
    }

    private class PageAdapter(val pages: List<Page>) :
        RecyclerView.Adapter<PageAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val hero: View = v.findViewById(R.id.onboardingHero)
            val icon: ImageView = v.findViewById(R.id.ivOnboardingIcon)
            val title: TextView = v.findViewById(R.id.tvOnboardingTitle)
            val summary: TextView = v.findViewById(R.id.tvOnboardingSummary)
            val whenToUse: TextView = v.findViewById(R.id.tvOnboardingWhen)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, position: Int) {
            val p = pages[position]
            h.hero.background = ContextCompat.getDrawable(h.itemView.context, p.bg)
            h.icon.setImageResource(p.icon)
            h.icon.setColorFilter(0xFFFFFFFF.toInt())
            h.title.text = p.title
            h.summary.text = p.summary
            h.whenToUse.text = p.whenToUse
        }

        override fun getItemCount() = pages.size
    }
}
