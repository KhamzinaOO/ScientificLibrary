package org.olgakhamzina.scientificlibrarythesis.server.unitTests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты для модуля BM25.
 * Ожидается, что документ с большим числом вхождений запроса получит более высокий score.
 */
class BM25Test {

    // Простая реализация BM25 для теста
    private fun bm25(tf: Int, docLen: Int, avgDocLen: Double = 100.0, k1: Double = 1.2, b: Double = 0.75): Double {
        val idf = 1.0  // игнорируем IDF, проверяем только TF-фактор
        return idf * (tf * (k1 + 1)) /
                (tf + k1 * (1 - b + b * docLen / avgDocLen))
    }

    @Test
    fun `bm25 increases with term frequency`() {
        val lowTfScore = bm25(tf = 1, docLen = 100)
        val highTfScore = bm25(tf = 5, docLen = 100)
        // Ожидается: при tf=5 оценка выше, чем при tf=1
        assertTrue(highTfScore > lowTfScore, "Ожидается bm25(5) > bm25(1)")
    }

    @Test
    fun `bm25 decreases with longer document`() {
        val shortDocScore = bm25(tf = 3, docLen = 50)
        val longDocScore  = bm25(tf = 3, docLen = 200)
        // Ожидается: при одинаковом tf, но большей длине документа, score ниже
        assertTrue(shortDocScore > longDocScore, "Ожидается bm25(docLen=50) > bm25(docLen=200)")
    }
}
