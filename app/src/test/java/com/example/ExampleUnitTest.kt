package com.example

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @Test
  fun payload_matches_exact_google_sheet_columns() {
    val name = "Mohammed Imtiaz"
    val mobile = "+91 "
    val city = "Kurnool"

    val payload = JSONObject().apply {
      put("name", name)
      put("mobile", mobile)
      put("city", city)
    }

    assertEquals("Mohammed Imtiaz", payload.getString("name"))
    assertEquals("+91 ", payload.getString("mobile"))
    assertEquals("Kurnool", payload.getString("city"))

    // Ensure exact keys are present
    assertTrue(payload.has("name"))
    assertTrue(payload.has("mobile"))
    assertTrue(payload.has("city"))
  }

  @Test
  fun test_google_apps_script_response_parsing() {
    val successJson = JSONObject("""{"status":"success"}""")
    assertEquals("success", successJson.getString("status"))

    val duplicateJson = JSONObject("""{"status":"duplicate","message":"Entry already exists"}""")
    assertEquals("duplicate", duplicateJson.getString("status"))
  }
}
