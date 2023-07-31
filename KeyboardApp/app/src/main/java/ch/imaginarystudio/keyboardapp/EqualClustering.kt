package ch.imaginarystudio.keyboardapp

import ch.imaginarystudio.keyboardapp.itertools.Combinations.combinations
import kotlin.math.abs
import kotlin.math.sqrt

object Settings{
    const val tolerance = 0.00000000000001f
}

class Clustering {

    // Point is actually a 2Vec
    data class Point(val x: Float, val y: Float) {
        operator fun times(t: Float): Point = Point(t * x, t * y)
        operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)
        operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)

        fun norm(): Float = sqrt(x * x + y * y)
        operator fun times(other: Point) = x * other.x + y * other.y
    }

    class Line(val a: Point, val b: Point) {
        fun pointAtT(t: Float) = a * t + b
        fun intersectionPoint(line: Line): Point? {
            val c = line.b - b
            val det = a.x * line.a.y - a.y * line.a.x
            if (abs(det) < Settings.tolerance) {
                return null
            }
            val t = 1 / det * (line.a.y * c.x - line.a.x * c.y)
            return pointAtT(t)
        }
        fun isPointLeft(point: Point) : Boolean {
            val v0 = b - point
            val crossProduct2D = v0.x*a.y - v0.y*a.x
            return crossProduct2D > 0
        }
        fun intersectsSegment(segment: Segment): Boolean {
            return isPointLeft(segment.a) xor isPointLeft(segment.b)
        }

    }

    class Segment(val a: Point, val b: Point) {
        fun toLine(): Line = Line(b-a,a)
    }

    class Polygon(var corners : MutableList<Point> = mutableListOf()) {
        fun shiftCornersTowardsPoint(factor:Float, center:Point) {
            corners = corners.onEach { p ->
                center + (p - center) * factor
            }
        }
        fun addCorner(c : Point) {corners.add(c)}
    }

    private fun constructPerpendicularBisector(a: Point, b: Point): Line {
        val d = b - a
        // 90 deg rotation
        val c = Point(-d.y, d.x)
        return Line(c, a + d * 0.5f)
    }

    private fun cutPoints(polygon: Polygon, line:Line): Pair<MutableList<Point>, MutableList<Int>>? {
        val points = mutableListOf<Point>()
        val indices = mutableListOf<Int>()
        for (i in 0..polygon.corners.size) {
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
        assert((points.size == 2) or (points.size == 0))
        if (points.size == 0)
            return null
        return Pair(points, indices)
    }

    fun constructPolygonAroundPoint(
        v0:Point,
        vListSupport:List<Point>,
        maxNearestNeighboursConsidered:Int? = null
    ): Polygon {
        val perpendiculars : MutableList<Line> = vListSupport
            .asSequence()
            .filter { it != v0 }
            .sortedBy { (it-v0).norm() }
            .take(maxNearestNeighboursConsidered ?: vListSupport.size)
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
                val intersection : Point? = pa.intersectionPoint(pb)
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
                polygon.corners.subList(0, indices[0]+1).clear()
                polygon.corners.subList(indices[1]+1, polygon.corners.size).clear()
                polygon.corners.add(0, cps[0])
                polygon.corners.add(cps[1])
            }
        }
        return polygon
    }

}