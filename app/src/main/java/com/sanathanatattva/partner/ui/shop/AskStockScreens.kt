package com.sanathanatattva.partner.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.CatalogProduct
import com.sanathanatattva.partner.data.api.RestockLine
import com.sanathanatattva.partner.data.shop.areaOf
import com.sanathanatattva.partner.data.shop.litresOf
import com.sanathanatattva.partner.data.shop.maskedPhone
import com.sanathanatattva.partner.data.shop.packOf
import com.sanathanatattva.partner.data.shop.restockMessage
import com.sanathanatattva.partner.data.shop.shortName
import com.sanathanatattva.partner.data.shop.shortOrderNumber
import com.sanathanatattva.partner.data.shop.sizeLabel
import com.sanathanatattva.partner.data.shop.suggestedQuantity
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.dashedBorder
import com.sanathanatattva.partner.ui.components.popIn
import com.sanathanatattva.partner.ui.components.rememberEntrance
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

/** One line of "Ask for stock". [left] is null for a product the shop doesn't hold yet. */
private data class AskRow(
    val productId: Long,
    val name: String,
    val litres: Double?,
    val price: Double,
    val left: Int?,
    val qty: Int,
) {
    val fullName: String get() = litres?.let { "$name ${sizeLabel(it)}" } ?: name
}

/** Ask for stock (canvas "Ask for stock"): low items filled in, big − / +, the total, then WhatsApp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskStockScreen(preselect: Long?, onBack: () -> Unit, onSent: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val shop = rememberShop()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val rows = remember { mutableStateListOf<AskRow>() }
    var filled by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }

    // Fill in once the shelf is known: what's low, plus the product this was opened from.
    LaunchedEffect(shop.inventory) {
        val inventory = shop.inventory ?: return@LaunchedEffect
        if (filled) return@LaunchedEffect
        val wanted = shop.attention.toMutableList()
        inventory.firstOrNull { it.productId == preselect }?.let { if (it !in wanted) wanted.add(0, it) }
        rows.addAll(wanted.map { AskRow(it.productId, shortName(it.name), litresOf(it.containerType, it.name), it.price, it.quantity, it.suggestedQuantity) })
        filled = true
    }

    val chosen = rows.filter { it.qty > 0 }
    val total = chosen.sumOf { it.price * it.qty }
    val nf = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply { maximumFractionDigits = 2 } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 210.dp)) {
            ShopHeader(stringResource(R.string.ask_title), null, onBack)
            Text(stringResource(R.string.ask_body), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = St.Ink2, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEachIndexed { i, row ->
                    AskRowCard(
                        row,
                        priceText = nf.format(row.price),
                        onLess = { if (row.qty > 0) { rows[i] = row.copy(qty = row.qty - 1); haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) } },
                        onMore = { if (row.qty < 999) { rows[i] = row.copy(qty = row.qty + 1); haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) } },
                    )
                }
            }
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().height(58.dp)
                    .dashedBorder(2.dp, St.Dash, 24.dp).clip(RoundedCornerShape(24.dp)).clickable { picking = true },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(StIcons.Plus, contentDescription = null, tint = St.Green, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.ask_add), style = MaterialTheme.typography.labelMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold), color = St.Green)
            }
            if (shop.offline) {
                Text(stringResource(R.string.ask_offline), style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(20.dp))
            }
        }

        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().shadow(16.dp, sheetShape).clip(sheetShape).background(St.Paper)
                .border(1.5.dp, St.BeigeLine, sheetShape).windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(pluralStringResource(R.plurals.shop_products, chosen.size, chosen.size), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), color = St.Ink2, modifier = Modifier.weight(1f))
                Text(rupees(total), style = displayStyle(30, FontWeight.ExtraBold), color = St.Ink)
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Brick, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
            ShopButton(
                stringResource(R.string.ask_send),
                icon = StIcons.WhatsApp,
                loading = sending,
                enabled = chosen.isNotEmpty(),
                onClick = {
                    sending = true
                    error = null
                    scope.launch {
                        val profile = shop.profile
                        val lines = chosen.map { RestockLine(it.productId, it.qty) }
                        val result = container.shop.askForStock(lines) { order ->
                            restockMessage(
                                traderName = profile?.name.orEmpty(),
                                shopArea = areaOf(profile?.address),
                                lines = chosen.map { it.fullName to it.qty },
                                nowInShop = chosen.filter { it.left != null }.map { it.fullName to (it.left ?: 0) },
                                orderRef = shortOrderNumber(order.orderNumber, order.id),
                                total = rupees(ceil(order.total)),
                            )
                        }
                        sending = false
                        when (result) {
                            is ApiResult.Success -> {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                container.shop.lastSent?.let { context.openWhatsApp(it.adminPhone, it.message) }
                                onSent()
                            }
                            is ApiResult.Failure -> {
                                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                                error = (result.problem as? ApiProblem.Server)?.message ?: context.getString(R.string.ask_failed)
                            }
                        }
                    }
                },
            )
            Text(stringResource(R.string.ask_note), style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp), color = St.Ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }

    if (picking) {
        ModalBottomSheet(onDismissRequest = { picking = false }, containerColor = St.Cream) {
            AddProductSheet(
                taken = rows.map { it.productId }.toSet(),
                onPick = { p, left ->
                    rows.add(AskRow(p.id, shortName(p.name), litresOf(p.containerType, p.name), p.price, left, 1))
                    picking = false
                },
            )
        }
    }
}

@Composable
private fun AskRowCard(row: AskRow, priceText: String, onLess: () -> Unit, onMore: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (row.qty == 0) St.Sand else St.Paper).border(1.5.dp, St.BeigeLine, shape)
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp), color = St.Ink)
            Text(stringResource(R.string.ask_each, packLabel(packOf(row.litres), row.litres), priceText), style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Ink2)
            row.left?.let { left ->
                Text(
                    if (left <= 0) stringResource(R.string.ask_none_left) else stringResource(R.string.ask_only_left, left),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = if (left <= 0) St.Brick else St.GoldInk,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepButton(false, stringResource(R.string.one_less) + " " + row.name, onLess, 50.dp, enabled = row.qty > 0)
            Text(
                row.qty.toString(),
                style = displayStyle(30, FontWeight.ExtraBold),
                color = St.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 40.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
            StepButton(true, stringResource(R.string.one_more) + " " + row.name, onMore, 50.dp, filled = true)
        }
    }
}

/** "Add another product": the website's catalogue, minus what's already in the list. */
@Composable
private fun AddProductSheet(taken: Set<Long>, onPick: (CatalogProduct, Int?) -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val shop = rememberShop()
    var products by remember { mutableStateOf<List<CatalogProduct>?>(null) }
    LaunchedEffect(Unit) { products = container.shop.catalog() }
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.ask_pick_title), style = displayStyle(26), color = St.Ink)
        val list = products
        if (list == null) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = St.Green) }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.filter { it.id !in taken }.forEach { p ->
                    val left = shop.inventory?.firstOrNull { it.productId == p.id }?.quantity
                    val litres = litresOf(p.containerType, p.name)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(20.dp)).background(St.Paper)
                            .border(1.5.dp, St.BeigeLine, RoundedCornerShape(20.dp)).clickable { onPick(p, left) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(shortName(p.name), style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold), color = St.Ink)
                            Text(packLabel(packOf(litres), litres) + " · " + rupees(p.price), style = MaterialTheme.typography.bodySmall.copy(fontSize = 16.sp), color = St.Ink2)
                        }
                        Box(Modifier.size(44.dp).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
                            Icon(StIcons.Plus, contentDescription = null, tint = St.Cream, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Order saved (canvas "Order saved → WhatsApp"): the saved order and the message WhatsApp got. */
@Composable
fun SentScreen(onSeeOrder: () -> Unit) {
    val context = LocalContext.current
    val sent = context.appContainer.shop.lastSent
    if (sent == null) {
        LaunchedEffect(Unit) { onSeeOrder() }
        return
    }
    val ref = shortOrderNumber(sent.order.orderNumber, sent.order.id)
    val pop = rememberEntrance(Unit, 520)
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(60.dp).popIn(pop, 0.4f).clip(CircleShape).background(St.Green),
                contentAlignment = Alignment.Center,
            ) { Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(32.dp)) }
            Column {
                Text(stringResource(R.string.sent_title), style = displayStyle(30), color = St.Ink)
                Text(stringResource(R.string.sent_sub, ref, rupees(ceil(sent.total))), style = MaterialTheme.typography.bodySmall.copy(fontSize = 17.sp), color = St.Ink2)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(St.GoldGlint).border(2.dp, St.GoldPale, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(St.Gold), contentAlignment = Alignment.Center) {
                Text("2", style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold), color = St.Ink)
            }
            Text(
                AnnotatedString.fromHtml(stringResource(R.string.sent_step)),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
                color = St.Ink,
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(St.GreenWash).border(1.5.dp, St.GreenMist, RoundedCornerShape(26.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.sent_to, maskedPhone(sent.adminPhone)), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = St.Green, modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                sent.message,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = St.Ink,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp))
                    .background(St.Paper).border(1.dp, St.GreenMist, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
        ShopButton(stringResource(R.string.sent_see), onSeeOrder, height = 64.dp, modifier = Modifier.padding(top = 6.dp))
        ShopOutlineButton(
            stringResource(R.string.sent_again),
            onClick = { context.openWhatsApp(sent.adminPhone, sent.message) },
            icon = StIcons.WhatsApp,
            height = 58.dp,
            fontSize = 19,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
