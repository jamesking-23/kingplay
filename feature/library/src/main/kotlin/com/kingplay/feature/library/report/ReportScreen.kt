package com.kingplay.feature.library.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingplay.core.designsystem.component.KpEmptyState
import com.kingplay.core.designsystem.component.KpIconButton
import com.kingplay.core.designsystem.theme.KpIcons
import com.kingplay.core.model.ListeningReport
import com.kingplay.core.model.navigation.AppNavigator
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val CardColors = listOf(
    Color(0xFF1DB954) to Color(0xFF04210F),
    Color(0xFFFFD166) to Color(0xFF2B2100),
    Color(0xFFEF476F) to Color(0xFFFFFFFF),
    Color(0xFF118AB2) to Color(0xFFFFFFFF),
    Color(0xFF7B2CBF) to Color(0xFFFFFFFF),
    Color(0xFFFF9F1C) to Color(0xFF2B1700),
)

/** Swipeable story cards for one month of listening. */
@Composable
fun ReportScreen(navigator: AppNavigator, modifier: Modifier = Modifier, viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KpIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = navigator::back,
            )
            Text("Listening report", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (state.months.size > 1 && state.selected != null) {
                MonthPicker(state.months, state.selected!!, viewModel::select)
            }
        }
        val report = state.report
        when {
            !state.loaded -> Unit
            report == null || report.isEmpty -> KpEmptyState(
                icon = KpIcons.MusicNote,
                title = "No listening yet",
                message = "Play some songs and your monthly report will appear here, and again at the end of each month.",
            )
            else -> ReportCards(report)
        }
    }
}

@Composable
private fun MonthPicker(months: List<String>, selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(monthTitle(selected)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            months.forEach { month ->
                DropdownMenuItem(text = { Text(monthTitle(month)) }, onClick = {
                    open = false
                    onSelect(month)
                })
            }
        }
    }
}

@Composable
private fun ReportCards(report: ListeningReport) {
    val cards = remember(report) { buildCards(report) }
    val pager = rememberPagerState(pageCount = { cards.size })
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val (bg, fg) = CardColors[page % CardColors.size]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(bg)
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                CardBody(cards[page], fg)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(cards.size) { i ->
                Box(
                    Modifier
                        .padding(3.dp)
                        .size(if (i == pager.currentPage) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (i == pager.currentPage) 0.9f else 0.3f)),
                )
            }
        }
    }
}

private data class Card(val kicker: String, val big: String, val caption: String, val lines: List<String> = emptyList())

@Composable
private fun CardBody(card: Card, fg: Color) {
    Text(card.kicker, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    Text(
        card.big,
        color = fg,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(vertical = 12.dp),
    )
    if (card.caption.isNotEmpty()) Text(card.caption, color = fg, fontSize = 20.sp)
    card.lines.forEachIndexed { i, line ->
        Text(
            "${i + 1}. $line",
            color = fg,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun monthTitle(key: String): String {
    val ym = YearMonth.parse(key)
    return ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.year
}

private fun buildCards(r: ListeningReport): List<Card> = buildList {
    val name = YearMonth.parse(r.month).month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val minutes = if (r.minutes == 1L) "1 minute" else "${r.minutes} minutes"
    add(Card("Your $name in music", minutes, "of listening across ${r.plays} plays."))
    add(Card("You explored", if (r.distinctSongs == 1) "1 song" else "${r.distinctSongs} songs", "Your library kept you busy."))
    r.topTracks.firstOrNull()?.let {
        add(Card("Your #1 song", it.title, "${it.artistName} · played ${it.plays} times"))
    }
    if (r.topTracks.size > 1) {
        add(Card("Top songs", "On repeat", "", r.topTracks.map { "${it.title} (${it.plays})" }))
    }
    r.topArtists.firstOrNull()?.let {
        add(Card("Your top artist", it.name, "${it.plays} plays", r.topArtists.drop(1).map { a -> a.name }))
    }
    val genre = r.topGenres.firstOrNull()
    val hour = r.busiestHour
    if (genre != null || hour != null) {
        val big = genre?.name ?: hourLabel(hour!!)
        val caption = buildString {
            if (genre != null) append("was your sound of the month. ")
            if (hour != null && genre != null) append("You listened most around ${hourLabel(hour)}.")
        }
        add(Card(if (genre != null) "Top genre" else "Peak time", big, caption.trim()))
    }
}

private fun hourLabel(hour: Int): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "$h12 ${if (hour < 12) "AM" else "PM"}"
}
