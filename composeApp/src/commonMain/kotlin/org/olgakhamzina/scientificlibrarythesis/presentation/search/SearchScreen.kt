package org.olgakhamzina.scientificlibrarythesis.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.olgakhamzina.scientificlibrarythesis.data.ScoringParams
import org.olgakhamzina.scientificlibrarythesis.datePicker.NullableDatePicker
import org.olgakhamzina.scientificlibrarythesis.datePicker.formatDate

@Composable
fun FullSearchScreen(
    onPublicationSelected: (String) -> Unit
) {
    val viewModel: SearchViewModel = koinViewModel()
    val publications by viewModel.publications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()


    var query by remember { mutableStateOf("") }
    var isAdvancedVisible by remember { mutableStateOf(false) }

    //TODO: provide through viewmodel
    var year by remember { mutableStateOf("") }
    var hindexFrom by remember { mutableStateOf("") }
    var hindexTo by remember { mutableStateOf("") }
    var citationsFrom by remember { mutableStateOf("") }
    var citationsTo by remember { mutableStateOf("") }
    var openAccess by remember { mutableStateOf(false) }

    var authorQuery by remember { mutableStateOf("") }
    val authorSuggestions by viewModel.authorSuggestions.collectAsState()
    var journalQuery by remember { mutableStateOf("") }
    val journalSuggestions by viewModel.journalSuggestions.collectAsState()
    var venueQuery by remember { mutableStateOf("") }
    val venueSuggestions by viewModel.venueSuggestions.collectAsState()
    var pubTypeQuery by remember { mutableStateOf("") }
    val pubTypeSuggestions by viewModel.pubTypeSuggestions.collectAsState()

    val showDialog = remember { mutableStateOf(false) }
    val scoringParams by viewModel.params.collectAsState()
    val scoringError by viewModel.error.collectAsState()

    var dateFrom: LocalDate? by remember { mutableStateOf(null) }
    var dateTo: LocalDate? by remember { mutableStateOf(null) }

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = { focusManager.clearFocus() },
                    interactionSource = MutableInteractionSource(),
                    indication = null
                )
                .padding( horizontal = 16.dp)
        ) {
            item {
                Button(onClick = {
                    viewModel.loadParams()
                    showDialog.value = true
                }) {
                    Text("Adjust Parameters")
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search query") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                if (isAdvancedVisible) {
                    Column {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Year") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        AutocompleteTextField(
                            label = "Author",
                            query = authorQuery,
                            onQueryChange = { authorQuery = it; viewModel.fetchSuggestions("author", it) },
                            suggestions = authorSuggestions ?: emptyList(),
                            onSuggestionSelected = { selected ->
                                viewModel.addFilter(ActiveFilter(FilterType.AUTHOR, selected))
                                authorQuery = ""
                            }
                        )

                        FilterChipsRow(
                            filterType = FilterType.AUTHOR,
                            activeFilters = viewModel.activeFilters,
                            onRemoveFilter = { viewModel.removeFilter(it) }
                        )

                        AutocompleteTextField(
                            label = "Journal",
                            query = journalQuery,
                            onQueryChange = { journalQuery = it; viewModel.fetchSuggestions("journal", it) },
                            suggestions = journalSuggestions ?: emptyList(),
                            onSuggestionSelected = { selected ->
                                viewModel.addFilter(ActiveFilter(FilterType.JOURNAL, selected))
                                journalQuery = ""
                            }
                        )

                        FilterChipsRow(
                            filterType = FilterType.JOURNAL,
                            activeFilters = viewModel.activeFilters,
                            onRemoveFilter = { viewModel.removeFilter(it) }
                        )

                        AutocompleteTextField(
                            label = "Venue",
                            query = venueQuery,
                            onQueryChange = { venueQuery = it; viewModel.fetchSuggestions("venue", it) },
                            suggestions = venueSuggestions ?: emptyList(),
                            onSuggestionSelected = { selected ->
                                viewModel.addFilter(ActiveFilter(FilterType.VENUE, selected))
                                venueQuery = ""
                            }
                        )

                        FilterChipsRow(
                            filterType = FilterType.VENUE,
                            activeFilters = viewModel.activeFilters,
                            onRemoveFilter = { viewModel.removeFilter(it) }
                        )

                        AutocompleteTextField(
                            label = "Publication Type",
                            query = pubTypeQuery,
                            onQueryChange = { pubTypeQuery = it; viewModel.fetchSuggestions("pubType", it) },
                            suggestions = pubTypeSuggestions ?: emptyList(),
                            onSuggestionSelected = { selected ->
                                viewModel.addFilter(ActiveFilter(FilterType.PUB_TYPE, selected))
                                pubTypeQuery = ""
                            }
                        )

                        FilterChipsRow(
                            filterType = FilterType.PUB_TYPE,
                            activeFilters = viewModel.activeFilters,
                            onRemoveFilter = { viewModel.removeFilter(it) }
                        )
                        Row {
                            OutlinedTextField(
                                value = hindexFrom,
                                onValueChange = { hindexFrom = it },
                                label = { Text("H-Index From") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            OutlinedTextField(
                                value = hindexTo,
                                onValueChange = { hindexTo = it },
                                label = { Text("H-Index To") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row {
                            OutlinedTextField(
                                value = citationsFrom,
                                onValueChange = { citationsFrom = it },
                                label = { Text("Citations From") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            OutlinedTextField(
                                value = citationsTo,
                                onValueChange = { citationsTo = it },
                                label = { Text("Citations To") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row {
                            NullableDatePicker(
                                selectedDate = dateFrom,
                                onDateChange = { dateFrom = it },
                                labelText = "Date From",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            NullableDatePicker(
                                selectedDate = dateTo,
                                onDateChange = { dateTo = it },
                                labelText = "Date To",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = openAccess, onCheckedChange = { openAccess = it })
                            Text("Open Access", fontSize = 14.sp)
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { isAdvancedVisible = !isAdvancedVisible }) {
                        Text(if (isAdvancedVisible) "Hide Filters ▲" else "Show Filters ▼")
                    }
                    Button(onClick = {
                        viewModel.searchWithFilters(
                            query = query,
                            year = year.toIntOrNull(),
                            venueInput = venueQuery.takeIf { it.isNotBlank() },
                            authorInput = authorQuery.takeIf { it.isNotBlank() },
                            journalInput = journalQuery.takeIf { it.isNotBlank() },
                            pubTypeInput = pubTypeQuery.takeIf { it.isNotBlank() },
                            hindexFrom = hindexFrom.toDoubleOrNull(),
                            hindexTo = hindexTo.toDoubleOrNull(),
                            citationsFrom = citationsFrom.toIntOrNull(),
                            citationsTo = citationsTo.toIntOrNull(),
                            dateFrom = dateFrom?.formatDate(),
                            dateTo = dateTo?.formatDate(),
                            openAccess = if (openAccess) openAccess else null
                        )
                    }) {
                        Text("Search")
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
            }
            items(publications) { pub ->
                Card(
                    modifier = Modifier
                        .clickable(onClick = { onPublicationSelected(pub.paperId) })
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
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
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .collect { lastVisibleItemIndex ->
                    if (lastVisibleItemIndex == publications.lastIndex && !isLoading) {
                        viewModel.loadMore()
                    }
                }
        }
    }

    if (showDialog.value && scoringParams != null) {
        ScoringParamsDialog(
            params = scoringParams!!,
            onDismiss = { showDialog.value = false },
            onUpdateAll = { value -> viewModel.updateAllParams(value) }
        )
    }
}


@Composable
fun FilterChipsRow(
    filterType: FilterType,
    activeFilters: List<ActiveFilter>,
    onRemoveFilter: (ActiveFilter) -> Unit
) {
    val filtersForType = activeFilters.filter { it.type == filterType }
    if (filtersForType.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            filtersForType.forEach { filter ->
                Card(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clickable { onRemoveFilter(filter) },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = filter.value)
                        Spacer(modifier = Modifier.width(4.dp))

                        Icon(
                            imageVector = Icons.Default.Clear,
                            "Delete"
                        )
                    }
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
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = lambda,
                    onValueChange = { lambda = it },
                    label = { Text("Lambda") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = alpha,
                    onValueChange = { alpha = it },
                    label = { Text("Alpha") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = beta,
                    onValueChange = { beta = it },
                    label = { Text("Beta") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
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