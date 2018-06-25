# Water Temperatures Android App

Work in progress. Uses Mapbox API.

## Building

First, create a file at `app/secrets.properties` containing the API keys:

    echo "gfroerli_api_key_public=<VALUE>" >> app/secrets.properties
    echo "mapbox_access_token=<VALUE>" >> app/secrets.properties

Then, build the app with Gradle:

    ./gradlew build

To install the app to your device:

    ./gradlew installZhDebug

(Alternatively, do all that stuff through Android Studio.)

## Dependency verification

To ensure the integrity of our dependencies, we use the
[gradle-witness](https://github.com/WhisperSystems/gradle-witness)
plugin by Open Whisper Systems. Therefore new dependencies
need to be updated in the `depencencyVerification` list in
`app/build.gradle`.

## License

Copyright © 2016–2018 Coredump Hackerspace.

Licensed under the GPLv3 or later, see `LICENSE.txt`.
