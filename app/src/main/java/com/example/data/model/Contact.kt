package com.example.data.model

data class Contact(
  val id: String,
  val name: String,
  val phone: String,
  val city: String, // "Kurnool" or "Abu Dhabi"
  val isFavorite: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)
