package com.kingplay.data.library.repo

import com.kingplay.core.database.dao.PlayEventDao
import com.kingplay.core.model.ListeningReport
import com.kingplay.core.model.MonthKey
import com.kingplay.core.model.ReportEntry
import com.kingplay.core.model.ReportTrack
import com.kingplay.core.model.repository.ListeningReportRepository
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomListeningReportRepository @Inject constructor(
    private val events: PlayEventDao,
) : ListeningReportRepository {

    override fun months(): Flow<List<MonthKey>> = events.observeMonths()

    override suspend fun report(month: MonthKey): ListeningReport {
        val zone = ZoneId.systemDefault()
        val ym = YearMonth.parse(month)
        val from = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val totals = events.totals(from, to)
        return ListeningReport(
            month = month,
            plays = totals.plays,
            minutes = totals.totalMs / MS_PER_MINUTE,
            distinctSongs = totals.distinctTracks,
            topTracks = events.topTracks(from, to, TOP_TRACKS).map {
                ReportTrack(it.trackId, it.title, it.artistName, it.albumId, it.uri, it.plays)
            },
            topArtists = events.topArtists(from, to, TOP_NAMES).map { ReportEntry(it.name, it.plays) },
            topGenres = events.topGenres(from, to, TOP_NAMES).map { ReportEntry(it.name, it.plays) },
            busiestHour = events.busiestHour(from, to)?.hour,
        )
    }

    private companion object {
        const val MS_PER_MINUTE = 60_000L
        const val TOP_TRACKS = 5
        const val TOP_NAMES = 5
    }
}
