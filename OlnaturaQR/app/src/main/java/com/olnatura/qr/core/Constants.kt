package com.olnatura.qr.core

import com.olnatura.qr.BuildConfig

/**
 * URL base del API. Configurable en build.gradle.kts:
 * - Por defecto: http://10.0.2.2:3001/ (emulador Android → localhost)
 * - Para dispositivo real: gradle -PAPI_BASE_URL=http://192.168.x.x:3001/
 */
object Constants {
    const val BASE_URL: String = BuildConfig.BASE_URL
}