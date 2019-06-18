cd ..
rm -rf olm
git clone https://gitlab.matrix.org/matrix-org/olm.git
cd olm/android
echo ndk.dir=$ANDROID_HOME/ndk-bundle > local.properties
./gradlew assembleRelease

cd ../../tchap-android
cp ../olm/android/olm-sdk/build/outputs/aar/olm-sdk-release-3.1.2.aar  vector/libs/olm-sdk.aar


