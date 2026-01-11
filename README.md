# Gfrörli: Water Temperatures – Android App

[![GitHub Actions][github-actions-badge]][github-actions]

The app "Gfrör.li" displays current water temperatures of Swiss lakes and
rivers in dozends of different locations. Ideal for temperature-sensitive
people that still like swimming outside when the temperature is sufficiently
high!

The data displayed comes from our community-operated LoRaWAN based measuring
stations and is updated multiple times per hour. Additionally, we integrate
data from public measuring stations (like from the Swiss FOEN) when the license
permits us to do so. You can find more information about the project at
[gfrör.li](https://gfrör.li/).

<a href="https://accrescent.app/"><img width="200" src="https://githubraw.com/gfroerli/app-android/main/graphics/get-it-on-accrescent.png" alt="Get it on Accrescent"></a>
<a href="https://play.google.com/store/apps/details?id=ch.coredump.watertemp.zh"><img width="200" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play"></a>

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


<!-- Badges -->
[github-actions]: https://github.com/gfroerli/app-android/actions/workflows/test.yml
[github-actions-badge]: https://github.com/gfroerli/app-android/actions/workflows/test.yml/badge.svg
