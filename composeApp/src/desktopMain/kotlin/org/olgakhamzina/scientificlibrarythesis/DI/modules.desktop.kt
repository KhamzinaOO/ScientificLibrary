package org.olgakhamzina.scientificlibrarythesis.DI

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun provideHttpClientEngine(): HttpClientEngine =
    OkHttp.create {
        config {
            retryOnConnectionFailure(true)
        }
    }