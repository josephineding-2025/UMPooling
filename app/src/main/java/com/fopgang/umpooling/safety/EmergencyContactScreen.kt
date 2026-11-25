package com.fopgang.umpooling.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.booking.RideAction
import com.fopgang.umpooling.booking.RideHeader
import com.fopgang.umpooling.booking.RideMuted
import com.fopgang.umpooling.booking.RidePage
import com.fopgang.umpooling.profile.ProfileUiState

/**
 * Emergency contact (name and phone), saved to the user's profile so it survives a new phone.
 * A copy of the phone number is kept on the device because the SOS screen reads it from there
 * and must work without a connection.
 */
@Composable
fun EmergencyContactScreen(
    owner: String,
    state: ProfileUiState,
    back: () -> Unit,
    onSave: (name: String, phone: String) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember(owner) { context.getSharedPreferences("emergency_$owner", 0) }
    var name by rememberSaveable(owner) { mutableStateOf("") }
    var phone by rememberSaveable(owner) { mutableStateOf("") }
    var filled by rememberSaveable(owner) { mutableStateOf(false) }

    // Fill the form once, from the saved profile, or from the number the SOS screen stored earlier.
    LaunchedEffect(state.loading, state.profile) {
        if (!state.loading && !filled) {
            filled = true
            name = state.contact?.name.orEmpty()
            phone = state.contact?.phone ?: prefs.getString("phone", "").orEmpty()
        }
    }
    // Keep the on-device copy in step with what the server accepted.
    LaunchedEffect(state.contactSaved) {
        if (state.contactSaved) {
            prefs.edit().putString("phone", state.contact?.phone.orEmpty()).apply()
            phone = state.contact?.phone.orEmpty()
        }
    }

    RidePage {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RideHeader(stringResource(R.string.contact_title), back)
            Text(stringResource(R.string.contact_body), color = RideMuted)
            when {
                state.loading -> CircularProgressIndicator()
                state.loadFailed && !filled -> {
                    Text(stringResource(R.string.profile_load_failed), color = MaterialTheme.colorScheme.error)
                    TextButton(onRetry) { Text(stringResource(R.string.profile_retry)) }
                }
                else -> {
                    OutlinedTextField(
                        name, { name = it.take(60) }, Modifier.fillMaxWidth(), enabled = !state.saving, singleLine = true,
                        label = { Text(stringResource(R.string.contact_name)) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    OutlinedTextField(
                        phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) }, Modifier.fillMaxWidth(),
                        enabled = !state.saving, singleLine = true, isError = state.contactInvalid,
                        label = { Text(stringResource(R.string.contact_phone)) },
                        supportingText = { Text(stringResource(R.string.contact_phone_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                    when {
                        state.contactInvalid -> Text(stringResource(R.string.contact_invalid), color = MaterialTheme.colorScheme.error)
                        state.saveFailed -> Text(stringResource(R.string.profile_save_failed), color = MaterialTheme.colorScheme.error)
                        state.contactSaved -> Text(stringResource(R.string.contact_saved), color = RideMuted)
                    }
                    RideAction(stringResource(R.string.contact_save), enabled = !state.saving) { onSave(name, phone) }
                    if (state.contact != null) {
                        TextButton({ name = ""; phone = ""; onSave("", "") }, enabled = !state.saving) {
                            Text(stringResource(R.string.contact_remove), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
