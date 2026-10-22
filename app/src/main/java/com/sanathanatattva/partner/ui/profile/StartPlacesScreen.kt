package com.sanathanatattva.partner.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.Place
import com.sanathanatattva.partner.data.api.PlaceBody
import com.sanathanatattva.partner.data.signup.BaseLabel
import com.sanathanatattva.partner.data.signup.SignupDraft
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.BackBar
import com.sanathanatattva.partner.ui.components.box
import com.sanathanatattva.partner.ui.components.dashedBorder
import com.sanathanatattva.partner.ui.components.dot
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.line
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.login.UiText
import com.sanathanatattva.partner.ui.login.text
import com.sanathanatattva.partner.ui.login.toUiText
import com.sanathanatattva.partner.ui.signup.BaseStep
import com.sanathanatattva.partner.ui.signup.SignupFrame
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch

/** Start places (canvas "Start places"): saved places to go online from, one is the default. */
@Composable
fun StartPlacesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.appContainer.partner
    val scope = rememberCoroutineScope()
    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var editing by remember { mutableStateOf<Place?>(null) }
    var adding by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    fun reload() = scope.launch { (repo.places() as? ApiResult.Success)?.let { places = it.value } }
    LaunchedEffect(Unit) { reload() }

    if (adding || editing != null) {
        PlaceEditor(
            place = editing,
            canDelete = editing != null && places.size > 1,
            onClose = { adding = false; editing = null },
            onSaved = { adding = false; editing = null; reload() },
        )
        return
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 22.dp)) {
        BackBar(stringResource(R.string.profile_start_places), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.places_title), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp), color = St.Ink, modifier = Modifier.padding(start = 4.dp, top = 8.dp).semantics { heading() })
            Text(stringResource(R.string.places_body), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), color = St.Ink2, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                places.forEachIndexed { i, p ->
                    PlaceCard(p, i, onEdit = { editing = p }, onMakeDefault = {
                        scope.launch {
                            when (val r = repo.makeDefault(p.id)) {
                                is ApiResult.Success -> reload()
                                is ApiResult.Failure -> error = r.problem.toUiText()
                            }
                        }
                    })
                }
            }
            error?.let { Text(text(it), color = St.Brick, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
            if (places.size < 5) {
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp))
                        .dashedBorder(2.5.dp, St.Dash, 20.dp).clickable { adding = true },
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(StIcons.Plus, contentDescription = null, tint = St.Green, modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.places_add), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Green)
                }
            }
            Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(St.GreenTint).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(StIcons.Locate, contentDescription = null, tint = St.Green, modifier = Modifier.padding(top = 1.dp).size(22.dp))
                Text(stringResource(R.string.places_note), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = St.Green)
            }
        }
    }
}

@Composable
private fun PlaceCard(p: Place, index: Int, onEdit: () -> Unit, onMakeDefault: () -> Unit) {
    // "sp-card": the border eases to green when this becomes the default.
    val border by animateColorAsState(if (p.isDefault) St.Green else St.BeigeLine, tween(300, easing = OilEase), label = "b")
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(St.Beige).border(2.dp, border, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniMap(index, Modifier.size(92.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(when (p.label) { "shop" -> R.string.label_shop; "other" -> R.string.label_other; else -> R.string.label_home }),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink,
                    )
                    if (p.isDefault) DefaultChip()
                }
                Text(p.address, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink2, maxLines = 3)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(St.Cream)
                    .border(2.dp, St.Green, RoundedCornerShape(14.dp)).clickable(onClick = onEdit),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(StIcons.Edit, contentDescription = null, tint = St.Green, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.signup_base_edit), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Green)
            }
            if (!p.isDefault) {
                Box(
                    Modifier.weight(1.4f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(St.Green).clickable(onClick = onMakeDefault),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.places_make_default), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = St.Cream)
                }
            }
        }
    }
}

/** "sp-chip": the Default chip pops from 0.6x. */
@Composable
private fun DefaultChip() {
    val reduce = LocalReduceMotion.current
    val p = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(450, easing = OilEase)) }
    Box(
        Modifier.graphicsLayer { alpha = p.value; scaleX = 0.6f + 0.4f * p.value; scaleY = scaleX }
            .height(24.dp).clip(RoundedCornerShape(12.dp)).background(St.Green).padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.places_default), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold), color = St.Cream)
    }
}

/** The canvas's street sketch with a pin; the roads shift per card so places don't look alike. */
@Composable
private fun MiniMap(index: Int, modifier: Modifier) {
    val roadY = if (index % 2 == 0) 60f else 70f
    val roadX = if (index % 2 == 0) 24f else 70f
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).background(St.MapLand)) {
        viewBox(92f, 92f) {
            line(svgPath("M-4 ${roadY}H96"), St.MapRoad, 9f, cap = StrokeCap.Butt)
            line(svgPath("M$roadX -4V96"), St.MapRoad, 8f, cap = StrokeCap.Butt)
            line(svgPath("M-4 18L96 30"), St.MapRoad, 5f, cap = StrokeCap.Butt)
            box(58f, 56f, 26f, 26f, r = 5f, fill = St.MapPark)
            fill(svgPath("M46 50s-11-10-11-18a11 11 0 0 1 22 0c0 8-11 18-11 18z"), St.Green)
            dot(46f, 32f, 4.5f, fill = St.Cream)
        }
    }
}

/** Add or edit a place, with the same map pin as signup. */
@Composable
private fun PlaceEditor(place: Place?, canDelete: Boolean, onClose: () -> Unit, onSaved: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val repo = context.appContainer.partner
    val scope = rememberCoroutineScope()
    var draft by remember {
        mutableStateOf(
            SignupDraft(
                baseLabel = BaseLabel.entries.firstOrNull { it.wire == place?.label } ?: BaseLabel.Home,
                baseAddress = place?.address.orEmpty(),
                baseLat = place?.latitude,
                baseLng = place?.longitude,
            ),
        )
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    SignupFrame(
        stepNumber = 0, stepCount = 0,
        header = stringResource(R.string.profile_start_places),
        title = stringResource(if (place == null) R.string.places_add else R.string.signup_base_edit),
        body = stringResource(R.string.signup_base_body),
        onBack = onClose,
        action = stringResource(R.string.places_save),
        actionLoading = saving,
        onAction = {
            val lat = draft.baseLat
            val lng = draft.baseLng
            if (lat == null || lng == null || draft.baseAddress.isBlank()) {
                error = UiText.Res(R.string.check_base)
                return@SignupFrame
            }
            saving = true
            scope.launch {
                val body = PlaceBody(draft.baseLabel.wire, draft.baseAddress.trim(), lat, lng)
                val r = if (place == null) repo.addPlace(body) else repo.updatePlace(place.id, body)
                saving = false
                when (r) {
                    is ApiResult.Success -> { haptics.performHapticFeedback(HapticFeedbackType.Confirm); onSaved() }
                    is ApiResult.Failure -> error = r.problem.toUiText()
                }
            }
        },
        footer = if (canDelete && place != null) {
            {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(
                        onClick = { scope.launch { repo.deletePlace(place.id); onSaved() } },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.places_delete), color = St.Brick, style = MaterialTheme.typography.labelMedium) }
                }
            }
        } else null,
    ) {
        BaseStep(draft, error?.let { mapOf("base" to text(it)) } ?: emptyMap(), onChange = { change -> draft = change(draft); error = null })
        Spacer(Modifier.height(4.dp))
    }
}
