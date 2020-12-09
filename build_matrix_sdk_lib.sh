#!/bin/bash

#
# Copyright 2018 New Vector Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# exit on any error
set -e

echo "Save current dir"
currentDir=`pwd`

echo "Go to matrix-android-sdk folder"
cd ../matrix-android-sdk

echo "The current matrix-android-sdk branch:"
git branch --show-current

echo "Build matrix sdk from source"
./gradlew clean assembleRelease

echo "Copy freshly built matrix sdk libs to the libs folder"
cd ${currentDir}
# Ensure the lib is updated by removing the previous one
rm vector/libs/matrix-sdk.aar
rm vector/libs/matrix-sdk-core.aar
rm vector/libs/matrix-sdk-crypto.aar

cp ../matrix-android-sdk/matrix-sdk/build/outputs/aar/matrix-sdk-release-*.aar vector/libs/matrix-sdk.aar
cp ../matrix-android-sdk/matrix-sdk-core/build/outputs/aar/matrix-sdk-core-release.aar vector/libs/matrix-sdk-core.aar
cp ../matrix-android-sdk/matrix-sdk-crypto/build/outputs/aar/matrix-sdk-crypto-release.aar vector/libs/matrix-sdk-crypto.aar
