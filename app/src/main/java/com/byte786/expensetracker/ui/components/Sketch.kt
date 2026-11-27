package com.byte786.expensetracker.ui.components

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.byte786.expensetracker.ui.theme.LocalHandmade
import kotlin.random.Random

/**
 * A card outline. With the handmade look it's two slightly wobbly pencil lines
 * (like a box drawn by hand); otherwise a plain 1 dp border in [shape].
 */
fun Modifier.sketchBorder(shape: Shape, corner: Dp = 16.dp): Modifier = composed {
    val hand = LocalHandmade.current
    if (!hand) {
        this.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    } else {
        val ink = MaterialTheme.colorScheme.onSurface
        this.drawWithCache {
            val inset = 1.6.dp.toPx()
            val r = (corner.toPx() * 1.2f).coerceAtMost(minOf(size.width, size.height) / 2.5f)
            val amp = 1.1.dp.toPx()
            val seed = (size.width * 31 + size.height).toInt()
            val first = wobblyBox(size, inset, r, amp, seed)
            val second = wobblyBox(size, inset + 0.8.dp.toPx(), r, amp * 1.3f, seed + 7)
            val w1 = 1.5.dp.toPx()
            val w2 = 1.dp.toPx()
            onDrawBehind {
                drawPath(first, ink.copy(alpha = 0.62f), style = Stroke(w1, cap = StrokeCap.Round))
                drawPath(second, ink.copy(alpha = 0.22f), style = Stroke(w2, cap = StrokeCap.Round))
            }
        }
    }
}

/** A rounded box traced with small random wobbles; the stroke overlaps a little where it starts and ends. */
private fun wobblyBox(size: Size, inset: Float, r: Float, amp: Float, seed: Int): Path {
    val rnd = Random(seed)
    fun j() = (rnd.nextFloat() * 2f - 1f) * amp
    val l = inset
    val t = inset
    val rr = size.width - inset
    val b = size.height - inset
    val step = 22f * (amp / 1.1f).coerceAtLeast(1f)
    val p = Path()
    p.moveTo(l + r * 0.6f, t + j())
    // top edge
    var x = l + r
    while (x < rr - r) { p.lineTo(x, t + j()); x += step }
    p.quadraticBezierTo(rr + j() * 0.5f, t + j() * 0.5f, rr + j(), t + r)
    // right edge
    var y = t + r
    while (y < b - r) { p.lineTo(rr + j(), y); y += step }
    p.quadraticBezierTo(rr + j() * 0.5f, b + j() * 0.5f, rr - r, b + j())
    // bottom edge
    x = rr - r
    while (x > l + r) { p.lineTo(x, b + j()); x -= step }
    p.quadraticBezierTo(l + j() * 0.5f, b + j() * 0.5f, l + j(), b - r)
    // left edge
    y = b - r
    while (y > t + r) { p.lineTo(l + j(), y); y -= step }
    p.quadraticBezierTo(l + j() * 0.5f, t + j() * 0.5f, l + r, t + j())
    // overshoot like a real pen stroke
    p.lineTo(l + r * 1.9f, t + j())
    return p
}
