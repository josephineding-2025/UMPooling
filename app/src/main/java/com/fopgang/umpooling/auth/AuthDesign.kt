package com.fopgang.umpooling.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.components.FigmaIcon

internal val AuthInk = Color(0xFF27211E)
internal val AuthMuted = Color(0xFF7D6B63)
internal val AuthAccent = Color(0xFFBD482B)
internal val AuthText = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 20.sp, color = AuthInk)

@Composable
internal fun AuthDesignPage(content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides AuthText, LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(Modifier.fillMaxSize().background(Color(0xFFFCEBE3)).drawWithCache {
            val light = Brush.radialGradient(listOf(Color.White, Color.Transparent), Offset.Zero, size.width * .8f)
            val peach = Brush.radialGradient(listOf(Color(0xFFF8C9BB), Color.Transparent),
                Offset(size.width, size.height * .25f), size.width * .85f)
            val bottom = Brush.radialGradient(listOf(Color(0xFFFBD0C0), Color.Transparent),
                Offset(0f, size.height), size.width * .65f)
            onDrawBehind { drawRect(light); drawRect(peach); drawRect(bottom) }
        }.imePadding().verticalScroll(rememberScrollState()), content = content)
    }
}

@Composable
internal fun AuthBack(back: () -> Unit) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = .85f),
        border = BorderStroke(1.dp, Color(0xFFF6D7CA)), shadowElevation = 3.dp) {
        IconButton(back, Modifier.size(46.dp)) {
            FigmaIcon(R.drawable.login_frame, Modifier.size(20.dp), stringResource(R.string.back))
        }
    }
}

@Composable
internal fun AuthChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, action: () -> Unit) {
    Surface(selected = selected, onClick = action, modifier = modifier, shape = CircleShape,
        color = if (selected) Color(0xFF352D29) else Color.Transparent,
        shadowElevation = if (selected) 7.dp else 0.dp) {
        Box(Modifier.height(38.dp).then(if (selected) Modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF5C5049), Color(0xFF292321)))) else Modifier),
            contentAlignment = Alignment.Center) {
            Text(label, Modifier.padding(horizontal = 20.dp), color = if (selected) Color.White else AuthInk,
                fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun AuthTabs(register: Boolean, navigate: (String) -> Unit) {
    Surface(Modifier.width(232.dp), shape = CircleShape, color = Color.White.copy(alpha = .7f),
        border = BorderStroke(1.dp, Color.White), shadowElevation = 6.dp) {
        Row(Modifier.padding(5.dp)) {
            AuthChoice(stringResource(R.string.login), !register, Modifier.weight(1f)) { navigate("login") }
            AuthChoice(stringResource(R.string.register), register, Modifier.weight(1f)) { navigate("register") }
        }
    }
}

@Composable
internal fun AuthAction(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, secondary: Boolean = false, action: () -> Unit) {
    Button(action, modifier.fillMaxWidth().height(56.dp), enabled = enabled, shape = CircleShape,
        border = if (secondary) BorderStroke(1.dp, Color(0xFFF2CABB)) else null,
        colors = ButtonDefaults.buttonColors(containerColor = if (secondary) Color.White.copy(alpha = .8f) else Color.Black,
            disabledContainerColor = Color(0xFF655952), disabledContentColor = Color.White.copy(alpha = .7f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (secondary) 2.dp else 12.dp)) {
        Text(label, fontFamily = FontFamily.SansSerif, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            color = if (secondary) AuthInk else if (enabled) Color.White else Color.White.copy(alpha = .7f))
    }
}

@Composable
internal fun AuthInput(label: String, value: String, onChange: (String) -> Unit, hint: String,
    password: Boolean = false, showEye: Boolean = true, tall: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text, enabled: Boolean = true) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(label, Modifier.padding(start = 6.dp), fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Surface(Modifier.fillMaxWidth(), shape = CircleShape, color = Color.White.copy(alpha = .78f),
            border = BorderStroke(1.dp, Color(0xFFF7CDBB)), shadowElevation = 3.dp) {
            Row(Modifier.height(if (tall) 57.dp else 53.dp).padding(start = 50.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(value, onChange, Modifier.weight(1f).semantics { contentDescription = label },
                    textStyle = AuthText.copy(fontSize = 16.sp), singleLine = true, enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
                    visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                    cursorBrush = SolidColor(AuthInk), decorationBox = { inner ->
                        if (value.isEmpty()) Text(hint, fontSize = 16.sp, color = AuthMuted)
                        inner()
                    })
                if (password && showEye) IconButton({ visible = !visible }, Modifier.size(25.dp)) {
                    FigmaIcon(R.drawable.login_frame3, Modifier.size(19.dp), stringResource(R.string.password))
                }
            }
        }
    }
}
