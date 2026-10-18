package com.kingplay.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kingplay.core.common.ConsentGate
import com.kingplay.core.model.repository.SettingsRepository
import com.kingplay.core.model.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "kingplay_settings")

private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ACCENT = stringPreferencesKey("accent")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val LOW_POWER = booleanPreferencesKey("low_power_visuals")
    val ANALYTICS_CONSENT = booleanPreferencesKey("analytics_consent")
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {
    private val store = context.settingsStore

    override val settings: Flow<AppSettings> = store.data
        .map { p ->
            StoredSettings(p[Keys.THEME_MODE], p[Keys.ACCENT], p[Keys.DYNAMIC_COLOR], p[Keys.LOW_POWER]).toSettings()
        }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val current = StoredSettings(p[Keys.THEME_MODE], p[Keys.ACCENT], p[Keys.DYNAMIC_COLOR], p[Keys.LOW_POWER])
            val next = transform(current.toSettings()).toStored()
            p[Keys.THEME_MODE] = next.themeMode!!
            p[Keys.ACCENT] = next.accent!!
            p[Keys.DYNAMIC_COLOR] = next.dynamicColor!!
            p[Keys.LOW_POWER] = next.lowPowerVisuals!!
        }
    }
}

/** Analytics stays off until the user turns it on, and the choice is remembered. */
@Singleton
class DataStoreConsentGate @Inject constructor(
    @ApplicationContext context: Context,
) : ConsentGate {
    private val store = context.settingsStore

    override val analyticsConsent: Flow<Boolean> = store.data.map { it[Keys.ANALYTICS_CONSENT] ?: false }
        .distinctUntilChanged()

    // Analytics only ever fires from user actions, long after DataStore has loaded, so a blocking first read is
    // acceptable here and keeps the interface simple.
    override fun isAnalyticsAllowed(): Boolean =
        runBlocking { analyticsConsent.first() }

    override suspend fun setAnalyticsConsent(granted: Boolean) {
        store.edit { it[Keys.ANALYTICS_CONSENT] = granted }
    }
}
