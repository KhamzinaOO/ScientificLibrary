package org.olgakhamzina.scientificlibrarythesis.presentation.detail


import java.awt.Desktop
import java.net.URI

actual fun openPdf(url: String) {
    println("Desktop: Opening URL: $url")
    Desktop.getDesktop().browse(URI(url))
}