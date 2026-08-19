# Releasing

## Tag Release

Set variables:

    $ export VERSION=X.Y.Z

Update version numbers:

    $ vim app/build.gradle.kts

Update changelog:

    $ vim CHANGELOG.md

Add the changelog to `metadata/{de,en-US}/changelogs/<versioncode>.txt` as well
(try to stick to <500 chars).

Commit & tag:

    $ git commit -m "Release v${VERSION}"
    $ git tag -a v${VERSION} -m "Version ${VERSION}"

## Build Release

To [get reproducible
releases](https://izzyondroid.org/docs/reproducibleBuilds/RBDevHints/), the
project should be built in a clean tree.

Clone to a new clean directory:

    $ git clone . ../gfroerli-android-build
    $ cp local.properties ../gfroerli-android-build/
    $ pushd ../gfroerli-android-build

Generate signed release artifacts:

    $ ./gradlew clean assembleRelease --no-build-cache --no-configuration-cache --no-daemon
    $ ./gradlew buildApksRelease --no-build-cache --no-configuration-cache --no-daemon

Test the signed APK.

    $ adb install app/build/outputs/apk/release/app-arm64-v8a-release.apk

Collect the release files:

    $ export RELEASEDIR=releases/$VERSION-$(grep -oP 'versionCode\s*=?\s*\K\d+' app/build.gradle.kts)
    $ mkdir -p "$RELEASEDIR"
    $ cp -Rv app/build/outputs/{apk,apkset,bundle}/release/* $RELEASEDIR/
    $ for abi in arm64-v8a armeabi-v7a x86_64 universal; do mv "$RELEASEDIR/app-${abi}-release.apk" "$RELEASEDIR/gfroerli-android-$VERSION-${abi}.apk"; done
    $ mv "$RELEASEDIR/app-release.apks" "$RELEASEDIR/gfroerli-android-$VERSION.apks"
    $ mv "$RELEASEDIR/app-release.aab" "$RELEASEDIR/gfroerli-android-$VERSION.aab"
    $ popd
    $ mv -v ../gfroerli-android-build/$RELEASEDIR releases/
    $ rm -rf ../gfroerli-android-build

Push the release:

    $ git push && git push --tags

For the releases:

- GitHub: Upload the signed per-ABI release APK files to GitHub releases
- Google Play: Upload the signed per-ABI release APK files
- Accrescent: Upload the signed release APKS file

## Android Auto

The app includes an Android Auto integration (POI category). For Google Play,
this requires a one-time opt-in in the Play Console (_Advanced settings >
Form factors > Android Auto_).

Releases touching the car app go through Google's [car app quality
review](https://developer.android.com/docs/quality-guidelines/car-app-quality),
which may delay the review of the first Android Auto release.

Note that car apps built with the Android for Cars App Library only run in a
real car when installed through Google Play — sideloaded builds are rejected
by Android Auto, regardless of the "unknown sources" developer setting. To
verify a release in a real car before promoting it to production, publish it
to the internal testing track first and install it from there. During
development, use the [Desktop Head Unit
(DHU)](https://developer.android.com/training/cars/testing/dhu) instead, which
runs sideloaded builds.
