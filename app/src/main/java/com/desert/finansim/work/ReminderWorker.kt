package com.desert.finansim.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.desert.finansim.FinansimApp
import com.desert.finansim.domain.DateUtils
import com.desert.finansim.domain.Money
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Gunluk bakim isi: yaklasan borc odemelerini bildirir.
 *
 * Aginternet erisimi gerektirmez; tamamen yerel calisir.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val container = (applicationContext as? FinansimApp)?.container
            ?: return ListenableWorker.Result.success()

        try {
            val settings = container.settingsRepository.settings.first()
            if (!settings.notificationsEnabled) return ListenableWorker.Result.success()

            Notifications.ensureChannel(applicationContext)

            val today = DateUtils.today()
            val horizon = today.plusDays(settings.reminderDaysBefore.toLong())
            val dashboard = container.analyticsRepository
                .dashboard(DateUtils.currentMonthKey())
                .first()

            val due = dashboard.upcoming.filter { !it.date.isAfter(horizon) }
            if (due.isNotEmpty()) {
                val lines = due.map { payment ->
                    val amount = Money.format(payment.amountMinor, settings.currencySymbol)
                    "${DateUtils.relativeLabel(payment.date)} • ${payment.title}: $amount"
                }
                val total = due.sumOf { it.amountMinor }
                Notifications.showUpcomingPayments(
                    applicationContext,
                    "Yaklaşan ödemeler: ${Money.format(total, settings.currencySymbol)}",
                    lines,
                )
            }

            return ListenableWorker.Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // Gecici bir hata olabilir; bir sonraki periyotta tekrar denenir.
            return ListenableWorker.Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "finansim-daily-reminder"
        private val NOTIFY_AT: LocalTime = LocalTime.of(9, 0)

        /** Gunluk isi planlar; zaten planliysa dokunmaz. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMinutes(), TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /** Bir sonraki 09:00'a kalan sure. */
        private fun initialDelayMinutes(): Long {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(NOTIFY_AT)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next).toMinutes().coerceAtLeast(1L)
        }
    }
}
