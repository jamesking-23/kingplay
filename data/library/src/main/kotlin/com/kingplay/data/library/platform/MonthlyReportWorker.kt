package com.kingplay.data.library.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kingplay.core.model.repository.ListeningReportRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Runs once a day. In the first days of a new month it tells the listener that last month's report is ready, once.
 * The report itself is always available inside the app, so a missing notification permission loses nothing.
 */
@HiltWorker
class MonthlyReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reports: ListeningReportRepository,
) : CoroutineWorker(appContext, params) {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun doWork(): Result = try {
        notifyIfDue()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    private suspend fun notifyIfDue() {
        val previous = YearMonth.now().minusMonths(1)
        val key = previous.toString()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_NOTIFIED, null) == key) return
        val report = reports.report(key)
        if (report.isEmpty) {
            prefs.edit().putString(KEY_LAST_NOTIFIED, key).apply()
            return
        }
        if (!canNotify()) return
        val monthName = previous.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val launch = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val tap = PendingIntent.getActivity(
            applicationContext,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val manager = NotificationManagerCompat.from(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, "Monthly report", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val topArtist = report.topArtists.firstOrNull()?.name
        val text = "${report.minutes} minutes of music" + (topArtist?.let { ", led by $it" }.orEmpty()) + ". Tap to see it."
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Your $monthName listening report is ready")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // checked in canNotify()
        manager.notify(NOTIFICATION_ID, notification)
        prefs.edit().putString(KEY_LAST_NOTIFIED, key).apply()
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
    }

    companion object {
        private const val PREFS = "kingplay_reports"
        private const val KEY_LAST_NOTIFIED = "last_notified_month"
        private const val CHANNEL = "monthly_report"
        private const val NOTIFICATION_ID = 4107
        private const val UNIQUE_NAME = "monthly-report"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MonthlyReportWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
