package org.olgakhamzina.scientificlibrarythesis

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinContext
import org.olgakhamzina.scientificlibrarythesis.navigation.NavGraph
import org.olgakhamzina.scientificlibrarythesis.ui.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        KoinContext {
            val navController = rememberNavController()
            NavGraph(
                navController = navController
            )
        }
    }
}