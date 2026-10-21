package com.sanathanatattva.partner.ui.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanathanatattva.partner.data.api.InventoryItem
import com.sanathanatattva.partner.data.shop.Oil
import com.sanathanatattva.partner.data.shop.Pack
import com.sanathanatattva.partner.data.shop.Stock
import com.sanathanatattva.partner.data.shop.level
import com.sanathanatattva.partner.data.shop.oil
import com.sanathanatattva.partner.data.shop.pack
import com.sanathanatattva.partner.data.shop.stock
import com.sanathanatattva.partner.ui.theme.St

// The oil shelf (canvas "Stock · the oil shelf"). Containers are drawn, not photos, so every
// phone shows the same shelf; the oil inside is the stock.

internal object ShelfInk {
    val Glass = Color(0xFF8A7B63)
    val WoodLight = Color(0xFFC08552)
    val Divider = Color(0xFFC9B48C)
    val FinishedBg = Color(0xFFF8E1DB)
    val FinishedLine = Color(0xFFE8B4A6)
    val Sheen = Color.White.copy(alpha = 0.55f)
}

/** Oil colour and the darker line on its surface. */
internal fun Oil.colors(): Pair<Color, Color> = when (this) {
    Oil.Groundnut -> St.Gold to St.GoldDeep
    Oil.Sesame -> Color(0xFFB8732A) to St.WoodDark
    Oil.Coconut -> Color(0xFFF1E4BF) to St.KraftDark
    Oil.Sunflower -> Color(0xFFF2C230) to Color(0xFFC9A21E)
    Oil.Mustard -> Color(0xFFC99A1C) to Color(0xFF9C7612)
    Oil.Castor -> Color(0xFFD9C27A) to Color(0xFFB19A50)
}

/** Drawing size of each container in the canvas's units (px at 1x). */
internal fun Pack.designSize(): Size = when (this) {
    Pack.Bottle -> Size(46f, 120f)
    Pack.Can -> Size(70f, 112f)
    Pack.Tin -> Size(74f, 116f)
}

/**
 * One container, [height] tall, filled to [level] (0..1). [alertLevel] draws the dashed alert
 * line used on the product screen.
 */
@Composable
fun OilContainer(pack: Pack, oil: Oil, level: Float, height: Dp, modifier: Modifier = Modifier, alertLevel: Float? = null) {
    val design = pack.designSize()
    val width = height * (design.width / design.height)
    Canvas(modifier.size(width, height)) {
        val u = size.width / design.width
        val (oilColor, edge) = oil.colors()
        when (pack) {
            Pack.Bottle -> drawBottle(u, level, oilColor, edge, alertLevel)
            Pack.Can -> drawCan(u, level, oilColor, edge, alertLevel)
            Pack.Tin -> drawTin(u, level, oilColor, alertLevel)
        }
    }
}

private fun roundRect(l: Float, t: Float, w: Float, h: Float, top: Float, bottom: Float = top) = Path().apply {
    addRoundRect(
        RoundRect(
            left = l, top = t, right = l + w, bottom = t + h,
            topLeftCornerRadius = CornerRadius(top), topRightCornerRadius = CornerRadius(top),
            bottomRightCornerRadius = CornerRadius(bottom), bottomLeftCornerRadius = CornerRadius(bottom),
        ),
    )
}

/** Fill, oil clipped inside, the glass sheen, then the outline drawn just inside the edge. */
private fun DrawScope.vessel(body: Path, inner: Path, u: Float, stroke: Float, top: Float, bottom: Float, left: Float, level: Float, oil: Color, edge: Color?, sheenX: Float?, sheenW: Float, alertLevel: Float?) {
    drawPath(body, St.Paper)
    clipPath(body) {
        val h = bottom - top
        val oilTop = bottom - h * level
        if (level > 0f) {
            drawRect(oil, Offset(0f, oilTop), Size(size.width, bottom - oilTop))
            if (edge != null) drawRect(edge, Offset(0f, oilTop), Size(size.width, 2f * u))
        }
        if (alertLevel != null) {
            val y = bottom - h * alertLevel
            drawLine(St.GoldInk, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * u, 4f * u)))
        }
        if (sheenX != null) drawRoundRect(ShelfInk.Sheen, Offset(left + sheenX * u, top + 10f * u), Size(sheenW * u, h - 20f * u), CornerRadius(3f * u))
    }
    drawPath(inner, ShelfInk.Glass, style = Stroke(stroke))
}

private fun DrawScope.drawBottle(u: Float, level: Float, oil: Color, edge: Color, alert: Float?) {
    drawRoundRect(St.Green, Offset(15f * u, 0f), Size(16f * u, 9f * u), CornerRadius(3f * u))
    drawRect(St.Paper, Offset(17f * u, 8f * u), Size(12f * u, 16f * u))
    val neck = Path().apply {
        moveTo(18f * u, 24f * u); lineTo(18f * u, 9f * u); lineTo(28f * u, 9f * u); lineTo(28f * u, 24f * u)
    }
    drawPath(neck, ShelfInk.Glass, style = Stroke(2f * u))
    val body = roundRect(0f, 22f * u, 46f * u, 98f * u, 16f * u, 10f * u)
    val inner = roundRect(1f * u, 23f * u, 44f * u, 96f * u, 15f * u, 9f * u)
    vessel(body, inner, u, 2f * u, 22f * u, 120f * u, 0f, level, oil, edge, 6f, 5f, alert)
}

private fun DrawScope.drawCan(u: Float, level: Float, oil: Color, edge: Color, alert: Float?) {
    val handle = Path().apply {
        moveTo(9.5f * u, 18f * u); lineTo(9.5f * u, 11.5f * u)
        quadraticTo(9.5f * u, 1.5f * u, 19.5f * u, 1.5f * u); lineTo(26.5f * u, 1.5f * u)
        quadraticTo(36.5f * u, 1.5f * u, 36.5f * u, 11.5f * u); lineTo(36.5f * u, 18f * u)
    }
    drawPath(handle, ShelfInk.Glass, style = Stroke(3f * u))
    drawRoundRect(St.Green, Offset(48f * u, 4f * u), Size(14f * u, 12f * u), CornerRadius(3f * u))
    val body = roundRect(0f, 14f * u, 70f * u, 98f * u, 10f * u)
    val inner = roundRect(1f * u, 15f * u, 68f * u, 96f * u, 9f * u)
    vessel(body, inner, u, 2f * u, 14f * u, 112f * u, 0f, level, oil, edge, 7f, 6f, alert)
}

private fun DrawScope.drawTin(u: Float, level: Float, oil: Color, alert: Float?) {
    drawRoundRect(St.SteelDark, Offset(0f, 0f), Size(74f * u, 9f * u), CornerRadius(3f * u))
    drawRoundRect(ShelfInk.Glass, Offset(0.75f * u, 0.75f * u), Size(72.5f * u, 7.5f * u), CornerRadius(3f * u), style = Stroke(1.5f * u))
    drawRoundRect(St.Steel, Offset(0f, 8f * u), Size(74f * u, 108f * u), CornerRadius(4f * u))
    drawRect(St.SteelDark, Offset(2f * u, 26f * u), Size(70f * u, 2f * u))
    drawRect(St.SteelDark, Offset(2f * u, 96f * u), Size(70f * u, 2f * u))
    drawRoundRect(ShelfInk.Glass, Offset(1f * u, 9f * u), Size(72f * u, 106f * u), CornerRadius(4f * u), style = Stroke(2f * u))
    val window = roundRect(25f * u, 18f * u, 20f * u, 88f * u, 10f * u)
    val inner = roundRect(25.75f * u, 18.75f * u, 18.5f * u, 86.5f * u, 9.25f * u)
    vessel(window, inner, u, 1.5f * u, 18f * u, 106f * u, 25f * u, level, oil, null, null, 0f, alert)
}

/** The status chip under each container: "Finished", "3 left". */
@Composable
fun StockChip(text: String, stock: Stock, modifier: Modifier = Modifier, fontSize: Int = 15) {
    val (bg, ink, line) = chipColors(stock)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize.sp, fontWeight = FontWeight.Bold, lineHeight = (fontSize * 1.25f).sp),
        color = ink,
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(bg).border(1.5.dp, line, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

internal fun chipColors(stock: Stock): Triple<Color, Color, Color> = when (stock) {
    Stock.Finished -> Triple(ShelfInk.FinishedBg, St.Brick, ShelfInk.FinishedLine)
    Stock.Low -> Triple(St.GoldGlint, St.GoldInk, St.GoldPale)
    Stock.Plenty -> Triple(St.GreenTint, St.Green, St.GreenMist)
}

/**
 * The whole shelf: compartments of three, wooden uprights between products, a thin line between
 * shelves, and a dashed red outline round anything finished (canvas, as edited by the owner).
 */
@Composable
fun OilShelf(
    items: List<InventoryItem>,
    name: (InventoryItem) -> String,
    packLabel: (InventoryItem) -> String,
    countLabel: (InventoryItem) -> String,
    describe: (InventoryItem) -> String,
    onOpen: ((InventoryItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(St.Sand).border(1.5.dp, St.SandDeep, shape)
            .padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        items.chunked(3).forEachIndexed { row, shelf ->
            Box {
                if (row > 0) {
                    Box(
                        Modifier.offset(y = (-13).dp).padding(horizontal = 6.dp).fillMaxWidth().height(3.dp)
                            .clip(RoundedCornerShape(2.dp)).background(ShelfInk.Divider),
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    shelf.forEachIndexed { i, item ->
                        ShelfCell(
                            item = item,
                            upright = i > 0,
                            name = name(item),
                            pack = packLabel(item),
                            count = countLabel(item),
                            description = describe(item),
                            onOpen = onOpen?.let { open -> { open(item) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - shelf.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ShelfCell(
    item: InventoryItem,
    upright: Boolean,
    name: String,
    pack: String,
    count: String,
    description: String,
    onOpen: (() -> Unit)?,
    modifier: Modifier,
) {
    val finished = item.stock == Stock.Finished
    Column(
        modifier
            .drawBehind {
                if (finished) {
                    // The outline reaches a little above and below the compartment.
                    val l = 3.dp.toPx(); val t = -8.dp.toPx(); val r = size.width - 3.dp.toPx(); val b = size.height + 6.dp.toPx()
                    val radius = CornerRadius(18.dp.toPx())
                    drawRoundRect(St.Brick.copy(alpha = 0.07f), Offset(l, t), Size(r - l, b - t), radius)
                    val sw = 3.dp.toPx()
                    drawRoundRect(
                        St.Brick, Offset(l + sw / 2, t + sw / 2), Size(r - l - sw, b - t - sw), radius,
                        style = Stroke(sw, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx()))),
                    )
                }
            }
            .drawWithContent {
                drawContent()
                if (upright) {
                    val w = 8.dp.toPx(); val h = 140.dp.toPx(); val x = -w / 2
                    drawRoundRect(St.WoodDark, Offset(x, 0f), Size(w, h), CornerRadius(4.dp.toPx()))
                    drawRect(St.Wood, Offset(x, 4.dp.toPx()), Size(2.dp.toPx(), h - 4.dp.toPx()))
                }
            }
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(124.dp), contentAlignment = Alignment.BottomCenter) {
            OilContainer(item.pack, item.oil, item.level, height = containerHeight(item.pack))
        }
        Plank(Modifier.fillMaxWidth())
        Column(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp), color = St.Ink, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(pack, style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp, lineHeight = 18.sp), color = St.Ink2, textAlign = TextAlign.Center, maxLines = 1)
            StockChip(count, item.stock, Modifier.padding(top = 2.dp))
        }
    }
}

internal fun containerHeight(pack: Pack): Dp = when (pack) {
    Pack.Bottle -> 120.dp
    Pack.Can -> 112.dp
    Pack.Tin -> 116.dp
}

/** A shelf plank: wood with a lighter top edge and a soft shadow below. */
@Composable
fun Plank(modifier: Modifier = Modifier, height: Dp = 16.dp) {
    Box(
        modifier.height(height).drawBehind {
            drawRect(St.Wood)
            drawRect(ShelfInk.WoodLight, size = Size(size.width, 3.dp.toPx()))
            drawRect(
                Brush.verticalGradient(listOf(St.WoodDark.copy(alpha = 0.2f), Color.Transparent), startY = size.height, endY = size.height + 6.dp.toPx()),
                topLeft = Offset(0f, size.height), size = Size(size.width, 6.dp.toPx()),
            )
        },
    )
}

/** Up to four small containers on a plank: the Stock tile and the "needs you" card. */
@Composable
fun MiniShelf(items: List<InventoryItem>, height: Dp, modifier: Modifier = Modifier, plank: Dp = 5.dp) {
    Column(modifier.width(IntrinsicSize.Max), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(horizontal = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            items.forEach { OilContainer(it.pack, it.oil, it.level, height = height * (containerHeight(it.pack) / 124.dp)) }
        }
        Box(Modifier.padding(top = 0.dp).height(plank).fillMaxWidth().clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)).background(St.Wood))
    }
}
