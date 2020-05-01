/*
 * Copyright 2020 New Vector Ltd
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

package fr.gouv.tchap.adapters

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import im.vector.R
import im.vector.adapters.VectorMessagesAdapter
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.adapters.MessageRow
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.db.MXMediaCache
import org.matrix.androidsdk.rest.model.Event
import android.util.TypedValue
import im.vector.adapters.AdapterUtils
import java.util.Date


class TchapFavouriteMessagesAdapter(session: MXSession, context: Context, mediaCache: MXMediaCache) :
        VectorMessagesAdapter(session,
                context,
                R.layout.adapter_item_tchap_favourite_message_text_emote_notice,
                R.layout.adapter_item_vector_message_image_video,
                R.layout.adapter_item_tchap_favourite_message_text_emote_notice,
                R.layout.adapter_item_vector_message_room_member,
                R.layout.adapter_item_tchap_favourite_message_text_emote_notice,
                R.layout.adapter_item_vector_message_file,
                R.layout.adapter_item_vector_message_merge,
                R.layout.adapter_item_vector_message_image_video,
                R.layout.adapter_item_vector_message_emoji,
                R.layout.adapter_item_vector_message_code,
                R.layout.adapter_item_vector_message_image_video,
                R.layout.adapter_item_vector_message_redact,
                R.layout.adapter_item_vector_message_room_versioned,
                R.layout.adapter_item_tchap_media_scan,
                mediaCache) {
    private val LOG_TAG = TchapFavouriteMessagesAdapter::class.java.getSimpleName()

    override fun mergeView(event: Event, position: Int, shouldBeMerged: Boolean): Boolean {
        return false
    }

    override fun supportMessageRowMerge(row: MessageRow): Boolean {
        return false
    }

    override fun getView(position: Int, convertView2: View?, parent: ViewGroup): View {
        val convertView = super.getView(position, convertView2, parent)

        try {
            val roomId = getItem(position)?.event?.roomId
            var roomName: String? = null
            var isOutgoingMsg = false

            getItem(position)?.event
                    ?.let { event ->
                        isOutgoingMsg = TextUtils.equals(mSession.myUserId, event.getSender())
                        mSession.dataHandler.getRoom(event.roomId, false)
                                ?.let { room ->
                                    if (!room.isDirect || isOutgoingMsg) roomName = room.getRoomDisplayName(mContext, null)
                                }
                    }

            // display the room name and the date
            val header = convertView.findViewById<View>(R.id.messagesAdapter_header_view)
            val date = date(position)

            if (!TextUtils.isEmpty(roomName) || !TextUtils.isEmpty(date)) {
                header.setVisibility(View.VISIBLE)

                val roomNameText = convertView.findViewById<TextView>(R.id.messagesAdapter_favourite_message_header_room_name)
                roomNameText.setText(roomName)
                val dateText = convertView.findViewById<TextView>(R.id.messagesAdapter_favourite_message_header_date)
                dateText.setText(date)

                var leftInDP = 59
                var rightInDP = 41
                if (isOutgoingMsg) {
                    leftInDP += 12
                    rightInDP = 19
                }
                val leftMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, leftInDP.toFloat(), mContext.resources
                        .displayMetrics).toInt()
                val rightMargin = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, rightInDP.toFloat(), mContext.resources
                        .displayMetrics).toInt()
                val headerParam = header.layoutParams as RelativeLayout.LayoutParams
                headerParam.setMargins(leftMargin, headerParam.topMargin, rightMargin, headerParam.bottomMargin)

            } else {
                header.setVisibility(View.GONE)
            }

            // display timestamp
            val timeTextView = convertView.findViewById<TextView>(R.id.messagesAdapter_timestamp)
            timeTextView?.visibility = View.VISIBLE

            // hide star icon
            val favouriteIcon = convertView.findViewById<View>(R.id.messagesAdapter_favourite_icon)
            favouriteIcon?.visibility = View.GONE

            convertView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    if (null != mVectorMessagesAdapterEventsListener) {
                        mVectorMessagesAdapterEventsListener.onContentClick(position)
                    }
                }
            })

            convertView.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View): Boolean {
                    return if (null != mVectorMessagesAdapterEventsListener) {
                        mVectorMessagesAdapterEventsListener.onContentLongClick(position)
                    } else false

                }
            })
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "## getView() failed " + t.message, t)
        }

        return convertView
    }

    private fun date(position: Int): String? {
        var messageDate: Date? = null

        synchronized(this) {
            if (position < mMessagesDateList.size) {
                messageDate = mMessagesDateList[position]
            }
        }

        // sanity check
        return if (null == messageDate) {
            null
        } else dateDiff(messageDate, (mReferenceDate.time - messageDate!!.getTime()) / AdapterUtils.MS_IN_DAY)

    }
}
