package com.sanathanatattva.partner.ui.login

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.StTextField
import com.sanathanatattva.partner.ui.theme.St

// Neither sheet has a canvas design yet (handoff §10.1): they reuse the Login sheet's parts.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StSheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = St.Cream,
        contentColor = St.Ink,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) { content() }
    }
}

@Composable
internal fun ForgotPasswordSheet(vm: LoginViewModel, prefill: String, onClose: () -> Unit) {
    var address by rememberSaveable { mutableStateOf(prefill) }
    StSheet(onClose) {
        val sentTo = vm.resetSentTo
        Text(
            stringResource(if (sentTo == null) R.string.forgot_title else R.string.forgot_sent_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        if (sentTo == null) {
            Text(stringResource(R.string.forgot_body), style = MaterialTheme.typography.bodyLarge, color = St.Ink2)
            Spacer(Modifier.height(18.dp))
            StTextField(
                value = address,
                onValueChange = { address = it },
                label = stringResource(R.string.login_email),
                icon = StIcons.Mail,
                error = vm.resetError?.let { text(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onSend = { vm.sendReset(address) }),
                contentType = ContentType.EmailAddress,
            )
            Spacer(Modifier.height(22.dp))
            PrimaryButton(
                text = stringResource(R.string.forgot_send),
                loading = vm.resetSending,
                onClick = { vm.sendReset(address) },
            )
        } else {
            Text(stringResource(R.string.forgot_sent_body, sentTo), style = MaterialTheme.typography.bodyLarge, color = St.Ink2)
            Spacer(Modifier.height(22.dp))
            PrimaryButton(text = stringResource(R.string.close), arrow = false, onClick = onClose)
        }
    }
}

@Composable
internal fun NoPartnerAccountSheet(email: String, onJoin: () -> Unit, onClose: () -> Unit) {
    StSheet(onClose) {
        Text(
            stringResource(R.string.login_google_no_account_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.login_google_no_account_body, email.ifBlank { stringResource(R.string.login_google_this_account) }),
            style = MaterialTheme.typography.bodyLarge,
            color = St.Ink2,
        )
        Spacer(Modifier.height(22.dp))
        PrimaryButton(text = stringResource(R.string.signup_join_google), onClick = onJoin)
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.close), style = MaterialTheme.typography.titleMedium, color = St.Green)
        }
    }
}

/** Opens the dialler with ST support filled in; the person still taps call. */
fun Context.dialSupport() {
    val intent = Intent(Intent.ACTION_DIAL, "tel:${getString(R.string.support_phone)}".toUri())
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Tablets without telephony: nothing sensible to open.
    }
}
