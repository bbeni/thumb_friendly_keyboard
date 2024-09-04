package ch.imaginarystudio.keyboardapp

import android.graphics.Paint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList

class KeyboardData(
    val finishedConstruction: MutableState<Boolean>,
    val alphaPage: SnapshotStateList<KeyInfo>,
    val numericPage: SnapshotStateList<KeyInfo>,
)

class KeyboardState(
    val modifierShift: MutableState<Boolean>,
    val modifierNumeric: MutableState<Boolean>,
    val showSettings: MutableState<Boolean>,
    val keyPaint: Paint
)