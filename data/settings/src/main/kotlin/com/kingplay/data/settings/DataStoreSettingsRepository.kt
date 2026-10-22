package com.kingplay.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    val UI_THEME = stringPreferencesKey("ui_theme")
    val FONT = stringPreferencesKey("font")
    val TEXT_SCALE = floatPreferencesKey("text_scale")
    val CUSTOM_ACCENT = longPreferencesKey("custom_accent")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val HAPTIC_PLAY = stringPreferencesKey("haptic_play")
    val HAPTIC_TRANSPORT = stringPreferencesKey("haptic_transport")
    val HAPTIC_TOGGLE = stringPreferencesKey("haptic_toggle")
    val HAPTIC_TAP = stringPreferencesKey("haptic_tap")
    val NICKNAME = stringPreferencesKey("nickname")
    val PROFILE_PHOTO = stringPreferencesKey("profile_photo")
    val COVER_THEME = booleanPreferencesKey("cover_theme")
    val ANALYTICS_CONSENT = booleanPreferencesKey("analytics_consent")
}

private fun Preferences.stored() = StoredSettings(
    themeMode = this[Keys.THEME_MODE],
    accent = this[Keys.ACCENT],
    dynamicColor = this[Keys.DYNAMIC_COLOR],
    lowPowerVisuals = this[Keys.LOW_POWER],
    uiTheme = this[Keys.UI_THEME],
    font = this[Keys.FONT],
    textScale = this[Keys.TEXT_SCALE],
    customAccent = this[Keys.CUSTOM_ACCENT],
    hapticsEnabled = this[Keys.HAPTICS_ENABLED],
    hapticPlay = this[Keys.HAPTIC_PLAY],
    hapticTransport = this[Keys.HAPTIC_TRANSPORT],
    hapticToggle = this[Keys.HAPTIC_TOGGLE],
    hapticTap = this[Keys.HAPTIC_TAP],
    nickname = this[Keys.NICKNAME],
    profilePhoto = this[Keys.PROFILE_PHOTO],
    coverTheme = this[Keys.COVER_THEME],
)

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {
    private val store = context.settingsStore

    override val settings: Flow<AppSettings> = store.data
        .map { p ->
            p.stored().toSettings()
        }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val next = transform(p.stored().toSettings()).toStored()
            p[Keys.THEME_MODE] = next.themeMode!!
            p[Keys.ACCENT] = next.accent!!
            p[Keys.DYNAMIC_COLOR] = next.dynamicColor!!
            p[Keys.LOW_POWER] = next.lowPowerVisuals!!
            p[Keys.UI_THEME] = next.uiTheme!!
            p[Keys.FONT] = next.font!!
            p[Keys.TEXT_SCALE] = next.textScale!!
            if (next.customAccent != null) p[Keys.CUSTOM_ACCENT] = next.customAccent else p.remove(Keys.CUSTOM_ACCENT)
            p[Keys.HAPTICS_ENABLED] = next.hapticsEnabled!!
            p[Keys.HAPTIC_PLAY] = next.hapticPlay!!
            p[Keys.HAPTIC_TRANSPORT] = next.hapticTransport!!
            p[Keys.HAPTIC_TOGGLE] = next.hapticToggle!!
            p[Keys.HAPTIC_TAP] = next.hapticTap!!
            p[Keys.NICKNAME] = next.nickname!!
            p[Keys.COVER_THEME] = next.coverTheme!!
            if (next.profilePhoto != null) p[Keys.PROFILE_PHOTO] = next.profilePhoto else p.remove(Keys.PROFILE_PHOTO)
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
