package org.olgakhamzina.scientificlibrarythesis.presentation.detail

import org.olgakhamzina.scientificlibrarythesis.data.PublicationDetail

object PublicationDetailContract {

    data class UiState(
        val isLoading: Boolean = false,
        val publication: PublicationDetail? = null,
        val errorMessage: String? = null
    )

    sealed class UiEvent {
        data class LoadPublication(val paperId: String) : UiEvent()
    }

    sealed class UiEffect {
        data class ShowError(val message: String) : UiEffect()
    }

}