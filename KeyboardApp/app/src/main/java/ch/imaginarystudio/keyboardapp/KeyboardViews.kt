package ch.imaginarystudio.keyboardapp

import android.graphics.Paint
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    var key: Key,
) {

    // Cut the part away from the center using the center line
    // from this to the other keyInfo

    // we can do an algorithm like this, as we know we will only get convex polygons
    // if we cut away slices from any convex staring polygon (here rectangle)
    fun cutAway(other: KeyInfo) {
        val centerLine = constructPerpendicularBisector(position, other.position)

        // store both polygons separated by center line
        // if we have a cut point the two cut points get added to both, so we have two adjacent new polygons
        // if we don't hit, we will always add to the first one (pointsLeft)
        // TODO: do not use dynamic list as this might be very inefficient
        var pointsLeft = mutableListOf<Vec2>()
        var pointsRight = mutableListOf<Vec2>()

        var phase = 0 // phase 0 is before first segment hit, 1 after, 2 after second hit

        val count = boundary.corners.count()
        for (i in 0 until count) {
            var i1 = i
            var i2 = (i+1) % count
            val p1 = boundary.corners[i1]
            val p2 = boundary.corners[i2]
            var segment = Segment(p1, p2)

            var intersects = centerLine.intersectsSegment(segment)
            var point = centerLine.intersectionPoint(segment.toLine())

            if (phase == 0) {
                pointsLeft.add(p1)
            }

            if (phase == 1) {
                pointsRight.add(p1)
            }

            if (phase == 2) {
                pointsLeft.add(p1)
            }
            if (intersects && point != null) {
                phase++
                pointsLeft.add(point)
                pointsRight.add(point)
            }

        }

        if (phase == 0) return
        assert(phase == 2)

        var poly_a = Polygon(pointsRight)
        var poly_b = Polygon(pointsLeft)

        if (poly_a.contains(position)) {
            boundary = poly_a
            return
        }

        if (poly_b.contains(position)) {
            boundary = poly_b
            return
        }

        assert(false) // we should never get here
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
fun addKey(keyInfos: SnapshotStateList<KeyInfo>, position: Vec2, key: Key) {
    var keyInfo = KeyInfo(position, boundary = Polygon(), key)
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

fun DrawScope.drawKeyField(key: KeyInfo, shrinkFactor: Float, bgColor: Color=Color.DarkGray, borderColor: Color = Color.Black) {
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

    // draw background
    var path = Path()
    path.moveTo(corners[0].x, corners[0].y)
    path.fillType = PathFillType.EvenOdd
    for (c in corners) {
        path.lineTo(c.x, c.y)
    }
    path.close()
    drawPath(path = path, color = bgColor)

    drawPoints(points = corners,
        PointMode.Polygon,
        brush = SolidColor(borderColor),
        cap = StrokeCap.Round,
        strokeWidth = 2.0f,
    )


}

fun DrawScope.DrawKeyboard(keyInfos: SnapshotStateList<KeyInfo>, keyPaint: Paint, modifierShift: Boolean) {

    // draw background
    drawRoundRect(
        brush = Brush.linearGradient(listOf(Color.Gray,Color.DarkGray)),
        cornerRadius = CornerRadius(4f, 4f),
        style = Fill
    )

    // draw keys
    for ((i, keyInfo) in keyInfos.withIndex()) {
        val p = Offset(keyInfo.position.x.toFloat(), keyInfo.position.y.toFloat());
        drawKeyField(keyInfo, 0.0f, bgColor = Color.Gray)
        var key = keyInfo.key.code
        if (modifierShift) {
            key = key.uppercase()
        }
        drawContext.canvas.nativeCanvas.drawText(
            key, p.x,p.y + keyPaint.textSize/2, keyPaint
        )
    }
}

fun DrawScope.DrawBackspace(pos: Offset, keyPaint: Paint) {
    var left = pos.x - keyPaint.textSize
    var right = pos.x + keyPaint.textSize
    var top = pos.y + keyPaint.textSize/2
    var bot = pos.y - keyPaint.textSize/2

    drawContext.canvas.nativeCanvas.drawRoundRect(
        left, top, right, bot, 2f, 2f, keyPaint
    )

    // TODO: make arrow head
    drawContext.canvas.nativeCanvas.drawCircle(
        left, pos.y, keyPaint.textSize/2, keyPaint
    )
}

fun closestKey(keyInfos: SnapshotStateList<KeyInfo>, position: Vec2): Key {
    var closestIndex = -1
    var closestDist = Float.MAX_VALUE
    for (i in 0 until keyInfos.count()) {
        var dist = (position - keyInfos[i].position).norm()
        if (dist < closestDist) {
            closestDist = dist.toFloat()
            closestIndex = i
        }
    }
    return keyInfos[closestIndex].key
}

@Composable
fun KeyboardView(keyInfos: SnapshotStateList<KeyInfo>, keyPaint: Paint, modifierShift: MutableState<Boolean>) {

    val ctx = LocalContext.current

    Canvas(
        modifier = Modifier
            .height(300.dp)
            .padding(2.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val key = closestKey(keyInfos, Vec2(offset.x, offset.y))
                    val ic = (ctx as KeyboardIMEService).currentInputConnection
                    if (!key.isControlChar) {
                        var str = key.code

                        if (modifierShift.value) {
                            str = str.uppercase()
                            modifierShift.value = false
                        }

                        ic.commitText(str, str.length)
                    } else {
                        when (key.code) {
                            "↩" -> {
                                // TODO: handle closing and reopening of window
                                ic.performEditorAction(EditorInfo.IME_ACTION_GO)
                                //ic.commitText("\n", 1)
                                ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                            }

                            "⇐" -> {
                                if (TextUtils.isEmpty(ic.getSelectedText(0)))
                                    ic.deleteSurroundingText(1, 0)
                                else
                                    ic.commitText("", 0)
                            }

                            "⇧" -> modifierShift.value = !modifierShift.value
                        }
                    }
                }
            }
    ) {
        DrawKeyboard(keyInfos, keyPaint, modifierShift.value)
    }
}

class Key(var code: String, var isControlChar: Boolean = false)

@OptIn(ExperimentalTextApi::class)
@Composable
fun KeyboardConstructView(keyInfos: SnapshotStateList<KeyInfo>, finishedConstruction: MutableState<Boolean>, keyPaint: Paint) {

    val keys = mutableListOf<Key>(
            Key("a"),
            Key("b"),
            Key("c"),
            Key("d"),
            Key("e"),
            Key("f"),
            Key("g"),
            Key("h"),
            Key("i"),
            Key("j"),
            Key("k"),
            Key("l"),
            Key("m"),
            Key("n"),
            Key("o"),
            Key("p"),
            Key("q"),
            Key("r"),
            Key("s"),
            Key("t"),
            Key("u"),
            Key("v"),
            Key("w"),
            Key("x"),
            Key("y"),
            Key("z"),
            Key(" "),
            Key("↩", true),
            Key("⇐", true),
            Key("⇧", true),
        )

    val curSelection = remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        Box(
            modifier = Modifier
                .padding(0.dp)
                .fillMaxWidth()
                .background(
                    color = Color.Red
                )
        ) {
            Text(text = "Constructing the Keyboard. Next Key drop is \"" + keys[curSelection.value].code + "\"")
        }
        Canvas(
            modifier = Modifier
                .height(300.dp)
                .padding(0.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        addKey(keyInfos, Vec2(offset.x, offset.y), keys[curSelection.value])
                        recalculateBoundaries(keyInfos, size)
                        curSelection.value += 1
                        if (curSelection.value == keys.count()) {
                            finishedConstruction.value = true;
                        }

                    }
                }
        ) {
            DrawKeyboard(keyInfos, keyPaint, false)
        }
    }
}