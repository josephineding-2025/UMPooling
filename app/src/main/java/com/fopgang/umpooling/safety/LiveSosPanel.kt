
package com.fopgang.umpooling.safety

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import androidx.core.net.toUri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.fopgang.umpooling.R
import com.fopgang.umpooling.coordination.CoordinationRepository
import com.fopgang.umpooling.coordination.TripRecord
import com.fopgang.umpooling.data.EmergencyContact
import com.fopgang.umpooling.data.TripOffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val LOCATION_TIMEOUT_MS = 10000L

private fun hasLocationPermission(context: Context): Boolean {
    return listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).any {
        ContextCompat.checkSelfPermission(
            context, it
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * One-time foreground location request.
 * Does not retain or continuously track location.
 */
private suspend fun obtainSosLocation(
    context: Context
): Location? {
    if (!hasLocationPermission(context)) return null

    val manager = context.getSystemService(
        Context.LOCATION_SERVICE
    ) as? LocationManager ?: return null

    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val providers = if (hasFine) {
        listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )
    } else {
        listOf(LocationManager.NETWORK_PROVIDER)
    }

    val provider = providers.firstOrNull {
        runCatching {
            manager.isProviderEnabled(it)
        }.getOrDefault(false)
    } ?: return null

    return try {
        withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    manager.removeUpdates(listener)
                }

                try {
                    manager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    manager.removeUpdates(listener)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                } catch (e: IllegalArgumentException) {
                    manager.removeUpdates(listener)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: SecurityException) {
        null
    }
}

@Composable
internal fun LiveSosPanel(
    repository: CoordinationRepository,
    trip: TripRecord,
    offer: TripOffer,
    back: () -> Unit,
    prepared: Boolean = false,
    emergencyContact: EmergencyContact? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val contact = emergencyContact?.phone.orEmpty()

    var ready by remember(trip.id) {
        mutableStateOf(false)
    }

    var preparing by remember(trip.id) {
        mutableStateOf(false)
    }

    var coordinates by remember(trip.id) {
        mutableStateOf<Location?>(null)
    }

    var error by remember {
        mutableStateOf<Int?>(null)
    }

    var permissionDenied by remember {
        mutableStateOf(false)
    }

    fun prepareEmergencyMessage() {
        if (preparing || ready) return

        if (contact.filter(Char::isDigit).length < 5) {
            error = R.string.sos_no_contact
            return
        }

        preparing = true
        error = null
        coordinates = null

        scope.launch {
            try {
                coordinates = obtainSosLocation(context)

                if (coordinates == null) {
                    error = if (permissionDenied) {
                        R.string.sos_permission_denied
                    } else {
                        R.string.sos_location_unavailable
                    }
                }

                ready = true
            } finally {
                preparing = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionDenied = result.values.none { it }
        prepareEmergencyMessage()
    }

    fun activateSos() {
        if (contact.filter(Char::isDigit).length < 5) {
            error = R.string.sos_no_contact
            return
        }

        if (hasLocationPermission(context)) {
            permissionDenied = false
            prepareEmergencyMessage()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun openComposer(whatsapp: Boolean) {
        val phone = SosMessageUtils.normalizePhone(contact)

        val locationText = coordinates?.let {
        SosMessageUtils.mapsLink(
            it.latitude,
            it.longitude
        )
    } ?: context.getString(
        R.string.sos_location_not_available
    )
        val unavailable = context.getString(
            R.string.sos_location_not_available
        )

        val body = context.getString(
            R.string.sos_emergency_body,
            trip.id,
            offer.originZone ?: unavailable,
            offer.destinationZone ?: unavailable,
            trip.plate ?: unavailable,
            locationText
        )
        val intent = if (whatsapp) {
            Intent(
                Intent.ACTION_VIEW,
                (
                    "https://wa.me/" +
                        SosMessageUtils.whatsappPhone(phone) +
                        "?text=" + Uri.encode(body)
                ).toUri()
            )
        } else {
            Intent(
                Intent.ACTION_SENDTO,
                "smsto:$phone".toUri()
            ).putExtra("sms_body", body)
        }
        try {
            repository.checkOwner()
            context.startActivity(intent)
            ready = false
            coordinates = null
        } catch (e: Exception) {
            error = R.string.sos_composer_error
        }
    }

    val summary = listOfNotNull(offer.originZone, offer.destinationZone)
        .joinToString(" → ")
        .ifBlank { context.getString(R.string.sos_trip_details_unavailable) }

    SosScreen(
        back = back,
        message = { error = it },
        onSosActivated = ::activateSos,
        onPrepareMessage = ::activateSos,
        onCallEmergency = {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, "tel:999".toUri())
                )
            } catch (_: Exception) {
                error = R.string.sos_dialer_error
            }
        },
        tripSummary = summary,
        contactName = emergencyContact?.name?.ifBlank { null },
        footer = {
            if (preparing) {
                CircularProgressIndicator()
                Text(stringResource(R.string.sos_preparing_location))
            }
            error?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error)
            }
        }
    )

    if (ready) {
        AlertDialog(
            onDismissRequest = {
                ready = false
                coordinates = null
            },
            title = {
                Text(stringResource(R.string.sos_prepare_message))
            },
            text = {
                Text(stringResource(R.string.sos_message_explanation))
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ready = false
                        coordinates = null
                    }
                ) {
                    Text(stringResource(R.string.sos_cancel))
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = { openComposer(false) }
                    ) {
                        Text(stringResource(R.string.sos_send_sms))
                    }

                    TextButton(
                        onClick = { openComposer(true) }
                    ) {
                        Text(stringResource(R.string.sos_send_whatsapp))
                    }
                }
            }
        )
    }
}