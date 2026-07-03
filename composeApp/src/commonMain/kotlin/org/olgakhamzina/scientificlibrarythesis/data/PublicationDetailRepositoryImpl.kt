package org.olgakhamzina.scientificlibrarythesis.data

import org.olgakhamzina.scientificlibrarythesis.domain.PublicationDetailRepository
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