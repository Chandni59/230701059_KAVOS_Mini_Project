package com.chandni.kavos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SafeZoneDao {
    @Query("SELECT * FROM safe_zones WHERE userId = :uid ORDER BY id DESC")
    fun getAllZonesForUser(uid: String): List<SafeZone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertZone(zone: SafeZone)

    @Delete
    fun deleteZone(zone: SafeZone)
}
