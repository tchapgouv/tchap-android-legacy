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

import android.text.TextUtils;
import org.matrix.androidsdk.util.Log;
import fr.gouv.tchap.model.MediaAntivirusScanStatus;
import io.realm.Realm;


public class MediaAntivirusScanStatusDao {

    private static final String LOG_TAG = MediaAntivirusScanStatusDao.class.getSimpleName();

    private Realm mRealm;

    /**
     * Constructor
     *
     * @param realm instance of realm
     */
    public void MediaAntivirusScanStatusDao(Realm realm) {
        mRealm = realm;
    }

    /**
     * Get the media scan status for a dedicated url.
     *
     * @param url        the media url.
     * @return the current scan status.
     */
    public MediaAntivirusScanStatus.ScanStatus getMediaAvScanByUrl(String url) {

        MediaAntivirusScanStatus.ScanStatus scanStatus = MediaAntivirusScanStatus.ScanStatus.UNKNOWN;

        if (null != url && !TextUtils.isEmpty(url)) {
            try {
                MediaAntivirusScanStatus mediaAntivirusScanStatus = mRealm.where(MediaAntivirusScanStatus.class).equalTo("url", url).findFirst();
                if (null != mediaAntivirusScanStatus) {
                    scanStatus = mediaAntivirusScanStatus.getScanStatus();
                }
            } finally {
                mRealm.close();
            }
        }
        return scanStatus;
    }

    /**
     * Update the media scan status for a dedicated url.
     *
     * @param url        the media url.
     * @param scanStatus the current scan status.
     */
    public void updateMediaAvScannerDict(final String url, final MediaAntivirusScanStatus.ScanStatus scanStatus) {

        if (!TextUtils.isEmpty(url)) {
            mRealm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    MediaAntivirusScanStatus mediaAntivirusScanStatus = realm.where(MediaAntivirusScanStatus.class).equalTo("url", url).findFirst();

                    if (null == mediaAntivirusScanStatus) {
                        // Create the realm object MediaAntivirusScanStatus
                        mediaAntivirusScanStatus = realm.createObject(MediaAntivirusScanStatus.class, url);
                    }

                    mediaAntivirusScanStatus.setScanStatus(scanStatus);
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    mRealm.close();
                }
            }, new Realm.Transaction.OnError() {
                @Override
                public void onError(Throwable error) {
                    Log.e(LOG_TAG, "## updateMediaAvScannerDict : Realm transaction failed");
                }
            });
        }
    }

    /**
     * Clear the Realm cached media scan results.
     */
    public void clearAntivirusScanResultsCache() {

        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.delete(MediaAntivirusScanStatus.class);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                mRealm.close();
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                Log.e(LOG_TAG, "## clearAntivirusScanResultsCache : Realm transaction failed");
            }
        });
    }
}
