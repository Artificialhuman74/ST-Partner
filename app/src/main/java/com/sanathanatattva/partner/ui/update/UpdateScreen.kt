package com.sanathanatattva.partner.ui.update

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.UpdateOffer
import com.sanathanatattva.partner.data.openPlayStore
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.theme.St

/**
 * After the splash, when the Play Store has a newer version (design canvas "Update, ST Delivery").
 * The button opens the Play Store page. A build below the server's minimum gets no "Later":
 * Back closes the app instead.
 */
@Composable
fun UpdateScreen(offer: UpdateOffer, onLater: () -> Unit) {
    val context = LocalContext.current
    BackHandler { if (offer.required) (context as? Activity)?.finish() else onLater() }

    Column(
        Modifier
            .fillMaxSize()
            .background(St.Cream)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // A phone with a download arrow, and a gold "!" when the update is needed.
            Box(Modifier.padding(bottom = 8.dp).size(width = 220.dp, height = 160.dp).background(St.Sand, RoundedCornerShape(24.dp))) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(width = 80.dp, height = 116.dp)
                        .background(St.Paper, RoundedCornerShape(16.dp))
                        .border(3.dp, St.Green, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(StIcons.Download, contentDescription = null, tint = St.Green, modifier = Modifier.size(34.dp)) }
                if (offer.required) {
                    Box(
                        Modifier.align(Alignment.TopEnd).offset(x = (-30).dp, y = 18.dp).size(36.dp).background(St.Gold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("!", color = St.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Text(
                stringResource(R.string.update_label),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 0.4.sp),
                color = St.Green,
            )
            Text(
                stringResource(if (offer.required) R.string.update_required_title else R.string.update_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                color = St.Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(if (offer.required) R.string.update_required_body else R.string.update_body),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = St.Ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .background(St.Beige, RoundedCornerShape(16.dp))
                    .border(1.dp, St.BeigeLine, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(StIcons.Info, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.update_note), style = MaterialTheme.typography.bodyLarge, color = St.Ink)
            }
        }

        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(text = stringResource(R.string.update_button), arrow = false, onClick = { openPlayStore(context) })
            if (!offer.required) {
                TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.update_later), style = MaterialTheme.typography.labelLarge, color = St.Green)
                }
            }
        }
    }
}
