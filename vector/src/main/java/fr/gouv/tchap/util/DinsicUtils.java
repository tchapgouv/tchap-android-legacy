/*
 * Copyright 2018 DINSIC
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

package fr.gouv.tchap.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomEmailInvitation;
import org.matrix.androidsdk.data.RoomPreviewData;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.data.store.IMXStore;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.RoomMember;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.androidsdk.rest.model.pid.RoomThirdPartyInvite;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import fr.gouv.tchap.activity.TchapLoginActivity;
import im.vector.activity.RiotAppCompatActivity;
import im.vector.activity.VectorHomeActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.adapters.ParticipantAdapterItem;
import im.vector.contacts.Contact;
import im.vector.contacts.ContactsManager;
import im.vector.contacts.PIDsRetriever;
import im.vector.util.RoomUtils;

public class DinsicUtils {
    private static final String LOG_TAG = "DinsicUtils";
    public static final String AGENT_OR_INTERNAL_SECURED_EMAIL_HOST = "gouv.fr";
    /**
     * Tells if an email is from gov
     *
     * @param email the email address to test.
     * @return true if the email address is from gov
     */
    public  static boolean isFromFrenchGov(final String email) {
        return (null != email && email.toLowerCase().endsWith (AGENT_OR_INTERNAL_SECURED_EMAIL_HOST));
    }

    /**
     * Tells if one of the email of the list  is from gov
     *
     * @param emails list to test.
     * @return true if one is from gov
     */
    public  static boolean isFromFrenchGov(final List<String> emails) {
        boolean myReturn=false;
        for (String myEmail : emails){
            if (!myReturn) myReturn = isFromFrenchGov(myEmail);
        }
        return myReturn;
    }

    /**
     * Get name part of a display name by removing the domain part if any.
     * For example in case of "Jean Martin [Modernisation]", this will return "Jean Martin".
     *
     * @param displayName
     * @return displayName without domain
     */
    public  static String getNameFromDisplayName(String displayName) {
        String myRet = displayName;
        if (displayName.contains("[")) {
            myRet = displayName.split("\\[")[0].trim();
        }
        return myRet;
    }
    /**
     * Get the potential domain name from a display name.
     * For example in case of "Jean Martin [Modernisation]", this will return "Modernisation".
     *
     * @param displayName
     * @return displayName without name, empty string if no domain is available.
     */
    public  static String getDomainFromDisplayName(String displayName) {
        String myRet = "";

        if (displayName.contains("[")) {
            myRet = displayName.split("\\[")[1];
            if (myRet.contains("]")) {
                myRet = myRet.split("\\]")[0];
            } else {
                myRet = "";
            }
        }
        return myRet.trim();
    }

    /**
     * Edit contact form
     */
    public static void editContactForm(Context theContext, Activity myActivity, String editContactWarningMsg, final Contact contact ) {
        LayoutInflater inflater = LayoutInflater.from(theContext);

        Cursor namesCur=null;
        boolean switchToContact=false;
        try
        {
            ContentResolver cr = theContext.getContentResolver();
            namesCur = cr.query(ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID
                    },
                    ContactsContract.Data.MIMETYPE + " = ? AND "+ ContactsContract.Data.CONTACT_ID + " = ?",

                    new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                            contact.getContactId()}, null);
            if (namesCur != null) {
                if (namesCur.moveToNext()) {
                    String contactId = namesCur.getString(namesCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID));
                    Uri mSelectedContactUri;
                    int mLookupKeyIndex;
                    int mIdIndex;
                    String mCurrentLookupKey;
                    mLookupKeyIndex = namesCur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                    mCurrentLookupKey = namesCur.getString(mLookupKeyIndex);
                    //mCursor.getColumnIndex(ContactsContract.Contacts._ID);
                    long mCurrentId = Integer.parseInt(contactId);//namesCur.getLong(mIdIndex);
                    mSelectedContactUri =
                            ContactsContract.Contacts.getLookupUri(mCurrentId, mCurrentLookupKey);


                    Intent editIntent = new Intent(Intent.ACTION_EDIT);
                    editIntent.setDataAndType(mSelectedContactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                    editIntent.putExtra("finishActivityOnSaveCompleted", true);
                    myActivity.startActivity(editIntent);
                    switchToContact=true;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## editContactForm(): Exception - Msg=" + e.getMessage());
        }
        if (!switchToContact){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(myActivity);
            alertDialogBuilder.setMessage(editContactWarningMsg);

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok,null);

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            // show it
            alertDialog.show();

        }
    }

    public static void editContact(final FragmentActivity activity, final Context theContext,final ParticipantAdapterItem item) {
        if (ContactsManager.getInstance().isContactBookAccessAllowed()) {
            //enterEmailAddress(item.mContact);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            alertDialogBuilder.setMessage(activity.getString(R.string.people_invalid_warning_msg));
            // set dialog message
            alertDialogBuilder
                    .setNegativeButton(R.string.cancel,null)
                    .setPositiveButton(R.string.action_edit_contact_form,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    editContactForm(theContext,activity,activity.getString(R.string.people_edit_contact_warning_msg),item.mContact);
                                }
                            });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            // show it
            alertDialog.show();

        }
        else {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            alertDialogBuilder.setMessage(activity.getString(R.string.people_invalid_warning_msg));

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();
            // show it
            alertDialog.show();

        }
    }  

    public static boolean participantAlreadyAdded(List<ParticipantAdapterItem> participants, ParticipantAdapterItem participant) {
        boolean find = false;
        Iterator<ParticipantAdapterItem> iterator = participants.iterator();
        boolean finish = !iterator.hasNext();
        while (!finish) {
            ParticipantAdapterItem curp = iterator.next();
            if (curp != null && curp.mIsValid)
                find = curp.mUserId.equals(participant.mUserId);
            finish = (find || !(iterator.hasNext()));
        }

        return find;

    }

    public static boolean removeParticipantIfExist(List<ParticipantAdapterItem> participants, ParticipantAdapterItem participant) {

        boolean find = false;
        Iterator<ParticipantAdapterItem> iterator = participants.iterator();
        boolean finish = !iterator.hasNext();
        while (!finish) {
            ParticipantAdapterItem curp = iterator.next();
            if (curp != null && curp.mIsValid) {
                find = curp.mUserId.equals(participant.mUserId);
                if (find) iterator.remove();
            }
            finish = (find || !(iterator.hasNext()));
        }

        return find;
    }

    public static void alertSimpleMsg(FragmentActivity activity, String msg) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setMessage(msg);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();

    }

    //=============================================================================================
    // Handle existing direct chat room
    //=============================================================================================

    /**
     * Select the most suitable direct chat for the provided user identifier (matrix or third party id).
     * During this search, the pending invite are considered too.
     * The selected room (if any) may be opened synchronously or not. It depends if the room is ready or not.
     *
     * @param activity current activity
     * @param participantId : participant id (matrix id ou email)
     * @param session current session
     * @param canCreate create the direct chat if it does not exist.
     * @return boolean that says if the direct chat room is found or not
     */
    public static boolean openDirectChat(final RiotAppCompatActivity activity, String participantId, final MXSession session, boolean canCreate) {
        Room existingRoom = isDirectChatRoomAlreadyExist(participantId, session, true);
        boolean succeeded = false;

        // direct message api callback
        ApiCallback<String> prepareDirectChatCallBack = new ApiCallback<String>() {
            @Override
            public void onSuccess(final String roomId) {
                activity.hideWaitingView();

                HashMap<String, Object> params = new HashMap<>();
                params.put(VectorRoomActivity.EXTRA_MATRIX_ID, session.getMyUserId());
                params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
                params.put(VectorRoomActivity.EXTRA_EXPAND_ROOM_HEADER, true);

                Log.d(LOG_TAG, "## prepareDirectChatCallBack: onSuccess - start goToRoomPage");
                CommonActivityUtils.goToRoomPage(activity, session, params);
            }

            private void onError(final String message) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != message) {
                            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                        }
                        activity.hideWaitingView();
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
        };

        if (null != existingRoom) {
            // If I am invited to this room, I accept invitation and join it
            if (existingRoom.isInvited()) {
                succeeded = true;
                activity.showWaitingView();
                session.joinRoom(existingRoom.getRoomId(), prepareDirectChatCallBack);
            } else {
                succeeded = true;
                HashMap<String, Object> params = new HashMap<>();
                params.put(VectorRoomActivity.EXTRA_MATRIX_ID, participantId);
                params.put(VectorRoomActivity.EXTRA_ROOM_ID, existingRoom.getRoomId());
                CommonActivityUtils.goToRoomPage(activity, session, params);
            }
        } else if (canCreate){
            // direct message flow
            //it will be more open on next sprints ...
            if (!TchapLoginActivity.isUserExternal(session)) {
                succeeded = true;
                activity.showWaitingView();
                session.createDirectMessageRoom(participantId, prepareDirectChatCallBack);
            } else {
                alertSimpleMsg(activity, activity.getString(R.string.room_creation_forbidden));
            }
        }
        return succeeded;
    }

    /**
     * Return the first direct chat room for a given user ID.
     *
     * @param aUserId user ID to search for
     * @param mSession current session
     * @param includeInvite boolean to tell us if pending invitations have to be consider or not
     * @return a room ID if search succeed, null otherwise.
     */
    public static Room isDirectChatRoomAlreadyExist(String aUserId, MXSession mSession, boolean includeInvite) {
        if (null != mSession) {
            IMXStore store = mSession.getDataHandler().getStore();
            HashMap<String, List<String>> directChatRoomsDict;

            if (null != store.getDirectChatRoomsDict()) {
                directChatRoomsDict = new HashMap<>(store.getDirectChatRoomsDict());

                if (directChatRoomsDict.containsKey(aUserId)) {
                    ArrayList<String> roomIdsList = new ArrayList<>(directChatRoomsDict.get(aUserId));

                    if (!roomIdsList.isEmpty()) {
                        // In the description of the memberships, we display first the current user status and the other member in second.
                        // We review all the direct chats by considering the memberships in the following priorities :
                        // 1. join-join
                        // 2. invite-join
                        // 3. join-invite
                        // 4. join-left (or invite-left)
                        // The case left-x isn't possible because we ignore for the moment the left rooms.
                        Room roomCandidateLeftByOther = null;
                        Room roomCandidatePendingInvite = null;
                        boolean isPendingInvite = false;

                        for (String roomId : roomIdsList) {
                            Room room = mSession.getDataHandler().getRoom(roomId, false);
                            // check if the room is already initialized
                            if ((null != room) && room.isReady() && !room.isLeaving()) {
                                isPendingInvite = room.isInvited();
                                if (includeInvite || !isPendingInvite) {
                                    // dinsic: if the member is not already in matrix and just invited he's not active but
                                    // the room can be considered as ok
                                    if (!MXSession.isUserId(aUserId)) {
                                        Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): for user: " + aUserId + " room id: " + roomId);
                                        return room;
                                    } else {
                                        RoomMember member = room.getMember(aUserId);

                                        if (null != member) {
                                            if (member.membership.equals(RoomMember.MEMBERSHIP_JOIN)) {
                                                if (!isPendingInvite) {
                                                    // the other user is present in this room (join-join)
                                                    Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): for user: " + aUserId + " (join) room id: " + roomId);
                                                    return room;
                                                } else {
                                                    // I am invited by the other member (invite-join)
                                                    // We consider first de case "invite-join" compare to "join-invite"
                                                    Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): set candidate (invite-join) room id: " + roomId);
                                                    roomCandidatePendingInvite = room;
                                                }
                                            } else if (member.membership.equals(RoomMember.MEMBERSHIP_INVITE)) {
                                                // the other user is invited (join-invite)
                                                if (roomCandidatePendingInvite == null) {
                                                    Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): set candidate (join-invite) room id: " + roomId);
                                                    roomCandidatePendingInvite = room;
                                                }
                                            } else if (member.membership.equals(RoomMember.MEMBERSHIP_LEAVE)) {
                                                // the other member has left this room
                                                // and I can be invite or join
                                                Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): set candidate (join-left) room id: " + roomId);
                                                roomCandidateLeftByOther = room;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // check if an invitation is pending
                        if (null != roomCandidatePendingInvite) {
                            Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): user: " + aUserId + " (invite) room id: " + roomCandidatePendingInvite.getRoomId());
                            return roomCandidatePendingInvite;
                        }

                        // by default we consider the room left by the other member
                        if (null != roomCandidateLeftByOther) {
                            Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): user: " + aUserId + " (leave) room id: " + roomCandidateLeftByOther.getRoomId());
                            return roomCandidateLeftByOther;
                        }
                    }
                }
            }
        }
        Log.d(LOG_TAG, "## isDirectChatRoomAlreadyExist(): for user=" + aUserId + " no found room");
        return null;
    }

    /**
     * Prepare a direct chat with a selected contact.
     *
     * @param activity  the current activity
     * @param session   the current session
     * @param selectedContact the selected contact
     */
    public static void startDirectChat (final RiotAppCompatActivity activity, final MXSession session, final ParticipantAdapterItem selectedContact) {
        if (selectedContact.mIsValid) {
            // Tell if contact is tchap user
            if (MXSession.isUserId(selectedContact.mUserId)) {
                // The contact is a Tchap user, try to get the corresponding User instance.
                User tchapUser = session.getDataHandler().getUser(selectedContact.mUserId);
                // The return value is null if we don't already share a room with him.
                if (null == tchapUser) {
                    tchapUser = new User();
                    tchapUser.user_id = selectedContact.mUserId;
                    tchapUser.avatar_url = selectedContact.mAvatarUrl;
                    tchapUser.displayname = selectedContact.mDisplayName;
                }
                startDirectChat(activity, session, tchapUser);
            } else {
                // The contact isn't a Tchap user
                String msg = activity.getResources().getString(R.string.room_invite_non_gov_people);
                if (isFromFrenchGov(selectedContact.mContact.getEmails()))
                    msg = activity.getResources().getString(R.string.room_invite_gov_people);

                if (!openDirectChat(activity, selectedContact.mUserId, session, false)) {
                    if (TchapLoginActivity.isUserExternal(session)) {
                        alertSimpleMsg(activity, activity.getResources().getString(R.string.room_creation_forbidden));
                    } else {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                        alertDialogBuilder.setMessage(msg);

                        // set dialog message
                        alertDialogBuilder
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                openDirectChat(activity, selectedContact.mUserId, session, true);
                                            }
                                        })
                                .setNegativeButton(R.string.cancel, null);

                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        // show it
                        alertDialog.show();
                    }
                }
            }
        } else { // tell the user that the email must be filled. Propose to fill it
            editContact(activity, activity, selectedContact);
        }
    }

    /**
     * Prepare a direct chat with a tchap user.
     *
     * @param activity      the current activity
     * @param session       the current session
     * @param selectedUser  the selected tchap user
     */
    public static void startDirectChat(final RiotAppCompatActivity activity, final MXSession session, User selectedUser) {
        // Consider here that the provided id is a correct matrix identifier, we don't check again
        // Try first to open an existing direct chat
        if (!openDirectChat(activity, selectedUser.user_id, session, false)) {
            // There is no direct chat with him yet
            // Display a fake room, the actual room will be created on the first message
            HashMap<String, Object> params = new HashMap<>();
            params.put(VectorRoomActivity.EXTRA_MATRIX_ID, session.getMyUserId());
            params.put(VectorRoomActivity.EXTRA_TCHAP_USER, selectedUser);
            CommonActivityUtils.goToRoomPage(activity, session, params);
        }
    }

    //=============================================================================================
    // Handle Rooms
    //=============================================================================================

    /**
     * Join a room described by an instance of the RoomPreviewData class.
     *
     * @param roomPreviewData the room preview data
     */
    public static void joinRoom(final RoomPreviewData roomPreviewData, final ApiCallback<Void> callback) {
        MXSession session = roomPreviewData.getSession();
        Room room = session.getDataHandler().getRoom(roomPreviewData.getRoomId());
        RoomEmailInvitation roomEmailInvitation = roomPreviewData.getRoomEmailInvitation();

        String signUrl = null;

        if (null != roomEmailInvitation) {
            signUrl = roomEmailInvitation.signUrl;
        }

        // Patch: Check in the current room state if a third party invite has been accepted by the tchap user.
        // Save this information in the room preview data before joining the room
        // because the room state will be flushed during this operation.
        // This information will be useful to consider or not the new joined room as a direct chat (see processDirectMessageRoom).
        RoomMember roomMember = room.getMember(session.getMyUserId());
        if (null != roomMember && null != roomMember.thirdPartyInvite && null == roomPreviewData.getRoomState()) {
            if (null != room.getLiveState().memberWithThirdPartyInviteToken(roomMember.thirdPartyInvite.signed.token)) {
                Log.d(LOG_TAG, "## joinRoom: save third party invites in the room preview.");
                roomPreviewData.setRoomState(room.getLiveState());
            }
        }

        room.joinWithThirdPartySigned(roomPreviewData.getRoomIdOrAlias(), signUrl, callback);
    }

    /**
     * A new room has been joined.
     * - check whether this is a direct chat.
     * - open the room activity for this room.
     *
     * @param activity         the current activity. This activity is closed when the joined room is valid.
     * @param roomPreviewData  the room preview data
     */
    public static void onNewJoinedRoom(Activity activity, final RoomPreviewData roomPreviewData) {
        MXSession session = roomPreviewData.getSession();

        Room room = session.getDataHandler().getRoom(roomPreviewData.getRoomId());
        if (null != room) {
            // Check first whether this new room is a direct one.
            String myUserId = session.getMyUserId();
            Collection<RoomMember> members = room.getMembers();

            if (2 == members.size()) {
                Boolean isDirectInvite = room.isDirectChatInvitation();

                if (!isDirectInvite) {
                    // Consider here the 3rd party invites for which the is_direct flag is not available.
                    Collection<RoomThirdPartyInvite> thirdPartyInvites = room.getLiveState().thirdPartyInvites();
                    // Consider the case where only one invite has been observed.
                    if (thirdPartyInvites.size() == 1) {
                        Log.d(LOG_TAG, "## onNewJoinedRoom(): Consider the third party invite");
                        RoomThirdPartyInvite invite = thirdPartyInvites.iterator().next();

                        // Check whether the user has accepted this third party invite or not
                        RoomMember roomMember = room.getLiveState().memberWithThirdPartyInviteToken(invite.token);
                        if (null != roomMember && roomMember.getUserId().equals(myUserId)) {
                            isDirectInvite = true;
                        } else if (null != roomPreviewData.getRoomState()){
                            // Most of the time the room state is not ready, the pagination is in progress
                            // Consider here the room state saved in the room preview (before joining the room).
                            roomMember = roomPreviewData.getRoomState().memberWithThirdPartyInviteToken(invite.token);
                            if (null != roomMember && roomMember.getUserId().equals(myUserId)) {
                                isDirectInvite = true;
                            }
                        }
                    }
                }

                if (isDirectInvite) {
                    Log.d(LOG_TAG, "## onNewJoinedRoom(): this new joined room is direct");
                    // test if room is already seen as "direct message"
                    if (!RoomUtils.isDirectChat(session, roomPreviewData.getRoomId())) {
                        // search for the second participant
                        String participantUserId;
                        for (RoomMember member : members) {
                            if (!member.getUserId().equals(myUserId)) {
                                participantUserId = member.getUserId();
                                CommonActivityUtils.setToggleDirectMessageRoom(session, roomPreviewData.getRoomId(), participantUserId, null);
                                break;
                            }
                        }
                    } else {
                        Log.d(LOG_TAG, "## onNewJoinedRoom(): attempt to add an already direct message room");
                    }
                }
            }

            // Then open the room activity for this room.
            HashMap<String, Object> params = new HashMap<>();
            params.put(VectorRoomActivity.EXTRA_MATRIX_ID, session.getMyUserId());
            params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomPreviewData.getRoomId());
            CommonActivityUtils.goToRoomPage(activity, session, params);
        }
    }

    /**
     * Return the Dinsic rooms comparator. We display first the pinned rooms, then we sort them by date.
     *
     * @param session
     * @param reverseOrder
     * @return comparator
     */
    public static Comparator<Room> getRoomsComparator(final MXSession session, final boolean reverseOrder) {
        return new Comparator<Room>() {
            private Comparator<Room> mRoomsDateComparator;

            // Retrieve the default room comparator by date
            private Comparator<Room> getRoomsDateComparator() {
                if (null == mRoomsDateComparator) {
                    mRoomsDateComparator = RoomUtils.getRoomsDateComparator(session, reverseOrder);
                }
                return mRoomsDateComparator;
            }

            public int compare(Room room1, Room room2) {
                // Check first whether some rooms are pinned
                final Set<String> tagsRoom1 = room1.getAccountData().getKeys();
                final boolean isPinnedRoom1 = tagsRoom1 != null && tagsRoom1.contains(RoomTag.ROOM_TAG_FAVOURITE);
                final Set<String> tagsRoom2 = room2.getAccountData().getKeys();
                final boolean isPinnedRoom2 = tagsRoom2 != null && tagsRoom2.contains(RoomTag.ROOM_TAG_FAVOURITE);

                if (isPinnedRoom1 && !isPinnedRoom2) {
                    return reverseOrder ? 1 : -1;
                } else if (!isPinnedRoom1 && isPinnedRoom2) {
                    return reverseOrder ? -1 : 1;
                }
                // Consider the last message date to sort them
                return getRoomsDateComparator().compare(room1, room2);
            }
        };
    }

    /* get contacts from direct chats */
    public static List<ParticipantAdapterItem> getContactsFromDirectChats(final MXSession mSession) {
        List<ParticipantAdapterItem> participants = new ArrayList<>();

        if ((null == mSession) || (null == mSession.getDataHandler())) {
            Log.e(LOG_TAG, "## getContactsFromDirectChats() : null session");
            return participants;
        }

        IMXStore store = mSession.getDataHandler().getStore();

        if (null != store.getDirectChatRoomsDict()) {
            // Retrieve all the keys of the direct chats HashMap (they correspond to the users with direct chats)
            List<String> keysList = new ArrayList<>(store.getDirectChatRoomsDict().keySet());

            for (String key : keysList) {
                // Check whether this key is an actual user id
                if (MXSession.isUserId(key)) {
                    // Ignore the current user if he appears in the direct chat map
                    if (key.equals(mSession.getMyUserId())) {
                        continue;
                    }

                    // Retrieve the user display name from the room members information.
                    // By this way we check that the current user has joined at least one of the direct chats for this user.
                    // The users for whom no direct is joined by the current user are ignored for the moment.
                    // @TODO Keep displaying these users in the contacts list, the problem is to get their displayname
                    // @NOTE The user displayname may be known thanks to the presence event. But
                    // it is unknown until we receive a presence event for this user.
                    List<String> roomIdsList = store.getDirectChatRoomsDict().get(key);
                    if (roomIdsList != null && !roomIdsList.isEmpty()) {
                        for (String roomId: roomIdsList) {
                            Room room = store.getRoom(roomId);
                            if (null != room) {
                                RoomMember roomMember = room.getMember(key);
                                if (null != roomMember && !TextUtils.isEmpty(roomMember.displayname)) {
                                    // Add a contact for this user
                                    Contact dummyContact = new Contact("null");
                                    dummyContact.setDisplayName(roomMember.displayname);
                                    ParticipantAdapterItem participant = new ParticipantAdapterItem(dummyContact);
                                    participant.mUserId = key;
                                    participants.add(participant);
                                    break;
                                }
                            }
                        }
                    }
                }
                else if (android.util.Patterns.EMAIL_ADDRESS.matcher(key).matches()) {
                    // Check whether this email corresponds to an actual user id, else ignore it.
                    // @TODO Trigger a lookup3Pid request if the info is not available.
                    final Contact.MXID contactMxId = PIDsRetriever.getInstance().getMXID(key);
                    if (null != contactMxId && contactMxId.mMatrixId.length() > 0) {
                        // @TODO Add MXSession API to update the HashMap in one run.
                        List<String> roomIdsList = new ArrayList<>(store.getDirectChatRoomsDict().get(key));
                        Log.d(LOG_TAG, "## getContactsFromDirectChats() update direct chat map " + roomIdsList + " " + key);
                        for (final String roomId : roomIdsList) {
                            Log.d(LOG_TAG, "## getContactsFromDirectChats() update direct chat map " + roomId);
                            // Disable first the direct chat to set it on the right user id
                            mSession.toggleDirectChatRoom(roomId, null, new ApiCallback<Void>() {
                                @Override
                                public void onSuccess(Void info) {
                                    mSession.toggleDirectChatRoom(roomId, contactMxId.mMatrixId, new ApiCallback<Void>() {
                                        @Override
                                        public void onSuccess(Void info) {
                                            Log.d(LOG_TAG, "## getContactsFromDirectChats() succeeded to update direct chat map ");
                                            // Here we used the local data of the PIDsRetriever, so the contact will be added by local contacts list.
                                            // @TODO if we support remote lookup to resolve the email, we have to add the resulting contact (but he may be already present)
                                        }

                                        private void onFails(final String errorMessage) {
                                            Log.e(LOG_TAG, "## getContactsFromDirectChats() failed to update direct chat map " + errorMessage);
                                        }

                                        @Override
                                        public void onNetworkError(Exception e) {
                                            onFails(e.getLocalizedMessage());
                                        }

                                        @Override
                                        public void onMatrixError(MatrixError e) {
                                            onFails(e.getLocalizedMessage());
                                        }

                                        @Override
                                        public void onUnexpectedError(Exception e) {
                                            onFails(e.getLocalizedMessage());
                                        }
                                    });
                                }

                                private void onFails(final String errorMessage) {
                                    Log.e(LOG_TAG, "## getContactsFromDirectChats() failed to update direct chat map " + errorMessage);
                                }

                                @Override
                                public void onNetworkError(Exception e) {
                                    onFails(e.getLocalizedMessage());
                                }

                                @Override
                                public void onMatrixError(MatrixError e) {
                                    onFails(e.getLocalizedMessage());
                                }

                                @Override
                                public void onUnexpectedError(Exception e) {
                                    onFails(e.getLocalizedMessage());
                                }
                            });
                        }
                    }
                }
            }
        }

        return participants;
    }
}
