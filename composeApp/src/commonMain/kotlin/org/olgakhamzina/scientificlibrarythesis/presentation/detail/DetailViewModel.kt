package org.olgakhamzina.scientificlibrarythesis.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.olgakhamzina.scientificlibrarythesis.domain.PublicationDetailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.olgakhamzina.scientificlibrarythesis.presentation.detail.PublicationDetailContract.UiEffect
import org.olgakhamzina.scientificlibrarythesis.presentation.detail.PublicationDetailContract.UiEvent
import org.olgakhamzina.scientificlibrarythesis.presentation.detail.PublicationDetailContract.UiState
import org.olgakhamzina.scientificlibrarythesis.utill.Result

class PublicationDetailViewModel(
    private val repository: PublicationDetailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<UiEffect>()
    val effect: SharedFlow<UiEffect> = _effect.asSharedFlow()

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.LoadPublication -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                viewModelScope.launch(Dispatchers.IO) {
                    when (val res = repository.getPublicationById(event.paperId)) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    publication = res.data
                                )
                            }
                        }
                        is Result.Error -> {
                            val msg = res.error.message.ifBlank { "Unknown error" }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = msg
                                )
                            }
                            _effect.emit(UiEffect.ShowError(msg))
                        }
                    }
                }
            }
        }
    }
}