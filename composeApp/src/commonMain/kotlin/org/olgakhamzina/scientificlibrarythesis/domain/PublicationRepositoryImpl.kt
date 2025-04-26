package org.olgakhamzina.scientificlibrarythesis.domain

import org.olgakhamzina.scientificlibrarythesis.data.Publication
import org.olgakhamzina.scientificlibrarythesis.data.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.service.ResultService
import org.olgakhamzina.scientificlibrarythesis.utill.NetworkError
import org.olgakhamzina.scientificlibrarythesis.utill.Result

class PublicationRepositoryImpl(
    private val apiClient: ResultService
) : PublicationRepository {

    override suspend fun searchPublications(
        query: String,
        page: Int,
        pageSize: Int,
        year: Int?,
        fields: List<String>?,
        venues: List<String>?,
        authors: List<String>?,
        journals: List<String>?,
        pubTypes: List<String>?,
        affiliation: String?,
        dateFrom: String?, //yyyy-MM-dd
        dateTo: String?,
        citationsFrom: Int?,
        citationsTo: Int?,
        openAccess: Boolean?,
        hindexFrom: Double?,
        hindexTo: Double?,
    ): Result<List<Publication>?, NetworkError> {
        return apiClient.searchPublications(
            query,
            page,
            pageSize,
            year,
            fields,
            venues,
            authors,
            journals,
            pubTypes,
            affiliation,
            dateFrom,
            dateTo,
            citationsFrom,
            citationsTo,
            openAccess,
            hindexFrom,
            hindexTo,
        )
    }

    override suspend fun getScoringParams(): Result<ScoringParams, NetworkError> {
        return apiClient.getScoringParams()
    }

    override suspend fun updateAllScoringParams(params: ScoringParams): Result<Unit, NetworkError> {
        return apiClient.updateScoringParams(params)
    }

    override suspend fun getSuggestions(type: String, query: String): Result<List<String>?, NetworkError> {
        return apiClient.getSuggestions(type, query)
    }
}