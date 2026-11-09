package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class ExampleUnitTest {

    @Test
    fun testMathematicalCalculations() {
        val dx = 3.0
        val dy = 4.0
        val dist = sqrt(dx * dx + dy * dy)
        assertEquals(5.0, dist, 0.0001)

        val fx = 1420.0
        val baseline = 0.10 // 10 cm
        val disparity = 32.0
        val depth = (fx * baseline) / disparity
        assertEquals(4.4375, depth, 0.001)
        assertTrue(depth > 0)
    }
}
