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
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.crypto.EncryptedFileInfo;

import fr.gouv.tchap.model.MediaScan;
import io.realm.Realm;

public class MediaScanManager {

    private static final String LOG_TAG = MediaScanManager.class.getSimpleName();

    MediaScanDao mMediaScanDao;

    /**
     * Constructor
     *
     * @param realm instance of mediaScanDao.
     */
    public MediaScanManager(Realm realm) {
        mMediaScanDao = new MediaScanDao(realm);
    }

    /**
     * Get the current scan result on a media (including antivirus status).
     * Trigger an antivirus scan if it is not already done and if a callback is provided.
     *
     * @param url       the media url.
     * @param callback  optional async response handler.
     * @return the current scan status for a dedicated media.
     */
    public MediaScan scanMedia(String url, @Nullable ApiCallback<Void> callback) {

        MediaScan mediaScan = mMediaScanDao.getMediaScan(url);

        if (null != callback && AntiVirusScanStatus.UNKNOWN != mediaScan.getAntiVirusScanStatus()) {

            // TODO trigger the scan, use the callback on result if any
            // TODO update the ScanAntiVirusScanStatus to IN_PROGRESS
            // TODO Case Error, return the updated AntiVirusScanStatus to UNKNOWN
        }

        return mediaScan;
    }

    /**
     * Get the current scan result on an encrypted media (including antivirus status).
     * Trigger an antivirus scan if it is not already done and if a callback is provided.
     *
     * @param mediaInfo  the encrypted media information.
     * @param callback   optional async response handler.
     * @return the current scan result for a dedicated encrypted media.
     */
    public MediaScan scanMedia(EncryptedFileInfo mediaInfo, @Nullable ApiCallback<Void> callback) {

        MediaScan mediaScan = mMediaScanDao.getMediaScan(mediaInfo.url);

        if (null != callback && AntiVirusScanStatus.UNKNOWN != mediaScan.getAntiVirusScanStatus()) {

            // TODO trigger the scan, use the callback on result if any
            // TODO update the AntiVirusScanStatus to IN_PROGRESS
            // TODO Case Error, return the updated AntiVirusScanStatus to UNKNOWN
        }

        return mediaScan;
    }

    /**
     * Clear the Realm media scan results.
     * This action will delete the table MediaScan in the Realm database.
     */
    public void clearAntiVirusScanResults() {
        mMediaScanDao.clearAntiVirusScanResults();
    }
}

