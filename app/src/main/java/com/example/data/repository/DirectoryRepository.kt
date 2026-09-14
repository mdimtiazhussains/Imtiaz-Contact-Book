package com.example.data.repository

import android.util.Log
import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.model.Contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class GoogleSheetSyncResult {
  data class Success(val message: String = "Sync Complete: Contact saved to Google Sheet") : GoogleSheetSyncResult()
  data class Duplicate(val message: String = "Entry already exists in Google Sheet") : GoogleSheetSyncResult()
  data class Error(val message: String) : GoogleSheetSyncResult()
}

class DirectoryRepository(private val contactDao: ContactDao) {

  companion object {
    private const val TAG = "DirectoryRepository"

    const val BASE_URL =
      "https://script.google.com/macros/s/AKfycbzTNPhvzgxFcQv8jjwUX9dZUXpfeATNl_1-TFpRQbf92TvjdZEKgu2f4pTlURNnH4QbwA/exec"

    const val DEPLOYMENT_ID =
      "AKfycbzTNPhvzgxFcQv8jjwUX9dZUXpfeATNl_1-TFpRQbf92TvjdZEKgu2f4pTlURNnH4QbwA"

    val AVAILABLE_CITIES = listOf("Kurnool", "Abu Dhabi")
  }

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing = _isSyncing.asStateFlow()

  private val _syncMessage = MutableStateFlow<String?>(null)
  val syncMessage = _syncMessage.asStateFlow()

  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
    .build()

  init {
    CoroutineScope(Dispatchers.IO).launch {
      if (contactDao.getContactCount() == 0) {
        seedInitialContacts()
      }
      // Attempt background initial refresh
      try {
        syncWithGoogleSheet()
      } catch (e: Exception) {
        Log.w(TAG, "Initial sync attempt: ${e.message}")
      }
    }
  }

  private suspend fun seedInitialContacts() {
    val initial = listOf(
      ContactEntity(
        id = "c1",
        name = "Mohammed Imtiaz Hussain",
        phone = "+91 98765 43210",
        city = "Kurnool",
        isFavorite = true
      ),
      ContactEntity(
        id = "c2",
        name = "Rashid Al-Nuaimi",
        phone = "+971 52 987 6543",
        city = "Abu Dhabi",
        isFavorite = true
      ),
      ContactEntity(
        id = "c3",
        name = "Syed Farhan",
        phone = "+91 91234 56789",
        city = "Kurnool",
        isFavorite = false
      ),
      ContactEntity(
        id = "c4",
        name = "Zain Khan",
        phone = "+971 50 123 4567",
        city = "Abu Dhabi",
        isFavorite = false
      )
    )
    contactDao.insertContacts(initial)
  }

  fun getContactsStream(): Flow<List<Contact>> {
    return contactDao.getAllContacts().map { entities ->
      entities.map { it.toContact() }
    }
  }

  fun getFavoriteContactsStream(): Flow<List<Contact>> {
    return contactDao.getFavoriteContacts().map { entities ->
      entities.map { it.toContact() }
    }
  }

  /**
   * Saves a contact locally to the Room database.
   */
  suspend fun addContactLocal(name: String, phone: String, city: String): Contact =
    withContext(Dispatchers.IO) {
      val validCity = if (city.equals("Abu Dhabi", ignoreCase = true)) "Abu Dhabi" else "Kurnool"
      val newContact = Contact(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        phone = phone.trim(),
        city = validCity,
        isFavorite = false,
        createdAt = System.currentTimeMillis()
      )
      contactDao.insertContact(ContactEntity.fromContact(newContact))
      newContact
    }

  /**
   * Sends the contact data formatted as:
   * {
   *   "name": "Contact Name Here",
   *   "mobile": "Mobile Number Here",
   *   "city": "City Name Here"
   * }
   * via HTTP POST to the Google Apps Script Web App.
   * Handles 302/307 redirects to Google user content echo endpoints.
   *
   * Response Handling Requirements:
   * - Treat HTTP status 200 OK with JSON {"status": "success"} as a verified successful sync.
   * - If the script returns {"status": "duplicate"}, notify the user that the entry already exists.
   * - Do not display a "Sync Complete" toast or notification unless a successful HTTP response
   *   is received from the Google Apps Script endpoint.
   */
  suspend fun postContactToGoogleSheet(
    name: String,
    mobile: String,
    city: String
  ): GoogleSheetSyncResult = withContext(Dispatchers.IO) {
    try {
      val payload = JSONObject().apply {
        put("name", name.trim())
        put("mobile", mobile.trim())
        put("city", city.trim())
      }.toString()

      val mediaType = "application/json; charset=utf-8".toMediaType()
      val body = payload.toRequestBody(mediaType)
      val request = Request.Builder()
        .url(BASE_URL)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .post(body)
        .build()

      var responseCode: Int
      var responseBody: String

      val initialResponse = httpClient.newCall(request).execute()
      responseCode = initialResponse.code
      responseBody = initialResponse.body?.string().orEmpty()

      // Check if 302/307 redirect was encountered and follow if not automatically consumed
      if (responseCode in 300..399) {
        val redirectLocation = initialResponse.header("Location")
        initialResponse.close()
        if (!redirectLocation.isNullOrBlank()) {
          val redirectRequest = Request.Builder()
            .url(redirectLocation)
            .addHeader("Accept", "application/json")
            .get()
            .build()

          httpClient.newCall(redirectRequest).execute().use { redirected ->
            responseCode = redirected.code
            responseBody = redirected.body?.string().orEmpty()
          }
        }
      } else {
        initialResponse.close()
      }

      Log.d(TAG, "Google Apps Script response [$responseCode]: $responseBody")

      if (responseCode == 200) {
        val clean = responseBody.trim()
        if (clean.startsWith("{")) {
          val json = JSONObject(clean)
          val status = json.optString("status", "").lowercase()
          when (status) {
            "success" -> {
              return@withContext GoogleSheetSyncResult.Success()
            }
            "duplicate" -> {
              val msg = json.optString("message", "This contact already exists in the Google Sheet.")
              return@withContext GoogleSheetSyncResult.Duplicate(msg)
            }
            "error" -> {
              val errorMsg = json.optString("message", "Google Sheet reported an error.")
              return@withContext GoogleSheetSyncResult.Error(errorMsg)
            }
            else -> {
              val genericMsg = json.optString("message", "Server response: $clean")
              return@withContext GoogleSheetSyncResult.Error(genericMsg)
            }
          }
        } else {
          return@withContext GoogleSheetSyncResult.Error("Invalid server response format from Google Apps Script")
        }
      } else {
        return@withContext GoogleSheetSyncResult.Error("HTTP error $responseCode from Google Apps Script endpoint")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error posting contact to Google Sheet: ${e.message}", e)
      return@withContext GoogleSheetSyncResult.Error("Network error: ${e.message ?: "Unable to reach Google Sheet"}")
    }
  }

  suspend fun toggleFavorite(contactId: String, currentFavorite: Boolean) =
    withContext(Dispatchers.IO) {
      contactDao.setFavorite(contactId, !currentFavorite)
    }

  suspend fun deleteContact(contactId: String) = withContext(Dispatchers.IO) {
    contactDao.deleteContact(contactId)
  }

  /**
   * Fetches contact data from the Google Apps Script Web App if the script provides GET list.
   * Returns a Result indicating whether sync was verified successful.
   */
  suspend fun syncWithGoogleSheet(): GoogleSheetSyncResult = withContext(Dispatchers.IO) {
    _isSyncing.value = true
    try {
      val request = Request.Builder()
        .url(BASE_URL)
        .addHeader("Accept", "application/json")
        .build()

      var code: Int
      var body: String

      val resp = httpClient.newCall(request).execute()
      code = resp.code
      body = resp.body?.string().orEmpty()

      if (code in 300..399) {
        val redirectUrl = resp.header("Location")
        resp.close()
        if (!redirectUrl.isNullOrBlank()) {
          val redirectRequest = Request.Builder()
            .url(redirectUrl)
            .addHeader("Accept", "application/json")
            .get()
            .build()
          httpClient.newCall(redirectRequest).execute().use { redirected ->
            code = redirected.code
            body = redirected.body?.string().orEmpty()
          }
        }
      } else {
        resp.close()
      }

      if (code == 200 && body.isNotBlank() && !body.trim().startsWith("<")) {
        val clean = body.trim()
        val parsedList = mutableListOf<ContactEntity>()

        if (clean.startsWith("[")) {
          val array = JSONArray(clean)
          for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val name = item.optString("name", item.optString("Name", "")).trim()
            if (name.isBlank()) continue
            val mobile = item.optString("mobile", item.optString("phone", item.optString("Mobile Number", ""))).trim()
            val rawCity = item.optString("city", item.optString("City", "Kurnool")).trim()
            val city = if (rawCity.contains("Abu Dhabi", ignoreCase = true)) "Abu Dhabi" else "Kurnool"
            parsedList.add(
              ContactEntity(
                id = item.optString("id", "sheet_$i"),
                name = name,
                phone = mobile,
                city = city,
                isFavorite = false
              )
            )
          }
        } else if (clean.startsWith("{")) {
          val json = JSONObject(clean)
          val status = json.optString("status", "").lowercase()
          if (status == "error") {
            val msg = json.optString("message", "Google Sheet access error")
            return@withContext GoogleSheetSyncResult.Error(msg)
          }
          val contactsArray = json.optJSONArray("contacts") ?: json.optJSONArray("data")
          if (contactsArray != null) {
            for (i in 0 until contactsArray.length()) {
              val item = contactsArray.optJSONObject(i) ?: continue
              val name = item.optString("name", item.optString("Name", "")).trim()
              if (name.isBlank()) continue
              val mobile = item.optString("mobile", item.optString("phone", "")).trim()
              val rawCity = item.optString("city", item.optString("City", "Kurnool")).trim()
              val city = if (rawCity.contains("Abu Dhabi", ignoreCase = true)) "Abu Dhabi" else "Kurnool"
              parsedList.add(
                ContactEntity(
                  id = item.optString("id", "sheet_$i"),
                  name = name,
                  phone = mobile,
                  city = city,
                  isFavorite = false
                )
              )
            }
          }
        }

        if (parsedList.isNotEmpty()) {
          contactDao.insertContacts(parsedList)
          return@withContext GoogleSheetSyncResult.Success("Sync Complete: Updated ${parsedList.size} contacts from Google Sheet")
        }
      }

      return@withContext GoogleSheetSyncResult.Error("No data returned from Google Sheet")
    } catch (e: Exception) {
      return@withContext GoogleSheetSyncResult.Error("Sync failed: ${e.message}")
    } finally {
      _isSyncing.value = false
    }
  }
}
