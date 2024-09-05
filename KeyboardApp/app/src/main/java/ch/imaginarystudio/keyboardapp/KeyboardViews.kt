package ch.imaginarystudio.keyboardapp

import android.graphics.Paint
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        val pointsLeft = mutableListOf<Vec2>()
        val pointsRight = mutableListOf<Vec2>()

        var phase = 0 // phase 0 is before first segment hit, 1 after, 2 after second hit

        val count = boundary.corners.count()
        for (i in 0 until count) {
            val p1 = boundary.corners[i]
            val p2 = boundary.corners[(i+1) % count]
            val segment = Segment(p1, p2)

            val intersects = centerLine.intersectsSegment(segment)
            val point = centerLine.intersectionPoint(segment.toLine())

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

        val poly_a = Polygon(pointsRight)
        val poly_b = Polygon(pointsLeft)

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
    val keyInfo = KeyInfo(position, Polygon(), key)
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
    val offsets = corners
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

fun DrawScope.drawKeyField(key: KeyInfo, shrinkPixels: Float, bgColor: Color=Color.DarkGray, borderColor: Color = Color.Black) {
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

    val corners = offsetsForDrawing(
        //key.boundary.shrunkCorners(0.02f, key.position)
        key.boundary.shrunkCornersPerpendicularToBorder(shrinkPixels)
    )

    // draw background
    val path = Path()
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

fun DrawScope.DrawKeyboard(keyboardData: KeyboardData, keyboardState: KeyboardState, theme: KeyboardTheme) {

    // draw background
    drawRoundRect(
        brush = Brush.linearGradient(listOf(Color.Gray,Color.DarkGray)),
        cornerRadius = CornerRadius(4f, 4f),
        style = Fill
    )

    var keyInfos = keyboardData.alphaPage
    if (keyboardState.modifierNumeric.value) {
        keyInfos = keyboardData.numericPage
    }

    // draw keys
    for ((i, keyInfo) in keyInfos.withIndex()) {
        //val p = Offset(keyInfo.position.x.toFloat(), keyInfo.position.y.toFloat());

        var offset = Offset(0.0f, 0.0f)
        keyInfo.boundary.corners.forEach {
            offset += Offset(it.x.toFloat(), it.y.toFloat())
        }
        offset /= keyInfo.boundary.corners.count().toFloat() + 0.00000001f

        //val p = Offset(keyInfo.position.x.toFloat(), keyInfo.position.y.toFloat());

        drawKeyField(keyInfo, theme.shrinkKeyDp.toPx(), bgColor = Color.Gray)
        var key = keyInfo.key.code
        if (keyboardState.modifierShift.value) {
            key = key.uppercase()
        }
        drawContext.canvas.nativeCanvas.drawText(
            key, offset.x, offset.y + theme.keyPaint.textSize/2, theme.keyPaint
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

fun handleKey(keyboardState: KeyboardState, key: Key, ic: InputConnection) {
    if (!key.isControlChar) {
        var str = key.code

        if (keyboardState.modifierShift.value) {
            str = str.uppercase()
            keyboardState.modifierShift.value = false
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

            "⇧" -> keyboardState.modifierShift.value =
                !keyboardState.modifierShift.value

            "⁝" -> {
                // TODO: settings
                keyboardState.showSettings.value = true
            }

            "?123" -> {
                keyboardState.modifierNumeric.value =
                    !keyboardState.modifierNumeric.value
            }
        }
    }
}

@Composable
fun KeyboardView(keyboardData: KeyboardData, state: KeyboardState, theme: KeyboardTheme) {

    val ctx = LocalContext.current

    Canvas(
        modifier = Modifier
            .aspectRatio(theme.aspectRatio)
            .padding(0.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // select page and find pressed key
                    var page = keyboardData.alphaPage
                    if (state.modifierNumeric.value) {
                        page = keyboardData.numericPage
                    }
                    val key = closestKey(page, Vec2(offset.x, offset.y))
                    val ic = (ctx as KeyboardIMEService).currentInputConnection

                    handleKey(state, key, ic)
                }
            }
    ) {
        DrawKeyboard(keyboardData, state, theme)
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun KeyboardConstructView(keyboardData: KeyboardData, keyboardState: KeyboardState, theme: KeyboardTheme) {

    val curSelection = remember { mutableStateOf(0) }
    val pageSelection = remember { mutableStateOf(0) }

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

            if (pageSelection.value == 0) {
                Text(text = "Constructing the page 1/2. Next Key drop: \"" + keysPageAlpha[curSelection.value].code + "\" (${curSelection.value + 1}/${keysPageAlpha.count()})")
            } else {
                Text(text = "Constructing the page 2/2. Next Key drop: \"" + keysPageNumeric[curSelection.value].code + "\" (${curSelection.value + 1}/${keysPageNumeric.count()})")
            }

        }
        Canvas(
            modifier = Modifier
                .aspectRatio(theme.aspectRatio)
                .padding(0.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        var toSelect = keysPageAlpha
                        var pageSelected = keyboardData.alphaPage

                        if (pageSelection.value == 1) {
                            toSelect = keysPageNumeric
                            pageSelected = keyboardData.numericPage
                        }

                        val pos = Vec2(offset.x, offset.y)
                        val key = toSelect[curSelection.value]

                        addKey(pageSelected, pos, key)
                        recalculateBoundaries(pageSelected, size)
                        curSelection.value += 1

                        if (curSelection.value >= toSelect.count()) {
                            pageSelection.value += 1
                            keyboardState.modifierNumeric.value = true
                            curSelection.value = 0
                        }

                        if (pageSelection.value > 1) {
                            keyboardData.finishedConstruction.value = true;
                        }

                    }
                }
        ) {
            DrawKeyboard(keyboardData, keyboardState, theme)
        }
    }
}