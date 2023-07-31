package ch.imaginarystudio.keyboardapp

import ch.imaginarystudio.keyboardapp.itertools.Combinations
import ch.imaginarystudio.keyboardapp.itertools.Combinations.combinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.yield
import kotlin.math.abs
import kotlin.math.sqrt

object Settings{
    val tolerance = 0.00000000000001f
}

class Clustering() {

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
        fun intersection_point(line: Line): Point? {
            var c = line.b - b
            var det = a.x * line.a.y - a.y * line.a.x
            if (abs(det) < Settings.tolerance) {
                return null
            }
            var t = 1 / det * (line.a.y * c.x - line.a.x * c.y)
            return pointAtT(t)
        }
        fun isPointLeft(point: Point) : Boolean {
            var v0 = b - point
            var crossp = v0.x*a.y - v0.y*a.x
            return crossp > 0
        }
        fun intersectsSegment(segment: Segment): Boolean {
            return isPointLeft(segment.a) xor isPointLeft(segment.b)
        }

    }

    class Segment(val a: Point, val b: Point) {
        fun toLine(): Line = Line(b-a,a)
    }

    class Polygon(var corners : MutableList<Point> = mutableListOf<Point>()) {
        fun shifCornersTowardsPoint(factor:Float, center:Point) {
            corners = corners.apply {
                forEach({
                        p -> center + (p - center) * factor
                })
            }
        }
        fun addCorner(c : Point) {corners.add(c)}
    }

    fun constructPerpendicularBisector(a: Point, b: Point): Line {
        var d = b - a
        // 90 deg rotation
        var c = Point(-d.y, d.x)
        return Line(c, a + d * 0.5f)
    }

    fun cutPoints(polygon: Polygon, line:Line): Pair<MutableList<Point>, MutableList<Int>>? {
        var points = mutableListOf<Point>()
        var indices = mutableListOf<Int>()
        for (i in 0..polygon.corners.size) {
            var c1 = polygon.corners[i]
            var c2 = polygon.corners[(i+1) % polygon.corners.size]
            var edge = Segment(c2, c1)
            if (line.intersectsSegment(edge)) {
                var ip = line.intersection_point(edge.toLine())
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

    fun constructPolygonAroundPoint(v0:Point, v_support:List<Point>, max_nn:Int? = null) {
        var perpendiculars : MutableList<Line> = v_support
            .filter { it != v0 }
            .sortedBy { (it-v0).norm() }
            .take(max_nn ?: v_support.size)
            .map { constructPerpendicularBisector(v0, it) }
            .toMutableList()

        // construct triangle

        var polygon :Polygon? = null
        outer@ for (c in perpendiculars.combinations(3))
        {
            var proposedPolygon = Polygon()
            val threepairs = c.toMutableList()
            threepairs.add(threepairs[0])
            for (pair in threepairs.zipWithNext()) {
                var (pa, pb) = pair
                var intersection : Point? = pa.intersection_point(pb)
                if (intersection == null) continue@outer
                proposedPolygon.addCorner(intersection)
            }
            var ca = proposedPolygon.corners[0]
            var cb = proposedPolygon.corners[1]
            var cc = proposedPolygon.corners[2]
            if (!(Segment(ca, cb).toLine().isPointLeft(v0)) or
                !(Segment(cb, cc).toLine().isPointLeft(v0)) or
                !(Segment(cc, ca).toLine().isPointLeft(v0))) {
                continue@outer
            }
            perpendiculars.remove(threepairs[0])
            perpendiculars.remove(threepairs[0])
            perpendiculars.remove(threepairs[0])
            polygon = proposedPolygon
            break
        }

        if (polygon == null) throw RuntimeException("Didn't find triangle.. ")


        for (perp in perpendiculars) {
            var x = cutPoints(polygon, perp)
            if (x == null) continue
            var (cps, indices) = x;
            var line = Segment(cps[0], cps[1]).toLine()
            if (!line.isPointLeft(v0)) {
                TODO()
            } else {
                TODO()
            }
        }



    }

}