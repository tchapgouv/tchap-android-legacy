/*
 * Copyright 2019 New Vector Ltd
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

package fr.gouv.tchap.preference

import android.content.Context
import android.util.AttributeSet

import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.data.Room

import im.vector.util.VectorUtils

/**
 * Specialized class to target a Room avatar preference.
 * Based on the avatar preference class it redefines refreshAvatar() and
 * add the new method  setConfiguration().
 */
class TchapRoomAvatarPreference : HexagonAvatarPreference {

    private var mRoom: Room? = null

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {}

    override fun refreshAvatar() {
        if (null != mAvatarView && null != mRoom) {
            VectorUtils.loadRoomAvatar(context, mSession, mAvatarView, mRoom)
            // Keep the default settings for the avatar border
        }
    }

    fun setConfiguration(aSession: MXSession, aRoom: Room) {
        mSession = aSession
        mRoom = aRoom
        refreshAvatar()
    }

}