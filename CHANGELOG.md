# Changelog

## v1.1.0 (2025-07-26)

- [feature] Show colored markers with temperature values directly on map (#97)
- [feature] Add French and Italian translations (#70)
- [feature] Support Android edge-to-edge mode (#91)
- [feature] Compensate for rounded device corners in UI (#101, #102)
- [change] Zoom level does not reset anymore when reloading data (#99)
- [change] Improve "About" screen (#102)
- [bug] Fix line wrapping of sensor preview caption text (#100)
- [chore] Upgrade many dependencies (Gradle, AGP, Kotlin, Compose, ...)
- [chore] Bump target API to 35

Note: This version increases the minimal required Android version from Android
5 to Android 8. Additionally, OpenGL ES 3.0 needs to be supported by your
device in order to properly render the map.

## v1.0.5 (2024-05-10)

- [feature] Add Swiss German translation (#66)
- [feature] Add support for app-specific languages (#66)
- [change] Exclude dependencies info when building APK (#67)

## v1.0.4 (2024-04-19)

- [bug] Properly close bottom sheet when tapping on map
- [change] Rounded corners for bottom sheet
- [chore] Upgrade many dependencies (Gradle, AGP, Kotlin, Compose, ...)
- [chore] Bump target API to 34

## v1.0.3 (2023-08-25)

- [bug] Fix display of overlapping sensors
- [chore] Upgrade MapLibre to get rid of proprietary transitive dependencies 
- [chore] Upgrade some dependencies (Gradle, AGP, appcompat)

## v1.0.2 (2023-08-23)

- [chore] Upgrade some dependencies (Gradle, AGP, appcompat)
- [chore] Bump target API to 33

## v1.0.1 (2022-05-21)

- [feature] Hide inactive sensors
- [chore] Upgrade dependencies

## v1.0.0 (2022-02-27)

- [change] Migrate entire UI to Jetpack Compose (#48)
- [feature] Add grab handle to bottom sheet
- [feature] Show app version in about dialog 
- [bug] Default zoom: Add more padding
- [bug] Fix crash when a bad API response is returned (#36)
- [bug] Linkify sponsor description (#50)
- [change] Build for target API 31 (#42)
- [change] About screen: Improve text (#49)
- [chore] Switch from Mapbox SDK to Maplibre SDK (#41)
- [chore] Update many dependencies

## v0.6.0 (2021-03-16)

- Adaptive app icon (#31)
- Show sponsor logos (#30)
- Increase minSdkVersion to 21 (Android 5)
- Use new API endpoints
- About screen: Link to GitHub
- UI fixes
- Disable MapBox telemetry (#22)
- Update many dependencies

## v0.5.1 (2020-09-22)

- Update many dependencies
- Show summary of aggregated data (#28)

## v0.5.0 (2019-01-18)

- Update icon
- Add loading indicator (#19)
- Handle data loading errors (#20)
- Fix bottom sheet behavior (#21)

## v0.4.1

- Rename app to "Gfrör.li"
- Android target SDK changed from 26 to 28
- Many dependency updates
