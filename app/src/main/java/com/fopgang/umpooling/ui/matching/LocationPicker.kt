package com.fopgang.umpooling.ui.matching

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider

/**
 * Reusable location selector Composable backed by the 75 verified UM campus locations,
 * equipped with both a fast search list and an interactive visual Map Picker (FR2.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectorField(
    label: String,
    selectedLocation: CampusLocation?,
    onLocationSelected: (CampusLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedLocation?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Select campus or residential zone") },
            leadingIcon = {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = if (selectedLocation != null) Color(0xFFF08F76) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showMapDialog = true }) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = "Pick on map",
                            tint = Color(0xFF8A3524)
                        )
                    }
                    IconButton(onClick = { showSheet = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose from list")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSheet = true },
            enabled = false,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }

    // Modal Bottom Sheet with Search and Map Picker CTA
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                searchQuery = ""
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(max = 520.dp)
            ) {
                // Interactive Map Shortcut Button
                OutlinedButton(
                    onClick = {
                        showSheet = false
                        showMapDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF8A3524)
                    )
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🗺️ Pick Location on Interactive Map")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search college, faculty, or area...") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val filtered = remember(searchQuery) {
                    CampusLocationProvider.search(searchQuery)
                }

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filtered, key = { it.id }) { loc ->
                        ListItem(
                            headlineContent = { Text(loc.name) },
                            supportingContent = {
                                Text(
                                    if (loc.lat != null && loc.lng != null) {
                                        "${loc.category} • ${String.format("%.4f", loc.lat)}, ${String.format("%.4f", loc.lng)}"
                                    } else {
                                        "${loc.category} • (Select on map for coordinates)"
                                    }
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = if (loc.id.startsWith("kk")) Color(0xFF2E7D32) else Color(0xFFF08F76)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLocationSelected(loc)
                                    showSheet = false
                                    searchQuery = ""
                                }
                        )
                    }
                }
            }
        }
    }

    // Interactive Map Picker Dialog
    if (showMapDialog) {
        MapPickerDialog(
            initialLocation = selectedLocation,
            onLocationConfirmed = { loc ->
                onLocationSelected(loc)
                showMapDialog = false
            },
            onDismiss = { showMapDialog = false }
        )
    }
}
