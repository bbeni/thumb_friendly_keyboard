package ch.imaginarystudio.keyboardapp

import android.graphics.Paint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList

class KeyboardDataStructure(
    val finishedConstruction: MutableState<Boolean>,
    val keyInfos: SnapshotStateList<KeyInfo>,
    val modifierShift: MutableState<Boolean>,
    val keyPaint: Paint
)