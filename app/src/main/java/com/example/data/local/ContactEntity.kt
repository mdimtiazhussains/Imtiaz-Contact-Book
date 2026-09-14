package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Contact

@Entity(tableName = "contacts")
data class ContactEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  val phone: String,
  val city: String,
  val isFavorite: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toContact(): Contact = Contact(
    id = id,
    name = name,
    phone = phone,
    city = city,
    isFavorite = isFavorite,
    createdAt = createdAt
  )

  companion object {
    fun fromContact(contact: Contact): ContactEntity = ContactEntity(
      id = contact.id,
      name = contact.name,
      phone = contact.phone,
      city = contact.city,
      isFavorite = contact.isFavorite,
      createdAt = contact.createdAt
    )
  }
}
