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
import android.widget.AbsListView
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
    private var favouriteEventIndex = 0
    private var isPaginating = false
    private var paginationId = 0

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
        // Do nothing here (we don't want to jump to the bottom of the list)
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
        Log.i(LOG_TAG, "## refreshDisplay()")

        // Remove the potential scroll listener
        mMessageListView.setOnScrollListener(null)

        paginationId++
        mAdapter.clear();
        favouriteEventIndex = 0

        paginate(paginationId, PAGINATION_LIMIT)

        // Restore the scroll listener
        mMessageListView.setOnScrollListener(scrollListener)
    }

    private fun addItems() {
        // Ignore if a pagination is in progress, or if all events are displayed
        if (isPaginating || favouriteEventIndex >= favouriteEvents.size) {
            Log.i(LOG_TAG, "## addItems(): ignored")
            return
        }

        Log.i(LOG_TAG, "## addItems()")

        paginationId++
        paginate(paginationId, favouriteEventIndex + PAGINATION_LIMIT)
    }

    private fun paginate(id: Int, limit: Int) {
        isPaginating = true
        Log.i(LOG_TAG, "## paginate(): id = " + id)

        showInitLoading()

        innerPaginate(id,  limit, object : ApiCallback<Void> {
            private fun done(isSuccessful: Boolean) {
                Log.i(LOG_TAG, "## paginate(): done, id =" + id)
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
                Log.e(LOG_TAG, "## paginate(): " + e.localizedMessage)
                done(false)
            }

            override fun onMatrixError(e: MatrixError) {
                Log.e(LOG_TAG, "## paginate(): " + e.localizedMessage)
                done(false)
            }

            override fun onUnexpectedError(e: Exception) {
                Log.e(LOG_TAG, "## paginate(): " + e.localizedMessage)
                done(false)
            }
        })
    }

    private fun innerPaginate(id: Int, limit: Int, callback: ApiCallback<Void> ) {
        session.dataHandler.store
                ?.takeIf { it.isReady }
                ?.let { store ->
                    if (favouriteEventIndex < favouriteEvents.size) {
                        val favourite = favouriteEvents[favouriteEventIndex]
                        session.dataHandler.dataRetriever.getEvent(store, favourite.roomId, favourite.eventId, object : ApiCallback<Event> {
                            override fun onSuccess(event: Event) {
                                // Check whether the pagination has not been cancelled by another one
                                if (id == paginationId) {
                                    favouriteEventIndex ++
                                    session.dataHandler.decryptEvent(event, null)
                                    mAdapter.add(MessageRow(event, favourite.roomState))
                                    if (favouriteEventIndex < limit) {
                                        innerPaginate(id, limit, callback)
                                    } else {
                                        callback.onSuccess(null)
                                    }
                                }
                            }

                            override fun onNetworkError(e: Exception) {
                                if (id == paginationId) {
                                    callback.onNetworkError(e)
                                }
                            }

                            override fun onUnexpectedError(e: Exception) {
                                if (id == paginationId) {
                                    callback.onUnexpectedError(e)
                                }
                            }

                            override fun onMatrixError(e: MatrixError) {
                                if (id == paginationId) {
                                    when (e.errcode) {
                                        MatrixError.NOT_FOUND -> {
                                            favouriteEvents.removeAt(favouriteEventIndex)
                                            if (favouriteEventIndex < limit) {
                                                innerPaginate(id, limit, callback)
                                            } else {
                                                callback.onSuccess(null)
                                            }
                                        }
                                        else -> {
                                            callback.onMatrixError(e)
                                        }
                                    }
                                }
                            }
                        } )
                    } else {
                        // All favourite events have been retrieved
                        callback.onSuccess(null)
                    }
                }
    }

    private val scrollListener = object : AbsListView.OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            // Check only when the user scrolls the content
            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                if (mMessageListView.lastVisiblePosition + 10 >= mMessageListView.count) {
                    addItems()
                }
            }
        }
        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            if (firstVisibleItem + visibleItemCount + 10 >= totalItemCount) {
                addItems()
            }
        }
    }

    data class FavouriteEvent(val roomId: String, val eventId: String, val eventInfo: TaggedEventInfo, val roomState: RoomState)

    companion object {
        fun newInstance(matrixId: String, layoutResId: Int): TchapFavouriteMessagesFragment {
            val frag = TchapFavouriteMessagesFragment()
            frag.arguments = getArguments(matrixId, null, layoutResId)
            return frag
        }

        private const val PAGINATION_LIMIT = 20
    }
}
