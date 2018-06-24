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

import android.support.annotation.NonNull;

import org.matrix.androidsdk.rest.model.MediaScanResult;

import java.util.Date;

import fr.gouv.tchap.model.MediaScan;
import io.realm.Realm;


public class MediaScanDao {

    private static final String LOG_TAG = MediaScanDao.class.getSimpleName();

    private Realm mRealm;

    /**
     * Constructor
     *
     * @param realm instance of realm.
     */
    public MediaScanDao(Realm realm) {
        mRealm = realm;
    }

    /**
     * Get the media scan object for a dedicated url.
     *
     * @param url   the media url, must not be null.
     * @return the media scan object for this url from the database (will be created if necessary).
     */
    /* package */ MediaScan getMediaScan(@NonNull String url) {

        return getMediaScan(mRealm, url);
    }

    /**
     * Update the media scan antivirus status for a dedicated url.
     * Refresh the last update date too.
     *
     * @param url                 the media url, must not be null.
     * @param antiVirusScanStatus the current antivirus scan status.
     */
    /* package */ void updateMediaAntiVirusScanStatus(@NonNull final String url, final AntiVirusScanStatus antiVirusScanStatus) {

        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MediaScan mediaScan = getMediaScan(realm, url);
                mediaScan.setAntiVirusScanStatus(antiVirusScanStatus);
                mediaScan.setAntiVirusScanDate(new Date());
            }
        });
    }

    /**
     * Update the media scan antivirus status for a dedicated url by considering the anti-virus server response.
     * Refresh the last update date too.
     *
     * @param url                 the media url, must not be null.
     * @param mediaScanResult     the antivirus scan result.
     */
    /* package */ void updateMediaAntiVirusScanStatus(@NonNull final String url, final MediaScanResult mediaScanResult) {

        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MediaScan mediaScan = getMediaScan(realm, url);
                AntiVirusScanStatus status;
                if (mediaScanResult.clean) {
                    // Trusted
                    status = AntiVirusScanStatus.TRUSTED;
                } else {
                    // Infected
                    status = AntiVirusScanStatus.INFECTED;
                }
                mediaScan.setAntiVirusScanStatus(status);
                mediaScan.setAntiVirusScanInfo(mediaScanResult.info);
                mediaScan.setAntiVirusScanDate(new Date());
            }
        });
    }

    /**
     * Clear the Realm media scan results.
     * This action will delete the table MediaScan in the Realm database.
     */
    /* package */ void clearAntiVirusScanResults() {

        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.delete(MediaScan.class);
            }
        });
    }

    /**
     * Get the media scan object for a dedicated url from the provided realm instance.
     *
     * @param realm instance of realm.
     * @param url   the media url, must not be null.
     * @return the current media scan object (will be created if necessary).
     */
    private MediaScan getMediaScan(Realm realm, @NonNull String url) {

        MediaScan mediaScan = null;

        mediaScan = realm.where(MediaScan.class)
                .equalTo("url", url)
                .findFirst();

        if (null == mediaScan) {
            // Create the realm object MediaScan
            if (realm.isInTransaction()) {
                mediaScan = realm.createObject(MediaScan.class, url);
            } else {
                realm.beginTransaction();
                mediaScan = realm.createObject(MediaScan.class, url);
                realm.commitTransaction();
            }
        }

        return mediaScan;
    }
}

