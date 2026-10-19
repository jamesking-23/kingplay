package com.kingplay.core.model.repository

import com.kingplay.core.model.ListeningReport
import com.kingplay.core.model.MonthKey
import kotlinx.coroutines.flow.Flow

interface ListeningReportRepository {
    /** Months that have any listening, newest first. */
    fun months(): Flow<List<MonthKey>>

    suspend fun report(month: MonthKey): ListeningReport
}
