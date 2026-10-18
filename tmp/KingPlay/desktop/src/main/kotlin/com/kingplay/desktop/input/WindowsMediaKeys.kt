package com.kingplay.desktop.input

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * Listens for the keyboard's media keys (play/pause, next, previous, stop) even when no KingPlay window has focus,
 * by registering them as Windows hot keys. A key another program already registered (a browser, another player)
 * cannot be taken and is simply skipped. Windows only: on any other system, or when anything goes wrong,
 * [start] returns false and the app carries on without it.
 */
class WindowsMediaKeys(private val onAction: (MediaKeyAction) -> Unit) {

    @Structure.FieldOrder("hwnd", "message", "wParam", "lParam", "time", "ptX", "ptY")
    class Msg : Structure() {
        @JvmField var hwnd: Pointer? = null

        @JvmField var message: Int = 0

        @JvmField var wParam: Pointer? = null

        @JvmField var lParam: Pointer? = null

        @JvmField var time: Int = 0

        @JvmField var ptX: Int = 0

        @JvmField var ptY: Int = 0
    }

    @Suppress("FunctionName")
    interface User32 : Library {
        fun RegisterHotKey(hWnd: Pointer?, id: Int, modifiers: Int, virtualKey: Int): Boolean

        fun UnregisterHotKey(hWnd: Pointer?, id: Int): Boolean

        fun GetMessageW(msg: Msg, hWnd: Pointer?, filterMin: Int, filterMax: Int): Int
    }

    @Volatile private var started = false

    /** Returns true when at least one media key was registered. Call once. */
    @Suppress("TooGenericExceptionCaught")
    fun start(): Boolean {
        if (started || !System.getProperty("os.name").orEmpty().startsWith("Windows")) return false
        val ready = java.util.concurrent.CompletableFuture<Boolean>()
        val thread = Thread({
            try {
                val user32 = Native.load("user32", User32::class.java)
                // Hot keys belong to the thread that registered them, so this thread also reads their messages.
                val registered = MediaKeyCodes.ALL.filter { user32.RegisterHotKey(null, it, MOD_NOREPEAT, it) }
                ready.complete(registered.isNotEmpty())
                if (registered.isEmpty()) return@Thread
                val msg = Msg()
                while (user32.GetMessageW(msg, null, 0, 0) > 0) {
                    if (msg.message == WM_HOTKEY) {
                        val id = Pointer.nativeValue(msg.wParam)
                        actionForHotKeyId(id)?.let(onAction)
                    }
                }
                registered.forEach { user32.UnregisterHotKey(null, it) }
            } catch (e: Throwable) {
                ready.complete(false)
            }
        }, "kingplay-media-keys")
        thread.isDaemon = true // dies with the app; Windows frees the hot keys with the process
        thread.start()
        started = runCatching { ready.get(START_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS) }
            .getOrDefault(false)
        return started
    }

    private companion object {
        const val WM_HOTKEY = 0x0312
        const val MOD_NOREPEAT = 0x4000
        const val START_TIMEOUT_SECONDS = 3L
    }
}
