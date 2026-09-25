package org.darulhuda.nabiurrahmah.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A quiet lattice of eight-pointed stars (khatam), the motif of Islamic geometric
 * art, drawn as vectors so it is crisp at any size and costs no image memory.
 * Drawn behind the content, above any background set earlier in the chain.
 */
fun Modifier.islamicPattern(
    color: Color,
    cell: Dp = 44.dp,
    strokeWidth: Dp = 1.dp,
): Modifier = drawWithCache {
    val step = cell.toPx()
    val outer = step * 0.34f
    // Inner radius where the two overlapping squares of the star cross.
    val inner = outer * (cos(PI / 4) / cos(PI / 8)).toFloat()
    val path = Path()
    var row = 0
    var y = 0f
    while (y <= size.height + step) {
        var x = if (row % 2 == 0) 0f else step / 2
        while (x <= size.width + step) {
            for (i in 0 until 16) {
                val radius = if (i % 2 == 0) outer else inner
                val angle = PI / 8 * i - PI / 2
                val px = x + (radius * cos(angle)).toFloat()
                val py = y + (radius * sin(angle)).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            x += step
        }
        y += step / 2
        row++
    }
    val stroke = Stroke(strokeWidth.toPx())
    onDrawBehind { drawPath(path, color, style = stroke) }
}
