package org.olgakhamzina.scientificlibrarythesis.server.unitTests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.log2

/**
 * Тесты для метрик Precision@K, Average Precision, NDCG@K.
 * Используем искусственные списки для точного контроля.
 */
/**
 * Тесты для метрик Precision@K, Average Precision, NDCG@K.
 * Используем искусственные списки для точного контроля.
 */
class RankingMetricsTest {

    private val relevant = setOf("A", "B", "C")
    private val predicted = listOf("X", "A", "Y", "B", "Z", "C", "W")

    private fun precisionAtK(pred: List<String>, rel: Set<String>, k: Int): Double {
        val top = pred.take(k)
        val hits = top.count { it in rel }
        return if (k == 0) 0.0 else hits.toDouble() / k
    }

    private fun averagePrecision(pred: List<String>, rel: Set<String>): Double {
        var hits = 0
        var sum = 0.0
        pred.forEachIndexed { i, id ->
            if (id in rel) {
                hits++
                sum += hits.toDouble() / (i + 1)
            }
        }
        return if (rel.isEmpty()) 0.0 else sum / rel.size
    }

    private fun ndcgAtK(pred: List<String>, rel: Set<String>, k: Int): Double {
        val dcg = pred.take(k)
            .mapIndexed { i, id ->
                if (id in rel) 1.0 / log2(i + 2.0) else 0.0
            }
            .sum()
        val idcg = (0 until minOf(rel.size, k))
            .mapIndexed { i, _ ->
                1.0 / log2(i + 2.0)
            }
            .sum()
        return if (idcg == 0.0) 0.0 else dcg / idcg
    }

    @Test
    fun `precision at 3`() {
        val p3 = precisionAtK(predicted, relevant, 3)
        // В топ-3 есть только одна релевантная ("A"), поэтому 1/3
        assertEquals(1.0 / 3, p3, 1e-6)
    }

    @Test
    fun `average precision calculation`() {
        val ap = averagePrecision(predicted, relevant)
        // AP = (precision@2 + precision@4 + precision@6) / 3 = (1/2 + 2/4 + 3/6)/3 = (0.5+0.5+0.5)/3 = 0.5
        assertEquals(0.5, ap, 1e-6)
    }

    @Test
    fun `ndcgAt5 is between zero and one`() {
        val ndcg = ndcgAtK(predicted, relevant, 5)
        assertTrue(ndcg >= 0.0 && ndcg <= 1.0,
            "Ожидается, что 0.0 ≤ NDCG@5 ≤ 1.0, но было $ndcg")
    }
}