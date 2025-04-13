package org.olgakhamzina.scientificlibrarythesis.data

import kotlinx.serialization.Serializable

@Serializable
data class PublicationDetail(
    val paperId: String? = null,
    val title: String? = null,
    val abstract: String? = null,
    val venue: String? = null,
    val journal: String? = null,
    val year: Int? = null,
    val publicationDate: String? = null,
    val citationCount: Int? = null,
    val influentialCitationCount: Int? = null,
    val avgHIndex: Double? = null,
    val authors: String? = null,
    val fields: List<String>? = null,
    val f2Fields: List<String>? = null,
    val publicationTypes: List<String>? = null,
    val tldr: String? = null,
    val isOpenAccess: Boolean? = null,
    val openAccessPdfUrl : String? = null,
    val score: Double? = null,
)