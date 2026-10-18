package com.kingplay.data.library.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Which runtime permission gates which part of the library, across Android versions. */
object MediaPermissions {
    fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun videoPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /** What to pass to the permission request launcher. */
    fun required(): Array<String> = arrayOf(audioPermission(), videoPermission()).distinct().toTypedArray()

    fun hasAudio(context: Context): Boolean = granted(context, audioPermission())

    fun hasVideo(context: Context): Boolean = granted(context, videoPermission())

    fun hasAny(context: Context): Boolean = hasAudio(context) || hasVideo(context)

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
