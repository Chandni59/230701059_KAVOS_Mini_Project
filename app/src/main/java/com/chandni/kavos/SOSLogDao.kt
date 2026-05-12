package com.chandni.kavos

import androidx.room.*

@Dao
interface SOSLogDao {
    @Query("SELECT * FROM sos_logs WHERE userId = :uid ORDER BY timestamp DESC")
    fun getAllLogsForUser(uid: String): List<SOSLog>

    @Insert
    fun insertLog(log: SOSLog): Long

    @Query("DELETE FROM sos_logs WHERE userId = :uid")
    fun clearAllForUser(uid: String)

    /** The most recent log without an acknowledgement, fired within the given window. */
    @Query(
        "SELECT * FROM sos_logs " +
            "WHERE userId = :uid AND acknowledgedBy IS NULL AND timestamp >= :since " +
            "ORDER BY timestamp DESC LIMIT 1"
    )
    fun getLatestPendingForUser(uid: String, since: Long): SOSLog?

    @Query(
        "UPDATE sos_logs SET acknowledgedBy = :name, acknowledgedAt = :at, " +
            "replyText = :reply, replyClassification = :cls " +
            "WHERE id = :id"
    )
    fun acknowledge(id: Int, name: String, at: Long, reply: String, cls: String?)
}
