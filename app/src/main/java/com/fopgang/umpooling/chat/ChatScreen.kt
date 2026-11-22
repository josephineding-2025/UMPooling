package com.fopgang.umpooling.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(val text: String, val time: String, val outgoing: Boolean = false, val location: Boolean = false)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val ChatFont = FontFamily(*listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map {
    Font(R.font.plus_jakarta_sans, weight = it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
}.toTypedArray())

@Composable
fun ChatScreen(back: () -> Unit, externalAction: () -> Unit, conversationId: String = "demo", liveMessages: List<ChatMessage>? = null, sending: Boolean = false, onSendMessage: ((String) -> Unit)? = null, sendAcknowledgement: Int = 0) {
    var draft by rememberSaveable(conversationId) { mutableStateOf("") }
    var sent by rememberSaveable(conversationId) { mutableStateOf(arrayListOf<String>()) }
    var times by rememberSaveable(conversationId) { mutableStateOf(arrayListOf<String>()) }
    val sample = listOf(
        ChatMessage(stringResource(R.string.four_message1), "07:52"),
        ChatMessage(stringResource(R.string.four_message2), "07:54", outgoing = true),
        ChatMessage(stringResource(R.string.four_message3), "08:06"),
        ChatMessage(stringResource(R.string.four_location_shared), "08:07", location = true),
        ChatMessage(stringResource(R.string.four_message4), "08:10", outgoing = true)
    )
    val messages = liveMessages ?: (sample + sent.mapIndexed { index, text -> ChatMessage(text, times.getOrElse(index) { "" }, outgoing = true) })
    LaunchedEffect(sendAcknowledgement) {if(sendAcknowledgement>0) draft=""}
    val listState = rememberLazyListState()
    val send: () -> Unit = {
        if (draft.isNotBlank() && !sending) {
            if(onSendMessage!=null) onSendMessage(draft.trim())
            else {
            sent = ArrayList(sent + draft.trim())
            times = ArrayList(times + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
            draft = ""
            }
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex + 1)
    }
    val base = MaterialTheme.typography
    MaterialTheme(typography = base.copy(
        titleMedium = base.titleMedium.copy(fontFamily = ChatFont, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
        titleSmall = base.titleSmall.copy(fontFamily = ChatFont, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = ChatFont, fontSize = 14.sp, lineHeight = 19.sp),
        bodySmall = base.bodySmall.copy(fontFamily = ChatFont, fontSize = 12.sp, lineHeight = 16.sp),
        labelMedium = base.labelMedium.copy(fontFamily = ChatFont, fontSize = 13.sp, fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(fontFamily = ChatFont, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )) {
    Column(Modifier.fillMaxSize().imePadding().background(Color(0xFFFCEBE3)).drawWithCache {
        val glow = Brush.radialGradient(listOf(Color(0xFFF7CCBD), Color.Transparent), Offset(size.width, size.height * .23f), size.width * .75f)
        val bottom = Brush.radialGradient(listOf(Color(0xFFF8CEBF), Color.Transparent), Offset(0f, size.height), size.width * .65f)
        onDrawBehind { drawRect(glow); drawRect(bottom) }
    }) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFFF9D9C), Color(0xFFEA7664), Color(0xFFFF8C8D))), RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .padding(horizontal = 20.dp).padding(top = 52.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f), border = BorderStroke(1.dp, Color.White.copy(alpha = .6f))) {
                    IconButton(back, Modifier.size(42.dp)) { FeatureIcon(FeatureSymbol.Back, tint = Color.White, description = stringResource(R.string.back)) }
                }
                Avatar(if(liveMessages==null) "HR" else "••", Modifier.size(44.dp))
                Column(Modifier.weight(1f)) {
                    Text(if(liveMessages==null) stringResource(R.string.driver_name) else "Conversation", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(if(liveMessages==null) stringResource(R.string.four_chat_status) else "Booking / trip messages", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .1f), border = BorderStroke(1.dp, Color.White.copy(alpha = .5f))) {
                    IconButton(externalAction, Modifier.size(42.dp), enabled = liveMessages==null) { FeatureIcon(FeatureSymbol.Phone, tint = Color.White, description = stringResource(R.string.four_call)) }
                }
            }
        }
        if(liveMessages==null) PickupSummary(externalAction, Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = PoolDesign.CoralLight.copy(alpha = .35f), border = BorderStroke(1.dp, Color.White)) {
                        Text(stringResource(R.string.four_today), Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            itemsIndexed(messages) { index, message -> ChatBubble(message, read = liveMessages==null && index == 4) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.four_quick1, R.string.four_quick2, R.string.four_quick3).forEach { resource ->
                val text = stringResource(resource)
                Surface(onClick = { if(!sending) draft = text }, shape = CircleShape, color = Color.White.copy(alpha = .85f),
                    border = BorderStroke(1.dp, Color.White), shadowElevation = 4.dp) {
                    Box(Modifier.heightIn(min = 36.dp).padding(horizontal = 16.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text(text, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if(sending) LinearProgressIndicator(Modifier.fillMaxWidth())
        val hint = stringResource(R.string.four_message_hint)
        Surface(Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 34.dp).fillMaxWidth(),
            shape = RoundedCornerShape(30.dp), color = Color.White.copy(alpha = .78f), shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color.White)) {
            Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(externalAction, enabled = liveMessages==null) { FeatureIcon(FeatureSymbol.Pin, description = stringResource(R.string.share_location)) }
                BasicTextField(draft, { draft = it }, Modifier.weight(1f).padding(horizontal = 6.dp).semantics { contentDescription = hint },
                    enabled = !sending, textStyle = MaterialTheme.typography.bodyLarge.copy(color = PoolDesign.Text), cursorBrush = SolidColor(PoolDesign.Text),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }),
                    maxLines = 4, decorationBox = { inner ->
                        if (draft.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = PoolDesign.Muted)
                        inner()
                    })
                Surface(shape = CircleShape, color = PoolDesign.Coral) {
                    IconButton(send, enabled = draft.isNotBlank() && !sending) {
                        FeatureIcon(FeatureSymbol.Send, tint = PoolDesign.Strong.copy(alpha = if (draft.isNotBlank()) 1f else 1f), description = stringResource(R.string.four_send))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun PickupSummary(share: () -> Unit, modifier: Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = Color(0xFFFFF4EF), border = BorderStroke(1.dp, Color.White), shadowElevation = 9.dp) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(22.dp), color = PoolDesign.CoralLight) {
                Canvas(Modifier.size(64.dp)) {
                    for (i in 0..9) {
                        val x = size.width * (.12f + i * .08f)
                        val y = size.height * (.82f - i * .075f)
                        drawCircle(PoolDesign.Strong, 1.4.dp.toPx(), Offset(x, y))
                    }
                    drawCircle(PoolDesign.Coral, 6.dp.toPx(), Offset(size.width * .59f, size.height * .44f))
                    drawCircle(PoolDesign.Strong, 2.dp.toPx(), Offset(size.width * .59f, size.height * .44f))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.four_pickup_point), style = MaterialTheme.typography.labelSmall, color = PoolDesign.Secondary)
                Text(stringResource(R.string.four_pickup_title), style = MaterialTheme.typography.titleSmall, color = PoolDesign.Text)
                Text(stringResource(R.string.four_pickup_meta), style = MaterialTheme.typography.bodySmall, color = PoolDesign.Secondary)
            }
            Surface(onClick = share, shape = CircleShape, color = Color(0xFFFBDDD0), border = BorderStroke(1.dp, Color.White)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeatureIcon(FeatureSymbol.Pin, Modifier.size(14.dp))
                    Text(stringResource(R.string.four_share), fontFamily = ChatFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, read: Boolean = false) {
    if (message.location) {
        Surface(Modifier.fillMaxWidth(.71f), shape = RoundedCornerShape(24.dp), color = PoolDesign.CoralLight.copy(alpha = .5f),
            border = BorderStroke(1.dp, Color.White)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = PoolDesign.Strong) {
                    FeatureIcon(FeatureSymbol.Pin, Modifier.padding(8.dp), tint = Color.White)
                }
                Column {
                    Text(message.text, style = MaterialTheme.typography.titleSmall, color = PoolDesign.Text)
                    Text(stringResource(R.string.four_location_meta), style = MaterialTheme.typography.bodySmall, color = PoolDesign.Secondary)
                }
            }
        }
    } else Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.outgoing) Alignment.End else Alignment.Start) {
        val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp,
            bottomStart = if (message.outgoing) 24.dp else 8.dp, bottomEnd = if (message.outgoing) 8.dp else 24.dp)
        Surface(Modifier.widthIn(max = 268.dp), shape = shape,
            color = if (message.outgoing) PoolDesign.Strong else Color.White.copy(alpha = .78f),
            contentColor = if (message.outgoing) Color.White else PoolDesign.Text,
            border = if (message.outgoing) BorderStroke(1.dp, Color.White.copy(alpha = .25f)) else BorderStroke(1.dp, Color.White),
            shadowElevation = if (message.outgoing) 10.dp else 0.dp) {
            Text(message.text, Modifier.then(if (message.outgoing) Modifier.background(PoolDesign.DarkGradient) else Modifier).padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge)
        }
        Text(message.time + if (read) " · " + stringResource(R.string.four_read) else "",
            Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.bodySmall, color = PoolDesign.Secondary)
    }
}

@Preview(name = "Chat", widthDp = 390, heightDp = 844)
@Preview(name = "Chat compact large text", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Preview(name = "Chat wide", widthDp = 600, heightDp = 900)
@Composable
private fun ChatPreview() { UMPoolingTheme { PeachBackground { ChatScreen({}, {}) } } }
