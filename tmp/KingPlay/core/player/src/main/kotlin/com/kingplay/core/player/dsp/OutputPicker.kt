package com.kingplay.core.player.dsp

import com.kingplay.core.model.OutputDevice
import com.kingplay.core.model.OutputDeviceType
import java.util.Locale

/** A connected audio output as the platform reports it, before we decide which one is in use. */
data class OutputCandidate(val type: OutputDeviceType, val name: String)

/**
 * The phone does not say which output is active before Android 13, so use the order Android itself prefers:
 * USB, then wired, then Bluetooth, then the built-in speaker. Ties keep the first one listed.
 */
fun pickActiveOutput(candidates: List<OutputCandidate>): OutputCandidate? =
    candidates.minByOrNull { priority(it.type) }

private fun priority(type: OutputDeviceType): Int = when (type) {
    OutputDeviceType.USB_DAC -> 0
    OutputDeviceType.WIRED -> 1
    OutputDeviceType.BLUETOOTH -> 2
    OutputDeviceType.SPEAKER -> 3
    OutputDeviceType.OTHER -> 4
}

/** Key that stays the same when the same headphones reconnect. The built-in speaker is always "speaker". */
fun outputKey(candidate: OutputCandidate): String =
    if (candidate.type == OutputDeviceType.SPEAKER) {
        "speaker"
    } else {
        "${candidate.type.name.lowercase(Locale.ROOT)}:${candidate.name.trim().lowercase(Locale.ROOT)}"
    }

fun OutputCandidate.toOutputDevice(speakerName: String): OutputDevice = OutputDevice(
    key = outputKey(this),
    type = type,
    displayName = if (type == OutputDeviceType.SPEAKER || name.isBlank()) speakerName.ifBlank { name } else name,
)
