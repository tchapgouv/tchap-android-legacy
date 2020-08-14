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

package fr.gouv.tchap.activity

import android.widget.TextView
import butterknife.BindView
import fr.gouv.tchap.fragments.TchapFavouriteMessagesFragment
import im.vector.Matrix
import im.vector.R
import im.vector.activity.VectorAppCompatActivity
import im.vector.fragments.VectorMessageListFragment
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.rest.model.Event
import im.vector.VectorApp
import org.matrix.androidsdk.listeners.MXEventListener


class TchapFavouriteMessagesActivity : VectorAppCompatActivity(), VectorMessageListFragment.VectorMessageListFragmentListener {
    /* ==========================================================================================
     * DATA
     * ========================================================================================== */

    private lateinit var session: MXSession

    private var eventsListener: MXEventListener? = null

    private lateinit var favouriteMessagesFragment: TchapFavouriteMessagesFragment

    @BindView(R.id.favourite_msg_action_bar_title)
    lateinit var actionBarCustomTitle: TextView

    @BindView(R.id.favourite_msg_action_bar_subtitle)
    lateinit var actionBarCustomSubTitle: TextView

    /* ==========================================================================================
     * Life cycle
     * ========================================================================================== */

    override fun getLayoutRes() = R.layout.activity_tchap_favourite_messages

    override fun initUiAndData() {
        super.initUiAndData()

        waitingView = findViewById(R.id.waiting_view)

        // Get the session
        val defaultSession = Matrix.getInstance(this).defaultSession
        if (defaultSession == null || defaultSession.isAlive == false) {
            Log.e(LOG_TAG, "No Session!")
            finish()
            return
        }

        session = defaultSession

        configureToolbar()
        actionBarCustomTitle.text = getString(R.string.favourite_messages_title)

        if (isFirstCreation()) {
            favouriteMessagesFragment = TchapFavouriteMessagesFragment.newInstance(session.myUserId, R.layout.fragment_matrix_message_list_fragment)
            // display the fragment
            supportFragmentManager.beginTransaction()
                    .replace(R.id.anchor_fragment_messages, favouriteMessagesFragment, FRAGMENT_TAG)
                    .commit()
        } else {
            favouriteMessagesFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as TchapFavouriteMessagesFragment
        }

        favouriteMessagesFragment.setListener(this)
    }

    override fun onResume() {
        super.onResume()

        if (session.isAlive()) {
            addEventsListener()
        }
    }

    override fun onPause() {
        super.onPause()

        if (session.isAlive()) {
            removeEventsListener()
        }
    }

    /**
     * Add a MXEventListener to the session listeners.
     */
    private fun addEventsListener() {
        eventsListener = object : MXEventListener() {
            // set to true when a refresh must be triggered
            private var refreshOnChunkEnd = false

            private fun refresh() {
                favouriteMessagesFragment.refreshFavouriteEvents()
                refreshTitle()
            }

            override fun onInitialSyncComplete(toToken: String?) {
                Log.d(LOG_TAG, "## onInitialSyncComplete()")
                refresh()
            }

            override fun onLiveEventsChunkProcessed(fromToken: String?, toToken: String?) {
                if (VectorApp.getCurrentActivity() == this@TchapFavouriteMessagesActivity && refreshOnChunkEnd) {
                    refresh()
                }

                refreshOnChunkEnd = false
            }

            override fun onTaggedEventsEvent(roomId: String?) {
                refreshOnChunkEnd = true
            }

            override fun onStoreReady() {
                refresh()
            }

            override fun onLeaveRoom(roomId: String?) {
                // Remove favourite events from this room
                refreshOnChunkEnd = true
            }

            override fun onEventDecrypted(roomId: String?, eventId: String?) {
                // TODO check whether this method is useful here
            }
        }

        session.dataHandler.addListener(eventsListener)
    }

    /**
     * Remove the MXEventListener to the session listeners.
     */
    private fun removeEventsListener() {
        if (session.isAlive()) {
            session.dataHandler.removeListener(eventsListener)
        }
    }

    private fun refreshTitle() {
        val favNb = favouriteMessagesFragment.favouriteEventsCount()
        actionBarCustomSubTitle.text = getResources().getQuantityString(R.plurals.favourite_messages_subtitle,
                favNb, favNb)
    }

    /* ==========================================================================================
     * Implement VectorMessageListFragmentListener
     * ========================================================================================== */

    override fun showPreviousEventsLoadingWheel() {
    }

    override fun hidePreviousEventsLoadingWheel() {
    }

    override fun showNextEventsLoadingWheel() {
        showWaitingView()
    }

    override fun hideNextEventsLoadingWheel() {
        hideWaitingView()
        refreshTitle()
    }

    override fun showMainLoadingWheel() {
        showWaitingView()
    }

    override fun hideMainLoadingWheel() {
        hideWaitingView()
        refreshTitle()
    }

    override fun replyTo(event: Event) {
    }

    companion object {
        private val LOG_TAG = TchapFavouriteMessagesActivity::class.java.getSimpleName()
        private const val FRAGMENT_TAG = "TchapFavouriteMessagesFragment"
    }
}
