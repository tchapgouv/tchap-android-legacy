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

package fr.gouv.tchap.fragments

import android.content.Intent
import android.os.Bundle
import fr.gouv.tchap.adapters.TchapFavouriteMessagesAdapter
import im.vector.activity.VectorRoomActivity
import im.vector.adapters.VectorMessagesAdapter
import im.vector.fragments.VectorMessageListFragment
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import fr.gouv.tchap.util.getJoinedRooms
import org.matrix.androidsdk.adapters.MessageRow
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.data.RoomState
import org.matrix.androidsdk.rest.model.TaggedEventInfo
import java.util.*
import kotlin.collections.ArrayList


class TchapFavouriteMessagesFragment : VectorMessageListFragment() {
    private val LOG_TAG = TchapFavouriteMessagesFragment::class.java.getSimpleName()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // Retrieve the favourites events
        refreshFavouriteMessages()

        return view
    }

    override fun createMessagesAdapter(): VectorMessagesAdapter? {
        val favouriteMessagesAdapter = activity?.let { TchapFavouriteMessagesAdapter(mSession, it, mxMediaCache) }
        // Add the current media scan manager if any
        if (null != mMediaScanManager) {
            favouriteMessagesAdapter?.setMediaScanManager(mMediaScanManager)
        }

        favouriteMessagesAdapter?.setNotifyOnChange(true)

        return favouriteMessagesAdapter
    }

    override fun onInitialMessagesLoaded() {
        // Do not jump to the bottom of the list
        Log.d(LOG_TAG, "## onInitialMessagesLoaded(): cancelled")
    }

    override fun onRowLongClick(position: Int): Boolean {
        onContentClick(position)
        return true
    }

    override fun onContentClick(position: Int) {
        mAdapter.getItem(position)?.event?.let { event ->
            val intent = Intent(activity, VectorRoomActivity::class.java)
            intent.putExtra(VectorRoomActivity.EXTRA_MATRIX_ID, mSession.myUserId)
            intent.putExtra(VectorRoomActivity.EXTRA_ROOM_ID, event.roomId)
            intent.putExtra(VectorRoomActivity.EXTRA_EVENT_ID, event.eventId)

            activity?.let { it.startActivity(intent) }
        }
    }

    /**
     * Called when a long click is performed on the message content
     *
     * @param position the cell position
     * @return true if managed
     */
    override fun onContentLongClick(position: Int): Boolean {
        return false
    }

    fun refreshFavouriteMessages(): Int {
        val messageRows = ArrayList<MessageRow>()
        val favouriteEvents = ArrayList<FavouriteEvent>()
        val joinedRooms = getJoinedRooms(session)

        for (room in joinedRooms) {
            room.accountData
                    ?.let { roomAccountData ->
                        roomAccountData.favouriteEventIds
                                ?.takeIf { it.isNotEmpty() }
                                ?.map { eventId ->
                                    roomAccountData.favouriteEventInfo(eventId)
                                            ?.let { eventInfo ->
                                                favouriteEvents.add(FavouriteEvent(room.roomId, eventId, eventInfo, room.state))
                                            }
                                }
                    }
        }

        favouriteEvents.apply { sortBy { it.eventInfo.originServerTs } }

        session.dataHandler.store
                .takeIf { it?.isReady?: false }
                ?.let { store ->
                    for (favourite in favouriteEvents) {
                        store.getEvent(favourite.eventId, favourite.roomId)
                                ?.let { event ->
                                    messageRows.add(MessageRow(event, favourite.roomState))
                                }
                    }
                }

        Collections.reverse(messageRows)

        mAdapter.clear();
        mAdapter.addAll(messageRows);

        mMessageListView.adapter = mAdapter

        return favouriteEvents.size
    }

    data class FavouriteEvent(val roomId: String, val eventId: String, val eventInfo: TaggedEventInfo, val roomState: RoomState)

    companion object {
        fun newInstance(matrixId: String, layoutResId: Int): TchapFavouriteMessagesFragment {
            val frag = TchapFavouriteMessagesFragment()
            frag.arguments = getArguments(matrixId, null, layoutResId)
            return frag
        }
    }
}
