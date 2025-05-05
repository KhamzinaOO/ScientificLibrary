package org.olgakhamzina.scientificlibrarythesis.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(engine: HttpClientEngine) : HttpClient{
    return HttpClient(engine){
        install(Logging){
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            level = LogLevel.ALL
        }
        install(ContentNegotiation){
            json(
                json = Json{
                    ignoreUnknownKeys = true
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis  = 30_000
        }
        install(DefaultRequest){
            url{
                protocol = URLProtocol.HTTP
                host = "192.168.1.72"
                port = 8080
            }
        }
    }
}