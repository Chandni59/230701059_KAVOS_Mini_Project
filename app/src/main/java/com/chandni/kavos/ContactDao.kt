package com.chandni.kavos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactDao {
    @Query("SELECT * FROM emergency_contacts WHERE userId = :uid")
    fun getAllContactsForUser(uid: String): List<Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContact(contact: Contact)

    @Delete
    fun deleteContact(contact: Contact)
}
