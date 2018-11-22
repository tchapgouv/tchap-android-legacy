#!/bin/bash
rm *.apk
./gradlew clean 

./gradlew lintAppWithVoipWithoutPinningRelease assembleAppWithVoipWithoutPinningMatrixorg
./gradlew lintAppfdroidWithVoipWithoutPinningRelease assembleAppfdroidWithVoipWithoutPinningMatrixorg

#cp the resulting apk
cp vector/build/outputs/apk/appWithVoipWithoutPinning/matrixorg/vector-app-withVoip-withoutPinning-matrixorg.apk ./tchapGooglePlay.apk
cp vector/build/outputs/apk/appfdroidWithVoipWithoutPinning/matrixorg/vector-appfdroid-withVoip-withoutPinning-matrixorg.apk ./tchapFDroid.apk