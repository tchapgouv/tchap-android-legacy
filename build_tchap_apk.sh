#!/bin/bash
rm *.apk
./gradlew clean 

./gradlew lintAppWithVoipWithoutPinningRelease assembleAppWithVoipWithoutPinningMatrixorg
./gradlew lintAppfdroidWithVoipWithoutPinningRelease assembleAppfdroidWithVoipWithoutPinningMatrixorg

#cp the resulting apk
cp vector/build/outputs/apk/appWithVoip/matrixorg/vector-app-withVoip-withoutPinning-matrixorg.apk ./tchapGooglePlay.apk
cp vector/build/outputs/apk/appfdroidWithVoip/matrixorg/vector-appfdroid-withVoip-withoutPinning-matrixorg.apk ./tchapFDroid.apk