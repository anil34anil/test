# Finansım

Kişisel finans, gelir-gider ve borç takip uygulaması. Tamamen çevrimdışı çalışır;
tüm veriler yalnızca cihazda saklanır ve uygulamanın **internet izni yoktur**.

- **Paket adı:** `com.desert.finansim`
- **Dil:** Türkçe · **Para birimi:** TRY (₺, ayarlardan değiştirilebilir)
- **Minimum Android:** 8.0 (API 26) · **Hedef:** Android 15 (API 35)

---

## 📱 APK'yı indir (bilgisayar gerekmez)

Her push'ta GitHub Actions uygulamayı derler, testleri çalıştırır ve imzalı
release APK'yı aynı adrese yükler:

**https://github.com/anil34anil/test/releases/download/apk-latest/finansim.apk**

Telefonda linke dokunun → indirin → açın. Android "bilinmeyen kaynaklardan
kurulum" izni isterse tarayıcınıza bu izni verin. Link sabittir; her zaman en
son derlemeyi verir.

İş akışı: [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml)

### İmzalama anahtarı nerede?

Depo public olduğu için imzalama anahtarı **repoya konmamıştır** — konsaydı
herkes bu uygulama adına APK imzalayabilirdi. Anahtar ilk derlemede üretilip
GitHub Actions cache'inde saklanır; sonraki derlemeler aynı anahtarı kullanır,
böylece yeni sürümler mevcut kurulumun üzerine güncelleme olarak iner.

Uzun süre derleme yapılmazsa cache düşebilir ve yeni bir anahtar üretilir. Bu
durumda yeni APK "farklı imza" hatası verir; uygulamayı kaldırıp yeniden
kurmanız gerekir. Önce **Ayarlar → Verileri yedekle** ile JSON yedeği alın,
kurulumdan sonra geri yükleyin.

Kalıcı bir anahtar isterseniz kendi keystore'unuzu üretip `ANDROID_KEYSTORE_BASE64`
gibi bir GitHub secret'ına koyabilir ve iş akışını onu kullanacak şekilde
değiştirebilirsiniz.

---

## Gereksinimler

| Araç | Sürüm |
|---|---|
| JDK | 17 (veya üzeri) |
| Android SDK Platform | 35 |
| Android Build Tools | 35.0.0 |
| Gradle | 8.11.1 (wrapper ile gelir) |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |

Android Studio kullanıyorsanız (Ladybug veya üzeri) bu bileşenler SDK Manager
üzerinden kurulur.

## Kurulum

```bash
git clone <repo-url>
cd <repo-dizini>
```

Android SDK yolunu tanımlayın — ya `ANDROID_HOME` ortam değişkeniyle ya da
proje kökünde bir `local.properties` dosyası oluşturarak:

```properties
sdk.dir=/home/kullanici/Android/Sdk
```

> `local.properties` sürüm kontrolüne girmez, `.gitignore` içindedir.

## Çalıştırma

Emülatör veya USB hata ayıklaması açık bir cihaz bağlıyken:

```bash
./gradlew installDebug     # derler ve bağlı cihaza kurar
```

Sadece derlemek için:

```bash
./gradlew assembleDebug
```

Testleri çalıştırmak için:

```bash
./gradlew test
```

## Debug APK oluşturma

```bash
./gradlew assembleDebug
```

Çıktı: `app/build/outputs/apk/debug/app-debug.apk`

Debug sürümü ayrı bir paket adıyla (`com.desert.finansim.debug`) kurulur, bu
yüzden release sürümüyle aynı cihazda yan yana durabilir.

## Release APK oluşturma

### 1. İmzalama anahtarı oluşturun (bir kez)

```bash
keytool -genkeypair -v \
  -keystore finansim-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias finansim
```

Komut sizden bir parola ve kimlik bilgileri isteyecek. Ürettiği
`finansim-release.jks` dosyasını **kaybetmeyin** — aynı uygulamanın sonraki
sürümlerini güncelleme olarak kurabilmek için aynı anahtar gerekir.

### 2. `keystore.properties` dosyasını oluşturun

Proje kökünde (`settings.gradle.kts` ile aynı dizinde):

```properties
storeFile=finansim-release.jks
storePassword=ANAHTAR_DEPOSU_PAROLASI
keyAlias=finansim
keyPassword=ANAHTAR_PAROLASI
```

> Bu dosya ve `.jks` uzantılı anahtarlar `.gitignore` içindedir; repoya
> **girmezler**. Parolalarınızı repoya eklemeyin.

### 3. APK'yı üretin

```bash
./gradlew assembleRelease
```

Çıktı: **`app/build/outputs/apk/release/app-release.apk`**

`keystore.properties` yoksa build yine çalışır ama APK imzasız üretilir
(`app-release-unsigned.apk`) ve cihaza kurulamaz. Kurulabilir bir dosya için
2. adımı atlamayın.

### Kod küçültme (R8) hakkında

Release yapılandırmasında `isMinifyEnabled = false` bırakıldı: doğrulanmamış R8
kurallarının çalışma anında Room/Compose/serialization tarafında çökmeye yol
açma riski var, APK'nın kurulabilir ve çalışır olması önceliklendirildi.
Şu anki APK ~12,8 MB.

Boyutu küçültmek isterseniz `app/build.gradle.kts` içinde
`isMinifyEnabled = true` yapın; gerekli keep kuralları
`app/proguard-rules.pro` dosyasında hazır bekliyor. Açtıktan sonra release
APK'yı cihazda mutlaka baştan sona test edin — R8 sorunları yalnızca çalışma
anında ortaya çıkar, derleme yeşil görünür.

## APK'yı cihaza kurma

**ADB ile (bilgisayara bağlıyken):**

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Elle kurulum:**

1. `app-release.apk` dosyasını telefona kopyalayın (USB, e-posta, bulut...).
2. Telefonda dosyaya dokunun.
3. Android "bilinmeyen kaynaklardan kuruluma" izin vermenizi isteyecek —
   ilgili uygulamaya (Dosyalar, Chrome vb.) bu izni verin.
4. Kur'a basın.

> Daha önce farklı bir anahtarla imzalanmış bir sürüm kuruluysa önce onu
> kaldırmanız gerekir; Android farklı imzayla güncellemeye izin vermez.

---

## Mimari

```
app/src/main/java/com/desert/finansim/
├── FinansimApp.kt              Application: bağımlılık kabı, ilk kurulum, iş planlama
├── MainActivity.kt             Tek Activity + onboarding kapısı
├── di/AppContainer.kt          Elle kurulan bağımlılık kabı
├── domain/
│   ├── Money.kt                Kuruş tabanlı para: biçimleme, ayrıştırma, taksit bölme
│   ├── DateUtils.kt            Türkçe tarih biçimleri, ay anahtarları
│   └── model/                  Enum'lar ve türetilmiş modeller (özet, gösterim modelleri...)
├── data/
│   ├── local/                  Room: entity'ler, DAO'lar, converter'lar, veritabanı
│   ├── repository/             Depolar + gösterge paneli hesaplamaları
│   └── backup/                 JSON yedekleme/geri yükleme
├── ui/
│   ├── theme/                  Material 3 renkleri, tipografi, açık/koyu tema
│   ├── components/             Ortak bileşenler (tutar alanı, işlem satırı, form alanları...)
│   ├── navigation/             Rotalar, alt menü, + butonu
│   └── screens/                Ekranlar (her biri kendi ViewModel'i ile)
└── work/                       Bildirimler ve günlük WorkManager işi
```

**Katmanlar:** Compose UI → ViewModel (StateFlow) → Repository → Room/DataStore.
Bağımlılık enjeksiyonu için Hilt yerine tek bir `AppContainer` kullanıldı;
uygulamanın boyutunda daha az dolaylılık ve daha hızlı derleme sağlıyor.

### Muhasebe kuralları (önemli)

Uygulamanın en kritik kararı, **gerçek para hareketleri** ile **finansal
yükümlülükleri** ayırmasıdır:

| Eylem | Para hareketi oluşur mu? |
|---|---|
| Borç oluşturma (ör. 10.000 TL kredi) | **Hayır** — sadece yükümlülük tanımlanır |
| Borç/taksit ödeme | Evet — nakit çıkışı (`DEBT_PAYMENT`) |

Bu yüzden ana ekranda "Gider" ile "Borç Ödemesi" ayrı satırlarda gösterilir ve
**Kalan = Gelir − (Gider + Borç Ödemesi)** olarak hesaplanır.

Tüm tutarlar veritabanında `Long` tipinde **kuruş** cinsinden tutulur; kayan
noktalı sayı hiç kullanılmaz, böylece toplamlarda yuvarlama hatası oluşmaz.
Taksit bölmede kuruş artığı son taksite eklenir, yani taksitlerin toplamı her
zaman ana tutara eşittir.

### Veri modeli

`categories`, `transactions`, `debts`, `installments`.

Ödenen tutarlar için ayrı sütun tutulmaz; bunlar `transactions` tablosundan
türetilir. Böylece iki kaynak arasında tutarsızlık oluşamaz.

Şema sürümü `FinansimDatabase.VERSION` ile yönetilir (şu an `2`). Genel kural
`fallbackToDestructiveMigration` kullanmamaktır — veri kaybına yol açar. `1`
sürümünden `2`'ye geçişte tek seferlik bir istisna yapıldı: uygulama
basitleştirilirken 4 tablo tamamen kalktı ve kalan tablolardan sütunlar
silindi; o noktada cihazlarda henüz gerçek kullanıcı verisi olmadığından
elle migration yazmak yerine `fallbackToDestructiveMigration(dropAllTables =
true)` tercih edildi. **Bu istisna yalnızca 1→2 geçişi içindir** — `VERSION`
3 ve sonrasında şema değişirse `FinansimDatabase.MIGRATIONS` dizisine gerçek
bir `Migration` eklenmelidir.

## Testler

```bash
./gradlew test
```

`app/src/test/` altında şartnamedeki kabul senaryoları test edilir. Her CI
derlemesinde çalışır; 20 testin tamamı geçmektedir. Testler kırmızıysa iş akışı
da kırmızıya döner (APK yine üretilir ama sonuç gizlenmez):

| Test | Senaryo |
|---|---|
| `FinancialScenariosTest` | 1: gelir 50.000 / gider 10.000 → net 40.000 |
| `FinancialScenariosTest` | 2: 12.000 TL borç, 12 taksit, ilki ödenince kalan 11.000 |
| `FinancialScenariosTest` | Taksit durumu (gecikmiş/bekleyen/ödendi) bugüne göre türetilir |
| `MoneyTest` | Para biçimleme/ayrıştırma, taksit bölme, kuruş artığı |
| `MoneyTest` | Tutar alanı girişi temizleme (`sanitizeAmountInput`) hiçbir karakter eklemez — IME bozulmasını önleyen düzeltmenin testi |

## Güvenlik ve gizlilik

- `AndroidManifest.xml` içinde **`INTERNET` izni yoktur**; uygulama ağ bağlantısı açamaz.
- Bulut yedeklemesi ve cihaz aktarımı `data_extraction_rules.xml` ile kapatılmıştır.
- Uygulama içi PIN/biyometrik kilit yoktur; veriler cihazın kendi ekran
  kilidiyle korunur.

## Yedekleme

- **Ayarlar → Verileri yedekle**: tüm veriyi JSON olarak istediğiniz konuma kaydeder
  (Android dosya seçici kullanılır, ek depolama izni gerekmez).
- **Ayarlar → Yedekten geri yükle**: dosya önce doğrulanır, sonra ne yapılacağı
  sorulur — *Ekle* (mevcut kayıtlar korunur) veya *Sil ve geri yükle* (her şey
  silinip yedek birebir yerine konur, ayrıca onay istenir).
- Geri yükleme tek bir veritabanı işlemi içinde yapılır: hata olursa hiçbir şey
  değişmez, mevcut veri olduğu gibi kalır.

## Uygulama adını / paket adını değiştirme

- Görünen ad: `app/src/main/res/values/strings.xml` → `app_name`
- Paket adı: `app/build.gradle.kts` → `namespace` ve `applicationId`
  (Android Studio'da `Refactor > Rename` ile kaynak dizinlerini de taşıyın)
