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

package fr.gouv.tchap.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;;
import android.widget.Switch;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.CreateRoomParams;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.util.Log;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.MXCActionBarActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.util.ThemeUtils;

public class TchapRoomCreationActivity extends MXCActionBarActivity {

    private static final String LOG_TAG = TchapRoomCreationActivity.class.getSimpleName();

    @BindView(R.id.btn_add_room_creation_avatar)
    Button btnAddAvatar;

    @BindView(R.id.et_room_name)
    TextInputEditText etRoomName;

    @BindView(R.id.switch_public_private_rooms)
    Switch switchPublicPrivateRoom;

    private MXSession mSession;
    private CreateRoomParams mRoomParams = new CreateRoomParams();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tchap_room_creation);
        ButterKnife.bind(this);

        setWaitingView(findViewById(R.id.room_creation_spinner_views));

        mSession = Matrix.getInstance(this).getDefaultSession();

        CreateRoomParams mRoomParams = new CreateRoomParams();
        mRoomParams.visibility = RoomState.DIRECTORY_VISIBILITY_PRIVATE;
        mRoomParams.preset = CreateRoomParams.PRESET_PRIVATE_CHAT;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        CommonActivityUtils.tintMenuIcons(menu, ThemeUtils.getColor(this, R.attr.icon_tint_on_dark_action_bar_color));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_create_new_room:
                createNewRoom();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tchap_room_creation_menu, menu);
        MenuItem item = menu.findItem(R.id.action_create_new_room);

        if (null != mRoomParams.name) {
            item.setEnabled(true);
            item.getIcon().setAlpha(255);
        } else {
            item.setEnabled(false);
            item.getIcon().setAlpha(130);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @OnClick(R.id.switch_public_private_rooms)
    void actionNotAvailable() {

        switchPublicPrivateRoom.setChecked(false);

        new AlertDialog.Builder(this)
                .setMessage(R.string.action_not_available_yet)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @OnTextChanged(R.id.et_room_name)
    protected void onTextChanged(CharSequence text) {
        String roomName = text.toString().trim();

        if (!roomName.isEmpty()) {
            mRoomParams.name = roomName;
        } else {
            mRoomParams.name = null;
        }

        invalidateOptionsMenu();
        Log.i(LOG_TAG, "room name:" + mRoomParams.name);
    }

    private void createNewRoom() {
        showWaitingView();
        mSession.createRoom(mRoomParams, new SimpleApiCallback<String>(TchapRoomCreationActivity.this) {
            @Override
            public void onSuccess(final String roomId) {

                getWaitingView().post(new Runnable() {
                    @Override
                    public void run() {
                        hideWaitingView();

                        Log.d(LOG_TAG, "## newRoomCreationSettings(): start VectorHomeActivity..");

                        HashMap<String, Object> params = new HashMap<>();
                        params.put(VectorRoomActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
                        params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
                        params.put(VectorRoomActivity.EXTRA_EXPAND_ROOM_HEADER, true);
                        CommonActivityUtils.goToRoomPage(TchapRoomCreationActivity.this, mSession, params);
                    }
                });
            }

            private void onError(final String message) {
                getWaitingView().post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != message) {
                            Toast.makeText(TchapRoomCreationActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                        hideWaitingView();
                    }
                });
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(final MatrixError e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(final Exception e) {
                onError(e.getLocalizedMessage());
            }
        });
    }
}