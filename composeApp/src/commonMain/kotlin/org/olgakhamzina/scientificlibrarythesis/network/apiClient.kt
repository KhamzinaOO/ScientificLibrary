package org.olgakhamzina.scientificlibrarythesis.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException
import org.olgakhamzina.scientificlibrarythesis.utill.NetworkError
import org.olgakhamzina.scientificlibrarythesis.utill.Result
import kotlin.random.Random

class ApiClient(
    val httpClient: HttpClient,
){
    suspend inline fun <reified T> get(
        endpoint: String,
        headers: Map<String, String> = emptyMap(),
        parameters: Map<String, String?> = emptyMap()
    ): Result<T, NetworkError> {
        val response = try {
            httpClient.get(endpoint) {
                header("X-Request-Id", RequestIdGenerator.generate())
                headers.forEach { (key, value) -> header(key, value) }
                parameters.forEach { (key, value) -> value?.let { parameter(key, it) } }
                accept(ContentType.Application.Json)
            }
        } catch (e : Exception){
            return handleException(e)
        }

        return handleResponse(response)
    }

    suspend inline fun <reified T, reified Body> post(
        endpoint: String,
        body: Body,
        headers: Map<String, String> = emptyMap()
    ): Result<T, NetworkError> {
        val response = try {
            httpClient.post(endpoint) {
                header("X-Request-Id", RequestIdGenerator.generate())
                headers.forEach { (key, value) -> header(key, value) }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        catch (e : Exception){
            return handleException(e)
        }

        return handleResponse(response)
    }

    inline fun handleException(e: Exception) : Result. Error<NetworkError> {
        return when (e){
            is CancellationException -> Result.Error(NetworkError.CANCELLATION)
            is UnresolvedAddressException -> Result.Error(NetworkError.NO_INTERNET)
            is SerializationException -> Result.Error(NetworkError.SERIALIZATION)
            is TimeoutCancellationException -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            else -> Result.Error(NetworkError.UNKNOWN)
        }
    }

    suspend inline fun <reified T> handleResponse(response: HttpResponse): Result<T, NetworkError> {
        return when (response.status.value) {
            in 200..299 -> {
                val result = response.body<T>()
                Result.Success(result)
            }
            400 -> Result.Error(NetworkError.BAD_REQUEST)
            403 -> Result.Error(NetworkError.FORBIDDEN)
            409 -> Result.Error(NetworkError.CONFLICT)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> Result.Error(NetworkError.UNKNOWN)
        }
    }
}

object RequestIdGenerator {
    fun generate(): String {
        return List(16) { Random.nextInt(0, 16).toString(16) }.joinToString("")
    }
}