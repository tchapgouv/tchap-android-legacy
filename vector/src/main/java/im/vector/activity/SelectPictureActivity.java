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


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.provider.MediaStore;
import android.content.ContentValues;

import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import android.net.Uri;
import android.os.Build;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Date;

import org.matrix.androidsdk.adapters.IconAndTextAdapter;
import org.matrix.androidsdk.util.Log;

import im.vector.util.ThemeUtils;
import im.vector.R;


public class SelectPictureActivity extends AppCompatActivity  {
    private static final int TAKE_IMAGE_REQUEST_CODE = 1;
    private static final int REQUEST_FILES_REQUEST_CODE = 2;
    private static final String TAG_FRAGMENT_SELECT_PICTURE = "picture";
    private static final String CAMERA_VALUE_TITLE = "attachment";
    private static final String LOG_TAG = SelectPictureActivity.class.getSimpleName();
    private String mLatestTakePictureCameraUri = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        IconAndTextDialogTchapFragment fragment = (IconAndTextDialogTchapFragment) fm.findFragmentByTag(TAG_FRAGMENT_SELECT_PICTURE);

        if (fragment != null) {
            fragment.dismissAllowingStateLoss();
        }

        final Integer[] messages;
        final Integer[] icons;

        messages = new Integer[]{
                R.string.option_select_image,
                //R.string.option_send_sticker,
                R.string.option_take_photo,
        };

        icons = new Integer[]{
                R.drawable.tchap_ic_attached_files,
                //R.drawable.ic_send_sticker,
                R.drawable.tchap_ic_camera,
        };

        fragment = IconAndTextDialogTchapFragment.newInstance(icons, messages,
                ThemeUtils.getColor(this, R.attr.riot_primary_background_color),
                ContextCompat.getColor(this, R.color.tchap_text_color_light));

        fragment.setOnClickListener(new IconAndTextDialogTchapFragment.OnItemClickListener() {
            @Override
            public void onItemClick(IconAndTextDialogTchapFragment dialogFragment, int position) {
                Integer selectedVal = messages[position];

                if (selectedVal == R.string.option_select_image) {
                    launchImageSelectionIntent();
                } else if (selectedVal == R.string.option_take_photo) {
                    launchNativeCamera();
                }
            }
        });

        fragment.show(fm,TAG_FRAGMENT_SELECT_PICTURE);
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

    public static class IconAndTextDialogTchapFragment extends DialogFragment {

        // params
        private static final String ARG_ICONS_LIST_ID = "org.matrix.androidsdk.fragments.IconAndTextDialogFragment.ARG_ICONS_LIST_ID";
        private static final String ARG_TEXTS_LIST_ID = "org.matrix.androidsdk.fragments.IconAndTextDialogFragment.ARG_TEXTS_LIST_ID";
        private static final String ARG_BACKGROUND_COLOR = "org.matrix.androidsdk.fragments.IconAndTextDialogFragment.ARG_BACKGROUND_COLOR";
        private static final String ARG_TEXT_COLOR = "org.matrix.androidsdk.fragments.IconAndTextDialogFragment.ARG_TEXT_COLOR";

        /**
         * Interface definition for a callback to be invoked when an item in this
         * AdapterView has been clicked.
         */
        public interface OnItemClickListener {
            /**
             * Callback method to be invoked when an item is clicked.
             *
             * @param dialogFragment the dialog.
             * @param position       The clicked position
             */
            void onItemClick(IconAndTextDialogTchapFragment dialogFragment, int position);
        }

        private ListView mListView;

        private ArrayList<Integer> mIconResourcesList;
        private ArrayList<Integer> mTextResourcesList;
        private Integer mBackgroundColor = null;
        private Integer mTextColor = null;

        private IconAndTextDialogTchapFragment.OnItemClickListener mOnItemClickListener;


        public static IconAndTextDialogTchapFragment newInstance(Integer[] iconResourcesList, Integer[] textResourcesList) {
            return IconAndTextDialogTchapFragment.newInstance(iconResourcesList, textResourcesList, null, null);
        }

        public static IconAndTextDialogTchapFragment newInstance(Integer[] iconResourcesList, Integer[] textResourcesList, Integer backgroundColor, Integer textColor) {
            IconAndTextDialogTchapFragment f = new IconAndTextDialogTchapFragment();
            Bundle args = new Bundle();

            args.putIntegerArrayList(ARG_ICONS_LIST_ID, new ArrayList<>(Arrays.asList(iconResourcesList)));
            args.putIntegerArrayList(ARG_TEXTS_LIST_ID, new ArrayList<>(Arrays.asList(textResourcesList)));

            if (null != backgroundColor) {
                args.putInt(ARG_BACKGROUND_COLOR, backgroundColor);
            }

            if (null != textColor) {
                args.putInt(ARG_TEXT_COLOR, textColor);
            }

            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mIconResourcesList = getArguments().getIntegerArrayList(ARG_ICONS_LIST_ID);
            mTextResourcesList = getArguments().getIntegerArrayList(ARG_TEXTS_LIST_ID);

            if (mIconResourcesList == null) mIconResourcesList = new ArrayList<Integer>();
            if (mTextResourcesList == null) mTextResourcesList = new ArrayList<Integer>();

            if (getArguments().containsKey(ARG_BACKGROUND_COLOR)) {
                mBackgroundColor = getArguments().getInt(ARG_BACKGROUND_COLOR);
            }

            if (getArguments().containsKey(ARG_TEXT_COLOR)) {
                mTextColor = getArguments().getInt(ARG_TEXT_COLOR);
            }

        }

        @Override
        public @NonNull Dialog onCreateDialog(@NonNull  Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            View view = getActivity().getLayoutInflater().inflate(org.matrix.androidsdk.R.layout.fragment_dialog_icon_text_list, null);
            builder.setView(view);
            initView(view);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (null != mOnItemClickListener) {
                        mOnItemClickListener.onItemClick(IconAndTextDialogTchapFragment.this, position);
                    }

                    IconAndTextDialogTchapFragment.this.dismiss();
                }
            });


            return builder.create();
        }

        /**
         * Init the dialog view.
         *
         * @param v the dialog view.
         */
        void initView(View v) {
            mListView = v.findViewById(org.matrix.androidsdk.R.id.listView_icon_and_text);
            IconAndTextAdapter adapter = new IconAndTextAdapter(getActivity(), org.matrix.androidsdk.R.layout.adapter_item_icon_and_text);

            for (int index = 0; index < mIconResourcesList.size(); index++) {
                adapter.add(mIconResourcesList.get(index), mTextResourcesList.get(index));
            }

            if (null != mBackgroundColor) {
                mListView.setBackgroundColor(mBackgroundColor);
                adapter.setBackgroundColor(mBackgroundColor);
            }

            if (null != mTextColor) {
                adapter.setTextColor(mTextColor);
            }

            mListView.setAdapter(adapter);
        }

        /**
         * Register a callback to be invoked when this view is clicked.
         *
         * @param l the listener
         */
        public void setOnClickListener(IconAndTextDialogTchapFragment.OnItemClickListener l) {
            mOnItemClickListener = l;
        }

        /**
         * Finish the parent activity, this class is designed to be used with SelectPictureActivitys
         * @param dialog
         */
        @Override
        public void onCancel(DialogInterface dialog) {
            getActivity().finish();
            super.onCancel(dialog);
        }

    }

}