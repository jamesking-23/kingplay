package com.kingplay.core.model.repository

import com.kingplay.core.model.settings.AppSettings
import kotlinx.coroutines.flow.Flow

/** App-wide preferences that survive restarts. Audio settings have their own repository. */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
