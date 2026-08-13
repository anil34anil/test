package com.desert.finansim.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.core.content.edit

/**
 * Web app'in (assets/www) JS tarafindan cagrilir. Taksit ozetini
 * SharedPreferences'a yazar; ReminderWorker gercek arka plan
 * bildirimlerini bu veriden uretir (WebView acik olmasa da calisir).
 */
class AndroidBridge(private val context: Context) {

    @JavascriptInterface
    fun saveInstallments(json: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_INSTALLMENTS, json)
        }
    }

    companion object {
        const val PREFS = "ay_sonu_prefs"
        const val KEY_INSTALLMENTS = "installments_json"
        const val KEY_LAST_REMINDER_MONTH = "last_reminder_month"
    }
}
