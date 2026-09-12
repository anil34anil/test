package com.desert.finansim.work

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.desert.finansim.bridge.AndroidBridge
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Gunluk kontrol: ayin 20'sinden sonra, o ay icin henuz hatirlatilmamis
 * aktif taksitler varsa tek bir bildirim gosterir. Web app (assets/www)
 * her taksit degisikliginde ozetini AndroidBridge uzerinden buraya yazar;
 * bu is WebView acik olmasa da calisir.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        try {
            val today = LocalDate.now()
            if (today.dayOfMonth < REMINDER_FROM_DAY) return ListenableWorker.Result.success()

            val prefs = applicationContext.getSharedPreferences(AndroidBridge.PREFS, Context.MODE_PRIVATE)
            val monthKey = YearMonth.from(today).toString()
            if (prefs.getString(AndroidBridge.KEY_LAST_REMINDER_MONTH, null) == monthKey) {
                return ListenableWorker.Result.success()
            }

            val json = prefs.getString(AndroidBridge.KEY_INSTALLMENTS, null)
                ?: return ListenableWorker.Result.success()
            val array = runCatching { JSONArray(json) }.getOrNull()
                ?: return ListenableWorker.Result.success()
            if (array.length() == 0) return ListenableWorker.Result.success()

            val formatter = NumberFormat.getNumberInstance(Locale("tr", "TR"))
            var total = 0.0
            val lines = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val amount = obj.optDouble("monthlyAmount", 0.0)
                total += amount
                lines.add(obj.optString("name", "Taksit") + ": " + formatter.format(amount) + " ₺")
            }

            Notifications.ensureChannel(applicationContext)
            Notifications.showReminder(
                applicationContext,
                "Ay sonu yaklaşıyor",
                lines.joinToString(", ") + " — toplam " + formatter.format(total) + " ₺",
            )

            prefs.edit { putString(AndroidBridge.KEY_LAST_REMINDER_MONTH, monthKey) }
            return ListenableWorker.Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return ListenableWorker.Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "ay-sonu-daily-check"
        private const val REMINDER_FROM_DAY = 20
        private val CHECK_AT: LocalTime = LocalTime.of(10, 0)

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

        private fun initialDelayMinutes(): Long {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(CHECK_AT)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next).toMinutes().coerceAtLeast(1L)
        }
    }
}
