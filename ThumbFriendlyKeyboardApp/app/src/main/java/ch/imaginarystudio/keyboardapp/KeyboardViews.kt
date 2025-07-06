package ch.imaginarystudio.keyboardapp

import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class KeyInfo(
    var position: Vec2,
    var boundary: Polygon = Polygon(),
    var key: Key,
) {

    // Cut the part away from the center using the center line
    // from this to the other keyInfo

    // we can do an algorithm like this, as we know we will only get convex polygons
    // if we cut away slices from any convex starting polygon (here we start from a
    // rectangle)
    fun cutAway(other: KeyInfo) {
        val centerLine = constructPerpendicularBisector(position, other.position)

        // store both polygons separated by center line
        // if we have a cut point the two cut points get added to both, so we have two adjacent new polygons
        // if we don't hit, we will always add to the first one (pointsLeft)
        // TODO: do not use dynamic list as this might be very inefficient, we could use a static array?
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


// this function is used to detect key press events by just finding the closest
// postition of a key
// euclidean metric can be used as we the key geometry is constructed that way!
fun closestKeyIndex(keyInfos: SnapshotStateList<KeyInfo>, position: Vec2): Int {
    var closestIndex = -1
    var closestDist = Float.MAX_VALUE
    for (i in 0 until keyInfos.count()) {
        val dist = (position - keyInfos[i].position).norm()
        if (dist < closestDist) {
            closestDist = dist.toFloat()
            closestIndex = i
        }
    }
    return closestIndex
}

fun closestKey(keyInfos: SnapshotStateList<KeyInfo>, position: Vec2): Key {
    return keyInfos[closestKeyIndex(keyInfos, position)].key
}

fun commitIfCharOrBackspace(keyboardState: KeyboardState, key: Key, ic: InputConnection): Boolean {
    if (!key.isControlChar) {
        var str = key.code
        if (keyboardState.modifierShift.value) {
            str = str.uppercase()
            keyboardState.modifierShift.value = false
        }
        ic.commitText(str, str.length)
        return true
    }

    if (key.code == "⇐") {
        if (TextUtils.isEmpty(ic.getSelectedText(0))) ic.deleteSurroundingText(1, 0)
        else ic.commitText("", 0)
        return true
    }

    return false
}

fun handleKey(keyboardState: KeyboardState, key: Key, ic: InputConnection, ei: EditorInfo) {

    if (commitIfCharOrBackspace(keyboardState, key, ic,)) return

    when (key.code) {
        "↩" -> {
            val requested_action = ei.imeOptions and EditorInfo.IME_MASK_ACTION
            val flags_no_action = EditorInfo.IME_FLAG_NO_ENTER_ACTION or
                    EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or
                    EditorInfo.TYPE_TEXT_FLAG_IME_MULTI_LINE

            val should_send_newline = ((ei.imeOptions and flags_no_action) != 0 ) ||
                    requested_action == EditorInfo.IME_ACTION_NONE

            if (should_send_newline) {
                ic.commitText("\n", 1)
            } else {
                ic.performEditorAction(requested_action)
            }
        }

        "⇧" -> keyboardState.modifierShift.value =
            !keyboardState.modifierShift.value

        "⁝⁝⁝⁝" -> {
            keyboardState.mode.value = Mode.MENU
        }

        "?123" -> {
            keyboardState.modifierNumeric.value =
                !keyboardState.modifierNumeric.value
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
    val scope = rememberCoroutineScope()

    Canvas(
        modifier = Modifier
            .aspectRatio(theme.aspectRatio)
            .padding(0.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // select page and find pressed key
                        val tapPos = offsetToPosition(offset, size.width)
                        var page = keyboardData.alphaPage
                        if (state.modifierNumeric.value) page = keyboardData.numericPage
                        val key = closestKey(page, tapPos)

                        val ic = (ctx as KeyboardIMEService).currentInputConnection
                        val ei = (ctx as KeyboardIMEService).currentInputEditorInfo

                        handleKey(state, key, ic, ei)
                    },
                    onPress = { offset ->
                        // select page and find pressed key
                        val tapPos = offsetToPosition(offset, size.width)
                        var page = keyboardData.alphaPage
                        if (state.modifierNumeric.value) { page = keyboardData.numericPage }
                        val key = closestKey(page, tapPos)

                        val ic = (ctx as KeyboardIMEService).currentInputConnection

                        val repeatJob = scope.launch {
                            delay(450)
                            while (true) {
                                delay(40)
                                commitIfCharOrBackspace(state, key, ic)
                            }
                        }

                        val releasedNormally = try {
                            awaitRelease()    // returns when finger lifts
                            true
                        } catch (_: GestureCancellationException) {
                            false
                        }

                        repeatJob.cancel()

                    }
                )
            }
    ) {
        drawKeyboard(keyboardData, state, theme)
    }
}


@Composable
fun KeyboardMoveEditorView(keyboardData: KeyboardData, state: KeyboardState, theme: KeyboardTheme) {

    val ctx = LocalContext.current

    var draggedKeyIndex = -1
    var hoverIndex = -1

    Column {
        Row(
            modifier = Modifier
                .background(Color.Yellow)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = {
                state.mode.value = Mode.KEYBOARD
            }) {
                Text(text = "Test Keyboard")
            }
        }
        Canvas(
            modifier = Modifier
                .aspectRatio(theme.aspectRatio)
                .padding(0.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val pos = offsetToPosition(offset, size.width)
                            draggedKeyIndex = closestKeyIndex(keyboardData.alphaPage, pos)
                            hoverIndex = draggedKeyIndex
                        },
                        onDragEnd = {

                            // handle swap
                            if (hoverIndex != -1 && draggedKeyIndex != -1) {
                                val a = keyboardData.alphaPage[draggedKeyIndex]
                                val b = keyboardData.alphaPage[hoverIndex]
                                a.key = b.key.also { b.key = a.key }
                            }

                            draggedKeyIndex = -1
                            hoverIndex = -1

                        },
                        onDragCancel = {
                            draggedKeyIndex = -1
                            hoverIndex = -1
                        },
                    ) { change, offset ->
                        if (draggedKeyIndex != -1) {
                            val pos = offsetToPosition(offset, size.width)
                            val nextHoverIndex = closestKeyIndex(keyboardData.alphaPage, pos)

                            // we change hover position
                            if (draggedKeyIndex != nextHoverIndex) {
                                hoverIndex = nextHoverIndex
                            }
                        }
                    }
                }
        ) {
            drawKeyboard(keyboardData, state, theme)
        }
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
                    color = Color.Yellow
                )
        ) {
            if (pageSelection.value == 0) {
                Text(text = "Constructing page 1/2. Next Key drop: \"${keysPageAlpha[curSelection.value].code}\" (${curSelection.value + 1}/${keysPageAlpha.count()})")
            } else {
                Text(text = "Constructing page 2/2. Next Key drop: \"${keysPageNumeric[curSelection.value].code}\" (${curSelection.value + 1}/${keysPageNumeric.count()})")
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