package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Contact
import com.example.data.repository.DirectoryRepository
import com.example.data.repository.GoogleSheetSyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DirectoryFilter(val label: String) {
  ALL("All"),
  FAVORITES("Favorites ★"),
  KURNOOL("Kurnool"),
  ABU_DHABI("Abu Dhabi")
}

data class DirectoryUiState(
  val contacts: List<Contact> = emptyList(),
  val nameInput: String = "",
  val phoneInput: String = "",
  val selectedCity: String = "Kurnool",
  val isCityDropdownExpanded: Boolean = false,
  val selectedFilter: DirectoryFilter = DirectoryFilter.ALL,
  val isSaving: Boolean = false,
  val isSyncing: Boolean = false,
  val toastMessage: String? = null
)

class DirectoryViewModel(private val repository: DirectoryRepository) : ViewModel() {

  private val _nameInput = MutableStateFlow("")
  val nameInput = _nameInput.asStateFlow()

  private val _phoneInput = MutableStateFlow("")
  val phoneInput = _phoneInput.asStateFlow()

  private val _selectedCity = MutableStateFlow("Kurnool")
  val selectedCity = _selectedCity.asStateFlow()

  private val _isCityDropdownExpanded = MutableStateFlow(false)
  val isCityDropdownExpanded = _isCityDropdownExpanded.asStateFlow()

  private val _selectedFilter = MutableStateFlow(DirectoryFilter.ALL)
  val selectedFilter = _selectedFilter.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving = _isSaving.asStateFlow()

  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage = _toastMessage.asStateFlow()

  val allContacts: StateFlow<List<Contact>> = repository.getContactsStream()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val isSyncing: StateFlow<Boolean> = repository.isSyncing

  val uiState: StateFlow<DirectoryUiState> = combine(
    allContacts,
    _nameInput,
    _phoneInput,
    _selectedCity,
    _isCityDropdownExpanded,
    _selectedFilter,
    _isSaving,
    isSyncing,
    _toastMessage
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val contacts = args[0] as List<Contact>
    val name = args[1] as String
    val phone = args[2] as String
    val city = args[3] as String
    val isDropdown = args[4] as Boolean
    val filter = args[5] as DirectoryFilter
    val saving = args[6] as Boolean
    val syncing = args[7] as Boolean
    val toast = args[8] as String?

    val filtered = when (filter) {
      DirectoryFilter.ALL -> contacts
      DirectoryFilter.FAVORITES -> contacts.filter { it.isFavorite }
      DirectoryFilter.KURNOOL -> contacts.filter { it.city.equals("Kurnool", ignoreCase = true) }
      DirectoryFilter.ABU_DHABI -> contacts.filter { it.city.equals("Abu Dhabi", ignoreCase = true) }
    }

    DirectoryUiState(
      contacts = filtered,
      nameInput = name,
      phoneInput = phone,
      selectedCity = city,
      isCityDropdownExpanded = isDropdown,
      selectedFilter = filter,
      isSaving = saving,
      isSyncing = syncing,
      toastMessage = toast
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = DirectoryUiState()
  )

  fun onNameChange(name: String) {
    _nameInput.value = name
  }

  fun onPhoneChange(phone: String) {
    _phoneInput.value = phone
  }

  fun onCitySelect(city: String) {
    _selectedCity.value = city
    _isCityDropdownExpanded.value = false
  }

  fun onCityDropdownToggle(expanded: Boolean) {
    _isCityDropdownExpanded.value = expanded
  }

  fun onFilterSelect(filter: DirectoryFilter) {
    _selectedFilter.value = filter
  }

  /**
   * Saves and syncs contact to Google Sheet.
   * Treats HTTP status 200 with {"status": "success"} as verified successful sync.
   * If {"status": "duplicate"}, notifies user that entry already exists.
   * Does NOT display "Sync Complete" unless verified response is received.
   */
  fun onSaveContact() {
    val name = _nameInput.value.trim()
    val phone = _phoneInput.value.trim()
    val city = _selectedCity.value

    if (name.isBlank()) {
      _toastMessage.value = "Please enter contact name"
      return
    }
    if (phone.isBlank()) {
      _toastMessage.value = "Please enter mobile number"
      return
    }

    viewModelScope.launch {
      _isSaving.value = true
      try {
        val result = repository.postContactToGoogleSheet(name = name, mobile = phone, city = city)
        when (result) {
          is GoogleSheetSyncResult.Success -> {
            // Save to local cache upon verified sync
            repository.addContactLocal(name, phone, city)
            _nameInput.value = ""
            _phoneInput.value = ""
            _selectedCity.value = "Kurnool"
            _toastMessage.value = "Sync Complete: Contact saved to Google Sheet"
          }
          is GoogleSheetSyncResult.Duplicate -> {
            _toastMessage.value = "Entry already exists in the Google Sheet"
          }
          is GoogleSheetSyncResult.Error -> {
            // Do not display "Sync Complete" on error
            _toastMessage.value = "Sync Failed: ${result.message}"
          }
        }
      } catch (e: Exception) {
        _toastMessage.value = "Sync Error: ${e.message}"
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun onToggleFavorite(contact: Contact) {
    viewModelScope.launch {
      repository.toggleFavorite(contact.id, contact.isFavorite)
    }
  }

  fun onDeleteContact(contact: Contact) {
    viewModelScope.launch {
      repository.deleteContact(contact.id)
      _toastMessage.value = "Deleted ${contact.name}"
    }
  }

  fun onSyncWithSheet() {
    viewModelScope.launch {
      val result = repository.syncWithGoogleSheet()
      when (result) {
        is GoogleSheetSyncResult.Success -> {
          _toastMessage.value = result.message
        }
        is GoogleSheetSyncResult.Duplicate -> {
          _toastMessage.value = result.message
        }
        is GoogleSheetSyncResult.Error -> {
          _toastMessage.value = result.message
        }
      }
    }
  }

  fun clearToast() {
    _toastMessage.value = null
  }

  class Factory(private val repository: DirectoryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return DirectoryViewModel(repository) as T
    }
  }
}
