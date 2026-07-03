package org.olgakhamzina.scientificlibrarythesis.server.unitTests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Тесты нормализации: приведение значений в диапазон [0;1].
 */
class NormalizationTest {

    private fun normalize(x: Double, min: Double, max: Double): Double =
        if (max==min) 0.0 else (x-min)/(max-min)

    @Test
    fun `normalize midpoint`() {
        val result = normalize(5.0, 0.0, 10.0)
        // (5-0)/(10-0) = 0.5
        assertEquals(0.5, result, 1e-6)
    }

    @Test
    fun `normalize equals bounds`() {
        val result = normalize(3.0, 3.0, 3.0)
        // при min==max возвращаем 0
        assertEquals(0.0, result, 1e-6)
    }
}