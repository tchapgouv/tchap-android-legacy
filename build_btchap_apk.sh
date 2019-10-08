#!/bin/bash

rm *.apk

./gradlew clean

output="vector/build/outputs/apk"

# Google Play |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppPreprodWithoutvoipWithoutpinningRelease;          cp ${output}/AppPreprodWithoutvoipWithoutpinning/matrixorg/vector-app-preprod-withoutvoip-withoutpinning-matrixorg.apk             ./tchapPreprodWithoutVoipGooglePlay.apk

# Google Play |  Preprod  |   With Voip  | Without Pinning
./gradlew assembleAppPreprodWithvoipWithoutpinningRelease;             cp ${output}/AppPreprodWithvoipWithoutpinning/matrixorg/vector-app-preprod-withvoip-withoutpinning-matrixorg.apk                   ./tchapPreprodWithVoipGooglePlay.apk


#    FDroid   |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppfdroidPreprodWithoutvoipWithoutpinningRelease;    cp ${output}/AppfdroidPreprodWithoutvoipWithoutpinning/matrixorg/vector-appfdroid-preprod-withoutvoip-withoutpinning-matrixorg.apk       ./tchapPreprodWithoutVoipFDroid.apk

#    FDroid   |  Preprod  |   With Voip  | Without Pinning
./gradlew assembleAppfdroidPreprodWithvoipWithoutpinningRelease;       cp ${output}/AppfdroidPreprodWithvoipWithoutpinning/matrixorg/vector-appfdroid-preprod-withvoip-withoutpinning-matrixorg.apk             ./tchapPreprodWithVoipFDroid.apk
