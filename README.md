# Water Temperatures Android App

[![GitHub Actions][github-actions-badge]][github-actions]

Android app written in Kotlin. Displays temperature sensor data on a map
(MapLibre SDK, map data provided by Mapbox).

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

    ./gradlew installZhDebug

(Alternatively, do all that stuff through Android Studio.)

## Translations

The translations can be found in the XML resource files at
`app/src/main/res/values-<qualifier>/strings.xml`.

We prefer informal, simple language. For all languages, the Swiss variety
should be used (as indicated by the `-rCH` regional qualifier).

## License

Copyright © 2016–2025 Coredump Hackerspace.

Licensed under the GPLv3 or later, see `LICENSE.txt`.


<!-- Badges -->
[github-actions]: https://github.com/gfroerli/app-android/actions/workflows/test.yml
[github-actions-badge]: https://github.com/gfroerli/app-android/actions/workflows/test.yml/badge.svg
