package org.olgakhamzina.scientificlibrarythesis

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.olgakhamzina.scientificlibrarythesis.DI.initKoin

fun main() {
    initKoin {}
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ScientificLibrary",
        ) {
            App()
        }
    }
}