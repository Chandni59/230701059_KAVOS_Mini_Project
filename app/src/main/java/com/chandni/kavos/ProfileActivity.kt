package com.chandni.kavos

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.hide()

        val prefs = getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)
        val name = prefs.getString("user_name", "") ?: ""
        val phone = prefs.getString("user_phone", "") ?: ""
        val email = prefs.getString("user_email", "") ?: ""
        val code = prefs.getString("secret_code", "111") ?: "111"

        findViewById<TextView>(R.id.tvProfileName).text = if (name.isNotEmpty()) name else "User"
        findViewById<TextView>(R.id.tvProfileEmail).text = if (email.isNotEmpty()) email else "—"
        findViewById<TextView>(R.id.tvProfilePhone).text = if (phone.isNotEmpty()) phone else "—"
        findViewById<TextView>(R.id.tvProfileCode).text = code
        findViewById<TextView>(R.id.tvAvatarInitial).text =
            if (name.isNotEmpty()) name.first().uppercase() else "?"

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnManageGuardians).setOnClickListener {
            startActivity(Intent(this, ManageGuardiansActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnHowItWorks).setOnClickListener {
            startActivity(
                Intent(this, OnboardingActivity::class.java)
                    .putExtra("from_replay", true)
            )
        }

        findViewById<MaterialButton>(R.id.btnAiSettings).setOnClickListener {
            showAiSettingsDialog()
        }

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            confirmLogout()
        }
    }

    private fun showAiSettingsDialog() {
        val current = GeminiClient.apiKey(this)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val info = TextView(this).apply {
            text = "KAVOS uses Google Gemini to write smarter SOS messages, classify guardian replies, and show a tailored safety nudge on the home screen.\n\n" +
                "It's optional and entirely best-effort — every safety feature still works without a key. " +
                "Get a free key at https://aistudio.google.com → Get API key. The key is stored only on this phone."
            textSize = 13f
            setTextColor(getColor(R.color.text_secondary))
            autoLinkMask = Linkify.WEB_URLS
            movementMethod = LinkMovementMethod.getInstance()
        }
        val edit = EditText(this).apply {
            hint = "Paste Gemini API key (starts with AIza…)"
            setSingleLine(true)
            setText(current ?: "")
        }
        val homeAiSwitch = Switch(this).apply {
            text = "Use AI on home screen (banner + safety plan)"
            textSize = 13f
            isChecked = GeminiClient.homeAiEnabled(this@ProfileActivity)
            setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                GeminiClient.setHomeAiEnabled(this@ProfileActivity, checked)
                if (!checked) {
                    getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE).edit()
                        .remove("home_banner_text")
                        .remove("home_banner_at")
                        .apply()
                }
            }
        }
        val switchHint = TextView(this).apply {
            text = "Turn off to save Gemini free-tier quota. SOS messages and reply classification still use AI when needed."
            textSize = 11f
            setTextColor(getColor(R.color.text_secondary))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(info)
            addView(edit, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = pad / 2 })
            addView(homeAiSwitch, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = pad / 2 })
            addView(switchHint, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = pad / 4 })
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (current == null) "Enable AI features" else "AI key (configured)")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val key = edit.text.toString().trim()
                if (key.isEmpty()) {
                    Toast.makeText(this, "Enter a key or tap Remove", Toast.LENGTH_SHORT).show()
                } else {
                    GeminiClient.setApiKey(this, key)
                    // Force the home banner to refresh next time MainActivity resumes.
                    getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE).edit()
                        .remove("home_banner_text")
                        .remove("home_banner_at")
                        .remove("home_banner_dismissed_at")
                        .apply()
                    Toast.makeText(this, "AI features enabled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)

        if (current != null) {
            builder.setNeutralButton("Remove") { _, _ ->
                GeminiClient.setApiKey(this, null)
                Toast.makeText(this, "AI key removed", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Log out of KAVOS?")
            .setMessage("You'll need to sign in again to access guardians and SOS history.")
            .setPositiveButton("Log Out") { _, _ -> doLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doLogout() {
        FirebaseAuth.getInstance().signOut()
        getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE).edit()
            .putBoolean("is_logged_in", false)
            .apply()

        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, AuthActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
