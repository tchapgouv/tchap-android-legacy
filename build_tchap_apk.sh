#!/bin/bash

rm *.apk

./gradlew clean

output="vector/build/outputs/apk"

# Google Play | Protected | Without Voip | Without Pinning
#./gradlew assembleAppProtecteedWithoutvoipWithoutpinningRelease;       cp ${output}/AppProtecteedWithoutvoipWithoutpinning/release/vector-app-protecteed-withoutvoip-withoutpinning-release.apk       ./tchapProtectedWithoutVoipGooglePlay.apk

# Google Play | Protected |   With Voip  | Without Pinning
#./gradlew assembleAppProtecteedWithvoipWithoutpinningRelease;          cp ${output}/AppProtecteedWithvoipWithoutpinning/release/vector-app-protecteed-withvoip-withoutpinning-release.apk             ./tchapProtectedWithVoipGooglePlay.apk

# Google Play |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppPreprodWithoutvoipWithoutpinningRelease;          cp ${output}/AppPreprodWithoutvoipWithoutpinning/release/vector-app-preprod-withoutvoip-withoutpinning-release.apk             ./tchapPreprodWithoutVoipGooglePlay.apk

# Google Play |  Preprod  |   With Voip  | Without Pinning
#./gradlew assembleAppPreprodWithvoipWithoutpinningRelease;             cp ${output}/AppPreprodWithvoipWithoutpinning/release/vector-app-preprod-withvoip-withoutpinning-release.apk                   ./tchapPreprodWithVoipGooglePlay.apk

# Google Play |   Agent   | Without Voip | Without Pinning
#./gradlew assembleAppAgentWithoutvoipWithoutpinningRelease;            cp ${output}/AppAgentWithoutvoipWithoutpinning/release/vector-app-agent-withoutvoip-withoutpinning-release.apk                 ./tchapAgentWithoutVoipGooglePlay.apk

# Google Play |   Agent   |   With Voip  | Without Pinning
#./gradlew assembleAppAgentWithvoipWithoutpinningRelease;               cp ${output}/AppAgentWithvoipWithoutpinning/release/vector-app-agent-withvoip-withoutpinning-release.apk                       ./tchapAgentWithVoipGooglePlay.apk

# Google Play |   Agent   | Without Voip | With Pinning
./gradlew assembleAppAgentWithoutvoipWithpinningRelease;               cp ${output}/AppAgentWithoutvoipWithpinning/release/vector-app-agent-withoutvoip-withpinning-release.apk                 ./tchapAgentWithoutVoipWithPinningGooglePlay.apk

# Google Play |   Agent   |   With Voip  | With Pinning
#./gradlew assembleAppAgentWithvoipWithpinningRelease;                  cp ${output}/AppAgentWithvoipWithpinning/release/vector-app-agent-withvoip-withpinning-release.apk                       ./tchapAgentWithVoipWithPinningGooglePlay.apk


#    FDroid   | Protected | Without Voip | Without Pinning
./gradlew assembleAppfdroidProtecteedWithoutvoipWithoutpinningRelease; cp ${output}/AppfdroidProtecteedWithoutvoipWithoutpinning/release/vector-appfdroid-protecteed-withoutvoip-withoutpinning-release.apk ./tchapProtectedWithoutVoipFDroid.apk

#    FDroid   | Protected |   With Voip  | Without Pinning
#./gradlew assembleAppfdroidProtecteedWithvoipWithoutpinningRelease;    cp ${output}/AppfdroidProtecteedWithvoipWithoutpinning/release/vector-appfdroid-protecteed-withvoip-withoutpinning-release.apk       ./tchapProtectedWithVoipFDroid.apk

#    FDroid   |  Preprod  | Without Voip | Without Pinning
./gradlew assembleAppfdroidPreprodWithoutvoipWithoutpinningRelease;    cp ${output}/AppfdroidPreprodWithoutvoipWithoutpinning/release/vector-appfdroid-preprod-withoutvoip-withoutpinning-release.apk       ./tchapPreprodWithoutVoipFDroid.apk

#    FDroid   |  Preprod  |   With Voip  | Without Pinning
#./gradlew assembleAppfdroidPreprodWithvoipWithoutpinningRelease;       cp ${output}/AppfdroidPreprodWithvoipWithoutpinning/release/vector-appfdroid-preprod-withvoip-withoutpinning-release.apk             ./tchapPreprodWithVoipFDroid.apk

#    FDroid   |   Agent   | Without Voip | Without Pinning
./gradlew assembleAppfdroidAgentWithoutvoipWithoutpinningRelease;      cp ${output}/AppfdroidAgentWithoutvoipWithoutpinning/release/vector-appfdroid-agent-withoutvoip-withoutpinning-release.apk           ./tchapAgentWithoutVoipFDroid.apk

#    FDroid   |   Agent   |   With Voip  | Without Pinning
#./gradlew assembleAppfdroidAgentWithvoipWithoutpinningRelease;         cp ${output}/AppfdroidAgentWithvoipWithoutpinning/release/vector-appfdroid-agent-withvoip-withoutpinning-release.apk                 ./tchapAgentWithVoipFDroid.apk

#    FDroid   |   Agent   | Without Voip | With Pinning
#./gradlew assembleAppfdroidAgentWithoutvoipWithpinningRelease;         cp ${output}/AppfdroidAgentWithoutvoipWithpinning/release/vector-appfdroid-agent-withoutvoip-withpinning-release.apk           ./tchapAgentWithoutVoipWithPinningFDroid.apk

#    FDroid   |   Agent   |   With Voip  | With Pinning
#./gradlew assembleAppfdroidAgentWithvoipWithpinningRelease;            cp ${output}/AppfdroidAgentWithvoipWithpinning/release/vector-appfdroid-agent-withvoip-withpinning-release.apk                 ./tchapAgentWithVoipWithPinningFDroid.apk

