package org.olgakhamzina.scientificlibrarythesis.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FacetsResponse(
    @SerialName("fields") val fields: List<String>,
    @SerialName("combinedFields") val combinedFields: List<String>,
    @SerialName("venues") val venues: List<String>,
    @SerialName("journals") val journals: List<String>,
    @SerialName("publicationTypes") val publicationTypes: List<String>,
    @SerialName("years") val years: List<Int>
)