package ch.imaginarystudio.keyboardapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolygonTest {
    @Test
    fun contains() {
        var p1 = Polygon(mutableListOf<Vec2>(
            Vec2(0.0,0.0),
            Vec2(0.0,100.0),
            Vec2(100.0,100.0),
            Vec2(100.0,0.0),
        ))

        assertTrue(p1.contains(Vec2(1.0, 1.0)))
        assertTrue(p1.contains(Vec2(99.0, 99.0)))
        assertFalse(p1.contains(Vec2(-1.0, 1.0)))
        assertFalse(p1.contains(Vec2(1.0, -1.0)))
        assertFalse(p1.contains(Vec2(-1.0, -1.0)))
        assertFalse(p1.contains(Vec2(101.0, 101.0)))
        assertFalse(p1.contains(Vec2(-10000.0, -10000.0)))

    }
}