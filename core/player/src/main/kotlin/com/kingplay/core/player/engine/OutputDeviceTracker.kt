package com.kingplay.core.player.engine

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.kingplay.core.model.OutputDevice
import com.kingplay.core.model.OutputDeviceType
import com.kingplay.core.player.R
import com.kingplay.core.player.dsp.OutputCandidate
import com.kingplay.core.player.dsp.pickActiveOutput
import com.kingplay.core.player.dsp.toOutputDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Watches which outputs are connected and reports the one the audio is most likely going to. */
@Singleton
class OutputDeviceTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val state = MutableStateFlow<OutputDevice?>(null)
    private var started = false

    val output: StateFlow<OutputDevice?> = state.asStateFlow()

    /** Safe to call more than once. */
    fun start() {
        if (started) return
        started = true
        refresh()
        audioManager.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
            },
            Handler(Looper.getMainLooper()),
        )
    }

    private fun refresh() {
        val candidates = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { info ->
            val type = classify(info.type) ?: return@mapNotNull null
            OutputCandidate(type, info.productName?.toString().orEmpty())
        }
        val speaker = context.getString(R.string.player_output_speaker)
        state.value = pickActiveOutput(candidates)?.toOutputDevice(speaker)
            ?: OutputCandidate(OutputDeviceType.SPEAKER, speaker).toOutputDevice(speaker)
    }

    private fun classify(type: Int): OutputDeviceType? = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> OutputDeviceType.SPEAKER
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL,
        -> OutputDeviceType.WIRED
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER -> OutputDeviceType.BLUETOOTH
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY ->
            OutputDeviceType.USB_DAC
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_DOCK -> OutputDeviceType.OTHER
        // Earpiece, phone-call and Bluetooth SCO routes are not music outputs.
        else -> null
    }

    private companion object {
        // Added in API 31; written as numbers so this compiles and runs on API 26.
        const val TYPE_BLE_HEADSET = 26
        const val TYPE_BLE_SPEAKER = 27
    }
}
