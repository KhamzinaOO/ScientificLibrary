package org.olgakhamzina.scientificlibrarythesis.presentation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.olgakhamzina.scientificlibrarythesis.data.PublicationDetail
import org.olgakhamzina.scientificlibrarythesis.ui.Dimensions.LargePadding
import org.olgakhamzina.scientificlibrarythesis.ui.Dimensions.MediumPadding
import org.olgakhamzina.scientificlibrarythesis.ui.Dimensions.SmallPadding

@Composable
fun PublicationDetailScreen(
    paperId: String,
    onBack: () -> Unit
) {
    val viewModel: PublicationDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            if (effect is PublicationDetailContract.UiEffect.ShowError) {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(paperId) {
        viewModel.onEvent(PublicationDetailContract.UiEvent.LoadPublication(paperId))
    }

    Scaffold(
        topBar = {
            androidx.compose.material.TopAppBar(
                backgroundColor = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    ){
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = "Publication Detail",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                }

                state.errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.errorMessage!!)
                    }
                }

                state.publication != null -> {
                    PublicationDetailContent(publication = state.publication!!)
                }
            }
        }
    }
}

@Composable
private fun PublicationDetailContent(publication: PublicationDetail) {
    Column(
        Modifier
            .padding(LargePadding)
            .verticalScroll(rememberScrollState())
    ) {
        publication.title?.let {
            Text(text = it)
        }
        Spacer(Modifier.height(MediumPadding))
        Text("Journal: ${publication.journal}")
        Text("Venue: ${publication.venue}")
        Text("Published in ${publication.year}")
        Spacer(Modifier.height(MediumPadding))
        Text("Authors: ${publication.authors}")
        Spacer(Modifier.height(LargePadding))

        publication.abstract?.let {
            Text("Abstract")
            Spacer(Modifier.height(SmallPadding))
            Text(it,)
            Spacer(Modifier.height(LargePadding))
        }

        publication.tldr?.let {
            Text("TLDR: $it",)
            Spacer(Modifier.height(LargePadding))
        }

        publication.openAccessPdfUrl
            ?.takeIf(String::isNotBlank)
            ?.let { url ->
                Button(
                    onClick = { openPdf(url) },
                    Modifier.fillMaxWidth()
                ) {
                    Text("Open PDF")
                }
            }
    }
}

expect fun openPdf(url: String)