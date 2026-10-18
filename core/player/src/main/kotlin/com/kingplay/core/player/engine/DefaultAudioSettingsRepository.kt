package com.kingplay.core.player.engine

import android.content.Context
import androidx.core.content.edit
import com.kingplay.core.common.ApplicationScope
import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.EqPreset
import com.kingplay.core.model.OutputDevice
import com.kingplay.core.model.repository.AudioSettingsRepository
import com.kingplay.core.model.repository.EqRepository
import com.kingplay.core.model.sanitized
import com.kingplay.core.player.R
import com.kingplay.core.player.dsp.AudioSettingsCodec
import com.kingplay.core.player.dsp.profileFrom
import com.kingplay.core.player.dsp.settingsFrom
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the live audio settings. Two layers:
 * - base: what the user set last, saved in SharedPreferences;
 * - device profile: when the current output has one, its curve and effects replace the base while it is connected.
 *
 * Edits made while a profile is active go to that profile (after a short pause, so dragging a slider does not write
 * to the database on every frame). Fade, headroom and the ReplayGain preamp are global and always go to the base.
 */
@Singleton
class DefaultAudioSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eq: EqRepository,
    private val tracker: OutputDeviceTracker,
    @ApplicationScope private val scope: CoroutineScope,
) : AudioSettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var base: AudioSettings = loadBase()
    private val live = MutableStateFlow(base)
    private val device = MutableStateFlow<OutputDevice?>(null)
    private val profileActive = MutableStateFlow(false)
    private var saveJob: Job? = null

    override val settings: StateFlow<AudioSettings> = live.asStateFlow()
    override val output: StateFlow<OutputDevice?> = device.asStateFlow()
    override val hasProfileForOutput: StateFlow<Boolean> = profileActive.asStateFlow()

    init {
        tracker.start()
        scope.launch { tracker.output.collect { onOutputChanged(it) } }
    }

    override fun update(transform: (AudioSettings) -> AudioSettings) {
        val previous = live.value
        var next = transform(previous).sanitized()
        val curveMoved = next.bandGainsDb != previous.bandGainsDb || next.preampDb != previous.preampDb
        if (curveMoved && next.presetId == previous.presetId) next = next.copy(presetId = null)
        commit(next)
    }

    override fun applyPreset(preset: EqPreset) {
        update {
            it.copy(
                eqEnabled = true,
                preampDb = preset.preampDb,
                bandGainsDb = preset.bandGainsDb,
                presetId = preset.id,
            )
        }
    }

    override suspend fun saveProfileForOutput() {
        saveJob?.cancel()
        writeProfile(live.value)
    }

    override suspend fun removeProfileForOutput() {
        val current = device.value ?: return
        saveJob?.cancel()
        eq.deleteDeviceProfile(current.key)
        profileActive.value = false
        live.value = base
    }

    private fun commit(next: AudioSettings) {
        live.value = next
        // The global fields always belong to the base.
        base = if (profileActive.value) {
            base.copy(
                fadeMs = next.fadeMs,
                autoHeadroom = next.autoHeadroom,
                replayGainPreampDb = next.replayGainPreampDb,
            )
        } else {
            next
        }
        persistBase()
        if (profileActive.value) {
            saveJob?.cancel()
            saveJob = scope.launch {
                delay(SAVE_DELAY_MS)
                writeProfile(next)
            }
        }
    }

    private suspend fun onOutputChanged(newOutput: OutputDevice?) {
        device.value = newOutput
        val profile = newOutput?.let { eq.deviceProfile(it.key) }
        if (profile == null) {
            profileActive.value = false
            live.update { base }
        } else {
            val preset = profile.eqPresetId?.let { eq.preset(it) }
            live.value = settingsFrom(profile, preset, base)
            profileActive.value = true
        }
    }

    private suspend fun writeProfile(settings: AudioSettings) {
        val current = device.value ?: return
        val presetId = if (!settings.eqEnabled) null else settings.presetId ?: saveCustomPreset(current, settings)
        eq.saveDeviceProfile(profileFrom(settings, current, presetId))
        profileActive.value = true
    }

    /** A hand-made curve is stored as a user preset named for the device, reusing the same one on later edits. */
    private suspend fun saveCustomPreset(current: OutputDevice, settings: AudioSettings): Long {
        val name = context.getString(R.string.player_output_custom_preset, current.displayName)
        val existing = eq.deviceProfile(current.key)?.eqPresetId?.let { eq.preset(it) }
            ?.takeIf { !it.isBuiltIn && it.name == name }
        return eq.savePreset(
            EqPreset(
                id = existing?.id ?: 0,
                name = name,
                preampDb = settings.preampDb,
                bandGainsDb = settings.bandGainsDb,
                isBuiltIn = false,
            ),
        )
    }

    private fun loadBase(): AudioSettings {
        val stored = prefs.all.mapNotNull { (key, value) -> value?.let { key to it.toString() } }.toMap()
        return AudioSettingsCodec.decode(stored)
    }

    private fun persistBase() {
        val encoded = AudioSettingsCodec.encode(base)
        prefs.edit { encoded.forEach { (key, value) -> putString(key, value) } }
    }

    private companion object {
        const val PREFS = "kingplay_audio"
        const val SAVE_DELAY_MS = 400L
    }
}
