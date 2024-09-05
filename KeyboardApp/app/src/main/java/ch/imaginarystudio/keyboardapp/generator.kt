package ch.imaginarystudio.keyboardapp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


fun defaultPositions(widthPixels: Float, aspectRatio: Float): List<Vec2> {

    val positions = mutableListOf<Vec2>()

    // values found empirically
    //
    // the rectangle is from
    //     x elem [0 to 1]
    //     y elem [0 to aspect]
    //
    // thumb positions
    //     left: (-0.45, 0.04)
    //     right: (1.45, 0.04)
    //
    // ring radii squared are
    //     1.32, 1, 0.72, 0.49, 0.3
    // corresponding number of segements
    //     2 (mid shared), 5 (mid shared), 6, 4, 3
    // total positions
    //     3 + 9 + 12 + 8 + 6 = 38
    // start - end angles for left in pi radians
    //     0.24, 0.295, 0.302, 0.25, 0.15
    //     0.19, 0.101, 0.0,   0.0,  0.0

    val tPosL = Vec2(-0.45, 0.04)
    val tPosR = Vec2(1.45, 0.04)
    val rs = mutableListOf<Float>(1.32f, 1f, 0.72f, 0.49f, 0.3f).map { x -> sqrt(x) }

    val nSeg1 = listOf<Int>(2, 5) // shared middle position
    val nSeg2 = listOf<Int>(6, 4, 3) // non shared

    val anglesStart = listOf(0.24, 0.295, 0.302, 0.25, 0.15)
    val anglesEnd = listOf(0.19, 0.101, 0.0, 0.0, 0.0)

    // calculatePosition based on circle, segmented
    val getPosOnCircle = { r: Float, angle1: Double, angle2: Double, i: Int, n: Int, center: Vec2 ->
        val t = i.toFloat() / (n-1)
        val angle = angle1 * t + angle2 * (1.0f - t)
        Vec2(r * sin(angle1 * PI) + center.x, r * cos(angle1 * PI) + center.y)
    }

    // including the shared
    nSeg1.forEachIndexed { index, n ->
        val ind = index
        for (i in 0 until n) {
            positions.add(getPosOnCircle(rs[ind], anglesStart[ind], anglesEnd[ind], i, n, tPosL))
        }

        for (i in n-1 downTo 1) {
            positions.add(getPosOnCircle(rs[ind], 1 - anglesStart[ind], 1 - anglesEnd[ind], i, n, tPosR))
        }
    }

    nSeg2.forEachIndexed { index, n ->
        val ind = index + nSeg1.count()
        for (i in 0 until n) {
            positions.add(getPosOnCircle(rs[ind], anglesStart[ind], anglesEnd[ind], i, n, tPosL))
        }

        for (i in n-1 downTo 1) {
            positions.add(getPosOnCircle(rs[ind], 1 - anglesStart[ind], 1 - anglesEnd[ind], i, n, tPosR))
        }
    }

    return positions.map { p -> Vec2(p.x * widthPixels, p.y * widthPixels * aspectRatio) }
}