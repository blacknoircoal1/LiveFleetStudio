package com.livefleet.studio

/**
 * Single source of truth for application identity, versioning, and branding metadata.
 * Version values are sourced directly from Gradle BuildConfig (app/build.gradle.kts)
 * so version bumps always stay in sync automatically.
 */
object AppConfig {
    const val APP_NAME = "LiveFleet Studio"
    val VERSION_NAME: String get() = BuildConfig.VERSION_NAME
    val VERSION_CODE: Int get() = BuildConfig.VERSION_CODE
    const val CREATOR = "KayAyyFit"
    val VERSION_LABEL: String get() = "$APP_NAME v$VERSION_NAME"
}
