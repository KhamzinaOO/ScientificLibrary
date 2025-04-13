package org.olgakhamzina.scientificlibrarythesis.service

import org.olgakhamzina.scientificlibrarythesis.data.Publication
import org.olgakhamzina.scientificlibrarythesis.data.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.network.ApiClient
import org.olgakhamzina.scientificlibrarythesis.utill.NetworkError
import org.olgakhamzina.scientificlibrarythesis.utill.Result

class ResultService (private val apiClient: ApiClient) {
    suspend fun searchPublications(
        query: String,
        page: Int = 1,
        pageSize: Int = 10,
        year: Int? = null,
        fields: List<String>? = null,
        // Change these from String? to List<String>? to support multiple values:
        venue: List<String>? = null,
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
    ): Result<List<Publication>?, NetworkError> {
        val queryParams = buildString {
            append("?query=${query.trim()}&page=$page&pageSize=$pageSize")
            year?.let { append("&year=$it") }
            fields?.takeIf { it.isNotEmpty() }?.let { append("&fields=${it.joinToString(",")}") }
            venue?.takeIf { it.isNotEmpty() }?.let { append("&venue=${it.joinToString(",")}") }
            authors?.takeIf { it.isNotEmpty() }?.let { append("&author=${it.joinToString(",")}") }
            journals?.takeIf { it.isNotEmpty() }?.let { append("&journal=${it.joinToString(",")}") }
            pubTypes?.takeIf { it.isNotEmpty() }?.let { append("&pubType=${it.joinToString(",")}") }
            affiliation?.let { append("&affiliation=$it") }
            dateFrom?.let { append("&dateFrom=$it") }
            dateTo?.let { append("&dateTo=$it") }
            citationsFrom?.let { append("&citationsFrom=$it") }
            citationsTo?.let { append("&citationsTo=$it") }
            openAccess?.let { append("&openAccess=$it") }
            hindexFrom?.let { append("&hindexFrom=$it") }
            hindexTo?.let { append("&hindexTo=$it") }
        }

        return apiClient.get<List<Publication>?>(endpoint = "/search$queryParams")
    }

    suspend fun getScoringParams(): Result<ScoringParams, NetworkError> {
        return apiClient.get("/params")
    }

    suspend fun updateScoringParams(params: ScoringParams): Result<Unit, NetworkError> {
        return apiClient.post(
            endpoint = "/params/update",
            body = mapOf(
                "bm25parameter" to params.bm25parameter,
                "lambda" to params.lambda,
                "alpha" to params.alpha,
                "beta" to params.beta,
                "gamma" to params.gamma
            )
        )
    }

    suspend fun getSuggestions(type: String, query: String): Result<List<String>?, NetworkError> {
        val encodedQuery = query.trim().replace(" ", "%20")
        return apiClient.get("/suggest?type=$type&query=$encodedQuery")
    }
}