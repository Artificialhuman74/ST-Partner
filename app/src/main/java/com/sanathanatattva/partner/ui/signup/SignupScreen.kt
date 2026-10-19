package com.sanathanatattva.partner.ui.signup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.User
import com.sanathanatattva.partner.data.signup.KycPhoto
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.login.text
import com.sanathanatattva.partner.ui.theme.St

/** Join as a delivery partner: 8 steps (7 without a licence), saved as the rider goes. */
@Composable
fun SignupScreen(viaGoogle: Boolean, onLeave: () -> Unit, onSubmitted: (User) -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val vm: SignupViewModel = viewModel { SignupViewModel(container.riders, container.google, container.banks, viaGoogle) }
    LaunchedEffect(vm) { vm.submittedEvents.collect(onSubmitted) }
    if (!vm.loaded) return

    var shooting by rememberSaveable { mutableStateOf<KycPhoto?>(null) }
    // Signing up with Google: first say what Google already gave us and what is still needed.
    var welcomed by rememberSaveable { mutableStateOf(!viaGoogle || vm.draft.phone.isNotBlank()) }
    if (!welcomed) {
        MissingInfoScreen(vm.draft.name, vm.draft.email, onContinue = { welcomed = true }, onBack = onLeave)
        return
    }
    BackHandler(enabled = shooting == null) { if (!vm.back()) onLeave() }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = vm.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (fadeIn(tween(320, easing = OilEase)) + slideInHorizontally(tween(420, easing = OilEase)) { w -> if (forward) w / 10 else -w / 10 }) togetherWith
                    fadeOut(tween(160))
            },
            label = "step",
        ) { step ->
            val errors = vm.errors.mapValues { text(it.value) }
            val draft = vm.draft
            val stepsNow = vm.steps
            val number = stepsNow.indexOf(step) + 1
            val (title, body) = when (step) {
                Step.Basics -> R.string.signup_basics_title to R.string.signup_basics_body
                Step.Base -> R.string.signup_base_title to R.string.signup_base_body
                Step.Selfie -> R.string.signup_selfie_title to R.string.signup_selfie_body
                Step.Vehicle -> R.string.signup_vehicle_title to R.string.signup_vehicle_body
                Step.Licence -> R.string.signup_licence_title to R.string.signup_licence_body
                Step.Bank -> R.string.signup_bank_title to R.string.signup_bank_body
                Step.Emergency -> R.string.signup_emergency_title to R.string.signup_emergency_body
                Step.Review -> R.string.signup_review_title to R.string.signup_review_body
            }
            val review = step == Step.Review
            SignupFrame(
                stepNumber = number,
                stepCount = stepsNow.size,
                title = stringResource(title),
                body = stringResource(body),
                onBack = { if (!vm.back()) onLeave() },
                action = stringResource(
                    when {
                        review && vm.sending -> R.string.signup_sending
                        review -> R.string.signup_send
                        step == Step.Base -> R.string.signup_base_next
                        else -> R.string.signup_next
                    },
                ),
                onAction = { if (review) vm.submit() else vm.next() },
                actionLoading = review && vm.sending,
                actionEnabled = !review || draft.confirmed,
                footer = vm.sendError?.let { problem ->
                    {
                        Text(
                            text(problem),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = St.Brick,
                            modifier = Modifier.padding(bottom = 10.dp).semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                },
            ) {
                when (step) {
                    Step.Basics -> BasicsStep(draft, errors, vm::update)
                    Step.Base -> BaseStep(draft, errors, vm::update)
                    Step.Selfie -> SelfieStep(draft, errors, newFile = { vm.newPhotoFile(KycPhoto.Selfie) }, onTaken = { vm.photoTaken(KycPhoto.Selfie, it) })
                    Step.Vehicle -> VehicleStep(draft, errors, vm::update)
                    Step.Licence -> LicenceStep(draft, errors, vm::update, onShoot = { shooting = it })
                    Step.Bank -> BankStep(draft, errors, vm::update, onShoot = { shooting = it })
                    Step.Emergency -> EmergencyStep(draft, errors, vm::update)
                    Step.Review -> ReviewStep(draft, onEdit = vm::edit, onChange = vm::update)
                }
            }
        }

        shooting?.let { kind ->
            DocumentCamera(
                hint = stringResource(R.string.signup_card_fit),
                newFile = { vm.newPhotoFile(kind) },
                onCaptured = { file ->
                    vm.photoTaken(kind, file)
                    shooting = null
                },
                onClose = { shooting = null },
            )
        }
    }
}
