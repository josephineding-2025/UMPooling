package com.fopgang.umpooling.home

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.*
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

data class RidePreview(val initials: String, val route: String, val details: String, val price: String,
    val offerId: String = com.fopgang.umpooling.booking.DemoRideIds.HOME_OFFER)

@Composable
fun sampleRide() = RidePreview("FH", stringResource(R.string.sample_route), stringResource(R.string.sample_ride_meta), stringResource(R.string.sample_price))

private val HomeInk = Color(0xFF212121)
private val HomeWhite = Color(0xFFFFFBF9)
private val HomeText = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 19.sp, color = HomeInk)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, onUnavailable: () -> Unit, modifier: Modifier = Modifier,
    onSearch: (com.fopgang.umpooling.booking.FindRideCriteria) -> Unit = { onNavigate("find_ride") }) {
    val campus = stringResource(R.string.um_campus)
    val mall = stringResource(R.string.mid_valley)
    var origin by rememberSaveable { mutableStateOf(campus) }
    var destination by rememberSaveable { mutableStateOf(mall) }
    var seats by rememberSaveable { mutableIntStateOf(1) }
    var date by rememberSaveable { mutableStateOf("") }
    var selectedDay by rememberSaveable { mutableLongStateOf(java.time.LocalDate.now(com.fopgang.umpooling.booking.CAMPUS_ZONE).plusDays(1).toEpochDay()) }
    val context = LocalContext.current
    val ride = sampleRide()
    CompositionLocalProvider(LocalTextStyle provides HomeText, LocalMinimumInteractiveComponentSize provides 0.dp) {
        Box(modifier.fillMaxSize().background(Color(0xFFFCEAE2)).drawWithCache {
            val white = Brush.radialGradient(listOf(Color.White, Color.Transparent), Offset.Zero, size.width * .9f)
            val coral = Brush.radialGradient(listOf(Color(0xFFFF9290), Color.Transparent),
                Offset(size.width * .65f, size.height * .37f), size.width * .8f)
            val bottom = Brush.radialGradient(listOf(Color(0xFFFCD5C9), Color.Transparent),
                Offset(0f, size.height), size.width * .65f)
            onDrawBehind { drawRect(white); drawRect(coral); drawRect(bottom) }
        }) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 17.dp).padding(top = 55.dp, bottom = 100.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Surface(onClick = { onNavigate("saved") }, shape = CircleShape, color = HomeWhite.copy(alpha = .88f)) {
                        Row(Modifier.width(218.dp).heightIn(min = 46.dp).padding(horizontal = 17.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FigmaIcon(R.drawable.home_frame9, Modifier.size(19.dp))
                            Text(campus, Modifier.weight(1f), fontSize = 16.sp, lineHeight = 19.sp)
                            FigmaIcon(R.drawable.home_frame10, Modifier.size(13.dp))
                        }
                    }
                    Surface(onClick = { onNavigate("profile") }, shape = CircleShape, color = Color.Transparent,
                        border = BorderStroke(2.dp, Color.White)) {
                        Box(Modifier.size(48.dp).background(
                            Brush.linearGradient(listOf(Color(0xFFC79C81), Color(0xFF976449))), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text("NS", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(stringResource(R.string.sample_date), Modifier.padding(start = 3.dp),
                    fontSize = 15.sp, lineHeight = 19.sp, color = Color(0xFF3D3D3D))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.good_morning), Modifier.padding(start = 3.dp),
                    fontSize = 36.sp, lineHeight = 35.sp, letterSpacing = (-.3).sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.sample_name), Modifier.padding(start = 3.dp),
                    fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(17.dp))
                Surface(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
                    color = Color.White.copy(alpha = .38f), border = BorderStroke(1.dp, Color.White.copy(alpha = .9f))
                ) {
                    Column(Modifier.padding(horizontal = 18.dp).padding(top = 21.dp, bottom = 17.dp)) {
                        Text(stringResource(R.string.where_going), fontSize = 23.sp, lineHeight = 29.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp)
                        Spacer(Modifier.height(9.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                RouteInput(stringResource(R.string.from), origin, { origin = it }, campus, R.drawable.home_frame)
                                RouteInput(stringResource(R.string.to), destination, { destination = it }, mall, R.drawable.home_frame1)
                            }
                            IconButton({ val from = origin; origin = destination; destination = from }, Modifier.width(44.dp)) {
                                FigmaIcon(R.drawable.home_swap_locations, Modifier.size(22.dp), stringResource(R.string.swap_locations))
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmallField(Modifier.weight(1.05f), R.drawable.home_frame2,
                                date.ifEmpty { java.time.LocalDate.ofEpochDay(selectedDay).format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())) }) {
                                val today = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    val chosen = Calendar.getInstance().apply { set(y, m, d) }
                                    date = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(chosen.time)
                                    selectedDay = java.time.LocalDate.of(y,m+1,d).toEpochDay()
                                }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show()
                            }
                            SmallField(Modifier.weight(1f), R.drawable.home_frame4,
                                stringResource(R.string.seat_count, seats).replace("(s)", if (seats == 1) "" else "s")) {
                                seats = seats % 3 + 1
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Button({ onSearch(com.fopgang.umpooling.booking.FindRideCriteria(
                            origin=com.fopgang.umpooling.booking.RideLocation(origin), destination=com.fopgang.umpooling.booking.RideLocation(destination),
                            departureDate=java.time.LocalDate.ofEpochDay(selectedDay), seats=seats)) }, Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            FigmaIcon(R.drawable.home_frame5, Modifier.size(19.dp))
                            Spacer(Modifier.width(13.dp))
                            Text(stringResource(R.string.find_carpool), fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(17.dp))
                SectionTitle(stringResource(R.string.quick_actions), stringResource(R.string.see_all), { onNavigate("saved") })
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    QuickAction(stringResource(R.string.offer_ride), R.drawable.home_frame6, Modifier.weight(1f)) { onNavigate("offer_ride") }
                    Surface(onClick = { onNavigate("dashboard") }, Modifier.weight(1f).height(62.dp),
                        shape = RoundedCornerShape(19.dp), color = HomeWhite.copy(alpha = .9f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Box(Modifier.size(26.dp, 22.dp)) {
                                Image(painterResource(R.drawable.home_group160), null, Modifier.align(Alignment.Center).size(18.dp, 17.dp))
                                Image(painterResource(R.drawable.home_vector), null, Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 3.dp).size(12.dp, 11.dp))
                            }
                            Text(stringResource(R.string.carbon_footprint), fontSize = 13.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
                        }
                    }
                    QuickAction(stringResource(R.string.my_trips), R.drawable.home_frame7, Modifier.weight(1f)) { onNavigate("ticket") }
                    QuickAction(stringResource(R.string.saved), R.drawable.home_frame8, Modifier.weight(1f)) { onNavigate("saved") }
                }
                Spacer(Modifier.height(22.dp))
                SectionTitle(stringResource(R.string.available_rides), stringResource(R.string.nearby))
                Spacer(Modifier.height(10.dp))
                RideCard(ride) { onNavigate(com.fopgang.umpooling.booking.BookingRoute.OfferDetails(ride.offerId).encode()) }
            }
            CommuterNavigation("home", onNavigate,
                Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp).padding(bottom = 30.dp))
        }
    }
}

@Composable
private fun RouteInput(label: String, value: String, onChange: (String) -> Unit, hint: String, icon: Int) {
    Surface(shape = RoundedCornerShape(16.dp), color = HomeWhite.copy(alpha = .88f)) {
        Row(Modifier.heightIn(min = 44.dp).padding(horizontal = 15.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            FigmaIcon(icon, Modifier.size(14.dp))
            Text(label, fontSize = 14.sp, color = Color(0xFF606060))
            BasicTextField(value, onChange, Modifier.weight(1f).semantics { contentDescription = label },
                singleLine = true, textStyle = HomeText,
                decorationBox = { inner -> if (value.isEmpty()) Text(hint); inner() })
        }
    }
}

@Composable
private fun SmallField(modifier: Modifier, icon: Int, label: String, action: () -> Unit) {
    Surface(onClick = action, modifier.heightIn(min = 42.dp), shape = RoundedCornerShape(18.dp), color = HomeWhite.copy(alpha = .88f)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FigmaIcon(icon, Modifier.size(18.dp))
            Text(label, Modifier.weight(1f), fontSize = 15.sp, lineHeight = 18.sp,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            FigmaIcon(R.drawable.home_frame3, Modifier.size(11.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, more: String, action: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp).heightIn(min = 25.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 21.sp, lineHeight = 25.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
        Text(more, if (action != null) Modifier.clickable(onClick = action) else Modifier,
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9D8C85))
    }
}

@Composable
private fun QuickAction(label: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick, modifier.height(62.dp), shape = RoundedCornerShape(19.dp), color = HomeWhite.copy(alpha = .9f)) {
        Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically)) {
            FigmaIcon(icon, Modifier.size(20.dp, 19.dp))
            Text(label, fontSize = 13.sp, lineHeight = 15.sp, textAlign = TextAlign.Center, letterSpacing = (-.4).sp)
        }
    }
}

@Composable
fun RideCard(ride: RidePreview, onClick: () -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides HomeText, LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
            color = HomeWhite.copy(alpha = .8f), border = BorderStroke(1.dp, Color.White),
            shadowElevation = 2.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(46.dp).background(Color(0xFFFDE1D4), CircleShape), contentAlignment = Alignment.Center) {
                    Text(ride.initials, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF40312B))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(ride.route, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
                    Text(ride.details, fontSize = 13.sp, lineHeight = 15.sp, color = Color(0xFF74635C))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(ride.price, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Surface(shape = CircleShape, color = Color(0xFFFFE2D3)) {
                        Text(stringResource(R.string.available), Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF85412B))
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "Compact", widthDp = 320, heightDp = 720)
@Composable
private fun HomePreview() {
    UMPoolingTheme { PeachBackground { HomeScreen({}, {}) } }
}
