# Gfrörli: Water Temperatures – Android App

<a href="https://github.com/gfroerli/app-android/actions/workflows/test.yml"><img height="20" src="https://github.com/gfroerli/app-android/actions/workflows/test.yml/badge.svg" alt="Build Status"></a>
<a href="https://shields.rbtlog.dev/ch.coredump.watertemp.zh"><img height="20" src="https://shields.rbtlog.dev/simple/ch.coredump.watertemp.zh?style=for-the-badge" alt="Reproducible Build Status"></a>

The app "Gfrör.li" displays current water temperatures of Swiss lakes and
rivers in dozends of different locations. Ideal for temperature-sensitive
people that still like swimming outside when the temperature is sufficiently
high!

The data displayed comes from our community-operated LoRaWAN based measuring
stations and is updated multiple times per hour. Additionally, we integrate
data from public measuring stations (like from the Swiss FOEN) when the license
permits us to do so. You can find more information about the project at
[gfrör.li](https://gfrör.li/).

<a href="https://accrescent.app/app/ch.coredump.watertemp.zh"><img height="80" src="https://accrescent.app/badges/get-it-on.png" alt="Get it on Accrescent"></a>
<a href="https://play.google.com/store/apps/details?id=ch.coredump.watertemp.zh"><img height="80" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play"></a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/ch.coredump.watertemp.zh"><img height="80" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it at IzzyOnDroid"></a>

## Building

First, create a file containing the API keys. You can use either:

- `app/secrets.properties` (project-specific, takes precedence)
- `~/.config/gfroerli-android/secrets.properties` (global fallback)

For example:

    echo "gfroerli_api_key_public=<VALUE>" >> app/secrets.properties
    echo "mapbox_access_token=<VALUE>" >> app/secrets.properties

Then, build the app with Gradle:

    ./gradlew build

To install the app to your device:

    ./gradlew installDebug

(Alternatively, do all that stuff through Android Studio.)

### Release APKs

To build a release APK, add the following entries to the `secrets.properties`
file (see above):

    keystoreFile=release.keystore
    keystorePassword=***
    keyAlias=the-key-alias
    keyPassword=***

Make sure that the `keystoreFile` path exists. Then run:

    ./gradlew clean assembleRelease

## Android Auto (Experimental)

The app includes an Android Auto integration that shows nearby water
temperatures and lets you navigate to a sensor location.

Note that Google requires apps built with the Android for Cars App Library to
be installed through Google Play: If the app was installed through Accrescent,
GitHub or IzzyOnDroid, it will not show up in Android Auto. Unlike for media
apps, this cannot be bypassed with the "Unknown sources" developer setting
([docs](https://developer.android.com/training/cars/testing#unknown-sources)).

For development, test the car app with the [Desktop Head Unit
(DHU)](https://developer.android.com/training/cars/testing/dhu), which does
run sideloaded builds. To test in a real car, the app must be installed
through Google Play (e.g. from the internal testing track).

## Development Tips

- If you use the Android Studio emulator, make sure to enable hardware graphics
  acceleration (_Edit > Additional Settings > Graphics acceleration >
  Hardware_), otherwise the sensors won't render correctly on the map.

## Translations

The translations can be found in the XML resource files at
`app/src/main/res/values-<qualifier>/strings.xml`.

We prefer informal, simple language. For all languages, the Swiss variety
should be used (as indicated by the `-rCH` regional qualifier).

## License

Copyright © 2016–2026 Coredump Hackerspace.

Licensed under the GPLv3 or later, see `LICENSE.txt`.
