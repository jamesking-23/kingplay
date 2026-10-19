package com.kingplay.feature.library.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingplay.core.model.ListeningReport
import com.kingplay.core.model.MonthKey
import com.kingplay.core.model.repository.ListeningReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

object ReportArgs {
    const val MONTH = "month"
    const val LATEST = "latest"
}

data class ReportUiState(
    val months: List<MonthKey> = emptyList(),
    val selected: MonthKey? = null,
    val report: ListeningReport? = null,
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val reports: ListeningReportRepository,
) : ViewModel() {

    private val requested = handle.get<String>(ReportArgs.MONTH)?.takeIf { it != ReportArgs.LATEST }
    private val choice = MutableStateFlow(requested)

    val state: StateFlow<ReportUiState> = combine(reports.months(), choice) { months, picked ->
        months to (picked?.takeIf { it in months } ?: months.firstOrNull())
    }.flatMapLatest { (months, selected) ->
        flow {
            if (selected == null) {
                emit(ReportUiState(months = months, loaded = true))
            } else {
                emit(ReportUiState(months = months, selected = selected, report = reports.report(selected), loaded = true))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), ReportUiState())

    fun select(month: MonthKey) {
        choice.value = month
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}

/** True when [months] contains last calendar month, i.e. a finished report the user has not necessarily seen. */
fun hasLastMonthReport(months: List<MonthKey>): Boolean = YearMonth.now().minusMonths(1).toString() in months

@HiltViewModel
class ReportBannerViewModel @Inject constructor(reports: ListeningReportRepository) : ViewModel() {
    val lastMonth: StateFlow<MonthKey?> = combine(reports.months(), MutableStateFlow(Unit)) { months, _ ->
        YearMonth.now().minusMonths(1).toString().takeIf { it in months }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    private companion object {
        const val STOP_MS = 5_000L
    }
}
