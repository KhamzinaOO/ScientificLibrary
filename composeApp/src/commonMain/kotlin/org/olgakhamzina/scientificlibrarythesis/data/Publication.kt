package org.olgakhamzina.scientificlibrarythesis.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Publication(
    @SerialName("paperId") val paperId: String,
    @SerialName("title") val title: String,
    @SerialName("abstract") val abstract: String,
    @SerialName("venue") val venue: String,
    @SerialName("journal") val journal: String,
    @SerialName("year") val year: Int,
    @SerialName("publicationDate") val publicationDate: String,
    @SerialName("citationCount") val citationCount: Int,
    @SerialName("influentialCitationCount") val influentialCitationCount: Int,
    @SerialName("avgHIndex") val avgHIndex: Double,
    @SerialName("authors") val authors: String,
    @SerialName("fields") val fields: List<String>,
    @SerialName("f2Fields") val f2Fields: List<String>,
    @SerialName("publicationTypes") val publicationTypes: List<String>,
    @SerialName("tldr") val tldr: String,
    @SerialName("isOpenAccess") val isOpenAccess: Boolean,
    @SerialName("score") val score: Double
)