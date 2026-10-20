package com.kingplay.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingplay.core.model.ListeningReport
import com.kingplay.desktop.library.LibraryIndex
import com.kingplay.desktop.userdata.UserData
import com.kingplay.desktop.userdata.buildReport
import com.kingplay.desktop.userdata.monthsWithPlays
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val Palette = listOf(
    Color(0xFF1DB954) to Color(0xFF04210F),
    Color(0xFFFFD166) to Color(0xFF2B2100),
    Color(0xFFEF476F) to Color.White,
    Color(0xFF118AB2) to Color.White,
    Color(0xFF7B2CBF) to Color.White,
    Color(0xFFFF9F1C) to Color(0xFF2B1700),
)

/** The monthly listening report: colourful story cards for one month, newest month first. */
@Composable
fun ReportPage(user: UserData, index: LibraryIndex, modifier: Modifier = Modifier) {
    val months = remember(user.history) { monthsWithPlays(user.history) }
    var picked by remember { mutableStateOf<String?>(null) }
    val month = picked?.takeIf { it in months } ?: months.firstOrNull()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Listening report", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (month != null && months.size > 1) {
                var open by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { open = true }) { Text(title(month)) }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        months.forEach { m -> DropdownMenuItem(text = { Text(title(m)) }, onClick = { picked = m; open = false }) }
                    }
                }
            }
        }
        if (month == null) {
            Text(
                "No listening yet. Play some songs and your report appears here, and again at the end of each month.",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            val report = remember(user.history, index, month) { buildReport(user.history, index, YearMonth.parse(month)) }
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cards(report).forEachIndexed { i, card ->
                    val (bg, fg) = Palette[i % Palette.size]
                    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(bg).padding(24.dp)) {
                        Text(card.kicker, color = fg, fontSize = 16.sp)
                        Text(card.big, color = fg, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                        if (card.caption.isNotEmpty()) Text(card.caption, color = fg, fontSize = 18.sp)
                        card.lines.forEachIndexed { n, l -> Text("${n + 1}. $l", color = fg, fontSize = 18.sp) }
                    }
                }
            }
        }
    }
}

internal data class ReportCard(val kicker: String, val big: String, val caption: String, val lines: List<String> = emptyList())

private fun title(key: String): String {
    val ym = YearMonth.parse(key)
    return ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.year
}

internal fun hourLabel(hour: Int): String = "${if (hour % 12 == 0) 12 else hour % 12} ${if (hour < 12) "AM" else "PM"}"

internal fun cards(r: ListeningReport): List<ReportCard> = buildList {
    val name = YearMonth.parse(r.month).month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val minutes = if (r.minutes == 1L) "1 minute" else "${r.minutes} minutes"
    add(ReportCard("Your $name in music", minutes, "of listening across ${r.plays} plays."))
    add(ReportCard("You explored", if (r.distinctSongs == 1) "1 song" else "${r.distinctSongs} songs", "Your library kept you busy."))
    r.topTracks.firstOrNull()?.let { add(ReportCard("Your #1 song", it.title, "${it.artistName} · played ${it.plays} times")) }
    if (r.topTracks.size > 1) add(ReportCard("Top songs", "On repeat", "", r.topTracks.map { "${it.title} (${it.plays})" }))
    r.topArtists.firstOrNull()?.let { add(ReportCard("Your top artist", it.name, "${it.plays} plays", r.topArtists.drop(1).map { a -> a.name })) }
    val genre = r.topGenres.firstOrNull()
    val hour = r.busiestHour
    if (genre != null || hour != null) {
        val caption = buildString {
            if (genre != null) append("was your sound of the month. ")
            if (hour != null && genre != null) append("You listened most around ${hourLabel(hour)}.")
        }
        add(ReportCard(if (genre != null) "Top genre" else "Peak time", genre?.name ?: hourLabel(hour!!), caption.trim()))
    }
}
