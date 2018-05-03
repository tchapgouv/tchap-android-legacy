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
import org.matrix.androidsdk.util.ContentUtils;
import org.matrix.androidsdk.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class MediaAVScanner {

    private static final String LOG_TAG = MediaAVScanner.class.getSimpleName();

    public enum ScanStatus { UNKNOWN, IN_PROGRESS, TRUSTED, INFECTED }

    private static final String FILENAME = "MediaAVScannerCache";

    final String MEDIAAVSCANNER_STORE_FOLDER = "MediaAVScannerStore";

    private HashMap<String, ScanStatus> mMediaAVScanByUrl = null;
    private String mUserId = null;
    private File mMediaAVScannerDirectory = null;
    private File mMediaAVScannerFile = null;

    /**
     * Constructor
     *
     * @param userId the user id
     */
    public MediaAVScanner(String userId) {
        mUserId = userId;
    }

    /**
     * Clear the cached scan results.
     */
    public void clearCache() {
        ContentUtils.deleteDirectory(mMediaAVScannerDirectory);
        mMediaAVScanByUrl = null;

        // TODO cancel pending scans.
    }

    /**
     * Open the scanner cache file.
     *
     * @param context the context.
     */
    private void openMediaAVScannerDict(Context context) {

        // already checked
        if (null != mMediaAVScanByUrl) {
            return;
        }

        mMediaAVScanByUrl = new HashMap<>();

        try {
            mMediaAVScannerDirectory = new File(context.getApplicationContext().getFilesDir(), MEDIAAVSCANNER_STORE_FOLDER);
            mMediaAVScannerDirectory = new File(mMediaAVScannerDirectory, mUserId);

            mMediaAVScannerFile = new File(mMediaAVScannerDirectory, FILENAME.hashCode() + "");

            if (!mMediaAVScannerDirectory.exists()) {

                // create dir tree
                mMediaAVScannerDirectory.mkdirs();
            }

            if (mMediaAVScannerFile.exists()) {
                FileInputStream fis = new FileInputStream(mMediaAVScannerFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                mMediaAVScanByUrl = (HashMap) ois.readObject();
                ois.close();
                fis.close();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## openMediaAVScannerDict failed " + e.getMessage());
        }
    }

    /**
     * Get the current scan status for a media.
     * Trigger a scan if it is not already done.
     *
     * @param context   the context.
     * @param url       the media url.
     * @param callback  optional async response handler.
     * @return the current scan status.
     */
    public ScanStatus scanEncryptedMedia(Context context, String url, @Nullable ApiCallback<Void> callback) {
        if (null == mMediaAVScanByUrl) {
            openMediaAVScannerDict(context);
        }

        if (TextUtils.isEmpty(url)) {
            return ScanStatus.UNKNOWN;
        }

        if (mMediaAVScanByUrl.containsKey(url)) {
            return mMediaAVScanByUrl.get(url);
        }

        // TODO trigger the scan, use the callback on result if any

        return ScanStatus.IN_PROGRESS;
    }

    /**
     * Get the current scan status for an encrypted media.
     * Trigger a scan if it is not already done.
     *
     * @param context the context.
     * @param mediaInfo  the encrypted media information.
     * @param callback   optional async response handler.
     * @return the current scan status.
     */
    public ScanStatus scanEncryptedMedia(Context context, EncryptedFileInfo mediaInfo, @Nullable ApiCallback<Void> callback) {
        if (null == mMediaAVScanByUrl) {
            openMediaAVScannerDict(context);
        }

        if (TextUtils.isEmpty(mediaInfo.url)) {
            return ScanStatus.UNKNOWN;
        }

        if (mMediaAVScanByUrl.containsKey(mediaInfo.url)) {
            return mMediaAVScanByUrl.get(mediaInfo.url);
        }

        // TODO trigger the scan, use the callback on result if any

        return ScanStatus.IN_PROGRESS;
    }

    /**
     * Store the scanner dictionary.
     *
     * @param context the context.
     */
    private void saveMediaAVScannerDict(Context context) {
        try {
            FileOutputStream fos = new FileOutputStream(mMediaAVScannerFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mMediaAVScanByUrl);
            oos.close();
            fos.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "## saveMediaAVScannerDict() failed " + e.getMessage());
        }
    }

    /**
     * Update the media scan status for a dedicated url.
     *
     * @param context the context.
     * @param url  the media url.
     * @param scanStatus the current scan status.
     */
    public void updateMediaAVScannerDict(Context context, String url, ScanStatus scanStatus) {
        if (null == mMediaAVScanByUrl) {
            openMediaAVScannerDict(context);
        }

        if (!TextUtils.isEmpty(url)) {
            mMediaAVScanByUrl.put(url, scanStatus);
            saveMediaAVScannerDict(context);
        }
    }
}

