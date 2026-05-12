package com.chandni.kavos

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure this layout exists in your res/layout folder
        setContentView(R.layout.activity_splash)

        // Initialize Firebase Auth
        val auth = FirebaseAuth.getInstance()

        // 2-second delay to show the KAVOS logo before making a decision
        Handler(Looper.getMainLooper()).postDelayed({

            if (auth.currentUser != null) {
                // SESSION FOUND: The user has already signed up/logged in.
                // Go straight to the safety dashboard.
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                // NO SESSION: This is a new user or they logged out.
                // Go to the Authentication (Login/Signup) screen.
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
            }

            // Close the SplashActivity so the user can't go back to it
            finish()

        }, 2000)
    }
}