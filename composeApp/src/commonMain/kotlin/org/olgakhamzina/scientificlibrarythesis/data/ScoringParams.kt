package org.olgakhamzina.scientificlibrarythesis.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoringParams(
    @SerialName("bm25parameter") val bm25parameter: Double,
    @SerialName("lambda") val lambda: Double,
    @SerialName("alpha") val alpha: Double,
    @SerialName("beta") val beta: Double,
    @SerialName("gamma") val gamma: Double
)

