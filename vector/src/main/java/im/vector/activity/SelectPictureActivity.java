/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.activity;


import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.content.ContentValues;

import android.support.annotation.NonNull;

import android.net.Uri;
import android.os.Build;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import org.matrix.androidsdk.core.Log;

import im.vector.dialogs.DialogListItem;
import im.vector.dialogs.DialogSendItemAdapter;
import im.vector.util.PermissionsToolsKt;
import im.vector.R;


public class SelectPictureActivity extends AppCompatActivity  {
    private static final int TAKE_IMAGE_REQUEST_CODE = 1;
    private static final int REQUEST_FILES_REQUEST_CODE = 2;
    private static final String CAMERA_VALUE_TITLE = "attachment";
    private static final String LOG_TAG = SelectPictureActivity.class.getSimpleName();
    private String mLatestTakePictureCameraUri = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final List<DialogListItem> items = new ArrayList<>();

        items.add(DialogListItem.SelectPicture.INSTANCE);
        items.add(DialogListItem.TakePhoto.INSTANCE);

        new android.support.v7.app.AlertDialog.Builder(this)
                .setAdapter(new DialogSendItemAdapter(this, items), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onSendChoiceClicked(items.get(which));
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();
    }

    private void onSendChoiceClicked(DialogListItem dialogListItem) {
        if (dialogListItem instanceof DialogListItem.SelectPicture) {
            launchImageSelectionIntent();
        } else if (dialogListItem instanceof DialogListItem.TakePhoto) {
            // Check permission
            if (PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_TAKING_PHOTO, SelectPictureActivity.this, PermissionsToolsKt.PERMISSION_REQUEST_CODE_LAUNCH_NATIVE_CAMERA)) {
                launchNativeCamera();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int aRequestCode, @NonNull String[] aPermissions, @NonNull int[] aGrantResults) {
        if (aRequestCode == PermissionsToolsKt.PERMISSION_REQUEST_CODE_LAUNCH_NATIVE_CAMERA) {
            boolean isCameraPermissionGranted = false;

            for (int i = 0; i < aPermissions.length; i++) {
                Log.d(LOG_TAG, "## onRequestPermissionsResult(): " + aPermissions[i] + "=" + aGrantResults[i]);

                if (Manifest.permission.CAMERA.equals(aPermissions[i])) {
                    if (PackageManager.PERMISSION_GRANTED == aGrantResults[i]) {
                        Log.d(LOG_TAG, "## onRequestPermissionsResult(): CAMERA permission granted");
                        isCameraPermissionGranted = true;
                    } else {
                        Log.d(LOG_TAG, "## onRequestPermissionsResult(): CAMERA permission not granted");
                    }
                }

                if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(aPermissions[i])) {
                    if (PackageManager.PERMISSION_GRANTED == aGrantResults[i]) {
                        Log.d(LOG_TAG, "## onRequestPermissionsResult(): WRITE_EXTERNAL_STORAGE permission granted");
                    } else {
                        Log.d(LOG_TAG, "## onRequestPermissionsResult(): WRITE_EXTERNAL_STORAGE permission not granted");
                    }
                }
            }

            // Because external storage permission is not mandatory to launch the camera,
            // external storage permission is not tested.
            if (isCameraPermissionGranted) {
                launchNativeCamera();
            } else {
                Toast.makeText(this, getString(R.string.missing_permissions_warning), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This method allow to select a filetype (file, image, video) when the user click on the
     * trombone icon.
     */
    private void launchImageSelectionIntent() {
        Intent fileIntent = new Intent(Intent.ACTION_PICK);
        fileIntent.setType("image/*");
        startActivityForResult(fileIntent, REQUEST_FILES_REQUEST_CODE);
    }

    /**
     * Launch the camera
     */
    private void launchNativeCamera() {
        Log.d(LOG_TAG, "***** launch native camera *********");

        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // the following is a fix for buggy 2.x devices
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, CAMERA_VALUE_TITLE + formatter.format(date));
        // The Galaxy S not only requires the name of the file to output the image to, but will also not
        // set the mime type of the picture it just took (!!!). We assume that the Galaxy S takes image/jpegs
        // so the attachment uploader doesn't freak out about there being no mimetype in the content database.
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri dummyUri = null;
        try {
            dummyUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (null == dummyUri) {
                Log.e(LOG_TAG, "Cannot use the external storage media to save image");
            }
        } catch (UnsupportedOperationException uoe) {
            Log.e(LOG_TAG, "Unable to insert camera URI into MediaStore.Images.Media.EXTERNAL_CONTENT_URI - no SD card? Attempting to insert into device storage.");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to insert camera URI into MediaStore.Images.Media.EXTERNAL_CONTENT_URI. " + e);
        }

        if (null == dummyUri) {
            try {
                dummyUri = getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);
                if (null == dummyUri) {
                    Log.e(LOG_TAG, "Cannot use the internal storage to save media to save image");
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "Unable to insert camera URI into internal storage. Giving up. " + e);
            }
        }

        if (dummyUri != null) {
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, dummyUri);
            Log.d(LOG_TAG, "trying to take a photo on " + dummyUri.toString());
        } else {
            Log.d(LOG_TAG, "trying to take a photo with no predefined uri");
        }

        // Store the dummy URI which will be set to a placeholder location. When all is lost on samsung devices,
        // this will point to the data we're looking for.
        // Because Activities tend to use a single MediaProvider for all their intents, this field will only be the
        // *latest* TAKE_PICTURE Uri. This is deemed acceptable as the normal flow is to create the intent then immediately
        // fire it, meaning onActivityResult/getUri will be the next thing called, not another createIntentFor.
        mLatestTakePictureCameraUri = dummyUri == null ? null : dummyUri.toString();

        startActivityForResult(captureIntent, TAKE_IMAGE_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(LOG_TAG, "****** on activity result ");

        if (resultCode == RESULT_OK) {
            if ((requestCode == TAKE_IMAGE_REQUEST_CODE) || (requestCode == REQUEST_FILES_REQUEST_CODE)) {

                // provide the Uri
                Intent intent = new Intent();
                if ((data != null) && (data.getData() != null))
                    intent.setData(data.getData());
                else
                    if (mLatestTakePictureCameraUri != null)
                        intent.setData(Uri.parse(mLatestTakePictureCameraUri));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (data != null) {
                        intent.setClipData(data.getClipData());
                    }
                }

                //intent.putExtras(conData);
                setResult(RESULT_OK, intent);
                finish();
            }
        } else {
            finish();
        }
    }
}