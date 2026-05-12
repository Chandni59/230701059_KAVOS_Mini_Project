package com.chandni.kavos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safe_zones")
data class SafeZone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val userId: String = ""
)
