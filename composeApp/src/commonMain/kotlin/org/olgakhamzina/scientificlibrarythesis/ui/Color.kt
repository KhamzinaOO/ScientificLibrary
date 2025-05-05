package org.olgakhamzina.scientificlibrarythesis.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Фирменные цвета Бонч-Бруевича
val CorporateOrange    = Color(0xFFF7941D)
val CorporateLightGray = Color(0xFFF2F2F2)
val CorporateDarkGray  = Color(0xFF4D4D4D)
val CorporateBlack     = Color(0xFF000000)
val CorporateWhite     = Color(0xFFFFFFFF)

// Light Theme
val LightColorScheme = lightColorScheme(
    primary                = CorporateOrange,
    onPrimary              = CorporateWhite,
    outline                = CorporateDarkGray,
    background             = CorporateWhite,
    onBackground           = CorporateDarkGray,
    surface                = CorporateLightGray,
    onSurface              = CorporateDarkGray,
    surfaceContainer       = CorporateLightGray,
    surfaceContainerHighest= CorporateWhite,
    surfaceContainerHigh   = CorporateWhite,
    surfaceContainerLow    = CorporateLightGray
)

// Dark Theme
val DarkColorScheme = darkColorScheme(
    primary                = CorporateOrange,
    onPrimary              = CorporateBlack,
    outline                = CorporateLightGray,
    background             = CorporateBlack,
    onBackground           = CorporateWhite,
    surface                = CorporateDarkGray,
    surfaceContainer       = CorporateDarkGray,
    surfaceContainerHighest= CorporateDarkGray,
    surfaceContainerHigh   = CorporateDarkGray,
    surfaceContainerLow    = CorporateDarkGray
)
