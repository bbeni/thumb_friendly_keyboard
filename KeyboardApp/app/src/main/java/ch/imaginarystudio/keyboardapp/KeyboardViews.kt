package ch.imaginarystudio.keyboardapp

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun MockKeyboard() {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Gray),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(color = Color.Black, text = "This will be the keyboard.. maybe")
        Button(modifier = Modifier.width(250.dp), onClick = { }) {
            Text(text = "A B C buttons")
        }
    }
}

data class KeyInfo(
    var position: Vec2,
    var boundary: Polygon = Polygon(),
    var key: String,
) {

    // Cut the part away from the center using the center line
    // from this to the other keyInfo
    fun cutAway(other: KeyInfo) {
        val center_line = constructPerpendicularBisector(position, other.position)

        var phase = 0 // phase 0 is before first hit, 1 after, 2 after second hit

        var points_left = mutableListOf<Vec2>()
        var points_right = mutableListOf<Vec2>()

        println("Hello Corners")
        println(boundary.corners)

        val count = boundary.corners.count()
        for (i in 0 until count) {
            var i1 = i
            var i2 = (i+1) % count
            val p1 = boundary.corners[i1]
            val p2 = boundary.corners[i2]
            var segment = Segment(p1, p2)

            var intersects = center_line.intersectsSegment(segment)
            var point = center_line.intersectionPoint(segment.toLine())//segment.intersection(center_line)


            if (phase == 0) {
                points_left.add(p1)
            }

            if (phase == 1) {
                points_right.add(p1)
            }

            if (phase == 2) {
                points_left.add(p1)
            }
            if (intersects && point != null) {
                phase++
                points_left.add(point)
                points_right.add(point)
            }

        }

        if (phase == 0) return
        assert(phase == 2)

        var poly_a = Polygon(points_right)
        var poly_b = Polygon(points_left)

        println("Bye Corners")
        println(boundary.corners)

        if (poly_a.contains(position)) {
            boundary = poly_a
            return
        }

        if (poly_b.contains(position)) {
            boundary = poly_b
            return
        }

        assert(false, { "Should never get here..." })
    }
}



fun recalculateBoundaries(keyInfos: SnapshotStateList<KeyInfo>, size: IntSize) {
    if (keyInfos.isEmpty()) return

    val offsetPixels = 5f
    val canvasBoundary = Polygon(mutableListOf<Vec2>(
        Vec2(offsetPixels, offsetPixels),
        Vec2(offsetPixels,size.height.toFloat() - offsetPixels),
        Vec2(size.width.toFloat() - offsetPixels, size.height.toFloat() - offsetPixels),
        Vec2(size.width.toFloat() - offsetPixels, offsetPixels),
    ))

    if (keyInfos.count() == 1) {
        keyInfos[0].boundary = canvasBoundary.copy()
        return
    }

    for (k in keyInfos) {
        println(k);
    }

    // take the rectangle for every key and cut it by perpendicular bisectors
    for (keyInfo in keyInfos) {
        keyInfo.boundary = canvasBoundary.copy()

        for (other in keyInfos) {
            if (other != keyInfo) {
                keyInfo.cutAway(other)
            }
        }
    }
}
fun addKey(keyInfos: SnapshotStateList<KeyInfo>, position: Vec2) {
    var keyInfo = KeyInfo(position, boundary = Polygon(), "Q")
    keyInfos.add(keyInfo)
}

fun DrawScope.drawHexagon(pos: Offset, scale: Float) {
    drawPoints(
        points = listOf(
            pos + Offset(0.5f, 0.8660254f) * scale,
            pos + Offset(1f, 0f) * scale,
            pos + Offset(0.5f, -0.8660254f) * scale,
            pos + Offset(-0.5f, -0.8660254f) * scale,
            pos + Offset(-1f, 0f) * scale,
            pos + Offset(-0.5f, 0.8660254f) * scale,
            pos + Offset(0.5f, 0.8660254f) * scale,
        ),
        PointMode.Polygon,
        brush = Brush.radialGradient(listOf(Color.Black, Color.Black)),
        strokeWidth = 3f,
    )
}

// TODO: this is not so efficient..
fun offsetsForDrawing(corners: List<Vec2>) : List<Offset> {
    var offsets = corners
        .map { c -> Offset(c.x.toFloat(), c.y.toFloat()) }
        .toMutableList()

    // add the first one for drawing as a polygon later on
    if (offsets.isNotEmpty()) {
        offsets.add(offsets[0])
    };
    return offsets.toList()
}

fun DrawScope.drawPolygon(polygon: Polygon, strokeWidth: Float, color: Color = Color.Black) {
    /*
    for (i in 0 until polygon.corners.count()) {
        val p1 = polygon.corners[i]
        val p2 = polygon.corners[(i+1) % polygon.corners.count()]
        drawLine(
            brush = SolidColor(color),
            Offset(p1.x.toFloat(), p1.y.toFloat()),
            Offset(p2.x.toFloat(), p2.y.toFloat()),
            strokeWidth = strokeWidth,
        )
    } */

    var corners = offsetsForDrawing(polygon.corners)

    drawPoints(points = corners,
        PointMode.Polygon,
        brush = Brush.radialGradient(listOf(Color.Black, color)),
        strokeWidth = strokeWidth,
    )
}

fun DrawScope.drawKey(key: KeyInfo, shrinkFactor: Float, bgColor: Color=Color.DarkGray, borderColor: Color = Color.Black) {
    /*
    for (i in 0 until polygon.corners.count()) {
        val p1 = polygon.corners[i]
        val p2 = polygon.corners[(i+1) % polygon.corners.count()]
        drawLine(
            brush = SolidColor(color),
            Offset(p1.x.toFloat(), p1.y.toFloat()),
            Offset(p2.x.toFloat(), p2.y.toFloat()),
            strokeWidth = strokeWidth,
        )
    } */

    var corners = offsetsForDrawing(
        key.boundary.shrunkCorners(shrinkFactor, key.position)
    )

    // TODO draw background
    /*
    drawPoints(points = corners,
        PointMode.Polygon,
        brush = SolidColor(bgColor),
        strokeWidth = 12.0f,
    )*/


    drawPoints(points = corners,
        PointMode.Polygon,
        brush = SolidColor(borderColor),
        cap = StrokeCap.Round,
        strokeWidth = 2.0f,
    )
}




@OptIn(ExperimentalTextApi::class)
@Composable
fun KeyboardConstructView() {
    val paint = Paint().apply {
        textAlign = Paint.Align.CENTER
        textSize = 64f
        color = Color.White.toArgb()
    }

    val keyInfos = remember { mutableStateListOf<KeyInfo>() }

    Canvas(
        modifier = Modifier
            .size(300.dp)
            .padding(2.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    addKey(keyInfos, Vec2(offset.x, offset.y))
                    recalculateBoundaries(keyInfos, size)
                }
            }
    ) {
        var x = drawRoundRect(
            brush = Brush.linearGradient(listOf(Color.Gray,Color.DarkGray)),
            cornerRadius = CornerRadius(4f, 4f),
            style = Fill
        )

        for ((i, k1) in keyInfos.withIndex()) {
            for (j in i+1 until keyInfos.count()) {
                val p2 = keyInfos[j].position
                val p1 = k1.position
                drawLine(
                    brush=SolidColor(Color.Green),
                    Offset(p1.x.toFloat(), p1.y.toFloat()),
                    Offset(p2.x.toFloat(), p2.y.toFloat()),
                    strokeWidth = 4f,
                    )
            }
        }

        for ((i, keyInfo) in keyInfos.withIndex()) {
            val p = Offset(keyInfo.position.x.toFloat(), keyInfo.position.y.toFloat());
            //drawHexagon(p, 70f)
            //drawPolygon(keyInfo.boundary, 3f, Color.Green)
            drawKey(keyInfo, 0.00f)
            drawContext.canvas.nativeCanvas.drawText(
                keyInfo.key, p.x,p.y + 32f, paint
            )
        }
    }
}