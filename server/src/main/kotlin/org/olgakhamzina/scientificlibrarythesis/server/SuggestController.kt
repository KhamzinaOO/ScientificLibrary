package org.olgakhamzina.scientificlibrarythesis.server

import org.springframework.web.bind.annotation.*

/**
 * SuggestController – контроллер для эндпоинта подсказок /suggest.
 */
@RestController
@RequestMapping("/suggest")
class SuggestController(private val suggestService: SuggestService) {

    /**
     * GET /suggest – возвращает подсказки (автодополнение) для фасетных фильтров.
     * Обязательный параметр: type (author | journal | venue | pubType).
     * Параметр query – текущий ввод пользователя (подстрока).
     * Дополнительные параметры (необязательные) для сужения области поиска:
     *   - author (для фильтрации журналов/venue/pubType по конкретному автору),
     *   - journal (для фильтрации авторов/venue/pubType по конкретному журналу),
     *   - venue (для фильтрации авторов/journal/pubType по конкретному venue),
     *   - pubType (для фильтрации авторов/journal/venue по конкретному типу публикации).
     * Возвращает JSON-массив строк – до 10 подходящих значений.
     */
    @GetMapping
    fun suggest(
        @RequestParam type: String,
        @RequestParam query: String,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) journal: String?,
        @RequestParam(required = false) venue: String?,
        @RequestParam(required = false) pubType: String?
    ): List<String> {
        // Собираем фильтры в Map для передачи в сервис
        val filters = mapOf(
            "author" to author,
            "journal" to journal,
            "venue" to venue,
            "pubType" to pubType
        )
        return suggestService.getSuggestions(type, query, filters)
    }
}