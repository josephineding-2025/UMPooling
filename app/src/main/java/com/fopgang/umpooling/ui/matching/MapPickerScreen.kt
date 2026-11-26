package com.fopgang.umpooling.ui.matching

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.Haversine
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import kotlin.math.roundToInt

private const val UM_CENTER_LAT = 3.1209
private const val UM_CENTER_LNG = 101.6538

/**
 * Real-world Interactive Map Picker powered by OpenStreetMap (OSM) & Leaflet.
 * 100% Free, requires zero API keys, renders real street maps, campus buildings,
 * and allows dropping custom pins for commuter pickup points.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLocation: CampusLocation? = null,
    onLocationConfirmed: (CampusLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val isPreview = LocalInspectionMode.current
    var selectedLat by remember { mutableDoubleStateOf(initialLocation?.lat ?: UM_CENTER_LAT) }
    var selectedLng by remember { mutableDoubleStateOf(initialLocation?.lng ?: UM_CENTER_LNG) }
    var selectedName by remember { mutableStateOf(initialLocation?.name ?: "UM Campus Center") }
    var selectedCategory by remember { mutableStateOf(initialLocation?.category ?: "Campus Landmark") }
    var isCustomPin by remember { mutableStateOf(initialLocation?.id?.startsWith("custom_") == true) }
    var isMapLoading by remember { mutableStateOf(true) }

    // Find nearest landmark to currently selected pin
    val nearestLandmark = remember(selectedLat, selectedLng) {
        CampusLocationProvider.allLocations
            .filter { it.lat != null && it.lng != null }
            .minByOrNull { Haversine.haversineKm(selectedLat, selectedLng, it.lat!!, it.lng!!) }
    }

    val distanceToNearestKm = remember(nearestLandmark, selectedLat, selectedLng) {
        nearestLandmark?.let {
            if (it.lat != null && it.lng != null) {
                Haversine.haversineKm(selectedLat, selectedLng, it.lat, it.lng)
            } else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pick Location on Map", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "OpenStreetMap • Tap landmark or drop custom pin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDEBE3)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F3EF))
        ) {
            if (isPreview) {
                // Static visual mock for Android Studio Compose Preview
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color(0xFFE8ECE9))
                    drawCircle(color = Color(0xFFF08F76), radius = 18f, center = Offset(size.width / 2f, size.height / 2f))
                }
            } else {
                // Real Live OpenStreetMap WebView via Leaflet Engine
                val initLat = initialLocation?.lat ?: UM_CENTER_LAT
                val initLng = initialLocation?.lng ?: UM_CENTER_LNG

                // Serialize verified locations into lightweight JSON for Leaflet
                val locationsJson = remember {
                    CampusLocationProvider.allLocations
                        .filter { it.lat != null && it.lng != null }
                        .joinToString(prefix = "[", postfix = "]") { loc ->
                            val cleanName = loc.name.replace("\"", "\\\"").replace("'", "\\'")
                            val cleanCat = loc.category.replace("\"", "\\\"").replace("'", "\\'")
                            """{"id":"${loc.id}","name":"$cleanName","category":"$cleanCat","lat":${loc.lat},"lng":${loc.lng}}"""
                        }
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true

                            // JavaScript bridge to receive pin click events from Leaflet
                            addJavascriptInterface(object : Any() {
                                @JavascriptInterface
                                fun onLocationSelected(id: String, name: String, category: String, lat: Double, lng: Double) {
                                    post {
                                        selectedLat = lat
                                        selectedLng = lng
                                        selectedName = name
                                        selectedCategory = category
                                        isCustomPin = false
                                    }
                                }

                                @JavascriptInterface
                                fun onCustomPinDropped(lat: Double, lng: Double) {
                                    post {
                                        selectedLat = lat
                                        selectedLng = lng
                                        selectedName = "Custom Pin (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
                                        selectedCategory = "Custom Location"
                                        isCustomPin = true
                                    }
                                }
                            }, "AndroidMapBridge")

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isMapLoading = false
                                }
                            }
                            webChromeClient = WebChromeClient()

                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                    <style>
                                        html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #eae6df; }
                                        .leaflet-control-attribution { font-size: 9px; opacity: 0.7; }
                                    </style>
                                </head>
                                <body>
                                    <div id="map"></div>
                                    <script>
                                        var map = L.map('map', { zoomControl: true }).setView([$initLat, $initLng], 14.5);
                                        
                                        // Standard High-Resolution OpenStreetMap Tiles (100% Free)
                                        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                            maxZoom: 19,
                                            attribution: '© OpenStreetMap contributors'
                                        }).addTo(map);

                                        var locations = $locationsJson;
                                        var customMarker = null;

                                        // Render verified campus locations as color-coded circular pins
                                        locations.forEach(function(loc) {
                                            var color = '#1976D2';
                                            if (loc.id.indexOf('kk') === 0) color = '#2E7D32';
                                            else if (loc.category.indexOf('Off-Campus') !== -1) color = '#F08F76';
                                            else if (loc.category.indexOf('Gates') !== -1) color = '#7B1FA2';

                                            var marker = L.circleMarker([loc.lat, loc.lng], {
                                                radius: 8,
                                                fillColor: color,
                                                color: '#ffffff',
                                                weight: 2,
                                                opacity: 1,
                                                fillOpacity: 0.95
                                            }).addTo(map);

                                            marker.bindPopup('<b>' + loc.name + '</b><br><small>' + loc.category + '</small>');
                                            marker.bindTooltip(loc.name, { direction: 'top', offset: [0, -8] });
                                            marker.on('click', function() {
                                                if (window.AndroidMapBridge) {
                                                    window.AndroidMapBridge.onLocationSelected(loc.id, loc.name, loc.category, loc.lat, loc.lng);
                                                }
                                            });
                                        });

                                        // Tap anywhere on the real street map to drop a draggable pin
                                        map.on('click', function(e) {
                                            var lat = e.latlng.lat;
                                            var lng = e.latlng.lng;

                                            if (customMarker) {
                                                customMarker.setLatLng(e.latlng);
                                            } else {
                                                customMarker = L.marker(e.latlng, { draggable: true }).addTo(map);
                                                customMarker.on('dragend', function(evt) {
                                                    var pos = evt.target.getLatLng();
                                                    if (window.AndroidMapBridge) {
                                                        window.AndroidMapBridge.onCustomPinDropped(pos.lat, pos.lng);
                                                    }
                                                });
                                            }

                                            if (window.AndroidMapBridge) {
                                                window.AndroidMapBridge.onCustomPinDropped(lat, lng);
                                            }
                                        });
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()

                            loadDataWithBaseURL("https://tile.openstreetmap.org", html, "text/html", "UTF-8", null)
                        }
                    }
                )
            }

            if (isMapLoading && !isPreview) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFF08F76)
                )
            }

            // Bottom Confirmation Floating Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFDE3D7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFF8A3524)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$selectedCategory • ${String.format("%.4f", selectedLat)}, ${String.format("%.4f", selectedLng)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B5C57)
                            )
                        }
                    }

                    if (isCustomPin && nearestLandmark != null && distanceToNearestKm != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val distanceMeters = (distanceToNearestKm * 1000).roundToInt()
                        Text(
                            text = "📍 ~$distanceMeters m from ${nearestLandmark.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val locationId = if (isCustomPin) {
                                "custom_${System.currentTimeMillis()}"
                            } else {
                                CampusLocationProvider.allLocations.firstOrNull {
                                    it.lat == selectedLat && it.lng == selectedLng
                                }?.id ?: "custom_pin"
                            }

                            val confirmedLocation = CampusLocation(
                                id = locationId,
                                name = selectedName,
                                category = selectedCategory,
                                lat = selectedLat,
                                lng = selectedLng,
                                pickupName = selectedName,
                                pickupLat = selectedLat,
                                pickupLng = selectedLng
                            )
                            onLocationConfirmed(confirmedLocation)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF08F76)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Location", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog wrapper for MapPickerScreen for seamless modal invocation.
 */
@Composable
fun MapPickerDialog(
    initialLocation: CampusLocation? = null,
    onLocationConfirmed: (CampusLocation) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            MapPickerScreen(
                initialLocation = initialLocation,
                onLocationConfirmed = { loc ->
                    onLocationConfirmed(loc)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPickerScreenPreview() {
    UMPoolingTheme {
        MapPickerScreen(
            onLocationConfirmed = {},
            onDismiss = {}
        )
    }
}
