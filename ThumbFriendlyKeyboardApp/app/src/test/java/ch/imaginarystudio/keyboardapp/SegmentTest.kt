package ch.imaginarystudio.keyboardapp

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SegmentTest {

    @Test
    fun intersection() {
        var s1 = Segment(Vec2(-1.0, 0.0), Vec2(1.0, 0.0))
        var s2 = Segment(Vec2(1.0, 1.0), Vec2(2.0, 1.0))
        var s3 = Segment(Vec2(-1.0, -1.0), Vec2(2.0, 2.0))
        assertNull(s1.intersection(s2))
        assertNull(s2.intersection(s1))

        assertNotNull(s1.intersection(s3))
        assertNotNull(s3.intersection(s1))

        assertNotNull(s3.intersection(s2))
        assertNotNull(s2.intersection(s3))

    }
}