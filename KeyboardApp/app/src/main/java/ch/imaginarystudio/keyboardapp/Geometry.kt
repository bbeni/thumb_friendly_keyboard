package ch.imaginarystudio.keyboardapp

import android.util.Log
import androidx.compose.ui.geometry.Offset
import ch.imaginarystudio.keyboardapp.itertools.Combinations.combinations
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sqrt

object Settings{
    const val tolerance = 0.0000001
}


// Point is actually a 2Vec
data class Vec2(var x: Double, var y: Double) {
    constructor(x: Float, y: Float): this(x.toDouble(), y.toDouble())
    constructor(offset: Offset): this(offset.x, offset.y)
    operator fun times(t: Double): Vec2 = Vec2(t * x, t * y)
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    fun norm(): Double = sqrt(x * x + y * y)
    operator fun times(other: Vec2) = x * other.x + y * other.y
    fun cross(other: Vec2)  : Double {
        return x*other.y - y * other.x
    }

    fun normalized(): Vec2 {
        val n = norm()
        if (n != 0.0) return Vec2(x/n, y/n)
        return Vec2(1.0, 0.0)
    }
}

class Line(val a: Vec2, val b: Vec2) {
    fun pointAtT(t: Double) = a * t + b
    fun intersectionPoint(line: Line): Vec2? {
        val c = line.b - b
        val det = a.x * line.a.y - a.y * line.a.x
        if (abs(det) < Settings.tolerance) {
            return null
        }
        val t = 1 / det * (line.a.y * c.x - line.a.x * c.y)
        return pointAtT(t)
    }
    fun isPointLeft(point: Vec2) : Boolean {
        val v0 = b - point
        val crossProduct2D = v0.x*a.y - v0.y*a.x
        return crossProduct2D > 0
    }
    fun intersectsSegment(segment: Segment): Boolean {
        return isPointLeft(segment.a) xor isPointLeft(segment.b)
    }

}

class Segment(val a: Vec2, val b: Vec2) {
    fun toLine(): Line = Line(b-a,a)
    fun midPoint(): Vec2 {
        return (a - b) * 0.5
    }
    fun intersection(other: Segment): Vec2? {
        val ab = b-a
        val cd = other.b - other.a
        val abCrossCd = ab.cross(cd)
        if (abCrossCd.absoluteValue < Settings.tolerance) {
            return null
        }
        val ac = other.a - a
        val t1 = ac.cross(cd) / abCrossCd
        val t2 = ac.cross(ab) / abCrossCd
        if (t1 < 0 || t1 > 1 || t2 < 0 || t2 > 1 ) {
            return null
        }
        return a + ab * t1
    }
}

data class Polygon(var corners : MutableList<Vec2> = mutableListOf()) {

    fun addCorner(c : Vec2) {corners.add(c)}
    fun scale(fl: Double) {
        corners = corners.map { c -> c * fl }.toMutableList()
    }
    fun shrunkCorners(percentage: Float, shrinkCenter: Vec2) : List<Vec2> {
        return corners.map { p -> p - (p - shrinkCenter) * (percentage.toDouble()) }
    }

    fun contains(point: Vec2): Boolean {

        // pathological case of empty polygon
        // should maybe check that we have 0 count and never 1 or 2?
        if (corners.count() < 3) return false

        // TODO implement ray, because this is stupid and unreliable
        var ray = Segment(point, Vec2(9000.0, 8800.0))
        var lineIntersectionCount = 0
        for (i in 0 until corners.count()) {
            val s = Segment(corners[i], corners[(i+1)%corners.count()])
            if (s.intersection(ray) != null) {
                lineIntersectionCount++
            }
        }

        // odd count means we are inside
        return (lineIntersectionCount % 2) == 1
    }
}

fun constructPerpendicularBisector(a: Vec2, b: Vec2): Line {
    val d = b - a
    // 90 deg rotation
    val c = Vec2(-d.y, d.x)
    return Line(c, a + d * 0.5)
}

@OptIn(ExperimentalStdlibApi::class)
private fun cutPoints(polygon: Polygon, line:Line): Pair<MutableList<Vec2>, MutableList<Int>>? {
    val points = mutableListOf<Vec2>()
    val indices = mutableListOf<Int>()
    for (i in 0..< polygon.corners.size) {
        val c1 = polygon.corners[i]
        val c2 = polygon.corners[(i+1) % polygon.corners.size]
        val edge = Segment(c2, c1)
        if (line.intersectsSegment(edge)) {
            val ip = line.intersectionPoint(edge.toLine())
            if (ip == null) return null
            points.add(ip)
            indices.add(i)
        }

    }
    Log.d("TFKeyboard", points.toString())
    assert((points.size == 2) or (points.size == 0))
    if (points.size == 0)
        return null
    return Pair(points, indices)
}

fun constructPolygonAroundPoint(
    v0:Vec2,
    vListSupport:List<Vec2>,
    maxNearestNeighboursConsidered:Int? = null
): Polygon {
    val perpendiculars : MutableList<Line> = vListSupport
        .asSequence()
        .filter { it != v0 }
        .sortedBy { (it-v0).norm() }
        .take(maxNearestNeighboursConsidered ?: vListSupport.size)
        .sortedBy { Math.atan(((it.x-v0.x)/(it.y-v0.y)).toDouble())}
        .map { constructPerpendicularBisector(v0, it) }
        .toMutableList()

    // construct triangle
    var polygon : Polygon? = null
    outer@ for (c in perpendiculars.combinations(3))
    {
        val proposedPolygon = Polygon()
        val threePerpendiculars = c.toMutableList()

        threePerpendiculars.add(threePerpendiculars[0])
        for (pair in threePerpendiculars.zipWithNext()) {
            val (pa, pb) = pair
            val intersection : Vec2? = pa.intersectionPoint(pb)
            if (intersection == null) continue@outer
            proposedPolygon.addCorner(intersection)
        }
        val ca = proposedPolygon.corners[0]
        val cb = proposedPolygon.corners[1]
        val cc = proposedPolygon.corners[2]
        if (!(Segment(ca, cb).toLine().isPointLeft(v0)) or
            !(Segment(cb, cc).toLine().isPointLeft(v0)) or
            !(Segment(cc, ca).toLine().isPointLeft(v0))) {
            continue@outer
        }
        perpendiculars.remove(threePerpendiculars[0])
        perpendiculars.remove(threePerpendiculars[0])
        perpendiculars.remove(threePerpendiculars[0])
        polygon = proposedPolygon
        break
    }

    if (polygon == null) throw RuntimeException("Didn't find triangle.. ")


    for (perpendicular in perpendiculars) {
        val x = cutPoints(polygon, perpendicular) ?: continue
        val (cps, indices) = x
        val line = Segment(cps[0], cps[1]).toLine()

        // check which part of the polygon needs to be cut
        // then add new corners
        if (!line.isPointLeft(v0)) {
            polygon.corners.subList(indices[0]+1, indices[1]+1).clear()
            polygon.corners.add(indices[0]+1,cps[0])
            polygon.corners.add(indices[0]+2,cps[1])
        } else {
            polygon.corners.subList(indices[1] + 1, polygon.corners.size).clear()
            polygon.corners.subList(0, indices[0]+1).clear()
            polygon.corners.add(0, cps[0])
            polygon.corners.add(cps[1])
        }
    }
    return polygon
}