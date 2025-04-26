package org.olgakhamzina.scientificlibrarythesis.presentation.search

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.olgakhamzina.scientificlibrarythesis.data.Publication
import org.olgakhamzina.scientificlibrarythesis.data.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.domain.PublicationRepository
import org.olgakhamzina.scientificlibrarythesis.utill.Result

enum class FilterType {
    AUTHOR, JOURNAL, VENUE, PUB_TYPE, YEAR
}

data class ActiveFilter(
    val type: FilterType,
    val value: String
)

class SearchViewModel(
    private val repository: PublicationRepository
) : ViewModel() {

    private val _publications = MutableStateFlow<List<Publication>>(emptyList())
    val publications: StateFlow<List<Publication>> = _publications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var currentPage = 1
    private var lastQuery = ""
    private var isLastPage = false
    private var lastFilters: SearchFilters? = null

    data class SearchFilters(
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

    private val _activeFilters = mutableStateListOf<ActiveFilter>()
    val activeFilters: List<ActiveFilter> get() = _activeFilters

    fun addFilter(filter: ActiveFilter) {
        if (_activeFilters.none { it.type == filter.type && it.value == filter.value }) {
            _activeFilters.add(filter)
        }
    }

    fun removeFilter(filter: ActiveFilter) {
        _activeFilters.remove(filter)
    }

    fun searchWithFilters(
        query: String,
        year: Int?,
        venueInput: String?,
        authorInput: String?,
        journalInput: String?,
        pubTypeInput: String?,
        hindexFrom: Double?,
        hindexTo: Double?,
        citationsFrom: Int?,
        citationsTo: Int?,
        dateFrom: String?,
        dateTo: String?,
        openAccess: Boolean?
    ) {
        lastQuery = query
        currentPage = 1
        isLastPage = false
        _publications.value = emptyList()

        val activeAuthors = _activeFilters.filter { it.type == FilterType.AUTHOR }.map { it.value }
            .ifEmpty { authorInput?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList() }
        val activeJournals = _activeFilters.filter { it.type == FilterType.JOURNAL }.map { it.value }
            .ifEmpty { journalInput?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList() }
        val activeVenues = _activeFilters.filter { it.type == FilterType.VENUE }.map { it.value }
            .ifEmpty { venueInput?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList() }
        val activePubTypes = _activeFilters.filter { it.type == FilterType.PUB_TYPE }.map { it.value }
            .ifEmpty { pubTypeInput?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList() }

        val filters = SearchFilters(
            query = query,
            year = year,
            venues = if (activeVenues.isNotEmpty()) activeVenues else null,
            authors = if (activeAuthors.isNotEmpty()) activeAuthors else null,
            journals = if (activeJournals.isNotEmpty()) activeJournals else null,
            pubTypes = if (activePubTypes.isNotEmpty()) activePubTypes else null,
            hindexFrom = hindexFrom,
            hindexTo = hindexTo,
            citationsFrom = citationsFrom,
            citationsTo = citationsTo,
            dateFrom = dateFrom,
            dateTo = dateTo,
            openAccess = openAccess
        )
        lastFilters = filters
        loadMore()
    }

    fun loadMore() {
        if (_isLoading.value || isLastPage || lastFilters == null) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val filters = lastFilters!!
            when (val result = repository.searchPublications(
                query = filters.query,
                page = currentPage,
                pageSize = 10,
                year = filters.year,
                venues = filters.venues,
                authors = filters.authors,
                journals = filters.journals,
                pubTypes = filters.pubTypes,
                hindexFrom = filters.hindexFrom,
                hindexTo = filters.hindexTo,
                citationsFrom = filters.citationsFrom,
                citationsTo = filters.citationsTo,
                dateFrom = filters.dateFrom,
                dateTo = filters.dateTo,
                openAccess = filters.openAccess
            )) {
                is Result.Success -> {
                    val newList = result.data ?: emptyList()
                    if (newList.isEmpty()) {
                        isLastPage = true
                    } else {
                        _publications.value += newList
                        currentPage++
                    }
                }
                is Result.Error -> _errorMessage.value = result.error.message
            }

            _isLoading.value = false
        }
    }

    private val _params = MutableStateFlow<ScoringParams?>(null)
    val params: StateFlow<ScoringParams?> = _params.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadParams() {
        viewModelScope.launch {
            when (val result = repository.getScoringParams()) {
                is Result.Success -> _params.value = result.data
                is Result.Error -> _error.value = "Failed to load parameters"
            }
        }
    }

    fun updateAllParams(params: ScoringParams) {
        viewModelScope.launch {
            when (val result = repository.updateAllScoringParams(params)) {
                is Result.Success -> loadParams()
                is Result.Error -> _error.value = "Failed to update parameters"
            }
        }
    }

    private val _authorSuggestions = MutableStateFlow<List<String>?>(emptyList())
    val authorSuggestions: StateFlow<List<String>?> = _authorSuggestions

    private val _journalSuggestions = MutableStateFlow<List<String>?>(emptyList())
    val journalSuggestions: StateFlow<List<String>?> = _journalSuggestions

    private val _venueSuggestions = MutableStateFlow<List<String>?>(emptyList())
    val venueSuggestions: StateFlow<List<String>?> = _venueSuggestions

    private val _pubTypeSuggestions = MutableStateFlow<List<String>?>(emptyList())
    val pubTypeSuggestions: StateFlow<List<String>?> = _pubTypeSuggestions

    private var fetchJob: Job? = null

    fun fetchSuggestions(type: String, query: String) {
        fetchJob?.cancel()

        if (query.isBlank()) {
            when (type) {
                "author" -> _authorSuggestions.value = emptyList()
                "journal" -> _journalSuggestions.value = emptyList()
                "venue" -> _venueSuggestions.value = emptyList()
                "pubType" -> _pubTypeSuggestions.value = emptyList()
            }
            return
        }

        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.getSuggestions(type, query)) {
                is Result.Success -> when (type) {
                    "author" -> _authorSuggestions.value = result.data
                    "journal" -> _journalSuggestions.value = result.data
                    "venue" -> _venueSuggestions.value = result.data
                    "pubType" -> _pubTypeSuggestions.value = result.data
                }
                is Result.Error -> {
                    when (type) {
                        "author" -> _authorSuggestions.value = emptyList()
                        "journal" -> _journalSuggestions.value = emptyList()
                        "venue" -> _venueSuggestions.value = emptyList()
                        "pubType" -> _pubTypeSuggestions.value = emptyList()
                    }
                }
            }
        }
    }
}
