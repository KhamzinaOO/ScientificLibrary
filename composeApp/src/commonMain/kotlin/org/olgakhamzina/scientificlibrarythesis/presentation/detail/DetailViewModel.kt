package org.olgakhamzina.scientificlibrarythesis.presentation.detail

import androidx.lifecycle.ViewModel
import org.olgakhamzina.scientificlibrarythesis.data.PublicationDetail
import org.olgakhamzina.scientificlibrarythesis.domain.PublicationDetailRepository
import org.olgakhamzina.scientificlibrarythesis.utill.onError
import org.olgakhamzina.scientificlibrarythesis.utill.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PublicationDetailViewModel(
    private val repository: PublicationDetailRepository
): ViewModel() {

    private val _publicationDetail = MutableStateFlow<PublicationDetail?>(null)
    val publicationDetail: StateFlow<PublicationDetail?> = _publicationDetail.asStateFlow()

    fun loadPublication(paperId: String) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = repository.getPublicationById(paperId)
                result.onSuccess { detail ->
                    _publicationDetail.value = detail
                }
                result.onError {

                }
            } catch (e: Exception) {
                // Handle error (e.g., set an error state)
                e.printStackTrace()
            }
        }
    }
}
