package com.sanathanatattva.partner.ui.review

import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.ui.components.riseIn
import com.sanathanatattva.partner.ui.components.rememberEntrance
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.ReviewIssue
import com.sanathanatattva.partner.data.api.RiderDetails
import com.sanathanatattva.partner.data.signup.BaseLabel
import com.sanathanatattva.partner.data.signup.KycPhoto
import com.sanathanatattva.partner.data.signup.Relation
import com.sanathanatattva.partner.data.signup.ReviewItem
import com.sanathanatattva.partner.data.signup.RiderRepository
import com.sanathanatattva.partner.data.signup.SignupDraft
import com.sanathanatattva.partner.data.signup.Vehicle
import com.sanathanatattva.partner.ui.components.Emblem
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.login.UiText
import com.sanathanatattva.partner.ui.login.dialSupport
import com.sanathanatattva.partner.ui.login.text
import com.sanathanatattva.partner.ui.login.toUiText
import com.sanathanatattva.partner.ui.signup.BankStep
import com.sanathanatattva.partner.ui.signup.BaseStep
import com.sanathanatattva.partner.ui.signup.DocumentCamera
import com.sanathanatattva.partner.ui.signup.EmergencyStep
import com.sanathanatattva.partner.ui.signup.LicenceStep
import com.sanathanatattva.partner.ui.signup.SelfieStep
import com.sanathanatattva.partner.ui.signup.SignupFrame
import com.sanathanatattva.partner.ui.signup.SignupRules
import com.sanathanatattva.partner.ui.signup.Step
import com.sanathanatattva.partner.ui.signup.VehicleStep
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

private val IssueRose = Color(0xFFEFC8BB)

class RejectedViewModel(private val riders: RiderRepository) : ViewModel() {
    var loading by mutableStateOf(true)
        private set
    var issues by mutableStateOf<List<ReviewIssue>>(emptyList())
        private set
    var draft by mutableStateOf(SignupDraft())
        private set
    var fixed by mutableStateOf<Set<String>>(emptySet())
        private set
    var fixing by mutableStateOf<String?>(null)
        private set
    var fixErrors by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<UiText?>(null)
        private set

    private val resent = Channel<Unit>(Channel.BUFFERED)
    val resentEvents = resent.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val r = riders.application()) {
                is ApiResult.Success -> {
                    issues = r.value.reviewIssues
                    r.value.application?.let { draft = prefill(it) }
                }
                is ApiResult.Failure -> error = r.problem.toUiText()
            }
            loading = false
        }
    }

    fun update(change: (SignupDraft) -> SignupDraft) {
        draft = change(draft)
        fixErrors = emptyMap()
    }

    fun photo(kind: KycPhoto, file: File) {
        draft.photos[kind]?.let { if (it != file.path) File(it).delete() }
        update { it.copy(photos = it.photos + (kind to file.path)) }
    }

    fun newPhotoFile(kind: KycPhoto): File = riders.drafts.newPhotoFile(kind)

    fun startFix(item: String) {
        fixErrors = emptyMap()
        fixing = item
    }

    fun closeFix() {
        fixing = null
    }

    fun saveFix() {
        val item = fixing ?: return
        val problems = problemsFor(item, draft)
        if (problems.isNotEmpty()) {
            fixErrors = problems
            return
        }
        fixed = fixed + item
        fixing = null
    }

    fun redo(item: String) {
        fixed = fixed - item
        startFix(item)
    }

    fun send() {
        if (sending || fixed.size < issues.size) return
        sending = true
        error = null
        viewModelScope.launch {
            when (val r = riders.resubmit(draft, issues.map { it.item }.toSet())) {
                is ApiResult.Success -> {
                    draft.photos.values.forEach { File(it).delete() }
                    resent.send(Unit)
                }
                is ApiResult.Failure -> error = r.problem.toUiText()
            }
            sending = false
        }
    }

    private fun prefill(a: RiderDetails) = SignupDraft(
        name = a.name,
        phone = a.phone,
        email = a.email,
        baseLabel = BaseLabel.entries.firstOrNull { it.wire == a.base?.label } ?: BaseLabel.Home,
        baseAddress = a.base?.address.orEmpty(),
        baseLat = a.base?.latitude,
        baseLng = a.base?.longitude,
        vehicle = Vehicle.from(a.vehicleType),
        plate = a.vehiclePlate.orEmpty(),
        licenceNumber = a.licenceNumber.orEmpty(),
        pan = a.pan.orEmpty(),
        accountName = a.bankAccountName.orEmpty(),
        ifsc = a.bankIfsc.orEmpty(),
        emergencyName = a.emergencyName.orEmpty(),
        emergencyRelation = Relation.from(a.emergencyRelation),
        emergencyPhone = a.emergencyPhone.orEmpty(),
    )

    companion object {
        /** What each flagged item needs before it counts as fixed. */
        fun problemsFor(item: String, d: SignupDraft): Map<String, Int> = when (item) {
            ReviewItem.SELFIE -> SignupRules.check(Step.Selfie, d)
            ReviewItem.LICENCE_FRONT -> if (d.photo(KycPhoto.LicenceFront) == null) mapOf("licence_photos" to R.string.check_licence_photos) else emptyMap()
            ReviewItem.LICENCE_BACK -> if (d.photo(KycPhoto.LicenceBack) == null) mapOf("licence_photos" to R.string.check_licence_photos) else emptyMap()
            ReviewItem.PAN -> SignupRules.check(Step.Bank, d).filterKeys { it == "pan" || it == "pan_photo" }
            ReviewItem.BANK -> SignupRules.check(Step.Bank, d).filterKeys { it != "pan" && it != "pan_photo" }
            ReviewItem.VEHICLE -> SignupRules.check(Step.Vehicle, d)
            ReviewItem.EMERGENCY -> SignupRules.check(Step.Emergency, d)
            ReviewItem.BASE -> SignupRules.check(Step.Base, d)
            else -> emptyMap()
        }
    }
}

private data class IssueLook(val icon: ImageVector, val title: Int, val action: Int, val camera: Boolean)

private fun lookOf(item: String) = when (item) {
    ReviewItem.SELFIE -> IssueLook(StIcons.Camera, R.string.issue_selfie, R.string.fix_retake_selfie, true)
    ReviewItem.LICENCE_FRONT -> IssueLook(StIcons.IdCard, R.string.issue_licence_front, R.string.fix_retake_licence, true)
    ReviewItem.LICENCE_BACK -> IssueLook(StIcons.IdCard, R.string.issue_licence_back, R.string.fix_retake_licence, true)
    ReviewItem.PAN -> IssueLook(StIcons.IdCard, R.string.issue_pan, R.string.fix_retake_pan, true)
    ReviewItem.BANK -> IssueLook(StIcons.Bank, R.string.issue_bank, R.string.fix_bank, false)
    ReviewItem.VEHICLE -> IssueLook(StIcons.Scooter, R.string.issue_vehicle, R.string.fix_vehicle, false)
    ReviewItem.EMERGENCY -> IssueLook(StIcons.Heart, R.string.issue_emergency, R.string.fix_emergency, false)
    else -> IssueLook(StIcons.Pin, R.string.issue_base, R.string.fix_base, false)
}

/** Sent back with reasons (canvas "Rejected"): one card per flagged item, fix each, send again. */
@Composable
fun RejectedScreen(onResent: () -> Unit, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val logOutHaptics = LocalHapticFeedback.current
    val vm: RejectedViewModel = viewModel { RejectedViewModel(container.riders) }
    LaunchedEffect(vm) { vm.resentEvents.collect { onResent() } }
    var shooting by remember { mutableStateOf<KycPhoto?>(null) }

    val fixing = vm.fixing
    if (fixing != null) {
        FixOverlay(vm, fixing, onShoot = { shooting = it })
        shooting?.let { kind ->
            DocumentCamera(
                hint = stringResource(R.string.signup_card_fit),
                newFile = { vm.newPhotoFile(kind) },
                onCaptured = { vm.photo(kind, it); shooting = null },
                onClose = { shooting = null },
            )
        }
        return
    }

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Emblem(40.dp)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = St.GreenDeep, modifier = Modifier.weight(1f))
            TextButton(onClick = { logOutHaptics.performHapticFeedback(HapticFeedbackType.ToggleOff); scope.launch { container.auth.logOut(); onLoggedOut() } }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.review_log_out), style = MaterialTheme.typography.labelMedium, color = St.Green)
            }
        }
        if (vm.loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = St.Green) }
            return@Column
        }
        val n = vm.issues.size
        val done = vm.fixed.size
        val left = n - done
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                if (left == 0) stringResource(R.string.rejected_all_fixed) else pluralStringResource(R.plurals.rejected_heading, n, n),
                style = MaterialTheme.typography.headlineMedium,
                color = St.Ink,
                modifier = Modifier.padding(top = 18.dp).semantics { heading() },
            )
            Text(
                stringResource(
                    when {
                        left == 0 -> R.string.rejected_sub_done
                        n == 1 -> R.string.rejected_sub_one
                        else -> R.string.rejected_sub_many
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = St.Ink2,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.issues.forEach { issue ->
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (issue.item in vm.fixed) St.Green else IssueRose))
                }
            }
            Text(stringResource(R.string.rejected_progress, done, n), style = MaterialTheme.typography.labelMedium, color = St.Ink2, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                vm.issues.forEachIndexed { index, issue ->
                    val look = lookOf(issue.item)
                    if (issue.item in vm.fixed) {
                        // Canvas "rj-in": a fixed item settles in as a green row (.55 s, from 8 dp below).
                        val settle = rememberEntrance(issue.item, 550)
                        Row(
                            Modifier.fillMaxWidth().riseIn(settle, 8.dp).clip(RoundedCornerShape(20.dp)).background(St.GreenWash).padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
                                Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(16.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(look.title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Ink)
                                Text(stringResource(R.string.rejected_fixed), style = MaterialTheme.typography.bodySmall, color = St.Green)
                            }
                            TextButton(onClick = { vm.redo(issue.item) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.rejected_redo), color = St.Green, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(St.Paper).border(2.dp, IssueRose, RoundedCornerShape(22.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                IssueArt(issue.item, index, look.icon)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(look.title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                                    Text(issue.reason, style = MaterialTheme.typography.bodyMedium, color = St.Ink3)
                                }
                            }
                            OutlinedButton(
                                onClick = { vm.startFix(issue.item) },
                                border = BorderStroke(2.dp, St.Green),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = St.GreenWash, contentColor = St.Green),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            ) {
                                Icon(if (look.camera) StIcons.Camera else StIcons.Edit, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.size(10.dp))
                                Text(stringResource(look.action), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
            val flagged = vm.issues.map { it.item }.toSet()
            val fine = buildList {
                if (ReviewItem.SELFIE !in flagged) add(R.string.issue_selfie)
                if (ReviewItem.BASE !in flagged) add(R.string.issue_base)
                if (ReviewItem.VEHICLE !in flagged) add(R.string.issue_vehicle)
                if (vm.draft.motorised && ReviewItem.LICENCE_FRONT !in flagged && ReviewItem.LICENCE_BACK !in flagged) add(R.string.signup_review_licence)
                if (ReviewItem.PAN !in flagged) add(R.string.issue_pan)
                if (ReviewItem.BANK !in flagged) add(R.string.issue_bank)
                if (ReviewItem.EMERGENCY !in flagged) add(R.string.issue_emergency)
            }
            if (fine.isNotEmpty()) {
                Text(stringResource(R.string.rejected_fine), style = MaterialTheme.typography.labelMedium, color = St.Ink2, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                fine.forEach { label ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(18.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = St.Ink)
                    }
                }
            }
            vm.error?.let { Text(text(it), color = St.Brick, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(12.dp))
        }
        PrimaryButton(
            text = if (left == 0) stringResource(R.string.rejected_send) else pluralStringResource(R.plurals.rejected_left, left, left),
            onClick = vm::send,
            enabled = left == 0 && n > 0,
            loading = vm.sending,
        )
        TextButton(onClick = { context.dialSupport() }, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.call_support), color = St.Green, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun FixOverlay(vm: RejectedViewModel, item: String, onShoot: (KycPhoto) -> Unit) {
    BackHandler(onBack = vm::closeFix)
    val look = lookOf(item)
    val reason = vm.issues.firstOrNull { it.item == item }?.reason
    val errors = vm.fixErrors.mapValues { stringResource(it.value) }
    SignupFrame(
        stepNumber = 0,
        stepCount = 0,
        title = stringResource(look.title),
        body = reason,
        onBack = vm::closeFix,
        action = stringResource(R.string.fix_save),
        onAction = vm::saveFix,
    ) {
        val d = vm.draft
        when (item) {
            ReviewItem.SELFIE -> SelfieStep(d, errors, newFile = { vm.newPhotoFile(KycPhoto.Selfie) }, onTaken = { vm.photo(KycPhoto.Selfie, it) })
            ReviewItem.LICENCE_FRONT -> LicenceStep(d, errors, vm::update, onShoot, only = KycPhoto.LicenceFront)
            ReviewItem.LICENCE_BACK -> LicenceStep(d, errors, vm::update, onShoot, only = KycPhoto.LicenceBack)
            ReviewItem.PAN -> BankStep(d, errors, vm::update, onShoot, showBank = false)
            ReviewItem.BANK -> BankStep(d, errors, vm::update, onShoot, showPan = false)
            ReviewItem.VEHICLE -> VehicleStep(d, errors, vm::update)
            ReviewItem.EMERGENCY -> EmergencyStep(d, errors, vm::update)
            ReviewItem.BASE -> BaseStep(d, errors, vm::update)
        }
    }
}
