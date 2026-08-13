# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# JS -> Kotlin kopru sinifi: metod adlari WebView tarafindan reflection ile cagrilir.
-keepclassmembers class com.desert.finansim.bridge.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}
