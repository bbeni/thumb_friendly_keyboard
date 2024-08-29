package ch.imaginarystudio.keyboardapp

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect

import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.view.setPadding

class ClusterTestView(context: Context?) : LinearLayout(context) {
    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.BOTTOM
        setPadding(2)
    }
    init {
        var topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.RIGHT
            setPadding(15)
        }
        var btn1 = Button(context).apply {
            text = "Hello"
        }
        var btn2 = Button(context).apply {
            text = "World"
        }

        topBar.addView(btn1)
        topBar.addView(btn2)
        addView(topBar)
    }

    init {
        var kbv = KeyboardViewTest(context).apply {
            layoutParams = LayoutParams(-1, 800)
        }
        addView(kbv)
    }
}

@Composable
fun KeyboardView3() {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Gray),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(color = Color.Black, text = "This should resemble a keyboard")

        for (p in "ABC".toList()){
            Button(modifier = Modifier.width(250.dp), onClick = { }) {
                Text(text = "A")
            }
        }
    }
}


class KeyboardViewTest(context: Context?) : View(context) {

    init {
        makeHexagonal()
    }


    var points = hexagonal
    var pointsSupport = hexagonalSupport
    var polygons = mutableListOf<Polygon>()

    init {
        for (point in points) {
            var p = constructPolygonAroundPoint(point, pointsSupport, 3)
            p.scale(900.0)
            Log.d("TFKeyboard", p.toString())
            polygons.add(p)
        }
    }

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.Yellow.toArgb()
        style = Paint.Style.STROKE
    }

    private val bgPaint = Paint().apply {
        if (context != null) {
            color = context.resources.getColor(R.color.purple_200)
        } else {
            color = Color.Black.toArgb()
        }
        style = Paint.Style.FILL
        setAlpha(0.98f)
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.Black.toArgb()
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
    }

    private val pointPaint = Paint().apply {
        color = Color.Black.toArgb()
        style = Paint.Style.FILL_AND_STROKE
    }

    private val pointPaint2 = Paint().apply {
        color = Color.Green.toArgb()
        style = Paint.Style.FILL_AND_STROKE
    }

    val path = Path()

    fun drawPolygon(canvas: Canvas, polygon: Polygon, paint: Paint) {
        path.moveTo(polygon.corners.last().x.toFloat(), polygon.corners.last().y.toFloat())
        for (c in polygon.corners){
            path.lineTo(c.x.toFloat(), c.y.toFloat())
        }
        canvas.drawPath(path, paint)
    }

    fun plotPoints(canvas: Canvas, points: MutableList<Vec2>, paint: Paint){
        canvas.apply {
            for (p in points) {
                drawCircle(p.x.toFloat()*900f, p.y.toFloat()*900f, 5f, paint)
            }
        }

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            drawRect(Rect(0,0,canvas.width,canvas.height), bgPaint)

            for (p in polygons) {
                drawPolygon(this, p, keyPaint)
            }

            plotPoints(canvas, pointsSupport, pointPaint2)
            plotPoints(canvas, points, pointPaint)
        }
    }
}