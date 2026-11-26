package com.fopgang.umpooling.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.*

@Composable
fun ProfileScreen(navigate: (String) -> Unit, unavailable: () -> Unit, message: (Int) -> Unit,
    notifications: Boolean = true, onNotificationsChange: (Boolean) -> Unit = {}, onLogout: () -> Unit,
    name: String? = null, role: String? = null) {
    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = FontFamily.SansSerif, color = Ink),
        LocalMinimumInteractiveComponentSize provides 0.dp) {
    Box(Modifier.fillMaxSize().background(Color(0xFFFCEAE2))) {
        Box(Modifier.fillMaxSize().drawWithCache {
            val base = Brush.verticalGradient(0f to Color(0xFFFF9E9B), .28f to Color(0xFFFF8D89), .43f to Color.Transparent, 1f to Color.Transparent)
            val glow = Brush.radialGradient(listOf(Color(0xFFDA6952), Color.Transparent),
                Offset(size.width * .87f, 0f), size.width * .58f)
            val lower = Brush.radialGradient(listOf(Color(0xFFCD654E), Color.Transparent),
                Offset(0f, size.height * .27f), size.width * .5f)
            onDrawBehind { drawRect(base); drawRect(glow); drawRect(lower) }
        })
        PageColumn(Modifier.padding(top = 25.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                Surface(shape = CircleShape, color = Apricot, border = androidx.compose.foundation.BorderStroke(3.dp, Color.White.copy(alpha = .8f)), shadowElevation = 10.dp) {
                    Box(Modifier.size(94.dp), contentAlignment = Alignment.Center) { Text(initials(name), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF321C13)) }
                }
                Surface(Modifier.align(Alignment.BottomEnd), shape = CircleShape, color = Color(0xFFF8C4B0), border = androidx.compose.foundation.BorderStroke(2.dp, Ink)) {
                    FigmaIcon(R.drawable.profile_frame7, Modifier.padding(6.dp).size(14.dp))
                }
                }
                Text(name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_name_unset), fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .6f))) {
                        Text(stringResource(R.string.verified_student), Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            color = Color.White, fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Surface(onClick = { navigate("tier") }, color = Color(0xFFF9CAB3), shape = CircleShape) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FigmaIcon(R.drawable.profile_frame8, Modifier.size(14.dp))
                            Text(stringResource(R.string.tier_level), fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF463027))
                        }
                    }
                }
                Text(stringResource(roleLabel(role)), Modifier.fillMaxWidth(.76f).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFCEAE2), Color(0xFFFCEAE2), Color.Transparent)),
                    CircleShape).padding(vertical = 7.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            GlassCard(Modifier.fillMaxWidth(), opacity = .9f) {
                Row(Modifier.padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStat(R.string.sample_stat_rides, R.string.rides, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(34.dp), color = Color(0xFFF4CBBB))
                    ProfileStat(R.string.sample_stat_rating, R.string.rating, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(34.dp), color = Color(0xFFF4CBBB))
                    ProfileStat(R.string.sample_stat_co2, R.string.co2_saved, Modifier.weight(1f))
                }
            }
            GlassCard(Modifier.fillMaxWidth(), opacity = .85f, radius = 30.dp) {
                ProfileRow(R.drawable.profile_frame, R.string.personal_details) { navigate("profile_setup") }
                ProfileRow(R.drawable.profile_frame2, R.string.my_vehicle) { navigate("profile_setup") }
                ProfileRow(R.drawable.profile_frame3, R.string.trusted_contacts) { navigate("emergency_contacts") }
                ProfileRow(R.drawable.profile_frame4, R.string.ride_history) { navigate("ticket") }
            }
            GlassCard(Modifier.fillMaxWidth().offset(y = (-12).dp), opacity = .85f, radius = 30.dp) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFFFE2D4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)) {
                        FigmaIcon(R.drawable.profile_frame5, Modifier.padding(10.dp).size(18.dp))
                    }
                    Text(stringResource(R.string.notifications), Modifier.weight(1f).clickable { navigate("notification_settings") }.padding(12.dp),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Switch(notifications, onNotificationsChange, colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF3D302B), checkedThumbColor = Color(0xFFFFF9F5),
                        checkedBorderColor = Color.Transparent))
                }
                ProfileRow(R.drawable.profile_frame6, R.string.logout, showArrow = false, click = onLogout)
            }
            Spacer(Modifier.height(80.dp))
        }
        CommuterNavigation("profile", navigate, Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp).padding(bottom = 30.dp))
    }
    }
}

internal fun initials(name: String?): String =
    name.orEmpty().trim().split(' ').filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "UM" }

internal fun roleLabel(role: String?): Int = when (role) {
    "drivers" -> R.string.role_drivers
    "both" -> R.string.role_both
    "staffAnalyst" -> R.string.role_staff
    else -> R.string.role_passengers
}

@Composable
private fun ProfileStat(value: Int, label: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(value), fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(stringResource(label), fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = Muted)
    }
}

@Composable
private fun ProfileRow(icon: Int, label: Int, showArrow: Boolean = true, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = click).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFFFE0D2),
            border = if (label == R.string.trusted_contacts || label == R.string.logout) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White)) {
            FigmaIcon(icon, Modifier.padding(10.dp).size(18.dp))
        }
        Text(stringResource(label), Modifier.weight(1f), fontSize = 16.sp, lineHeight = 20.sp,
            fontWeight = FontWeight.Bold, color = if (label == R.string.logout) Color(0xFFA83E2A) else Ink)
        if (showArrow) FigmaIcon(R.drawable.profile_frame1, Modifier.size(18.dp))
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ProfilePreview() { UMPoolingTheme { PeachBackground { ProfileScreen({}, {}, {}, onLogout = {}) } } }
