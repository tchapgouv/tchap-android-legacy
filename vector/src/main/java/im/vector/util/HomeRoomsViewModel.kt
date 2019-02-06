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
package im.vector.util

import fr.gouv.tchap.model.TchapRoom
import fr.gouv.tchap.model.TchapSession
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.data.Room
import org.matrix.androidsdk.data.RoomTag
import org.matrix.androidsdk.util.Log

/**
 * This class is responsible for filtering and ranking rooms whenever there is a need to update in the context of the HomeScreens
 */
class HomeRoomsViewModel(private val tchapSession: TchapSession) {

    /**
     * A data class holding the result of filtering and ranking algorithm
     * A room can't be in multiple lists at the same time.
     * Order is favourites -> directChats -> otherRooms -> lowPriorities -> serverNotices
     */
    data class Result(val favourites: List<TchapRoom> = emptyList(),
                      val directChats: List<TchapRoom> = emptyList(),
                      val otherRooms: List<TchapRoom> = emptyList(),
                      val lowPriorities: List<TchapRoom> = emptyList(),
                      val serverNotices: List<TchapRoom> = emptyList()) {

        /**
         * Use this method when you need to get all the directChats, favorites included
         * Low Priorities are always excluded
         */
        fun getDirectChatsWithFavorites(): List<TchapRoom> {
            return directChats + favourites.filter { it.isDirect() }
        }

        /**
         * Use this method when you need to get all the other rooms, favorites included
         * Low Priorities are always excluded
         */
        fun getOtherRoomsWithFavorites(): List<TchapRoom> {
            return otherRooms + favourites.filter { !it.isDirect() }
        }

        /**
         * Use this method when you need to get all the joined rooms
         */
        fun getJoinedRooms(): List<TchapRoom> {
            return favourites + directChats + lowPriorities + otherRooms
        }
    }

    /**
     * The last result
     */
    var result = Result()

    /**
     * The update method
     * This method should be called whenever the room data have changed
     */
    //TODO Take it off the main thread using coroutine
    fun update(): Result {
        val favourites = ArrayList<TchapRoom>()
        val directChats = ArrayList<TchapRoom>()
        val otherRooms = ArrayList<TchapRoom>()
        val lowPriorities = ArrayList<TchapRoom>()
        val serverNotices = ArrayList<TchapRoom>()

        val joinedRooms = getJoinedRooms(tchapSession.mainSession)
        for (room in joinedRooms) {
            val tchapRoom = TchapRoom(room, tchapSession.mainSession, tchapSession.config.hasProtectedAccess)

            val tags = room.accountData?.keys ?: emptySet()
            when {
                tags.contains(RoomTag.ROOM_TAG_SERVER_NOTICE) -> serverNotices.add(tchapRoom)
                tags.contains(RoomTag.ROOM_TAG_FAVOURITE) -> favourites.add(tchapRoom)
                tags.contains(RoomTag.ROOM_TAG_LOW_PRIORITY) -> lowPriorities.add(tchapRoom)
                RoomUtils.isDirectChat(tchapSession.mainSession, room.roomId) -> directChats.add(tchapRoom)
                else -> otherRooms.add(tchapRoom)
            }
        }

        tchapSession.shadowSession?.let {
            val joinedRooms = getJoinedRooms(it)
            for (room in joinedRooms) {
                val tchapRoom = TchapRoom(room, it)

                val tags = room.accountData?.keys ?: emptySet()
                when {
                    tags.contains(RoomTag.ROOM_TAG_SERVER_NOTICE) -> serverNotices.add(tchapRoom)
                    tags.contains(RoomTag.ROOM_TAG_FAVOURITE) -> favourites.add(tchapRoom)
                    tags.contains(RoomTag.ROOM_TAG_LOW_PRIORITY) -> lowPriorities.add(tchapRoom)
                    RoomUtils.isDirectChat(it, room.roomId) -> directChats.add(tchapRoom)
                    else -> otherRooms.add(tchapRoom)
                }
            }
        }

        result = Result(
                favourites = favourites,
                directChats = directChats,
                otherRooms = otherRooms,
                lowPriorities = lowPriorities,
                serverNotices = serverNotices)
        Log.d("HomeRoomsViewModel", result.toString())
        return result
    }

    //region private methods

    private fun getJoinedRooms(session: MXSession): List<Room> {
        return session.dataHandler.store.rooms
                .filter {
                    val isJoined = it.isJoined
                    val tombstoneContent = it.state.roomTombstoneContent
                    val redirectRoom = session.dataHandler.getRoom(tombstoneContent?.replacementRoom)
                    val isVersioned = redirectRoom?.isJoined
                            ?: false
                    isJoined && !isVersioned && !it.isConferenceUserRoom
                }
    }

    //endregion
}