package com.fopgang.umpooling.safety

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.FigmaIcon
import com.fopgang.umpooling.ui.theme.UMPoolingTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SafetyInk = Color(0xFF24211F)
private val SafetyMuted = Color(0xFF72635E)

@Composable
private fun SafetyPage(review: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, color = SafetyInk),
        LocalMinimumInteractiveComponentSize provides 0.dp
    ) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFFFCEBE3)).drawWithCache {
                val white = Brush.radialGradient(listOf(Color.White, Color.Transparent), Offset.Zero, size.width * .8f)
                val peach = Brush.radialGradient(listOf(Color(0xFFF9CCBE), Color.Transparent),
                    Offset(size.width, size.height * .18f), size.width * .8f)
                val coral = Brush.radialGradient(listOf(Color(0xFFFF9392), Color.Transparent),
                    Offset(size.width * .5f, size.height * .55f), size.width * .8f)
                onDrawBehind {
                    drawRect(white); drawRect(peach)
                    if (review) drawRect(coral)
                }
            }.imePadding().verticalScroll(rememberScrollState()), content = content
        )
    }
}

@Composable
private fun SafetyHeader(title: String, back: () -> Unit, close: Boolean = false) {
    Box(Modifier.fillMaxWidth().padding(horizontal = if (close) 19.dp else 24.dp).height(46.dp)) {
        Surface(
            shape = CircleShape, color = Color.White.copy(alpha = .9f),
            border = if (close) null else BorderStroke(1.dp, Color(0xFFF7D8CC)),
            shadowElevation = if (close) 0.dp else 3.dp
        ) {
            IconButton(back, Modifier.size(if (close) 43.dp else 46.dp)) {
                if (close) {
                    Canvas(Modifier.size(18.dp).semantics { contentDescription = "Close" }) {
                        val a = size.width * .25f
                        val b = size.width * .75f
                        drawLine(SafetyInk, Offset(a, a), Offset(b, b), 1.8.dp.toPx(),
                            androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(SafetyInk, Offset(b, a), Offset(a, b), 1.8.dp.toPx(),
                            androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                } else FigmaIcon(R.drawable.login_frame, Modifier.size(20.dp), stringResource(R.string.back))
            }
        }
        Text(title, Modifier.align(Alignment.Center), fontSize = if (close) 20.sp else 21.sp,
            lineHeight = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
    }
}

@Composable
fun SosScreen(back: () -> Unit, message: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val holdLabel = stringResource(R.string.hold_sos)
    SafetyPage {
        Spacer(Modifier.height(52.dp))
        SafetyHeader(stringResource(R.string.emergency_sos), back)
        Spacer(Modifier.height(14.dp))
        Surface(Modifier.align(Alignment.CenterHorizontally), shape = CircleShape,
            color = Color(0xFF3D3430), shadowElevation = 8.dp) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(Color(0xFFF7C5AD), CircleShape))
                Text(stringResource(R.string.trip_progress), color = Color.White, fontSize = 13.sp,
                    lineHeight = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.774f).aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color(0xFFF8BDAA).copy(alpha = .27f),
                border = BorderStroke(1.3.dp, Color(0xFFF3AE96))) {}
            Surface(Modifier.fillMaxSize(.775f), shape = CircleShape, color = Color.White.copy(alpha = .18f),
                border = BorderStroke(1.3.dp, Color.White.copy(alpha = .5f))) {}
            Surface(
                Modifier.fillMaxSize(.544f)
                    .semantics {
                        role = Role.Button
                        onClick(holdLabel) { message(R.string.sos_demo); true }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            val job = scope.launch { delay(3000); message(R.string.sos_demo) }
                            try { tryAwaitRelease() } finally { job.cancel() }
                        })
                    },
                shape = CircleShape, color = Color.Transparent, shadowElevation = 18.dp
            ) {
                Column(
                    Modifier.background(Brush.linearGradient(listOf(Color(0xFFF6AC96), Color(0xFFFF9296), Color(0xFFE78482)))),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    FigmaIcon(R.drawable.sos_frame, Modifier.size(29.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.sos), fontSize = 42.sp, lineHeight = 46.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color(0xFF301B11))
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        Text(holdLabel, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 17.sp,
            lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.sos_cancel_note), Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            textAlign = TextAlign.Center, fontSize = 12.5.sp, lineHeight = 17.sp, color = SafetyMuted)
        Spacer(Modifier.height(23.dp))
        Surface(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            color = Color.White.copy(alpha = .86f), shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 23.dp, bottom = 26.dp)) {
                Surface(shape = CircleShape, color = Color(0xFFFFE5D9).copy(alpha = .8f),
                    border = BorderStroke(1.dp, Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).background(Color(0xFFFDE0D2), CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text("HR", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.sos_driver), fontSize = 15.sp, lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
                            Text(stringResource(R.string.sos_destination), fontSize = 12.sp,
                                lineHeight = 16.sp, color = SafetyMuted, letterSpacing = (-.1).sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        R.drawable.sos_frame2 to R.string.sos_location,
                        R.drawable.sos_frame3 to R.string.sos_security,
                        R.drawable.sos_frame4 to R.string.sos_contacts
                    ).forEach { (icon, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(30.dp).background(Color(0xFFFFE1D2), RoundedCornerShape(11.dp)),
                                contentAlignment = Alignment.Center) { FigmaIcon(icon, Modifier.size(16.dp)) }
                            Text(stringResource(text), Modifier.weight(1f), fontSize = 14.sp, lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium, letterSpacing = (-.3).sp)
                        }
                    }
                }
                Spacer(Modifier.height(39.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Button(
                        { message(R.string.sos_demo) }, Modifier.weight(1f).height(51.dp), shape = CircleShape,
                        contentPadding = PaddingValues(0.dp), border = BorderStroke(1.dp, Color(0xFFC58B7D)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE8DC), contentColor = Color(0xFFA43D26))
                    ) {
                        Canvas(Modifier.size(17.dp)) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * .2f, size.height * .1f)
                                lineTo(size.width * .4f, size.height * .1f)
                                lineTo(size.width * .48f, size.height * .35f)
                                lineTo(size.width * .35f, size.height * .45f)
                                quadraticBezierTo(size.width * .45f, size.height * .65f, size.width * .62f, size.height * .7f)
                                lineTo(size.width * .72f, size.height * .55f)
                                lineTo(size.width * .92f, size.height * .65f)
                                lineTo(size.width * .9f, size.height * .9f)
                                cubicTo(size.width * .4f, size.height * .97f, size.width * .08f, size.height * .5f,
                                    size.width * .2f, size.height * .1f)
                                close()
                            }
                            drawPath(path, Color(0xFFA43D26), style = androidx.compose.ui.graphics.drawscope.Stroke(
                                1.4.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.call_999), fontFamily = FontFamily.SansSerif,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA43D26))
                    }
                    Button(
                        { message(R.string.sos_demo) }, Modifier.weight(1.2f).height(51.dp),
                        shape = CircleShape, contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) { Text(stringResource(R.string.share_location), color = Color.White,
                        fontFamily = FontFamily.SansSerif, fontSize = 16.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(back: () -> Unit, message: (Int) -> Unit, onSubmit: () -> Unit = {message(R.string.not_connected)}) {
    var rating by rememberSaveable { mutableIntStateOf(4) }
    var selected by rememberSaveable { mutableStateOf(listOf(R.string.punctual, R.string.friendly, R.string.clean_car)) }
    var review by rememberSaveable { mutableStateOf("") }
    SafetyPage(review = true) {
        Spacer(Modifier.height(30.dp))
        SafetyHeader(stringResource(R.string.rate_trip), back, close = true)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 18.dp)) {
            ReviewCard {
                Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp).padding(top = 21.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(61.dp, 58.dp).background(
                        Brush.linearGradient(listOf(Color(0xFFBC957A), Color(0xFF79543E))), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("DT", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.review_question), fontSize = 20.sp, lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, letterSpacing = (-.7).sp)
                    Spacer(Modifier.height(3.dp))
                    Text(stringResource(R.string.review_trip), fontSize = 13.sp, lineHeight = 18.sp,
                        color = SafetyMuted, textAlign = TextAlign.Center, letterSpacing = (-.3).sp)
                    Spacer(Modifier.height(17.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        (1..5).forEach { star ->
                            IconButton({ rating = star }, Modifier.size(40.dp)) {
                                Image(painterResource(if (star <= rating) R.drawable.review_frame else R.drawable.review_frame1),
                                    stringResource(R.string.rating_value, star), Modifier.size(32.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(if (rating == 4) stringResource(R.string.great) else stringResource(R.string.rating_value, rating),
                        fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF965022))
                }
            }
            Spacer(Modifier.height(12.dp))
            ReviewCard {
                Column(Modifier.padding(17.dp)) {
                    Text(stringResource(R.string.what_went_well), fontSize = 18.sp, lineHeight = 23.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = (-.5).sp)
                    Spacer(Modifier.height(9.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(R.string.punctual, R.string.friendly, R.string.safe_driving, R.string.clean_car,
                            R.string.good_conversation, R.string.fair_price).forEach { tag ->
                            Surface(
                                selected = tag in selected,
                                onClick = { selected = if (tag in selected) selected - tag else selected + tag },
                                shape = CircleShape,
                                color = if (tag in selected) Color(0xFF2D2D2D) else Color(0xFFFFFCFB),
                                border = if (tag in selected) null else BorderStroke(1.dp, Color(0xFFE9E5E2))
                            ) {
                                Text(stringResource(tag), Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                                    color = if (tag in selected) Color.White else Color(0xFF303030),
                                    fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            ReviewCard {
                Column(Modifier.padding(17.dp)) {
                    Text(stringResource(R.string.write_review), fontSize = 18.sp, lineHeight = 23.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = (-.5).sp)
                    Spacer(Modifier.height(9.dp))
                    val reviewLabel = stringResource(R.string.write_review)
                    BasicTextField(review, { review = it },
                        Modifier.fillMaxWidth().heightIn(min = 76.dp)
                            .background(Color(0xFFFFFCFC), RoundedCornerShape(15.dp))
                            .padding(12.dp).semantics { contentDescription = reviewLabel },
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp,
                            lineHeight = 19.sp, color = SafetyInk), cursorBrush = SolidColor(SafetyInk),
                        decorationBox = { inner ->
                            if (review.isEmpty()) Text(stringResource(R.string.review_hint), fontSize = 14.sp,
                                lineHeight = 19.sp, color = Color(0xFF858585))
                            inner()
                        })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FigmaIcon(R.drawable.sos_frame3, Modifier.size(16.dp))
                Text(stringResource(R.string.review_note), Modifier.weight(1f), fontSize = 12.sp,
                    lineHeight = 17.sp, color = Color(0xFF55514F))
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onSubmit, Modifier.fillMaxWidth().height(47.dp),
                shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
            ) {
                Text(stringResource(R.string.submit_review), color = Color.White, fontFamily = FontFamily.SansSerif,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(back, Modifier.align(Alignment.CenterHorizontally).height(34.dp), contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.skip), fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = Color(0xFF514D4B))
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun ReviewCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = .64f),
        border = BorderStroke(1.dp, Color.White)) {
        Column(content = content)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SosPreview() { UMPoolingTheme { SosScreen({}, {}) } }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReviewPreview() { UMPoolingTheme { ReviewScreen({}, {}) } }
