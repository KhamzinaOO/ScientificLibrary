package org.olgakhamzina.scientificlibrarythesis.shared

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Publication(
    @SerialName("paperId") @JsonProperty("paperId")
    val paperId: String,               // Уникальный идентификатор публикации

    @SerialName("title") @JsonProperty("title")
    val title: String,                 // Заголовок публикации

    @SerialName("abstract") @JsonProperty("abstract")
    val abstractText: String,          // Текст аннотации (abstract) публикации

    @SerialName("venue") @JsonProperty("venue")
    val venue: String,                 // Место публикации (конференция, сборник и т.д.)

    @SerialName("journal") @JsonProperty("journal")
    val journal: String,               // Название журнала (если публикация в журнале, иначе пустая строка)

    @SerialName("year") @JsonProperty("year")
    val year: Int,                     // Год публикации

    @SerialName("publicationDate") @JsonProperty("publicationDate")
    val publicationDate: String,       // Дата публикации в формате "YYYY-MM-DD"

    @SerialName("citationCount") @JsonProperty("citationCount")
    val citationCount: Int,            // Общее число цитирований

    @SerialName("influentialCitationCount") @JsonProperty("influentialCitationCount")
    val influentialCitationCount: Int, // Число "влиятельных" цитирований

    @SerialName("avgHIndex") @JsonProperty("avgHIndex")
    val avgHIndex: Double,             // Средний h-index авторов публикации

    @SerialName("authors") @JsonProperty("authors")
    val authors: String,               // Имена авторов одной строкой (через запятую)

    @SerialName("fields") @JsonProperty("fields")
    val fields: List<String>,          // Список областей исследований (Field of Study)

    @SerialName("f2Fields") @JsonProperty("f2Fields")
    val f2Fields: List<String>,        // Список вторичных областей/категорий (S2 Fields of Study)

    @SerialName("publicationTypes") @JsonProperty("publicationTypes")
    val publicationTypes: List<String>,// Список типов публикации

    @SerialName("tldr") @JsonProperty("tldr")
    val tldr: String,                  // Краткое резюме TLDR (если есть)

    @SerialName("isOpenAccess") @JsonProperty("isOpenAccess")
    val isOpenAccess: Boolean,         // Признак открытого доступа

    @SerialName("openAccessPdfUrl") @JsonProperty("openAccessPdfUrl")
    val openAccessPdfUrl: String = "", // URL PDF-файла (если открыт доступ; может быть пустой)

    @SerialName("score") @JsonProperty("score")
    var score: Double = 0.0            // Итоговый рейтинг (вычисляется при поиске на сервере)
) {
    @JsonIgnore
    var fullText: String = ""          // Полнотекстовый индексируемый контент (название, аннотация, авторы и пр.).
    // Используется только на сервере для расчёта BM25, не сериализуется в JSON ответа.

    @JsonIgnore
    var affiliations: String = ""      // Аффилиации авторов (для фильтрации по организациям на сервере).
    // Тоже не передается клиенту, используется в логике фильтрации при поиске.
}