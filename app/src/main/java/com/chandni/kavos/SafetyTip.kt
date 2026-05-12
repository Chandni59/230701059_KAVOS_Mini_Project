package com.chandni.kavos

data class SafetyTipSection(
    val heading: String,
    val bullets: List<String>
)

data class SafetyTip(
    val id: String,
    val title: String,
    val summary: String,
    val tagline: String,
    val iconRes: Int,
    val accentColor: Int,
    val gradientStart: Int,
    val gradientEnd: Int,
    val sections: List<SafetyTipSection>,
    val helplines: List<String> = emptyList()
)
