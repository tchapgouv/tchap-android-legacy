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
import android.view.LayoutInflater;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomEmailInvitation;
import org.matrix.androidsdk.data.RoomPreviewData;
import org.matrix.androidsdk.data.RoomTag;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;
import org.matrix.androidsdk.rest.model.RoomMember;
import org.matrix.androidsdk.rest.model.pid.RoomThirdPartyInvite;
import org.matrix.androidsdk.util.Log;

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
import im.vector.activity.VectorRoomCreationActivity;
import im.vector.adapters.ParticipantAdapterItem;
import im.vector.contacts.Contact;
import im.vector.contacts.ContactsManager;
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
                                    DinsicUtils.editContactForm(theContext,activity,activity.getString(R.string.people_edit_contact_warning_msg),item.mContact);
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

    public static boolean participantAlreadyAdded(List<ParticipantAdapterItem> participants, ParticipantAdapterItem participant){

        boolean find = false;
        Iterator<ParticipantAdapterItem> iterator = participants.iterator();
        boolean finish = !iterator.hasNext();
        while (!finish){
            ParticipantAdapterItem curp = iterator.next();
            if (curp!= null)
                find = curp.mUserId.equals(participant.mUserId);
            finish = (find || !(iterator.hasNext()));
        }

        return find;

    }

    public static boolean removeParticipantIfExist(List<ParticipantAdapterItem> participants, ParticipantAdapterItem participant){

        boolean find = false;
        Iterator<ParticipantAdapterItem> iterator = participants.iterator();
        boolean finish = !iterator.hasNext();
        while (!finish){
            ParticipantAdapterItem curp = iterator.next();
            if (curp!= null) {
                find = curp.mUserId.equals(participant.mUserId);
                if (find) iterator.remove();
            }
            finish = (find || !(iterator.hasNext()));
        }

        return find;
    }

    public static void alertSimpleMsg(FragmentActivity activity, String msg){
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
        Room existingRoom = VectorRoomCreationActivity.isDirectChatRoomAlreadyExist(participantId, session, true);
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
                DinsicUtils.alertSimpleMsg(activity, activity.getString(R.string.room_creation_forbidden));
            }
        }
        return succeeded;
    }

    /**
     * Prepare a direct chat with a selected contact.
     *
     * @param activity  the current activity
     * @param session   the current session
     * @param selectedContact the selected contact
     */
    public static void startDirectChat(final RiotAppCompatActivity activity, final MXSession session, final ParticipantAdapterItem selectedContact) {
        if (selectedContact.mIsValid) {

            // Tell if contact is tchap user
            if (MXSession.isUserId(selectedContact.mUserId)) { // || DinsicUtils.isFromFrenchGov(item.mContact.getEmails()))
                // The contact is a Tchap user
                if (DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, false)) {
                    // If a direct chat already exist with him, open it
                    DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, true);
                } else {
                    // If it's a Tchap user without a direct chat with him
                    // Display a popup to confirm the creation of a new direct chat with him
                    String msg = activity.getResources().getString(R.string.start_new_chat_prompt_msg, selectedContact.mDisplayName);
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                    alertDialogBuilder.setMessage(msg);

                    // set dialog message
                    alertDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, true);
                                        }
                                    })
                            .setNegativeButton(R.string.cancel, null);

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    // show it
                    alertDialog.show();
                }
            } else {
                // The contact isn't a Tchap user
                String msg = activity.getResources().getString(R.string.room_invite_non_gov_people);
                if (DinsicUtils.isFromFrenchGov(selectedContact.mContact.getEmails()))
                    msg = activity.getResources().getString(R.string.room_invite_gov_people);

                if (!DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, false)) {
                    if (TchapLoginActivity.isUserExternal(session)) {
                        DinsicUtils.alertSimpleMsg(activity, activity.getResources().getString(R.string.room_creation_forbidden));
                    } else {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
                        alertDialogBuilder.setMessage(msg);

                        // set dialog message
                        alertDialogBuilder
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                DinsicUtils.openDirectChat(activity, selectedContact.mUserId, session, true);
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
            DinsicUtils.editContact(activity, activity, selectedContact);
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

            if (null != roomPreviewData.getEventId()) {
                params.put(VectorRoomActivity.EXTRA_EVENT_ID, roomPreviewData.getEventId());
            }

            // clear the activity stack to home activity
            Intent intent = new Intent(activity, VectorHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.putExtra(VectorHomeActivity.EXTRA_JUMP_TO_ROOM_PARAMS, params);
            activity.startActivity(intent);
            activity.finish();
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
}
