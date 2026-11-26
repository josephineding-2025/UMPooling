package com.fopgang.umpooling.matching

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fopgang.umpooling.R
import com.fopgang.umpooling.SupabaseClientProvider
import com.fopgang.umpooling.data.CampusLocation
import com.fopgang.umpooling.data.CampusLocationProvider
import com.fopgang.umpooling.data.Haversine
import com.fopgang.umpooling.data.NewSavedRoute
import com.fopgang.umpooling.data.SavedRoute
import com.fopgang.umpooling.data.repository.RideRepository
import com.fopgang.umpooling.ui.components.FigmaIcon
import com.fopgang.umpooling.ui.matching.LocationSelectorField
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.util.Locale

private val SavedInk = Color(0xFF25211F)
private val SavedMuted = Color(0xFF74635D)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedRoutesScreen(
    back: () -> Unit = {},
    unavailable: () -> Unit = {},
    onNavigate: (String) -> Unit = { unavailable() },
    initialRoutes: List<String> = emptyList(),
    onRoutesChanged: (List<String>) -> Unit = {},
    showSamples: Boolean = true,
    repository: RideRepository = remember { RideRepository() }
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var originLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var destinationLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var preferredTime by remember { mutableStateOf("08:30") }

    var savedRoutes by remember { mutableStateOf<List<SavedRoute>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var savedNames by rememberSaveable { mutableStateOf(ArrayList(initialRoutes)) }

    val coroutineScope = rememberCoroutineScope()

    fun refreshRoutes() {
        val currentUserId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return
        coroutineScope.launch {
            isLoading = true
            repository.getSavedRoutes(currentUserId).fold(
                onSuccess = { routes ->
                    savedRoutes = routes
                    isLoading = false
                },
                onFailure = {
                    isLoading = false
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshRoutes()
    }

    CompositionLocalProvider(
        LocalTextStyle provides TextStyle(fontFamily = FontFamily.SansSerif, color = SavedInk, fontSize = 15.sp),
        LocalMinimumInteractiveComponentSize provides 0.dp
    ) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFFFCEBE3)).drawWithCache {
                val light = Brush.radialGradient(listOf(Color.White, Color.Transparent), Offset.Zero, size.width * .8f)
                val peach = Brush.radialGradient(listOf(Color(0xFFF7C5B8), Color.Transparent),
                    Offset(size.width * .9f, size.height * .16f), size.width * .75f)
                val bottom = Brush.radialGradient(listOf(Color(0xFFFBD2C4), Color.Transparent),
                    Offset(0f, size.height), size.width * .65f)
                onDrawBehind { drawRect(light); drawRect(peach); drawRect(bottom) }
            }.verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 52.dp, bottom = 32.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape, color = Color.White.copy(alpha = .85f),
                    border = BorderStroke(1.dp, Color(0xFFF9D8CE)), shadowElevation = 3.dp
                ) {
                    IconButton(back, Modifier.size(46.dp)) {
                        FigmaIcon(R.drawable.saved_frame6, Modifier.size(19.dp), stringResource(R.string.back))
                    }
                }
                Text(stringResource(R.string.saved_routes), Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
                Surface(shape = CircleShape,
                    color = Color(0xFFF39C80).copy(alpha = .85f),
                    border = BorderStroke(.75.dp, Color(0xFFFFD2BE))) {
                    IconButton({ adding = true }, Modifier.size(44.dp)) {
                        SavedPlus(SavedInk, Modifier.size(20.dp)
                            .semantics { contentDescription = "Add route" })
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                val routes = listOf(
                    Triple(R.string.saved_route1, R.string.saved_meta1, R.drawable.saved_frame),
                    Triple(R.string.saved_route2, R.string.saved_meta2, R.drawable.saved_frame1),
                    Triple(R.string.saved_route3, R.string.saved_meta3, R.drawable.saved_frame2)
                )
                if (showSamples) routes.forEachIndexed { index, (title, subtitle, icon) ->
                    SavedRouteRow(
                        stringResource(title), stringResource(subtitle), icon,
                        if (index == 1) stringResource(R.string.offer_ride).replace("a Ride", "ride")
                        else stringResource(R.string.find_ride), { onNavigate(if (index == 1) "offer_ride" else "find_ride") }
                    )
                }

                // Render saved routes from Supabase backend
                savedRoutes.forEach { route ->
                    val originLoc = route.originZone?.let { CampusLocationProvider.findById(it) }
                    val destLoc = route.destinationZone?.let { CampusLocationProvider.findById(it) }
                    val title = "${originLoc?.name ?: route.originZone ?: "Origin"} → ${destLoc?.name ?: route.destinationZone ?: "Destination"}"
                    val subtitle = "Time: ${route.preferredTime ?: "Anytime"} • ${route.distanceKm?.let { String.format(Locale.US, "%.1f km", it) } ?: "UM Campus"}"
                    SavedRouteRow(
                        title, subtitle, R.drawable.saved_frame1,
                        stringResource(R.string.find_ride), { onNavigate("find_ride") }
                    )
                    TextButton(onClick = {
                        val rId = route.routeId ?: return@TextButton
                        coroutineScope.launch {
                            repository.deleteSavedRoute(rId).fold(
                                onSuccess = { refreshRoutes() },
                                onFailure = {}
                            )
                        }
                    }) {
                        Text("Delete route")
                    }
                }

                savedNames.forEach { route ->
                    SavedRouteRow(
                        route.substringBefore("|"), route.substringAfter("|"), R.drawable.saved_frame1,
                        stringResource(R.string.find_ride), { onNavigate("find_ride") }
                    )
                    TextButton({ savedNames = ArrayList(savedNames - route); onRoutesChanged(savedNames) }) { Text("Delete route") }
                }
            }
            Spacer(Modifier.height(17.dp))
            Text(stringResource(R.string.saved_places), Modifier.padding(start = 4.dp),
                fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
            Spacer(Modifier.height(9.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(R.string.kk11, R.string.fsktm, R.string.library, R.string.kl_sentral).forEachIndexed { index, place ->
                    Surface(
                        onClick = unavailable, shape = CircleShape,
                        color = Color.White.copy(alpha = .8f), border = BorderStroke(1.dp, Color.White),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            Modifier.heightIn(min = 38.dp).padding(horizontal = 15.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painterResource(if (index == 0) R.drawable.saved_frame else R.drawable.saved_frame4),
                                null, Modifier.size(12.dp),
                                colorFilter = ColorFilter.tint(Color(0xFFCF482B))
                            )
                            Text(stringResource(place), fontSize = 14.sp, lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold, letterSpacing = (-.3).sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val addLabel = stringResource(R.string.save_new_route)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = .4f))
                    .drawWithCache {
                        val stroke = Stroke(width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
                        onDrawBehind {
                            val inset = stroke.width / 2
                            drawRoundRect(Color(0xFFF0AE97), Offset(inset, inset),
                                Size(size.width - stroke.width, size.height - stroke.width),
                                CornerRadius(size.height / 2), style = stroke)
                        }
                    }.clickable { adding = true }.padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                SavedPlus(Color(0xFF9C3924), Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(addLabel, fontSize = 16.sp, lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF9C3924))
            }
        }
    }

    if (adding) {
        Dialog(onDismissRequest = { adding = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFFFF7F5),
                border = BorderStroke(1.dp, Color(0xFFF9D8CE)),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Save a daily route",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SavedInk
                    )
                    Text(
                        text = "Save your frequent commuting path for fast offer posting and automated driver matching",
                        fontSize = 13.sp,
                        color = SavedMuted,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "From (home, college or gate)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SavedMuted
                    )
                    LocationSelectorField(
                        label = "Select origin",
                        selectedLocation = originLocation,
                        onLocationSelected = { originLocation = it }
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "To (faculty or campus zone)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SavedMuted
                    )
                    LocationSelectorField(
                        label = "Select destination",
                        selectedLocation = destinationLocation,
                        onLocationSelected = { destinationLocation = it }
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Preferred Time (HH:MM)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SavedMuted
                    )
                    OutlinedTextField(
                        value = preferredTime,
                        onValueChange = { preferredTime = it },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFF2CDBF),
                            focusedBorderColor = Color(0xFF9C3924)
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val orig = originLocation
                            val dest = destinationLocation
                            if (orig != null && dest != null) {
                                val distanceKm = if (orig.lat != null && orig.lng != null && dest.lat != null && dest.lng != null) {
                                    Haversine.estimateRoadDistanceKm(orig.lat, orig.lng, dest.lat, dest.lng)
                                } else 0.0

                                val routeTitle = "${orig.name} → ${dest.name}"
                                val routeSub = "Time: $preferredTime • ${String.format(Locale.US, "%.1f km", distanceKm)}"
                                savedNames = ArrayList(savedNames + "$routeTitle|$routeSub")
                                onRoutesChanged(savedNames)

                                val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                                if (user != null && orig.ewkt != null && dest.ewkt != null) {
                                    isSaving = true
                                    coroutineScope.launch {
                                        val newRoute = NewSavedRoute(
                                            userId = user.id,
                                            originGeo = orig.ewkt!!,
                                            destinationGeo = dest.ewkt!!,
                                            originZone = orig.id,
                                            destinationZone = dest.id,
                                            distanceKm = distanceKm,
                                            preferredTime = preferredTime
                                        )
                                        repository.createSavedRoute(newRoute).fold(
                                            onSuccess = {
                                                isSaving = false
                                                adding = false
                                                originLocation = null
                                                destinationLocation = null
                                                refreshRoutes()
                                            },
                                            onFailure = {
                                                isSaving = false
                                                adding = false
                                            }
                                        )
                                    }
                                } else {
                                    adding = false
                                    originLocation = null
                                    destinationLocation = null
                                }
                            }
                        },
                        enabled = !isSaving && originLocation != null && destinationLocation != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "+ Save Route",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRouteRow(title: String, subtitle: String, icon: Int, action: String, onAction: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = .82f), border = BorderStroke(1.dp, Color.White),
        shadowElevation = 9.dp
    ) {
        Row(
            Modifier.heightIn(min = 75.dp).padding(horizontal = 17.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).background(
                    if (icon == R.drawable.saved_frame) Color(0xFF2B2623) else Color(0xFFFFE1D3),
                    RoundedCornerShape(13.dp)
                ), contentAlignment = Alignment.Center
            ) { FigmaIcon(icon, Modifier.size(18.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 16.sp, lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-.45).sp)
                Text(subtitle, fontSize = 13.sp, lineHeight = 16.sp, color = SavedMuted, letterSpacing = (-.25).sp)
            }
            Button(
                onAction, Modifier.heightIn(min = 34.dp), shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text(action, fontFamily = FontFamily.SansSerif, color = Color.White,
                    fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SavedPlus(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val width = 1.8.dp.toPx()
        drawLine(color, Offset(size.width * .2f, size.height / 2), Offset(size.width * .8f, size.height / 2),
            width, StrokeCap.Round)
        drawLine(color, Offset(size.width / 2, size.height * .2f), Offset(size.width / 2, size.height * .8f),
            width, StrokeCap.Round)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SavedPreview() { UMPoolingTheme { SavedRoutesScreen({}, {}) } }
