package ch.imaginarystudio.keyboardapp

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt


fun regularGrid1(aspectRatio: Float): List<Vec2> {
    val positions = mutableListOf<Vec2>()

    val getPos = { i: Int, n: Int, y: Float, leftOffset: Float, rightOffset: Float ->
        val dist = 1f - leftOffset - rightOffset
        Vec2(leftOffset + dist * i.toFloat()/(n-1), y)
    }

    val rowHeight = 1f / aspectRatio / 5

    for (i in 0..9) {
        positions.add(getPos(i, 10, 0.5f * rowHeight, 0.04f, 0.04f))
    }

    for (i in 0..8) {
        positions.add(getPos(i, 9, 1.5f * rowHeight, 0.10f, 0.10f))
    }

    for (i in 0..9) {
        positions.add(getPos(i, 10, 2.5f * rowHeight, 0.04f, 0.04f))
    }

    for (i in 0..8) {
        positions.add(getPos(i, 5, 3.5f* rowHeight, 0.14f, 0.14f))
    }

    return positions
}


// The values to generate this are empirically found
// To see how they have been generated see ipython notebook concentric_keys.ipynb in ideas_and_notes
fun concentricGrid1(aspectRatio: Float, border: Double = 0.05): List<Vec2> {

    val thumbPosition = Vec2(-0.57, 0.06)
    val radii = listOf(1.268, 1.15, 1.031, 0.9, 0.76)
    val numbers = listOf(1, 3, 5, 5, 4)

    val height = 1.0 / aspectRatio
    val upperLeft = Vec2(border, height - border)
    val lowerRight = Vec2(1.0 - border, border)

    return generateConcentric(radii, numbers, thumbPosition, true, upperLeft, lowerRight).map {
        // flip y axis for printing
        it.y = height - it.y
        // shrink it
        it
    }
}


fun concentric(pos: Vec2, angles: List<Double>, radius: Double) : List<Vec2> =
    angles.map {
        Vec2(cos(it) * radius + pos.x, sin(it) * radius + pos.y)
    }


// Functions to determine the angles of a circle intersected with rectangle, used to generate keyboard layout
// only works when the center of the circle is below and to the left of the topLeftCorner
// TODO: make it more general
fun circleRectIntersectionAngleLeft(radius: Double, center: Vec2, topLeftCorner: Vec2): Double {
    val delta = topLeftCorner - center
    val dxSquared = radius * radius - delta.y * delta.y
    val dySquared = radius * radius - delta.x * delta.x
    val angle1 = if (dxSquared > 0.0) {atan(delta.y / sqrt(dxSquared)) } else {PI}
    val angle2 = if (dySquared > 0.0) {atan(sqrt(dySquared) / delta.x) } else {PI}
    return min(angle1, angle2)
}

// COPYPASTE! TODO: refactor
// only works when the center of the circle is below and to the left of the topLeftCorner
fun circleRectIntersectionAngleRight(radius: Double, center: Vec2, lowerRightCorner: Vec2): Double {
    val delta = lowerRightCorner - center
    val dxSquared = radius * radius - delta.y * delta.y
    val dySquared = radius * radius - delta.x * delta.x
    val angle1 = if (dxSquared > 0.0) {atan(delta.y / sqrt(dxSquared)) } else {-PI}
    val angle2 = if (dySquared > 0.0) {atan(sqrt(dySquared) / delta.x) } else {-PI}
    return max(angle1, angle2)
}

fun linearSpace(a: Double, b: Double, n: Int): List<Double> {
    if (n == 1) return listOf(a)
    val step = (b - a) / (n - 1)
    return (0 until n).map { a + step * it }
}
fun generateConcentric(
    radii: List<Double>,
    numbers: List<Int>,
    position: Vec2 = Vec2(-0.55, 0.0),
    reflect: Boolean = false,
    upperLeft: Vec2 = Vec2(0.0, 1.0),
    lowerRight: Vec2 = Vec2(1.0, 0.0)
) : List<Vec2> {

    val height = upperLeft.y - lowerRight.y
    val mid = 0.5 * (lowerRight.x + upperLeft.x)

    if (reflect) {
        lowerRight.x = mid
    }

    val resultingPoints = mutableListOf<Vec2>()

    for ((r, n) in radii.zip(numbers)) {
        val a1 = circleRectIntersectionAngleLeft(r, position, upperLeft)
        val a2 = circleRectIntersectionAngleRight(r, position, lowerRight)
        val angles = linearSpace(a1, a2, n)
        val points = concentric(position, angles, r)

        resultingPoints.addAll(points)
        if (reflect) {
            for (p in points) {
                if (p.x < mid - 0.001) {
                    resultingPoints.add(Vec2(mid + (mid - p.x), p.y))
                }
            }
        }
    }

    return resultingPoints
}
