#!/bin/bash

rm *.apk

./gradlew clean

output="vector/build/outputs/apk"

# Google Play | Protected | Without Voip | Without Pinning
./gradlew assembleAppProtecteedWithoutvoipWithoutpinningMatrixorg;       cp ${output}/AppProtecteedWithoutvoipWithoutpinning/matrixorg/vector-app-protecteed-withoutvoip-withoutpinning-matrixorg.apk       ./tchapProtectedWithoutVoipGooglePlay.apk

# Google Play | Protected |   With Voip  | Without Pinning
./gradlew assembleAppProtecteedWithvoipWithoutpinningMatrixorg;          cp ${output}/AppProtecteedWithvoipWithoutpinning/matrixorg/vector-app-protecteed-withvoip-withoutpinning-matrixorg.apk             ./tchapProtectedWithVoipGooglePlay.apk

# Google Play |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppPreprodWithoutvoipWithoutpinningMatrixorg;          cp ${output}/AppPreprodWithoutvoipWithoutpinning/matrixorg/vector-app-preprod-withoutvoip-withoutpinning-matrixorg.apk             ./tchapPreprodWithoutVoipGooglePlay.apk

# Google Play |  Preprod  |   With Voip  | Without Pinning
./gradlew assembleAppPreprodWithvoipWithoutpinningMatrixorg;             cp ${output}/AppPreprodWithvoipWithoutpinning/matrixorg/vector-app-preprod-withvoip-withoutpinning-matrixorg.apk                   ./tchapPreprodWithVoipGooglePlay.apk

# Google Play |   Agent   | Without Voip | Without Pinning
./gradlew assembleAppAgentWithoutvoipWithoutpinningMatrixorg;            cp ${output}/AppAgentWithoutvoipWithoutpinning/matrixorg/vector-app-agent-withoutvoip-withoutpinning-matrixorg.apk                 ./tchapAgentWithoutVoipGooglePlay.apk

# Google Play |   Agent   |   With Voip  | Without Pinning
./gradlew assembleAppAgentWithvoipWithoutpinningMatrixorg;               cp ${output}/AppAgentWithvoipWithoutpinning/matrixorg/vector-app-agent-withvoip-withoutpinning-matrixorg.apk                       ./tchapAgentWithVoipGooglePlay.apk




#    FDroid   | Protected | Without Voip | Without Pinning
./gradlew assembleAppfdroidProtecteedWithoutvoipWithoutpinningMatrixorg; cp ${output}/AppfdroidProtecteedWithoutvoipWithoutpinning/matrixorg/vector-appfdroid-protecteed-withoutvoip-withoutpinning-matrixorg.apk ./tchapProtectedWithoutVoipFDroid.apk

#    FDroid   | Protected |   With Voip  | Without Pinning
./gradlew assembleAppfdroidProtecteedWithvoipWithoutpinningMatrixorg;    cp ${output}/AppfdroidProtecteedWithvoipWithoutpinning/matrixorg/vector-appfdroid-protecteed-withvoip-withoutpinning-matrixorg.apk       ./tchapProtectedWithVoipFDroid.apk

#    FDroid   |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppfdroidPreprodWithoutvoipWithoutpinningMatrixorg;    cp ${output}/AppfdroidPreprodWithoutvoipWithoutpinning/matrixorg/vector-appfdroid-preprod-withoutvoip-withoutpinning-matrixorg.apk       ./tchapPreprodWithoutVoipFDroid.apk

#    FDroid   |  Preprod  |   With Voip  | Without Pinning
./gradlew assembleAppfdroidPreprodWithvoipWithoutpinningMatrixorg;       cp ${output}/AppfdroidPreprodWithvoipWithoutpinning/matrixorg/vector-appfdroid-preprod-withvoip-withoutpinning-matrixorg.apk             ./tchapPreprodWithVoipFDroid.apk

#    FDroid   |   Agent   | Without Voip | Without Pinning
./gradlew assembleAppfdroidAgentWithoutvoipWithoutpinningMatrixorg;      cp ${output}/AppfdroidAgentWithoutvoipWithoutpinning/matrixorg/vector-appfdroid-agent-withoutvoip-withoutpinning-matrixorg.apk           ./tchapAgentWithoutVoipFDroid.apk

#    FDroid   |   Agent   |   With Voip  | Without Pinning
./gradlew assembleAppfdroidAgentWithvoipWithoutpinningMatrixorg;         cp ${output}/AppfdroidAgentWithvoipWithoutPinning/matrixorg/vector-appfdroid-agent-withvoip-withoutpinning-matrixorg.apk                 ./tchapAgentWithVoipFDroid.apk
