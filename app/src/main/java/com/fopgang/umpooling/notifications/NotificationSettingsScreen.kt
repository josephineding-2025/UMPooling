package com.fopgang.umpooling.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.*

@Immutable
data class NotificationPreferences(
    val push: Boolean = true,
    val requests: Boolean = true,
    val confirmations: Boolean = true,
    val grouping: Boolean = true,
    val reminders: Boolean = true,
    val chat: Boolean = true,
    val reviews: Boolean = true,
    val quiet: Boolean = false,
    val reminderMinutes: Int = 30
)

val NotificationPreferencesSaver = listSaver<NotificationPreferences, Int>(
    save = { p -> listOf(p.push, p.requests, p.confirmations, p.grouping, p.reminders, p.chat, p.reviews, p.quiet).map { if (it) 1 else 0 } + p.reminderMinutes },
    restore = { v -> NotificationPreferences(v[0] == 1, v[1] == 1, v[2] == 1, v[3] == 1, v[4] == 1, v[5] == 1, v[6] == 1, v[7] == 1, v[8]) }
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val SettingsFont = FontFamily(*listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map {
    Font(R.font.plus_jakarta_sans, weight = it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
}.toTypedArray())

@Composable
fun NotificationSettingsScreen(preferences: NotificationPreferences, change: (NotificationPreferences) -> Unit, back: () -> Unit) {
    val base = MaterialTheme.typography
    MaterialTheme(typography = base.copy(
        titleSmall = base.titleSmall.copy(fontFamily = SettingsFont, fontSize = 15.sp, fontWeight = FontWeight.Bold),
        bodyMedium = base.bodyMedium.copy(fontFamily = SettingsFont, fontSize = 13.sp),
        labelLarge = base.labelLarge.copy(fontFamily = SettingsFont, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    )) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFDEBE3)).drawWithCache {
        val top = Brush.radialGradient(listOf(Color(0xFFF9CCBE), Color.Transparent), Offset(size.width, size.height * .12f), size.width * .8f)
        val bottom = Brush.radialGradient(listOf(Color(0xFFF9D1C3), Color.Transparent), Offset.Zero.copy(y = size.height), size.width * .7f)
        onDrawBehind { drawRect(top); drawRect(bottom) }
    }.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 52.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = .8f), border = BorderStroke(1.dp, Color(0xFFF5D6C9))) {
                IconButton(back, Modifier.size(46.dp)) { FeatureIcon(FeatureSymbol.Back, Modifier.size(20.dp), description = stringResource(R.string.back)) }
            }
            Text(stringResource(R.string.four_notification_settings), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontFamily = SettingsFont, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
        SettingsCard(Modifier.fillMaxWidth(), dark = true) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = Color.White.copy(alpha = .2f), shape = MaterialTheme.shapes.small) {
                    FeatureIcon(FeatureSymbol.Bell, Modifier.padding(10.dp), tint = Color.White)
                }
                NotificationToggle(stringResource(R.string.four_push),
                    stringResource(if (preferences.push) R.string.four_push_on else R.string.four_push_off),
                    preferences.push, { change(preferences.copy(push = it)) }, Modifier.weight(1f), dark = true)
            }
        }
        SettingsCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionHeading(R.string.four_rides_groups)
                NotificationToggle(stringResource(R.string.four_requests), stringResource(R.string.four_requests_help),
                    preferences.requests, { change(preferences.copy(requests = it)) }, enabled = preferences.push)
                NotificationToggle(stringResource(R.string.four_confirmations), stringResource(R.string.four_confirmations_help),
                    preferences.confirmations, { change(preferences.copy(confirmations = it)) }, enabled = preferences.push)
                NotificationToggle(stringResource(R.string.four_grouping), stringResource(R.string.four_grouping_help),
                    preferences.grouping, { change(preferences.copy(grouping = it)) }, enabled = preferences.push)
                NotificationToggle(stringResource(R.string.four_reminders), stringResource(R.string.four_reminders_help),
                    preferences.reminders, { change(preferences.copy(reminders = it)) }, enabled = preferences.push)
            }
        }
        SettingsCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.four_remind_before), style = MaterialTheme.typography.titleSmall, color = PoolDesign.Text)
                ReminderSelector(preferences.reminderMinutes, preferences.push && preferences.reminders) { change(preferences.copy(reminderMinutes = it)) }
            }
        }
        SettingsCard(Modifier.fillMaxWidth().heightIn(min = 308.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionHeading(R.string.four_community)
                NotificationToggle(stringResource(R.string.four_chat_messages), stringResource(R.string.four_chat_help),
                    preferences.chat, { change(preferences.copy(chat = it)) }, enabled = preferences.push)
                NotificationToggle(stringResource(R.string.four_reviews), stringResource(R.string.four_reviews_help),
                    preferences.reviews, { change(preferences.copy(reviews = it)) }, enabled = preferences.push)
                NotificationToggle(stringResource(R.string.four_quiet), stringResource(R.string.four_quiet_help),
                    preferences.quiet, { change(preferences.copy(quiet = it)) }, enabled = preferences.push)
            }
        }
        Text(stringResource(R.string.four_settings_demo), style = MaterialTheme.typography.bodySmall, color = PoolDesign.Secondary)
    }
    }
}

@Composable
private fun SettingsSectionHeading(resource: Int) {
    Text(stringResource(resource), style = MaterialTheme.typography.labelLarge, color = PoolDesign.Accent)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSelector(minutes: Int, enabled: Boolean, select: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15 to R.string.four_15min, 30 to R.string.four_30min, 60 to R.string.four_1hour).forEach { (value, label) ->
            val chosen = minutes == value
            Surface(Modifier.weight(1f), shape = CircleShape, color = if (chosen) Color.Black else Color.White,
                contentColor = (if (chosen) Color.White else PoolDesign.Strong).copy(alpha = if (enabled) 1f else .5f),
                shadowElevation = if (enabled) 6.dp else 0.dp) {
                Box(Modifier.fillMaxWidth().heightIn(min = 38.dp)
                    .selectable(chosen, enabled = enabled, role = Role.RadioButton, onClick = { select(value) })
                    .padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview(name = "Settings", widthDp = 390, heightDp = 946)
@Preview(name = "Settings compact large text", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Preview(name = "Settings wide", widthDp = 600, heightDp = 900)
@Composable
private fun SettingsPreview() {
    var preferences by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = NotificationPreferencesSaver) { mutableStateOf(NotificationPreferences()) }
    UMPoolingTheme { PeachBackground { NotificationSettingsScreen(preferences, { preferences = it }, {}) } }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, dark: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(30.dp), color = Color.Transparent,
        border = BorderStroke(1.dp, if (dark) Color(0xFF8E827C) else Color.White), shadowElevation = 5.dp) {
        Column(Modifier.background(if (dark) PoolDesign.DarkGradient else Brush.linearGradient(listOf(
            Color(0xFFFFFCFA).copy(alpha = .95f), Color(0xFFFFF2EC).copy(alpha = .9f)))), content = content)
    }
}

@Composable
private fun NotificationToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, dark: Boolean = false) {
    Row(modifier.fillMaxWidth().heightIn(min = if (dark) 58.dp else 56.dp)
        .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = SettingsFont, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold,
                color = (if (dark) Color.White else PoolDesign.Text).copy(alpha = if (enabled) 1f else .5f))
            Text(subtitle, fontFamily = SettingsFont, fontSize = 13.sp, lineHeight = 16.sp,
                color = (if (dark) Color(0xFFEBC8B9) else PoolDesign.Secondary).copy(alpha = if (enabled) 1f else .5f))
        }
        Switch(checked, null, enabled = enabled, colors = SwitchDefaults.colors(
            checkedTrackColor = Color(0xFF3D302B), checkedThumbColor = Color(0xFFFFF9F5),
            checkedBorderColor = Color.Transparent, uncheckedTrackColor = Color(0xFFE4CEC5),
            uncheckedThumbColor = Color(0xFFFFF9F5), uncheckedBorderColor = Color.Transparent))
    }
}
