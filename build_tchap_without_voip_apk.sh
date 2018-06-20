#!/bin/bash
rm *.apk
./gradlew clean 

./gradlew lintAppWithoutVoipRelease assembleAppWithoutVoipMatrixorg
./gradlew lintAppfdroidWithoutVoipRelease assembleAppfdroidWithoutVoipMatrixorg

#cp resulting apk
cp vector/build/outputs/apk/appWithoutVoip/matrixorg/vector-app-withoutVoip-matrixorg.apk ./tchapWithoutVoipGooglePlay.apk
cp vector/build/outputs/apk/appfdroidWithoutVoip/matrixorg/vector-appfdroid-withoutVoip-matrixorg.apk ./tchapWithoutVoipFDroid.apk