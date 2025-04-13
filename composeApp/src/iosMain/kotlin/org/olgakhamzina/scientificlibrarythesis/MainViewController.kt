package org.olgakhamzina.scientificlibrarythesis

import androidx.compose.ui.window.ComposeUIViewController
import org.olgakhamzina.scientificlibrarythesis.DI.initKoin

fun MainViewController()  = ComposeUIViewController(
    configure = {
        initKoin{
        }
    }
) { App() }