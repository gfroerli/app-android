# Releasing

Set variables:

    $ export VERSION=X.Y.Z
    $ export GPG_KEY=20EE002D778AE197EF7D0D2CB993FF98A90C9AB1

Update version numbers:

    $ vim app/build.gradle

Update changelog:

    $ vim CHANGELOG.md

Add the changelog to `metadata/{de,en-US}/changelogs/<versioncode>.txt` as well
(try to stick to <500 chars).

Commit & tag:

    $ git commit -S${GPG_KEY} -m "Release v${VERSION}"
    $ git tag -s -u ${GPG_KEY} v${VERSION} -m "Version ${VERSION}"

Generate signed release artifacts:

    $ ./gradlew clean assembleRelease buildApksRelease

Test the signed APK.

    $ adb install app/build/outputs/apk/release/app-arm64-v8a-release.apk

Collect the release files:

    $ export RELEASEDIR=releases/$VERSION-$(grep -oP 'versionCode\s*=?\s*\K\d+' app/build.gradle)
    $ mkdir -p "$RELEASEDIR"
    $ cp -Rv app/build/outputs/{apk,apkset,bundle}/release/* $RELEASEDIR/
    $ for abi in arm64-v8a armeabi-v7a x86_64; do mv "$RELEASEDIR/app-${abi}-release.apk" "$RELEASEDIR/gfroerli-android-$VERSION-${abi}.apk"; done
    $ mv "$RELEASEDIR/app-release.apks" "$RELEASEDIR/gfroerli-android-$VERSION.apks"

Push the release:

    $ git push && git push --tags

For the releases:

- GitHub: Upload the signed per-ABI release APK files to GitHub releases
- Google Play: Upload the signed per-ABI release APK files
- Accrescent: Upload the signed release APKS file
