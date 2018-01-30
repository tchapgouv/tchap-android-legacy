#!/bin/bash
rm *.apk
./gradlew clean 

./gradlew lintAppRelease assembleAppMatrixorg

#cp app/build/outputs/apk/app-alpha-matrixorg.apk ./alpha.apk
cp vector/build/outputs/apk/vector-app-matrixorg.apk ./tchapGooglePlay.apk