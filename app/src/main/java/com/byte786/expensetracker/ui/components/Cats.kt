package com.byte786.expensetracker.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.byte786.expensetracker.data.TxnType
import kotlin.math.min

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

val CatFur = Color(0xFFE8913A)
private val CatFurDark = Color(0xFFC46F22)
private val CatInk = Color(0xFF2A1F17)
private val InnerEar = Color(0xFFF6C7A0)
private val Whisker = Color(0xFF7A4A1E)
private val Blush = Color(0xFFF29C9C)
private val TearBlue = Color(0xFF4FA3E0)

/** Paths in a 64×64 box (faces) or a 150×72 box (running cat). */
private object CatPaths {
    val earL = svg("M11 30 L13 7 L29 19 Z")
    val earR = svg("M53 30 L51 7 L35 19 Z")
    val innerL = svg("M15 25 L16 13 L24 20 Z")
    val innerR = svg("M49 25 L48 13 L40 20 Z")
    val happyEyes = svg("M21 36 q4 -5 8 0 M35 36 q4 -5 8 0")
    val sadBrows = svg("M19 29 l9 3 M45 29 l-9 3")
    val sadEyes = svg("M21 36 q4 3 8 0 M35 36 q4 3 8 0")
    val tear = svg("M0 0 q-2 4 0 6 q2 -2 0 -6 Z")
    val nose = svg("M30 41 h4 l-2 2.5 Z")
    val smile = svg("M32 43.5 q-3 5 -7 1.5 M32 43.5 q3 5 7 1.5")
    val frown = svg("M26 50 q6 -5 12 0")
    val whiskers = svg("M8 40 h9 M8 45 l9 -2 M56 40 h-9 M56 45 l-9 -2")
    val transferArrows = svg("M43 45.5 h9 l-2.5 -2.5 M53 50.5 h-9 l2.5 2.5")

    val tail = svg("M28 38 q-16 -2 -14 -20")
    val stripes = svg("M44 29 q3 6 0 12 M52 28 q3 6 0 13 M60 29 q3 6 0 12")
    val runEarL = svg("M74 25 L76 13 L82 21 Z")
    val runEarR = svg("M84 21 L91 12 L91 25 Z")
    val runNose = svg("M92 33 l2.5 1 l-2.5 1.2 Z")
    val runWhiskers = svg("M88 36 h12 M88 38 l11 2")
}

enum class CatMood { HAPPY, CRYING, PLAIN }

/**
 * A round cat face. [badge] (a [TxnType]) adds a small −, + or ⇄ badge,
 * used on the add-menu buttons.
 */
@Composable
fun CatFace(
    mood: CatMood,
    modifier: Modifier = Modifier,
    fur: Color = CatFur,
    eyes: Color = CatInk,
    nose: Color = CatInk,
    badge: Int? = null,
    description: String? = null,
) {
    val tearT = if (mood == CatMood.CRYING) {
        val inf = rememberInfiniteTransition(label = "tears")
        val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "tear")
        t
    } else 0f
    val badgeColor = when (badge) {
        TxnType.EXPENSE -> Color(0xFFB3261E)
        TxnType.INCOME -> Color(0xFF1E7A34)
        TxnType.TRANSFER -> Color(0xFF4E5D6C)
        else -> null
    }
    val sem = if (description != null) Modifier.semantics { contentDescription = description; role = Role.Image } else Modifier
    Canvas(modifier.then(sem)) {
        val s = size.minDimension / 64f
        withTransform({
            translate((size.width - 64f * s) / 2f, (size.height - 64f * s) / 2f)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawPath(CatPaths.earL, fur)
            drawPath(CatPaths.earR, fur)
            drawPath(CatPaths.innerL, InnerEar)
            drawPath(CatPaths.innerR, InnerEar)
            drawCircle(fur, 22f, Offset(32f, 37f))
            when (mood) {
                CatMood.HAPPY -> {
                    drawPath(CatPaths.happyEyes, eyes, style = Stroke(2.5f, cap = StrokeCap.Round))
                    drawCircle(Blush, 3f, Offset(20f, 43f))
                    drawCircle(Blush, 3f, Offset(44f, 43f))
                    drawPath(CatPaths.nose, nose)
                    drawPath(CatPaths.smile, CatInk, style = Stroke(2.2f, cap = StrokeCap.Round))
                }
                CatMood.CRYING -> {
                    drawPath(CatPaths.sadBrows, CatInk, style = Stroke(2.2f, cap = StrokeCap.Round))
                    drawPath(CatPaths.sadEyes, eyes, style = Stroke(2.5f, cap = StrokeCap.Round))
                    val dy = tearT * 7f
                    val a = 1f - tearT
                    translate(23f, 40f + dy) { drawPath(CatPaths.tear, TearBlue, alpha = a) }
                    translate(41f, 40f + dy) { drawPath(CatPaths.tear, TearBlue, alpha = a) }
                    drawPath(CatPaths.nose, nose)
                    drawPath(CatPaths.frown, CatInk, style = Stroke(2.2f, cap = StrokeCap.Round))
                }
                CatMood.PLAIN -> {
                    drawCircle(eyes, 2.8f, Offset(25f, 34f))
                    drawCircle(eyes, 2.8f, Offset(39f, 34f))
                    drawPath(CatPaths.nose, nose)
                }
            }
            drawPath(CatPaths.whiskers, Whisker, style = Stroke(1.4f, cap = StrokeCap.Round))
            if (badgeColor != null) {
                drawCircle(badgeColor, 11f, Offset(48f, 48f))
                when (badge) {
                    TxnType.EXPENSE -> drawLine(Color.White, Offset(43f, 48f), Offset(53f, 48f), 3f, StrokeCap.Round)
                    TxnType.INCOME -> {
                        drawLine(Color.White, Offset(43f, 48f), Offset(53f, 48f), 3f, StrokeCap.Round)
                        drawLine(Color.White, Offset(48f, 43f), Offset(48f, 53f), 3f, StrokeCap.Round)
                    }
                    else -> drawPath(CatPaths.transferArrows, Color.White, style = Stroke(2.4f, cap = StrokeCap.Round))
                }
            }
        }
    }
}

/** A ginger cat running after a coin. Loops forever. */
@Composable
fun RunningCat(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "run")
    val step by inf.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "step",
    )
    val coin by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "coin")
    val ground by inf.animateFloat(0f, 24f, infiniteRepeatable(tween(450, easing = LinearEasing)), label = "ground")
    val measurer = rememberTextMeasurer()

    Canvas(modifier.semantics { contentDescription = "A cat running after money" }) {
        val s = min(size.width / 150f, size.height / 72f)
        withTransform({
            translate((size.width - 150f * s) / 2f, (size.height - 72f * s) / 2f)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawLine(
                Color(0xFFD9C2AA), Offset(0f, 66f), Offset(150f, 66f), 2.5f, StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), ground),
            )
            translate(4f * coin, -7f * coin) {
                drawCircle(Color(0xFFF2C14E), 11f, Offset(128f, 36f))
                drawCircle(Color(0xFFB8891F), 11f, Offset(128f, 36f), style = Stroke(2f))
                val layout = measurer.measure(
                    "৳", TextStyle(fontSize = 13f.toSp(), fontWeight = FontWeight.Black, color = Color(0xFF7A5A0E)),
                )
                drawText(layout, topLeft = Offset(128f - layout.size.width / 2f, 36f - layout.size.height / 2f))
            }
            translate(0f, -1.5f * (step + 1f)) {
                rotate(step * 11f, pivot = Offset(28f, 38f)) {
                    drawPath(CatPaths.tail, CatFur, style = Stroke(5.5f, cap = StrokeCap.Round))
                }
                listOf(36f to 1f, 44f to -1f, 60f to -1f, 68f to 1f).forEachIndexed { i, (x, dir) ->
                    rotate(step * 28f * dir, pivot = Offset(x, 47f)) {
                        drawLine(if (i % 2 == 0) CatFurDark else CatFur, Offset(x, 47f), Offset(x, 62f), 5.5f, StrokeCap.Round)
                    }
                }
                drawOval(CatFur, topLeft = Offset(27f, 28f), size = Size(50f, 24f))
                drawPath(CatPaths.stripes, CatFurDark, style = Stroke(2.5f, cap = StrokeCap.Round))
                drawPath(CatPaths.runEarL, CatFur)
                drawPath(CatPaths.runEarR, CatFur)
                drawCircle(CatFur, 11.5f, Offset(82f, 31f))
                drawCircle(CatInk, 2f, Offset(86f, 29f))
                drawPath(CatPaths.runNose, CatInk)
                drawPath(CatPaths.runWhiskers, Whisker, style = Stroke(1.2f, cap = StrokeCap.Round))
            }
        }
    }
}
