#!/bin/bash

rm *.apk

./gradlew clean

output="vector/build/outputs/apk"

# Google Play | Protected | Without Void | Without Pinning
./gradlew assembleAppProtecteedWithoutVoipWithoutPinningMatrixorg;       cp ${output}/AppProtecteedWithoutVoipWithoutPinning/matrixorg/vector-app-protecteed-withoutVoip-withoutPinning-matrixorg.apk       ./tchapProtectedWithoutVoidGooglePlay.apk

# Google Play | Protected |   With Void  | Without Pinning
./gradlew assembleAppProtecteedWithVoipWithoutPinningMatrixorg;          cp ${output}/AppProtecteedWithVoipWithoutPinning/matrixorg/vector-app-protecteed-withVoip-withoutPinning-matrixorg.apk             ./tchapProtectedWithVoidGooglePlay.apk

# Google Play |  Preprod  | Without Void | Without Pinning
./gradlew assembleAppPreprodWithoutVoipWithoutPinningMatrixorg;          cp ${output}/AppPreprodWithoutVoipWithoutPinning/matrixorg/vector-app-preprod-withoutVoip-withoutPinning-matrixorg.apk             ./tchapPreprodWithoutVoidGooglePlay.apk

# Google Play |  Preprod  |   With Void  | Without Pinning
./gradlew assembleAppPreprodWithVoipWithoutPinningMatrixorg;             cp ${output}/AppPreprodWithVoipWithoutPinning/matrixorg/vector-app-preprod-withVoip-withoutPinning-matrixorg.apk                   ./tchapPreprodWithVoidGooglePlay.apk

# Google Play |   Agent   | Without Void | Without Pinning
./gradlew assembleAppAgentWithoutVoipWithoutPinningMatrixorg;            cp ${output}/AppAgentWithoutVoipWithoutPinning/matrixorg/vector-app-agent-withoutVoip-withoutPinning-matrixorg.apk                 ./tchapAgentWithoutVoidGooglePlay.apk

# Google Play |   Agent   |   With Void  | Without Pinning
./gradlew assembleAppAgentWithVoipWithoutPinningMatrixorg;               cp ${output}/AppAgentWithVoipWithoutPinning/matrixorg/vector-app-agent-withVoip-withoutPinning-matrixorg.apk                       ./tchapAgentWithVoidGooglePlay.apk




#    FDroid   | Protected | Without Void | Without Pinning
./gradlew assembleAppfdroidProtecteedWithoutVoipWithoutPinningMatrixorg; cp ${output}/AppfdroidProtecteedWithoutVoipWithoutPinning/matrixorg/vector-app-protecteed-withoutVoip-withoutPinning-matrixorg.apk ./tchapProtectedWithoutVoidFDroid.apk

#    FDroid   | Protected |   With Void  | Without Pinning
./gradlew assembleAppfdroidProtecteedWithVoipWithoutPinningMatrixorg;    cp ${output}/AppfdroidProtecteedWithVoipWithoutPinning/matrixorg/vector-app-protecteed-withVoip-withoutPinning-matrixorg.apk       ./tchapProtectedWithVoidFDroid.apk

#    FDroid   |  Preprod  | Without Void | Without Pinning
./gradlew assembleAppfdroidPreprodWithoutVoipWithoutPinningMatrixorg;    cp ${output}/AppfdroidPreprodWithoutVoipWithoutPinning/matrixorg/vector-app-preprod-withoutVoip-withoutPinning-matrixorg.apk       ./tchapPreprodWithoutVoidFDroid.apk

#    FDroid   |  Preprod  |   With Void  | Without Pinning
./gradlew assembleAppfdroidPreprodWithVoipWithoutPinningMatrixorg;       cp ${output}/AppfdroidPreprodWithVoipWithoutPinning/matrixorg/vector-app-preprod-withVoip-withoutPinning-matrixorg.apk             ./tchapPreprodWithVoidFDroid.apk

#    FDroid   |   Agent   | Without Void | Without Pinning
./gradlew assembleAppfdroidAgentWithoutVoipWithoutPinningMatrixorg;      cp ${output}/AppfdroidAgentWithoutVoipWithoutPinning/matrixorg/vector-app-agent-withoutVoip-withoutPinning-matrixorg.apk           ./tchapAgentWithoutVoidFDroid.apk

#    FDroid   |   Agent   |   With Void  | Without Pinning
./gradlew assembleAppfdroidAgentWithVoipWithoutPinningMatrixorg;         cp ${output}/AppfdroidAgentWithVoipWithoutPinning/matrixorg/vector-app-agent-withVoip-withoutPinning-matrixorg.apk                 ./tchapAgentWithVoidFDroid.apk
