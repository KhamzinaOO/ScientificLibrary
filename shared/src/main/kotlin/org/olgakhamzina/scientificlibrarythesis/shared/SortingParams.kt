package org.olgakhamzina.scientificlibrarythesis.shared

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoringParams(
    @SerialName("bm25parameter") @JsonProperty("bm25parameter")
    val bm25Parameter: Double,  // Коэффициент при компоненте BM25

    @SerialName("lambda") @JsonProperty("lambda")
    val lambda: Double,         // Параметр λ для экспоненциального затухания рейтинга по времени (возраст публикации)

    @SerialName("alpha") @JsonProperty("alpha")
    val alpha: Double,          // Параметр α – вес логарифма h-index авторов

    @SerialName("beta") @JsonProperty("beta")
    val beta: Double,           // Параметр β – вес логарифма общего числа цитирований

    @SerialName("gamma") @JsonProperty("gamma")
    val gamma: Double,      // Параметр γ – вес числа "влиятельных" цитирований

    @SerialName("maxLogHIndex")        val maxLogHIndex: Double,
    @SerialName("maxLogCitations")     val maxLogCitations: Double,
    @SerialName("maxLogInfluential")   val maxLogInfluential: Double
)

data class WeightParams(
    @SerialName("bm25parameter") @JsonProperty("bm25parameter")
    val bm25Parameter: Double,  // Коэффициент при компоненте BM25

    @SerialName("lambda") @JsonProperty("lambda")
    val lambda: Double,         // Параметр λ для экспоненциального затухания рейтинга по времени (возраст публикации)

    @SerialName("alpha") @JsonProperty("alpha")
    val alpha: Double,          // Параметр α – вес логарифма h-index авторов

    @SerialName("beta") @JsonProperty("beta")
    val beta: Double,           // Параметр β – вес логарифма общего числа цитирований

    @SerialName("gamma") @JsonProperty("gamma")
    val gamma: Double,      // Параметр γ – вес числа "влиятельных" цитирований
)

/**
 * SearchFilters – модель параметров фильтрации поиска.
 * Используется для передачи критериев фильтрации как с клиента на сервер (в запросах API),
 * так и внутри клиента при хранении состояния фильтров.
 */
@Serializable
data class SearchFilters(
    @SerialName("query") @JsonProperty("query")
    val query: String = "",              // Строка поискового запроса

    @SerialName("year") @JsonProperty("year")
    val year: Int? = null,               // Год публикации для фильтрации (точное совпадение)

    @SerialName("authors") @JsonProperty("authors")
    val authors: List<String>? = null,   // Список авторов для фильтрации (по имени)

    @SerialName("journals") @JsonProperty("journals")
    val journals: List<String>? = null,  // Список журналов для фильтрации

    @SerialName("venues") @JsonProperty("venues")
    val venues: List<String>? = null,    // Список конференций/мест публикации для фильтрации

    @SerialName("pubTypes") @JsonProperty("pubTypes")
    val pubTypes: List<String>? = null,  // Список типов публикаций для фильтрации

    @SerialName("hindexFrom") @JsonProperty("hindexFrom")
    val hindexFrom: Double? = null,      // Нижний порог h-index авторов

    @SerialName("hindexTo") @JsonProperty("hindexTo")
    val hindexTo: Double? = null,        // Верхний порог h-index

    @SerialName("citationsFrom") @JsonProperty("citationsFrom")
    val citationsFrom: Int? = null,      // Нижний порог числа цитирований

    @SerialName("citationsTo") @JsonProperty("citationsTo")
    val citationsTo: Int? = null,        // Верхний порог числа цитирований

    @SerialName("dateFrom") @JsonProperty("dateFrom")
    val dateFrom: String? = null,        // Начало диапазона дат публикации (YYYY-MM-DD)

    @SerialName("dateTo") @JsonProperty("dateTo")
    val dateTo: String? = null,          // Конец диапазона дат публикации

    @SerialName("openAccess") @JsonProperty("openAccess")
    val openAccess: Boolean? = null      // Флаг фильтрации только открытого доступа (true/false)
)