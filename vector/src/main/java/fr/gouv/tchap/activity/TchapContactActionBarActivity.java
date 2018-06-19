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

package fr.gouv.tchap.activity;

import android.widget.ImageView;
import android.widget.TextView;

import im.vector.R;
import im.vector.activity.MXCActionBarActivity;
import im.vector.util.VectorUtils;

/**
 * extends ActionBarActivity to manage the rageshake
 */
public abstract class TchapContactActionBarActivity extends MXCActionBarActivity {
    protected TextView mActionBarCustomTitle;
    protected TextView mActionBarCustomTopic;
    protected ImageView mAvatar;


    @Override
    public void initUiAndData() {
        mActionBarCustomTitle = findViewById(R.id.room_action_bar_title);
        mActionBarCustomTopic = findViewById(R.id.room_action_bar_topic);
        mAvatar = findViewById(R.id.big_avatar_img);
    }
    /**
     * Set the title value in the action bar and in the
     * room header layout
     */
    protected void setTitle(String titleToApply) {
        if (mActionBarCustomTitle != null) {
            mActionBarCustomTitle.setText(titleToApply);
        }
    }
    /**
     * Set the topic
     */
    protected void setTopic(String topic) {
        if (mActionBarCustomTopic != null) {
            mActionBarCustomTopic.setText(topic);
        }
    }
    /**
     * Refresh the room avatar.
     */
    protected void setAvatar() {
        if (null != mRoom) {
            VectorUtils.loadRoomAvatar(this, mSession, mAvatar, mRoom);
        }
    }
}
