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

package im.vector.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.crypto.EncryptedFileInfo;
import im.vector.activity.RiotAppCompatActivity;
import io.realm.Realm;
import io.realm.RealmResults;

public class MediaAVScanner extends RiotAppCompatActivity {

    private static final String LOG_TAG = MediaAVScanner.class.getSimpleName();

    private String mUserId = null;

    /**
     * Constructor
     *
     * @param userId the user id
     */
    public MediaAVScanner(String userId) {
        mUserId = userId;
    }


    public MediaAntivirusScanStatus.ScanStatus getMediaAvScanByUrl(String url) {

        MediaAntivirusScanStatus mediaAntivirusScanStatus;
        MediaAntivirusScanStatus.ScanStatus scanStatus = MediaAntivirusScanStatus.ScanStatus.UNKNOWN;

        if (null != url && !TextUtils.isEmpty(url)) {
            try {
                mediaAntivirusScanStatus = realm.where(MediaAntivirusScanStatus.class).equalTo("url", url).findFirst();
                if (null != mediaAntivirusScanStatus) {
                    scanStatus = mediaAntivirusScanStatus.getScanStatus();
                }
            } finally {
                realm.close();
            }
        }
        return scanStatus;

    }

    /**
     * Clear the Realm cached media scan results.
     */
    public void clearAntivirusScanResultsCache() {

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {
                RealmResults<MediaAntivirusScanStatus> results = realm.where(MediaAntivirusScanStatus.class).findAll();
                results.deleteAllFromRealm();
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                realm.close();
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Log.e(LOG_TAG, "## clearAntivirusScanResultsCache : Realm transaction failed");
            }
        });
    }

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

        getMediaAvScanByUrl(url);

        if (null != getMediaAvScanByUrl(url)) {
            return getMediaAvScanByUrl(url);
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

        getMediaAvScanByUrl(mediaInfo.url);

        if (null != getMediaAvScanByUrl(mediaInfo.url)) {
            return getMediaAvScanByUrl(mediaInfo.url);
        }

        return MediaAntivirusScanStatus.ScanStatus.IN_PROGRESS;

        // TODO trigger the scan, use the callback on result if any
    }

    /**
     * Update the media scan status for a dedicated url.
     *
     * @param url        the media url.
     * @param scanStatus the current scan status.
     */
    public void updateMediaAvScannerDict(final String url, final MediaAntivirusScanStatus.ScanStatus scanStatus) {

        if (null != url && !TextUtils.isEmpty(url)) {
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm bgRealm) {
                    MediaAntivirusScanStatus mediaAntivirusScanStatus = realm.createObject(MediaAntivirusScanStatus.class);
                    mediaAntivirusScanStatus.setUrl(url);
                    mediaAntivirusScanStatus.setScanStatus(scanStatus);
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    realm.close();
                }
            }, new Realm.Transaction.OnError() {
                @Override
                public void onError(Throwable error) {
                    Log.e(LOG_TAG, "## updateMediaAvScannerDict : Realm transaction failed");
                }
            });
        }
    }
}

