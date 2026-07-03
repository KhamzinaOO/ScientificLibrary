package org.olgakhamzina.scientificlibrarythesis.server

import org.olgakhamzina.scientificlibrarythesis.shared.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.shared.WeightParams
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

/**
 * ParamRepository – доступ к таблице `scoring_parameters` для чтения и обновления параметров ранжирования.
 * Таблица `scoring_parameters` содержит записи: bm25parameter, lambda, alpha, beta, gamma.
 */
@Repository
class ParamRepository(private val jdbcTemplate: JdbcTemplate) {

    /**
     * Загружает текущие параметры ранжирования из таблицы `scoring_parameters`.
     * @return ScoringParams с текущими значениями
     */
    fun loadScoringParams(): ScoringParams {
        val sql = "SELECT name, value FROM scoring_parameters"
        val map = mutableMapOf<String, Double>()
        jdbcTemplate.query(sql) { rs ->
            map[rs.getString("name")] = rs.getDouble("value")
        }

        return ScoringParams(
            bm25Parameter       = map["bm25parameter"]       ?: 1.0,
            lambda              = map["lambda"]              ?: 0.1,
            alpha               = map["alpha"]               ?: 1.0,
            beta                = map["beta"]                ?: 1.0,
            gamma               = map["gamma"]               ?: 1.0,
            maxLogHIndex        = map["maxLogHIndex"]        ?: 1.0,
            maxLogCitations     = map["maxLogCitations"]     ?: 1.0,
            maxLogInfluential   = map["maxLogInfluential"]   ?: 1.0
        )
    }

    /**
     * Обновляет значения параметров ранжирования в таблице `scoring_parameters`.
     * @param params новый набор параметров ScoringParams, который нужно сохранить
     */
    fun updateScoringParams(params: WeightParams) {
        // Обновляем каждую запись. Предполагается, что записи с именами уже существуют в таблице.
        jdbcTemplate.update(
            "UPDATE scoring_parameters SET value = ? WHERE name = ?",
            params.bm25Parameter, "bm25parameter"
        )
        jdbcTemplate.update(
            "UPDATE scoring_parameters SET value = ? WHERE name = ?",
            params.lambda, "lambda"
        )
        jdbcTemplate.update(
            "UPDATE scoring_parameters SET value = ? WHERE name = ?",
            params.alpha, "alpha"
        )
        jdbcTemplate.update(
            "UPDATE scoring_parameters SET value = ? WHERE name = ?",
            params.beta, "beta"
        )
        jdbcTemplate.update(
            "UPDATE scoring_parameters SET value = ? WHERE name = ?",
            params.gamma, "gamma"
        )
    }
}