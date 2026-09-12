# Ay Sonu

Aylık gelir, gider ve taksitli giderlerini takip eden, ay sonunda elinde ne
kaldığını gösteren basit bir kişisel finans uygulaması. Tüm veriler yalnızca
cihazında kalır — hiçbir sunucuya, hiçbir hesaba bağlı değil.

## Neden bu mimari

Uygulama tek bir web app (`web/`) olarak yazılıyor ve iki yere paketleniyor:

- **Android**: `app/` altındaki ince bir WebView kabuğu, aynı web app'i
  `assets/www` içinden yükler ve GitHub Actions ile imzalı bir APK üretir.
- **iPhone / herhangi bir tarayıcı**: aynı `web/` klasörü doğrudan statik
  bir site olarak yayınlanır; Safari'den açılıp "Ana Ekrana Ekle" ile normal
  bir uygulama gibi kurulur (PWA).

İki ayrı native kod tabanı (Kotlin + Swift) yazmak yerine tek bir HTML/CSS/JS
kaynağı her iki platformda da aynı deneyimi verir. Gerçek bir iOS uygulaması
(App Store, .ipa) için Mac + Xcode + ücretli Apple Developer hesabı gerekir;
bu proje bunlara sahip değil, bu yüzden iPhone tarafı PWA'dır.

## Özellikler

- Bu ayki gelir/gider hareketlerini ekle, düzenle, sil.
- Taksitli gider planı oluştur (taksit tutarı + taksit sayısı → toplam
  otomatik hesaplanır); her aktif taksidin o ayki payı "Bu ay kalan"dan
  otomatik düşer.
- Taksitler sekmesinde ilerleme çubuğu, kaç taksit ödendiği/kaldığı, "Bu ayı
  öde" ile ilerletme, taksidi tamamen silme.
- Geçmiş sekmesi: bir ay bittiğinde (uygulama yeni bir ayda açıldığında)
  önceki ayın gelir/gider/taksit dökümü otomatik olarak buraya arşivlenir.
- Taksit hatırlatması: ayın 20'sinden sonra, henüz hatırlatılmamışsa aktif
  taksitleri özetleyen bir bildirim. Android'de bu **gerçek arka plan
  bildirimi**dir (uygulama kapalıyken de çalışır, WorkManager ile günde bir
  kontrol edilir); tarayıcı/iPhone'da ise uygulama açıldığında best-effort
  bir bildirim/banner olarak çalışır (web'in arka planda çalışma imkânı
  Android kadar geniş değildir).

## Proje yapısı

```
web/                          Tek gerçek kaynak: Android VE iPhone bundan üretilir
├── index.html                Tüm uygulama mantığı (localStorage kalıcılık,
│                              ay geçişi, taksit hesapları, bildirimler)
├── manifest.json              PWA manifesti (ikon, isim, tema rengi)
├── sw.js                      Service worker: offline önbellek + bildirim tıklaması
└── icons/                     Uygulama ikonları (192/512/apple-touch/favicon)

app/                           Android sarmalayıcı
├── src/main/assets/www/       web/ klasörünün birebir kopyası (derlemede paketlenir)
└── src/main/java/com/desert/finansim/
    ├── MainActivity.kt        Tek Activity: WebViewAssetLoader ile www/index.html'i yükler
    ├── FinansimApp.kt         Application: bildirim kanalı + günlük iş planlama
    ├── bridge/AndroidBridge.kt  JS → Kotlin köprüsü (taksit özetini SharedPreferences'a yazar)
    └── work/
        ├── ReminderWorker.kt  Günlük WorkManager job'ı: SharedPreferences'ı okuyup
        │                      gerçek bildirim gösterir (uygulama kapalıyken de çalışır)
        └── Notifications.kt   Bildirim kanalı + gösterim yardımcıları
```

`web/` klasörü değiştiğinde `app/src/main/assets/www/` içine de kopyalanması
gerekir (iki klasör bilerek aynı içeriktedir; Android tarafı offline
çalıştığı için derleme zamanında ayrı bir kopyalama adımına gerek duyulmadı).

## Veri ve gizlilik

- Veriler `localStorage`'da tutulur (Android'de WebView'in kendi disk
  alanında, iPhone'da Safari'nin PWA depolama alanında) — hiçbir sunucuya
  gönderilmez.
- `AndroidManifest.xml`'de `INTERNET` izni **yoktur**; Android tarafı ağ
  bağlantısı açamaz.
- Bulut yedeği ve cihaz aktarımı `data_extraction_rules.xml` ile kapalıdır.
- Taksit hatırlatması için Android'e yalnızca taksit adı ve aylık tutarı
  (SharedPreferences, `ay_sonu_prefs`) aktarılır; bu bilgi cihazdan çıkmaz.

## Geliştirme

- Web app'i yerelde test etmek için `web/` klasöründe basit bir statik
  sunucu yeterli (`python3 -m http.server` gibi) — harici bağımlılığı yok.
- Android tarafını derlemek için Android SDK gerekir; bu repoda yerel SDK
  erişimi olmadığından derleme GitHub Actions (`.github/workflows/build-apk.yml`)
  üzerinden yapılır: push'ta otomatik derlenip imzalı APK
  `releases/download/apk-latest/finansim.apk` adresine yayınlanır.
- İmzalama anahtarı repoda tutulmaz; ilk çalışmada üretilip Actions cache'inde
  saklanır, böylece sonraki derlemeler aynı anahtarla imzalanır ve APK
  güncelleme olarak kurulabilir.
