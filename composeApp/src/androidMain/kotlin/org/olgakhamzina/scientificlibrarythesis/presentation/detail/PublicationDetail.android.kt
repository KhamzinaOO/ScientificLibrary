package org.olgakhamzina.scientificlibrarythesis.presentation.detail

import android.content.Intent
import android.net.Uri
import org.olgakhamzina.scientificlibrarythesis.MainApplication
import androidx.core.net.toUri

actual fun openPdf(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Use the globally stored application context.
    MainApplication.getContext().startActivity(intent)
}