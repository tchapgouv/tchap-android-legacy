/*
 * Copyright 2018 DINSIC
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

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.crypto.EncryptedFileInfo;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class MediaAVScanner extends RealmObject {

    private static final String LOG_TAG = MediaAVScanner.class.getSimpleName();

    public enum ScanStatus { UNKNOWN, IN_PROGRESS, TRUSTED, INFECTED }

    class MediaAntivirusMap extends RealmObject {
        private String url;
        private ScanStatus scanStatus;

        private String getUrl() {return url;}
        private void setUrl(String url)  {this.url = url;}

        private ScanStatus getScanStatus() { return scanStatus;}
        private void setScanStatus(ScanStatus scanStatus) {this.scanStatus = scanStatus;}

    }

    @PrimaryKey
    private String mUserId = null;
    private RealmList<MediaAntivirusMap> mMediaAvScanByUrl = null;

    public RealmList getMediaAVScanByUrl() { return mMediaAvScanByUrl; }

    // Initialization and configuration of realm are done in VectorApp class
    // Get a Realm instance for this thread
    Realm realm = Realm.getDefaultInstance(); // opens "MediaAVScannerCache.realm"

    /**
     * Constructor
     *
     * @param userId the user id
     */
    public MediaAVScanner(String userId) {
        mUserId = userId;
    }

    /**
     * Clear the Realm cached scan results.
     */
    public void clearRealmCache() {

        try {
            // The method deleteAll() doesn't require all Realm instances closed.
            // It will just delete all the objects in the Realm without clearing the schemas.
            realm.beginTransaction();
            realm.deleteAll();
            realm.commitTransaction();
        } finally {
            realm.close();
        }

        mMediaAvScanByUrl = null;
    }

    /**
     * Get the current scan status for a media.
     * Trigger a scan if it is not already done.
     *
     * @param url       the media url.
     * @param callback  optional async response handler.
     * @return the current scan status.
     */
    public ScanStatus scanMedia(String url, @Nullable ApiCallback<Void> callback) {

        if (TextUtils.isEmpty(url)) {
            return ScanStatus.UNKNOWN;
        }

        ScanStatus scanStatus = (ScanStatus) this.getMediaAVScanByUrl().where().equalTo("url", url).findFirst();

        if (null == scanStatus) {
            return ScanStatus.UNKNOWN;
        } else {
            return ScanStatus.IN_PROGRESS;
        }

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
    public ScanStatus scanEncryptedMedia(EncryptedFileInfo mediaInfo, @Nullable ApiCallback<Void> callback) {
        if (TextUtils.isEmpty(mediaInfo.url)) {
            return ScanStatus.UNKNOWN;
        }

        ScanStatus scanStatus = (ScanStatus) this.getMediaAVScanByUrl().where().equalTo("url", mediaInfo.url).findFirst();

        if (null == scanStatus) {
            return ScanStatus.UNKNOWN;
        } else {
            return ScanStatus.IN_PROGRESS;
        }

        // TODO trigger the scan, use the callback on result if any
    }

    /**
     * Update the media scan status for a dedicated url.
     *
     * @param url  the media url.
     * @param scanStatus the current scan status.
     */
    public void updateMediaAvScannerDict(String url, ScanStatus scanStatus) {

        if (!TextUtils.isEmpty(url)) {
            realm.beginTransaction();;
            MediaAntivirusMap mediaAntivirusMap = realm.createObject(MediaAntivirusMap.class);
            mediaAntivirusMap.setUrl(url);
            mediaAntivirusMap.setScanStatus(scanStatus);
            realm.commitTransaction();
        }
    }
}

