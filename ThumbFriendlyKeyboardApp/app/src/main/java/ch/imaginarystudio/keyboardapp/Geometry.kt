package ch.imaginarystudio.keyboardapp

import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sqrt

object Settings{
    const val tolerance = 0.0000001
}


// Point is actually a 2Vec
data class Vec2(var x: Double, var y: Double) {
    constructor(x: Float, y: Float): this(x.toDouble(), y.toDouble())
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

class Line(private val a: Vec2, private val b: Vec2) {
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
