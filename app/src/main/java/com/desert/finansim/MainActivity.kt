package com.desert.finansim

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.webkit.WebViewAssetLoader
import com.desert.finansim.bridge.AndroidBridge

/**
 * Tek Activity: butun uygulama assets/www icindeki web app'tir (bkz. README).
 * WebViewAssetLoader, dosyalarin https:// kokeninden servis edilmesini saglar;
 * boylece localStorage, Service Worker ve Notification API duz file:// yuklemede
 * calismayan sinirlar olmadan calisir.
 */
class MainActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)
            }
            webChromeClient = WebChromeClient()
            addJavascriptInterface(AndroidBridge(context), "AndroidBridge")
            loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
        }

        setContentView(webView)
    }
}
