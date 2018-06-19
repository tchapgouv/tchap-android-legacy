#!/bin/bash
rm *.apk
./gradlew clean 

./gradlew lintAppWithVoipRelease assembleAppWithVoipMatrixorg
./gradlew lintAppfdroidWithVoipRelease assembleAppfdroidWithVoipMatrixorg

#cp the resulting apk
cp vector/build/outputs/apk/appWithVoip/matrixorg/vector-app-withVoip-matrixorg.apk ./tchapGooglePlay.apk
cp vector/build/outputs/apk/appfdroidWithVoip/matrixorg/vector-appfdroid-withVoip-matrixorg.apk ./tchapFDroid.apk