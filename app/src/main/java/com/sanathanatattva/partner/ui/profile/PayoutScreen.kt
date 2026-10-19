package com.sanathanatattva.partner.ui.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.Payout
import com.sanathanatattva.partner.ui.components.BackBar
import com.sanathanatattva.partner.ui.components.SecondaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.login.dialSupport
import com.sanathanatattva.partner.ui.theme.St
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Payout details (canvas "Payout details"): masked bank card, Razorpay account steps, recent payouts. */
@Composable
fun PayoutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    // Opens drawn from the last answer (Profile fetches it ahead), then refreshes in place.
    var payout by remember { mutableStateOf(container.partner.lastPayout) }
    var bank by remember { mutableStateOf(container.partner.lastPayout?.ifsc?.let { container.banks.cached(it) }) }
    LaunchedEffect(Unit) {
        (container.partner.payout() as? ApiResult.Success)?.value?.let { p ->
            payout = p
            p.ifsc?.let { bank = container.banks.describe(it) }
        }
    }
    val p = payout
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 22.dp)) {
        BackBar(stringResource(R.string.profile_payout), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (p != null) {
                BankCard(p, bank, Modifier.padding(top = 8.dp))
                StatusCard(p, Modifier.padding(top = 12.dp))
                if (p.recent.isNotEmpty()) Recent(p, Modifier.padding(top = 12.dp))
            }
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Bank changes go through the team (they re-check KYC before payouts restart).
            SecondaryButton(stringResource(R.string.payout_change), onClick = { context.dialSupport() }, green = true, modifier = Modifier.height(56.dp))
            Text(stringResource(R.string.payout_change_note), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = St.Ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BankCard(p: Payout, bank: String?, modifier: Modifier) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(St.Green)) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(StIcons.Bank, contentDescription = null, tint = St.Cream, modifier = Modifier.size(26.dp))
                Text(bank ?: stringResource(R.string.payout_bank_account), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = St.Cream, modifier = Modifier.weight(1f))
            }
            Text(
                if (p.last4 != null) "•••• •••• ${p.last4}" else stringResource(R.string.payout_no_bank),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.12.em),
                color = St.Cream,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Field(stringResource(R.string.payout_name), p.holder.orEmpty())
                Field(stringResource(R.string.payout_ifsc), p.ifsc.orEmpty(), tracking = 0.04f)
            }
        }
        Row(
            Modifier.fillMaxWidth().background(St.GreenDeep).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.payout_pan, p.panMasked ?: "—"),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = St.Cream,
                modifier = Modifier.weight(1f),
            )
            if (p.panVerified) {
                Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.payout_verified), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = St.Cream)
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, tracking: Float = 0f) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.GreenMist)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = tracking.em), color = St.Cream)
    }
}

@Composable
private fun StatusCard(p: Payout, modifier: Modifier) {
    val steps = listOf(R.string.payout_step_made, R.string.payout_step_bank, R.string.payout_step_checked, R.string.payout_step_active)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(St.Beige)
            .border(1.5.dp, St.BeigeLine, RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    when {
                        p.step >= 4 -> R.string.payout_on
                        p.needsClarification -> R.string.payout_needs_info
                        else -> R.string.payout_setting_up
                    },
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                color = St.Ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(stringResource(R.string.payout_via), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = St.Ink2)
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            steps.forEachIndexed { i, label ->
                val done = i < p.step
                val current = i == (p.step - 1).coerceAtLeast(0)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (done) St.Green else St.Track))
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = if (current && done) FontWeight.ExtraBold else FontWeight.Bold),
                        color = if (current && done) St.Ink else if (done) St.Green else St.Ink2,
                    )
                }
            }
        }
        Text(stringResource(R.string.payout_body), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = St.Ink3, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun Recent(p: Payout, modifier: Modifier) {
    Column(modifier) {
        Text(stringResource(R.string.payout_recent), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Ink, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp).semantics { heading() })
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(20.dp))) {
            p.recent.forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(dayLabel(r.date), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = St.Ink, modifier = Modifier.weight(1f))
                    val sent = r.status == "sent"
                    Box(Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(if (sent) St.GreenTint else St.GoldWash).padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(if (sent) R.string.payout_sent else R.string.payout_on_way),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
                            color = if (sent) St.Green else St.GoldText,
                        )
                    }
                    Text(rupees(r.amount), textAlign = TextAlign.End, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink, modifier = Modifier.width(64.dp))
                }
                if (i < p.recent.lastIndex) HorizontalDivider(thickness = 1.5.dp, color = St.BeigeLine)
            }
        }
    }
}

@Composable
private fun dayLabel(date: String): String {
    val d = LocalDate.parse(date)
    val text = d.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    return if (d == LocalDate.now()) stringResource(R.string.earnings_today, text) else text
}
