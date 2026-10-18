package com.kingplay.core.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {

    private data class CounterState(val count: Int = 0) : UiState

    private sealed interface CounterEvent : UiEvent {
        data object Increment : CounterEvent

        data object Ping : CounterEvent
    }

    private data object Pinged : UiEffect

    private class CounterViewModel : MviViewModel<CounterState, CounterEvent, Pinged>(CounterState()) {
        override fun onEvent(event: CounterEvent) {
            when (event) {
                CounterEvent.Increment -> setState { copy(count = count + 1) }
                CounterEvent.Ping -> sendEffect(Pinged)
            }
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun reducesEventsIntoState() {
        val vm = CounterViewModel()
        vm.onEvent(CounterEvent.Increment)
        vm.onEvent(CounterEvent.Increment)
        assertEquals(2, vm.state.value.count)
    }

    @Test
    fun deliversEffectsOnce() = runTest {
        val vm = CounterViewModel()
        vm.effects.test {
            vm.onEvent(CounterEvent.Ping)
            assertEquals(Pinged, awaitItem())
            expectNoEvents()
        }
    }
}
