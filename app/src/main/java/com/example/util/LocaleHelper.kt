package com.example.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun createLocalizedContext(context: Context, appLanguage: String): Context {
        val localeCode = if (appLanguage.equals("हिंदी", ignoreCase = true) ||
            appLanguage.equals("hi", ignoreCase = true) ||
            appLanguage.equals("Hindi", ignoreCase = true)
        ) {
            "hi"
        } else {
            "en"
        }

        val locale = Locale(localeCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
