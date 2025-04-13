package org.olgakhamzina.scientificlibrarythesis.domain

import org.olgakhamzina.scientificlibrarythesis.data.PublicationDetail
import org.olgakhamzina.scientificlibrarythesis.utill.NetworkError
import org.olgakhamzina.scientificlibrarythesis.utill.Result

interface PublicationDetailRepository {
    suspend fun getPublicationById(paperId: String): Result<PublicationDetail, NetworkError>
}