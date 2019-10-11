#!/bin/bash

rm *.apk

./gradlew clean

output="vector/build/outputs/apk"

# Google Play |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppPreprodWithoutvoipWithoutpinningRelease;          cp ${output}/AppPreprodWithoutvoipWithoutpinning/release/vector-app-preprod-withoutvoip-withoutpinning-release.apk             ./tchapPreprodWithoutVoipGooglePlay.apk

# Google Play |  Preprod  |   With Voip  | Without Pinning
./gradlew assembleAppPreprodWithvoipWithoutpinningRelease;             cp ${output}/AppPreprodWithvoipWithoutpinning/release/vector-app-preprod-withvoip-withoutpinning-release.apk                   ./tchapPreprodWithVoipGooglePlay.apk


#    FDroid   |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppfdroidPreprodWithoutvoipWithoutpinningRelease;    cp ${output}/AppfdroidPreprodWithoutvoipWithoutpinning/release/vector-appfdroid-preprod-withoutvoip-withoutpinning-release.apk       ./tchapPreprodWithoutVoipFDroid.apk

#    FDroid   |  Preprod  |   With Voip  | Without Pinning
./gradlew assembleAppfdroidPreprodWithvoipWithoutpinningRelease;       cp ${output}/AppfdroidPreprodWithvoipWithoutpinning/release/vector-appfdroid-preprod-withvoip-withoutpinning-release.apk             ./tchapPreprodWithVoipFDroid.apk
