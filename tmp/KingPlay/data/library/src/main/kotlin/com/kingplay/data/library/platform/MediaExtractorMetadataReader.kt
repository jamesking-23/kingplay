package com.kingplay.data.library.platform

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.kingplay.core.common.Dispatcher
import com.kingplay.core.common.KpDispatchers
import com.kingplay.data.library.scan.AudioDetails
import com.kingplay.data.library.scan.AudioMetadataReader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Opens the audio stream to read the real sample rate and, when the container declares it, the bitrate. */
@Singleton
class MediaExtractorMetadataReader @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(KpDispatchers.IO) private val io: CoroutineDispatcher,
) : AudioMetadataReader {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun read(uri: String): AudioDetails? = withContext(io) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, Uri.parse(uri), null)
            firstAudioFormat(extractor)?.let { format ->
                AudioDetails(
                    bitrateKbps = format.intOrZero(MediaFormat.KEY_BIT_RATE) / BITS_PER_KILOBIT,
                    sampleRateHz = format.intOrZero(MediaFormat.KEY_SAMPLE_RATE),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Corrupt or unsupported files are expected in a real library. Report "unknown" and move on.
            null
        } finally {
            extractor.release()
        }
    }

    private fun firstAudioFormat(extractor: MediaExtractor): MediaFormat? {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) return format
        }
        return null
    }

    private fun MediaFormat.intOrZero(key: String): Int = if (containsKey(key)) getInteger(key) else 0

    private companion object {
        const val BITS_PER_KILOBIT = 1000
    }
}
