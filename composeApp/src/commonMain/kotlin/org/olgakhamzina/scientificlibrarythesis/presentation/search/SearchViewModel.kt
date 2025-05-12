package org.olgakhamzina.scientificlibrarythesis.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.olgakhamzina.scientificlibrarythesis.domain.PublicationRepository
import org.olgakhamzina.scientificlibrarythesis.presentation.detail.PublicationDetailContract.UiEffect
import org.olgakhamzina.scientificlibrarythesis.presentation.search.SearchContract.UiEvent
import org.olgakhamzina.scientificlibrarythesis.presentation.search.SearchContract.UiState
import org.olgakhamzina.scientificlibrarythesis.utill.Result

enum class FilterType {
    AUTHOR, JOURNAL, VENUE, PUB_TYPE
}

data class ActiveFilter(
    val type: FilterType,
    val value: String
)

class SearchViewModel(
    private val repository: PublicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var suggestionsJob: Job? = null
    private var searchJob: Job? = null
    private var paramsJob: Job? = null

    private var currentPage = 1
    private var isLastPage = false
    private var lastFilters: SearchFilters? = null

    private data class SearchFilters(
        val query: String,
        val year: Int?,
        val venues: List<String>?,
        val authors: List<String>?,
        val journals: List<String>?,
        val pubTypes: List<String>?,
        val hindexFrom: Double?,
        val hindexTo: Double?,
        val citationsFrom: Int?,
        val citationsTo: Int?,
        val dateFrom: String?,
        val dateTo: String?,
        val openAccess: Boolean?
    )

    private val _effect = MutableSharedFlow<UiEffect>()
    val effect: SharedFlow<UiEffect> = _effect.asSharedFlow()

    fun onEvent(event: UiEvent) {
        when (event) {
            UiEvent.ToggleAdvancedSection ->
                _uiState.update { it.copy(isAdvancedVisible = !it.isAdvancedVisible) }

            is UiEvent.QueryChanged -> {
                _uiState.update { it.copy(query = event.query) }
                suggestionsJob = viewModelScope.launch(Dispatchers.IO) {
                    requestSuggestionsInternal("general", event.query)
                }
            }

            is UiEvent.AuthorQueryChanged -> {
                _uiState.update { it.copy(authorQuery = event.query) }
                suggestionsJob?.cancel()
                suggestionsJob = viewModelScope.launch(Dispatchers.IO) {
                    requestSuggestionsInternal("author", event.query)
                }
            }

            is UiEvent.JournalQueryChanged -> {
                _uiState.update { it.copy(journalQuery = event.query) }
                suggestionsJob?.cancel()
                suggestionsJob = viewModelScope.launch(Dispatchers.IO) {
                    requestSuggestionsInternal("journal", event.query)
                }
            }

            is UiEvent.VenueQueryChanged -> {
                _uiState.update { it.copy(venueQuery = event.query) }
                suggestionsJob?.cancel()
                suggestionsJob = viewModelScope.launch(Dispatchers.IO) {
                    requestSuggestionsInternal("venue", event.query)
                }
            }

            is UiEvent.PubTypeQueryChanged -> {
                _uiState.update { it.copy(pubTypeQuery = event.query) }
                suggestionsJob?.cancel()
                suggestionsJob = viewModelScope.launch(Dispatchers.IO) {
                    requestSuggestionsInternal("pubType", event.query)
                }
            }

            is UiEvent.YearChanged ->
                _uiState.update { it.copy(year = event.year) }

            is UiEvent.HIndexFromChanged ->
                _uiState.update { it.copy(hindexFrom = event.value) }

            is UiEvent.HIndexToChanged ->
                _uiState.update { it.copy(hindexTo = event.value) }

            is UiEvent.CitationsFromChanged ->
                _uiState.update { it.copy(citationsFrom = event.value) }

            is UiEvent.CitationsToChanged ->
                _uiState.update { it.copy(citationsTo = event.value) }

            UiEvent.OpenAccessToggled ->
                _uiState.update { it.copy(openAccess = !it.openAccess) }

            is UiEvent.DateFromChanged ->
                _uiState.update { it.copy(dateFrom = event.date) }

            is UiEvent.DateToChanged ->
                _uiState.update { it.copy(dateTo = event.date) }

            UiEvent.SearchClicked -> {

                val state = uiState.value
                currentPage = 1
                isLastPage = false
                _uiState.update { it.copy(publications = emptyList(), errorMessage = null) }

                lastFilters = buildFilters(state)

                searchJob?.cancel()
                searchJob = viewModelScope.launch(Dispatchers.IO) {
                    loadMoreInternal()
                }
            }

            UiEvent.LoadMore -> {
                if (_uiState.value.isLoading || isLastPage) return
                searchJob?.cancel()
                searchJob = viewModelScope.launch(Dispatchers.IO) {
                    loadMoreInternal()
                }
            }

            is UiEvent.AddFilter ->
                _uiState.update { it.copy(activeFilters = it.activeFilters + event.filter) }

            is UiEvent.RemoveFilter ->
                _uiState.update { it.copy(activeFilters = it.activeFilters - event.filter) }

            is UiEvent.SuggestionSelected -> {
                _uiState.update { state ->
                    when (event.type) {
                        "author"  -> state.copy(
                            activeFilters = state.activeFilters + ActiveFilter(FilterType.AUTHOR, event.value),
                            authorQuery = ""
                        )
                        "journal" -> state.copy(
                            activeFilters = state.activeFilters + ActiveFilter(FilterType.JOURNAL, event.value),
                            journalQuery = ""
                        )
                        "venue"   -> state.copy(
                            activeFilters = state.activeFilters + ActiveFilter(FilterType.VENUE, event.value),
                            venueQuery = ""
                        )
                        "pubType" -> state.copy(
                            activeFilters = state.activeFilters + ActiveFilter(FilterType.PUB_TYPE, event.value),
                            pubTypeQuery = ""
                        )
                        else      -> state
                    }
                }
            }

            UiEvent.AdjustParamsClicked -> {
                _uiState.update { it.copy(isParamsDialogOpen = true) }
                paramsJob?.cancel()
                paramsJob = viewModelScope.launch(Dispatchers.IO) {
                    loadParamsInternal()
                }
            }

            UiEvent.DismissParamsDialog ->
                _uiState.update { it.copy(isParamsDialogOpen = false) }

            is UiEvent.UpdateAllParams -> {

                paramsJob?.cancel()
                paramsJob = viewModelScope.launch(Dispatchers.IO) {
                    when (val res = repository.updateAllScoringParams(event.params)) {
                        is Result.Success -> loadParamsInternal()
                        is Result.Error   ->
                            _uiState.update { it.copy(scoringError = res.error.message) }
                    }
                }
                _uiState.update { it.copy(isParamsDialogOpen = false) }
            }
        }
    }

    private suspend fun loadMoreInternal() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val filters = lastFilters ?: return
        val result = repository.searchPublications(
            query         = filters.query,
            page          = currentPage,
            pageSize      = 10,
            year          = filters.year,
            venues        = filters.venues,
            authors       = filters.authors,
            journals      = filters.journals,
            pubTypes      = filters.pubTypes,
            hindexFrom    = filters.hindexFrom,
            hindexTo      = filters.hindexTo,
            citationsFrom = filters.citationsFrom,
            citationsTo   = filters.citationsTo,
            dateFrom      = filters.dateFrom,
            dateTo        = filters.dateTo,
            openAccess    = filters.openAccess
        )
        withContext(Dispatchers.Main) {
            when (result) {
                is Result.Success -> {
                    val newList = result.data.orEmpty()
                    if (newList.isEmpty()) isLastPage = true
                    _uiState.update { it.copy(publications = it.publications + newList) }
                    currentPage++
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                    _effect.emit(UiEffect.ShowError(result.error.message)
                    )
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun requestSuggestionsInternal(type: String, query: String) {
        if (query.isBlank()) {
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    when (type) {
                        "author"  -> state.copy(authorSuggestions = emptyList())
                        "journal" -> state.copy(journalSuggestions = emptyList())
                        "venue"   -> state.copy(venueSuggestions = emptyList())
                        "pubType" -> state.copy(pubTypeSuggestions = emptyList())
                        else      -> state
                    }
                }
            }
            return
        }

        val res = repository.getSuggestions(type, query)
        withContext(Dispatchers.Main) {
            if (res is Result.Success) {
                _uiState.update { state ->
                    when (type) {
                        "author"  -> state.copy(authorSuggestions = res.data.orEmpty())
                        "journal" -> state.copy(journalSuggestions = res.data.orEmpty())
                        "venue"   -> state.copy(venueSuggestions = res.data.orEmpty())
                        "pubType" -> state.copy(pubTypeSuggestions = res.data.orEmpty())
                        else      -> state
                    }
                }
            }
        }
    }

    private suspend fun loadParamsInternal() {
        val res = repository.getScoringParams()
        withContext(Dispatchers.Main) {
            if (res is Result.Success)
                _uiState.update { it.copy(scoringParams = res.data) }
            else{
                _uiState.update { it.copy(scoringError = "Failed to load parameters") }
            }
        }
    }

    private fun buildFilters(state: UiState): SearchFilters = SearchFilters(
        query         = state.query,
        year          = state.year.toIntOrNull(),
        venues        = state.activeFilters.filter { it.type == FilterType.VENUE }.map { it.value }
            .ifEmpty { state.venueQuery.takeIf(String::isNotBlank)?.let(::listOf) },
        authors       = state.activeFilters.filter { it.type == FilterType.AUTHOR }.map { it.value }
            .ifEmpty { state.authorQuery.takeIf(String::isNotBlank)?.let(::listOf) },
        journals      = state.activeFilters.filter { it.type == FilterType.JOURNAL }.map { it.value }
            .ifEmpty { state.journalQuery.takeIf(String::isNotBlank)?.let(::listOf) },
        pubTypes      = state.activeFilters.filter { it.type == FilterType.PUB_TYPE }.map { it.value }
            .ifEmpty { state.pubTypeQuery.takeIf(String::isNotBlank)?.let(::listOf) },
        hindexFrom    = state.hindexFrom.toDoubleOrNull(),
        hindexTo      = state.hindexTo.toDoubleOrNull(),
        citationsFrom = state.citationsFrom.toIntOrNull(),
        citationsTo   = state.citationsTo.toIntOrNull(),
        dateFrom      = state.dateFrom?.toString(),
        dateTo        = state.dateTo?.toString(),
        openAccess    = if (state.openAccess) state.openAccess else null
    )
}

