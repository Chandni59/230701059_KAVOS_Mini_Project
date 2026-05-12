package com.chandni.kavos

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.LocationManager
import android.hardware.Sensor
import android.os.BatteryManager
import java.util.Calendar
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var auth: FirebaseAuth

    // Shake detection — high-pass filter
    private val gravity = FloatArray(3)
    private val linearAcceleration = FloatArray(3)
    private val ALPHA = 0.8f
    private val SHAKE_THRESHOLD_MS2 = 12f
    private val SHAKE_COUNT_NEEDED = 3
    private val SHAKE_RESET_TIME = 2000L
    private var lastUpdate: Long = 0
    private var shakeCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val resetShakeRunnable = Runnable { shakeCount = 0 }

    // Heavy emergency siren — synthesized PCM wail + sub-bass + thump vibration
    private val sirenPlayer by lazy { SirenPlayer(this) }

    // Pulse animation on SOS ring
    private var pulseAnimSet: AnimatorSet? = null

    // Tile references kept for state updates (siren / trip-shield)
    private var tileSirenView: View? = null
    private var tileTripShieldView: View? = null

    // ── Tile model ───────────────────────────────────────────────────────────

    private data class Tile(
        val id: String,
        val title: String,
        val iconRes: Int,
        val tintColor: Int,
        val explainer: Pair<String, String>?
    )

    private val tiles = listOf(
        Tile(
            "trip_shield", "Trip Shield",
            R.drawable.ic_security_shield, R.color.purple_primary,
            "Trip Shield · NEW" to "Guards a whole trip end-to-end.\n\nKAVOS will send your live location to all guardians every 2 minutes, show a sticky 'I'm Safe' notification, and auto-fire a full SOS if you don't end the trip in time.\n\nUse it for: walking home, a cab ride, meeting a stranger, anything where someone needs to know you arrived."
        ),
        Tile(
            "nearest_help", "Nearby Help",
            R.drawable.ic_zone_hospital, R.color.tip_red,
            "Nearby Help" to "Find the closest police stations, hospitals, and pharmacies on the map. Quick-call any emergency helpline (112, 100, 108, 1091)."
        ),
        Tile(
            "share_location", "Share Location",
            R.drawable.ic_share_location, R.color.tip_blue,
            "Share Live Location" to "Sends a non-emergency SMS with your current Google Maps location to every guardian.\n\nUse it for: \"I'm walking home, here's where I am\" — without firing a full SOS."
        ),
        Tile(
            "safe_zones", "Safe Zones",
            R.drawable.ic_zone_home, R.color.tip_green,
            "Safe Zones" to "Save trusted places — home, college, a friend's house, the nearest police station. Tap any zone to navigate there fast in an emergency."
        ),
        Tile(
            "siren", "Siren",
            R.drawable.ic_volume_up, R.color.tip_orange,
            "Siren" to "Plays a loud, attention-grabbing alarm + vibration to scare off attackers or alert nearby people. Tap once to start, tap again to stop. No SMS is sent."
        ),
        Tile(
            "fake_call", "Fake Call",
            R.drawable.ic_call, R.color.tip_purple,
            "Fake Call" to "Simulates an incoming call from \"Dad\" to give you an excuse to leave an uncomfortable situation. Hold the screen for ~3 seconds — the fake call rings."
        ),
        Tile(
            "checkin_timer", "Check-in",
            R.drawable.ic_timer_sos, R.color.tip_blue,
            "Check-in Timer" to "Set a timer (default 5 min) before doing something risky. If you don't tap \"I'm Safe\" before it ends, KAVOS auto-sends an SOS to all your guardians.\n\nUse it for: walking home alone, meeting a stranger, taking a cab."
        ),
        Tile(
            "voice_sos", "Voice SOS",
            R.drawable.ic_mic_sos, R.color.tip_red,
            "Voice SOS" to "Listens for trigger words: \"help me\", \"save me\", \"bachao\", \"madad\". Hearing any of them auto-fires SOS — no tapping needed.\n\nUse it when your hands aren't free."
        ),
        Tile(
            "safety_plan", "Safety Plan",
            R.drawable.ic_awareness, R.color.grad_indigo_a,
            "AI Safety Plan" to "One tap → KAVOS uses AI to generate a personalised 4-step plan for the next 30 minutes, based on the time, your battery, your guardians, and whether Trip Shield is on.\n\nNo typing. Works offline with a sensible static fallback. Set up your free Gemini key in Profile → AI Settings to enable."
        ),
        Tile(
            "ai_advisor", "AI Advisor",
            R.drawable.ic_awareness, R.color.tip_purple,
            "AI Advisor" to "Ask anything about a tricky safety situation — \"someone is following me\", \"is this route safe at 11pm?\", \"my cab took a different turn\". KAVOS gives a calm 4-6 step plan tailored to the moment.\n\nFree to use with a Gemini key from aistudio.google.com (set up in Profile → AI Settings). Online-only."
        ),
        Tile(
            "disguise", "Disguise",
            R.drawable.ic_grid_apps, R.color.tip_purple,
            "Disguise Mode" to "Opens a real working calculator. To get back into KAVOS, type your secret code (default: 111) and tap =.\n\nUse it when someone is forcing you to unlock your phone."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First-launch onboarding — show before anything else if not seen yet.
        val prefs = getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_done", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val name = user.displayName ?: prefs.getString("user_name", "User") ?: "User"
            findViewById<TextView>(R.id.tvWelcomeUser).text = "Stay Safe, $name"
        }

        requestAllPermissions()
        setupSensor()
        startPulseAnimation()
        updateStatusRow()
        buildFeatureGrid()

        // SOS long-press
        findViewById<View>(R.id.btnSOS).setOnLongClickListener {
            triggerSOSWithCountdown("Manual Hold")
            true
        }

        findViewById<View>(R.id.cardGuardians).setOnClickListener {
            startActivity(Intent(this, ManageGuardiansActivity::class.java))
        }
        findViewById<View>(R.id.btnGoToManage).setOnClickListener {
            startActivity(Intent(this, ManageGuardiansActivity::class.java))
        }
        findViewById<View>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, SOSHistoryActivity::class.java))
        }
        findViewById<View>(R.id.btnHandbook).setOnClickListener {
            startActivity(Intent(this, SafetyHandbookActivity::class.java))
        }
        findViewById<View>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<View>(R.id.btnHowToUse).setOnClickListener {
            startActivity(
                Intent(this, OnboardingActivity::class.java)
                    .putExtra("from_replay", true)
            )
        }
        findViewById<View>(R.id.btnImSafe).setOnClickListener { sendImSafePing() }

        findViewById<View>(R.id.btnDismissAiTip).setOnClickListener {
            findViewById<View>(R.id.cardAiTip).visibility = View.GONE
            getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE).edit()
                .putLong("home_banner_dismissed_at", System.currentTimeMillis())
                .apply()
        }
    }

    // ── AI Safety Plan (one-shot dialog) ────────────────────────────────────

    private fun showSafetyPlanDialog() {
        val prefs = getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)
        val firstName = prefs.getString("user_name", null)?.split(" ")?.firstOrNull()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val battery = batteryPercent()
        val tripShieldOn = TripShieldService.isActive
        val key = GeminiClient.apiKey(this)
        val online = GeminiClient.isOnline(this)

        val loading = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Building your plan…")
            .setMessage("Reading time, battery, guardians, and Trip Shield state.")
            .setCancelable(false)
            .show()

        Thread {
            val guardianCount = AppDatabase.getDatabase(this).contactDao()
                .getAllContactsForUser(UserScope.uid()).size
            val recentSos = AppDatabase.getDatabase(this).sosLogDao()
                .getLatestPendingForUser(UserScope.uid(), System.currentTimeMillis() - 60 * 60 * 1000L) != null

            val homeAi = GeminiClient.homeAiEnabled(this)
            val aiPlan = if (key != null && online && homeAi) {
                GeminiClient.safetyPlan(
                    apiKey = key,
                    firstName = firstName,
                    hourOfDay = hour,
                    batteryPct = battery,
                    guardianCount = guardianCount,
                    tripShieldOn = tripShieldOn,
                    recentSosWithinHour = recentSos
                )
            } else null

            val plan = aiPlan ?: staticSafetyPlan(
                hour = hour,
                battery = battery,
                guardianCount = guardianCount,
                tripShieldOn = tripShieldOn,
                recentSos = recentSos
            )

            val source = when {
                aiPlan != null -> "Personalised by AI · just now"
                key == null -> "Default plan · enable AI in Profile for a personalised one"
                !homeAi -> "Default plan · home AI is off (Profile → AI Settings)"
                !online -> "Default plan · you're offline"
                else -> "Default plan · AI couldn't respond"
            }

            runOnUiThread {
                loading.dismiss()
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Your next 30 minutes")
                    .setMessage("$plan\n\n— $source")
                    .setPositiveButton("Got it", null)
                    .setNeutralButton("Refresh") { _, _ -> showSafetyPlanDialog() }
                    .show()
            }
        }.start()
    }

    private fun staticSafetyPlan(
        hour: Int,
        battery: Int,
        guardianCount: Int,
        tripShieldOn: Boolean,
        recentSos: Boolean
    ): String {
        val steps = mutableListOf<String>()
        when {
            recentSos -> steps += "Check your messages — a recent SOS is awaiting a guardian reply."
            guardianCount == 0 -> steps += "Add at least one guardian — open Manage Guardians from the home card."
            else -> steps += "Confirm your guardians' numbers are reachable on this network."
        }
        if (battery < 25) steps += "Charge your phone now or enable battery saver — battery is at $battery%."
        if (hour in 21..23 || hour in 0..5) {
            if (!tripShieldOn) steps += "Turn on Trip Shield before heading out — it auto-alerts if you stop moving."
            else steps += "Trip Shield is ON — keep your phone unlocked and visible."
        } else {
            steps += "Save the place you're going to as a Safe Zone for one-tap navigation later."
        }
        steps += "Test Voice SOS once today — say \"help me\" with the screen open to confirm it triggers."
        return steps.take(4).mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")
    }

    // ── AI safety nudge banner ───────────────────────────────────────────────

    private fun refreshAiTipBanner() {
        val card = findViewById<View>(R.id.cardAiTip)
        val tv = findViewById<TextView>(R.id.tvAiTip)
        val key = GeminiClient.apiKey(this)
        val prefs = getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)

        // Hide if user has no key configured — banner is opt-in via Profile.
        if (key == null) { card.visibility = View.GONE; return }
        // Hide if user disabled home AI calls (saves quota).
        if (!GeminiClient.homeAiEnabled(this)) { card.visibility = View.GONE; return }

        // Respect a 6-hour dismiss window before nagging again.
        val dismissedAt = prefs.getLong("home_banner_dismissed_at", 0L)
        if (System.currentTimeMillis() - dismissedAt < 6 * 60 * 60 * 1000L) {
            card.visibility = View.GONE
            return
        }

        // Reuse cached tip if it's <60 minutes old — saves an API call per resume.
        val cachedTip = prefs.getString("home_banner_text", null)
        val cachedAt = prefs.getLong("home_banner_at", 0L)
        if (cachedTip != null && System.currentTimeMillis() - cachedAt < 60 * 60 * 1000L) {
            tv.text = cachedTip
            card.visibility = View.VISIBLE
            return
        }

        if (!GeminiClient.isOnline(this)) { card.visibility = View.GONE; return }

        val firstName = prefs.getString("user_name", null)?.split(" ")?.firstOrNull()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val battery = batteryPercent()
        val tripShieldOn = TripShieldService.isActive

        Thread {
            val guardianCount = AppDatabase.getDatabase(this).contactDao()
                .getAllContactsForUser(UserScope.uid()).size
            val tip = GeminiClient.homeBanner(
                apiKey = key,
                firstName = firstName,
                hourOfDay = hour,
                batteryPct = battery,
                guardianCount = guardianCount,
                tripShieldOn = tripShieldOn
            )
            runOnUiThread {
                if (tip.isNullOrBlank()) {
                    card.visibility = View.GONE
                } else {
                    tv.text = tip
                    card.visibility = View.VISIBLE
                    prefs.edit()
                        .putString("home_banner_text", tip)
                        .putLong("home_banner_at", System.currentTimeMillis())
                        .apply()
                }
            }
        }.start()
    }

    private fun batteryPercent(): Int {
        return try {
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        } catch (_: Exception) { 100 }
    }

    // ── "I'm Safe" status broadcast ─────────────────────────────────────────

    private fun sendImSafePing() {
        Thread {
            val contacts = AppDatabase.getDatabase(this).contactDao().getAllContactsForUser(UserScope.uid())
            runOnUiThread {
                if (contacts.isEmpty()) {
                    showExplainer(
                        "No guardians yet",
                        "Add at least one guardian first — tap Manage in the Guardians card below."
                    )
                    return@runOnUiThread
                }

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Send safe check-in?")
                    .setMessage(
                        "Sends \"I'm safe\" with your current location to ${contacts.size} guardian(s).\n\nUse it when you've reached home or finished a trip — let people know you're okay."
                    )
                    .setPositiveButton("Send") { _, _ -> doSendImSafe(contacts) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }.start()
    }

    private fun doSendImSafe(contacts: List<Contact>) {
        Thread {
            val location = getLiveLocation()
            val mapsLink = "https://www.google.com/maps?q=$location"
            val body = "I'm safe and reached. Location: $mapsLink — sent via KAVOS"

            try {
                val smsManager = getSystemService(SmsManager::class.java)
                for (contact in contacts) {
                    val parts = smsManager.divideMessage(body)
                    smsManager.sendMultipartTextMessage(
                        contact.phoneNumber, null, parts, null, null
                    )
                }
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Safe check-in sent to ${contacts.size} guardian(s) ✓",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this, "Could not send SMS — check permissions", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    // ── Feature grid ─────────────────────────────────────────────────────────

    private fun buildFeatureGrid() {
        val grid = findViewById<GridLayout>(R.id.featureGrid)
        grid.removeAllViews()

        val cols = grid.columnCount
        val density = resources.displayMetrics.density
        val tileMargin = (5 * density).toInt()
        val tileHeight = (76 * density).toInt()

        for (tile in tiles) {
            val view = LayoutInflater.from(this)
                .inflate(R.layout.item_feature_tile, grid, false)

            view.findViewById<TextView>(R.id.tileTitle).text = tile.title
            val icon = view.findViewById<ImageView>(R.id.tileIcon)
            icon.setImageResource(tile.iconRes)
            val tint = ContextCompat.getColor(this, tile.tintColor)
            icon.imageTintList = ColorStateList.valueOf(tint)
            val bgTint = (tint and 0x00FFFFFF) or 0x22000000
            view.findViewById<View>(R.id.tileIconBg).backgroundTintList =
                ColorStateList.valueOf(bgTint)

            // GridLayout requires specific layout params
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = tileHeight
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                setMargins(tileMargin, tileMargin, tileMargin, tileMargin)
            }
            view.layoutParams = lp

            view.setOnClickListener { handleTileClick(tile) }
            tile.explainer?.let { (title, body) ->
                view.setOnLongClickListener {
                    showExplainer(title, body)
                    true
                }
            }

            when (tile.id) {
                "siren" -> tileSirenView = view
                "trip_shield" -> {
                    tileTripShieldView = view
                    setTileBadge(view, "NEW", R.color.tip_orange)
                }
            }

            grid.addView(view)
        }
    }

    private fun handleTileClick(tile: Tile) {
        when (tile.id) {
            "trip_shield" -> startActivity(Intent(this, TripShieldActivity::class.java))
            "nearest_help" -> startActivity(Intent(this, NearestHelpActivity::class.java))
            "share_location" -> shareLiveLocation()
            "safe_zones" -> startActivity(Intent(this, SafeZonesActivity::class.java))
            "siren" -> toggleSiren()
            "fake_call" -> startActivity(Intent(this, FakeCallActivity::class.java))
            "checkin_timer" -> startActivity(Intent(this, CheckInTimerActivity::class.java))
            "voice_sos" -> startActivity(Intent(this, VoiceSOSActivity::class.java))
            "safety_plan" -> showSafetyPlanDialog()
            "ai_advisor" -> startActivity(Intent(this, AIAdvisorActivity::class.java))
            "disguise" -> startActivity(Intent(this, DisguiseActivity::class.java))
        }
    }

    private fun setTileBadge(view: View?, text: String?, bgColorRes: Int = R.color.green_safe) {
        val badge = view?.findViewById<TextView>(R.id.tileBadge) ?: return
        if (text == null) {
            badge.visibility = View.GONE
        } else {
            badge.visibility = View.VISIBLE
            badge.text = text
            badge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, bgColorRes))
        }
    }

    private fun showExplainer(title: String, body: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton("Got it", null)
            .show()
    }

    // ── Share Live Location (non-emergency) ──────────────────────────────────

    private fun shareLiveLocation() {
        Thread {
            val contacts = AppDatabase.getDatabase(this).contactDao().getAllContactsForUser(UserScope.uid())
            runOnUiThread {
                if (contacts.isEmpty()) {
                    showExplainer(
                        "No guardians yet",
                        "Add at least one guardian first — tap Manage in the Guardians card below."
                    )
                    return@runOnUiThread
                }

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Share live location?")
                    .setMessage(
                        "Sends your current Google Maps location to ${contacts.size} guardian(s) as a normal SMS — no SOS alarm.\n\nUse it for: \"I'm on my way / running late / here's where I am.\""
                    )
                    .setPositiveButton("Send") { _, _ -> doShareLocation(contacts) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }.start()
    }

    private fun doShareLocation(contacts: List<Contact>) {
        Thread {
            val location = getLiveLocation()
            val mapsLink = "https://www.google.com/maps?q=$location"
            val body = "Hey, just sharing where I am right now: $mapsLink — KAVOS"

            try {
                val smsManager = getSystemService(SmsManager::class.java)
                for (contact in contacts) {
                    val parts = smsManager.divideMessage(body)
                    smsManager.sendMultipartTextMessage(
                        contact.phoneNumber, null, parts, null, null
                    )
                }
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Location shared with ${contacts.size} guardian(s)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this, "Could not send SMS — check permissions", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun getLiveLocation(): String {
        return try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) "${loc.latitude},${loc.longitude}" else "12.9152,80.0000"
            } else "12.9152,80.0000"
        } catch (_: Exception) {
            "12.9152,80.0000"
        }
    }

    // ── Siren ───────────────────────────────────────────────────────────────

    private fun toggleSiren() {
        if (!sirenPlayer.isRunning()) {
            try {
                sirenPlayer.start()
                setTileBadge(tileSirenView, "ON", R.color.sos_red)
            } catch (_: Exception) {
                Toast.makeText(this, "Could not start siren", Toast.LENGTH_SHORT).show()
            }
        } else {
            sirenPlayer.stop()
            setTileBadge(tileSirenView, null)
        }
    }

    // ── Pulse animation ──────────────────────────────────────────────────────

    private fun startPulseAnimation() {
        val ring = findViewById<View>(R.id.sosRing)
        val scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.45f)
        val scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.45f)
        val alpha = ObjectAnimator.ofFloat(ring, "alpha", 0.65f, 0f)

        pulseAnimSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1800
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ring.scaleX = 1f
                    ring.scaleY = 1f
                    ring.alpha = 0.65f
                    start()
                }
            })
            start()
        }
    }

    // ── Status row ───────────────────────────────────────────────────────────

    private fun updateStatusRow() {
        val tv = findViewById<TextView>(R.id.tvStatusRow)
        tv.text = "Shake: ON  •  Trip Shield: ${if (TripShieldService.isActive) "ON" else "OFF"}"
    }

    // ── SOS countdown ────────────────────────────────────────────────────────

    fun triggerSOSWithCountdown(method: String) {
        val tvCountdown = findViewById<TextView>(R.id.tvCountdown)
        tvCountdown.visibility = View.VISIBLE
        var seconds = 5

        val countdownRunnable = object : Runnable {
            override fun run() {
                if (seconds > 0) {
                    tvCountdown.text = "SOS in $seconds s — Tap to cancel"
                    tvCountdown.setOnClickListener {
                        mainHandler.removeCallbacks(this)
                        tvCountdown.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Cancelled", Toast.LENGTH_SHORT).show()
                    }
                    seconds--
                    mainHandler.postDelayed(this, 1000)
                } else {
                    tvCountdown.visibility = View.GONE
                    SOSHelper.sendSOS(this@MainActivity, method)
                }
            }
        }
        mainHandler.post(countdownRunnable)
    }

    // ── Guardian list ────────────────────────────────────────────────────────

    private fun refreshGuardianList() {
        val tvContactList = findViewById<TextView>(R.id.tvContactList)
        val tvGuardianCount = findViewById<TextView>(R.id.tvGuardianCount)
        Thread {
            val contacts = AppDatabase.getDatabase(this).contactDao().getAllContactsForUser(UserScope.uid())
            val text = if (contacts.isEmpty()) "No guardians added yet"
            else contacts.joinToString("\n") { "• ${it.name}" }
            runOnUiThread {
                tvContactList.text = text
                tvGuardianCount.text = contacts.size.toString()
            }
        }.start()
    }

    // ── Last SOS banner ──────────────────────────────────────────────────────

    private fun refreshLastSosBanner() {
        val card = findViewById<View>(R.id.cardLastSOS)
        val title = findViewById<TextView>(R.id.tvLastSosTitle)
        val sub = findViewById<TextView>(R.id.tvLastSosSub)
        val dot = findViewById<View>(R.id.lastSosDot)

        Thread {
            val logs = AppDatabase.getDatabase(this).sosLogDao().getAllLogsForUser(UserScope.uid())
            val latest = logs.firstOrNull()
            runOnUiThread {
                if (latest == null) {
                    card.visibility = View.GONE
                    return@runOnUiThread
                }
                card.visibility = View.VISIBLE
                val ago = relativeTime(System.currentTimeMillis() - latest.timestamp)
                val ackBy = latest.acknowledgedBy
                val ackAt = latest.acknowledgedAt
                if (!ackBy.isNullOrEmpty() && ackAt != null) {
                    val cls = runCatching { ReplyClass.valueOf(latest.replyClassification.orEmpty()) }
                        .getOrNull()
                    val (dotRes, headline) = when (cls) {
                        ReplyClass.COMING_NOW -> R.drawable.dot_status_green to "Last SOS · $ackBy is on the way"
                        ReplyClass.ACKNOWLEDGED -> R.drawable.dot_status_green to "Last SOS · $ackBy got your alert"
                        ReplyClass.CANT_HELP -> R.drawable.dot_status_amber to "Last SOS · $ackBy can't help right now"
                        ReplyClass.UNCLEAR, null -> R.drawable.dot_status_amber to "Last SOS · $ackBy replied"
                    }
                    dot.background = ContextCompat.getDrawable(this, dotRes)
                    val deltaMin = ((ackAt - latest.timestamp) / 60000L).coerceAtLeast(0L)
                    title.text = headline
                    sub.text = if (deltaMin == 0L)
                        "Replied immediately · $ago ago"
                    else
                        "Replied $deltaMin min later · $ago ago"
                } else {
                    dot.background = ContextCompat.getDrawable(this, R.drawable.dot_status_amber)
                    title.text = "Last SOS · awaiting reply"
                    sub.text = "Sent $ago ago via ${latest.triggerMethod} · tap History for details"
                }
                card.setOnClickListener {
                    startActivity(Intent(this, SOSHistoryActivity::class.java))
                }
            }
        }.start()
    }

    private fun relativeTime(deltaMs: Long): String {
        val mins = deltaMs / 60_000L
        return when {
            mins < 1 -> "just now"
            mins < 60 -> "${mins}m"
            mins < 60 * 24 -> "${mins / 60}h"
            else -> "${mins / (60 * 24)}d"
        }
    }

    // ── Sensor setup ─────────────────────────────────────────────────────────

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private fun requestAllPermissions() {
        val perms = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 101)
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        refreshGuardianList()
        refreshLastSosBanner()
        refreshAiTipBanner()
        updateStatusRow()
        // Trip Shield badge: ON while a trip is running, NEW otherwise.
        if (TripShieldService.isActive) {
            setTileBadge(tileTripShieldView, "ON", R.color.sos_red)
        } else {
            setTileBadge(tileTripShieldView, "NEW", R.color.tip_orange)
        }
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        sirenPlayer.stop()
        pulseAnimSet?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
    }

    // ── Shake detection (high-pass filter) ───────────────────────────────────

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val curTime = System.currentTimeMillis()
        if ((curTime - lastUpdate) < 50) return
        lastUpdate = curTime

        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0]
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1]
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2]
        linearAcceleration[0] = event.values[0] - gravity[0]
        linearAcceleration[1] = event.values[1] - gravity[1]
        linearAcceleration[2] = event.values[2] - gravity[2]

        val accelMag = sqrt(
            linearAcceleration[0].pow(2) +
            linearAcceleration[1].pow(2) +
            linearAcceleration[2].pow(2)
        )

        if (accelMag > SHAKE_THRESHOLD_MS2) {
            shakeCount++
            mainHandler.removeCallbacks(resetShakeRunnable)
            mainHandler.postDelayed(resetShakeRunnable, SHAKE_RESET_TIME)

            if (shakeCount >= SHAKE_COUNT_NEEDED) {
                shakeCount = 0
                triggerSOSWithCountdown("Shake")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
