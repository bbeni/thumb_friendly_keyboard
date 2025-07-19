package ch.imaginarystudio.keyboardapp.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Yellow = Color(0xffffff00)
val Orange = Color(0xfff99e00)
val Dark = Color(0xff2d2d2d)


val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

data class KeyboardColorTheme(
    val keyBackground: Color,
    val keyForeground: Color,
    val background: Color,
)

val KeyboardColorThemes = arrayOf(
    KeyboardColorTheme(Color(0xFF04253A), Color(0xFFE1DDBF), Color(0xFF4C837A)), // https://www.slideteam.net/blog/9-beautiful-color-palettes-for-designing-powerful-powerpoint-slides
    KeyboardColorTheme(Color(0xFF231F20), Color(0xFFEEEEEE), Color(0xFFEC7404)), // Graphite Orange
    KeyboardColorTheme(Color(0xFFF0F8FF), Color(0xFF1E3A8A), Color(0xFF2563EB)), // Arctic Blue
    KeyboardColorTheme(Color(0xFF2D6A4F), Color(0xFF40916C), Color(0xFFB7E4C7)), // Forest Green
    KeyboardColorTheme(Color(0xFF5B2A86), Color(0xFFFFD166), Color(0xFFEF476F)), // Sunset Pop
    KeyboardColorTheme(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77)), // Navy Slate
    KeyboardColorTheme(Color(0xFFFBF8CC), Color(0xFFFFC8DD), Color(0xFFFFAFCC)), // Pastel Sorbet
    KeyboardColorTheme(Color(0xFF3E2723), Color(0xFFDAD7CD), Color(0xFFA98467))  // Cocoa Latte
)