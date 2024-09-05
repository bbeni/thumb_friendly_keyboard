package ch.imaginarystudio.keyboardapp

import android.graphics.Paint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.Dp

class KeyboardData(
    val finishedConstruction: MutableState<Boolean>,
    val alphaPage: SnapshotStateList<KeyInfo>,
    val numericPage: SnapshotStateList<KeyInfo>,
)

class KeyboardState(
    val modifierShift: MutableState<Boolean>,
    val modifierNumeric: MutableState<Boolean>,
    val showSettings: MutableState<Boolean>,
)

class KeyboardTheme(
    val aspectRatio: Float = 17f/15,
    val keyPaint: Paint,
    val shrinkKeyDp: Dp,
)

data class Key(var code: String, var isControlChar: Boolean = false)

val keysPageAlpha = listOf<Key>(
    Key("q"), Key("w"), Key("e"), Key("r"), Key("t"), Key("y"), Key("u"), Key("i"), Key("o"), Key("p"),
    Key("a"), Key("s"), Key("d"), Key("f"), Key("g"), Key("h"), Key("j"), Key("k"), Key("l"),
    Key("z"), Key("x"), Key("c"), Key("v"), Key("b"), Key("n"), Key("m"),
    Key("."), Key("?"), Key(" "),
    Key("⇧", true),
    Key("↩", true),
    Key("⇐", true),
    Key("⁝", true),
    Key("?123", true),
)

val keysPageNumeric = listOf<Key>(
    Key("1"), Key("2"), Key("3"), Key("4"), Key("5"), Key("6"), Key("7"), Key("8"), Key("9"), Key("0"),
    Key("."), Key(":"), Key(","), Key(";"), Key("-"), Key("["), Key("]"), Key("("), Key(")"),
    Key("_"), Key("%"), Key("&"), Key("*"), Key("/"), Key("\\"), Key("+"),
    Key("<"), Key(">"), Key("^"),
    Key("@"),
    Key("↩", true),
    Key("⇐", true),
    Key("⁝", true),
    Key("?123", true),
)