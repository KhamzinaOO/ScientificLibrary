package org.olgakhamzina.scientificlibrarythesis.server.integrationTests

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.olgakhamzina.scientificlibrarythesis.server.ParamRepository
import org.olgakhamzina.scientificlibrarythesis.server.PublicationRepository
import org.olgakhamzina.scientificlibrarythesis.server.SearchService
import org.olgakhamzina.scientificlibrarythesis.shared.SearchFilters
import org.olgakhamzina.scientificlibrarythesis.shared.WeightParams
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.AssertionErrors.assertEquals
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Интеграционный тест, работающий с реальной тестовой БД.
 * Схема уже заполнена – мы только читаем данные.
 */
@SpringBootTest
@ActiveProfiles("test")          // профиль уже настроен в архиве
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NormalizationRealDbIT @Autowired constructor(
    private val searchService: SearchService,
    private val paramRepository: ParamRepository
) {

    /**
     * 1. Нормализованные метрики не превышают 1.
     */
    @Test
    fun `normalized values are in 0-1`() {
        val params = paramRepository.loadScoringParams()
        val pubs   = searchService.searchPublications(SearchFilters(query = ""), 1, 100)

        pubs.forEach { p ->
            val logH = if (p.avgHIndex > 0) ln(p.avgHIndex + 1.0) else 0.0
            val logC = if (p.citationCount > 0) ln(p.citationCount + 1.0) else 0.0
            val logI = if (p.influentialCitationCount > 0) ln(p.influentialCitationCount + 1.0) else 0.0

            val normH = logH / params.maxLogHIndex
            val normC = logC / params.maxLogCitations
            val normI = logI / params.maxLogInfluential

            assertThat(normH).isBetween(0.0, 1.0001)
            assertThat(normC).isBetween(0.0, 1.0001)
            assertThat(normI).isBetween(0.0, 1.0001)
            assertThat(p.score).isGreaterThanOrEqualTo(0.0)
        }
    }

    /**
     * 2. Увеличиваем вес α и убеждаемся, что
     *    публикации с высоким h-индексом поднимаются в выдаче.
     */
    @Test
    fun `bigger alpha promotes high h-index papers`() {
        // базовые параметры
        var filters = SearchFilters(query = "neural network")
        val original = searchService.searchPublications(filters, 1, 50)

        // временно увеличиваем α (остальное оставляем как есть)
        val a = paramRepository.loadScoringParams()
        val p = WeightParams(
            bm25Parameter = a.bm25Parameter,
            lambda = a.lambda,
            alpha = a.alpha,
            beta = a.beta,
            gamma = a.gamma
        )
        paramRepository.updateScoringParams(
            p.copy(alpha = p.alpha * 5)      // ×5 к h-индексу
        )

        val boosted = searchService.searchPublications(filters, 1, 50)

        // проверяем: первая десятка стала «h-индекснее»
        val avgHOriginal = original.take(10).map { it.avgHIndex }.average()
        val avgHBoosted  = boosted.take(10).map { it.avgHIndex }.average()

        assertThat(avgHBoosted).isGreaterThan(avgHOriginal)

        // возвращаем параметры на место
        paramRepository.updateScoringParams(p)
    }

    @Test
    fun `beta weight boosts highly cited papers`() {
        val a = paramRepository.loadScoringParams()
        val params = WeightParams(
            bm25Parameter = a.bm25Parameter,
            lambda = a.lambda,
            alpha = a.alpha,
            beta = a.beta,
            gamma = a.gamma
        )

        val base     = searchService.searchPublications(SearchFilters(query="neural network"), 1, 30)
        val avgCBase = base.take(10).map { it.citationCount }.average()

        // увеличиваем β в 10 раз
        paramRepository.updateScoringParams(params.copy(beta = params.beta * 10))
        val boosted     = searchService.searchPublications(SearchFilters(query="neural network"), 1, 30)
        val avgCBoosted = boosted.take(10).map { it.citationCount }.average()

        assertThat(avgCBoosted).isGreaterThan(avgCBase)   // должно стать «цитируемее»

        paramRepository.updateScoringParams(params)       // откат
    }

    @Test
    fun `gamma increases share of influential citations in top`() {
        val a = paramRepository.loadScoringParams()
        val p0 = WeightParams(
            bm25Parameter = a.bm25Parameter,
            lambda = a.lambda,
            alpha = a.alpha,
            beta = a.beta,
            gamma = a.gamma
        )
        val baseline  = searchService.searchPublications(SearchFilters(query="neural network"), 1, 30)
        val inflBase  = baseline.take(10).map { it.influentialCitationCount }.average()

        paramRepository.updateScoringParams(p0.copy(gamma = p0.gamma * 8))
        val boosted   = searchService.searchPublications(SearchFilters(query="neural network"), 1, 30)
        val inflBoost = boosted.take(10).map { it.influentialCitationCount }.average()

        assertThat(inflBoost).isGreaterThan(inflBase)

        paramRepository.updateScoringParams(p0)
    }

    @Test
    fun `lambda shifts ranking towards recent papers`() {
        val a = paramRepository.loadScoringParams()
        val p0 = WeightParams(
            bm25Parameter = a.bm25Parameter,
            lambda = a.lambda,
            alpha = a.alpha,
            beta = a.beta,
            gamma = a.gamma
        )

        // Запрос без текстового фильтра, сортировка «как есть»
        val oldTopDate = searchService.searchPublications(SearchFilters(query=""), 1, 1)
            .first().publicationDate

        // Увеличиваем λ (ускоряем затухание старых статей)
        paramRepository.updateScoringParams(p0.copy(lambda = p0.lambda * 3))
        val newTopDate = searchService.searchPublications(SearchFilters(query=""), 1, 1)
            .first().publicationDate

        // чем больше λ, тем «моложе» первая статья
        assertThat(LocalDate.parse(newTopDate))
            .isAfterOrEqualTo(LocalDate.parse(oldTopDate))

        paramRepository.updateScoringParams(p0)
    }

    @Test
    fun `bm25 weight shifts ranking to text relevance`() {
        val a = paramRepository.loadScoringParams()
        val params0 = WeightParams(
            bm25Parameter = a.bm25Parameter,
            lambda = a.lambda,
            alpha = a.alpha,
            beta = a.beta,
            gamma = a.gamma
        )
        val query   = "neural network"

        /* 1) bm25 выключен  ------------------------------- */
        paramRepository.updateScoringParams(params0.copy(bm25Parameter = 0.0))
        val listNoText = searchService.searchPublications(
            SearchFilters(query = query), page = 1, pageSize = 20
        )
        val topNoTextTitle   = listNoText.firstOrNull()?.title          // ★

        /* 2) bm25 сильно увеличен  ------------------------- */
        paramRepository.updateScoringParams(params0.copy(bm25Parameter = 5.0))
        val listWithText = searchService.searchPublications(
            SearchFilters(query = query), page = 1, pageSize = 20
        )
        val topWithTextTitle = listWithText.firstOrNull()?.title        // ★

        /* 3) Проверяем, что первое место изменилось
              (или хотя-бы его итоговый score стал больше).           */
        assertThat(topWithTextTitle)
            .describedAs("при высоком весе BM25 первый результат должен смениться")
            .isNotEqualTo(topNoTextTitle)                               // ★

        /* 4) откатываем параметры  ------------------------- */
        paramRepository.updateScoringParams(params0)
    }


}


