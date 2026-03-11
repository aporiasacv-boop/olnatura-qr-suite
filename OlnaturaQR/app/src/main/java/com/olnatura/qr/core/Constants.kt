package com.olnatura.qr.core

import com.olnatura.qr.BuildConfig

/**
 * URL base del API. Configurable en build.gradle.kts:
 * - Por defecto: / (emulador Android → localhost)
 * - Para dispositivo real: gradle -PAPI_BASE_URL=192.168.41.177/
 */
object Constants {
    const val BASE_URL: String = BuildConfig.BASE_URL
}