@file:OptIn(ExperimentalMaterial3Api::class)

package com.byte786.expensetracker.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MOVE = 0
private const val TL = 1
private const val TR = 2
private const val BL = 3
private const val BR = 4

/** A starting crop box: 90% of the image, or the biggest centred square. */
private fun defaultRect(w: Float, h: Float, square: Boolean): Rect {
    return if (square) {
        val side = min(w, h) * 0.9f
        Rect(Offset((w - side) / 2, (h - side) / 2), Size(side, side))
    } else {
        Rect(w * 0.05f, h * 0.05f, w * 0.95f, h * 0.95f)
    }
}

/** Moves one corner (or the whole box) by [d], keeping it inside [w]×[h] and at least [minSide]. */
private fun adjust(r: Rect, handle: Int, d: Offset, w: Float, h: Float, square: Boolean, minSide: Float): Rect {
    if (handle == MOVE) {
        val dx = d.x.coerceIn(-r.left, w - r.right)
        val dy = d.y.coerceIn(-r.top, h - r.bottom)
        return r.translate(dx, dy)
    }
    // The opposite corner stays put.
    val ax = if (handle == TL || handle == BL) r.right else r.left
    val ay = if (handle == TL || handle == TR) r.bottom else r.top
    val cx = (if (handle == TL || handle == BL) r.left else r.right) + d.x
    val cy = (if (handle == TL || handle == TR) r.top else r.bottom) + d.y
    val goLeft = handle == TL || handle == BL
    val goUp = handle == TL || handle == TR
    val roomX = if (goLeft) ax else w - ax
    val roomY = if (goUp) ay else h - ay
    var sx = abs(ax - cx).coerceIn(minSide, roomX)
    var sy = abs(ay - cy).coerceIn(minSide, roomY)
    if (square) {
        val side = max(sx, sy).coerceAtMost(min(roomX, roomY)).coerceAtLeast(min(minSide, min(roomX, roomY)))
        sx = side; sy = side
    }
    val left = if (goLeft) ax - sx else ax
    val top = if (goUp) ay - sy else ay
    return Rect(left, top, left + sx, top + sy)
}

/**
 * Full-screen crop: drag inside the box to move it, drag a corner to resize.
 * [forceSquare] is used for icons. [onDone] gets the cropped bitmap.
 */
@Composable
fun CropDialog(source: Bitmap, forceSquare: Boolean, title: String, onDismiss: () -> Unit, onDone: (Bitmap) -> Unit) {
    var bmp by remember(source) { mutableStateOf(source) }
    var square by remember { mutableStateOf(forceSquare) }
    // Crop box in bitmap pixels.
    var rect by remember(source) { mutableStateOf(defaultRect(source.width.toFloat(), source.height.toFloat(), forceSquare)) }
    val image = remember(bmp) { bmp.asImageBitmap() }

    fun reset(newSquare: Boolean = square) {
        rect = defaultRect(bmp.width.toFloat(), bmp.height.toFloat(), newSquare)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Cancel", tint = Color.White) }
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        val l = rect.left.toInt().coerceIn(0, bmp.width - 1)
                        val t = rect.top.toInt().coerceIn(0, bmp.height - 1)
                        val cw = rect.width.toInt().coerceIn(1, bmp.width - l)
                        val ch = rect.height.toInt().coerceIn(1, bmp.height - t)
                        onDone(Bitmap.createBitmap(bmp, l, t, cw, ch))
                    }) {
                        Icon(Icons.Filled.Crop, null); Spacer(Modifier.width(6.dp)); Text("Crop")
                    }
                    Spacer(Modifier.width(8.dp))
                }

                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    val maxW = constraints.maxWidth.toFloat()
                    val maxH = constraints.maxHeight.toFloat()
                    val scale = min(maxW / bmp.width, maxH / bmp.height)
                    val density = LocalDensity.current
                    val grab = with(density) { 32.dp.toPx() }
                    val bw = bmp.width.toFloat()
                    val bh = bmp.height.toFloat()
                    Box(Modifier.size(with(density) { (bw * scale).toDp() }, with(density) { (bh * scale).toDp() })) {
                        Image(image, "Photo to crop", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                        Canvas(
                            Modifier.fillMaxSize().pointerInput(bmp, square, scale) {
                                var handle = MOVE
                                detectDragGestures(
                                    onDragStart = { p ->
                                        val r = rect
                                        val corners = listOf(
                                            TL to Offset(r.left, r.top), TR to Offset(r.right, r.top),
                                            BL to Offset(r.left, r.bottom), BR to Offset(r.right, r.bottom),
                                        ).map { (k, o) -> k to (o * scale - p).getDistance() }
                                        val near = corners.minByOrNull { it.second }!!
                                        handle = if (near.second <= grab) near.first else MOVE
                                    },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        rect = adjust(rect, handle, drag / scale, bw, bh, square, min(bw, bh) * 0.08f)
                                    },
                                )
                            },
                        ) {
                            val r = Rect(rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale)
                            val dim = Color(0xAA000000)
                            drawRect(dim, Offset.Zero, Size(size.width, r.top))
                            drawRect(dim, Offset(0f, r.bottom), Size(size.width, size.height - r.bottom))
                            drawRect(dim, Offset(0f, r.top), Size(r.left, r.height))
                            drawRect(dim, Offset(r.right, r.top), Size(size.width - r.right, r.height))
                            // Rule-of-thirds grid
                            for (i in 1..2) {
                                val x = r.left + r.width * i / 3
                                val y = r.top + r.height * i / 3
                                drawLine(Color(0x66FFFFFF), Offset(x, r.top), Offset(x, r.bottom), 1f)
                                drawLine(Color(0x66FFFFFF), Offset(r.left, y), Offset(r.right, y), 1f)
                            }
                            drawRect(Color.White, r.topLeft, r.size, style = Stroke(2.dp.toPx()))
                            val c = 6.dp.toPx()
                            listOf(r.topLeft, r.topRight, r.bottomLeft, r.bottomRight).forEach { drawCircle(Color.White, c, it) }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!forceSquare) {
                        FilterChip(selected = !square, onClick = { square = false; reset(false) }, label = { Text("Free", color = Color.White) })
                        FilterChip(selected = square, onClick = { square = true; reset(true) }, label = { Text("Square", color = Color.White) })
                    }
                    IconButton(onClick = {
                        val m = Matrix().apply { postRotate(90f) }
                        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                        rect = defaultRect(bmp.width.toFloat(), bmp.height.toFloat(), square)
                    }) { Icon(Icons.Filled.Rotate90DegreesCw, "Rotate", tint = Color.White) }
                    TextButton(onClick = { reset() }) { Text("Reset", color = Color.White) }
                }
            }
        }
    }
}
