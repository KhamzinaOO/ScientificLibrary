package org.olgakhamzina.scientificlibrarythesis.service

import org.olgakhamzina.scientificlibrarythesis.data.PublicationDetail
import org.olgakhamzina.scientificlibrarythesis.network.ApiClient
import org.olgakhamzina.scientificlibrarythesis.utill.NetworkError
import org.olgakhamzina.scientificlibrarythesis.utill.Result

class DetailService (private val apiClient: ApiClient) {

    suspend fun getPublicationById(paperId: String): Result<PublicationDetail, NetworkError> {
        return apiClient.get(endpoint = "/publication/$paperId")
    }

}