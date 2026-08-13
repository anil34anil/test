package com.desert.finansim

import android.app.Application
import com.desert.finansim.di.AppContainer
import com.desert.finansim.work.Notifications
import com.desert.finansim.work.ReminderWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinansimApp : Application() {

    lateinit var container: AppContainer
        private set

    /**
     * Acilis isleri icin uygulama omurlu kapsam. Hatalar yutulur: acilista
     * olusan bir sorun uygulamanin baslamasini engellememeli.
     */
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }
    )

    override fun onCreate() {
        super.onCreate()

        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"

        container = AppContainer(this, versionName)

        Notifications.ensureChannel(this)

        appScope.launch {
            container.seedIfNeeded()
        }

        ReminderWorker.schedule(this)
    }
}
