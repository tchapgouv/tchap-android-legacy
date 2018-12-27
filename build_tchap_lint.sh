#!/bin/bash
./gradlew clean

echo "Do all lint check"
./gradlew \
    lintAppProtecteedWithoutVoipWithoutPinningRelease \
    lintAppProtecteedWithVoipWithoutPinningRelease \
    lintAppPreprodWithoutVoipWithoutPinningRelease \
    lintAppPreprodWithVoipWithoutPinningRelease \
    lintAppAgentWithoutVoipWithoutPinningRelease \
    lintAppAgentWithVoipWithoutPinningRelease \
    lintAppfdroidProtecteedWithoutVoipWithoutPinningRelease \
    lintAppfdroidProtecteedWithVoipWithoutPinningRelease \
    lintAppfdroidPreprodWithoutVoipWithoutPinningRelease \
    lintAppfdroidPreprodWithVoipWithoutPinningRelease \
    lintAppfdroidAgentWithoutVoipWithoutPinningRelease \
    lintAppfdroidAgentWithVoipWithoutPinningRelease
