package com.chandni.kavos

import com.google.firebase.auth.FirebaseAuth

object UserScope {
    fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
}
