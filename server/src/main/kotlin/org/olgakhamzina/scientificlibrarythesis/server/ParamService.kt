package org.olgakhamzina.scientificlibrarythesis.server

import org.olgakhamzina.scientificlibrarythesis.shared.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.shared.WeightParams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * ParamService – сервис для получения и обновления параметров расчета рейтинга.
 */
@Service
class ParamService(private val paramRepository: ParamRepository) {

    /**
     * Возвращает текущие параметры ранжирования (λ, α, β, γ, bm25Parameter).
     */
    fun getCurrentParams(): ScoringParams {
        return paramRepository.loadScoringParams()
    }

    /**
     * Обновляет параметры ранжирования на новые значения.
     * Аннотация @Transactional гарантирует, что все обновления произойдут атомарно.
     * @param newParams новый набор параметров (в JSON ожидаются поля bm25parameter, lambda, alpha, beta, gamma)
     */
    @Transactional
    fun updateParams(newParams: WeightParams) {
        paramRepository.updateScoringParams(newParams)
    }
}