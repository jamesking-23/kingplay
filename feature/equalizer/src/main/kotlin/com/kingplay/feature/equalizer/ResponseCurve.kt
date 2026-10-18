package com.kingplay.feature.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingplay.core.model.AudioSettings
import com.kingplay.core.model.EqPreset
import com.kingplay.core.player.dsp.MAX_BASS_BOOST_DB
import com.kingplay.core.player.dsp.responseDb
import com.kingplay.feature.equalizer.logic.curveFrequencies
import com.kingplay.feature.equalizer.logic.formatDb
import com.kingplay.feature.equalizer.logic.logPosition

private const val CURVE_RANGE_DB = 12f
private const val SUMMARY_BASS_HZ = 100.0
private const val SUMMARY_MID_HZ = 1_000.0
private const val SUMMARY_TREBLE_HZ = 8_000.0

/**
 * What the EQ does to the sound, drawn from the same filter math the audio engine uses. TalkBack gets a three-point
 * summary instead of a picture.
 */
@Composable
fun ResponseCurve(
    bandGainsDb: List<Float>,
    bassBoostStrength: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val bands = remember(bandGainsDb, enabled) {
        DoubleArray(EqPreset.BAND_COUNT) { if (enabled) bandGainsDb[it].toDouble() else 0.0 }
    }
    val bass = bassBoostStrength / AudioSettings.MAX_BASS_BOOST.toDouble() * MAX_BASS_BOOST_DB
    val frequencies = remember { curveFrequencies() }
    val values = remember(bands, bass) { frequencies.map { responseDb(bands, bass, it).toFloat() } }
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    val summary = stringResource(
        R.string.eq_curve_description,
        formatDb(responseDb(bands, bass, SUMMARY_BASS_HZ).toFloat()),
        formatDb(responseDb(bands, bass, SUMMARY_MID_HZ).toFloat()),
        formatDb(responseDb(bands, bass, SUMMARY_TREBLE_HZ).toFloat()),
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = summary },
    ) {
        val midY = size.height / 2
        drawLine(grid, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1.dp.toPx())
        for (hz in listOf(100.0, 1_000.0, 10_000.0)) {
            val x = logPosition(hz).toFloat() * size.width
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        }
        val path = Path()
        frequencies.forEachIndexed { i, hz ->
            val x = logPosition(hz).toFloat() * size.width
            val y = midY - (values[i].coerceIn(-CURVE_RANGE_DB, CURVE_RANGE_DB) / CURVE_RANGE_DB) * midY
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, if (enabled) line else dim, style = Stroke(width = 3.dp.toPx()))
    }
}
