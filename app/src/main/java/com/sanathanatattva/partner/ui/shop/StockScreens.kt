package com.sanathanatattva.partner.ui.shop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.InventoryItem
import com.sanathanatattva.partner.data.shop.Pack
import com.sanathanatattva.partner.data.shop.Stock
import com.sanathanatattva.partner.data.shop.level
import com.sanathanatattva.partner.data.shop.oil
import com.sanathanatattva.partner.data.shop.pack
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch

/** Stock (canvas "Stock · the oil shelf"): the shelf, a key, and "Ask for stock" at the bottom. */
@Composable
fun ShelfScreen(onBack: () -> Unit, onProduct: (Long) -> Unit, onAsk: () -> Unit) {
    val shop = rememberShop()
    val items = remember(shop.inventory) {
        // Needs attention first, then the rest as the website lists them.
        shop.inventory.orEmpty().sortedWith(compareBy({ stockRank(it) }, { it.quantity }))
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
            ShopHeader(stringResource(R.string.shelf_title), stringResource(R.string.shelf_sub), onBack)
            if (shop.inventory?.isEmpty() == true) {
                EmptyShelf(onAsk, Modifier.padding(horizontal = 20.dp))
            } else {
                val name: (InventoryItem) -> String = { it.label }
                val packs = items.associateWith { packLabel(it) }
                val counts = items.associateWith { countLabel(it) }
                val descriptions = items.associateWith { describe(it) }
                OilShelf(
                    items = items,
                    name = name,
                    packLabel = { packs.getValue(it) },
                    countLabel = { counts.getValue(it) },
                    describe = { descriptions.getValue(it) },
                    onOpen = { onProduct(it.productId) },
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp),
                )
                Legend(Modifier.padding(top = 14.dp))
            }
        }
        val count = shop.attention.size
        ShopButton(
            text = if (count > 0) pluralStringResource(R.plurals.shelf_ask_count, count, count) else stringResource(R.string.shop_ask),
            onClick = onAsk,
            icon = StIcons.WhatsApp,
            raised = true,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(start = 16.dp, end = 16.dp, bottom = 22.dp),
        )
    }
}

private fun stockRank(item: InventoryItem): Int = when {
    item.quantity <= 0 -> 0
    item.quantity <= item.alertAt -> 1
    else -> 2
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        listOf(Stock.Plenty to St.Green, Stock.Low to St.Gold, Stock.Finished to St.Brick).forEach { (stock, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                Text(stockWord(stock), style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp), color = St.Ink2)
            }
        }
    }
}

/** One product (canvas "One product · alert level"): how many are left and the alert level. */
@Composable
fun ProductScreen(productId: Long, onBack: () -> Unit, onAsk: (Long) -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val shop = rememberShop()
    val item = shop.inventory?.firstOrNull { it.productId == productId }
    if (item == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) { ShopHeader(stringResource(R.string.shelf_title), null, onBack) }
        return
    }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var alert by remember(item.productId) { mutableIntStateOf(item.alertAt) }
    var saving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf<String?>(null) }
    val changed = alert != item.alertAt
    // The chip follows the level being set, before it's saved.
    val preview = item.copy(alertAt = alert)
    val stock = when {
        preview.quantity <= 0 -> Stock.Finished
        preview.quantity <= preview.alertAt -> Stock.Low
        else -> Stock.Plenty
    }
    val pack = item.pack
    val unitOne = unitWord(pack, 1)
    val description = describe(item)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
            ShopHeader(item.label, packLabel(item), onBack, backLabel = stringResource(R.string.shelf_title))
            val heroShape = RoundedCornerShape(30.dp)
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().clip(heroShape).background(St.Sand).border(1.5.dp, St.SandDeep, heroShape)
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(Modifier.width(132.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OilContainer(
                        pack, item.oil, item.level,
                        height = when (pack) { Pack.Bottle -> 176.dp; Pack.Can -> 166.dp; Pack.Tin -> 170.dp },
                        alertLevel = (alert.toFloat() / maxOf(item.alertAt * 4, 8)).coerceIn(0f, 0.95f),
                        modifier = Modifier.semantics { contentDescription = description },
                    )
                    Plank(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)), height = 14.dp)
                }
                Column(Modifier.padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.quantity.toString(), style = displayStyle(76, FontWeight.ExtraBold).copy(lineHeight = 70.sp), color = St.Ink)
                        Text(
                            stringResource(R.string.product_left, unitWord(pack, item.quantity)),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
                            color = St.Ink,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    StockChip(stockWord(stock), stock, fontSize = 17)
                }
            }

            val cardShape = RoundedCornerShape(28.dp)
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(cardShape).background(St.Paper).border(1.5.dp, St.BeigeLine, cardShape).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(StIcons.Bell, contentDescription = null, tint = St.GoldInk, modifier = Modifier.size(26.dp))
                    Text(stringResource(R.string.product_alert_title), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    StepButton(false, stringResource(R.string.one_less), { if (alert > 1) { alert--; saved = false; haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) } }, 68.dp, enabled = alert > 1)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(alert.toString(), style = displayStyle(52, FontWeight.ExtraBold), color = St.Ink)
                        Text(pluralStringResource(R.plurals.product_alert_left, alert, unitWord(pack, alert)), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = St.Ink2)
                    }
                    StepButton(true, stringResource(R.string.one_more), { if (alert < 999) { alert++; saved = false; haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) } }, 68.dp)
                }
                Text(stringResource(R.string.product_alert_note, unitOne), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp), color = St.Ink2)
                AnimatedVisibility(changed, enter = fadeIn(), exit = fadeOut()) {
                    ShopButton(
                        stringResource(R.string.product_save),
                        onClick = {
                            saving = true
                            failed = null
                            scope.launch {
                                when (val r = container.shop.setAlertLevel(item.productId, alert)) {
                                    is ApiResult.Success -> { saved = true; haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
                                    is ApiResult.Failure -> { failed = context.getString(R.string.error_network); haptics.performHapticFeedback(HapticFeedbackType.Reject) }
                                }
                                saving = false
                            }
                        },
                        height = 58.dp,
                        loading = saving,
                    )
                }
                if (saved && !changed) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        Icon(StIcons.Check, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
                        Text(stringResource(R.string.product_saved), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = St.Green)
                    }
                }
                failed?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = St.Brick) }
            }
            Spacer(Modifier.height(8.dp))
        }
        ShopButton(
            stringResource(R.string.product_ask_more, unitWord(pack, 2)),
            onClick = { onAsk(item.productId) },
            icon = StIcons.WhatsApp,
            raised = true,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(start = 16.dp, end = 16.dp, bottom = 22.dp),
        )
    }
}
