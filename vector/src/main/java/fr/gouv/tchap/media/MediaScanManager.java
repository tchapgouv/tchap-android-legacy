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

import org.matrix.androidsdk.HomeServerConnectionConfig;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.client.MediaScanRestClient;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.MediaScanResult;
import org.matrix.androidsdk.rest.model.crypto.EncryptedFileInfo;
import org.matrix.androidsdk.util.ContentManager;
import org.matrix.androidsdk.util.Log;

import java.util.Date;
import java.util.List;

import fr.gouv.tchap.model.MediaScan;
import im.vector.util.SlidableMediaInfo;
import io.realm.Realm;

public class MediaScanManager {

    private static final String LOG_TAG = MediaScanManager.class.getSimpleName();

    private final int MEDIA_SCAN_MANAGER_RETRY_DELAY = 10000;

    // Media scan listener
    public interface MediaScanManagerListener {
        /**
         * Called when a media scan has been updated.
         */
        void onMediaScanChange(MediaScan mediaScan);
    }

    private MediaScanManagerListener mListener;

    private MediaScanRestClient mMediaScanRestClient;
    // The Data Access Object (DAO)
    MediaScanDao mMediaScanDao;

    /**
     * Constructor
     *
     * @param hsConfig the home server connection config
     * @param realm    the Realm instance to use.
     */
    public MediaScanManager(HomeServerConnectionConfig hsConfig, Realm realm) {
        mMediaScanRestClient = new MediaScanRestClient(hsConfig);
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
     * Get the current scan result of an unencrypted media (including antivirus status).
     * Trigger an antivirus scan if it is not already done.
     *
     * @param url the matrix content url.
     * @return the current scan result of the media.
     */
    public MediaScan scanUnencryptedMedia(final String url) {

        // Sanity check
        if (!ContentManager.isValidMatrixContentUrl(url)) {
            return new MediaScan();
        }

        // Retrieve the existing result if any
        MediaScan mediaScan = mMediaScanDao.getMediaScan(url);

        // Check whether a new request is required
        if (isUpdateRequired(mediaScan)) {
            // Trigger the antivirus scan, update the current scan status in the database
            mMediaScanDao.updateMediaAntiVirusScanStatus(url, AntiVirusScanStatus.IN_PROGRESS);
            mediaScan = mMediaScanDao.getMediaScan(url);

            String mediaServerAndId = url.substring(ContentManager.MATRIX_CONTENT_URI_SCHEME.length());
            int index = mediaServerAndId.indexOf("/");
            if (index < 0 || index > mediaServerAndId.length() - 2) {
                // Invalid url
                Log.e(LOG_TAG, "## scanUnencryptedMedia failed: invalid url");
                return new MediaScan();
            }

            String domain = mediaServerAndId.substring(0, index);
            String mediaId = mediaServerAndId.substring(index+1);

            mMediaScanRestClient.scanUnencryptedFile(domain, mediaId, new ApiCallback<MediaScanResult>() {
                @Override
                public void onSuccess(MediaScanResult mediaScanResult) {
                    Log.d(LOG_TAG, "## scanUnencryptedFile succeeded" + mediaScanResult.info);
                    mMediaScanDao.updateMediaAntiVirusScanStatus(url, mediaScanResult);

                    // Call the listener if any
                    if (null != mListener) {
                        mListener.onMediaScanChange(mMediaScanDao.getMediaScan(url));
                    }
                }

                private void onError(final String message) {
                    Log.e(LOG_TAG, "## scanUnencryptedFile failed " + message);
                    mMediaScanDao.updateMediaAntiVirusScanStatus(url, AntiVirusScanStatus.UNKNOWN);

                    // Call the listener if any
                    if (null != mListener) {
                        mListener.onMediaScanChange(mMediaScanDao.getMediaScan(url));
                    }
                }

                @Override
                public void onNetworkError(Exception e) {
                    onError(e.getLocalizedMessage());
                }

                @Override
                public void onMatrixError(MatrixError matrixError) {
                    onError(matrixError.getLocalizedMessage());
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    onError(e.getLocalizedMessage());
                }
            });
        }

        return mediaScan;
    }

    /**
     * Get the current scan result of an encrypted media (including antivirus status).
     * Trigger an antivirus scan if it is not already done.
     *
     * @param mediaInfo  the encrypted media information.
     * @return the current scan result of the encrypted media..
     */
    public MediaScan scanEncryptedMedia(final EncryptedFileInfo mediaInfo) {

        MediaScan mediaScan = mMediaScanDao.getMediaScan(mediaInfo.url);

        // Check whether a new request is required
        if (isUpdateRequired(mediaScan)) {
            // Trigger the antivirus scan, update the current scan status in the database
            mMediaScanDao.updateMediaAntiVirusScanStatus(mediaInfo.url, AntiVirusScanStatus.IN_PROGRESS);
            mediaScan = mMediaScanDao.getMediaScan(mediaInfo.url);

            mMediaScanRestClient.scanEncryptedFile(mediaInfo, new ApiCallback<MediaScanResult>() {
                @Override
                public void onSuccess(MediaScanResult mediaScanResult) {
                    Log.d(LOG_TAG, "## scanEncryptedFile succeeded" + mediaScanResult.info);
                    mMediaScanDao.updateMediaAntiVirusScanStatus(mediaInfo.url, mediaScanResult);

                    // Call the listener if any
                    if (null != mListener) {
                        mListener.onMediaScanChange(mMediaScanDao.getMediaScan(mediaInfo.url));
                    }
                }

                private void onError(final String message) {
                    Log.e(LOG_TAG, "## scanEncryptedFile failed " + message);
                    mMediaScanDao.updateMediaAntiVirusScanStatus(mediaInfo.url, AntiVirusScanStatus.UNKNOWN);

                    // Call the listener if any
                    if (null != mListener) {
                        mListener.onMediaScanChange(mMediaScanDao.getMediaScan(mediaInfo.url));
                    }
                }

                @Override
                public void onNetworkError(Exception e) {
                    onError(e.getLocalizedMessage());
                }

                @Override
                public void onMatrixError(MatrixError matrixError) {
                    onError(matrixError.getLocalizedMessage());
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    onError(e.getLocalizedMessage());
                }
            });
        }

        return mediaScan;
    }

    private boolean isUpdateRequired (MediaScan mediaScan) {
        boolean isUpdateRequired = false;

        // Consider to launch a new request only if the scan is unknown for the moment.
        if (AntiVirusScanStatus.UNKNOWN == mediaScan.getAntiVirusScanStatus()) {

            // Check the date of the last update before triggering a new request.
            Date lastUpdateDate = mediaScan.getAntiVirusScanDate();
            if (null != lastUpdateDate) {
                long lastUpdateDateTime = lastUpdateDate.getTime();
                long nowTime = System.currentTimeMillis();

                if (lastUpdateDateTime > nowTime || (nowTime - lastUpdateDateTime) > MEDIA_SCAN_MANAGER_RETRY_DELAY) {
                    isUpdateRequired = true;
                }
            } else {
                isUpdateRequired = true;
            }

        }
        return isUpdateRequired;
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

        if (event.isEncrypted()) {
            List<EncryptedFileInfo> encryptedFileInfos = event.getEncryptedFileInfos();
            for (EncryptedFileInfo encryptedFileInfo : encryptedFileInfos) {
                MediaScan mediaScan = scanEncryptedMedia(encryptedFileInfo);
                if (mediaScan.getAntiVirusScanStatus() != AntiVirusScanStatus.TRUSTED) {
                    return true;
                }
            }
        } else {
            List<String> urls = event.getMediaUrls();
            for (String url : urls) {
                MediaScan mediaScan = scanUnencryptedMedia(url);
                if (mediaScan.getAntiVirusScanStatus() != AntiVirusScanStatus.TRUSTED) {
                    return true;
                }
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
            MediaScan mediaScan;
            if (null != mediaInfo.mEncryptedFileInfo) {
                mediaScan = scanEncryptedMedia(mediaInfo.mEncryptedFileInfo);
            } else {
                mediaScan = scanUnencryptedMedia(mediaInfo.mMediaUrl);
            }
            if (mediaScan.getAntiVirusScanStatus() == AntiVirusScanStatus.TRUSTED) {
                // Check the thumbnail url (if any)
                if (null != mediaInfo.mThumbnailUrl) {
                    if (null != mediaInfo.mEncryptedThumbnailFileInfo) {
                        mediaScan = scanEncryptedMedia(mediaInfo.mEncryptedThumbnailFileInfo);
                    } else {
                        mediaScan = scanUnencryptedMedia(mediaInfo.mThumbnailUrl);
                    }

                    isTrusted = (mediaScan.getAntiVirusScanStatus() == AntiVirusScanStatus.TRUSTED);
                } else {
                    isTrusted = true;
                }
            }
        }
        return isTrusted;
    }
}

