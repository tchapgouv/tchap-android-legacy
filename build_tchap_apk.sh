#!/bin/bash

rm *.apk

./gradlew clean

output="vector/build/outputs/apk"

# Google Play | Protected | Without Voip | Without Pinning
./gradlew assembleAppProtecteedWithoutvoipWithoutpinningRelease;       cp ${output}/AppProtecteedWithoutvoipWithoutpinning/matrixorg/vector-app-protecteed-withoutvoip-withoutpinning-matrixorg.apk       ./tchapProtectedWithoutVoipGooglePlay.apk

# Google Play | Protected |   With Voip  | Without Pinning
./gradlew assembleAppProtecteedWithvoipWithoutpinningRelease;          cp ${output}/AppProtecteedWithvoipWithoutpinning/matrixorg/vector-app-protecteed-withvoip-withoutpinning-matrixorg.apk             ./tchapProtectedWithVoipGooglePlay.apk

# Google Play |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppPreprodWithoutvoipWithoutpinningRelease;          cp ${output}/AppPreprodWithoutvoipWithoutpinning/matrixorg/vector-app-preprod-withoutvoip-withoutpinning-matrixorg.apk             ./tchapPreprodWithoutVoipGooglePlay.apk

# Google Play |  Preprod  |   With Voip  | Without Pinning
./gradlew assembleAppPreprodWithvoipWithoutpinningRelease;             cp ${output}/AppPreprodWithvoipWithoutpinning/matrixorg/vector-app-preprod-withvoip-withoutpinning-matrixorg.apk                   ./tchapPreprodWithVoipGooglePlay.apk

# Google Play |   Agent   | Without Voip | Without Pinning
./gradlew assembleAppAgentWithoutvoipWithoutpinningRelease;            cp ${output}/AppAgentWithoutvoipWithoutpinning/matrixorg/vector-app-agent-withoutvoip-withoutpinning-matrixorg.apk                 ./tchapAgentWithoutVoipGooglePlay.apk

# Google Play |   Agent   |   With Voip  | Without Pinning
./gradlew assembleAppAgentWithvoipWithoutpinningRelease;               cp ${output}/AppAgentWithvoipWithoutpinning/matrixorg/vector-app-agent-withvoip-withoutpinning-matrixorg.apk                       ./tchapAgentWithVoipGooglePlay.apk

# Google Play |   Agent   | Without Voip | With Pinning
./gradlew assembleAppAgentWithoutvoipWithpinningRelease;               cp ${output}/AppAgentWithoutvoipWithpinning/matrixorg/vector-app-agent-withoutvoip-withpinning-matrixorg.apk                 ./tchapAgentWithoutVoipWithPinningGooglePlay.apk

# Google Play |   Agent   |   With Voip  | With Pinning
./gradlew assembleAppAgentWithvoipWithpinningRelease;                  cp ${output}/AppAgentWithvoipWithpinning/matrixorg/vector-app-agent-withvoip-withpinning-matrixorg.apk                       ./tchapAgentWithVoipWithPinningGooglePlay.apk


#    FDroid   | Protected | Without Voip | Without Pinning
./gradlew assembleAppfdroidProtecteedWithoutvoipWithoutpinningRelease; cp ${output}/AppfdroidProtecteedWithoutvoipWithoutpinning/matrixorg/vector-appfdroid-protecteed-withoutvoip-withoutpinning-matrixorg.apk ./tchapProtectedWithoutVoipFDroid.apk

#    FDroid   | Protected |   With Voip  | Without Pinning
./gradlew assembleAppfdroidProtecteedWithvoipWithoutpinningRelease;    cp ${output}/AppfdroidProtecteedWithvoipWithoutpinning/matrixorg/vector-appfdroid-protecteed-withvoip-withoutpinning-matrixorg.apk       ./tchapProtectedWithVoipFDroid.apk

#    FDroid   |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppfdroidPreprodWithoutvoipWithoutpinningRelease;    cp ${output}/AppfdroidPreprodWithoutvoipWithoutpinning/matrixorg/vector-appfdroid-preprod-withoutvoip-withoutpinning-matrixorg.apk       ./tchapPreprodWithoutVoipFDroid.apk

#    FDroid   |  Preprod  |   With Voip  | Without Pinning
./gradlew assembleAppfdroidPreprodWithvoipWithoutpinningRelease;       cp ${output}/AppfdroidPreprodWithvoipWithoutpinning/matrixorg/vector-appfdroid-preprod-withvoip-withoutpinning-matrixorg.apk             ./tchapPreprodWithVoipFDroid.apk

#    FDroid   |   Agent   | Without Voip | Without Pinning
./gradlew assembleAppfdroidAgentWithoutvoipWithoutpinningRelease;      cp ${output}/AppfdroidAgentWithoutvoipWithoutpinning/matrixorg/vector-appfdroid-agent-withoutvoip-withoutpinning-matrixorg.apk           ./tchapAgentWithoutVoipFDroid.apk

#    FDroid   |   Agent   |   With Voip  | Without Pinning
./gradlew assembleAppfdroidAgentWithvoipWithoutpinningRelease;         cp ${output}/AppfdroidAgentWithvoipWithoutpinning/matrixorg/vector-appfdroid-agent-withvoip-withoutpinning-matrixorg.apk                 ./tchapAgentWithVoipFDroid.apk

#    FDroid   |   Agent   | Without Voip | With Pinning
./gradlew assembleAppfdroidAgentWithoutvoipWithpinningRelease;         cp ${output}/AppfdroidAgentWithoutvoipWithpinning/matrixorg/vector-appfdroid-agent-withoutvoip-withpinning-matrixorg.apk           ./tchapAgentWithoutVoipWithPinningFDroid.apk

#    FDroid   |   Agent   |   With Voip  | With Pinning
./gradlew assembleAppfdroidAgentWithvoipWithpinningRelease;            cp ${output}/AppfdroidAgentWithvoipWithpinning/matrixorg/vector-appfdroid-agent-withvoip-withpinning-matrixorg.apk                 ./tchapAgentWithVoipWithPinningFDroid.apk

