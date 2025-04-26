package org.olgakhamzina.scientificlibrarythesis.presentation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.olgakhamzina.scientificlibrarythesis.data.PublicationDetail

@Composable
fun PublicationDetailScreen(
    paperId: String,
    onBack: () -> Unit
) {

    val viewModel : PublicationDetailViewModel = koinViewModel()

    LaunchedEffect(key1 = paperId) {
        viewModel.loadPublication(paperId)
    }

    val publication by viewModel.publicationDetail.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Publication Detail") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                }
            })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (publication == null) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                PublicationDetailContent(publication = publication!!)
            }
        }
    }
}

@Composable
fun PublicationDetailContent(publication: PublicationDetail) {
    Column(modifier = Modifier
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
    ) {
        publication.title?.let { Text(text = it, style = MaterialTheme.typography.h5) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Journal: ${publication.journal}", style = MaterialTheme.typography.body1)
        Text(text = "Venue: ${publication.venue}", style = MaterialTheme.typography.body2)
        Text(text = "Published in ${publication.year}", style = MaterialTheme.typography.body2)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Authors: ${publication.authors}", style = MaterialTheme.typography.body2)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Abstract", style = MaterialTheme.typography.subtitle1)
        publication.abstract?.let { Text(text = it, style = MaterialTheme.typography.body1) }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "TLDR: ${publication.tldr}", style = MaterialTheme.typography.body2)
        Spacer(modifier = Modifier.height(16.dp))

        if (!publication.openAccessPdfUrl.isNullOrBlank()) {
            Button(
                onClick = {

                    if (publication.openAccessPdfUrl.isNotBlank()) {
                        openPdf(publication.openAccessPdfUrl)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open PDF")
            }
        }
    }
}

expect fun openPdf(url: String)