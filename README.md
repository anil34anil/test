# Finansım

Kişisel finans, gelir-gider ve borç takip uygulaması. Tamamen çevrimdışı çalışır;
tüm veriler yalnızca cihazda saklanır ve uygulamanın **internet izni yoktur**.

- **Paket adı:** `com.desert.finansim`
- **Dil:** Türkçe · **Para birimi:** TRY (₺, ayarlardan değiştirilebilir)
- **Minimum Android:** 8.0 (API 26) · **Hedef:** Android 15 (API 35)

---

## ⚠️ Derleme durumu — önce bunu okuyun

Bu proje **kaynak kod olarak tamamdır**, ancak geliştirildiği ortamda derlenememiştir:
o ortamın ağ politikası `dl.google.com` adresini engelliyor ve Android SDK
platformu, build-tools ve Android Gradle Plugin **yalnızca** oradan indirilebiliyor.

Bunun pratik anlamı:

- Kod **derleyiciden geçirilmemiştir**; ilk `assembleDebug` çalıştırmasında
  düzeltilmesi gereken derleme hataları çıkabilir.
- Aşağıdaki komutlar Android SDK'sı kurulu **sizin makinenizde** çalışacak
  şekilde yazılmıştır ve APK üretimi orada yapılmalıdır.

Android Studio'da projeyi açıp `Build > Make Project` demek, kalan hataları
görmenin en hızlı yoludur.

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

Release yapılandırmasında `isMinifyEnabled = false` bırakıldı. Sebep: proje bu
ortamda derlenip test edilemediği için, doğrulanmamış R8 kurallarının çalışma
anında Room/Compose/serialization tarafında çökmeye yol açma riski var. APK'nın
kurulabilir ve çalışır olması önceliklendirildi.

Uygulamayı bir kez sorunsuz çalıştırdıktan sonra `app/build.gradle.kts` içinde
`isMinifyEnabled = true` yapıp APK boyutunu küçültebilirsiniz; gerekli keep
kuralları `app/proguard-rules.pro` dosyasında hazır bekliyor. Açtıktan sonra
release APK'yı cihazda mutlaka bir kez baştan sona test edin.

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
├── MainActivity.kt             Tek Activity + kilit/onboarding kapısı
├── di/AppContainer.kt          Elle kurulan bağımlılık kabı
├── domain/
│   ├── Money.kt                Kuruş tabanlı para: biçimleme, ayrıştırma, taksit bölme
│   ├── DateUtils.kt            Türkçe tarih biçimleri, ay anahtarları
│   └── model/                  Enum'lar ve türetilmiş modeller (özet, plan, bütçe...)
├── data/
│   ├── local/                  Room: entity'ler, DAO'lar, converter'lar, veritabanı
│   ├── repository/             Depolar + analiz/rapor hesaplamaları + tekrar motoru
│   └── backup/                 JSON yedekleme/geri yükleme ve CSV dışa aktarma
├── ui/
│   ├── theme/                  Material 3 renkleri, tipografi, açık/koyu tema
│   ├── components/             Ortak bileşenler + Canvas ile çizilen grafikler
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
| Alacak oluşturma | **Hayır** — gelir değildir |
| Alacak tahsilatı | Evet — nakit girişi (`RECEIVABLE_COLLECTION`) |
| Kredi kartıyla harcama | Gider sayılır, **nakitten düşülmez** |
| Kredi kartı ekstresi ödeme | Evet — nakit çıkışı, kullanılan limiti azaltır |

Bu yüzden ana ekranda "Gider" ile "Borç Ödemesi" ayrı satırlarda gösterilir ve
**Kalan = Gelir − (Gider + Borç Ödemesi)** olarak hesaplanır.

Tüm tutarlar veritabanında `Long` tipinde **kuruş** cinsinden tutulur; kayan
noktalı sayı hiç kullanılmaz, böylece toplamlarda yuvarlama hatası oluşmaz.
Taksit bölmede kuruş artığı son taksite eklenir, yani taksitlerin toplamı her
zaman ana tutara eşittir.

### Veri modeli

`categories`, `transactions`, `debts`, `installments`, `credit_cards`,
`receivables`, `budgets`, `recurring_rules`.

Ödenen/tahsil edilen tutarlar için ayrı sütun tutulmaz; bunlar `transactions`
tablosundan türetilir. Böylece iki kaynak arasında tutarsızlık oluşamaz.

Şema sürümü `FinansimDatabase.VERSION` ile yönetilir. Veri kaybı riski nedeniyle
`fallbackToDestructiveMigration` **bilerek kullanılmamıştır**; şema değişince
`FinansimDatabase.MIGRATIONS` dizisine bir `Migration` eklemeniz gerekir (örneği
dosyanın içinde yorumda mevcuttur).

## Testler

```bash
./gradlew test
```

`app/src/test/` altında şartnamedeki kabul senaryoları test edilir:

| Test | Senaryo |
|---|---|
| `FinancialScenariosTest` | 1: gelir 50.000 / gider 10.000 → net 40.000 |
| `FinancialScenariosTest` | 2: 12.000 TL borç, 12 taksit, ilki ödenince kalan 11.000 |
| `FinancialScenariosTest` | 3: 5.000 TL alacaktan 2.000 tahsilat → kalan 3.000 |
| `FinancialScenariosTest` | 4: 50.000 limit, 5.000 harcama → kalan limit 45.000 |
| `RecurringScheduleTest` | 5: aylık kira sonraki ayda otomatik oluşur |
| `MoneyTest` | Para biçimleme/ayrıştırma, taksit bölme, kuruş artığı |

## Güvenlik ve gizlilik

- `AndroidManifest.xml` içinde **`INTERNET` izni yoktur**; uygulama ağ bağlantısı açamaz.
- Bulut yedeklemesi ve cihaz aktarımı `data_extraction_rules.xml` ile kapatılmıştır.
- PIN düz metin olarak saklanmaz: cihazda üretilen rastgele bir tuz ile
  PBKDF2-HMAC-SHA256 (120.000 tur) özeti tutulur, karşılaştırma sabit zamanlıdır.
- Biyometrik doğrulama sistemin `BiometricPrompt` bileşenine devredilmiştir.
- JSON yedeğine PIN özeti ve tuz **dahil edilmez**.

## Yedekleme

- **Ayarlar → Verileri yedekle**: tüm veriyi JSON olarak istediğiniz konuma kaydeder
  (Android dosya seçici kullanılır, ek depolama izni gerekmez).
- **Ayarlar → Yedekten geri yükle**: dosya önce doğrulanır, sonra ne yapılacağı
  sorulur — *Ekle* (mevcut kayıtlar korunur) veya *Sil ve geri yükle* (her şey
  silinip yedek birebir yerine konur, ayrıca onay istenir).
- Geri yükleme tek bir veritabanı işlemi içinde yapılır: hata olursa hiçbir şey
  değişmez, mevcut veri olduğu gibi kalır.
- **Ayarlar → CSV dışa aktar**: işlemleri Excel ile açılabilir biçimde verir
  (noktalı virgül ayracı ve UTF-8 BOM ile, Türkçe karakterler bozulmaz).

## Uygulama adını / paket adını değiştirme

- Görünen ad: `app/src/main/res/values/strings.xml` → `app_name`
- Paket adı: `app/build.gradle.kts` → `namespace` ve `applicationId`
  (Android Studio'da `Refactor > Rename` ile kaynak dizinlerini de taşıyın)
