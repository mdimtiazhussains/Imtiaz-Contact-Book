package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.DirectoryRepository
import com.example.ui.DirectoryViewModel
import com.example.ui.screens.ImtiazDirectoryScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = DirectoryRepository(database.contactDao())
    val factory = DirectoryViewModel.Factory(repository)

    setContent {
      MyApplicationTheme {
        val vm: DirectoryViewModel = viewModel(factory = factory)
        val uiState by vm.uiState.collectAsStateWithLifecycle()

        ImtiazDirectoryScreen(
          uiState = uiState,
          onNameChange = vm::onNameChange,
          onPhoneChange = vm::onPhoneChange,
          onCitySelect = vm::onCitySelect,
          onCityDropdownToggle = vm::onCityDropdownToggle,
          onFilterSelect = vm::onFilterSelect,
          onSaveContact = vm::onSaveContact,
          onToggleFavorite = vm::onToggleFavorite,
          onDeleteContact = vm::onDeleteContact,
          onSyncWithSheet = vm::onSyncWithSheet,
          onCall = { phone ->
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}"))
            startActivitySafely(intent)
          },
          onMessage = { phone ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${phone.replace(" ", "")}"))
            startActivitySafely(intent)
          },
          onClearToast = vm::clearToast
        )
      }
    }
  }

  private fun startActivitySafely(intent: Intent): Boolean {
    return try {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
