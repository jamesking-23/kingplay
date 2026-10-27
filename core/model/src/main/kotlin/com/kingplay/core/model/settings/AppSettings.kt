package com.kingplay.core.model.settings

/** Theme choices, kept here as plain enums so the settings can be stored and backed up without the UI toolkit. */
enum class AppThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/**
 * The overall design language, like switching between two apps that share one engine.
 * [KING_ART] is the hand-drawn paper look (default); [KK_UI] is the original Material design.
 */
enum class AppUiTheme { KING_ART, KK_UI, KING_NEON, KING_GLASS, KING_CLASSIC }

enum class AppAccent { VIOLET, OCEAN, EMBER, EMERALD }

/** Typeface choices. [DEFAULT] keeps each design's own type; the others replace it everywhere. */
enum class AppFont { DEFAULT, POPPINS, HANKEN, SOURCE_SANS, SOURCE_SERIF, LORA, FREDOKA, HANDWRITTEN, SKETCH, MONO, READABLE }

/** How strongly a kind of button vibrates. */
enum class HapticLevel { OFF, LIGHT, MEDIUM, STRONG }

/** Which family of buttons a tap belongs to, so each family can be tuned on its own. */
enum class HapticKind { PLAY, TRANSPORT, TOGGLE, TAP }

data class HapticSettings(
    val enabled: Boolean = true,
    val play: HapticLevel = HapticLevel.STRONG,
    val transport: HapticLevel = HapticLevel.MEDIUM,
    val toggle: HapticLevel = HapticLevel.MEDIUM,
    val tap: HapticLevel = HapticLevel.LIGHT,
) {
    fun levelFor(kind: HapticKind): HapticLevel = if (!enabled) {
        HapticLevel.OFF
    } else {
        when (kind) {
            HapticKind.PLAY -> play
            HapticKind.TRANSPORT -> transport
            HapticKind.TOGGLE -> toggle
            HapticKind.TAP -> tap
        }
    }
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accent: AppAccent = AppAccent.VIOLET,
    val dynamicColor: Boolean = true,
    val lowPowerVisuals: Boolean = false,
    val uiTheme: AppUiTheme = AppUiTheme.KING_ART,
    val font: AppFont = AppFont.FREDOKA,
    /** Multiplies every text size; 1.0 is normal. */
    val textScale: Float = 1f,
    /** A colour the person picked (ARGB) that replaces the design's main colour, or null to keep the design's own. */
    val customAccent: Long? = null,
    val haptics: HapticSettings = HapticSettings(),
    /** What the app calls the person. Empty until they finish the first-run welcome. */
    val nickname: String = "",
    /** Path of the profile picture inside the app's private storage, or null. */
    val profilePhoto: String? = null,
    /** Tint the app's colours with the colours of the cover art of the song that is playing. */
    val coverTheme: Boolean = true,
) {
    companion object {
        const val MIN_TEXT_SCALE = 0.85f
        const val MAX_TEXT_SCALE = 1.5f
        const val MAX_NICKNAME = 24
    }
}
