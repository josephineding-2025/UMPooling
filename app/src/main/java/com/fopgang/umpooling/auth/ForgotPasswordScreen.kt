package com.fopgang.umpooling.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fopgang.umpooling.R

/**
 * Password reset in two steps: email a 6-digit code, then enter the code with a new password.
 * Needs the "Reset password" email template in Supabase to include the code ({{ .Token }}).
 */
@Composable
fun ForgotPasswordScreen(
    navigate: (String) -> Unit,
    back: () -> Unit,
    state: AuthUiState = AuthUiState(),
    onRequestCode: (String) -> Unit = {},
    onReset: (email: String, code: String, newPassword: String) -> Unit = { _, _, _ -> },
) {
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    // The password is never saved with the screen state.
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.notice) {
        when (state.notice) {
            AuthNotice.RESET_CODE_SENT -> codeSent = true
            AuthNotice.PASSWORD_UPDATED -> navigate("login")
            else -> Unit
        }
    }

    AuthDesignPage {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(40.dp))
            AuthBack(back)
            Spacer(Modifier.height(48.dp))
            Text(
                stringResource(R.string.forgot_title),
                fontSize = 29.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-.8).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.forgot_body), fontSize = 15.sp, lineHeight = 21.sp, color = AuthMuted)
            Spacer(Modifier.height(24.dp))

            AuthInput(
                label = stringResource(R.string.um_email),
                value = email,
                onChange = { email = it },
                hint = stringResource(R.string.email_hint),
                keyboardType = KeyboardType.Email,
                enabled = !state.busy && !codeSent,
            )
            if (codeSent) {
                Spacer(Modifier.height(12.dp))
                AuthInput(
                    label = stringResource(R.string.reset_code_label),
                    value = code,
                    onChange = { code = it.filter(Char::isDigit).take(CODE_LENGTH) },
                    hint = stringResource(R.string.reset_code_hint),
                    keyboardType = KeyboardType.NumberPassword,
                    enabled = !state.busy,
                )
                Spacer(Modifier.height(12.dp))
                AuthInput(
                    label = stringResource(R.string.reset_new_password),
                    value = password,
                    onChange = { password = it },
                    hint = stringResource(R.string.new_password_hint),
                    password = true,
                    enabled = !state.busy,
                )
            }

            Spacer(Modifier.height(8.dp))
            AuthFeedback(state)
            Spacer(Modifier.height(8.dp))

            if (codeSent) {
                AuthAction(stringResource(R.string.reset_confirm), enabled = !state.busy) {
                    onReset(email, code, password)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(
                        { onRequestCode(email) },
                        enabled = !state.busy,
                        contentPadding = PaddingValues(0.dp),
                    ) { Text(stringResource(R.string.reset_resend), fontSize = 15.sp, color = AuthAccent) }
                }
            } else {
                AuthAction(stringResource(R.string.reset_send_code), enabled = !state.busy) {
                    onRequestCode(email)
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(
                { navigate("login") },
                Modifier.align(Alignment.CenterHorizontally),
                enabled = !state.busy,
            ) { Text(stringResource(R.string.back_to_login), fontSize = 15.sp, color = AuthAccent) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private const val CODE_LENGTH = 6
