/*
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.activity;

import android.view.View;

import butterknife.BindView;
import im.vector.R;

public class JitsiCallActivity extends RiotAppCompatActivity {
    private static final String LOG_TAG = JitsiCallActivity.class.getSimpleName();

    /**
     * The linked widget
     */
    public static final String EXTRA_WIDGET_ID = "EXTRA_WIDGET_ID";

    /**
     * set to true to start a video call
     */
    public static final String EXTRA_ENABLE_VIDEO = "EXTRA_ENABLE_VIDEO";

    @BindView(R.id.jsti_back_to_app_icon)
    View mBackToAppIcon;

    @BindView(R.id.jsti_close_widget_icon)
    View mCloseWidgetIcon;

    @BindView(R.id.jsti_connecting_text_view)
    View mConnectingTextView;

    @BindView(R.id.jitsi_progress_layout)
    View waitingView;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_jitsi_call;
    }
}