package org.olgakhamzina.scientificlibrarythesis.presentation.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.olgakhamzina.scientificlibrarythesis.data.Publication
import org.olgakhamzina.scientificlibrarythesis.data.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.datePicker.NullableDatePicker
import org.olgakhamzina.scientificlibrarythesis.presentation.detail.PublicationDetailContract
import org.olgakhamzina.scientificlibrarythesis.presentation.search.SearchContract.UiEvent
import org.olgakhamzina.scientificlibrarythesis.ui.Dimensions.LargePadding
import org.olgakhamzina.scientificlibrarythesis.ui.Dimensions.MediumPadding
import org.olgakhamzina.scientificlibrarythesis.ui.Dimensions.SmallPadding

@Composable
fun FullSearchScreen(
    onPublicationSelected: (String) -> Unit
) {
    val viewModel: SearchViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            if (effect is PublicationDetailContract.UiEffect.ShowError) {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    onClick = { focusManager.clearFocus() },
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(horizontal = LargePadding)
            ) {
                item {
                    Button(
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { viewModel.onEvent(UiEvent.AdjustParamsClicked) })
                    {
                        Text("Adjust Parameters")
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.onEvent(UiEvent.QueryChanged(it)) },
                        label = { Text("Search query") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(MediumPadding))
                }
                if (state.isAdvancedVisible) {
                    item {
                        AdvancedFilters(state = state, onEvent = viewModel::onEvent)
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { viewModel.onEvent(UiEvent.ToggleAdvancedSection) }) {
                            Text(if (state.isAdvancedVisible) "Hide Filters ▲" else "Show Filters ▼")
                        }
                        Button(onClick = { viewModel.onEvent(UiEvent.SearchClicked) }) {
                            Text("Search")
                        }
                    }
                }
                items(state.publications) { pub: Publication ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SmallPadding)
                            .clickable { onPublicationSelected(pub.paperId) },
                        elevation = CardDefaults.cardElevation(SmallPadding)
                    ) {
                        Column(Modifier.padding(LargePadding)) {
                            Text(pub.title, style = MaterialTheme.typography.titleLarge)
                            Text("Authors: ${pub.authors}")
                            Text("Venue: ${pub.venue}")
                            Text("Journal: ${pub.journal}")
                            Text("Year: ${pub.year}")
                            Text("Citations: ${pub.citationCount}")
                            Text("H-Index: ${pub.avgHIndex}")
                            Text("Score: ${pub.score}")
                        }
                    }
                }
                if (state.isLoading) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(LargePadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            LaunchedEffect(listState) {
                snapshotFlow {
                    listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                }.collect { idx ->
                    if (idx == state.publications.lastIndex && !state.isLoading) {
                        viewModel.onEvent(UiEvent.LoadMore)
                    }
                }
            }
        }
    }

    if (state.isParamsDialogOpen) {
        state.scoringParams?.let { params ->
            ScoringParamsDialog(
                params = params,
                onDismiss = { viewModel.onEvent(UiEvent.DismissParamsDialog) },
                onUpdateAll = { viewModel.onEvent(UiEvent.UpdateAllParams(it)) }
            )
        }
    }
}

@Composable
private fun AdvancedFilters(
    state: SearchContract.UiState,
    onEvent: (UiEvent) -> Unit
) {
    Column {
        OutlinedTextField(
            value = state.year,
            onValueChange = { onEvent(UiEvent.YearChanged(it)) },
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth()
        )
        AutocompleteTextField(
            label = "Author",
            query = state.authorQuery,
            onQueryChange = { onEvent(UiEvent.AuthorQueryChanged(it)) },
            suggestions = state.authorSuggestions,
            onSuggestionSelected = { onEvent(UiEvent.SuggestionSelected("author", it)) }
        )
        FilterChipsGrid(
            filterType = FilterType.AUTHOR,
            activeFilters = state.activeFilters,
            onRemoveFilter = { onEvent(UiEvent.RemoveFilter(it)) }
        )
        AutocompleteTextField(
            label = "Journal",
            query = state.journalQuery,
            onQueryChange = { onEvent(UiEvent.JournalQueryChanged(it)) },
            suggestions = state.journalSuggestions,
            onSuggestionSelected = { onEvent(UiEvent.SuggestionSelected("journal", it)) }
        )
        FilterChipsGrid(
            filterType = FilterType.JOURNAL,
            activeFilters = state.activeFilters,
            onRemoveFilter = { onEvent(UiEvent.RemoveFilter(it)) }
        )
        AutocompleteTextField(
            label = "Venue",
            query = state.venueQuery,
            onQueryChange = { onEvent(UiEvent.VenueQueryChanged(it)) },
            suggestions = state.venueSuggestions,
            onSuggestionSelected = { onEvent(UiEvent.SuggestionSelected("venue", it)) }
        )
        FilterChipsGrid(
            filterType = FilterType.VENUE,
            activeFilters = state.activeFilters,
            onRemoveFilter = { onEvent(UiEvent.RemoveFilter(it)) }
        )
        AutocompleteTextField(
            label = "Publication Type",
            query = state.pubTypeQuery,
            onQueryChange = { onEvent(UiEvent.PubTypeQueryChanged(it)) },
            suggestions = state.pubTypeSuggestions,
            onSuggestionSelected = { onEvent(UiEvent.SuggestionSelected("pubType", it)) }
        )
        FilterChipsGrid(
            filterType = FilterType.PUB_TYPE,
            activeFilters = state.activeFilters,
            onRemoveFilter = { onEvent(UiEvent.RemoveFilter(it)) }
        )
        Row {
            OutlinedTextField(
                value = state.hindexFrom,
                onValueChange = { onEvent(UiEvent.HIndexFromChanged(it)) },
                label = { Text("H-Index From") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = MediumPadding)
            )
            OutlinedTextField(
                value = state.hindexTo,
                onValueChange = { onEvent(UiEvent.HIndexToChanged(it)) },
                label = { Text("H-Index To") },
                modifier = Modifier.weight(1f)
            )
        }
        Row {
            OutlinedTextField(
                value = state.citationsFrom,
                onValueChange = { onEvent(UiEvent.CitationsFromChanged(it)) },
                label = { Text("Citations From") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = MediumPadding)
            )
            OutlinedTextField(
                value = state.citationsTo,
                onValueChange = { onEvent(UiEvent.CitationsToChanged(it)) },
                label = { Text("Citations To") },
                modifier = Modifier.weight(1f)
            )
        }
        Row {
            NullableDatePicker(
                selectedDate = state.dateFrom,
                onDateChange = { onEvent(UiEvent.DateFromChanged(it)) },
                labelText = "Date From",
                modifier = Modifier
                    .weight(1f)
                    .padding(end = MediumPadding)
            )
            NullableDatePicker(
                selectedDate = state.dateTo,
                onDateChange = { onEvent(UiEvent.DateToChanged(it)) },
                labelText = "Date To",
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.openAccess,
                onCheckedChange = { onEvent(UiEvent.OpenAccessToggled) }
            )
            Text("Open Access", fontSize = 14.sp)
        }
    }
}


@Composable
fun FilterChipsGrid(
    filterType: FilterType,
    activeFilters: List<ActiveFilter>,
    onRemoveFilter: (ActiveFilter) -> Unit
) {
    val filtersForType = activeFilters.filter { it.type == filterType }
    if (filtersForType.isEmpty()) return

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(horizontal = SmallPadding),
        horizontalArrangement = Arrangement.spacedBy(SmallPadding),
        verticalArrangement = Arrangement.spacedBy(SmallPadding),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp),
    ) {
        items(filtersForType) { filter ->
            Card(
                modifier = Modifier
                    .clickable { onRemoveFilter(filter) },
                elevation = CardDefaults.cardElevation(SmallPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = MediumPadding, vertical = SmallPadding)
                ) {
                    Text(
                        text = filter.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(SmallPadding))
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Remove"
                    )
                }
            }
        }
    }
}

@Composable
fun ScoringParamsDialog(
    params: ScoringParams,
    onDismiss: () -> Unit,
    onUpdateAll: (ScoringParams) -> Unit
) {
    var bm25 by remember { mutableStateOf(params.bm25parameter.toString()) }
    var lambda by remember { mutableStateOf(params.lambda.toString()) }
    var alpha by remember { mutableStateOf(params.alpha.toString()) }
    var beta by remember { mutableStateOf(params.beta.toString()) }
    var gamma by remember { mutableStateOf(params.gamma.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit All Scoring Parameters") },
        text = {
            Column {
                OutlinedTextField(
                    value = bm25,
                    onValueChange = { bm25 = it },
                    label = { Text("BM25 Parameter") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(MediumPadding))
                OutlinedTextField(
                    value = lambda,
                    onValueChange = { lambda = it },
                    label = { Text("Lambda") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(MediumPadding))
                OutlinedTextField(
                    value = alpha,
                    onValueChange = { alpha = it },
                    label = { Text("Alpha") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(MediumPadding))
                OutlinedTextField(
                    value = beta,
                    onValueChange = { beta = it },
                    label = { Text("Beta") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(MediumPadding))
                OutlinedTextField(
                    value = gamma,
                    onValueChange = { gamma = it },
                    label = { Text("Gamma") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newParams = ScoringParams(
                        bm25parameter = bm25.toDoubleOrNull() ?: params.bm25parameter,
                        lambda = lambda.toDoubleOrNull() ?: params.lambda,
                        alpha = alpha.toDoubleOrNull() ?: params.alpha,
                        beta = beta.toDoubleOrNull() ?: params.beta,
                        gamma = gamma.toDoubleOrNull() ?: params.gamma
                    )
                    onUpdateAll(newParams)
                    onDismiss()
                }
            ) {
                Text("Save All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteTextField(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionSelected(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}