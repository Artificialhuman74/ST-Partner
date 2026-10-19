package com.sanathanatattva.partner.ui.signup

import com.sanathanatattva.partner.ui.components.ShutterButton
import com.sanathanatattva.partner.ui.components.CameraTrigger
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.sanathanatattva.partner.ui.components.around
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.rememberLoop
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.Anek
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.tween
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.popIn
import com.sanathanatattva.partner.ui.components.riseIn
import com.sanathanatattva.partner.ui.components.rememberEntrance
import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.signup.BaseLabel
import com.sanathanatattva.partner.data.signup.KycPhoto
import com.sanathanatattva.partner.data.signup.Relation
import com.sanathanatattva.partner.data.signup.SignupDraft
import com.sanathanatattva.partner.data.signup.Vehicle
import com.sanathanatattva.partner.ui.components.CameraCapture
import com.sanathanatattva.partner.ui.components.CameraGuide
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.StTextField
import com.sanathanatattva.partner.ui.theme.St
import java.io.File
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

typealias Edit = ((SignupDraft) -> SignupDraft) -> Unit

/** "98765 43210" while typing, stored as plain digits. */
private object MobileSpacing : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = if (raw.length > 5) raw.take(5) + " " + raw.drop(5) else raw
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset > 5) offset + 1 else offset
            override fun transformedToOriginal(offset: Int) = if (offset > 5) offset - 1 else offset
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

// ── 1 · About you ─────────────────────────────────────────────────────

@Composable
fun BasicsStep(draft: SignupDraft, errors: Map<String, String>, onChange: Edit) {
    var shown by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StTextField(
            value = draft.name,
            onValueChange = { v -> onChange { it.copy(name = v) } },
            label = stringResource(R.string.signup_name),
            error = errors["name"],
            valid = draft.name.trim().length >= 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            contentType = ContentType.PersonFullName,
        )
        StTextField(
            value = draft.phone,
            onValueChange = { v -> onChange { it.copy(phone = mobileDigits(v)) } },
            label = stringResource(R.string.signup_phone),
            prefix = "+91",
            error = errors["phone"],
            valid = SignupRules.isMobile(draft.phone),
            visualTransformation = MobileSpacing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            contentType = ContentType.PhoneNumberNational,
        )
        if (draft.viaGoogle) {
            StTextField(
                value = draft.email,
                onValueChange = {},
                label = stringResource(R.string.signup_email_google),
                valid = true,
                readOnly = true,
            )
        } else {
            StTextField(
                value = draft.email,
                onValueChange = { v -> onChange { it.copy(email = v.trim()) } },
                label = stringResource(R.string.signup_email),
                error = errors["email"],
                valid = draft.email.contains('@') && draft.email.substringAfter('@').contains('.'),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                contentType = ContentType.EmailAddress,
            )
            Column {
                StTextField(
                    value = draft.password,
                    onValueChange = { v -> onChange { it.copy(password = v) } },
                    label = stringResource(R.string.signup_password),
                    error = errors["password"],
                    visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                    contentType = ContentType.NewPassword,
                    trailing = {
                        IconButton(onClick = { shown = !shown }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                if (shown) StIcons.EyeOff else StIcons.Eye,
                                contentDescription = stringResource(if (shown) R.string.login_hide_password else R.string.login_show_password),
                                tint = St.Ink2,
                            )
                        }
                    },
                )
                if (errors["password"] == null) PasswordMeter(draft.password)
            }
        }
    }
}

@Composable
private fun PasswordMeter(password: String) {
    val strength = when {
        password.isEmpty() -> 0
        password.length < 8 -> 1
        password.length < 12 && !(password.any(Char::isDigit) && password.any(Char::isLetter)) -> 2
        password.length < 12 -> 3
        else -> 4
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { i ->
                val color = when {
                    i >= strength -> St.Track
                    strength <= 1 -> St.Gold
                    else -> St.Green
                }
                Box(Modifier.size(22.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(color))
            }
        }
        Text(stringResource(R.string.signup_password_hint), style = MaterialTheme.typography.bodySmall, color = St.Ink2)
    }
}

// ── 3 · Selfie ───────────────────────────────────────────────────────

@Composable
fun SelfieStep(draft: SignupDraft, errors: Map<String, String>, newFile: () -> File, onTaken: (File) -> Unit) {
    val path = draft.photos[KycPhoto.Selfie]
    var retaking by remember { mutableStateOf(false) }
    val trigger = remember { CameraTrigger() }
    val capturing = path == null || retaking
    Column {
        if (capturing) {
            CameraCapture(
                front = true,
                guide = CameraGuide.Oval,
                hint = stringResource(R.string.signup_selfie_fit),
                newFile = newFile,
                onCaptured = {
                    retaking = false
                    onTaken(it)
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(350f / 340f),
                trigger = trigger,
            )
        } else {
            val thumb = rememberThumbnail(path, 1080)
            Box(Modifier.fillMaxWidth().aspectRatio(350f / 340f).clip(RoundedCornerShape(28.dp)).background(St.Sand)) {
                if (thumb != null) Image(thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                OutlinedButton(
                    onClick = { retaking = true },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = St.Paper, contentColor = St.Green),
                    border = BorderStroke(2.dp, St.Green),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).heightIn(min = 52.dp),
                ) {
                    Icon(StIcons.Retake, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.signup_retake), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        FieldError(errors["selfie"])
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tip(StIcons.Sun, stringResource(R.string.signup_tip_light), Modifier.weight(1f))
            Tip(StIcons.NoHelmet, stringResource(R.string.signup_tip_helmet), Modifier.weight(1f))
            Tip(StIcons.NoGlasses, stringResource(R.string.signup_tip_glasses), Modifier.weight(1f))
        }
        if (capturing) {
            Spacer(Modifier.height(18.dp))
            ShutterButton(trigger, stringResource(R.string.signup_take_photo), size = 80.dp, inner = 62.dp)
        }
    }
}

@Composable
private fun Tip(icon: ImageVector, text: String, modifier: Modifier) {
    Column(
        modifier.heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp)).background(St.Sand).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp), color = St.Ink)
    }
}

// ── 4 · Vehicle ──────────────────────────────────────────────────────

@Composable
fun VehicleStep(draft: SignupDraft, errors: Map<String, String>, onChange: Edit) {
    val tiles = listOf(
        Triple(Vehicle.Walk, StIcons.Walk, R.string.signup_vehicle_walk),
        Triple(Vehicle.Cycle, StIcons.Cycle, R.string.signup_vehicle_cycle),
        Triple(Vehicle.Scooter, StIcons.Scooter, R.string.signup_vehicle_scooter),
        Triple(Vehicle.Bike, StIcons.Bike, R.string.signup_vehicle_bike),
    )
    Column {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (vehicle, icon, label) ->
                        VehicleTile(
                            icon = icon,
                            label = stringResource(label),
                            selected = draft.vehicle == vehicle,
                            onClick = { onChange { it.copy(vehicle = vehicle) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        FieldError(errors["vehicle"])
        Spacer(Modifier.height(18.dp))
        // Canvas "vh-in": the plate field or the "no licence needed" note rises in (.6 s, 10 dp).
        val enter = rememberEntrance(draft.motorised to (draft.vehicle != null), 600)
        Box(Modifier.riseIn(enter, 10.dp)) { when {
            draft.motorised -> StTextField(
                value = draft.plate,
                onValueChange = { v -> onChange { it.copy(plate = v.uppercase().take(13)) } },
                label = stringResource(R.string.signup_plate),
                error = errors["plate"],
                helper = stringResource(R.string.signup_plate_hint),
                valid = draft.plate.replace(" ", "").length >= 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                leadingBadge = { PlateBadge() },
            )
            draft.vehicle != null -> InlineNote(stringResource(R.string.signup_no_licence), StIcons.Check)
        } }
    }
}

@Composable
private fun PlateBadge() {
    Column(
        Modifier.width(30.dp).height(40.dp).clip(RoundedCornerShape(6.dp)).background(PlateBlue),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text("IND", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = St.Cream, modifier = Modifier.padding(bottom = 3.dp))
    }
}

private val PlateBlue = Color(0xFF1F4FA0)

@Composable
private fun VehicleTile(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(24.dp)
    // Canvas "vh-tile": colours ease over .35 s.
    val border by animateColorAsState(if (selected) St.Green else St.LineSoft, tween(350, easing = OilEase), label = "vb")
    val fill by animateColorAsState(if (selected) St.GreenWash else St.Paper, tween(350, easing = OilEase), label = "vf")
    Box(
        modifier
            .heightIn(min = 128.dp)
            .clip(shape)
            .background(fill)
            .border(if (selected) 3.dp else 2.dp, border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick); onClick() })
            .padding(12.dp),
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = if (selected) St.Green else St.Ink, modifier = Modifier.size(64.dp))
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (selected) St.Green else St.Ink)
        }
        if (selected) {
            // Canvas "vh-pop": the tick badge pops in over .5 s.
            val pop = rememberEntrance(Unit, 500)
            Box(Modifier.align(Alignment.TopEnd).popIn(pop, 0.3f).size(28.dp).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
                Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── 5 · Licence ──────────────────────────────────────────────────────

@Composable
fun LicenceStep(
    draft: SignupDraft,
    errors: Map<String, String>,
    onChange: Edit,
    onShoot: (KycPhoto) -> Unit,
    /** Fixing one side after an admin flagged it: show only that side, not the number. */
    only: KycPhoto? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (only == null || only == KycPhoto.LicenceFront) {
            PhotoTile(stringResource(R.string.signup_front), draft.photos[KycPhoto.LicenceFront], onTake = { onShoot(KycPhoto.LicenceFront) })
        }
        if (only == null || only == KycPhoto.LicenceBack) {
            PhotoTile(stringResource(R.string.signup_back_side), draft.photos[KycPhoto.LicenceBack], onTake = { onShoot(KycPhoto.LicenceBack) })
        }
        FieldError(errors["licence_photos"])
        if (only != null) {
            InlineNote(stringResource(R.string.signup_private_docs), StIcons.Lock)
            return@Column
        }
        Spacer(Modifier.height(4.dp))
        StTextField(
            value = draft.licenceNumber,
            onValueChange = { v -> onChange { it.copy(licenceNumber = v.uppercase().take(20)) } },
            label = stringResource(R.string.signup_licence_number),
            error = errors["licence_number"],
            valid = draft.licenceNumber.replace(" ", "").length >= 8,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        )
        InlineNote(stringResource(R.string.signup_private_docs), StIcons.Lock)
    }
}

// ── 6 · PAN + bank ───────────────────────────────────────────────────

@Composable
fun BankStep(
    draft: SignupDraft,
    errors: Map<String, String>,
    onChange: Edit,
    onShoot: (KycPhoto) -> Unit,
    showPan: Boolean = true,
    showBank: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showPan) PanSection(draft, errors, onChange, onShoot)
        if (showBank) BankSection(draft, errors, onChange, topGap = showPan)
        InlineNote(stringResource(R.string.signup_bank_safety), StIcons.Shield)
    }
}

@Composable
private fun PanSection(draft: SignupDraft, errors: Map<String, String>, onChange: Edit, onShoot: (KycPhoto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(stringResource(R.string.signup_pan_card), StIcons.IdCard)
        PhotoTile(stringResource(R.string.signup_pan_photo), draft.photos[KycPhoto.Pan], onTake = { onShoot(KycPhoto.Pan) })
        FieldError(errors["pan_photo"])
        StTextField(
            value = draft.pan,
            onValueChange = { v -> onChange { it.copy(pan = v.uppercase().filter(Char::isLetterOrDigit).take(10)) } },
            label = stringResource(R.string.signup_pan_number),
            error = errors["pan"],
            valid = SignupRules.isPan(draft.pan),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        )
    }
}

@Composable
private fun BankSection(draft: SignupDraft, errors: Map<String, String>, onChange: Edit, topGap: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(stringResource(R.string.signup_bank_account), StIcons.Bank, Modifier.padding(top = if (topGap) 14.dp else 0.dp))
        StTextField(
            value = draft.accountName,
            onValueChange = { v -> onChange { it.copy(accountName = v) } },
            label = stringResource(R.string.signup_account_name),
            error = errors["account_name"],
            helper = stringResource(R.string.signup_account_name_hint),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )
        StTextField(
            value = draft.accountNumber,
            onValueChange = { v -> onChange { it.copy(accountNumber = v.filter(Char::isDigit).take(18)) } },
            label = stringResource(R.string.signup_account_number),
            error = errors["account_number"],
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        val matches = draft.accountNumber.length >= 9 && draft.accountNumber == draft.accountNumberAgain
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StTextField(
                value = draft.accountNumberAgain,
                onValueChange = { v -> onChange { it.copy(accountNumberAgain = v.filter(Char::isDigit).take(18)) } },
                label = stringResource(R.string.signup_account_again),
                error = errors["account_again"],
                valid = matches,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if (matches) {
                val match = rememberEntrance(Unit, 600, delayMs = 300)
                Text(
                    stringResource(R.string.signup_account_match),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    color = St.Green,
                    modifier = Modifier.riseIn(match, (-6).dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StTextField(
                value = draft.ifsc,
                onValueChange = { v -> onChange { it.copy(ifsc = v.uppercase().filter(Char::isLetterOrDigit).take(11)) } },
                label = stringResource(R.string.signup_ifsc),
                error = errors["ifsc"],
                valid = SignupRules.isIfsc(draft.ifsc),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            )
            if (draft.bankLabel.isNotBlank()) {
                val bank = rememberEntrance(draft.bankLabel, 700, delayMs = 500)
                Row(
                    Modifier.fillMaxWidth().popIn(bank, 0.96f).heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp))
                        .background(St.GreenWash).padding(horizontal = 14.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(20.dp))
                    Text(draft.bankLabel, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = St.Green)
                }
            }
        }
    }
}

// ── 7 · Emergency contact ────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencyStep(draft: SignupDraft, errors: Map<String, String>, onChange: Edit) {
    val context = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) return@rememberLauncherForActivityResult
        // Picking a phone row grants one-time read access to it; no contacts permission needed.
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null,
        )?.use { c ->
            if (c.moveToFirst()) {
                val name = c.getString(0).orEmpty()
                val number = mobileDigits(c.getString(1).orEmpty())
                onChange { it.copy(emergencyName = name, emergencyPhone = number) }
            }
        }
    }
    val relations = listOf(
        Relation.Mother to R.string.relation_mother, Relation.Father to R.string.relation_father,
        Relation.Wife to R.string.relation_wife, Relation.Husband to R.string.relation_husband,
        Relation.Brother to R.string.relation_brother, Relation.Sister to R.string.relation_sister,
        Relation.Friend to R.string.relation_friend, Relation.Other to R.string.relation_other,
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CareCard(draft, relations.firstOrNull { it.first == draft.emergencyRelation }?.second)
        StTextField(
            value = draft.emergencyName,
            onValueChange = { v -> onChange { it.copy(emergencyName = v) } },
            label = stringResource(R.string.signup_emergency_name),
            error = errors["emergency_name"],
            valid = draft.emergencyName.trim().length >= 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            labelEnd = {
                TextButton(onClick = {
                    pick.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                }, modifier = Modifier.heightIn(min = 44.dp)) {
                    Icon(StIcons.Contacts, contentDescription = null, tint = St.Green, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.signup_pick_contact), style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp), color = St.Green)
                }
            },
        )
        Column {
            Text(stringResource(R.string.signup_they_are), style = MaterialTheme.typography.labelMedium, color = St.Ink)
            FlowRow(
                Modifier.padding(top = 10.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                relations.forEach { (relation, label) ->
                    val on = draft.emergencyRelation == relation
                    val haptics = LocalHapticFeedback.current
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (on) St.Green else St.Paper)
                            .border(2.dp, if (on) St.Green else St.Line, RoundedCornerShape(24.dp))
                            .selectable(selected = on, role = Role.RadioButton, onClick = {
                                if (!on) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onChange { it.copy(emergencyRelation = relation) }
                            })
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(label), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = if (on) St.Cream else St.Ink)
                    }
                }
            }
            FieldError(errors["relation"])
        }
        StTextField(
            value = draft.emergencyPhone,
            onValueChange = { v -> onChange { it.copy(emergencyPhone = mobileDigits(v)) } },
            label = stringResource(R.string.signup_emergency_phone),
            prefix = "+91",
            error = errors["emergency_phone"],
            valid = SignupRules.isMobile(draft.emergencyPhone) && draft.emergencyPhone != draft.phone,
            visualTransformation = MobileSpacing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
    }
}

/**
 * Canvas "We look out for you" card: you and your contact, joined by a gold arc that draws
 * itself while an oil drop travels along it ("ec-arc", "ec-drop"), a ripple where it lands
 * ("ec-rip"), then a heart ("ec-heart"). One 6 s loop; the name line rises in ("ec-who").
 */
@Composable
private fun CareCard(draft: SignupDraft, relationLabel: Int?) {
    val first = draft.emergencyName.trim().substringBefore(' ')
    val who = when {
        first.isBlank() -> null
        relationLabel == null || draft.emergencyRelation == Relation.Other -> first
        else -> "$first · ${stringResource(relationLabel)}"
    }
    val me = rememberThumbnail(draft.photos[KycPhoto.Selfie], 240)
    val reduce = LocalReduceMotion.current
    val clock by rememberLoop(6000, still = 0.7f)
    val text = rememberTextMeasurer()
    val youLabel = stringResource(R.string.signup_you)
    val initial = first.take(1).uppercase()
    val ink = St.Ink
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(350f / 120f).clip(RoundedCornerShape(24.dp)).background(St.Sand)
            .semantics(mergeDescendants = true) { contentDescription = who?.let { "$youLabel · $it" } ?: youLabel },
    ) {
        val u = maxWidth / 350f
        Canvas(Modifier.fillMaxSize()) {
            val t = clock
            viewBox(350f, 120f) {
                drawPath(CareArc, CareLine, style = Stroke(2.5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 7f))))
                // The arc draws 8% → 48%, holds, fades at the end.
                val drawn = if (reduce) 1f else kf(t, 0.08f to 0f, 0.48f to 1f, easing = CareEase)
                val arcAlpha = if (reduce) 1f else kf(t, 0.90f to 1f, 1f to 0f)
                if (drawn > 0f) drawPath(partialPath(CareArc, drawn), St.Gold, alpha = arcAlpha, style = Stroke(3f, cap = StrokeCap.Round))

                drawCircle(St.Cream, 31f, Offset(78f, 50f))
                if (me == null) {
                    clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(78f, 50f), 27f)) }) {
                        drawRect(CareYouBg, Offset(50f, 22f), Size(56f, 56f))
                        drawCircle(CareYouInk, 10f, Offset(78f, 44f))
                        drawPath(CareYouBody, CareYouInk)
                    }
                }
                drawCircle(St.Green, 29f, Offset(78f, 50f), style = Stroke(3f))

                if (!reduce) {
                    val rip = kf(t, 0.48f to 0f, 0.52f to 1f, 0.80f to 2f, easing = OilEase)
                    if (rip > 0f && rip < 2f) {
                        val a = if (rip <= 1f) 0.9f * rip else 0.9f * (2f - rip)
                        val r = 31f * (if (rip <= 1f) 1f else 1f + 0.45f * (rip - 1f))
                        drawCircle(St.Gold, r, Offset(272f, 50f), alpha = a, style = Stroke(3f))
                    }
                }
                drawCircle(St.Cream, 31f, Offset(272f, 50f))
                drawCircle(CareThemBg, 27f, Offset(272f, 50f))
                drawCircle(St.Gold, 29f, Offset(272f, 50f), style = Stroke(3f))

                // The drop travels the arc 8% → 50%, fading in and out at its ends.
                if (!reduce) {
                    val along = kf(t, 0.08f to 0f, 0.12f to 0.04f, 0.46f to 0.96f, 0.50f to 1f, easing = CareEase)
                    val dropAlpha = kf(t, 0.08f to 0f, 0.12f to 1f, 0.46f to 1f, 0.50f to 0f)
                    if (dropAlpha > 0f) {
                        val pos = CareArcMeasure.getPosition(CareArcMeasure.length * along)
                        translate(pos.x - 7f, pos.y - 9f) {
                            drawPath(CareDrop, St.Gold, alpha = dropAlpha)
                            drawOval(St.GoldGlint, Offset(4.6f - 1.3f, 11.5f - 2.4f), Size(2.6f, 4.8f), alpha = dropAlpha)
                        }
                    }
                }
                // The heart badge pops on the contact 50% → 60%, leaves at the end.
                val heart = if (reduce) 1f else kf(t, 0.50f to 0f, 0.60f to 1f, easing = OilEase)
                val heartOut = if (reduce) 1f else kf(t, 0.92f to 1f, 1f to 0f)
                if (heart > 0f) {
                    val sc = 0.3f + 0.7f * heart
                    around(299f, 27f + 6f * (1f - heart), sx = sc) {
                        drawCircle(St.Sand, 15f, Offset(299f, 27f + 6f * (1f - heart)), alpha = heart * heartOut)
                        drawCircle(St.Green, 12f, Offset(299f, 27f + 6f * (1f - heart)), alpha = heart * heartOut)
                        translate(299f - 7f, 27f + 6f * (1f - heart) - 7f) {
                            scale(14f / 24f, 14f / 24f, pivot = Offset.Zero) { drawPath(CareHeart, St.Cream, alpha = heart * heartOut) }
                        }
                    }
                }
            }
            // Text in screen pixels (a viewBox would scale the glyphs too): centred on the canvas x, baseline at its y.
            val k = size.width / 350f
            if (initial.isNotEmpty()) {
                val m = text.measure(initial, TextStyle(fontFamily = Anek, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = CareInitial))
                drawText(m, topLeft = Offset(272f * k - m.size.width / 2f, 60f * k - m.firstBaseline))
            }
            val y = text.measure(youLabel, TextStyle(fontFamily = Anek, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ink))
            drawText(y, topLeft = Offset(78f * k - y.size.width / 2f, 106f * k - y.firstBaseline))
        }
        if (me != null) {
            // Your selfie inside the "you" ring (27 of 350 units).
            Image(
                me, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.offset(x = u * (78f - 27f), y = u * (50f - 27f)).size(u * 54f).clip(CircleShape),
            )
        }
        if (who != null) {
            val rise = rememberEntrance(who, 500)
            Box(Modifier.offset(x = u * 196f, y = u * 92f).width(u * 150f).riseIn(rise, 4.dp), contentAlignment = Alignment.TopCenter) {
                Text(who, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Ink, maxLines = 1, softWrap = false)
            }
        }
    }
}

private val CareArc = svgPath("M112 50 Q175 2 238 50")
private val CareArcMeasure = PathMeasure().apply { setPath(CareArc, false) }
private val CareDrop = svgPath("M7 0.5C7 0.5 1 7.5 1 11.5a6 6 0 0 0 12 0C13 7.5 7 0.5 7 0.5z")
private val CareHeart = svgPath("M12 21s-8.5-5.2-8.5-11.2A4.8 4.8 0 0 1 12 6.9a4.8 4.8 0 0 1 8.5 2.9C20.5 15.8 12 21 12 21z")
private val CareYouBody = svgPath("M58 78c2-12 10-18 20-18s18 6 20 18z")
private val CareEase = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)
private val CareLine = Color(0xFFD8C8AB)
private val CareYouBg = Color(0xFFCFE0C9)
private val CareYouInk = Color(0xFF5F7D5A)
private val CareThemBg = Color(0xFFFBEFD2)
private val CareInitial = Color(0xFF7A4F00)

private fun partialPath(path: Path, fraction: Float): Path {
    val measure = PathMeasure().apply { setPath(path, false) }
    return Path().also { measure.getSegment(0f, measure.length * fraction, it, true) }
}

// ── 8 · Check and send ───────────────────────────────────────────────

@Composable
fun ReviewStep(draft: SignupDraft, onEdit: (Step) -> Unit, onChange: Edit) {
    val selfie = rememberThumbnail(draft.photos[KycPhoto.Selfie], 240)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(24.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(St.GreenMist)) {
                if (selfie != null) Image(selfie, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f)) {
                Text(draft.name.trim(), style = MaterialTheme.typography.titleLarge, color = St.Ink)
                Text("+91 " + prettyMobile(draft.phone), style = MaterialTheme.typography.bodyMedium, color = St.Ink2)
                val vehicleText = when (draft.vehicle) {
                    null -> ""
                    Vehicle.Walk -> stringResource(R.string.signup_vehicle_walk)
                    Vehicle.Cycle -> stringResource(R.string.signup_vehicle_cycle)
                    else -> draft.plate.trim()
                }
                if (vehicleText.isNotBlank()) Text(vehicleText, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.04.sp), color = St.Ink)
            }
            EditLink { onEdit(Step.Basics) }
        }
        ReviewRow(StIcons.Pin, stringResource(R.string.signup_review_base), baseSummary(draft)) { onEdit(Step.Base) }
        ReviewRow(vehicleIcon(draft.vehicle), stringResource(R.string.signup_review_vehicle), vehicleName(draft.vehicle)) { onEdit(Step.Vehicle) }
        if (draft.motorised) {
            ReviewRow(StIcons.IdCard, stringResource(R.string.signup_review_licence), stringResource(R.string.signup_review_licence_ok) + " · " + draft.licenceNumber.trim()) { onEdit(Step.Licence) }
        }
        val bankName = draft.bankLabel.substringBefore(" · ").ifBlank { draft.ifsc }
        ReviewRow(StIcons.Bank, stringResource(R.string.signup_review_bank), "$bankName ···· ${draft.accountNumber.takeLast(4)}") { onEdit(Step.Bank) }
        val relation = when (draft.emergencyRelation) {
            null -> ""
            else -> " · " + stringResource(relationLabel(draft.emergencyRelation))
        }
        ReviewRow(StIcons.Heart, stringResource(R.string.signup_review_emergency), draft.emergencyName.trim().substringBefore(' ') + relation) { onEdit(Step.Emergency) }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(18.dp))
                .toggleable(value = draft.confirmed, role = Role.Checkbox, onValueChange = { v -> onChange { it.copy(confirmed = v) } })
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (draft.confirmed) St.Green else St.Paper)
                    .border(2.5.dp, if (draft.confirmed) St.Green else St.FieldBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (draft.confirmed) {
                    val tick = rememberEntrance(Unit, 450)
                    Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(18.dp).popIn(tick, 0.4f))
                }
            }
            Text(stringResource(R.string.signup_confirm), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = St.Ink)
        }
    }
}

@Composable
private fun baseSummary(d: SignupDraft): String {
    val label = when (d.baseLabel) {
        BaseLabel.Home -> stringResource(R.string.signup_base_home)
        BaseLabel.Shop -> stringResource(R.string.signup_base_shop)
        BaseLabel.Other -> stringResource(R.string.signup_base_other)
    }
    return label + " · " + d.baseAddress.substringBefore(',').ifBlank { d.baseAddress }
}

@Composable
private fun vehicleName(v: Vehicle?): String = when (v) {
    Vehicle.Walk -> stringResource(R.string.signup_vehicle_walk)
    Vehicle.Cycle -> stringResource(R.string.signup_vehicle_cycle)
    Vehicle.Scooter -> stringResource(R.string.signup_vehicle_scooter)
    Vehicle.Bike -> stringResource(R.string.signup_vehicle_bike)
    null -> ""
}

private fun vehicleIcon(v: Vehicle?): ImageVector = when (v) {
    Vehicle.Walk -> StIcons.Walk
    Vehicle.Cycle -> StIcons.Cycle
    Vehicle.Bike -> StIcons.Bike
    else -> StIcons.Scooter
}

fun relationLabel(r: Relation): Int = when (r) {
    Relation.Mother -> R.string.relation_mother
    Relation.Father -> R.string.relation_father
    Relation.Wife -> R.string.relation_wife
    Relation.Husband -> R.string.relation_husband
    Relation.Brother -> R.string.relation_brother
    Relation.Sister -> R.string.relation_sister
    Relation.Friend -> R.string.relation_friend
    Relation.Other -> R.string.relation_other
}

@Composable
private fun ReviewRow(icon: ImageVector, title: String, detail: String, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(St.Paper).border(2.dp, St.LineSoft, RoundedCornerShape(20.dp))
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = St.Ink2, maxLines = 2)
        }
        EditLink(onEdit)
    }
}

@Composable
private fun EditLink(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.signup_base_edit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Green)
    }
}
