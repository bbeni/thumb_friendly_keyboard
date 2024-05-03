package ch.imaginarystudio.keyboardapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Fill
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

@Composable
fun KeyboardView() {
    val points = listOf(
        Offset(200f, 100f),
        Offset(300f, 100f),
        Offset(360f, 160f),
        Offset(300f, 220f),
        Offset(200f, 220f),
        Offset(140f, 160f),
        Offset(200f, 100f),
        )
    Canvas(
        modifier = Modifier
            .size(300.dp)
            .padding(2.dp)
    ) {
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color.Gray,Color.DarkGray)),
            cornerRadius = CornerRadius(4f, 4f),
            style = Fill
        )

        drawPoints(
            points = points,
            PointMode.Polygon,
            brush = Brush.radialGradient(listOf(Color.Black, Color.Black)),
            strokeWidth = 10f,
        )
    }
}
