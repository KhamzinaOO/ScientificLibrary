package org.olgakhamzina.scientificlibrarythesis.server

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

/**
 * SuggestService – сервис для получения подсказок (автодополнения) по различным фильтрам.
 * Оптимизирован: использует полнотекстовый поиск (MATCH ... AGAINST) вместо медленных операций LIKE.
 */
@Service
class SuggestService(private val jdbcTemplate: JdbcTemplate) {

    /**
     * Получает список подсказок по определенному типу фильтра.
     * @param type тип фасета: "author", "journal", "venue" или "pubType"
     * @param query подстрока, введенная пользователем (для автодополнения)
     * @param filters дополнительные фильтры, суужающие область поиска подсказок (например, чтобы предлагать авторов только по выбранному журналу и т.п.)
     * @return Список до 10 строк-подсказок, соответствующих введенному запросу.
     * @throws IllegalArgumentException если указан неподдерживаемый тип.
     *
     * Метод формирует SQL-запрос с использованием операторов MATCH ... AGAINST для эффективного поиска по началу слов.
     * Для каждого типа подсказок учитываются связанные ограничения:
     * - Author: опциональные фильтры по venue, journal, pubType.
     * - Journal: опциональные фильтры по venue, pubType, author.
     * - Venue: опциональные фильтры по journal, pubType, author.
     * - PubType: опциональные фильтры по venue, journal, author.
     */
    fun getSuggestions(type: String, query: String, filters: Map<String, String?>): List<String> {
        val trimmedQuery = query.trim()
        // Шаблон для полнотекстового поиска: добавляем '*' для префиксного поиска по словам
        val fullTextQuery = "$trimmedQuery*"

        return when (type.lowercase()) {
            "author" -> {
                // Подсказки по именам авторов
                // Оптимизация: используем полнотекстовый индекс по имени автора вместо LIKE
                val sql = """
                    SELECT DISTINCT a.name
                    FROM authors a
                    JOIN author_publication ap ON a.authorId = ap.author_id
                    JOIN publications p ON p.paperId = ap.publication_id
                    LEFT JOIN journals j ON p.journal_id = j.id
                    WHERE (? = '' OR MATCH(a.name) AGAINST(? IN BOOLEAN MODE))
                      AND (? = '' OR p.venue LIKE ?)
                      AND (? = '' OR j.name LIKE ?)
                      AND (? = '' OR EXISTS (
                                SELECT 1 FROM paper_publicationTypes pt
                                WHERE pt.paperId = p.paperId AND pt.typeName LIKE ?
                          ))
                    LIMIT 10
                """.trimIndent()
                // Фильтры
                val venueFilter = filters["venue"] ?: ""
                val journalFilter = filters["journal"] ?: ""
                val pubTypeFilter = filters["pubType"] ?: ""
                val args = arrayOf(
                    trimmedQuery, fullTextQuery,
                    venueFilter, "%$venueFilter%",
                    journalFilter, "%$journalFilter%",
                    pubTypeFilter, "%$pubTypeFilter%"
                )
                jdbcTemplate.queryForList(sql, args, String::class.java)
            }
            "journal" -> {
                // Подсказки по названиям журналов
                val sql = """
                    SELECT DISTINCT j.name
                    FROM journals j
                    JOIN publications p ON p.journal_id = j.id
                    LEFT JOIN author_publication ap ON p.paperId = ap.publication_id
                    LEFT JOIN authors a ON ap.author_id = a.authorId
                    WHERE (? = '' OR MATCH(j.name) AGAINST(? IN BOOLEAN MODE))
                      AND (? = '' OR p.venue LIKE ?)
                      AND (? = '' OR EXISTS (
                                SELECT 1 FROM paper_publicationTypes pt
                                WHERE pt.paperId = p.paperId AND pt.typeName LIKE ?
                          ))
                      AND (? = '' OR a.name LIKE ?)
                    LIMIT 10
                """.trimIndent()
                val venueFilter = filters["venue"] ?: ""
                val pubTypeFilter = filters["pubType"] ?: ""
                val authorFilter = filters["author"] ?: ""
                val args = arrayOf(
                    trimmedQuery, fullTextQuery,
                    venueFilter, "%$venueFilter%",
                    pubTypeFilter, "%$pubTypeFilter%",
                    authorFilter, "%$authorFilter%"
                )
                jdbcTemplate.queryForList(sql, args, String::class.java)
            }
            "venue" -> {
                // Подсказки по местам публикации (конференции, сборники)
                val sql = """
                    SELECT DISTINCT p.venue
                    FROM publications p
                    LEFT JOIN journals j ON p.journal_id = j.id
                    LEFT JOIN author_publication ap ON p.paperId = ap.publication_id
                    LEFT JOIN authors a ON ap.author_id = a.authorId
                    WHERE (? = '' OR MATCH(p.venue) AGAINST(? IN BOOLEAN MODE))
                      AND (? = '' OR j.name LIKE ?)
                      AND (? = '' OR EXISTS (
                                SELECT 1 FROM paper_publicationTypes pt
                                WHERE pt.paperId = p.paperId AND pt.typeName LIKE ?
                          ))
                      AND (? = '' OR a.name LIKE ?)
                    LIMIT 10
                """.trimIndent()
                val journalFilter = filters["journal"] ?: ""
                val pubTypeFilter = filters["pubType"] ?: ""
                val authorFilter = filters["author"] ?: ""
                val args = arrayOf(
                    trimmedQuery, fullTextQuery,
                    journalFilter, "%$journalFilter%",
                    pubTypeFilter, "%$pubTypeFilter%",
                    authorFilter, "%$authorFilter%"
                )
                jdbcTemplate.queryForList(sql, args, String::class.java)
            }
            "pubtype", "pubType" -> {
                // Подсказки по типам публикаций
                val sql = """
                    SELECT DISTINCT pt.typeName
                    FROM paper_publicationTypes pt
                    JOIN publications p ON pt.paperId = p.paperId
                    LEFT JOIN journals j ON p.journal_id = j.id
                    LEFT JOIN author_publication ap ON p.paperId = ap.publication_id
                    LEFT JOIN authors a ON ap.author_id = a.authorId
                    WHERE (? = '' OR MATCH(pt.typeName) AGAINST(? IN BOOLEAN MODE))
                      AND (? = '' OR p.venue LIKE ?)
                      AND (? = '' OR j.name LIKE ?)
                      AND (? = '' OR a.name LIKE ?)
                    LIMIT 10
                """.trimIndent()
                val venueFilter = filters["venue"] ?: ""
                val journalFilter = filters["journal"] ?: ""
                val authorFilter = filters["author"] ?: ""
                val args = arrayOf(
                    trimmedQuery, fullTextQuery,
                    venueFilter, "%$venueFilter%",
                    journalFilter, "%$journalFilter%",
                    authorFilter, "%$authorFilter%"
                )
                jdbcTemplate.queryForList(sql, args, String::class.java)
            }
            else -> throw IllegalArgumentException("Unsupported suggest type: $type")
        }
    }
}