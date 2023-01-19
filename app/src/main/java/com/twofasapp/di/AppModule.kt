package com.twofasapp.di

import com.twofasapp.analytics.AnalyticsFirebase
import com.twofasapp.common.analytics.Analytics
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

class AppModule : KoinModule {
    override fun provide() = module {
        singleOf(::AnalyticsFirebase) { bind<Analytics>() }
    }
}