package ch.imaginarystudio.keyboardapp

import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas

// convert the Vec2 list to a list of offset in pixel_space using width
// shrinkPixels is used to inset the corners inwards (when positive value given)
// TODO: improve efficiency.. this function is not very efficient and readable..
fun offsetsForDrawing(corners: List<Vec2>, width: Float, shrinkPixels: Float = 0f) : List<Offset> {

    val offsets = if (shrinkPixels == 0f) {
        corners.map {
                c -> positionToOffset(c, width)
        }.toMutableList()
    } else {
        // shrink the corners inwards
        val shrinkPixelsF = shrinkPixels / width
        corners.mapIndexed { i, corner ->
            val iPrev = (i - 1 + corners.count()) % corners.count()
            val iNext = (i + 1) % corners.count()

            // normalized directions from this corner (x) to the previous and next
            val xToPrev = (corners[iPrev] - corner).normalized()
            val xToNext = (corners[iNext] - corner).normalized()

            // rotate by 90 degrees
            val perpendicularPrevious = Vec2(-xToPrev.y, xToPrev.x)
            val perpendicularNext = Vec2(xToNext.y, -xToNext.x)

            // calculate the factors to by which to move the corner inwards
            // the dot product is used to get the projection onto the perpendicular
            // TODO: make sure the dot products are never 0..
            val f1 = shrinkPixelsF / (perpendicularPrevious * xToNext)
            val f2 = shrinkPixelsF / (perpendicularNext * xToPrev)

            val shifted = corner + xToNext * f1 + xToPrev * f2
            positionToOffset(shifted, width)
        }.toMutableList()
    }

    // add the first one two times for drawing as a polygon later on
    if (offsets.isNotEmpty()) {
        offsets.add(offsets[0])
    }

    return offsets.toList()
}

fun DrawScope.drawPolygon(polygon: Polygon, strokeWidth: Float, color: Color = Color.Black) {

    val corners = offsetsForDrawing(polygon.corners, size.width)

    drawPoints(points = corners,
        PointMode.Polygon,
        brush = Brush.radialGradient(listOf(Color.Black, color)),
        strokeWidth = strokeWidth,
    )
}

fun DrawScope.drawKeyField(key: KeyInfo, shrinkPixels: Float, theme: KeyboardTheme) {

    val corners = offsetsForDrawing(key.boundary.corners, size.width, shrinkPixels)

    // draw background
    val path = Path()
    path.moveTo(corners[0].x, corners[0].y)
    path.fillType = PathFillType.EvenOdd
    for (c in corners) { path.lineTo(c.x, c.y) }
    path.close()
    drawPath(path = path, color = theme.colorTheme.keyBackground)

    drawPoints(points = corners,
        PointMode.Polygon,
        brush = SolidColor(theme.colorTheme.background),
        cap = StrokeCap.Round,
        strokeWidth = 2.0f,
    )
}

fun DrawScope.drawKeyboard(keyboardData: KeyboardData, keyboardState: KeyboardState, theme: KeyboardTheme) {

    // draw background
    drawRoundRect(
        theme.colorTheme.background,
        cornerRadius = CornerRadius(4f, 4f),
        style = Fill
    )

    var keyInfos = keyboardData.alphaPage
    if (keyboardState.modifierNumeric.value) {
        keyInfos = keyboardData.numericPage
    }

    // draw keys
    for (keyInfo in keyInfos) {
        val p = positionToOffset(keyInfo.position, size.width)

        drawKeyField(keyInfo, theme.shrinkKeyDp.toPx(), theme)
        var key = keyInfo.key.code
        if (keyboardState.modifierShift.value) {
            key = key.uppercase()
        }

        drawContext.canvas.nativeCanvas.drawText(
            key, p.x, p.y + theme.keyPaint.textSize/2, theme.keyPaint
        )
    }
}

fun DrawScope.drawBackspace(pos: Offset, keyPaint: Paint) {
    val left = pos.x - keyPaint.textSize
    val right = pos.x + keyPaint.textSize
    val top = pos.y + keyPaint.textSize/2
    val bot = pos.y - keyPaint.textSize/2

    drawContext.canvas.nativeCanvas.drawRoundRect(
        left, top, right, bot, 2f, 2f, keyPaint
    )

    // TODO: make arrow head
    drawContext.canvas.nativeCanvas.drawCircle(
        left, pos.y, keyPaint.textSize/2, keyPaint
    )
}