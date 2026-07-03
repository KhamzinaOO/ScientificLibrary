package org.olgakhamzina.scientificlibrarythesis.server

import org.olgakhamzina.scientificlibrarythesis.shared.Publication
import org.olgakhamzina.scientificlibrarythesis.shared.SearchFilters
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * SearchController – контроллер для эндпоинта поиска публикаций /search.
 */
@RestController
@RequestMapping("/search")
class SearchController(private val searchService: SearchService) {

    /**
     * GET /search – полнотекстовый поиск публикаций с поддержкой фильтров, пагинации и расчётом рейтинга.
     * Параметры запроса:
     *  - query: строка поискового запроса (может быть пустой)
     *  - page: номер страницы (>=1, по умолчанию 1)
     *  - pageSize: размер страницы (по умолчанию 10)
     *  - year: год публикации (точное совпадение)
     *  - author: один или несколько авторов (через запятую)
     *  - journal: один или несколько журналов (через запятую)
     *  - venue: один или несколько venue (через запятую)
     *  - pubType: один или несколько типов публикации (через запятую)
     *  - fields: одна или несколько областей исследований (через запятую)
     *  - affiliation: фильтр по аффилиации автора (подстрока в названии организации)
     *  - openAccess: true/false – фильтр по открытости доступа
     *  - citationsFrom, citationsTo: диапазон количества цитирований
     *  - hindexFrom, hindexTo: диапазон значений среднего h-индекса авторов
     *  - dateFrom, dateTo: диапазон дат публикации (формат YYYY-MM-DD)
     *
     * Возвращает страницу списка публикаций (JSON-массив объектов Publication) с заполненным полем score, отсортированных по убыванию score.
     */
    @GetMapping
    fun search(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) pageSize: Int?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) journal: String?,
        @RequestParam(required = false) venue: String?,
        @RequestParam(required = false) pubType: String?,
        @RequestParam(required = false) fields: String?,
        @RequestParam(required = false) affiliation: String?,
        @RequestParam(required = false) openAccess: Boolean?,
        @RequestParam(required = false) citationsFrom: Int?,
        @RequestParam(required = false) citationsTo: Int?,
        @RequestParam(required = false) hindexFrom: Double?,
        @RequestParam(required = false) hindexTo: Double?,
        @RequestParam(required = false) dateFrom: String?,
        @RequestParam(required = false) dateTo: String?
    ): List<Publication> {
        // Разбираем множественные фильтры (author, journal, venue, pubType, fields) – строка с несколькими значениями через запятую.
        fun splitParam(param: String?): List<String> {
            if (param.isNullOrBlank()) return emptyList()
            return param.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        val filters = SearchFilters(
            query = query ?: "",
            year = year,
            authors = splitParam(author),
            journals = splitParam(journal),
            venues = splitParam(venue),
            pubTypes = splitParam(pubType),
            openAccess = openAccess,
            citationsFrom = citationsFrom,
            citationsTo = citationsTo,
            hindexFrom = hindexFrom,
            hindexTo = hindexTo,
            dateFrom = dateFrom,
            dateTo = dateTo
        )
        val pageNumber = page ?: 1
        val size = pageSize ?: 10

        // Вызываем сервис поиска
        return searchService.searchPublications(filters, pageNumber?: 1, pageSize ?: 10)
    }
}