Tchap-Android
============

 Tchap-Android is an Android Matrix client.

 [<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Get it on Google Play" height="60">](https://play.google.com/store/apps/details?id=fr.gouv.tchap.a)

Contributing
============

Please refer to [CONTRIBUTING.md](https://github.com/dinsic-pim/tchap-android/blob/develop/CONTRIBUTING.md) if you want to contribute on the project!

Build instructions
==================

This client is a standard android studio project.

Several [flavorDimensions](https://github.com/dinsic-pim/tchap-android/blob/develop/vector/build.gradle#L143) are defined: "base", "target", "voip", "pinning".
- The 'base' dimension permits to deal with GooglePlay/Fdroid app
- The 'target' dimension permits to specify which platform are used
- The 'voip' flavor dimension permits to include/exclude jitsi at compilation time
- The 'pinning' flavor dimension permits to enable/disable certificate pinning with fingerprint check

If you want to compile the Google Play variant in command line with gradle, go to the project directory:

Debug mode:

`./gradlew assembleAppAgentWithoutvoipWithpinningDebug`

Release mode:

`./gradlew assembleAppAgentWithoutvoipWithpinningRelease`


Matrix Android SDK 
------------------

By default the tchap-android project will build with the current version of the Matrix SDK libs (matrix-sdk.aar, matrix-sdk-core.aar and matrix-sdk-crypto.aar) available in the tchap-android/vector/libs/ directory.

To compile the Matrix Android SDK with the tchap-android project:
- Clone the [matrix-android-sdk](https://github.com/matrix-org/matrix-android-sdk) repository in the same directory as tchap-android, and checkout the wanted branch or revision.
- Run the following script:

`sh compile_with_sdk_project.sh`

You may compile again with the available Matrix SDK libs by running:

`sh compile_with_sdk_lib.sh`

You may update/replace the Matrix SDK libs (in tchap-android/vector/libs/ dir) thanks to the following steps:
- Clone the [matrix-android-sdk](https://github.com/matrix-org/matrix-android-sdk) repository in the same directory as tchap-android, and checkout the wanted branch or revision.
- Run the dedicated script:

`sh build_matrix_sdk_lib.sh`

FAQ
===

1. What is the minimum android version supported?

    > the mininum SDK is 21 (Android 5.0 Lollipop)

2. Where the apk is generated?

	> tchap-android/vector/build/outputs/apk
