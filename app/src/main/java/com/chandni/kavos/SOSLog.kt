package com.chandni.kavos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sos_logs")
data class SOSLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val triggerMethod: String,
    val location: String,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val replyText: String? = null,
    val replyClassification: String? = null,
    val userId: String = ""
)
