package org.olgakhamzina.scientificlibrarythesis.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destination {
    @Serializable
    object Search : Destination()

    @Serializable
    data class PublicationDetail (
        val paperId: String
    )  : Destination()
}