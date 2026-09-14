package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.repository.DirectoryRepository
import com.example.ui.DirectoryFilter
import com.example.ui.DirectoryUiState
import com.example.ui.theme.Primary
import com.example.ui.theme.PrimaryVibrant
import com.example.ui.theme.SurfaceCanvas

private val FavoriteGold = Color(0xFFF59E0B)
private val KurnoolTeal = Color(0xFF0D9488)
private val AbuDhabiAmber = Color(0xFFD97706)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImtiazDirectoryScreen(
  uiState: DirectoryUiState,
  onNameChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit,
  onCitySelect: (String) -> Unit,
  onCityDropdownToggle: (Boolean) -> Unit,
  onFilterSelect: (DirectoryFilter) -> Unit,
  onSaveContact: () -> Unit,
  onToggleFavorite: (Contact) -> Unit,
  onDeleteContact: (Contact) -> Unit,
  onSyncWithSheet: () -> Unit,
  onCall: (String) -> Unit,
  onMessage: (String) -> Unit,
  onClearToast: () -> Unit,
  modifier: Modifier = Modifier
) {
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.toastMessage) {
    uiState.toastMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      onClearToast()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = SurfaceCanvas,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Imtiaz Directory",
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("app_title")
            )
            Text(
              text = "Synced with Google Apps Script",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        actions = {
          // Sync button with rotating animation when sync is in progress
          val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
          val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
              animation = tween(1000, easing = LinearEasing)
            ),
            label = "spin_angle"
          )

          IconButton(
            onClick = onSyncWithSheet,
            enabled = !uiState.isSyncing,
            modifier = Modifier.testTag("sync_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Sync with Google Sheet",
              tint = PrimaryVibrant,
              modifier = if (uiState.isSyncing) Modifier.rotate(rotation) else Modifier
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Simple Contact Form (Name, Mobile, City Dropdown, Save)
      item {
        AddContactCard(
          name = uiState.nameInput,
          phone = uiState.phoneInput,
          city = uiState.selectedCity,
          isDropdownExpanded = uiState.isCityDropdownExpanded,
          isSaving = uiState.isSaving,
          onNameChange = onNameChange,
          onPhoneChange = onPhoneChange,
          onCitySelect = onCitySelect,
          onDropdownToggle = onCityDropdownToggle,
          onSave = onSaveContact
        )
      }

      // 2. Filter Pills (All, Favorites, Kurnool, Abu Dhabi)
      item {
        FilterSection(
          selectedFilter = uiState.selectedFilter,
          onFilterSelect = onFilterSelect
        )
      }

      // 3. Contacts Header with count
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${uiState.selectedFilter.label} Contacts (${uiState.contacts.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (uiState.isSyncing) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = PrimaryVibrant
              )
              Text(
                text = "Syncing...",
                fontSize = 12.sp,
                color = PrimaryVibrant
              )
            }
          }
        }
      }

      // 4. Contact Cards List
      if (uiState.contacts.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (uiState.selectedFilter == DirectoryFilter.FAVORITES) {
                "No favorite contacts marked yet.\nTap ★ on any contact to add to favorites."
              } else {
                "No contacts found for ${uiState.selectedFilter.label}."
              },
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 14.sp,
              lineHeight = 20.sp
            )
          }
        }
      } else {
        items(
          items = uiState.contacts,
          key = { it.id }
        ) { contact ->
          SimpleContactCard(
            contact = contact,
            onToggleFavorite = { onToggleFavorite(contact) },
            onDelete = { onDeleteContact(contact) },
            onCall = { onCall(contact.phone) },
            onMessage = { onMessage(contact.phone) }
          )
        }
      }

      // Bottom Spacer for clean edge padding
      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

/**
 * Clean Form Card featuring Name, Mobile Number, and City dropdown (Kurnool / Abu Dhabi).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactCard(
  name: String,
  phone: String,
  city: String,
  isDropdownExpanded: Boolean,
  isSaving: Boolean,
  onNameChange: (String) -> Unit,
  onPhoneChange: (String) -> Unit,
  onCitySelect: (String) -> Unit,
  onDropdownToggle: (Boolean) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(PrimaryVibrant.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = PrimaryVibrant,
            modifier = Modifier.size(18.dp)
          )
        }
        Text(
          text = "Add Contact",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // 1. Name Input Field
      OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Contact Name") },
        placeholder = { Text("e.g. Mohammed Imtiaz") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("name_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PrimaryVibrant,
          focusedLabelColor = PrimaryVibrant
        )
      )

      // 2. Mobile Number Input Field
      OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Mobile Number") },
        placeholder = { Text("e.g. +91 98765 43210") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("phone_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PrimaryVibrant,
          focusedLabelColor = PrimaryVibrant
        )
      )

      // 3. City Dropdown Menu (Kurnool / Abu Dhabi)
      ExposedDropdownMenuBox(
        expanded = isDropdownExpanded,
        onExpandedChange = { onDropdownToggle(!isDropdownExpanded) },
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = city,
          onValueChange = {},
          readOnly = true,
          label = { Text("City") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.LocationCity,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
          modifier = Modifier
            .menuAnchor()
            .fillMaxWidth()
            .testTag("city_dropdown"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryVibrant,
            focusedLabelColor = PrimaryVibrant
          )
        )

        ExposedDropdownMenu(
          expanded = isDropdownExpanded,
          onDismissRequest = { onDropdownToggle(false) }
        ) {
          DirectoryRepository.AVAILABLE_CITIES.forEach { cityName ->
            DropdownMenuItem(
              text = { Text(cityName, fontWeight = if (city == cityName) FontWeight.Bold else FontWeight.Normal) },
              onClick = { onCitySelect(cityName) },
              modifier = Modifier.testTag("city_option_${cityName.lowercase().replace(" ", "_")}")
            )
          }
        }
      }

      // 4. Save Button
      Button(
        onClick = onSave,
        enabled = !isSaving && name.isNotBlank() && phone.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("save_contact_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = PrimaryVibrant,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = Color.White
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Saving & Syncing...")
        } else {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Save & Sync to Sheet", fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

/**
 * Filter chips for All, Favorites, Kurnool, and Abu Dhabi.
 */
@Composable
private fun FilterSection(
  selectedFilter: DirectoryFilter,
  onFilterSelect: (DirectoryFilter) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(DirectoryFilter.values()) { filter ->
      val isSelected = selectedFilter == filter
      FilterChip(
        selected = isSelected,
        onClick = { onFilterSelect(filter) },
        label = { Text(filter.label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = PrimaryVibrant,
          selectedLabelColor = Color.White,
          containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
      )
    }
  }
}

/**
 * Clean Contact Card displaying Contact Name, Mobile Number, City badge,
 * Favorite toggle, Call and Message buttons.
 * (Completely removed: photos, AI research, Design system, position, location, department, AI assist, Live mesh, microphone, search bar).
 */
@Composable
private fun SimpleContactCard(
  contact: Contact,
  onToggleFavorite: () -> Unit,
  onDelete: () -> Unit,
  onCall: () -> Unit,
  onMessage: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left info column: Name, Mobile, City Badge
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = contact.name,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Text(
          text = contact.phone,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(2.dp))

        // City Badge: Kurnool or Abu Dhabi
        val isAbuDhabi = contact.city.contains("Abu Dhabi", ignoreCase = true)
        val badgeColor = if (isAbuDhabi) AbuDhabiAmber else KurnoolTeal
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = contact.city,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = badgeColor
          )
        }
      }

      // Right actions column: Favorite Toggle, Call, Message
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        // 1. Functional Favorite Toggle
        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier
            .size(40.dp)
            .testTag("favorite_toggle_${contact.id}")
        ) {
          Icon(
            imageVector = if (contact.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (contact.isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (contact.isFavorite) FavoriteGold else MaterialTheme.colorScheme.outline
          )
        }

        // 2. Call Action
        IconButton(
          onClick = onCall,
          modifier = Modifier
            .size(40.dp)
            .testTag("call_button_${contact.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Call ${contact.name}",
            tint = PrimaryVibrant
          )
        }

        // 3. Message / SMS Action
        IconButton(
          onClick = onMessage,
          modifier = Modifier
            .size(40.dp)
            .testTag("message_button_${contact.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Message,
            contentDescription = "Message ${contact.name}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // 4. Delete Action
        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(40.dp)
            .testTag("delete_button_${contact.id}")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete ${contact.name}",
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
