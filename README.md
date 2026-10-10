# 📟 RetroBit Launcher (Android Native v2.5.0)
> **The Ultimate Minimalist E-Paper Niagara-Style Launcher with Cyberpunk Retro Deck Aesthetics**

**RetroBit Launcher** menggabungkan ergonomi satu tangan legendaris dari **Niagara Launcher**, fitur produktivitas bebas distraksi terbaik dari **Minimalist Phone & Before Launcher**, serta estetika **Retro Cyberpunk Cyberdeck** dengan palet monokrom grayscale layar **E-Paper (E-Ink Carta)**.

---

## 🌟 Fitur Unggulan (Top Launchers Features)

1. **Stealth Auto-Hide Directory (Daftar Aplikasi Otomatis Hilang & Muncul Saat Disentuh):**
   - Layar default berada dalam mode **Cold Standby** murni (hanya menampilkan jam monospaced, baterai ASCII, kontrol musik, dan status telemetry).
   - Sentuh area tengah layar atau geser slider alfabet A–Z di kanan: daftar aplikasi Niagara langsung muncul seketika secara halus (*animated fade & expand*).
   - Setelah 4.5 detik tanpa interaksi, daftar aplikasi akan otomatis kembali tersembunyi untuk menjaga fokus pikiran dari distraksi (*pure digital zen*).
   - Dapat diatur ke mode `STEALTH: ON` atau `STEALTH: OFF` melalui tombol toolbar bawah.
2. **Ergonomic Niagara Curved Thumb Arc:**
   - Susunan aplikasi melengkung vertikal yang mengikuti jangkauan jempol secara natural berbasis fungsi sinus matematis.
2. **Fluid Alphabet Scrubber (A–Z Slider):**
   - Slider huruf di sisi kanan layar dengan respon getaran haptik mekanik native (`HapticFeedbackConstants.CLOCK_TICK`) dan balon indikator HUD mengambang.
3. **App Hiding & Detox Vault (Sembunyikan Aplikasi Distraksi):**
   - Sembunyikan aplikasi media sosial atau game yang memicu distraksi (*digital declutter*).
   - Akses kembali kapan saja melalui tab `#HIDDEN` atau tombol Vault di toolbar.
4. **Custom App Renaming / Aliases (Ganti Nama Aplikasi):**
   - Beri nama kustom pada aplikasi apapun (misal: "WhatsApp" -> "Transmission", "YouTube" -> "CyberTube", "Chrome" -> "Netrunner").
5. **Interactive Retro Media Player Widget (Cassette Deck):**
   - Kontrol musik langsung di home screen dengan animasi pita kaset `▰▱`, tombol Play/Pause/Skip, dan progress bar ASCII `[████████░░░░░░░░░░]`.
6. **Digital Wellbeing & Screentime Counter:**
   - Monitor waktu penggunaan layar (*screen time*) dan jumlah unlock harian langsung di bagian atas layar (`DETOX: 1h 38m TODAY // 14 UNLOCKS`).
7. **Category Tag Filter Chips:**
   - Filter cepat menggunakan tag: `#ALL`, `#FAV`, `#COMM`, `#NET`, `#DATA`, `#SYS`, `#MEDIA`, `#TOOL`, `#HIDDEN`.
8. **Inline Math Calculator di Search Bar:**
   - Ketik perhitungan langsung di search bar (misal: `> 45 * 12 + 8`) dan hasilnya langsung muncul seketika (`= 548`).
9. **Hardware E-Paper Refresh Cycle (Inversion Flash):**
   - Mensimulasikan kedipan inversi hitam-putih khas layar E-Ink Carta 1200 untuk membersihkan ghosting visual.
   - Dapat dipicu via tombol toolbar, tombol samping chassis, atau gestur **double-tap** pada layar!
10. **4 Skema Warna E-Paper Monokrom Grayscale:**
    - **Paper White (Kindle Standard):** Latar gading hangat + tinta karbon hitam pekat.
    - **Slate Onyx (Dark E-Ink):** Latar arang gelap hemat baterai untuk layar OLED/AMOLED.
    - **GameBoy Pocket (90s 2-Bit):** Nuansa hijau zaitun retro tahun 1996.
    - **Stark 1-Bit Noir:** Hitam dan putih mutlak tanpa gradasi abu-abu.
11. **Cyberdeck CLI Console (`>_ TERM`):**
    - Terminal konsol mini untuk menjalankan perintah: `help`, `refresh`, `calc`, `screentime`, `theme [mode]`, `clear`.
12. **Backup & Restore Konfigurasi:**
    - Ekspor dan impor data launcher (daftar favorit, custom label, aplikasi tersembunyi) dalam format JSON.
13. **Niagara-Style Inline Notification Preview:**
    - Mengintegrasikan `NotificationListenerService` (`NotificationMonitorService.kt`) sehingga pesan/chat yang masuk (seperti WhatsApp, Telegram, Signal) langsung muncul tepat di bawah nama aplikasi di daftar favorit (`» Nama: "Pesan..."`).
14. **ICE Breaker / Mindful Delay Pause (Digital Detox Anti-Kecanduan):**
    - Tambahkan jeda sadar 5–15 detik sebelum membuka aplikasi yang mencandu (seperti media sosial/game), meredakan impuls refleks membuka aplikasi tanpa sadar.
15. **Auto-Build APK via GitHub Actions CI/CD:**
    - File `.github/workflows/build-apk.yml` telah disertakan. Cukup push proyek ini ke repositori GitHub Anda, dan GitHub Actions akan secara otomatis mengompilasi dan menghasilkan file `.apk` siap download langsung ke smartphone tanpa perlu menginstal Android Studio di komputer Anda!

---

## ⚡ Cara Menghasilkan APK Tanpa Android Studio (via GitHub Actions)

Jika Anda tidak memiliki Android Studio di komputer:
1. Buat repositori baru di GitHub (misal: `RetroBit-Launcher`).
2. Ekstrak isi file `RetroBitLauncher-AndroidStudio.zip` dan push ke repositori GitHub Anda.
3. Buka tab **Actions** di repositori GitHub Anda.
4. Alur kerja **"Build RetroBit Launcher APK"** akan otomatis berjalan selama 2–3 menit.
5. Klik pada build yang selesai, lalu unduh artefak **`RetroBit-Launcher-Debug-APK`**.
6. Kirim file `.apk` tersebut ke smartphone Android Anda dan langsung instal!

---

## 🚀 Cara Membuka & Menjalankan di Android Studio

### Persyaratan:
- **Android Studio** (Hedgehog, Iguana, Koala, atau versi terbaru).
- **Target SDK:** Android 14 (API 34) | **Min SDK:** Android 8.0 (API 26).
- **JDK:** Java 17+.

### Langkah-langkah:
1. Unduh dan ekstrak file **`RetroBitLauncher-AndroidStudio.zip`**.
2. Buka **Android Studio** -> Pilih **File** -> **Open...** -> Pilih folder `RetroBitLauncher`.
3. Tunggu proses **Gradle Sync** selesai (sekitar 1 menit).
4. Sambungkan smartphone Android Anda dengan mengaktifkan **USB Debugging** (atau jalankan di Android Emulator).
5. Klik tombol **Run (▶)** di Android Studio.

---

## 📱 Menjadikan Sebagai Launcher Default HP

1. Buka **Pengaturan (Settings)** di smartphone Android Anda.
2. Masuk ke **Aplikasi (Apps)** -> **Aplikasi Default (Default Apps)**.
3. Pilih **Aplikasi Layar Utama (Home App)**.
4. Pilih **RetroBit Launcher**.
5. Tekan tombol Home; smartphone Anda kini menjadi perangkat cyberdeck retro berlayar e-paper!

---

## 📂 Struktur Direktori Proyek

```
RetroBitLauncher/
├── app/
│   ├── build.gradle.kts                 // Dependensi Jetpack Compose & Material 3
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml          // Filter CATEGORY_HOME, QUERY_ALL_PACKAGES
│       ├── java/com/retrobit/launcher/
│       │   ├── MainActivity.kt          // Entry point & lifecycle
│       │   ├── data/
│       │   │   ├── AppModel.kt          // Model aplikasi & custom alias
│       │   │   └── AppRepository.kt     // LauncherApps, Hidden apps, Rename, Backup
│       │   └── ui/
│       │       ├── MainLauncherScreen.kt // Layar utama dengan inline calc & filters
│       │       ├── theme/               // Palet warna E-Paper & Tipografi Monospace
│       │       └── components/          // Scrubber, CurvedList, Media, Header, Dialogs
│       └── res/                         // Ikon adaptive vector retro & strings
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```
