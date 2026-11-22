package com.fopgang.umpooling.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.fopgang.umpooling.R

internal fun AuthIssue.stringId(): Int = when (this) {
    AuthIssue.INVALID_EMAIL -> R.string.email_invalid
    AuthIssue.INVALID_FORM -> R.string.form_invalid
    AuthIssue.CREDENTIALS -> R.string.auth_credentials_error
    AuthIssue.UNVERIFIED -> R.string.auth_unverified
    AuthIssue.CODE -> R.string.auth_code_error
    AuthIssue.RATE_LIMIT -> R.string.auth_rate_limit
    AuthIssue.CONFIGURATION -> R.string.auth_configuration
    AuthIssue.SESSION -> R.string.auth_session_error
    AuthIssue.REQUEST -> R.string.auth_request_error
}

@Composable
fun AuthFeedback(state: AuthUiState) {
    if (!state.busy && state.issue == null && state.notice == null) return
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.auth_working))
        }
        state.issue?.let { Text(stringResource(it.stringId()), color = MaterialTheme.colorScheme.error) }
        if (state.notice == AuthNotice.EMAIL_SENT) Text(stringResource(R.string.auth_email_sent))
    }
}
