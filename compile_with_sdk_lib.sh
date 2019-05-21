echo replace step 1
sed -i '' -e "s/^include ':matrix-sdk'/\/\/include ':matrix-sdk'/" settings.gradle || true
sed -i '' -e "s/^project(':matrix-sdk')/\/\/project(':matrix-sdk')/" settings.gradle || true
sed -i '' -e "s/^include ':matrix-sdk-core'/\/\/include ':matrix-sdk-core'/" settings.gradle || true
sed -i '' -e "s/^project(':matrix-sdk-core')/\/\/project(':matrix-sdk-core')/" settings.gradle || true
sed -i '' -e "s/^include ':matrix-sdk-crypto'/\/\/include ':matrix-sdk-crypto'/" settings.gradle || true
sed -i '' -e "s/^project(':matrix-sdk-crypto')/\/\/project(':matrix-sdk-crypto')/" settings.gradle || true

echo replace step 2
sed -i '' -e "s/^    \/\/implementation(name: 'matrix/    implementation(name: 'matrix/" vector/build.gradle || true
sed -i '' -e "s/^    implementation project(':matrix-/    \/\/implementation project(':matrix-/" vector/build.gradle || true
sed -i '' -e "s/^    \/\/implementation(name: 'matrix-sdk-core/    implementation(name: 'matrix-sdk-core/" vector/build.gradle || true
sed -i '' -e "s/^    implementation project(':matrix-sdk-core-/    \/\/implementation project(':matrix-sdk-core-/" vector/build.gradle || true
sed -i '' -e "s/^    \/\/implementation(name: 'matrix-sdk-crypto/    implementation(name: 'matrix-sdk-crypto/" vector/build.gradle || true
sed -i '' -e "s/^    implementation project(':matrix-sdk-crypto-/    \/\/implementation project(':matrix-sdk-crypto-/" vector/build.gradle || true
sed -i '' -e "s/^    \/\/implementation(name: 'olm-sdk/    implementation(name: 'olm-sdk/" vector/build.gradle || true

echo "please sync project with gradle files"
