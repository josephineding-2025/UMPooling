package com.fopgang.umpooling.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.ui.theme.*

@Composable
fun FigmaIcon(@DrawableRes asset: Int, modifier: Modifier = Modifier, description: String? = null) {
    Image(painterResource(asset), description, modifier.then(Modifier.size(22.dp)), contentScale = ContentScale.Fit)
}

@Composable
fun PeachBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Peach)) {
        Box(Modifier.matchParentSize().drawWithCache {
            val white = Brush.radialGradient(listOf(Color.White.copy(alpha = .95f), Color.Transparent), Offset.Zero, size.width * .7f)
            val pink = Brush.radialGradient(listOf(SoftCoral, Color.Transparent), Offset(size.width, size.height * .2f), size.width * .9f)
            val peach = Brush.radialGradient(listOf(Color(0xFFFCD9D3), Color.Transparent), Offset(0f, size.height), size.width * .85f)
            onDrawBehind { drawRect(white); drawRect(pink); drawRect(peach) }
        })
        content()
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, radius: Dp = 28.dp, opacity: Float = .65f, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(radius), color = Color.White.copy(alpha = opacity),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .9f)), shadowElevation = 8.dp) {
        Column(content = content)
    }
}

@Composable
fun PillButton(label: String, modifier: Modifier = Modifier, secondary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = if (secondary) Color.White.copy(alpha = .7f) else Color.Black,
            contentColor = if (secondary) Ink else Color.White),
        border = if (secondary) BorderStroke(1.dp, Color(0xFFE8C7BD)) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Avatar(initials: String, modifier: Modifier = Modifier, dark: Boolean = false) {
    Box(modifier.size(46.dp).background(if (dark) Color.Black else Color(0xFFFDE3D7), CircleShape), contentAlignment = Alignment.Center) {
        Text(initials, color = if (dark) Color.White else Color(0xFF3A2C28), fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color(0xFFFFE3D5), shape = CircleShape) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Nutmeg, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ScreenHeader(title: String, back: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = .75f), shadowElevation = 5.dp) {
            IconButton(back) { FigmaIcon(R.drawable.login_frame, description = stringResource(R.string.back)) }
        }
        Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)
        trailing?.invoke()
    }
}

@Composable
fun PageColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable
fun LabeledInput(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "", password: Boolean = false, icon: Int? = null) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), placeholder = { Text(placeholder) },
            singleLine = true, shape = CircleShape,
            visualTransformation = if (password && !visible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            leadingIcon = if (icon != null) { { FigmaIcon(icon, Modifier.size(20.dp)) } } else null,
            trailingIcon = if (password) { { IconButton({ visible = !visible }) { FigmaIcon(R.drawable.login_frame3, description = stringResource(R.string.password)) } } } else null,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White.copy(alpha = .65f),
                focusedContainerColor = Color.White.copy(alpha = .8f), unfocusedBorderColor = Color(0xFFF0C9BB)))
    }
}
