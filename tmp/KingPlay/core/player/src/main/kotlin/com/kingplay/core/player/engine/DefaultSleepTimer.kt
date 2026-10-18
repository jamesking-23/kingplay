package com.kingplay.core.player.engine

import android.os.SystemClock
import com.kingplay.core.model.repository.SleepTimer
import com.kingplay.core.model.sleep.SleepTimerSpec
import com.kingplay.core.model.sleep.SleepTimerState
import com.kingplay.core.model.sleep.extendSleepTimer
import com.kingplay.core.model.sleep.startSleepTimer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Holds the timer's state. The countdown itself runs in the playback service ([SleepTimerBinder]). */
@Singleton
class DefaultSleepTimer @Inject constructor() : SleepTimer {
    private val current = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)

    override val state: StateFlow<SleepTimerState> = current.asStateFlow()

    override fun start(spec: SleepTimerSpec) {
        current.value = startSleepTimer(spec, SystemClock.elapsedRealtime())
    }

    override fun extend(minutes: Int) {
        current.update { extendSleepTimer(it, minutes) }
    }

    override fun cancel() {
        current.value = SleepTimerState.Off
    }

    internal fun set(newState: SleepTimerState) {
        current.value = newState
    }
}
