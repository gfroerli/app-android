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

    $ adb install app/build/outputs/apk/release/app-release.apk

Push the release:

    $ git push --tags

For the releases:

- GitHub: Upload the signed release APK file to GitHub releases
- Google Play: Upload the signed release APK file
- Accrescent: Upload the signed release APKS file
