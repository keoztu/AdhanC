# Salah Times Only / Sadece Namaz Vakitleri

Prayer times. Adhan. Qibla. Nothing else.

Android app (Kotlin, Jetpack Compose). minSdk 26, targetSdk 35.

## Building with GitHub Actions

Every push to `main` builds the app. Open the **Actions** tab, pick the latest run, and download:

- `salah-times-only-debug-apk`: install this on your phone to test. It installs alongside a release build (its ID ends in `.debug`).
- `salah-times-only-release`: the release APK and the `.aab` bundle for Google Play (signed once you add the secrets below).

The workflow also:
- downloads the GeoNames city list (about 28,000 places) and builds `app/src/main/assets/cities.tsv`;
- downloads the two Adhan recordings from Wikimedia Commons and makes full and 5-second versions;
- runs the prayer-time unit tests.

### Release signing (needed before Google Play)

Create a keystore once and keep it safe; you need the same key for every update.

```
keytool -genkeypair -v -keystore release.jks -alias salah -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks > release.jks.b64
```

In the repository: Settings > Secrets and variables > Actions, add
`SIGNING_KEYSTORE_BASE64` (contents of `release.jks.b64`), `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS` (`salah`) and `SIGNING_KEY_PASSWORD`.

## Before publishing

- `app/src/main/java/com/salahtimesonly/Config.kt`: set the support email and (optionally) the donation link.
- `applicationId` in `app/build.gradle.kts` is `com.salahtimesonly`; change it before the first Play upload if you prefer another.
- Raise `targetSdk` to the level Google Play currently requires.
- Listen to both Adhan recordings. If the short version should end somewhere other than 5 s, set `SHORT_SECONDS` in `scripts/fetch_adhan.sh`.

## Building locally

Open the folder in Android Studio, then run once:

```
python3 scripts/build_cities.py
bash scripts/fetch_adhan.sh      # needs ffmpeg
```

## Structure

- `calc/` prayer-time engine (pure Kotlin, unit-tested) and Qibla bearing
- `data/` settings (DataStore), offline city search, day model
- `schedule/` exact alarms, notification channels, full-Adhan playback service, reboot and time-change handling
- `widget/` the home-screen widget (Glance)
- `ui/` Compose screens in the "Evening" design

## Credits

Place data: GeoNames, CC BY 4.0. Adhan 1: "Beautiful adhan" by Adam-synagda (Wikimedia Commons, CC0). Adhan 2: "Azan" by Andrewler (Wikimedia Commons, CC BY-SA 4.0). Typeface: Readex Pro (SIL OFL 1.1).
