package org.olgakhamzina.scientificlibrarythesis.domain

import org.olgakhamzina.scientificlibrarythesis.data.Publication
import org.olgakhamzina.scientificlibrarythesis.data.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.utill.NetworkError
import org.olgakhamzina.scientificlibrarythesis.utill.Result

interface PublicationRepository {
    suspend fun searchPublications(
        query: String,
        page: Int = 1,
        pageSize: Int = 10,
        year: Int? = null,
        fields: List<String>? = null,
        // Change these from String? to List<String>? to support multiple values:
        venues: List<String>? = null,
        authors: List<String>? = null,
        journals: List<String>? = null,
        pubTypes: List<String>? = null,
        // affiliation is left as a single string (if it is meant to be a free text term)
        affiliation: String? = null,
        dateFrom: String? = null, // format: yyyy-MM-dd
        dateTo: String? = null,   // format: yyyy-MM-dd
        citationsFrom: Int? = null,
        citationsTo: Int? = null,
        openAccess: Boolean? = null,
        hindexFrom: Double? = null,
        hindexTo: Double? = null,
    ): Result<List<Publication>?, NetworkError>
    suspend fun getScoringParams(): Result<ScoringParams, NetworkError>
    suspend fun updateAllScoringParams(params: ScoringParams): Result<Unit, NetworkError>
    suspend fun getSuggestions(type: String, query: String): Result<List<String>?, NetworkError>
}
