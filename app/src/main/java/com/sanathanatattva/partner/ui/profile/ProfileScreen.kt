package com.sanathanatattva.partner.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.alerts.AlertReadiness
import com.sanathanatattva.partner.alerts.OnlineService
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.Place
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.DockContentClearance
import com.sanathanatattva.partner.ui.login.dialSupport
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch

enum class ProfileLink { StartPlaces, DeliveryHours, Vehicle, Payout, Language, Alerts, RiderJobs, ContainerPickups, Edit }

/** Profile (canvas "Profile"): tiles for the partner's own settings. Traders and riders see different tiles. */
@Composable
fun ProfileScreen(onOpen: (ProfileLink) -> Unit, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val session by container.sessions.session.collectAsState(initial = container.sessions.latest)
    val user = session?.user
    var places by remember { mutableStateOf(container.partner.lastPlaces.orEmpty()) }
    var missing by remember { mutableStateOf(AlertReadiness.check(context).missing.size) }
    LifecycleResumeEffect(Unit) {
        missing = AlertReadiness.check(context).missing.size
        // Start places and the bank change only when the partner edits them (Start places
        // refreshes the saved list after each edit, and Home on every visit), so Profile
        // draws the last answer and only asks again once it's 3 hours old.
        places = container.partner.lastPlaces.orEmpty()
        if (!container.partner.placesFresh) {
            scope.launch { (container.partner.places() as? ApiResult.Success)?.let { places = it.value } }
        }
        if (!container.partner.payoutFresh) scope.launch {
            (container.partner.payout() as? ApiResult.Success)?.value?.ifsc?.let { container.banks.describe(it) }
        }
        onPauseOrDispose { }
    }
    val rider = user?.isRider == true

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp + DockContentClearance),
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(24.dp))
                    .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(St.GreenMist).border(2.5.dp, St.Green, CircleShape), contentAlignment = Alignment.Center) {
                    Text(user?.firstName?.take(1)?.uppercase().orEmpty(), style = MaterialTheme.typography.headlineMedium, color = St.Green)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(user?.name.orEmpty(), style = MaterialTheme.typography.titleLarge, color = St.Ink)
                    Text(
                        if (rider) listOfNotNull(stringResource(R.string.home_partner_rider), user?.vehiclePlate).joinToString(" · ")
                        else stringResource(R.string.home_partner_trader),
                        style = MaterialTheme.typography.bodySmall, color = St.Ink3,
                    )
                    Row(
                        Modifier.padding(top = 2.dp).clip(RoundedCornerShape(13.dp)).background(St.GreenTint).padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(14.dp))
                        Text(stringResource(if (rider) R.string.profile_badge_rider else R.string.profile_badge_trader), style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), color = St.Green)
                    }
                }
                Text(
                    stringResource(R.string.signup_base_edit),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = St.Green,
                    modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable { onOpen(ProfileLink.Edit) }.padding(horizontal = 10.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            val placeLabels = places.map { labelOf(it.label) }.joinToString(", ").ifBlank { null }
            val tiles = buildList {
                add(Tile(StIcons.Pin, R.string.profile_start_places, placeLabels, ProfileLink.StartPlaces))
                if (rider) add(Tile(StIcons.Scooter, R.string.profile_vehicle, user?.vehiclePlate, ProfileLink.Vehicle))
                else add(Tile(StIcons.Clock, R.string.profile_delivery_hours, null, ProfileLink.DeliveryHours))
                add(Tile(StIcons.Bank, R.string.profile_payout, null, ProfileLink.Payout))
                add(Tile(StIcons.Language, R.string.profile_language, stringResource(R.string.profile_language_value), ProfileLink.Language))
            }
            tiles.chunked(2).forEach { pair ->
                Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEach { tile -> TileView(tile, Modifier.weight(1f)) { onOpen(tile.link) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            RowLink(
                StIcons.Bell,
                if (missing == 0) stringResource(R.string.profile_alerts_ok) else pluralStringResource(R.plurals.perm_left, missing, missing),
                null,
                highlight = missing > 0,
            ) { onOpen(ProfileLink.Alerts) }
            if (!rider) {
                RowLink(StIcons.Scooter, stringResource(R.string.profile_rider_jobs), stringResource(R.string.profile_rider_jobs_body)) { onOpen(ProfileLink.RiderJobs) }
                RowLink(StIcons.CanNew, stringResource(R.string.profile_pickups), null) { onOpen(ProfileLink.ContainerPickups) }
            }
            RowLink(StIcons.Headset, stringResource(R.string.profile_support), null) { context.dialSupport() }
            RowLink(StIcons.ArrowLeft, stringResource(R.string.profile_log_out), null, danger = true) {
                // Logging out settles softly (design canvas "Haptics").
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
                scope.launch {
                    if (OnlineService.running.value) {
                        container.partner.goOffline()
                        OnlineService.stopOnline(context)
                    }
                    container.auth.logOut()
                    onLoggedOut()
                }
            }
        }
    }
}

@Composable
private fun labelOf(label: String) = stringResource(
    when (label) { "shop" -> R.string.label_shop; "other" -> R.string.label_other; else -> R.string.label_home },
)

private data class Tile(val icon: ImageVector, val title: Int, val detail: String?, val link: ProfileLink)

/** "pf-tile:active": tiles press in to 98%. */
@Composable
private fun TileView(tile: Tile, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
    Column(
        modifier.heightIn(min = 128.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(tile.icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(28.dp))
        Column {
            Text(stringResource(tile.title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            if (tile.detail != null) Text(tile.detail, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Ink2, maxLines = 1)
        }
    }
}

@Composable
private fun RowLink(icon: ImageVector, title: String, body: String?, highlight: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.padding(bottom = 14.dp).fillMaxWidth().heightIn(min = 66.dp).clip(RoundedCornerShape(20.dp))
            .background(if (highlight) St.Gold else St.Paper).border(1.5.dp, if (highlight) St.Gold else St.BeigeLine, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) St.Brick else if (highlight) St.Ink else St.Green, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = if (danger) St.Brick else St.Ink)
            if (body != null) Text(body, style = MaterialTheme.typography.bodySmall, color = St.Ink2)
        }
        if (!danger) Icon(StIcons.ChevronRight, contentDescription = null, tint = St.Ink2, modifier = Modifier.size(20.dp))
    }
}
