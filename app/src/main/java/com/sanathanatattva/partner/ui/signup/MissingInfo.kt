package com.sanathanatattva.partner.ui.signup

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.rememberEntrance
import com.sanathanatattva.partner.ui.components.riseIn
import com.sanathanatattva.partner.ui.theme.St

/**
 * Canvas "MissingInfo": a Google sign-in brings the name and email; this says what is still
 * needed before the signup steps. Heading and cards rise in one after another ("mi-in", .8 s,
 * 80 ms apart) and the progress bar fills to half ("mi-bar", 1.1 s after .2 s).
 */
@Composable
fun MissingInfoScreen(name: String, email: String, onContinue: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val first = name.trim().substringBefore(' ').ifBlank { name }
    val inHead = rememberEntrance(Unit, 800)
    val inBody = rememberEntrance(Unit, 800, delayMs = 80)
    val inCards = rememberEntrance(Unit, 800, delayMs = 160)
    val bar = rememberEntrance(Unit, 1100, delayMs = 200)
    Column(
        Modifier.fillMaxSize().background(St.Cream).windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(St.Paper)
                    .border(2.dp, St.LineSoft, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(St.GreenMist), contentAlignment = Alignment.Center) {
                    Text(first.take(1).uppercase(), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = St.Green)
                }
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(email, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = St.Ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
            }
            Text(
                stringResource(R.string.missing_title, first),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 34.sp),
                color = St.Ink,
                modifier = Modifier.padding(top = 32.dp).riseIn(inHead, 14.dp).semantics { heading() },
            )
            Column(Modifier.riseIn(inBody, 14.dp)) {
                Text(stringResource(R.string.missing_body), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = St.Ink2, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.missing_progress), style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = St.Ink)
                    Text(stringResource(R.string.missing_from_google), style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Ink2, modifier = Modifier.padding(start = 8.dp))
                }
                Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Track)) {
                    Box(
                        Modifier.fillMaxWidth(0.5f).height(10.dp)
                            .graphicsLayer { scaleX = bar.value; transformOrigin = TransformOrigin(0f, 0.5f) }
                            .clip(RoundedCornerShape(5.dp)).background(St.Green),
                    )
                }
            }
            Column(Modifier.padding(top = 28.dp).riseIn(inCards, 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NeedCard(StIcons.Phone, stringResource(R.string.missing_phone), stringResource(R.string.missing_phone_body), onContinue)
                NeedCard(StIcons.Pin, stringResource(R.string.missing_base), stringResource(R.string.missing_base_body), onContinue)
            }
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(text = stringResource(R.string.missing_cta), onClick = onContinue)
    }
}

@Composable
private fun NeedCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(shape).background(St.Paper).border(2.5.dp, St.Gold, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(GoldWash), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = St.GoldInk, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, fontWeight = FontWeight.Bold), color = St.Ink)
            Text(body, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp), color = St.Ink2)
        }
        Icon(StIcons.ChevronRight, contentDescription = null, tint = St.GoldInk, modifier = Modifier.size(24.dp))
    }
}

private val Track = Color(0xFFE8DCC8)
private val GoldWash = Color(0xFFFBEFD2)
