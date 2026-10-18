package com.kingplay.core.player.logic

/**
 * Who may connect to the exported playback service. The service has to be exported so cars, assistants and
 * Bluetooth can reach it, but a random app has no business browsing the library or starting playback.
 *
 * Allowed: this app itself, the system (Media3 reports those as "trusted"), and the packages below, which are the
 * ones that legitimately drive a music app. An unlisted package is refused, and its name goes to the log so a
 * missing car or watch system is easy to spot and add.
 */
object CallerPolicy {
    val ALLOWED_PACKAGES: Set<String> = setOf(
        "com.google.android.projection.gearhead", // Android Auto
        "com.google.android.projection.gearhead.emulator", // Android Auto on the desktop head unit
        "com.google.android.carassistant", // Assistant in the car
        "com.google.android.googlequicksearchbox", // Google Assistant and voice search
        "com.google.android.apps.googleassistant",
        "com.google.android.wearable.app", // Wear OS companion
        "com.google.android.gms", // Cast and Nearby media controls
        "com.google.android.apps.mediashell", // Cast receiver
        "com.android.bluetooth", // headset and car buttons (AVRCP)
        "com.android.systemui", // lock screen, quick settings and notification controls
        "com.android.car.media", // Android Automotive OS media UI
        "com.android.car.mediacenter",
    )

    fun isAllowed(
        callerPackage: String,
        ownPackage: String,
        trusted: Boolean,
        allowed: Set<String> = ALLOWED_PACKAGES,
    ): Boolean = trusted || callerPackage == ownPackage || callerPackage in allowed
}
