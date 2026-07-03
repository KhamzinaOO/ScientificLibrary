package org.olgakhamzina.scientificlibrarythesis.server.integrationTests

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.olgakhamzina.scientificlibrarythesis.server.*
import org.olgakhamzina.scientificlibrarythesis.shared.Publication
import org.olgakhamzina.scientificlibrarythesis.shared.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.shared.SearchFilters
import org.olgakhamzina.scientificlibrarythesis.shared.WeightParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import org.springframework.transaction.annotation.Transactional
import org.tartarus.snowball.ext.EnglishStemmer
import org.tartarus.snowball.ext.RussianStemmer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.*

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RankingDataCollectionTest {

    @Autowired private lateinit var searchService: SearchService
    @Autowired private lateinit var paramService: ParamService
    @Autowired private lateinit var publicationRepository: PublicationRepository

    private lateinit var baseline: ScoringParams

    @BeforeAll
    fun loadBaseline() {
        baseline = paramService.getCurrentParams()
    }

    @AfterEach
    fun restoreBaseline() {
        paramService.updateParams(baseline.toWeights())
    }

    private val allPubs by lazy { publicationRepository.findAllPublications() }
    private val avgDocLen by lazy {
        allPubs.map { it.fullText.split("\\s+".toRegex()).size }.average()
    }
    private val totalDocs by lazy { allPubs.size.toDouble().coerceAtLeast(1.0) }

    private fun idf(term: String): Double {
        val count = allPubs.count { it.fullText.contains(term, ignoreCase = true) }
        return ln((totalDocs - count + 0.5) / (count + 0.5) + 1.0)
    }

    private fun ScoringParams.toWeights() = WeightParams(
        bm25Parameter, lambda, alpha, beta, gamma
    )

    private fun ScoringParams.asMap() = mapOf(
        "bm25parameter" to bm25Parameter,
        "lambda" to lambda,
        "alpha" to alpha,
        "beta" to beta,
        "gamma" to gamma,
        "maxLogHIndex" to maxLogHIndex,
        "maxLogCitations" to maxLogCitations,
        "maxLogInfluential" to maxLogInfluential
    )

    private fun WeightParams.toScoring(proto: ScoringParams) = mapOf(
        "bm25parameter" to bm25Parameter,
        "lambda" to lambda,
        "alpha" to alpha,
        "beta" to beta,
        "gamma" to gamma,
        "maxLogHIndex" to proto.maxLogHIndex,
        "maxLogCitations" to proto.maxLogCitations,
        "maxLogInfluential" to proto.maxLogInfluential
    )

    private fun computeScore(
        pub: Publication,
        params: Map<String, Double>,
        queryTerms: List<String>
    ): Double {
        val wBM25 = params["bm25parameter"] ?: 1.0
        val λ = params["lambda"] ?: 0.0
        val α = params["alpha"] ?: 0.0
        val β = params["beta"] ?: 0.0
        val γ = params["gamma"] ?: 0.0

        val text = (pub.title + " " + pub.abstractText).lowercase()
        val docLen = text.split("\\s+".toRegex()).size
        val k1 = 1.2; val b = 0.75
        var bm25 = 0.0
        for (term in queryTerms) {
            val tf = Regex("\\b${Regex.escape(term)}\\b").findAll(text).count()
            if (tf > 0) {
                val normTf = tf * (k1 + 1) / (tf + k1 * (1 - b + b * (docLen / avgDocLen)))
                bm25 += idf(term) * normTf
            }
        }
        bm25 *= wBM25

        val logH = ln(pub.avgHIndex + 1.0)
        val logC = ln(pub.citationCount + 1.0)
        val logI = ln(pub.influentialCitationCount + 1.0)
        val maxH = params["maxLogHIndex"] ?: 1.0
        val maxC = params["maxLogCitations"] ?: 1.0
        val maxI = params["maxLogInfluential"] ?: 1.0
        val nH = if (maxH > 0) logH / maxH else 0.0
        val nC = if (maxC > 0) logC / maxC else 0.0
        val nI = if (maxI > 0) logI / maxI else 0.0

        val age = (LocalDate.now().year - pub.year).toDouble()
        val metrics = α * nH + β * nC + γ * nI
        return bm25 + metrics * exp(-λ * age)
    }

    private fun dumpCsv(
        filename: String,
        base: List<Publication>,
        changed: List<Publication>,
        baseMap: Map<String, Double>,
        newMap: Map<String, Double>
    ) {
        val dir = File("build/reports/ranking").apply { mkdirs() }
        val file = File(dir, filename)
        file.printWriter().use { out ->
            out.println("paperId,title,baseRank,baseScore,newRank,newScore,rankShift")
            val ids = (base.map { it.paperId } + changed.map { it.paperId }).toSet()
            for (id in ids) {
                val title = (base + changed)
                    .find { it.paperId == id }?.title
                    ?.replace('"', '\'') ?: ""

                // determine ranks as strings
                val baseIdx = base.indexOfFirst { it.paperId == id }
                val brStr = if (baseIdx < 0) ">50" else (baseIdx + 1).toString()
                val newIdx = changed.indexOfFirst { it.paperId == id }
                val nrStr = if (newIdx < 0) ">50" else (newIdx + 1).toString()

                // compute shift only if both in range
                val shift = if (baseIdx >= 0 && newIdx >= 0) (baseIdx + 1) - (newIdx + 1) else 0

                val bs = baseMap[id] ?: 0.0
                val ns = newMap[id] ?: 0.0
                out.println("$id,\"$title\",$brStr,${"%.8f".format(bs)},$nrStr,${"%.8f".format(ns)},$shift")
            }
        }
    }

    private fun oneParamTest(
        filename: String,
        query: String = "",
        change: WeightParams.() -> WeightParams
    ) {
        val terms = if (query.isBlank()) emptyList() else query.split("\\s+".toRegex())
        val filters = SearchFilters(query = query)

        val baseRes = searchService.searchPublications(filters, 1, 50)
        val newWeights = baseline.toWeights().change()
        paramService.updateParams(newWeights)
        val newRes = searchService.searchPublications(filters, 1, 50)

        val baseMap = baseRes.associate { it.paperId to computeScore(it, baseline.asMap(), terms) }
        val newMap = newRes.associate { it.paperId to computeScore(it, newWeights.toScoring(baseline), terms) }

        dumpCsv(filename, baseRes, newRes, baseMap, newMap)
    }

    @Test fun bm25Variation() = oneParamTest("bm25_parameter_variation.csv") { copy(bm25Parameter = bm25Parameter * 5) }
    @Test fun lambdaVariation() = oneParamTest("lambda_parameter_variation.csv") { copy(lambda = lambda * 5) }
    @Test fun alphaVariation() = oneParamTest("alpha_parameter_variation.csv") { copy(alpha = alpha * 2) }
    @Test fun betaVariation() = oneParamTest("beta_parameter_variation.csv") { copy(beta = beta * 2) }
    @Test fun gammaVariation() = oneParamTest("gamma_parameter_variation.csv") { copy(gamma = gamma * 2) }

    @Test fun alphaBetaCombo() = oneParamTest("alpha_beta_combined.csv") { copy(alpha = alpha * 2, beta = beta * 2) }
    @Test fun alphaGammaCombo() = oneParamTest("alpha_gamma_combined.csv") { copy(alpha = alpha * 2, gamma = gamma * 2) }
    @Test fun betaGammaCombo() = oneParamTest("beta_gamma_combined.csv") { copy(beta = beta * 2, gamma = gamma * 2) }
    @Test fun allMetricsCombo() = oneParamTest(
        "all_metrics_emphasis.csv",
        query = "neural network"
    ) { copy(alpha = alpha * 2, beta = beta * 2, gamma = gamma * 2) }
    @Test fun textVsMetrics() = oneParamTest(
        "text_vs_metrics_variation.csv",
        query = "neural network"
    ) { copy(bm25Parameter = bm25Parameter * 3, alpha = alpha * 0.5, beta = beta * 0.5, gamma = gamma * 0.5) }
}


@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RankingPredictabilityTest {

    @Autowired lateinit var searchService: SearchService
    @Autowired lateinit var paramService : ParamService

    private lateinit var baseline: ScoringParams
    private fun restore() = paramService.updateParams(baseline.toWeights())

    @BeforeAll fun snap() { baseline = paramService.getCurrentParams() }
    @AfterEach fun back()  { restore() }

    /* ------------ маленькие help-функции ------------- */

    private fun ScoringParams.toWeights() = WeightParams(
        bm25Parameter, lambda, alpha, beta, gamma
    )

    private fun set(weight: WeightParams) = paramService.updateParams(weight)

    /* ------------------- Тесты ----------------------- */

    @Test
    fun bm25Increase_pushesRelevantPaperUp() {
        val q = "neural network"
        val base = searchService.searchPublications(SearchFilters(query=q),1,50)
        val topRelevant = base.maxByOrNull {
            val text = (it.title+" "+it.abstractText).lowercase()
            listOf("neural","network").sumOf { t ->
                Regex("\\b$t\\b").findAll(text).count()
            }
        }
        assertNotNull(topRelevant)

        set(baseline.toWeights().copy(bm25Parameter = baseline.bm25Parameter*5))
        val newRes = searchService.searchPublications(SearchFilters(query=q),1,50)

        val oldRank = base.indexOf(topRelevant)+1
        val newRank = newRes.indexOfFirst { it.paperId== topRelevant!!.paperId }.let { if(it<0) 999 else it+1 }

        assertTrue(newRank <= oldRank, "Релевантная статья должна подняться выше при росте BM25-веса")
    }

    @Test
    fun lambdaIncrease_pushesNewestUp() {
        val base = searchService.searchPublications(SearchFilters(),1,50)
        val newest = base.maxByOrNull { it.year }; assertNotNull(newest)

        set(baseline.toWeights().copy(lambda = baseline.lambda*5))
        val newRes = searchService.searchPublications(SearchFilters(),1,50)

        val old = base.indexOf(newest)+1
        val newR = newRes.indexOfFirst { it.paperId== newest!!.paperId }.let{if(it<0)999 else it+1}

        assertTrue(newR <= old, "Самая новая работа должна подняться при увеличении λ")
    }

    @Test
    fun alphaIncrease_pushesHighHindexUp() {
        val base = searchService.searchPublications(SearchFilters(),1,50)
        val topH = base.maxByOrNull { it.avgHIndex }; assertNotNull(topH)

        set(baseline.toWeights().copy(alpha = baseline.alpha*2))
        val newRes = searchService.searchPublications(SearchFilters(),1,50)

        val old = base.indexOf(topH)+1
        val newR = newRes.indexOfFirst { it.paperId== topH!!.paperId }.let{if(it<0)999 else it+1}

        assertTrue(newR <= old, "Публикация с высоким avgHIndex должна подняться при росте α")
    }

    @Test
    fun betaIncrease_pushesMostCitedUp() {
        val base = searchService.searchPublications(SearchFilters(),1,50)
        val mc = base.maxByOrNull { it.citationCount }; assertNotNull(mc)

        set(baseline.toWeights().copy(beta = baseline.beta*2))
        val newRes = searchService.searchPublications(SearchFilters(),1,50)

        val old = base.indexOf(mc)+1
        val newR = newRes.indexOfFirst { it.paperId== mc!!.paperId }.let{if(it<0)999 else it+1}

        assertTrue(newR <= old, "Самая цитируемая статья должна подняться при росте β")
    }

    @Test
    fun gammaIncrease_pushesInfluentialUp() {
        val base = searchService.searchPublications(SearchFilters(),1,50)
        val mi = base.maxByOrNull { it.influentialCitationCount }; assertNotNull(mi)

        set(baseline.toWeights().copy(gamma = baseline.gamma*2))
        val newRes = searchService.searchPublications(SearchFilters(),1,50)

        val old = base.indexOf(mi)+1
        val newR = newRes.indexOfFirst { it.paperId== mi!!.paperId }.let{if(it<0)999 else it+1}
        assertTrue(newR <= old, "Статья с max influential citations должна подняться при росте γ")
    }
}
