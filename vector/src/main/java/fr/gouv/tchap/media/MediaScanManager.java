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
import org.matrix.androidsdk.rest.model.crypto.EncryptedFileInfo;

import fr.gouv.tchap.model.MediaScan;
import io.realm.Realm;

public class MediaScanManager {

    private static final String LOG_TAG = MediaScanManager.class.getSimpleName();

    public interface MediaScanManagerListener {
        /**
         * Called when a media scan has been updated.
         */
        void onMediaScanChange(MediaScan mediaScan);
    }

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
     * Get the current scan result of a media (including antivirus status).
     * Trigger an antivirus scan if it is not already done, and if a listener is provided.
     *
     * @param url       the media url.
     * @param listener  optional listener on the media scan update.
     * @return the current scan result of the media.
     */
    public MediaScan scanMedia(String url, @Nullable MediaScanManagerListener listener) {

        MediaScan mediaScan = mMediaScanDao.getMediaScan(url);

        if (null != listener && AntiVirusScanStatus.UNKNOWN == mediaScan.getAntiVirusScanStatus()) {

            // TODO trigger the scan, call the listener on result if any
            // TODO update the ScanAntiVirusScanStatus to IN_PROGRESS
            // TODO Case Error, return the updated AntiVirusScanStatus to UNKNOWN
        }

        return mediaScan;
    }

    /**
     * Get the current scan result of an encrypted media (including antivirus status).
     * Trigger an antivirus scan if it is not already done, and if a listener is provided.
     *
     * @param mediaInfo  the encrypted media information.
     * @param listener  optional listener on the media scan update.
     * @return the current scan result of the encrypted media..
     */
    public MediaScan scanMedia(EncryptedFileInfo mediaInfo, @Nullable MediaScanManagerListener listener) {

        MediaScan mediaScan = mMediaScanDao.getMediaScan(mediaInfo.url);

        if (null != listener && AntiVirusScanStatus.UNKNOWN == mediaScan.getAntiVirusScanStatus()) {

            // TODO trigger the scan, call the listener on result if any
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

