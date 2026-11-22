package com.fopgang.umpooling.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
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
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.FigmaIcon
import com.fopgang.umpooling.ui.theme.UMPoolingTheme

@Composable
fun VoiceRequestScreen(back: () -> Unit, unavailable: () -> Unit, message: (Int) -> Unit, onManual: (String) -> Unit = { unavailable() }) {
    val example = stringResource(R.string.ai_sample)
    var request by rememberSaveable { mutableStateOf(example) }
    ProvideTextStyle(TextStyle(fontFamily = FontFamily.SansSerif, color = Color.White, fontSize = 15.sp)) {
        Box(Modifier.fillMaxSize().imePadding()) {
            VoiceBackdrop()
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(298.dp)) {
                    Image(
                        painterResource(R.drawable.voice_frame1), null,
                        Modifier.offset(x = -15.dp).wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(maxWidth * 1.04f, maxWidth * 1.095f),
                        contentScale = ContentScale.FillBounds
                    )
                    Box(Modifier.fillMaxWidth().padding(horizontal = 19.dp).padding(top = 57.dp)) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f),
                            border = BorderStroke(.6.dp, Color.White.copy(alpha = .8f))) {
                            IconButton(back, Modifier.size(50.dp)) {
                                Image(painterResource(R.drawable.login_frame), stringResource(R.string.back),
                                    Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White))
                            }
                        }
                        Text(stringResource(R.string.book_ride),
                            Modifier.align(Alignment.Center).padding(start = 16.dp),
                            fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(start = 158.dp, end = 19.dp).padding(top = 194.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.voice_request), fontSize = 23.sp,
                            lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp,
                            maxLines = 1)
                        Text(stringResource(R.string.ai_body), fontSize = 15.sp, lineHeight = 20.sp)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 37.dp)
                        .heightIn(min = 177.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Color(0x77FFF5E7), Color(0x55FFC1B5))))
                        .drawWithCache {
                            val edge = Brush.radialGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = .35f)),
                                center = Offset(size.width / 2, size.height / 2), radius = size.width * .55f
                            )
                            onDrawBehind { drawRect(edge) }
                        }
                ) {
                    Surface(
                        Modifier.matchParentSize(), shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent, border = BorderStroke(1.dp, Color.White.copy(alpha = .6f))
                    ) {}
                    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 23.dp, bottom = 11.dp)) {
                        val requestLabel = stringResource(R.string.voice_request)
                        BasicTextField(
                            request, { request = it },
                            Modifier.fillMaxWidth().heightIn(min = 107.dp)
                                .semantics { contentDescription = requestLabel },
                            textStyle = TextStyle(fontFamily = FontFamily.SansSerif,
                                fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = (-.65).sp, color = Color.White),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White)
                        )
                        Spacer(Modifier.height(5.dp))
                        Surface(Modifier.align(Alignment.End), shape = CircleShape,
                            color = Color(0xFFCAC5C1).copy(alpha = .92f)) {
                            IconButton({ message(R.string.not_connected) }, Modifier.size(52.dp, 30.dp)) {
                                FigmaIcon(R.drawable.voice_frame3, Modifier.size(22.dp),
                                    description = stringResource(R.string.send_request))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.try_example),
                    Modifier.padding(start = 35.dp), fontSize = 16.sp,
                    lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF1E9))
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(horizontal = 19.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        R.string.example1 to R.drawable.voice_frame4,
                        R.string.example2 to R.drawable.voice_frame5,
                        R.string.example3 to R.drawable.voice_frame6
                    ).forEach { (text, icon) ->
                        val value = stringResource(text)
                        Surface(
                            onClick = { request = value.trim('“', '”') },
                            shape = CircleShape, color = Color.White.copy(alpha = .65f)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 50.dp)
                                    .padding(horizontal = 18.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                FigmaIcon(icon, Modifier.size(28.dp))
                                Text(value, Modifier.weight(1f), fontSize = 14.sp, lineHeight = 17.sp,
                                    letterSpacing = (-.25).sp, color = Color(0xFF292929))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(42.dp))
                VoiceWaveform(Modifier.align(Alignment.CenterHorizontally).width(142.dp).height(18.dp))
                Spacer(Modifier.height(10.dp))
                Button(
                    { message(R.string.not_connected) },
                    Modifier.align(Alignment.CenterHorizontally).width(224.dp).height(37.dp),
                    shape = CircleShape, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                ) {
                    VoiceSparkles(Modifier.size(23.dp))
                    Spacer(Modifier.width(15.dp))
                    Text(stringResource(R.string.hold_talk), fontFamily = FontFamily.SansSerif,
                        fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
                TextButton({onManual(request)}, Modifier.align(Alignment.CenterHorizontally).height(30.dp),
                    contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.fill_manually), fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, color = Color(0xFF6F6D66))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun VoiceBackdrop() {
    Box(Modifier.fillMaxSize().background(Color(0xFFC7B0AC)).drawWithCache {
        val rose = Brush.radialGradient(listOf(Color(0xFF8B4056), Color.Transparent),
            Offset(size.width * .72f, size.height * .26f), size.width * .8f)
        val peach = Brush.radialGradient(listOf(Color(0xFFFFA77D), Color.Transparent),
            Offset(size.width, size.height * .43f), size.width * .65f)
        val bottom = Brush.verticalGradient(
            0f to Color.Transparent, .78f to Color.Transparent, 1f to Color(0xFFFFD8D0))
        onDrawBehind { drawRect(rose); drawRect(peach); drawRect(bottom) }
    }) {
        Image(painterResource(R.drawable.voice_rectangle58), null,
            Modifier.offset(x = 110.dp, y = (-40).dp).size(145.dp, 190.dp)
                .rotate(-28f).blur(24.dp, BlurredEdgeTreatment.Unbounded), contentScale = ContentScale.Crop, alpha = .6f)
        Image(painterResource(R.drawable.voice_rectangle56), null,
            Modifier.align(Alignment.BottomStart).offset(x = 94.dp, y = (-80).dp)
                .size(110.dp, 140.dp).rotate(28f).blur(14.dp, BlurredEdgeTreatment.Unbounded),
            contentScale = ContentScale.Crop, alpha = .9f)
        Column(Modifier.offset(x = (-38).dp, y = 91.dp).rotate(-35f).blur(2.dp)
            .clip(RoundedCornerShape(24.dp)).background(Color(0x66FFE3D4)).padding(22.dp)) {
            Text("Soft", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Sounds", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.align(Alignment.BottomStart).offset(x = (-42).dp, y = (-127).dp)
            .rotate(34f).blur(2.dp).clip(RoundedCornerShape(24.dp))
            .background(Color(0x77FFEBDD)).padding(22.dp)) {
            Text("Mellow", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Waves", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VoiceWaveform(modifier: Modifier) {
    Canvas(modifier) {
        val heights = listOf(6, 6, 6, 6, 6, 8, 10, 15, 10, 8, 6, 5, 5, 5, 5)
        heights.forEachIndexed { index, height ->
            val x = index * size.width / (heights.size - 1)
            drawLine(Color(0xFFFFEADB), Offset(x, size.height),
                Offset(x, size.height - height.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun VoiceSparkles(modifier: Modifier) {
    Canvas(modifier) {
        fun star(x: Float, y: Float, radius: Float) {
            val path = Path().apply {
                moveTo(x, y - radius); lineTo(x + radius * .3f, y - radius * .3f)
                lineTo(x + radius, y); lineTo(x + radius * .3f, y + radius * .3f)
                lineTo(x, y + radius); lineTo(x - radius * .3f, y + radius * .3f)
                lineTo(x - radius, y); lineTo(x - radius * .3f, y - radius * .3f); close()
            }
            drawPath(path, Color.White)
        }
        star(size.width * .35f, size.height * .6f, size.width * .32f)
        star(size.width * .76f, size.height * .25f, size.width * .16f)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun VoicePreview() { UMPoolingTheme { VoiceRequestScreen({}, {}, {}) } }
