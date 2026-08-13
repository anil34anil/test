package com.desert.finansim

import android.app.Application
import com.desert.finansim.work.Notifications
import com.desert.finansim.work.ReminderWorker

class FinansimApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        ReminderWorker.schedule(this)
    }
}
