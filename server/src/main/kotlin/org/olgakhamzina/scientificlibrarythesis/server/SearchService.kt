package org.olgakhamzina.scientificlibrarythesis.server

import org.olgakhamzina.scientificlibrarythesis.shared.Publication
import org.olgakhamzina.scientificlibrarythesis.shared.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.shared.SearchFilters
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.tartarus.snowball.ext.EnglishStemmer
import org.tartarus.snowball.ext.RussianStemmer
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * SearchService – сервисный класс для выполнения полнотекстового поиска публикаций с учетом фильтров и расчета рейтинга.
 * Оптимизирован для использования полнотекстовых индексов в БД и уменьшения объема данных, обрабатываемых в памяти.
 */
@Service
class SearchService(
    private val publicationRepository: PublicationRepository,
    private val paramRepository: ParamRepository,
    @Value("\${app.bm25.k:1.5}") private val bm25K: Double,    // BM25 параметр k (по умолчанию 1.5)
    @Value("\${app.bm25.b:0.75}") private val bm25B: Double,   // BM25 параметр b (по умолчанию 0.75)
    @Value("\${app.stemLanguage:english}") private val stemLanguage: String // Язык для стеммера: "english" или "russian"
) {

    // Стеммеры для английского и русского языков (для обработки поискового запроса перед расчетом BM25).
    private val englishStemmer = EnglishStemmer()
    private val russianStemmer = RussianStemmer()

    /**
     * Выполняет поиск публикаций по заданным фильтрам и текстовому запросу.
     * @param filters объект SearchFilters с критериями поиска (строка запроса и фасетные фильтры)
     * @param page номер страницы (начиная с 1)
     * @param pageSize размер страницы (количество результатов на страницу)
     * @return список публикаций, соответствующих критериям, с рассчитанным полем score (рейтинговый балл)
     *
     * Алгоритм:
     * 1. Запрашивает из БД публикации, удовлетворяющие фильтрам и текстовому запросу, **сразу на уровне SQL** (чтобы не загружать лишние данные).
     * 2. Для найденных публикаций выполняет расчет релевантности (score) на основе BM25 и дополнительных метрик (цитирования, h-index и др.).
     * 3. Сортирует результаты по убыванию score и возвращает нужную страницу результатов.
     */
    fun searchPublications(filters: SearchFilters, page: Int, pageSize: Int): List<Publication> {
        // 1. Получаем отфильтрованные публикации из базы данных с помощью оптимизированного запроса
        val filteredPublications: List<Publication> = publicationRepository.findPublicationsByFilters(filters)

        // 2. Подготавливаем данные для расчета BM25: общую статистику корпуса
        val totalDocs = filteredPublications.size.toDouble()
        val avgDocLength = filteredPublications.map { it.fullText.length }.average()  // средняя длина документа (для BM25)
        val queryTerms = preprocessQuery(filters.query)  // обработка запроса: приведение к нижнему регистру, стемминг и т.д.

        // 3. Рассчитываем промежуточные величины для BM25 и других метрик
        // Получаем текущие параметры ранжирования (ScoringParams) из репозитория настроек
        val scoringParams: ScoringParams = paramRepository.loadScoringParams()
        val bm25Weight = scoringParams.bm25Parameter
        val lambda = scoringParams.lambda
        val alpha = scoringParams.alpha
        val beta = scoringParams.beta
        val gamma = scoringParams.gamma

        // 4. Для каждой публикации из выборки рассчитываем релевантность
        for (pub in filteredPublications) {
            // Подготовка: получаем длину документа (количество терминов в fullText)
            val docLength = pub.fullText.split("\\s+".toRegex()).size.toDouble()

            // 4.a BM25: рассчитываем суммарный вес запроса по документу
            var bm25Score = 0.0
            for (term in queryTerms) {
                val termFrequency = countTermFrequency(term, pub.fullText)
                if (termFrequency == 0) continue  // данный термин отсутствует в документе
                // классическая формула BM25
                val idf = ln((totalDocs - getDocCountContaining(term, filteredPublications) + 0.5) / (getDocCountContaining(term, filteredPublications) + 0.5) + 1.0)
                bm25Score += idf * (termFrequency * (bm25K + 1)) / (termFrequency + bm25K * (1 - bm25B + bm25B * (docLength / avgDocLength)))
            }
            bm25Score *= bm25Weight  // учитываем вес BM25 в итоговом скоре



            // 4.c Затухание по времени: насколько стара публикация (в годах)
            val yearsOld = (LocalDate.now().year - pub.year).toDouble()

            // 4.b Дополнительные метрики: логарифмы h-index, числа цитирований, и число влиятельных цитирований
            val logH = if(pub.avgHIndex > 0) ln(pub.avgHIndex.toDouble() + 1.0) else 0.0
            val logC = if(pub.citationCount > 0) ln(pub.citationCount.toDouble() + 1.0) else 0.0
            val logI = if(pub.influentialCitationCount > 0) ln(pub.influentialCitationCount.toDouble() + 1.0) else 0.0

            val normH = if(scoringParams.maxLogHIndex > 0.0) logH / scoringParams.maxLogHIndex else 0.0
            val normC = if(scoringParams.maxLogCitations > 0.0) logC / scoringParams.maxLogCitations else 0.0
            val normI = if(scoringParams.maxLogInfluential > 0.0) logI / scoringParams.maxLogInfluential else 0.0

            val timeDecay = exp(-lambda * yearsOld)

            pub.score = bm25Score + timeDecay * (alpha * normH + beta * normC + gamma * normI)

        }

        // 5. Сортируем публикации по рассчитанному рейтингу (score) в порядке убывания
        val sortedByScore = filteredPublications.sortedByDescending { it.score }

        // 6. Реализуем постраничность (пагинацию) результатов
        val fromIndex = ((page - 1).coerceAtLeast(0)) * pageSize
        return if (fromIndex < sortedByScore.size) {
            sortedByScore.drop(fromIndex).take(pageSize)
        } else {
            emptyList()
        }
    }

    /**
     * Приводит поисковый запрос к нормализованной форме:
     * - переводит в нижний регистр
     * - выделяет слова и приводит их к базовой форме (стемминг) для английского и русского языков.
     */
    private fun preprocessQuery(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val terms = query.lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return terms.map { term ->
            when (stemLanguage.lowercase()) {
                "russian" -> { russianStemmer.current = term; russianStemmer.stem(); russianStemmer.current }
                else -> { englishStemmer.current = term; englishStemmer.stem(); englishStemmer.current }
            }
        }
    }

    /**
     * Подсчитывает частоту термина в тексте документа.
     */
    private fun countTermFrequency(term: String, text: String): Int {
        if (text.isEmpty()) return 0
        return text.lowercase().split("\\s+".toRegex()).count { it == term }
    }

    /**
     * Возвращает количество документов в коллекции, содержащих заданный терм.
     */
    private fun getDocCountContaining(term: String, documents: List<Publication>): Int {
        return documents.count { doc -> doc.fullText.lowercase().contains(term) }
    }
}

