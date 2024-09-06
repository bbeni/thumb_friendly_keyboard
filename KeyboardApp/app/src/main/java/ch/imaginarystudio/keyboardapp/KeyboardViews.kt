package ch.imaginarystudio.keyboardapp

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

        val polyA = Polygon(pointsRight)
        val polyB = Polygon(pointsLeft)

        if (polyA.contains(position)) {
            boundary = polyA
            return
        }

        if (polyB.contains(position)) {
            boundary = polyB
            return
        }

        assert(false) // we should never get here
    }
}

fun recalculateBoundaries(keyInfos: SnapshotStateList<KeyInfo>, aspect: Float) {
    if (keyInfos.isEmpty()) return

    val height = 1 / aspect

    val canvasBoundary = Polygon(mutableListOf(
        Vec2(0f, 0f),
        Vec2(0f, height),
        Vec2(1f, height),
        Vec2(1f, 0f),
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


fun closestKey(keyInfos: SnapshotStateList<KeyInfo>, position: Vec2): Key {
    var closestIndex = -1
    var closestDist = Float.MAX_VALUE
    for (i in 0 until keyInfos.count()) {
        val dist = (position - keyInfos[i].position).norm()
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

// map from offset to normalized coordinates
fun offsetToPosition(offset: Offset, width: Int): Vec2 {
    return Vec2( offset.x / width, offset.y / width )
}
fun positionToOffset(position: Vec2, width: Float): Offset {
    return Offset((position.x * width).toFloat(), (position.y * width).toFloat())
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
                    val tapPos = offsetToPosition(offset, size.width)

                    // select page and find pressed key
                    var page = keyboardData.alphaPage
                    if (state.modifierNumeric.value) {
                        page = keyboardData.numericPage
                    }

                    val key = closestKey(page, tapPos)
                    val ic = (ctx as KeyboardIMEService).currentInputConnection

                    handleKey(state, key, ic)
                }
            }
    ) {
        drawKeyboard(keyboardData, state, theme)
    }
}

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

                        val tapPos = offsetToPosition(offset, size.width)

                        var toSelect = keysPageAlpha
                        var pageSelected = keyboardData.alphaPage

                        if (pageSelection.value == 1) {
                            toSelect = keysPageNumeric
                            pageSelected = keyboardData.numericPage
                        }

                        val key = toSelect[curSelection.value]

                        addKey(pageSelected, tapPos, key)
                        recalculateBoundaries(pageSelected, theme.aspectRatio)
                        curSelection.value += 1

                        if (curSelection.value >= toSelect.count()) {
                            pageSelection.value += 1
                            keyboardState.modifierNumeric.value = true
                            curSelection.value = 0
                        }

                        if (pageSelection.value > 1) {
                            keyboardState.modifierNumeric.value = false
                            keyboardData.finishedConstruction.value = true
                        }

                    }
                }
        ) {
            drawKeyboard(keyboardData, keyboardState, theme)
        }
    }
}