package org.olgakhamzina.scientificlibrarythesis.presentation.search

import kotlinx.datetime.LocalDate
import org.olgakhamzina.scientificlibrarythesis.data.Publication
import org.olgakhamzina.scientificlibrarythesis.data.ScoringParams

object SearchContract {
    data class UiState(
        val query: String = "",
        val isAdvancedVisible: Boolean = false,
        val year: String = "",
        val authorQuery: String = "",
        val journalQuery: String = "",
        val venueQuery: String = "",
        val pubTypeQuery: String = "",
        val hindexFrom: String = "",
        val hindexTo: String = "",
        val citationsFrom: String = "",
        val citationsTo: String = "",
        val openAccess: Boolean = false,
        val dateFrom: LocalDate? = null,
        val dateTo: LocalDate? = null,
        val publications: List<Publication> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val activeFilters: List<ActiveFilter> = emptyList(),
        val authorSuggestions: List<String> = emptyList(),
        val journalSuggestions: List<String> = emptyList(),
        val venueSuggestions: List<String> = emptyList(),
        val pubTypeSuggestions: List<String> = emptyList(),
        val isParamsDialogOpen: Boolean = false,
        val scoringParams: ScoringParams? = null,
        val scoringError: String? = null
    )

    sealed interface UiEvent {
        object ToggleAdvancedSection : UiEvent
        data class QueryChanged(val query: String) : UiEvent
        data class YearChanged(val year: String) : UiEvent
        data class AuthorQueryChanged(val query: String) : UiEvent
        data class JournalQueryChanged(val query: String) : UiEvent
        data class VenueQueryChanged(val query: String) : UiEvent
        data class PubTypeQueryChanged(val query: String) : UiEvent
        data class HIndexFromChanged(val value: String) : UiEvent
        data class HIndexToChanged(val value: String) : UiEvent
        data class CitationsFromChanged(val value: String) : UiEvent
        data class CitationsToChanged(val value: String) : UiEvent
        object OpenAccessToggled : UiEvent
        data class DateFromChanged(val date: LocalDate?) : UiEvent
        data class DateToChanged(val date: LocalDate?) : UiEvent
        object SearchClicked : UiEvent
        object LoadMore : UiEvent
        data class AddFilter(val filter: ActiveFilter) : UiEvent
        data class RemoveFilter(val filter: ActiveFilter) : UiEvent
        data class SuggestionSelected(val type: String, val value: String) : UiEvent
        object AdjustParamsClicked : UiEvent
        object DismissParamsDialog : UiEvent
        data class UpdateAllParams(val params: ScoringParams) : UiEvent
    }
}