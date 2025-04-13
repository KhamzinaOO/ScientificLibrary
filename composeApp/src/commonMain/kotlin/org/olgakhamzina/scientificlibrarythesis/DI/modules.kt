package org.olgakhamzina.scientificlibrarythesis.DI

import io.ktor.client.engine.HttpClientEngine
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.olgakhamzina.scientificlibrarythesis.domain.PublicationDetailRepository
import org.olgakhamzina.scientificlibrarythesis.domain.PublicationDetailRepositoryImpl
import org.olgakhamzina.scientificlibrarythesis.domain.PublicationRepository
import org.olgakhamzina.scientificlibrarythesis.domain.PublicationRepositoryImpl
import org.olgakhamzina.scientificlibrarythesis.network.ApiClient
import org.olgakhamzina.scientificlibrarythesis.network.createHttpClient
import org.olgakhamzina.scientificlibrarythesis.presentation.detail.PublicationDetailViewModel
import org.olgakhamzina.scientificlibrarythesis.presentation.search.SearchViewModel
import org.olgakhamzina.scientificlibrarythesis.service.DetailService
import org.olgakhamzina.scientificlibrarythesis.service.ResultService

val httpClientEngine = provideHttpClientEngine()

fun networkModule(httpClientEngine: HttpClientEngine): Module {
    return module {
        single { createHttpClient(httpClientEngine) }
        single { ApiClient(get()) }
        single { ResultService(get()) }
        single { DetailService(get()) }
    }
}

val ViewModelModule = module {
    viewModelOf(::SearchViewModel)
    viewModelOf(::PublicationDetailViewModel)
}

val RepositoryModule = module {
    singleOf (
        ::PublicationRepositoryImpl
    ).bind<PublicationRepository>()

    singleOf(
        ::PublicationDetailRepositoryImpl
    ).bind<PublicationDetailRepository>()
}

expect fun provideHttpClientEngine() : HttpClientEngine