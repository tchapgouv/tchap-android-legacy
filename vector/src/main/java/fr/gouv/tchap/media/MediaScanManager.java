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

import android.os.Handler;
import android.os.Looper;

import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.crypto.EncryptedFileInfo;

import java.util.List;

import fr.gouv.tchap.model.MediaScan;
import im.vector.util.SlidableMediaInfo;
import io.realm.Realm;

public class MediaScanManager {

    private static final String LOG_TAG = MediaScanManager.class.getSimpleName();

    // Media scan listener
    public interface MediaScanManagerListener {
        /**
         * Called when a media scan has been updated.
         */
        void onMediaScanChange(MediaScan mediaScan);
    }

    private MediaScanManagerListener mListener;

    // The Data Access Object (DAO)
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
     * Set the listener to be notified on each media scan update handled by this manager
     *
     * @param listener the listener
     */
    public void setListener(MediaScanManagerListener listener) {
        mListener = listener;
    }

    /**
     * Get the current scan result of a media (including antivirus status).
     * Trigger an antivirus scan if it is not already done.
     *
     * @param url       the media url.
     * @return the current scan result of the media.
     */
    public MediaScan scanMedia(String url) {

        // Use the fake scanMedia for encrypted media until the server api is ready
        EncryptedFileInfo mediaInfo = new EncryptedFileInfo();
        mediaInfo.url = url;
        return scanMedia(mediaInfo);

        /*MediaScan mediaScan = mMediaScanDao.getMediaScan(url);

        if (AntiVirusScanStatus.UNKNOWN == mediaScan.getAntiVirusScanStatus()) {

            // TODO trigger the scan, call the listener on result
            // TODO update the ScanAntiVirusScanStatus to IN_PROGRESS
            // TODO Case Error, return the updated AntiVirusScanStatus to UNKNOWN
        }

        return mediaScan;*/
    }

    /**
     * Get the current scan result of an encrypted media (including antivirus status).
     * Trigger an antivirus scan if it is not already done.
     *
     * @param mediaInfo  the encrypted media information.
     * @return the current scan result of the encrypted media..
     */
    public MediaScan scanMedia(final EncryptedFileInfo mediaInfo) {

        MediaScan mediaScan = mMediaScanDao.getMediaScan(mediaInfo.url);

        if (AntiVirusScanStatus.UNKNOWN == mediaScan.getAntiVirusScanStatus()) {

            // Trigger the antivirus scan, update the current scan status in the database
            mMediaScanDao.updateMediaAntiVirusScanStatus(mediaInfo.url, AntiVirusScanStatus.IN_PROGRESS);
            mediaScan = mMediaScanDao.getMediaScan(mediaInfo.url);

            // Dummy scan
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {

                    // Fake status according to the end of the url
                    char tmp = mediaInfo.url.toLowerCase().charAt(mediaInfo.url.length()- 1);
                    AntiVirusScanStatus status = AntiVirusScanStatus.UNKNOWN;
                    if (tmp < 'l') {
                        // Trusted
                        status = AntiVirusScanStatus.TRUSTED;
                    } else if (tmp < 't') {
                        // Infected
                        status = AntiVirusScanStatus.INFECTED;
                    } else {
                        // Failure
                        //status = AntiVirusScanStatus.UNKNOWN;
                    }
                    mMediaScanDao.updateMediaAntiVirusScanStatus(mediaInfo.url, status);

                    // Call the listener if any
                    if (null != mListener) {
                        mListener.onMediaScanChange(mMediaScanDao.getMediaScan(mediaInfo.url));
                    }
                }
            }, 2000);
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

    /**
     * Check whether an event contains some unchecked or untrusted urls.
     *
     * @param event
     * @return true if the event contains at least one unchecked or untrusted url.
     */
    public boolean isUncheckedOrUntrustedMediaEvent(Event event) {
        List<String> urls = event.getMediaUrls();

        for (String url : urls) {
            MediaScan mediaScan = scanMedia(url);
            if (mediaScan.getAntiVirusScanStatus() != AntiVirusScanStatus.TRUSTED) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check whether all the urls of a media description have been checked and trusted.
     *
     * @param mediaInfo
     * @return true if the media description contains trusted urls.
     */
    public boolean isTrustedSlidableMediaInfo(SlidableMediaInfo mediaInfo) {
        boolean isTrusted = false;
        if (null != mediaInfo.mMediaUrl) {
            // Check whether the media is trusted
            MediaScan mediaScan = scanMedia(mediaInfo.mMediaUrl);
            if (mediaScan.getAntiVirusScanStatus() == AntiVirusScanStatus.TRUSTED) {
                // Check the thumbnail url (if any)
                if (null != mediaInfo.mThumbnailUrl) {
                    mediaScan = scanMedia(mediaInfo.mThumbnailUrl);
                    isTrusted = (mediaScan.getAntiVirusScanStatus() == AntiVirusScanStatus.TRUSTED);
                } else {
                    isTrusted = true;
                }
            }
        }
        return isTrusted;
    }
}

