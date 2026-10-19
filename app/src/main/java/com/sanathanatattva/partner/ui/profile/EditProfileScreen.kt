package com.sanathanatattva.partner.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.ui.components.BackBar
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.StTextField
import com.sanathanatattva.partner.ui.login.UiText
import com.sanathanatattva.partner.ui.login.text
import com.sanathanatattva.partner.ui.login.toUiText
import com.sanathanatattva.partner.ui.signup.mobileDigits
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Edit profile (not on the canvas; built from the signup fields): name and mobile. Email and vehicle go through support. */
@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var fields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        container.sessions.current()?.user?.let { u ->
            name = u.name
            // Stored numbers come in every shape ("+91 98765 43210", "919876543210", "0987…").
            phone = mobileDigits(u.phone.orEmpty())
            email = u.email
        }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 22.dp)) {
        BackBar(stringResource(R.string.edit_title), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StTextField(
                value = name,
                onValueChange = { name = it; fields = fields - "name" },
                label = stringResource(R.string.edit_name),
                icon = StIcons.Person,
                error = fields["name"],
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                contentType = ContentType.PersonFullName,
            )
            StTextField(
                value = phone,
                // Same forgiving parse as signup, so a pasted or autofilled "+91 …" lands as the 10 digits.
                onValueChange = { v -> phone = mobileDigits(v); fields = fields - "phone" },
                label = stringResource(R.string.edit_phone),
                icon = StIcons.Phone2,
                prefix = "+91",
                error = fields["phone"],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                contentType = ContentType.PhoneNumberNational,
            )
            StTextField(value = email, onValueChange = {}, label = stringResource(R.string.edit_email), readOnly = true, helper = stringResource(R.string.edit_email_note))
            error?.let { Text(text(it), color = St.Brick, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp)) }
        }
        PrimaryButton(
            text = stringResource(R.string.edit_save),
            arrow = false,
            loading = saving,
            done = saved,
            onClick = {
                saving = true
                error = null
                scope.launch {
                    when (val r = container.partner.saveProfile(name, phone)) {
                        is ApiResult.Success -> {
                            container.sessions.current()?.user?.let { u -> container.sessions.updateUser(u.copy(name = r.value.name, phone = r.value.phone)) }
                            // Let the tick land before leaving, so the save is felt and seen.
                            saved = true
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            delay(750)
                            onBack()
                        }
                        is ApiResult.Failure -> {
                            val p = r.problem
                            fields = (p as? ApiProblem.Server)?.fields.orEmpty()
                            if (fields.isEmpty()) error = p.toUiText()
                        }
                    }
                    saving = false
                }
            },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
