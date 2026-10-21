package com.sanathanatattva.partner.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.shop.ShopState
import com.sanathanatattva.partner.data.shop.Stock
import com.sanathanatattva.partner.data.shop.areaOf
import com.sanathanatattva.partner.data.shop.stock
import com.sanathanatattva.partner.data.shop.weeklyTotals
import com.sanathanatattva.partner.ui.components.DockContentClearance
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.rupees
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

enum class ShopPlace { Shelf, Money, Orders, People, AskStock }

/** Keeps the shop fresh while a My Shop screen is showing; returns what to draw. */
@Composable
fun rememberShop(): ShopState {
    val container = LocalContext.current.appContainer
    val state by container.shop.state.collectAsState()
    val tier1 = container.sessions.latest?.user?.tier == 1
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        val job = scope.launch { container.shop.refreshIfStale(tier1) }
        onPauseOrDispose { job.cancel() }
    }
    return state
}

/** My Shop tab (canvas "My Shop"): what needs you, then Stock · Money · Orders · People. */
@Composable
fun MyShopScreen(onOpen: (ShopPlace) -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val shop = rememberShop()
    val tier1 = container.sessions.latest?.user?.tier == 1
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(bottom = 16.dp + DockContentClearance),
    ) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(St.GoldGlint).border(2.dp, St.GoldPale, CircleShape), contentAlignment = Alignment.Center) {
                Icon(StIcons.Store, contentDescription = null, tint = St.GoldInk, modifier = Modifier.size(28.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.shop_title), style = displayStyle(34), color = St.Ink, modifier = Modifier.semantics { heading() })
                val p = shop.profile
                if (p != null) {
                    Text(listOf(p.name, areaOf(p.address)).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall.copy(fontSize = 17.sp, lineHeight = 22.sp), color = St.Ink2)
                }
            }
        }

        val inventory = shop.inventory
        when {
            inventory == null && !shop.offline -> LoadingShop()
            inventory != null && inventory.isEmpty() -> EmptyShelf(onAsk = { onOpen(ShopPlace.AskStock) }, modifier = Modifier.padding(horizontal = 20.dp))
            else -> {
                if (shop.offline) {
                    OfflineCard(shop.loadedAt, onRetry = { scope.launch { container.shop.refresh(tier1) } }, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
                }
                if (inventory != null) {
                    if (shop.attention.isNotEmpty()) NeedsYouCard(shop, onAsk = { onOpen(ShopPlace.AskStock) })
                    else AllFineCard()
                }
                Tiles(shop, tier1, onOpen, Modifier.alpha(if (shop.offline) 0.7f else 1f))
            }
        }
    }
}

@Composable
private fun NeedsYouCard(shop: ShopState, onAsk: () -> Unit) {
    val attention = shop.attention
    val finished = attention.count { it.stock == Stock.Finished }
    val low = attention.size - finished
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth().clip(shape).background(St.GoldGlint).border(2.dp, St.GoldPale, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniShelf(attention.take(3), height = 58.dp, plank = 6.dp)
            Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(pluralStringResource(R.plurals.shop_need_you, attention.size, attention.size), style = displayStyle(26), color = St.Ink)
                val parts = buildList {
                    if (finished > 0) add(pluralStringResource(R.plurals.shop_finished_count, finished, finished))
                    if (low > 0) add(pluralStringResource(R.plurals.shop_low_count, low, low))
                }
                Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp), color = St.GoldInk)
            }
        }
        ShopButton(stringResource(R.string.shop_ask), onAsk, icon = StIcons.WhatsApp, height = 64.dp)
    }
}

@Composable
private fun AllFineCard() {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth().clip(shape).background(St.GreenWash).border(2.dp, St.GreenMist, shape).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(St.Green), contentAlignment = Alignment.Center) {
            Icon(StIcons.Check, contentDescription = null, tint = St.Cream, modifier = Modifier.size(26.dp))
        }
        Column {
            Text(stringResource(R.string.shop_all_fine), style = displayStyle(24), color = St.Ink)
            Text(stringResource(R.string.shop_all_fine_body), style = MaterialTheme.typography.bodySmall.copy(fontSize = 17.sp), color = St.Ink2)
        }
    }
}

@Composable
private fun Tiles(shop: ShopState, tier1: Boolean, onOpen: (ShopPlace) -> Unit, modifier: Modifier) {
    val inventory = shop.inventory.orEmpty()
    val week = shop.money?.commissions?.let { weeklyTotals(it).lastOrNull()?.amount } ?: 0.0
    val tiles = buildList {
        add(TileInfo(ShopPlace.Shelf, null, stringResource(R.string.shop_stock), pluralStringResource(R.plurals.shop_products, inventory.size, inventory.size)))
        add(TileInfo(ShopPlace.Money, StIcons.Rupee, stringResource(R.string.shop_money), stringResource(R.string.shop_money_week, rupees(week))))
        add(
            TileInfo(
                ShopPlace.Orders, StIcons.Truck, stringResource(R.string.shop_orders),
                if (shop.onTheWay > 0) stringResource(R.string.shop_on_the_way, shop.onTheWay) else stringResource(R.string.shop_none_on_the_way),
            ),
        )
        if (tier1) {
            val subs = shop.subShops?.size ?: 0
            add(TileInfo(ShopPlace.People, StIcons.People, stringResource(R.string.shop_people), pluralStringResource(R.plurals.shop_sub_dealers, subs, subs)))
        }
    }
    Column(modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { t ->
                    Tile(t, Modifier.weight(1f), onOpen) {
                        if (t.place == ShopPlace.Shelf) MiniShelf(inventory.take(4), height = 30.dp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class TileInfo(val place: ShopPlace, val icon: ImageVector?, val title: String, val line: String)

@Composable
private fun Tile(t: TileInfo, modifier: Modifier, onOpen: (ShopPlace) -> Unit, art: @Composable () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier.height(148.dp).clip(shape).background(St.Beige).border(1.5.dp, St.BeigeLine, shape)
            .clickable(role = Role.Button) { onOpen(t.place) }.padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (t.icon != null) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(St.Cream), contentAlignment = Alignment.Center) {
                Icon(t.icon, contentDescription = null, tint = St.Green, modifier = Modifier.size(24.dp))
            }
        } else {
            art()
        }
        Column {
            Text(t.title, style = displayStyle(24), color = St.Ink)
            Text(t.line, style = MaterialTheme.typography.bodySmall.copy(fontSize = 17.sp, lineHeight = 22.sp), color = St.Ink2, maxLines = 1)
        }
    }
}

/** States panel "Loading": the shape of the page in beige while the shop loads. */
@Composable
private fun LoadingShop() {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.shop_loading), style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp), color = St.Ink2)
        Box(Modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(28.dp)).background(St.Beige))
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { Box(Modifier.weight(1f).height(148.dp).clip(RoundedCornerShape(26.dp)).background(St.Beige)) }
            }
        }
    }
}

/** States panel "Empty shelf". */
@Composable
fun EmptyShelf(onAsk: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val shape = RoundedCornerShape(30.dp)
        Column(Modifier.padding(top = 24.dp).fillMaxWidth().clip(shape).background(St.Sand).border(1.5.dp, St.SandDeep, shape).padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 24.dp)) {
            Plank(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(2) { Box(Modifier.size(width = 10.dp, height = 26.dp).clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)).background(St.WoodDark)) }
            }
        }
        Text(stringResource(R.string.shop_empty_title), style = displayStyle(26), color = St.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        Text(stringResource(R.string.shop_empty_body), style = MaterialTheme.typography.bodyLarge, color = St.Ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        ShopButton(stringResource(R.string.shop_empty_cta), onAsk, modifier = Modifier.padding(top = 12.dp))
    }
}

/** States panel "No internet": the last numbers stay, with when they're from. */
@Composable
fun OfflineCard(loadedAt: Long?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    Column(modifier.fillMaxWidth().clip(shape).background(St.Beige).border(1.5.dp, St.BeigeLine, shape).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(StIcons.WifiOff, contentDescription = null, tint = St.Ink, modifier = Modifier.size(28.dp))
            Text(stringResource(R.string.shop_offline_title), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = St.Ink)
        }
        if (loadedAt != null) {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(loadedAt))
            Text(stringResource(R.string.shop_offline_body, time), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp), color = St.Ink2)
        }
        ShopOutlineButton(stringResource(R.string.try_again), onRetry, modifier = Modifier.fillMaxWidth(), height = 54.dp, fontSize = 19)
    }
}
