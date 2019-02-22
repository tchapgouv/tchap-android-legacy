#!/bin/bash
./gradlew clean

echo "Do all lint check"
./gradlew \
    lintAppProtecteedWithoutvoipWithoutpinningRelease \
    lintAppProtecteedWithvoipWithoutpinningRelease \
    lintAppPreprodWithoutvoipWithoutpinningRelease \
    lintAppPreprodWithvoipWithoutpinningRelease \
    #lintAppAgentWithoutvoipWithoutpinningRelease \
    #lintAppAgentWithvoipWithoutpinningRelease \
    lintAppAgentWithoutvoipWithpinningRelease \
    lintAppAgentWithvoipWithpinningRelease \
    lintAppfdroidProtecteedWithoutvoipWithoutpinningRelease \
    lintAppfdroidProtecteedWithvoipWithoutpinningRelease \
    lintAppfdroidPreprodWithoutvoipWithoutpinningRelease \
    lintAppfdroidPreprodWithvoipWithoutpinningRelease \
    #lintAppfdroidAgentWithoutvoipWithoutpinningRelease \
    #lintAppfdroidAgentWithvoipWithoutpinningRelease \
    lintAppfdroidAgentWithoutvoipWithpinningRelease \
    lintAppfdroidAgentWithvoipWithpinningRelease
