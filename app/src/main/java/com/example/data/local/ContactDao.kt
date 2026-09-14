package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
  @Query("SELECT * FROM contacts ORDER BY name ASC")
  fun getAllContacts(): Flow<List<ContactEntity>>

  @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY name ASC")
  fun getFavoriteContacts(): Flow<List<ContactEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContact(contact: ContactEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContacts(contacts: List<ContactEntity>)

  @Query("UPDATE contacts SET isFavorite = :isFavorite WHERE id = :id")
  suspend fun setFavorite(id: String, isFavorite: Boolean)

  @Query("DELETE FROM contacts WHERE id = :id")
  suspend fun deleteContact(id: String)

  @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
  suspend fun getContactById(id: String): ContactEntity?

  @Query("SELECT COUNT(*) FROM contacts")
  suspend fun getContactCount(): Int
}
