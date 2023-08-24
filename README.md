# Water Temperatures Android App

[![CircleCI][circle-ci-badge]][circle-ci]

Android app written in Kotlin. Displays temperature sensor data on a map (Maplibre SDK, map data
provided by Mapbox).

## Building

First, create a file at `app/secrets.properties` containing the API keys:

    echo "gfroerli_api_key_public=<VALUE>" >> app/secrets.properties
    echo "mapbox_access_token=<VALUE>" >> app/secrets.properties

Then, build the app with Gradle:

    ./gradlew build

To install the app to your device:

    ./gradlew installZhDebug

(Alternatively, do all that stuff through Android Studio.)

## License

Copyright © 2016–2023 Coredump Hackerspace.

Licensed under the GPLv3 or later, see `LICENSE.txt`.


<!-- Badges -->
[circle-ci]: https://circleci.com/gh/gfroerli/app-android/tree/master
[circle-ci-badge]: https://circleci.com/gh/gfroerli/app-android/tree/master.svg?style=shield
