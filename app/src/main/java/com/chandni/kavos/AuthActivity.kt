package com.chandni.kavos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {

    private var isLoginMode = true
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etAuthEmail)
        val etPassword = findViewById<EditText>(R.id.etAuthPassword)
        val etSecretCode = findViewById<EditText>(R.id.etAuthPin)
        val etName = findViewById<EditText>(R.id.etAuthName)
        val etPhone = findViewById<EditText>(R.id.etAuthPhone)

        val btnAction = findViewById<Button>(R.id.btnAuthAction)
        val tvSwitch = findViewById<TextView>(R.id.tvSwitchMode)
        val tvTitle = findViewById<TextView>(R.id.tvAuthTitle)
        val tvSub = findViewById<TextView>(R.id.tvAuthSub)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        updateMode(tvTitle, tvSub, btnAction, etName, etPhone, tvSwitch)

        tvSwitch.setOnClickListener {
            isLoginMode = !isLoginMode
            updateMode(tvTitle, tvSub, btnAction, etName, etPhone, tvSwitch)
        }

        btnAction.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val secretCode = etSecretCode.text.toString().trim()

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (secretCode.length < 3) {
                Toast.makeText(this, "Secret code must be at least 3 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnAction.isEnabled = false

            if (isLoginMode) {
                doLogin(email, password, progressBar, btnAction)
            } else {
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()

                if (name.isEmpty()) {
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (phone.length < 10) {
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    Toast.makeText(this, "Please enter a valid phone number (10+ digits)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                doSignup(email, password, name, phone, secretCode, progressBar, btnAction)
            }
        }
    }

    private fun doLogin(
        email: String,
        password: String,
        progressBar: ProgressBar,
        btnAction: Button
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fetchProfileAndNavigate(auth.currentUser?.uid, progressBar, btnAction)
                } else {
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    val msg = task.exception?.localizedMessage ?: "Login failed"
                    Toast.makeText(this, "Login failed: $msg", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Login error", task.exception)
                }
            }
    }

    private fun doSignup(
        email: String,
        password: String,
        name: String,
        phone: String,
        secretCode: String,
        progressBar: ProgressBar,
        btnAction: Button
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    progressBar.visibility = View.GONE
                    btnAction.isEnabled = true
                    val msg = task.exception?.localizedMessage ?: "Signup failed"
                    Toast.makeText(this, "Signup failed: $msg", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Signup error", task.exception)
                    return@addOnCompleteListener
                }

                // AUTH SUCCEEDED. Save locally first — the app needs nothing else to work.
                saveLocalPrefs(name, phone, secretCode, email)

                // Now attempt cloud backup. If this fails, the user is still signed up locally.
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                    return@addOnCompleteListener
                }

                val userMap = hashMapOf(
                    "name" to name,
                    "phone" to phone,
                    "secret_code" to secretCode,
                    "email" to email
                )

                db.collection("users").document(uid).set(userMap)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                    .addOnFailureListener { e ->
                        // Cloud write failed (likely Firestore rules). User is still signed up.
                        progressBar.visibility = View.GONE
                        Log.w(TAG, "Cloud sync failed (using local profile): ${e.message}", e)
                        Toast.makeText(
                            this,
                            "Account created (offline mode). You can use the app normally.",
                            Toast.LENGTH_LONG
                        ).show()
                        navigateToMain()
                    }
            }
    }

    private fun fetchProfileAndNavigate(
        uid: String?,
        progressBar: ProgressBar,
        btnAction: Button
    ) {
        if (uid == null) {
            progressBar.visibility = View.GONE
            btnAction.isEnabled = true
            Toast.makeText(this, "Login failed: no user id", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val code = document.getString("secret_code") ?: "111"
                    val email = document.getString("email") ?: ""
                    saveLocalPrefs(name, phone, code, email)
                }
                // If the document doesn't exist, fall back to whatever is already in prefs
                // (or defaults). User is authed — let them in.
                ensureLoggedInFlag()
                navigateToMain()
            }
            .addOnFailureListener { e ->
                // Cloud read failed — but auth already succeeded. Use existing local prefs.
                progressBar.visibility = View.GONE
                Log.w(TAG, "Cloud profile read failed: ${e.message}", e)
                ensureLoggedInFlag()
                navigateToMain()
            }
    }

    private fun saveLocalPrefs(name: String, phone: String, code: String, email: String) {
        val prefs = getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_name", name)
            putString("user_phone", phone)
            putString("secret_code", code)
            putString("user_email", email)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    private fun ensureLoggedInFlag() {
        val prefs = getSharedPreferences("KAVOS_PREFS", MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", true).apply()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun updateMode(
        tvTitle: TextView, tvSub: TextView,
        btnAction: Button, etName: EditText,
        etPhone: EditText, tvSwitch: TextView
    ) {
        if (isLoginMode) {
            tvTitle.text = "Welcome Back"
            tvSub.text = "Login to your KAVOS account"
            btnAction.text = "Login"
            etName.visibility = View.GONE
            etPhone.visibility = View.GONE
            tvSwitch.text = "New here? Create an account"
        } else {
            tvTitle.text = "Create Account"
            tvSub.text = "Setup your disguise and safety info"
            btnAction.text = "Sign Up"
            etName.visibility = View.VISIBLE
            etPhone.visibility = View.VISIBLE
            tvSwitch.text = "Already have an account? Login"
        }
    }

    companion object {
        private const val TAG = "AuthActivity"
    }
}
