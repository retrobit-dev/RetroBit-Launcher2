# 📟 RetroBit Launcher (Android Native v2.5.0)
> **The Ultimate Minimalist E-Paper Niagara-Style Launcher with Cyberpunk Retro Deck Aesthetics**

**RetroBit Launcher** menggabungkan ergonomi satu tangan legendaris dari **Niagara Launcher**, fitur produktivitas bebas distraksi terbaik dari **Minimalist Phone & Before Launcher**, serta estetika **Retro Cyberpunk Cyberdeck** dengan palet monokrom grayscale layar **E-Paper (E-Ink Carta)**.

---

## 🌟 Fitur Unggulan (Top Launchers Features)

1. **Personal Deck + Stealth App Drawer:**
   - Launcher membuka homescreen Personal Deck berisi shortcut aplikasi dan widget Android yang dapat discroll.
   - Handle `A–Z` di tepi kanan membuka app drawer; handle menyusut otomatis setelah 2.5 detik dan tetap bisa diketuk atau digeser dari tepi.
   - App drawer dapat otomatis tersembunyi setelah 2–10 detik tanpa interaksi, atau dibiarkan terbuka. Atur cepat melalui ikon mata di toolbar atau tab `TOOLS`.
2. **Optional Ergonomic Niagara Curved Thumb Arc:**
   - Susunan aplikasi melengkung vertikal yang mengikuti jangkauan jempol secara natural berbasis fungsi sinus matematis; nonaktif secara default dan dapat diaktifkan di `APPEARANCE`.
3. **Fluid Alphabet Scrubber (A–Z Slider):**
   - Scrubber huruf di dalam app drawer dengan respon getaran haptik native (`HapticFeedbackConstants.CLOCK_TICK`) dan balon indikator HUD mengambang.
3. **App Hiding & Detox Vault (Sembunyikan Aplikasi Distraksi):**
   - Sembunyikan aplikasi media sosial atau game yang memicu distraksi (*digital declutter*).
   - Akses kembali kapan saja melalui tab `#HIDDEN` atau tombol Vault di toolbar.
4. **Custom App Renaming / Aliases (Ganti Nama Aplikasi):**
   - Beri nama kustom pada aplikasi apapun (misal: "WhatsApp" -> "Transmission", "YouTube" -> "CyberTube", "Chrome" -> "Netrunner").
5. **Interactive Retro Media Player Widget (Cassette Deck):**
   - Kontrol musik langsung di home screen dengan animasi pita kaset `▰▱`, tombol Play/Pause/Skip, dan progress bar ASCII `[████████░░░░░░░░░░]`.
6. **Digital Wellbeing & Screentime Counter:**
   - Waktu penggunaan layar hari ini dibaca dari Android Usage Stats dan tampil di bagian atas; waktu per aplikasi, Focus Mode, dan timer harian tersedia di Settings. Tidak menampilkan angka unlock yang tidak disediakan Android.
7. **Category Tag Filter Chips:**
   - Filter cepat menggunakan tag: `#ALL`, `#FAV`, `#COMM`, `#NET`, `#DATA`, `#SYS`, `#MEDIA`, `#TOOL`, `#HIDDEN`.
8. **Inline Math Calculator di Search Bar:**
   - Ketik perhitungan langsung di search bar (misal: `> 45 * 12 + 8`) dan hasilnya langsung muncul seketika (`= 548`).
9. **Hardware E-Paper Refresh Cycle (Inversion Flash):**
   - Mensimulasikan kedipan inversi hitam-putih khas layar E-Ink Carta 1200 untuk membersihkan ghosting visual.
   - Dapat dipicu melalui tombol refresh di toolbar. Double-tap layar homescreen disediakan untuk fitur kunci ponsel jika diaktifkan.
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

## 🧩 Feature Center & Integrasi Perangkat

Buka **SETTINGS** di toolbar bawah untuk panel lima tab: `PROFILE`, `WELLBEING`, `APPEARANCE`, `SERVICES`, dan `TOOLS`.

- **Home Workspace & Widget Deck:** widget berada di atas pinned apps. Panel `TIME PASSED` (`DAY`, `MONTH`, dan `YEAR`) menghitung waktu lokal dan diperbarui tiap menit; bar progresnya kembali selebar panel seperti sebelumnya, dengan fill dan latar kosong berbentuk segmen seperti indikator baterai, dan panel tampil tepat di atas kolom pencarian. Kotak tambah/atur disembunyikan; tahan lalu lepaskan area kosong Home untuk masuk mode edit, lalu tambah app/widget atau buka `WIDGET SETTINGS`; mode ini menampilkan kontrol edit (`✎`) dan hapus (`×`) pada widget, sedangkan pinned app hanya menampilkan ikon dan label. Geser ke kanan dari area Home yang kosong untuk membuka halaman widget penuh; kalender Monday-first memungkinkan memilih tanggal untuk melihat atau menambah agenda hari itu (titik menandai tanggal yang sudah memiliki acara), Notes tersimpan lokal, dan To-do bisa ditambah/centang/hapus langsung. Tahan lalu lepaskan area kosong di halaman widget untuk menampilkan/menyembunyikan serta mengurutkan modul Calendar, Notes, To-do, Weather, dan Media. Di `WIDGET SETTINGS`, pilih `STACK + SWIPE`, `SIDE SCROLL`, `VERTICAL`, atau `MANUAL ORDER`. Tahan ikon atau label pinned app untuk membuka menu edit/resize, ganti ikon, hapus dari Home, atau uninstall; tahan widget untuk mengedit, memindahkan, mengatur lebar/tinggi, atau menghapusnya. Kartu juga bisa langsung ditahan lalu diseret ke kartu sejenis atau area kosong untuk mengubah urutan; seret ke zona `DROP HERE TO REMOVE` untuk menghapusnya.
- **Custom App Icons:** tahan ikon atau label shortcut di Home untuk membuka menu aplikasi, lalu pilih `CHANGE ICON`. Alternatifnya, buka menu aplikasi dari drawer lalu pilih `CHANGE APP ICON`. Pilih gambar dari perangkat, atau telusuri dan pilih ikon mana pun dari resource drawable icon pack kompatibel yang terpasang—tidak harus cocok dengan mapping aplikasi di `appfilter.xml`. Pilihan manual hanya berlaku untuk aplikasi tersebut dan dipakai di Home serta drawer; reset mengembalikan icon pack global atau ikon sistem.
- **Toolbar & ARC:** toolbar bawah dibuat ringkas dengan tinggi tombol seragam dan padding isi tombol 10 dp yang seimbang pada keempat sisi; `REFRESH` dan `TERM` berada di kiri, `APPS` dan `SETTINGS` di kanan, serta ada jarak tambahan sebelum `SETTINGS`. Ikon seukuran label. ARC app-list dimatikan secara default dan dapat diaktifkan lewat `SETTINGS` → `APPEARANCE` → `ARC APP LIST`; pengaturan ini hanya memengaruhi app drawer.
- **Gestures:** handle `A–Z` di sisi kanan membuka app drawer dengan ketukan atau geser kiri dan otomatis menyusut menjadi garis tipis. Dari Home, swipe kanan membuka Widget Deck dan swipe kiri kembali; tombol Back juga kembali ke Home. Double-tap untuk mengunci, swipe cepat atas/bawah menjalankan aksi pilihan di Home, dan tahan lalu lepaskan area kosong untuk masuk/keluar mode edit. Aksi swipe atas/bawah dan sensitivitas jarak dapat diatur di tab `TOOLS`; waktu auto-hide drawer juga dapat disesuaikan. Double-tap lock memerlukan aktivasi Device Admin secara eksplisit di Android; aplikasi hanya meminta kebijakan `FORCE LOCK`, tidak meminta hak wipe/manage perangkat.

- **Voice Command (Indonesia / English):** tombol `MIC` di samping pencarian. Perintah meliputi `cari/search`, `buka/open [nama aplikasi]`, `tema/theme [light|dark|pocket|stark]`, `tampilkan aplikasi`, `refresh`, `matrix`, `tiles`, `todo`, `calendar`, dan `help`. Pilih bahasa `ID` atau `EN` di samping tombol mic.
- **Digital Wellbeing:** total waktu layar dan waktu per aplikasi dibaca dari Android Usage Stats. Aktifkan *Usage Access* di tab `WELLBEING`; di sana juga tersedia Focus Mode (hanya favorit) dan batas harian aplikasi 15/30/60 menit.
- **Music Player:** widget menghubungkan `MediaSession` aktif dan menyediakan play/pause/previous/next. Izinkan Notification Access agar Android mengizinkan akses ke sesi media.
- **Weather:** widget cuaca mengambil data aktual dari Open-Meteo tanpa API key. Ketuk widget untuk mengatur kota.
- **Network Monitor & Quick Controls:** status koneksi dan RSSI Wi-Fi ditampilkan secara aktual. `tiles` pada terminal membuka kontrol volume, brightness, flashlight, Wi-Fi, Bluetooth, dan airplane mode. Android mewajibkan konfirmasi di Settings untuk perubahan Wi-Fi/Bluetooth/airplane; perubahan brightness memerlukan special access.
- **Profiles:** simpan dan tukar snapshot Personal/Work/Gaming untuk favorit, aplikasi tersembunyi, dan alias di perangkat lokal.
- **Search:** pencarian fuzzy berperingkat tetap bekerja bersama filter kategori; baris hasil memakai kunci unik per activity launcher untuk menghindari bentrok jika satu paket mengekspos beberapa pintasan.
- **Easter eggs:** buka Matrix Rain dari tab `TOOLS` atau perintah `matrix`. Kode Konami dapat dimasukkan sebagai perintah terminal berurutan: `UP UP DOWN DOWN LEFT RIGHT LEFT RIGHT B A`.
- **Calendar & Todo:** kalender dimulai dari hari Senin, membaca agenda perangkat setelah izin baca diberikan, dan dapat menambahkan agenda sepanjang hari atau berdurasi dengan judul serta catatan ke kalender Android yang dapat ditulis (memerlukan izin baca/tulis). Agenda dapat diulang mingguan, bulanan, atau tahunan; jenis `BIRTHDAY` menjadi acara sepanjang hari dan berulang tahunan secara default, sementara `TASK` adalah agenda tenggat bergaya tugas yang disimpan di kalender pilihan (bukan tugas pada Google Tasks). Todo tetap disimpan secara lokal.
- **Appearance:** empat tema bawaan, ukuran ikon dan skala font launcher yang bisa diatur, dukungan icon pack format ADW `appfilter.xml`, pemilihan manual icon per aplikasi dari galeri atau resource ikon apa pun dalam icon pack yang terpasang, dan impor palet JSON. Skala font mempertahankan pengaturan aksesibilitas font Android; format palet yang diterima terlihat di `APPEARANCE`.
- **Cyberdeck Lockscreen:** prompt Android langsung terbuka untuk fingerprint atau face unlock; PIN lokal disembunyikan selama biometrik tersedia dan baru ditampilkan setelah autentikasi gagal. Jika biometrik tidak tersedia atau belum didaftarkan, PIN dapat langsung digunakan. Kartu Calendar, Notes, To-do, Weather, dan Media yang dipilih di Settings tampil langsung di lockscreen sebagai pratinjau baca-saja. Overlay ini hanya tampilan pengunci di dalam launcher dan **tidak menggantikan** lockscreen/keamanan Android.

Fitur yang memerlukan akses khusus hanya bekerja setelah pengguna memberi izin. Wi-Fi, Bluetooth, dan mode pesawat tidak dapat diubah diam-diam oleh aplikasi biasa di Android modern; launcher membuka panel Settings yang sesuai.

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
