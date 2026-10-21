package com.sanathanatattva.partner.ui.shop

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.api.InventoryItem
import com.sanathanatattva.partner.data.shop.Pack
import com.sanathanatattva.partner.data.shop.Stock
import com.sanathanatattva.partner.data.shop.litres
import com.sanathanatattva.partner.data.shop.pack
import com.sanathanatattva.partner.data.shop.shortName
import com.sanathanatattva.partner.data.shop.sizeLabel
import com.sanathanatattva.partner.data.shop.stock
import com.sanathanatattva.partner.data.shop.whatsappNumber
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.theme.AnekNarrow
import com.sanathanatattva.partner.ui.theme.St

/** Sub-screens of My Shop: a round back button, a big title and a line under it. */
@Composable
fun ShopHeader(title: String, subtitle: String?, onBack: () -> Unit, modifier: Modifier = Modifier, backLabel: String = stringResource(R.string.shop_back)) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(St.Beige).clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(StIcons.ChevronLeft, contentDescription = backLabel, tint = St.Ink, modifier = Modifier.size(26.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = displayStyle(32), color = St.Ink, modifier = Modifier.semantics { heading() }, maxLines = 2)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 17.sp, lineHeight = 22.sp), color = St.Ink2)
        }
    }
}

/** The canvas's heavy narrow display type at [size]sp. */
@Composable
@ReadOnlyComposable
fun displayStyle(size: Int, weight: FontWeight = FontWeight.Bold) =
    MaterialTheme.typography.headlineMedium.copy(fontFamily = AnekNarrow, fontSize = size.sp, lineHeight = (size * 1.1f).sp, fontWeight = weight, letterSpacing = (-0.01f * size).sp)

/** The big green pill ("Ask for stock", "Send on WhatsApp"): 64–66dp, rounded all the way. */
@Composable
fun ShopButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 66.dp,
    loading: Boolean = false,
    enabled: Boolean = true,
    raised: Boolean = false,
) {
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier.fillMaxWidth().height(height)
            .then(if (raised) Modifier.shadow(12.dp, shape, ambientColor = St.Green, spotColor = St.Green) else Modifier)
            .clip(shape).background(if (enabled) St.Green else St.Track)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = St.Cream, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        } else {
            if (icon != null) Icon(icon, contentDescription = null, tint = if (enabled) Color.White else St.Ink2, modifier = Modifier.size(26.dp))
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 22.sp, fontWeight = FontWeight(650)), color = if (enabled) Color.White else St.Ink2)
        }
    }
}

/** Outlined green pill ("Call", "Open WhatsApp again"). */
@Composable
fun ShopOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 56.dp, fontSize: Int = 18) {
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier.height(height).clip(shape).border(BorderStroke(2.dp, St.Green), shape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize.sp, fontWeight = FontWeight.Bold), color = St.Green)
    }
}

/** A round − / + button; the + is filled when [filled]. */
@Composable
fun StepButton(plus: Boolean, label: String, onClick: () -> Unit, size: Dp, filled: Boolean = false, enabled: Boolean = true) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (filled) St.Green else St.Cream)
            .then(if (filled) Modifier else Modifier.border(2.dp, St.Green, CircleShape))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (plus) StIcons.Plus else StIcons.Minus, contentDescription = label, tint = if (filled) Color.White else St.Green, modifier = Modifier.size(size * 0.46f))
    }
}

// ── Words for a product ───────────────────────────────────────────────────────

/** "Groundnut oil" without its size. */
val InventoryItem.label: String get() = shortName(name)

/** "5 L can", "1 L bottle"; just "Can" when the size isn't known. */
@Composable
fun packLabel(item: InventoryItem): String = packLabel(item.pack, item.litres)

@Composable
fun packLabel(pack: Pack, litres: Double?): String {
    val size = litres?.let(::sizeLabel)
    return when (pack) {
        Pack.Bottle -> size?.let { stringResource(R.string.pack_bottle, it) } ?: stringResource(R.string.pack_bottle_plain)
        Pack.Can -> size?.let { stringResource(R.string.pack_can, it) } ?: stringResource(R.string.pack_can_plain)
        Pack.Tin -> size?.let { stringResource(R.string.pack_tin, it) } ?: stringResource(R.string.pack_tin_plain)
    }
}

/** "can" / "cans" for [count]. */
@Composable
fun unitWord(pack: Pack, count: Int): String = when (pack) {
    Pack.Bottle -> pluralStringResource(R.plurals.unit_bottles, count)
    Pack.Can -> pluralStringResource(R.plurals.unit_cans, count)
    Pack.Tin -> pluralStringResource(R.plurals.unit_tins, count)
}

/** "Finished" or "3 left" under a container. */
@Composable
fun countLabel(item: InventoryItem): String =
    if (item.stock == Stock.Finished) stringResource(R.string.stock_finished) else stringResource(R.string.shelf_left, item.quantity)

@Composable
fun stockWord(stock: Stock): String = stringResource(
    when (stock) {
        Stock.Plenty -> R.string.stock_plenty
        Stock.Low -> R.string.stock_low
        Stock.Finished -> R.string.stock_finished
    },
)

/** "Groundnut oil 5 L" for messages: name plus size. */
fun InventoryItem.fullName(): String = litres?.let { "$label ${sizeLabel(it)}" } ?: label

/** "Sesame oil, 1 L bottle: finished" for TalkBack. */
@Composable
fun describe(item: InventoryItem): String = stringResource(R.string.shelf_describe, item.label, packLabel(item), countLabel(item))

// ── Leaving the app ───────────────────────────────────────────────────────────

/** Opens WhatsApp on [phone] with [text] typed in (the person still presses send). */
fun Context.openWhatsApp(phone: String?, text: String): Boolean {
    val number = whatsappNumber(phone)
    val uri = Uri.parse("https://wa.me/${number.orEmpty()}?text=" + Uri.encode(text))
    return try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.no_whatsapp), Toast.LENGTH_LONG).show()
        false
    }
}

/** Opens the dialler with [phone] filled in. */
fun Context.dial(phone: String?) {
    val digits = phone.orEmpty().filter { it.isDigit() || it == '+' }
    if (digits.isBlank()) return
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) { }
}

/** WhatsApp's own share sheet for [text]; any app that takes text if WhatsApp isn't there. */
fun Context.shareOnWhatsApp(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    try {
        startActivity(Intent(send).setPackage("com.whatsapp").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
