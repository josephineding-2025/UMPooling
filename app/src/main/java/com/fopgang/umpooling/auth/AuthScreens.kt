package com.fopgang.umpooling.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R
import com.fopgang.umpooling.data.UmEmail
import com.fopgang.umpooling.ui.components.*
import com.fopgang.umpooling.ui.theme.*

@Composable
fun WelcomeScreen(navigate: (String) -> Unit) {
    AuthDesignPage {
        Box(Modifier.fillMaxWidth().height(468.dp)) {
            Image(painterResource(R.drawable.welcome_universiti_malaya_campus_entrance_with_the_faculty_tower_and_university_sign),
                null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color(0x66321C14), Color(0x44321C14)))))
            Surface(Modifier.align(Alignment.TopCenter).padding(top = 149.dp).size(152.dp, 139.dp),
                shape = RoundedCornerShape(56.dp), color = Color(0xFFDED2C9).copy(alpha = .4f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .3f))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(painterResource(R.drawable.welcome_image2), null, Modifier.size(72.dp, 45.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append("UM") }
                        withStyle(SpanStyle(color = Color(0xFFF28A76))) { append("Pooling") }
                    }, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 66.dp).width(226.dp),
                shape = RoundedCornerShape(29.dp), color = Color.Transparent,
                border = BorderStroke(1.dp, Color.White.copy(alpha = .75f))) {
                Row(Modifier.background(Brush.linearGradient(listOf(Color(0xFF9C9583), Color(0xFF64574F))))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(58.dp, 34.dp)) {
                        listOf("HR", "AS").forEachIndexed { index, initials ->
                            Surface(Modifier.align(if (index == 0) Alignment.CenterStart else Alignment.CenterEnd).size(34.dp),
                                shape = CircleShape, color = if (index == 0) Color(0xFFFFE2D6) else Color(0xFFF18F75),
                                border = BorderStroke(2.dp, Color.White)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(initials, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.welcome_route), color = Color.White, fontSize = 15.sp,
                            lineHeight = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp)
                        Text(stringResource(R.string.welcome_meta), color = Color.White, fontSize = 13.sp, lineHeight = 16.sp)
                    }
                }
            }
            Surface(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 10.dp),
                shape = CircleShape, color = Color(0xFF55513B),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .65f))) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(Color(0xFFF08B6C), CircleShape))
                    Text(stringResource(R.string.welcome_price), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            color = Color(0xFFFFF9F6), border = BorderStroke(1.dp, Color.White)) {
            Column(Modifier.padding(horizontal = 25.dp).padding(top = 28.dp, bottom = 28.dp)) {
                Surface(shape = CircleShape, color = Color(0xFFF3DED4), border = BorderStroke(1.dp, Color.White)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FigmaIcon(R.drawable.welcome_frame, Modifier.size(15.dp))
                        Text(stringResource(R.string.um_only), fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.welcome_title).replace("lighten the campus.", "lighten\nthe campus."), fontSize = 29.sp, lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-.5).sp)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.welcome_body), fontSize = 16.sp, lineHeight = 22.sp, color = AuthMuted)
                Spacer(Modifier.height(26.dp))
                Surface(onClick = { navigate("register") }, shape = CircleShape, color = Color.Transparent,
                    shadowElevation = 12.dp, border = BorderStroke(1.dp, Color(0xFF887F78))) {
                    Box(Modifier.fillMaxWidth().height(56.dp).background(Brush.linearGradient(
                        listOf(Color(0xFF62564F), Color(0xFF27221F)))) , contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.get_started), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("›››", Modifier.align(Alignment.CenterEnd).padding(end = 17.dp),
                            color = Color.White, fontSize = 25.sp, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button({ navigate("login") }, Modifier.fillMaxWidth().height(56.dp), shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFCFA)),
                    border = BorderStroke(1.dp, Color(0xFFB1AAA4)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp)) {
                    Text(stringResource(R.string.already_account), fontFamily = FontFamily.SansSerif,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AuthInk)
                }
            }
        }
    }
}

@Composable
fun AuthScreen(register: Boolean, navigate: (String) -> Unit, back: () -> Unit, message: (Int) -> Unit,
    state: AuthUiState = AuthUiState(), onSubmit: (AuthSubmission) -> Unit = {}) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var matric by rememberSaveable { mutableStateOf("") }
    var staff by rememberSaveable { mutableStateOf(false) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    AuthDesignPage {
        Column(Modifier.padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(if (register) 94.dp else 40.dp))
        if (register) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AuthBack(back)
                AuthTabs(true, navigate)
            }
        } else {
            AuthBack(back)
            Spacer(Modifier.height(84.dp))
            AuthTabs(false, navigate)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(if (register) R.string.create_account else R.string.welcome_back),
            fontSize = if (register) 29.sp else 31.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.8).sp)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(if (register) R.string.register_body else R.string.login_body),
            fontSize = 15.sp, lineHeight = 21.sp, color = AuthMuted)
        Spacer(Modifier.height(if (register) 20.dp else 28.dp))
        if (register) {
            AuthInput(stringResource(R.string.full_name), name, { name = it }, stringResource(R.string.name_hint), enabled = !state.busy)
            Spacer(Modifier.height(8.dp))
        }
        AuthInput(stringResource(R.string.um_email), email, { email = it }, stringResource(R.string.email_hint),
            tall = !register, keyboardType = androidx.compose.ui.text.input.KeyboardType.Email, enabled = !state.busy)
        Spacer(Modifier.height(if (register) 8.dp else 15.dp))
        if (register) {
            AuthInput(stringResource(R.string.matric_id), matric, { matric = it }, stringResource(R.string.matric_hint), enabled = !state.busy)
            Spacer(Modifier.height(8.dp))
        }
        AuthInput(stringResource(R.string.password), password, { password = it },
            stringResource(if (register) R.string.new_password_hint else R.string.password_hint),
            password = true, showEye = !register, tall = !register, enabled = !state.busy)
        if (register) {
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.i_am), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                AuthChoice(stringResource(R.string.student), !staff) { staff = false }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .75f), border = BorderStroke(1.dp, Color(0xFFF4CEBD))) {
                    AuthChoice(stringResource(R.string.staff), staff) { staff = true }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Checkbox(accepted, { accepted = it }, Modifier.size(22.dp),
                    colors = CheckboxDefaults.colors(checkedColor = AuthInk, uncheckedColor = AuthInk))
                Text(stringResource(R.string.guidelines), Modifier.weight(1f), fontSize = 14.sp, lineHeight = 19.sp, color = AuthMuted)
            }
            Spacer(Modifier.height(18.dp))
        } else {
            TextButton({ message(R.string.not_connected) }, Modifier.align(Alignment.End).height(33.dp), contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.forgot_password), color = AuthAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
        }
        AuthFeedback(state)
        AuthAction(stringResource(if (register) R.string.create_account_button else R.string.login), Modifier.fillMaxWidth(), enabled = !state.busy) {
            val validEmail = UmEmail.isValid(email)
            when {
                !validEmail -> message(R.string.email_invalid)
                password.isBlank() || (register && (password.length < 8 || name.isBlank() || matric.isBlank() || !accepted)) -> message(R.string.form_invalid)
                else -> onSubmit(AuthSubmission(UmEmail.normalize(email), password, name, matric, staff, accepted))
            }
        }
        if (!register) {
            Spacer(Modifier.height(13.dp))
            TextButton({ navigate("register") }, Modifier.align(Alignment.CenterHorizontally).heightIn(min = 32.dp), contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.new_account), fontSize = 15.sp, color = AuthAccent)
            }
            Spacer(Modifier.height(76.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
                color = Color(0xFFFFE0D1).copy(alpha = .6f), border = BorderStroke(1.dp, Color.White), shadowElevation = 6.dp) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = MaterialTheme.shapes.small, color = Ink) {
                        FigmaIcon(R.drawable.login_frame4, Modifier.padding(10.dp).size(20.dp))
                    }
                    Text(stringResource(R.string.verified_only), Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
        // Keep the existing explicit demo entry separate from the form actions.
        Spacer(Modifier.height(if (register) 70.dp else 30.dp))
        TextButton({ navigate("demo") }, Modifier.align(Alignment.CenterHorizontally), enabled = !state.busy) {
            Text(stringResource(R.string.explore_demo), fontSize = 13.sp, color = AuthAccent)
        }
        Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun VerificationScreen(back: () -> Unit, message: (Int) -> Unit, navigate: (String) -> Unit,
    state: AuthUiState = AuthUiState(), onVerify: (String) -> Unit = {}, onResend: () -> Unit = {}) {
    var code by rememberSaveable { mutableStateOf("") }
    var codeFocused by remember { mutableStateOf(false) }
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(codeFocused, code) {
        cursorVisible = true
        while (codeFocused) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }
    AuthDesignPage {
        Box(Modifier.fillMaxWidth().height(230.dp)) {
            Box(Modifier.padding(start = 19.dp, top = 31.dp)) { AuthBack(back) }
            Box(Modifier.align(Alignment.TopCenter).padding(top = 79.dp)) {
                Surface(Modifier.size(114.dp).background(Brush.linearGradient(listOf(
                    Color(0xFFFFF3EC).copy(alpha = .5f), Color(0xFFFDDCD0).copy(alpha = .55f))), CircleShape),
                    shape = CircleShape, color = Color.Transparent,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .9f))) {
                    Box(contentAlignment = Alignment.Center) { FigmaIcon(R.drawable.verify_frame, Modifier.size(38.dp)) }
                }
                Surface(Modifier.align(Alignment.TopEnd).padding(top = 7.dp).size(34.dp),
                    shape = CircleShape, color = Color(0xFFFF9291), border = BorderStroke(3.dp, Color(0xFFFFE6DB))) {
                    Box(contentAlignment = Alignment.Center) { FigmaIcon(R.drawable.profile_frame7, Modifier.size(15.dp)) }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            color = Color.White.copy(alpha = .85f), border = BorderStroke(1.dp, Color.White)) {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 27.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.verify_title), fontSize = 28.sp, lineHeight = 35.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, letterSpacing = (-.7).sp)
                Spacer(Modifier.height(9.dp))
                Text(stringResource(R.string.verify_body), fontSize = 15.sp, lineHeight = 21.sp,
                    color = AuthMuted, textAlign = TextAlign.Center)
                (state.access as? AuthAccess.VerificationRequired)?.let { Text(it.email, color = AuthMuted) }
                AuthFeedback(state)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    listOf("@siswa.um.edu.my", "@um.edu.my").forEach { domain ->
                        Surface(shape = CircleShape, color = Color(0xFFFFE4D7).copy(alpha = .75f),
                            border = BorderStroke(1.dp, Color.White)) {
                            Text(domain, Modifier.padding(horizontal = 15.dp, vertical = 9.dp), fontSize = 14.sp,
                                lineHeight = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                val codeLabel = stringResource(R.string.code_label)
                BasicTextField(code, { code = it.filter(Char::isDigit).take(6) }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { codeFocused = it.isFocused }
                        .semantics { contentDescription = codeLabel }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                    decorationBox = { inner ->
                        Box {
                            Box(Modifier.alpha(0f)) { inner() }
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                repeat(6) { index ->
                                    val active = codeFocused && index == code.length
                                    Box(Modifier.weight(1f).height(56.dp), contentAlignment = Alignment.Center) {
                                        if (active) {
                                            Box(Modifier.matchParentSize().background(Color(0xFFDFDAD6), RoundedCornerShape(19.dp)))
                                        }
                                        Surface(
                                            Modifier.fillMaxSize().padding(if (active) 3.dp else 0.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color(0xFFFFFDFC),
                                            border = BorderStroke(if (active) 2.dp else 1.dp,
                                                if (active) AuthInk else Color(0xFFF6D5C7))
                                        ) {
                                            Box(Modifier.background(Brush.verticalGradient(listOf(
                                                Color(0xFFFFECE3).copy(alpha = .45f), Color.White.copy(alpha = .8f),
                                                Color(0xFFFFF7F2)))), contentAlignment = Alignment.Center) {
                                                Text(code.getOrNull(index)?.toString().orEmpty(), fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold, color = AuthInk)
                                                if (active && cursorVisible) {
                                                    Box(Modifier.width(2.dp).height(24.dp).background(AuthInk))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    })
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(30.dp), color = Color.Transparent,
                    border = BorderStroke(1.dp, Color.White), shadowElevation = 1.dp) {
                    Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFFFFF9F5), Color(0xFFFDE1D4))))
                        .padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        listOf(R.string.verify_step1, R.string.verify_step2, R.string.verify_step3).forEachIndexed { index, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(28.dp).background(if (index < 2) AuthInk else Color(0xFFFF9392),
                                    RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Text((index + 1).toString(), fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold, color = if (index < 2) Color.White else AuthInk)
                                }
                                Text(stringResource(step), Modifier.weight(1f), fontSize = 14.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(55.dp))
                Button(
                    { onVerify(code) }, Modifier.fillMaxWidth().height(54.dp),
                    enabled = code.length == 6 && !state.busy, shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black,
                        contentColor = Color.White, disabledContainerColor = Color.Black,
                        disabledContentColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp, disabledElevation = 12.dp)
                ) {
                    Text(stringResource(R.string.verify_email), fontFamily = FontFamily.SansSerif,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(14.dp))
                TextButton(onResend, enabled = !state.busy, contentPadding = PaddingValues(0.dp)) {
                    val resendLabel = stringResource(R.string.resend)
                    val split = resendLabel.indexOf('?').let { if (it >= 0) it + 1 else 0 }
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = AuthMuted)) { append(resendLabel.take(split)) }
                        withStyle(SpanStyle(color = AuthAccent, fontWeight = FontWeight.Bold)) {
                            append(resendLabel.drop(split))
                        }
                    }, fontSize = 15.sp)
                }
                Spacer(Modifier.height(20.dp))
                TextButton(back, enabled = !state.busy) { Text(stringResource(R.string.auth_verified_sign_in)) }
                TextButton({ navigate("demo") }, enabled = !state.busy) {
                    Text(stringResource(R.string.explore_demo), fontSize = 13.sp, color = AuthAccent)
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun WelcomePreview() { UMPoolingTheme { PeachBackground { WelcomeScreen({}) } } }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LoginPreview() { UMPoolingTheme { PeachBackground { AuthScreen(false, {}, {}, {}) } } }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun RegisterPreview() { UMPoolingTheme { PeachBackground { AuthScreen(true, {}, {}, {}) } } }

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun VerificationPreview() { UMPoolingTheme { VerificationScreen({}, {}, {}) } }
