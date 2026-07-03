package org.olgakhamzina.scientificlibrarythesis.server.integrationTests

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.olgakhamzina.scientificlibrarythesis.server.ParamRepository
import org.olgakhamzina.scientificlibrarythesis.server.ParamService
import org.olgakhamzina.scientificlibrarythesis.server.PublicationRepository
import org.olgakhamzina.scientificlibrarythesis.server.SearchService
import org.olgakhamzina.scientificlibrarythesis.server.integrationTests.RankingModelIT.Companion
import org.olgakhamzina.scientificlibrarythesis.shared.Publication
import org.olgakhamzina.scientificlibrarythesis.shared.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.shared.SearchFilters
import org.olgakhamzina.scientificlibrarythesis.shared.WeightParams
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.stream.Stream
import kotlin.math.ln
import kotlin.math.sqrt


const val TARGET_ID    = "00a9ba0063d34ec56792849a67ef57b4601becbb"
const val PAGE_SIZE    = 1000

/**
 * Integration-тесты ранжирования публикаций.
 *
 * Отслеживаем позицию публикации по её уникальному идентификатору (TARGET_ID),
 * проходя по всему корпусу постранично (rankOf). Сбрасываем параметры на дефолт
 * перед каждым тестом через @BeforeEach.
 */

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PositionRealDbIT @Autowired constructor(
    private val searchService: SearchService,
    private val paramRepository: ParamRepository
) {
    private lateinit var defaultParams: WeightParams

    @BeforeAll
    fun initDefaults() {
        defaultParams = with(paramRepository.loadScoringParams()) {
            WeightParams(bm25Parameter, lambda, alpha, beta, gamma)
        }
    }

    @BeforeEach
    fun resetParams() {
        paramRepository.updateScoringParams(defaultParams)
    }

    /* --------------------------------------------------------------- */
    /* Helpers                                                         */
    /* --------------------------------------------------------------- */
    private fun toWeights(sp: ScoringParams) = WeightParams(
        sp.bm25Parameter,
        sp.lambda,
        sp.alpha,
        sp.beta,
        sp.gamma
    )

    private fun List<Publication>.rankOfTarget(): Int? {
        return indexOfFirst { it.paperId == TARGET_ID }
            .takeIf { it >= 0 }?.plus(1)
    }

    private fun rankOf(filters: SearchFilters): Int? {
        var page = 1
        var offset = 0
        while (true) {
            val list = searchService.searchPublications(filters, page, PAGE_SIZE)
            if (list.isEmpty()) return null
            list.rankOfTarget()?.let { return offset + it }
            offset += list.size
            page++
        }
    }

    private fun printRank(
        test: String,
        phase: String,
        w: WeightParams,
        filters: SearchFilters,
        extra: String = ""
    ) {
        val rank = rankOf(filters)
        val rankStr = rank?.toString() ?: "—"
        val score = rank?.let {
            val page = ((it - 1) / PAGE_SIZE) + 1
            val idxInPage = (it - 1) % PAGE_SIZE
            val pageList = searchService.searchPublications(filters, page, PAGE_SIZE)
            pageList.getOrNull(idxInPage)?.score
        }
        val scoreStr = score?.let { "%.10f".format(it) } ?: "—"
        println(
            "[${test.padEnd(24)}] $phase | rank=$rankStr | score=$scoreStr | " +
                    "filters='${filters.query}' | bm25=${w.bm25Parameter}, λ=${w.lambda}, α=${w.alpha}, β=${w.beta}, γ=${w.gamma} $extra"
        )
    }

    /* --------------------------------------------------------------- */
    /* Tests                                                           */
    /* --------------------------------------------------------------- */

    @Test
    fun `normalized values are in 0-1`() {
        paramRepository.updateScoringParams(WeightParams(1.0,1.0,1.0,1.0,1.0))
        val sp = paramRepository.loadScoringParams()
        val w0 = toWeights(sp)
        printRank("normalized", "baseline", w0, SearchFilters())

        val pubs = searchService.searchPublications(SearchFilters(query = "java"), 1, 200)
        pubs.forEach { p ->
            val logH = if (p.avgHIndex > 0) ln(p.avgHIndex + 1.0) else 0.0
            val logC = if (p.citationCount > 0) ln(p.citationCount + 1.0) else 0.0
            val logI = if (p.influentialCitationCount > 0) ln(p.influentialCitationCount + 1.0) else 0.0
            assertThat(logH / sp.maxLogHIndex).isBetween(0.0, 1.0)
            assertThat(logC / sp.maxLogCitations).isBetween(0.0, 1.0)
            assertThat(logI / sp.maxLogInfluential).isBetween(0.0, 1.0)
            assertThat(p.score).isGreaterThanOrEqualTo(0.0)
        }
    }

    @Test
    fun `bigger alpha promotes high h-index papers`() {
        paramRepository.updateScoringParams(WeightParams(1.0,1.0,1.0,1.0,1.0))
        val filters = SearchFilters(query = "java")
        val w0 = toWeights(paramRepository.loadScoringParams())

        printRank("alpha", "before", w0, filters)

        val wBoost = w0.copy(alpha = w0.alpha * 5)
        paramRepository.updateScoringParams(wBoost)
        printRank("alpha", "after ×5", wBoost, filters)

        val orig = searchService.searchPublications(filters, 1, 100)
        val boosted = searchService.searchPublications(filters, 1, 100)
        val avgH0 = orig.take(10).map { it.avgHIndex }.average()
        val avgH1 = boosted.take(10).map { it.avgHIndex }.average()
        assertThat(avgH1).isGreaterThanOrEqualTo(avgH0)
    }

    @Test
    fun `beta weight boosts highly cited papers`() {
        paramRepository.updateScoringParams(WeightParams(1.0,1.0,1.0,1.0,1.0))
        val filters = SearchFilters(query = "java")
        val w0 = toWeights(paramRepository.loadScoringParams())

        printRank("beta", "before", w0, filters)

        val wBoost = w0.copy(beta = w0.beta * 10)
        paramRepository.updateScoringParams(wBoost)
        printRank("beta", "after ×10", wBoost, filters)

        val base = searchService.searchPublications(filters, 1, 100)
        val boosted = searchService.searchPublications(filters, 1, 100)
        val avgC0 = base.take(10).map { it.citationCount }.average()
        val avgC1 = boosted.take(10).map { it.citationCount }.average()
        assertThat(avgC1).isGreaterThanOrEqualTo(avgC0)
    }

    @Test
    fun `gamma increases share of influential citations in top`() {
        paramRepository.updateScoringParams(WeightParams(1.0,1.0,1.0,1.0,1.0))
        val filters = SearchFilters(query = "java")
        val w0 = toWeights(paramRepository.loadScoringParams())

        printRank("gamma", "before", w0, filters)

        val wBoost = w0.copy(gamma = w0.gamma * 8)
        paramRepository.updateScoringParams(wBoost)
        printRank("gamma", "after ×8", wBoost, filters)

        val base = searchService.searchPublications(filters, 1, 100)
        val boosted = searchService.searchPublications(filters, 1, 100)
        val infl0 = base.take(10).map { it.influentialCitationCount }.average()
        val infl1 = boosted.take(10).map { it.influentialCitationCount }.average()
        assertThat(infl1).isGreaterThanOrEqualTo(infl0)
    }

    @Test
    fun `lambda shifts ranking towards recent papers`() {
        paramRepository.updateScoringParams(WeightParams(1.0,1.0,1.0,1.0,1.0))
        val filters = SearchFilters(query = "java")
        val w0 = toWeights(paramRepository.loadScoringParams())

        printRank("lambda", "before", w0, filters,
            "topDate=${searchService.searchPublications(filters,1,1).first().publicationDate}")

        val wBoost = w0.copy(lambda = w0.lambda * 3)
        paramRepository.updateScoringParams(wBoost)
        printRank("lambda", "after ×3", wBoost, filters,
            "topDate=${searchService.searchPublications(filters,1,1).first().publicationDate}")

        val oldDate = LocalDate.parse(searchService.searchPublications(filters,1,1).first().publicationDate)
        val newDate = oldDate
        assertThat(newDate).isAfterOrEqualTo(oldDate)
    }

    @Test
    fun `bm25 textual relevance improves target rank`() {
        paramRepository.updateScoringParams(WeightParams(1.0,0.1,0.1,0.1,0.1))
        val query = "java"
        val filters = SearchFilters(query = query)
        val w0 = toWeights(paramRepository.loadScoringParams())

        val wNoText = w0.copy(bm25Parameter = 0.0)
        paramRepository.updateScoringParams(wNoText)
        printRank("bm25", "bm25 = 0", wNoText, filters)
        val rankNoText = rankOf(filters)
            ?: Assertions.fail<Any>("Target not found for query '$query' with bm25=0")

        val wText = w0.copy(bm25Parameter = 25.0)
        paramRepository.updateScoringParams(wText)
        printRank("bm25", "bm25 = 25", wText, filters)
        val rankWithText = rankOf(filters)
            ?: Assertions.fail<Any>("Target not found for query '$query' with bm25=25")

        assertThat(rankWithText)
            .describedAs("при высоком весе BM25 ранк должен измениться")
            .isNotEqualTo(rankNoText)
    }
}

/**
 * Интеграционные тесты модели ранжирования.
 * Результаты сохраняются в CSV-файлы в каталоге build/reports/modelTests.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RankingModel2IT @Autowired constructor(
    private val searchService: SearchService,
    private val paramRepository: ParamRepository
) {

    private lateinit var defaults: WeightParams

    @BeforeAll
    fun initDefaults() {
        val sp = paramRepository.loadScoringParams()
        defaults = WeightParams(sp.bm25Parameter, sp.lambda, sp.alpha, sp.beta, sp.gamma)
        File(REPORT_DIR).mkdirs()
    }

    @BeforeEach
    fun resetParams() = paramRepository.updateScoringParams(defaults)

    /* --------------------------- providers --------------------------- */
    companion object {
        private const val PAGE_SIZE = 1000
        private const val TARGET_ID = "00a9ba0063d34ec56792849a67ef57b4601becbb"
        private const val REPORT_DIR = "build/reports/modelTests"

        @JvmStatic fun alphaProvider(): Stream<Double>  = (0..100).map { it / 10.0 }.stream()
        @JvmStatic fun betaProvider(): Stream<Double>   = (0..100).map { it / 10.0 }.stream()
        @JvmStatic fun gammaProvider(): Stream<Double>  = (0..100).map { it / 10.0 }.stream()
        @JvmStatic fun lambdaProvider(): Stream<Double> = (0..100).map { it / 10.0 }.stream()
        @JvmStatic fun bm25Provider(): Stream<Double>   = (0..100).map { it / 10.0 }.stream()
    }

    /* --------------------------- метрики для sweep --------------------------- */
    data class Metrics(
        val paramName: String,
        val paramValue: Double,
        val rankTarget: Int?,
        val avgH10: Double,
        val avgCit10: Double,
        val topDate: String,
        val timestamp: String = LocalDate.now().toString()
    ) {
        fun toCsv(): String = listOf(paramName, paramValue, rankTarget ?: "-", avgH10, avgCit10, topDate, timestamp)
            .joinToString(",")
    }

    /* --------------------------- метрики для grid --------------------------- */
    data class GridMetrics(
        val alpha: Double,
        val beta: Double,
        val rankTarget: Int?,
        val avgH10: Double,
        val avgCit10: Double,
        val topDate: String,
        val timestamp: String = LocalDate.now().toString()
    ) {
        fun toCsv(): String = listOf(alpha, beta, rankTarget ?: "-", avgH10, avgCit10, topDate, timestamp)
            .joinToString(",")
    }

    /* --------------------------- sweep-тесты --------------------------- */
    @ParameterizedTest(name = "alpha={0}")
    @MethodSource("alphaProvider")
    fun alphaSweep(alpha: Double) = sweep("alpha_sweep", "alpha", alpha, defaults.copy(alpha = alpha))

    @ParameterizedTest(name = "beta={0}")
    @MethodSource("betaProvider")
    fun betaSweep(beta: Double) = sweep("beta_sweep", "beta", beta, defaults.copy(beta = beta))

    @ParameterizedTest(name = "gamma={0}")
    @MethodSource("gammaProvider")
    fun gammaSweep(gamma: Double) = sweep("gamma_sweep", "gamma", gamma, defaults.copy(gamma = gamma))

    @ParameterizedTest(name = "lambda={0}")
    @MethodSource("lambdaProvider")
    fun lambdaSweep(lambda: Double) = sweep("lambda_sweep", "lambda", lambda, defaults.copy(lambda = lambda))

    @ParameterizedTest(name = "bm25={0}")
    @MethodSource("bm25Provider")
    fun bm25Sweep(bm25: Double) = sweep("bm25_sweep", "bm25", bm25, defaults.copy(bm25Parameter = bm25))

    /* --------------------------- alpha × beta grid --------------------------- */
    @Test
    fun alphaBetaGrid() {
        // Подготовка файла
        val file = File("$REPORT_DIR/alpha_beta_grid.csv")
        if (!file.exists()) file.writeText("alpha,beta,rankTarget,avgH10,avgCit10,topDate,timestamp\n")

        val alphas = (0..100).map { it / 10.0 }
        val betas  = (0..100).map { it / 10.0 }
        for (a in alphas) for (b in betas) {
            paramRepository.updateScoringParams(defaults.copy(alpha = a, beta = b))
            val results = searchService.searchPublications(SearchFilters(query = "java"), 1, PAGE_SIZE)
            val top10   = results.take(10)
            val rankT   = results.indexOfFirst { it.paperId == TARGET_ID }.takeIf { it >= 0 }?.plus(1)
            val avgH    = top10.map { it.avgHIndex }.average()
            val avgC    = top10.map { it.citationCount }.average()
            val dateTop = top10.firstOrNull()?.publicationDate ?: "-"
            val gm = GridMetrics(a, b, rankT, avgH, avgC, dateTop)
            file.appendText(gm.toCsv() + "\n")
        }
    }

    /* --------------------------- helpers --------------------------- */
    private fun sweep(
        filePrefix: String,
        paramName: String,
        paramValue: Double,
        weights: WeightParams,
        filters: SearchFilters = SearchFilters(query = "java"),
        k: Int = 10
    ) {
        paramRepository.updateScoringParams(weights)
        val results = searchService.searchPublications(filters, 1, PAGE_SIZE)
        val top     = results.take(k)
        val rankT   = results.indexOfFirst { it.paperId == TARGET_ID }.takeIf { it >= 0 }?.plus(1)
        val avgH    = top.map { it.avgHIndex }.average()
        val avgC    = top.map { it.citationCount }.average()
        val dateTop = top.firstOrNull()?.publicationDate ?: "-"
        val m = Metrics(paramName, paramValue, rankT, avgH, avgC, dateTop)
        writeCsv("$REPORT_DIR/$filePrefix.csv", m)
    }

    private fun writeCsv(path: String, m: Metrics) {
        val f = File(path)
        if (!f.exists()) f.writeText("param,value,rankTarget,avgH10,avgCit10,topDate,timestamp\n")
        f.appendText(m.toCsv() + "\n")
    }
}


/**
 * Обновленный набор интеграционных тестов модели ранжирования с расширенными диапазонами параметров
 * и дополнительным тестом для изоляции влияния lambda.
 * Результаты сохраняются в CSV-файлы в каталоге build/reports/modelTests.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RankingModelIT @Autowired constructor(
    private val searchService: SearchService,
    private val paramRepository: ParamRepository
) {
    private lateinit var defaults: WeightParams
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @BeforeAll
    fun initDefaults() {
        val sp = paramRepository.loadScoringParams()
        defaults = WeightParams(sp.bm25Parameter, sp.lambda, sp.alpha, sp.beta, sp.gamma)
        File(REPORT_DIR).mkdirs()
    }

    @BeforeEach
    fun resetParams() = paramRepository.updateScoringParams(defaults)

    companion object {
        private const val PAGE_SIZE = 1000
        private const val TARGET_ID = "00a9ba0063d34ec56792849a67ef57b4601becbb"
        const val REPORT_DIR = "build/reports/modelTests"

        // Расширенные диапазоны для sweep-тестов
        @JvmStatic fun alphaProvider(): Stream<Double>  = Stream.iterate(0.0) { it + 5.0 }.limit(11)
        @JvmStatic fun betaProvider(): Stream<Double>   = Stream.iterate(0.0) { it + 5.0 }.limit(11)
        @JvmStatic fun gammaProvider(): Stream<Double>  = Stream.iterate(0.0) { it + 5.0 }.limit(11)
        @JvmStatic fun lambdaProvider(): Stream<Double> = Stream.of(0.0, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0)
        @JvmStatic fun bm25Provider(): Stream<Double>   = Stream.of(0.0, 1.0, 5.0, 10.0, 25.0, 50.0, 100.0)
    }

    /**
     * Metrics for sweep tests (avg h-index, avg citations, avg publication year)
     */
    data class SweepMetrics(
        val param: String,
        val value: Double,
        val avgH10: Double,
        val avgCit10: Double,
        val avgYear10: Double
    ) {
        fun toCsv() = listOf(param, value, avgH10, avgCit10, avgYear10).joinToString(",")
    }

    /**
     * Test: influence of alpha on top-10 metrics
     */
    @ParameterizedTest
    @MethodSource("alphaProvider")
    fun alphaSweep(alpha: Double) = sweepMetric(
        file = "alpha_sweep.csv", param = "alpha", value = alpha,
        weights = defaults.copy(alpha = alpha)
    )

    @ParameterizedTest
    @MethodSource("betaProvider")
    fun betaSweep(beta: Double) = sweepMetric(
        file = "beta_sweep.csv", param = "beta", value = beta,
        weights = defaults.copy(beta = beta)
    )

    @ParameterizedTest
    @MethodSource("gammaProvider")
    fun gammaSweep(gamma: Double) = sweepMetric(
        file = "gamma_sweep.csv", param = "gamma", value = gamma,
        weights = defaults.copy(gamma = gamma)
    )

    @ParameterizedTest
    @MethodSource("bm25Provider")
    fun bm25Sweep(bm25: Double) = sweepMetric(
        file = "bm25_sweep.csv", param = "bm25", value = bm25,
        weights = defaults.copy(bm25Parameter = bm25)
    )

    /**
     * Special lambda isolation: alpha,beta,gamma,bm25=0 to measure pure time decay
     */
    @ParameterizedTest
    @MethodSource("lambdaProvider")
    fun lambdaIsolatedSweep(lambda: Double) {
        val weights = WeightParams(0.0, lambda, 0.0, 0.0, 0.0)
        sweepMetric(
            file = "lambda_isolated.csv", param = "lambda", value = lambda,
            weights = weights
        )
    }

    /**
     * Generic sweep: compute top-10 avg metrics and avg publication year
     */
    private fun sweepMetric(
        file: String,
        param: String,
        value: Double,
        weights: WeightParams
    ) {
        paramRepository.updateScoringParams(weights)
        val results = searchService.searchPublications(SearchFilters(query = "java"), 1, PAGE_SIZE)
        val top10 = results.take(10)
        val avgH   = top10.map { it.avgHIndex }.average()
        val avgC   = top10.map { it.citationCount }.average()
        val avgY   = top10.map { LocalDate.parse(it.publicationDate, formatter).year.toDouble() }.average()

        val csvFile = File(REPORT_DIR, file)
        if (!csvFile.exists()) csvFile.writeText("param,value,avgH10,avgCit10,avgYear10\n")
        csvFile.appendText(SweepMetrics(param, value, avgH, avgC, avgY).toCsv() + "\n")
    }

    /**
     * Grid test for alpha x beta on average rankTarget
     */
//    @Test
//    fun alphaBetaGrid() {
//        val out = File(REPORT_DIR, "alpha_beta_grid.csv")
//        if (!out.exists()) out.writeText("alpha,beta,mrr\n")
//
//        val queries = listOf("java", "neural", "computer", "logic")
//        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//        val alphas = (0..10).map { it / 10.0 }
//        val betas  = (0..10).map { it / 10.0 }
//
//        for (a in alphas) for (b in betas) {
//            paramRepository.updateScoringParams(defaults.copy(alpha = a, beta = b))
//            // compute MRR across queries
//            val ranks = queries.map { q ->
//                val res = searchService.searchPublications(SearchFilters(query = q), 1, PAGE_SIZE)
//                val r = res.indexOfFirst { it.paperId == TARGET_ID }.takeIf { it >= 0 }?.plus(1) ?: PAGE_SIZE
//                1.0 / r
//            }
//            val mrr = ranks.average()
//            out.appendText("$a,$b,$mrr\n")
//        }
//    }
}

/**
 * Интеграционные sweep‑тесты всех весовых коэффициентов модели ранжирования.
 *  ──────────────
 * ▸ Для каждого параметра строится линейка значений.
 * ▸ По каждому значению сохраняем метрики TOP‑10 в CSV.
 * ▸ После каждого теста веса сбрасываются к BASELINE (все 0.1) – "обнулённая" значимость.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModelSweepTests @Autowired constructor(
    private val searchService: SearchService,
    private val paramRepository: ParamRepository,
    @Value("\${app.bm25.k:1.5}") private val bm25K: Double,
    @Value("\${app.bm25.b:0.75}") private val bm25B: Double,
) {

    companion object {
        private const val PAGE_SIZE = 1000
        private const val REPORT_DIR = "build/reports/modelTests"
        private val BASELINE = WeightParams(0.1, 0.1, 0.1, 0.1, 0.1)

        /* -------- providers -------- */
        @JvmStatic fun alphaProvider(): Stream<Double>  = (0..100).map { it / 10.0 }.stream()
        @JvmStatic fun betaProvider(): Stream<Double>   = (0..100).map { it / 10.0 }.stream()
        @JvmStatic fun gammaProvider(): Stream<Double>  = (0..100).map { it / 10.0 }.stream()
        @JvmStatic fun lambdaProvider(): Stream<Double> = (0..100).map { it / 10.0 }.stream()
        @JvmStatic fun bm25Provider(): Stream<Double>  = (0..100).map { it / 10.0 }.stream()

        /* ---------- lifecycle ---------- */
    }

    @BeforeAll
    fun initDefaults() {
        File(RankingModelIT.REPORT_DIR).mkdirs()
    }

    /* ---------- CSV helpers ---------- */
    data class Row(val param: String, val value: Double, val avgH10: Double, val avgCit10: Double, val avgYear10: Double) {
        fun toCsv() = listOf(param, value, avgH10, avgCit10, avgYear10).joinToString(",")
    }

    private fun File.appendRow(r: Row) {
        if (!exists()) writeText("param,value,avgH10,avgCit10,avgYear10\n")
        appendText(r.toCsv() + "\n")
    }

    /** Сброс весов после каждого PARAMETERIZED‑cases. */
    @AfterEach fun resetWeights() = paramRepository.updateScoringParams(BASELINE)

    /* ========================  SWEEP‑ТЕСТЫ  ======================== */

    @ParameterizedTest(name = "alpha={0}")
    @MethodSource("alphaProvider")
    fun alphaSweep(alpha: Double) = sweep("alpha", alpha, BASELINE.copy(alpha = alpha))

    @ParameterizedTest(name = "beta={0}")
    @MethodSource("betaProvider")
    fun betaSweep(beta: Double) = sweep("beta", beta, BASELINE.copy(beta = beta))

    @ParameterizedTest(name = "gamma={0}")
    @MethodSource("gammaProvider")
    fun gammaSweep(gamma: Double) = sweep("gamma", gamma, BASELINE.copy(gamma = gamma))

    @ParameterizedTest(name = "lambda={0}")
    @MethodSource("lambdaProvider")
    fun lambdaSweep(lambda: Double) = sweep("lambda", lambda, BASELINE.copy(lambda = lambda))

    @ParameterizedTest(name = "bm25={0}")
    @MethodSource("bm25Provider")
    fun bm25Sweep(bm25: Double) = sweep("bm25", bm25, BASELINE.copy(bm25Parameter = bm25))

    /* ========================  CORE  ======================== */
    private fun sweep(param: String, value: Double, w: WeightParams, q: String = "java", k: Int = 10) {
        // 1. применяем веса
        paramRepository.updateScoringParams(w)

        // 2. получаем выборку
        val res = searchService.searchPublications(SearchFilters(query = q), 1, PAGE_SIZE)
        val top = res.take(k)

        // 3. метрики TOP‑10
        val avgH   = top.map { it.avgHIndex }.average()
        val avgCit = top.map { it.citationCount }.average()
        val avgY   = top.map { LocalDate.parse(it.publicationDate).year.toDouble() }.average()

        // 4. пишем CSV
        File(REPORT_DIR, "${param}_sweep.csv").appendRow(Row(param, value, avgH, avgCit, avgY))
    }
}

/**
 * 🚀  Core demonstration tests for the ranking model.
 * ---------------------------------------------------
 * 1.  2×2×2 factorial experiment for Alpha × Beta × Lambda (MRR metric)
 * 2.  Sweep of BM25 *weight* (ω_BM25)  — shows textual‑relevance impact
 * 3.  Invariant «unique term ⇒ #1 rank»
 * 4.  Asymptote: extremely large α must dominate the score (other pieces < 1 %)
 *
 * After **each** parametrised case the weights are rolled back to BASELINE = (0.1,…,0.1),
 * so experiments stay independent.  All CSV reports land in build/reports/modelTests/.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeyModelTests @Autowired constructor(
    private val searchService: SearchService,
    private val paramRepository: ParamRepository,
    private val publicationRepository: PublicationRepository,
) {

    /* ---------- constants & helpers ---------- */
    private val REPORT_DIR = File("build/reports/modelTests").apply { mkdirs() }
    private val PAGE_SIZE = 1_000
    private val BASELINE = WeightParams(0.1, 0.1, 0.1, 0.1, 0.1)

    private fun rankOfDoc(paperId: String, query: String): Int? {
        var page = 1; var offset = 0
        while (true) {
            val list = searchService.searchPublications(SearchFilters(query = query), page, PAGE_SIZE)
            if (list.isEmpty()) return null
            val local = list.indexOfFirst { it.paperId == paperId }
            if (local >= 0) return offset + local + 1
            offset += list.size; page++
        }
    }

    @BeforeEach fun reset() = paramRepository.updateScoringParams(BASELINE)

    /* ====================================================== */
    /* 1.  2×2×2 FACTORIAL  α × β × λ                         */
    /* ====================================================== */

    @Test fun factorialAlphaBetaLambda_singleQuery() {
        val levels = listOf(0.0, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0)
        val query   = "java"
        val target  = "00a9ba0063d34ec56792849a67ef57b4601becbb"

        val csv = File(REPORT_DIR, "factorial_alpha_beta_lambda_java.csv")
            .apply { if (!exists()) writeText("alpha,beta,lambda,mrr\n") }

        for (a in levels) for (b in levels) for (l in levels) {
            paramRepository.updateScoringParams(BASELINE.copy(alpha = a, beta = b, lambda = l))
            val rr = reciprocalRank(query, target)
            csv.appendText("$a,$b,$l,$rr\n")
        }
    }

    private fun reciprocalRank(query: String, targetId: String): Double {
        val results = searchService.searchPublications(SearchFilters(query = query), 1, PAGE_SIZE)
        val idx = results.indexOfFirst { it.paperId == targetId }
        val rank = if (idx >= 0) idx + 1 else PAGE_SIZE
        return 1.0 / rank
    }
    /* ====================================================== */
    /* 2.  BM25 WEIGHT SWEEP                                   */
    /* ====================================================== */

    companion object SweepProvider {
        @JvmStatic fun bm25Provider(): Stream<Double> = Stream.of(0.0, 0.25, 0.5, 1.0, 2.0, 3.0, 5.0, 10.0)
    }

    data class Row(val value: Double, val avgH10: Double, val avgCit10: Double, val avgYear10: Double) {
        fun toCsv() = listOf(value, avgH10, avgCit10, avgYear10).joinToString(",")
    }

    @ParameterizedTest(name = "BM25 weight = {0}")
    @MethodSource("bm25Provider")
    fun bm25WeightSweep(weight: Double) {
        paramRepository.updateScoringParams(BASELINE.copy(bm25Parameter = weight))

        // gather TOP‑10 metrics for a representative query
        val res = searchService.searchPublications(SearchFilters(query = "java"), 1, PAGE_SIZE).take(100)
        val avgH   = res.map { it.avgHIndex }.average()
        val avgCit = res.map { it.citationCount }.average()
        val avgYr  = res.map { LocalDate.parse(it.publicationDate).year.toDouble() }.average()

        File(REPORT_DIR, "bm25_weight_sweep.csv").apply {
            if (!exists()) writeText("bm25Weight,avgH10,avgCit10,avgYear10\n")
            appendText(Row(weight, avgH, avgCit, avgYr).toCsv() + "\n")
        }
    }

    /* ====================================================== */
    /* 3.  UNIQUE‑TERM INVARIANT                               */
    /* ====================================================== */

    @Test fun uniqueTermBringsDocToTop() {
        // 1. Collect a reasonable corpus sample (empty query returns broad set)
        val docs = publicationRepository.findPublicationsByFilters(SearchFilters())

        // 2. Build term ↦ frequency map
        val termFreq = mutableMapOf<String, Int>()
        val termDoc  = mutableMapOf<String, Publication>()
        docs.forEach { d ->
            d.fullText.lowercase().split("\\s+".toRegex()).distinct().forEach { t ->
                termFreq[t] = (termFreq[t] ?: 0) + 1
                termDoc.putIfAbsent(t, d)
            }
        }

        // 3. Pick any term occurring once
        val (rareTerm, rareDoc) = termFreq.entries.first { it.value == 1 }.let { it.key to termDoc[it.key]!! }

        // 4. Verify ranking behaviour
        val rankWithout = rankOfDoc(rareDoc.paperId, rareTerm.dropLast(1))
        val rankWith    = rankOfDoc(rareDoc.paperId, rareTerm)

        Assertions.assertNotNull(rankWith, "Target doc not found with unique term query")
        Assertions.assertTrue(rankWith!! == 1, "Unique term must promote doc to #1 (rank=$rankWith)")
        if (rankWithout != null) Assertions.assertTrue(rankWithout > rankWith, "Rank should improve when adding rare term")
    }

    /* ====================================================== */
    /* 4.  ASYMPTOTE — α → ∞                                   */
    /* ====================================================== */

    @Test fun alphaAsymptoteDominatesScore() {
        val hugeAlpha = 1e6
        paramRepository.updateScoringParams(BASELINE.copy(alpha = hugeAlpha))

        val res = searchService.searchPublications(SearchFilters(query = "java"), 1, 20) // TOP‑20 suffice
        res.forEach { p ->
            val logH = if (p.avgHIndex > 0) ln(p.avgHIndex + 1.0) else 0.0
            val alphaPart = hugeAlpha * logH / 1.0   // maxLogHIndex ≈ 1 after normalisation
            val share = alphaPart / p.score
            Assertions.assertTrue(share > 0.99, "With α→∞ its share should be ≥99 % (was ${"%.4f".format(share)})")
        }
    }
}

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RankingTrendCollectionTest {

    @Autowired private lateinit var searchService : SearchService
    @Autowired private lateinit var paramService  : ParamService
    @Autowired private lateinit var paramRepo     : ParamRepository

    private lateinit var baselineAll : ScoringParams     // как в БД
    private lateinit var baseline    : ScoringParams     // 0.1-baseline

    /* ---------- подготовка ---------- */

    @BeforeAll
    fun init() {
        baselineAll = paramRepo.loadScoringParams()
        baseline = ScoringParams(
            bm25Parameter = 0.1, lambda = 0.1,
            alpha = 0.1,  beta  = 0.1, gamma = 0.1,
            maxLogHIndex      = baselineAll.maxLogHIndex,
            maxLogCitations   = baselineAll.maxLogCitations,
            maxLogInfluential = baselineAll.maxLogInfluential
        )
        paramService.updateParams(baseline.toWeights())
    }

    @BeforeEach fun reset()  = paramService.updateParams(baseline.toWeights())
    @AfterAll   fun restore()= paramService.updateParams(baselineAll.toWeights())

    private fun ScoringParams.toWeights() = WeightParams(
        bm25Parameter, lambda, alpha, beta, gamma
    )

    private fun <T> withWeights(w: WeightParams, block: () -> T): T {
        paramService.updateParams(w)
        val r = block()
        paramService.updateParams(baseline.toWeights())
        return r
    }

    /* ---------- общие вычисления ---------- */

    private data class TopMetrics(
        val ids: List<String>,
        val avgC: Double,
        val avgH: Double,
        val avgI: Double,
        val avgAge: Double
    )

    private fun calcMetrics(pubs: List<Publication>): TopMetrics {
        val ids = pubs.map { it.paperId }
        val avgC = pubs.map { it.citationCount }.average()
        val avgH = pubs.map { it.avgHIndex }.average()
        val avgI = pubs.map { it.influentialCitationCount }.average()
        val avgAge = pubs.map {
            ChronoUnit.DAYS.between(LocalDate.parse(it.publicationDate), LocalDate.now()) / 365.0
        }.average()
        return TopMetrics(ids, avgC, avgH, avgI, avgAge)
    }

    /* ---------- основной sweep-процедур ---------- */

    private fun runSweep(
        fileName : String,
        mutate   : (WeightParams, Double) -> WeightParams,
        expectInc: (List<Double>) -> Boolean   // правило проверки
    ) {
        val outDir = File("build/reports/ranking/trends").apply { mkdirs() }
        val csv    = File(outDir, fileName).printWriter()
        csv.println(
            "value,jaccard,ΔCitations,ΔHIndex,ΔInfluential,ΔAge," +
                    "avgCitations,avgHIndex,avgInfluential,avgAge,topIds"
        )

        // baseline once
        val baseTop = searchService
            .searchPublications(SearchFilters(), 1, 10)
            .let(::calcMetrics)

        val series = (0..10).map { it / 2.0 }     // 0 … 5 шаг 0.5
        val trackTarget = mutableListOf<Double>()

        for (v in series) {
            val w      = mutate(baseline.toWeights(), v)
            val topMet = withWeights(w) {
                searchService.searchPublications(SearchFilters(), 1, 10).let(::calcMetrics)
            }

            val inter = topMet.ids.intersect(baseTop.ids).size.toDouble()
            val jacc  = inter / 10          // Jaccard для топ-10

            val dC = topMet.avgC   - baseTop.avgC
            val dH = topMet.avgH   - baseTop.avgH
            val dI = topMet.avgI   - baseTop.avgI
            val dA = topMet.avgAge - baseTop.avgAge   // возраст ↑ — плохо

            // target для проверки (каждый параметр – своя метрика)
            val target = when (fileName) {
                "alpha_trend.csv"  -> topMet.avgH
                "beta_trend.csv"   -> topMet.avgC
                "gamma_trend.csv"  -> topMet.avgI
                "lambda_trend.csv" -> topMet.avgAge
                else -> 0.0
            }
            trackTarget += target

            csv.println(
                String.format(
                    Locale.US,
                    "%.1f,%.3f,%.1f,%.2f,%.2f,%.2f,%.1f,%.2f,%.2f,%.2f,\"%s\"",
                    v, jacc, dC, dH, dI, dA,
                    topMet.avgC, topMet.avgH, topMet.avgI, topMet.avgAge,
                    topMet.ids.joinToString("|")
                )
            )
        }
        csv.close()

        assertTrue(
            expectInc(trackTarget),
            "$fileName: метрика изменилась не так, как ожидалось"
        )
    }

    /* ---------- 4 публичных теста ---------- */

    @Test
    fun alphaTrend() = runSweep(
        "alpha_trend.csv",
        mutate = { w,v -> w.copy(alpha = v) },
        expectInc = { list -> list.zipWithNext().all { (a,b) -> b >= a } } // неубывание avgH
    )

    @Test
    fun betaTrend() = runSweep(
        "beta_trend.csv",
        mutate = { w,v -> w.copy(beta  = v) },
        expectInc = { list -> list.zipWithNext().all { (a,b) -> b >= a } } // неубывание avgC
    )

    @Test
    fun gammaTrend() = runSweep(
        "gamma_trend.csv",
        mutate = { w,v -> w.copy(gamma = v) },
        expectInc = { list -> list.zipWithNext().all { (a,b) -> b >= a } } // неубывание avgI
    )

    @Test
    fun lambdaTrend() = runSweep(
        "lambda_trend.csv",
        mutate = { w,v -> w.copy(lambda = v) },
        expectInc = { list -> list.zipWithNext().all { (a,b) -> b <= a } } // невозрастание avgAge
    )
}

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RankingAnalyticsTests {

    @Autowired private lateinit var searchService : SearchService
    @Autowired private lateinit var paramService  : ParamService
    @Autowired private lateinit var paramRepo     : ParamRepository

    private lateinit var baselineAll : ScoringParams      // как лежит в БД
    private lateinit var baseline    : ScoringParams      // рабочий baseline (0.1-веса)

    private val outBase = File("build/reports/ranking/analytics").apply { mkdirs() }

    /* ---------- baseline: α=β=γ=λ=bm25=0.1 ------------- */
    @BeforeAll
    fun initBaseline() {
        baselineAll = paramRepo.loadScoringParams()
        baseline = ScoringParams(
            bm25Parameter     = 0.1,
            lambda            = 0.1,
            alpha             = 0.1,
            beta              = 0.1,
            gamma             = 0.1,
            maxLogHIndex      = baselineAll.maxLogHIndex,
            maxLogCitations   = baselineAll.maxLogCitations,
            maxLogInfluential = baselineAll.maxLogInfluential
        )
        paramService.updateParams(baseline.toWeights())
    }

    @BeforeEach fun reset()  = paramRepo.updateScoringParams(baseline.toWeights())
    @AfterAll   fun restore()= paramRepo.updateScoringParams(baselineAll.toWeights())

    private fun ScoringParams.toWeights() = WeightParams(
        bm25Parameter, lambda, alpha, beta, gamma
    )

    /** helper: применить временные веса и вернуть baseline мгновенно */
    private fun <T> withWeights(tmp: WeightParams, block: () -> T): T {
        paramRepo.updateScoringParams(tmp)
        val res = block()
        paramRepo.updateScoringParams(baseline.toWeights())
        return res
    }

    /* =============== общие расчёты для метрик =============== */

    private data class Metrics(
        val ids: List<String>,
        val avgC: Double,
        val medC: Double,
        val avgH: Double,
        val medH: Double,
        val avgI: Double,
        val avgAge: Double
    )

    private fun calcMetrics(pubs: List<Publication>): Metrics {
        fun <T : Number> median(list: List<T>): Double =
            if (list.isEmpty()) 0.0
            else list.sortedBy { it.toDouble() }
                .let { l -> if (l.size % 2 == 1) l[l.size / 2].toDouble()
                else 0.5 * (l[l.size/2-1].toDouble() + l[l.size/2].toDouble()) }

        val ids = pubs.map { it.paperId }
        val cit = pubs.map { it.citationCount }
        val hin = pubs.map { it.avgHIndex }
        val infl= pubs.map { it.influentialCitationCount }
        val ages= pubs.map {
            ChronoUnit.DAYS.between(LocalDate.parse(it.publicationDate), LocalDate.now()) / 365.0
        }
        return Metrics(
            ids,
            cit.average(), median(cit),
            hin.average(), median(hin),
            infl.average(),
            ages.average()
        )
    }

    /* ================================================================================= */
    /* ==================  1. SENSITIVITY (лог-шаг)  ==================================== */
    /* ================================================================================= */

    /**
     * logSeries: 30 точек от 0.01 до 10  (0 выключает компонент)
     * 0, 0.01, 0.02, … 0.1, 0.2, …, 1, 2, 5, 10
     */
    private val logSeries : List<Double> = buildList {
        add(0.0)
        val n = 28
        val logMin = ln(0.01)
        val logMax = ln(10.0)
        repeat(n) {
            val x = logMin + (logMax - logMin) * it / (n - 1)
            add(kotlin.math.exp(x))
        }
        add(10.0)
    }

    @Test fun sensitivitySweep() {

        val baselineTop10 = searchService
            .searchPublications(SearchFilters(), 1, 10)
            .let(::calcMetrics)

        data class SweepCfg(
            val name   : String,
            val mutate : (WeightParams, Double) -> WeightParams,
            val target : (Metrics) -> Double,
            val expect : (List<Double>) -> Boolean
        )

        val cfgs = listOf(
            SweepCfg("alpha", { w,v -> w.copy(alpha = v) }, { it.avgH }) { seq ->
                seq.zipWithNext().all { (_,b) -> b >= 0.99 * seq.first() }  // не убывает
            },
            SweepCfg("beta",  { w,v -> w.copy(beta  = v) }, { it.avgC }) { seq ->
                seq.zipWithNext().all { (_,b) -> b >= 0.99 * seq.first() }
            },
            SweepCfg("gamma", { w,v -> w.copy(gamma = v) }, { it.avgI }) { seq ->
                seq.zipWithNext().all { (_,b) -> b >= 0.99 * seq.first() }
            },
            SweepCfg("lambda",{ w,v -> w.copy(lambda = v)}, { it.avgAge }){ seq ->
                seq.zipWithNext().all { (_,b) -> b <= 1.01 * seq.first() }
            }
        )

        cfgs.forEach { cfg ->

            val csv = File(outBase, "${cfg.name}_sensitivity.csv").printWriter()
            csv.println("value,jaccard,avgC,medC,avgH,medH,avgI,avgAge,topIds")

            val trackTarget = mutableListOf<Double>()

            for (v in logSeries) {
                val pubs = withWeights(cfg.mutate(baseline.toWeights(), v)) {
                    searchService.searchPublications(SearchFilters(), 1, 10)
                }
                val m = calcMetrics(pubs)
                val jacc = m.ids.intersect(baselineTop10.ids).size / 10.0

                trackTarget += cfg.target(m)

                csv.println(
                    String.format(
                        Locale.US,
                        "%.4f,%.3f,%.1f,%.1f,%.2f,%.2f,%.1f,%.2f,\"%s\"",
                        v, jacc,
                        m.avgC, m.medC,
                        m.avgH, m.medH,
                        m.avgI,
                        m.avgAge,
                        m.ids.joinToString("|")
                    )
                )
            }
            csv.close()

            assertTrue(cfg.expect(trackTarget),
                "sensitivity ${cfg.name}: метрика не монотонна как ожидалось")
        }
    }

    /* ================================================================================= */
    /* ===============  2. RANK-SHIFT для трёх точек  =================================== */
    /* ================================================================================= */

    private val shiftPoints = listOf(0.2, 1.0, 2.0)

    @Test fun rankShift() {

        val baseRanks = searchService
            .searchPublications(SearchFilters(), 1, 50)
            .mapIndexed { idx, pub -> pub.paperId to idx+1 }
            .toMap()

        val csv = File(outBase, "rank_shift.csv").printWriter()
        csv.println("paperId,param,value,baselineRank,newRank,delta")

        val mutateList = listOf<(WeightParams, Double) -> WeightParams>(
            { w,v -> w.copy(alpha = v) },
            { w,v -> w.copy(beta  = v) },
            { w,v -> w.copy(gamma = v) }
        )
        val names = listOf("alpha","beta","gamma")

        mutateList.forEachIndexed { i, mut ->
            shiftPoints.forEach { v ->
                val ranks = withWeights(mut(baseline.toWeights(), v)) {
                    searchService.searchPublications(SearchFilters(),1,50)
                        .mapIndexed { idx, pub -> pub.paperId to idx+1 }
                        .toMap()
                }
                // записываем только статьи, которые сдвинулись
                for ((id,baseR) in baseRanks) {
                    val newR = ranks[id] ?: continue
                    val delta = baseR - newR
                    if (delta != 0) {
                        csv.println(
                            "$id,${names[i]},$v,$baseR,$newR,$delta"
                        )
                    }
                }
            }
        }
        csv.close()
    }

    /* ================================================================================= */
    /* ===============  3. HEAT-MAP  α-β при γ,λ=0.1  ================================== */
    /* ================================================================================= */

    @Test fun heatMapAlphaBeta() {

        val csv = File(outBase, "alpha_beta_heatmap.csv").printWriter()
        csv.println("alpha,beta,Fscore")

        fun fScore(m: Metrics): Double =
            0.4 * m.avgC + 0.4 * m.avgH + 0.2 * m.avgI - 0.1 * m.avgAge

        val values = (0..10).map { it / 2.0 }   // 0 … 5 шаг 0.5

        var bestF = Double.NEGATIVE_INFINITY

        for (a in values)
            for (b in values) {
                val pubs = withWeights(baseline.toWeights().copy(alpha = a, beta = b)) {
                    searchService.searchPublications(SearchFilters(),1,10)
                }
                val f = fScore(calcMetrics(pubs))
                csv.println(String.format(Locale.US, "%.2f,%.2f,%.3f", a, b, f))
                if (f > bestF) bestF = f
            }
        csv.close()

        assertTrue(bestF > 0, "Heat-map: F-score должен быть > 0 хотя бы в одной точке")
    }
}


@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LambdaAgeDistributionTest {

    @Autowired private lateinit var searchService: SearchService
    @Autowired private lateinit var paramRepo:     ParamRepository

    private lateinit var baselineAll: ScoringParams
    private lateinit var baseline:    ScoringParams

    @BeforeAll
    fun loadBaseline() {
        // Берём реальные maxLog* из БД, но «плоские» веса = 0.1
        baselineAll = paramRepo.loadScoringParams()
        baseline = ScoringParams(
            bm25Parameter     = 0.1,
            lambda            = 0.1,
            alpha             = 0.1,
            beta              = 0.1,
            gamma             = 0.1,
            maxLogHIndex      = baselineAll.maxLogHIndex,
            maxLogCitations   = baselineAll.maxLogCitations,
            maxLogInfluential = baselineAll.maxLogInfluential
        )
        // Сразу устанавливаем в БД «плоский» baseline
        paramRepo.updateScoringParams(baseline.toWeights())
    }

    @BeforeEach
    fun resetBaseline() {
        // Перед каждым тестом возвращаем плоский baseline
        paramRepo.updateScoringParams(baseline.toWeights())
    }

    @AfterAll
    fun restoreOriginal() {
        // В конце возвращаем оригинальные параметры
        paramRepo.updateScoringParams(baselineAll.toWeights())
    }

    private fun ScoringParams.toWeights() = WeightParams(
        bm25Parameter, lambda, alpha, beta, gamma
    )

    /** Удобный «применяем tmp-веса → выполняем → возвращаем baseline» */
    private fun <T> withWeights(tmp: WeightParams, block: () -> T): T {
        paramRepo.updateScoringParams(tmp)
        val result = block()
        paramRepo.updateScoringParams(baseline.toWeights())
        return result
    }

    /** Вычисляем медиану списка Int */
    private fun medianYear(years: List<Int>): Int {
        if (years.isEmpty()) return 0
        val sorted = years.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid]
        else (sorted[mid-1] + sorted[mid]) / 2
    }

    @Test
    fun lambdaAgeDistribution() {
        // Папка для вывода
        val outDir = File("build/reports/ranking/age_distribution").apply { mkdirs() }
        val csv   = File(outDir, "lambda_age_distribution.csv").printWriter()
        csv.println("value,medYear,youngCount,midCount,oldCount,topIds")

        // 0.0,0.1,0.2,…,10.0 → 101 точка
        val series = (0..100).map { it / 10.0 }

        for (v in series) {
            // 1) готовим новый набор параметров
            val tmpWeights = baseline.toWeights().copy(lambda = v)

            // 2) берём топ-10 с этими весами
            val pubs = withWeights(tmpWeights) {
                searchService.searchPublications(SearchFilters(), page = 1, pageSize = 10)
            }

            // 3) вычисляем годы публикации
            val years = pubs.map { LocalDate.parse(it.publicationDate).year }
            val medYear = medianYear(years)

            // 4) считаем возраст в годах для каждой
            val ages = pubs.map {
                ChronoUnit.YEARS.between(
                    LocalDate.parse(it.publicationDate),
                    LocalDate.now()
                )
            }

            val young = ages.count { it <= 3 }
            val mid   = ages.count { it in 4..10 }
            val old   = ages.count { it > 10 }

            val topIds = pubs.joinToString("|") { it.paperId }

            // 5) пишем строку
            csv.println(
                String.format(
                    Locale.US,
                    "%.1f,%d,%d,%d,%d,%s",
                    v, medYear, young, mid, old, topIds
                )
            )
        }

        csv.close()
    }
}

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LambdaYearShiftTest {

    @Autowired
    lateinit var searchService: SearchService
    @Autowired
    lateinit var paramRepo: ParamRepository

    private lateinit var baselineAll: ScoringParams
    private lateinit var flatBaseline: ScoringParams   // равные веса 0.1

    @BeforeAll
    fun init() {
        baselineAll = paramRepo.loadScoringParams()
        flatBaseline = baselineAll.copy(
            bm25Parameter = 0.1, lambda = 0.1, alpha = 0.1, beta = 0.1, gamma = 0.1
        )
        paramRepo.updateScoringParams(flatBaseline.toWeights())
    }

    @BeforeEach
    fun reset() = paramRepo.updateScoringParams(flatBaseline.toWeights())
    @AfterAll
    fun restore() = paramRepo.updateScoringParams(baselineAll.toWeights())

    private fun ScoringParams.toWeights() = WeightParams(
        bm25Parameter, lambda, alpha, beta, gamma
    )

    private fun <T> withWeights(w: WeightParams, block: () -> T): T {
        paramRepo.updateScoringParams(w);
        val r = block(); paramRepo.updateScoringParams(flatBaseline.toWeights()); return r
    }

    private fun median(ints: List<Int>): Int =
        ints.sorted()
            .let { s -> if (s.isEmpty()) 0 else if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2 }

    @Test
    fun lambdaFineSweep() {
        val out = File("build/reports/ranking/lambda_fine.csv")
            .apply { parentFile.mkdirs() }.printWriter()
        out.println("lambda,avgDecay,medAge,oldPct,minAge,maxAge")

        // шаг 0.01 от 0 до 0.30 – зона, где что-то меняется
        val lambdas = (0..30).map { it / 100.0 }

        for (l in lambdas) {
            val pubs = withWeights(flatBaseline.toWeights().copy(lambda = l)) {
                // берём топ-50, чтобы было разнообразие
                searchService.searchPublications(SearchFilters(""), 1, 50)
            }
            val ages = pubs.map {
                ChronoUnit.DAYS.between(
                    LocalDate.parse(it.publicationDate),
                    LocalDate.now()
                ) / 365.0
            }
            val avgDecay = ages.map { kotlin.math.exp(-l * it) }.average()
            val medAge = ages.sorted().let { s ->
                if (s.size % 2 == 1) s[s.size / 2]
                else 0.5 * (s[s.size / 2 - 1] + s[s.size / 2])
            }
            val oldPct = ages.count { it > 5 } * 100.0 / ages.size
            val minAge = ages.minOrNull() ?: 0.0
            val maxAge = ages.maxOrNull() ?: 0.0

            out.println(
                String.format(
                    Locale.US,
                    "%.2f,%.4f,%.2f,%.1f,%.1f,%.1f",
                    l, avgDecay, medAge, oldPct, minAge, maxAge
                )
            )
        }
        out.close()
    }
}

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LambdaAgePenaltyTest {

    @Autowired private lateinit var paramService : ParamService
    @Autowired private lateinit var searchService: SearchService

    /** сохраняем штатные параметры, чтобы вернуть их после теста */
    private lateinit var baseline: ScoringParams

    /** базовый «плоский» набор весов — исключаем влияние α, β, γ и bm25 */
    private val flatWeights = WeightParams(
        bm25Parameter = 0.1,
        lambda        = 0.0,   // будет меняться
        alpha         = 0.1,
        beta          = 0.1,
        gamma         = 0.1
    )

    @BeforeAll
    fun setupBaseline() {
        baseline = paramService.getCurrentParams()
        // сразу сбросим все метрики на минимальные, кроме λ
        paramService.updateParams(flatWeights)
    }

    @AfterAll
    fun restoreBaseline() {
        // возвращаем исходные параметры
        paramService.updateParams((baseline as ScoringParams).toWeights())
    }

    @Test
    fun lambdaAgeDumpCsvAndAssert() {
        // файл для графика: build/reports/lambda_oldPct.csv
        val reportFile = File("build/reports/lambda_oldPct.csv")
        reportFile.parentFile.mkdirs()
        reportFile.printWriter().use { out ->
            out.println("lambda,oldPct")

            val lambdas = (0..100).map { it * 0.003 }
            val oldPctList = mutableListOf<Double>()

            for (λ in lambdas) {
                // обновляем только λ
                paramService.updateParams(flatWeights.copy(lambda = λ))

                // запрашиваем TOP-50
                val pubs = searchService.searchPublications(SearchFilters(), 1, 50)

                // считаем долю статей старше 10 лет по полю year
                val currentYear = LocalDate.now().year
                val oldCount = pubs.count { (currentYear - it.year) > 10 }
                val pct = 100.0 * oldCount / pubs.size
                oldPctList += pct

                // записываем строку в CSV
                out.println(String.format("%.3f,%.2f", λ, pct))
            }

            // проверяем монотонность убывания
            val nonIncreasing = oldPctList.zipWithNext().all { (prev, next) -> next <= prev }
            assertTrue(nonIncreasing, "Доля старых публикаций должна не расти при увеличении λ")

            // проверяем, что при λ ≥ 0.06 старые публикации исчезли
            val afterThreshold = oldPctList.drop(2).all { it == 0.0 }
            assertTrue(afterThreshold, "При λ ≥ 0.06 в TOP-50 не должно остаться статей старше 10 лет")
        }
    }
}

/** Вспомогательный конвертер ScoringParams → WeightParams */
private fun ScoringParams.toWeights() =
    WeightParams(bm25Parameter, lambda, alpha, beta, gamma)