package org.olgakhamzina.scientificlibrarythesis.domain

import org.olgakhamzina.scientificlibrarythesis.data.PublicationDetail
import org.olgakhamzina.scientificlibrarythesis.service.DetailService
import org.olgakhamzina.scientificlibrarythesis.utill.NetworkError
import org.olgakhamzina.scientificlibrarythesis.utill.Result

class PublicationDetailRepositoryImpl(
    private val apiService: DetailService
): PublicationDetailRepository {
    override suspend fun getPublicationById(paperId: String): Result<PublicationDetail, NetworkError> {
            return apiService.getPublicationById(paperId)
    }
}