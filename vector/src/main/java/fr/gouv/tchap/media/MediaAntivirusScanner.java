/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gouv.tchap.media;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.crypto.EncryptedFileInfo;

import fr.gouv.tchap.model.MediaAntivirusScanStatus;

public class MediaAntivirusScanner {

    private static final String LOG_TAG = MediaAntivirusScanner.class.getSimpleName();

    private MediaAntivirusScanStatusDao mediaAntivirusScanStatusDao;

    /**
     * Get the current scan status for a media.
     * Trigger a scan if it is not already done.
     *
     * @param url       the media url.
     * @param callback  optional async response handler.
     * @return the current scan status.
     */
    public MediaAntivirusScanStatus.ScanStatus scanMedia(String url, @Nullable ApiCallback<Void> callback) {

        if (TextUtils.isEmpty(url)) {
            return MediaAntivirusScanStatus.ScanStatus.UNKNOWN;
        }

        MediaAntivirusScanStatus.ScanStatus scanStatus = mediaAntivirusScanStatusDao.getMediaAvScanByUrl(url);

        if (null != scanStatus) {
            return scanStatus;
        }

        return MediaAntivirusScanStatus.ScanStatus.IN_PROGRESS;

        // TODO trigger the scan, use the callback on result if any

    }

    /**
     * Get the current scan status for an encrypted media.
     * Trigger a scan if it is not already done.
     *
     * @param mediaInfo  the encrypted media information.
     * @param callback   optional async response handler.
     * @return the current scan status.
     */
    public MediaAntivirusScanStatus.ScanStatus scanEncryptedMedia(EncryptedFileInfo mediaInfo, @Nullable ApiCallback<Void> callback) {

        if (TextUtils.isEmpty(mediaInfo.url)) {
            return MediaAntivirusScanStatus.ScanStatus.UNKNOWN;
        }

        MediaAntivirusScanStatus.ScanStatus scanStatus = mediaAntivirusScanStatusDao.getMediaAvScanByUrl(mediaInfo.url);

        if (null != scanStatus) {
            return scanStatus;
        }

        return MediaAntivirusScanStatus.ScanStatus.IN_PROGRESS;

        // TODO trigger the scan, use the callback on result if any
    }

}

