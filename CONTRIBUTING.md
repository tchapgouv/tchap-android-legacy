## Android Studio settings

Please set the "hard wrap" setting of Android Studio to 160 chars, this is the setting we use internally to format the source code (Menu `Settings/Editor/Code Style` then `Hard wrap at`).

## Compilation

tchap-android uses by default the Matrix Android SDK library (see tchap-android/vector/libs/).
At each release, this library is updated.
Between two releases, the tchap-android code may not compile due to evolution of the library API.
To compile against the source of the Matrix Android library, please clone the project [matrix-android-sdk](https://github.com/matrix-org/matrix-android-sdk)
 and run the following command:

> ./compile_with_sdk_project.sh

## I want to submit a PR to fix an issue

Please check if a corresponding issue exists. If yes, please let us know in a comment that you're working on it.
If an issue does not exist yet, it may be relevant to open a new issue and let us know that you're implementing it.

### Kotlin

Please write every new classes in Kotlin. You can also convert existing Java classes (limited to classes impacted by the PR) to Kotlin (Android Studio has a tool to do this), but if you do so, please do atomic commit of the conversion, to ensure there is no other change, it will facilitate code review.
Also please check that everything works fine after Kotlin conversion, especially regarding nullity check.

### TCHAP_CHANGES.rst

Please add a line to the top of the file `TCHAP_CHANGES.rst` describing your change.

### Tests

Please test your change on an Android device (or Android emulator) running with API 21. Many issues can happen (including crashes) on older devices.
Also, if possible, please test your change on a real device. Testing on Android emulator may not be sufficient.

### Internationalisation

When adding new string resources, please only add new entries in the 2 files: `value/strings.xml` and `values-fr/strings.xml`.
Do not hesitate to use plurals when appropriate.

### Layout

Also please check that the colors are ok for all the current themes of Tchap. Please use `?attr` instead of `@color` to reference colors in the layout. You can check this in the layout editor preview by selecting all the main themes (`AppTheme.Status`, `AppTheme.Dark`, etc.).

### Authors

Feel free to add an entry in file AUTHORS.rst

## Thanks

Thanks for contributing to Tchap projects!
