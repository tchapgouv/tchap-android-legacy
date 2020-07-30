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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.TRANSCRIPT_MODE_DISABLED
import androidx.appcompat.app.AlertDialog
import fr.gouv.tchap.adapters.TchapFavouriteMessagesAdapter
import fr.gouv.tchap.util.convertDaysToMs
import fr.gouv.tchap.util.getJoinedRooms
import fr.gouv.tchap.util.getRoomRetention
import im.vector.R
import im.vector.activity.VectorRoomActivity
import im.vector.adapters.VectorMessagesAdapter
import im.vector.fragments.VectorMessageListFragment
import org.matrix.androidsdk.adapters.MessageRow
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.core.model.MatrixError
import org.matrix.androidsdk.data.RoomState
import org.matrix.androidsdk.rest.model.Event
import org.matrix.androidsdk.rest.model.TaggedEventInfo


class TchapFavouriteMessagesFragment : VectorMessageListFragment() {
    private val LOG_TAG = TchapFavouriteMessagesFragment::class.java.getSimpleName()

    private val favouriteEvents = ArrayList<FavouriteEvent>()
    private var paginationIndex = 0
    private var isPaginating = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // Force the adapter in preview mode to hide some actions on selected event
        mAdapter.setIsPreviewMode(true)

        mMessageListView.adapter = mAdapter

        // Prevent the list from scrolling down automatically
        mMessageListView.transcriptMode = TRANSCRIPT_MODE_DISABLED
        //mMessageListView.lockSelectionOnResize()

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

    override fun onContentClick(position: Int) {
        mAdapter.getItem(position)?.event?.let { event ->
            val intent = Intent(activity, VectorRoomActivity::class.java)
            intent.putExtra(VectorRoomActivity.EXTRA_MATRIX_ID, mSession.myUserId)
            intent.putExtra(VectorRoomActivity.EXTRA_ROOM_ID, event.roomId)
            intent.putExtra(VectorRoomActivity.EXTRA_EVENT_ID, event.eventId)

            activity?.let { it.startActivity(intent) }
        }
    }

    fun refreshFavouriteEvents(): Int {
        favouriteEvents.clear()

        val joinedRooms = getJoinedRooms(session)
        for (room in joinedRooms) {
            val limitEventTs = System.currentTimeMillis() - convertDaysToMs(getRoomRetention(room))
            room.accountData
                    ?.let { roomAccountData ->
                        roomAccountData.favouriteEventIds
                                ?.takeIf { it.isNotEmpty() }
                                ?.map { eventId ->
                                    roomAccountData.favouriteEventInfo(eventId)
                                            // Ignore the favourite events which are out of the room retention period
                                            ?.takeIf { it.originServerTs == null || it.originServerTs!! >= limitEventTs }
                                            ?.let { eventInfo ->
                                                favouriteEvents.add(FavouriteEvent(room.roomId, eventId, eventInfo, room.state))
                                            }
                                }
                    }
        }

        favouriteEvents.apply { sortByDescending { it.eventInfo.originServerTs } }

        // Refresh favourite events display
        refreshDisplay()

        return favouriteEvents.size
    }

    fun favouriteEventsCount(): Int {
        return favouriteEvents.size
    }

    private fun refreshDisplay() {
        mAdapter.clear();
        paginationIndex = 0
        isPaginating = true

        showInitLoading()

        paginate(PAGINATION_LIMIT, object : ApiCallback<Void> {
            private fun done(isSuccessful: Boolean) {
                isPaginating = false
                hideInitLoading()

                if (!isSuccessful) {
                    // Prompt the user on the limitations
                    activity?.let {
                        AlertDialog.Builder(it)
                                .setTitle(R.string.favourite_offline_notification)
                                .setMessage(R.string.favourite_offline_message)
                                .setPositiveButton(R.string.ok, null)
                                .show()
                    }
                }
            }

            override fun onSuccess(info: Void?) {
                done(true)
            }

            override fun onNetworkError(e: Exception) {
                Log.e(LOG_TAG, "## refreshDisplay(): " + e.localizedMessage)
                done(false)
            }

            override fun onMatrixError(e: MatrixError) {
                Log.e(LOG_TAG, "## refreshDisplay(): " + e.localizedMessage)
                done(false)
            }

            override fun onUnexpectedError(e: Exception) {
                Log.e(LOG_TAG, "## refreshDisplay(): " + e.localizedMessage)
                done(false)
            }
        })
    }

    private fun paginate(limit: Int, callback: ApiCallback<Void> ) {
        if (paginationIndex < favouriteEvents.size) {
            session.dataHandler.store
                    ?.takeIf { it.isReady }
                    ?.let { store ->
                        val favourite = favouriteEvents[paginationIndex]
                        session.dataHandler.dataRetriever.getEvent(store, favourite.roomId, favourite.eventId, object : ApiCallback<Event> {
                            override fun onSuccess(event: Event) {
                                paginationIndex ++
                                session.dataHandler.decryptEvent(event, null)
                                mAdapter.add(MessageRow(event, favourite.roomState))
                                if (paginationIndex < limit) {
                                    paginate(limit, callback)
                                } else {
                                    callback.onSuccess(null)
                                }
                            }

                            override fun onNetworkError(e: Exception) {
                                callback.onNetworkError(e)
                            }

                            override fun onUnexpectedError(e: Exception) {
                                callback.onUnexpectedError(e)
                            }

                            override fun onMatrixError(e: MatrixError) {
                                when (e.errcode) {
                                    MatrixError.NOT_FOUND -> {
                                        favouriteEvents.removeAt(paginationIndex)
                                        if (paginationIndex < limit) {
                                            paginate(limit, callback)
                                        } else {
                                            callback.onSuccess(null)
                                        }
                                    }
                                    else -> {
                                        callback.onMatrixError(e)
                                    }
                                }
                            }
                        } )
                        return
                    }
        }

        callback.onSuccess(null)
    }


    data class FavouriteEvent(val roomId: String, val eventId: String, val eventInfo: TaggedEventInfo, val roomState: RoomState)

    companion object {
        fun newInstance(matrixId: String, layoutResId: Int): TchapFavouriteMessagesFragment {
            val frag = TchapFavouriteMessagesFragment()
            frag.arguments = getArguments(matrixId, null, layoutResId)
            return frag
        }

        private const val PAGINATION_LIMIT = 30
    }
}
