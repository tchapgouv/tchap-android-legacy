/*
 * Copyright 2018 New Vector Ltd
 * Copyright 2018 DINSIC
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

package fr.gouv.tchap.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Filter;

import org.matrix.androidsdk.core.Log;
import org.matrix.androidsdk.core.callback.ApiCallback;
import org.matrix.androidsdk.core.model.MatrixError;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.features.terms.TermsManager;
import org.matrix.androidsdk.listeners.MXEventListener;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.androidsdk.rest.model.search.SearchUsersResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import fr.gouv.tchap.util.DinumUtilsKt;
import im.vector.R;
import im.vector.activity.VectorAppCompatActivity;
import im.vector.activity.VectorRoomInviteMembersActivity;
import im.vector.activity.ReviewTermsActivity;
import im.vector.activity.util.RequestCodesKt;
import im.vector.adapters.ParticipantAdapterItem;
import fr.gouv.tchap.adapters.TchapContactAdapter;
import im.vector.contacts.Contact;
import im.vector.contacts.ContactsManager;
import im.vector.contacts.PIDsRetriever;
import im.vector.fragments.AbsHomeFragment;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.ui.themes.ThemeUtils;
import im.vector.util.HomeRoomsViewModel;
import im.vector.util.PermissionsToolsKt;
import im.vector.util.VectorUtils;
import im.vector.view.EmptyViewItemDecoration;
import im.vector.view.SimpleDividerItemDecoration;


public class TchapContactFragment extends AbsHomeFragment implements ContactsManager.ContactsManagerListener, AbsHomeFragment.OnRoomChangedListener {
    private static final String LOG_TAG = TchapContactFragment.class.getSimpleName();

    private static final int MAX_KNOWN_CONTACTS_FILTER_COUNT = 50;

    @BindView(R.id.recyclerview)
    RecyclerView mRecycler;

    @BindView(R.id.listView_spinner_views)
    View waitingView;

    @BindView(R.id.ly_invite_contacts_to_tchap)
    View mInviteContactLayout;

    private TchapContactAdapter mAdapter;

    private final List<Room> mDirectChats = new ArrayList<>();
    private final List<ParticipantAdapterItem> mLocalContacts = new ArrayList<>();
    // the known contacts are not sorted
    private final List<ParticipantAdapterItem> mKnownContacts = new ArrayList<>();

    // way to detect that the contacts list has been updated
    private int mContactsSnapshotSession = -1;
    private MXEventListener mEventsListener;


    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static TchapContactFragment newInstance() {
        return new TchapContactFragment();
    }

    /*
     * *********************************************************************************************
     * Fragment lifecycle
     * *********************************************************************************************
     */

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_people_and_invite;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mEventsListener = new MXEventListener() {

            @Override
            public void onPresenceUpdate(final Event event, final User user) {
                mAdapter.updateKnownContact(user);
            }
        };

        mPrimaryColor = ThemeUtils.INSTANCE.getColor(getActivity(), R.attr.vctr_tab_home);
        mSecondaryColor = ThemeUtils.INSTANCE.getColor(getActivity(), R.attr.vctr_tab_home_secondary);

        mFabColor = ContextCompat.getColor(getActivity(), R.color.tab_people);
        mFabPressedColor = ContextCompat.getColor(getActivity(), R.color.tab_people_secondary);

        initViews();

        mOnRoomChangedListener = this;

        // Initialize the filter inputs
        mCurrentFilter = mActivity.getSearchQuery();
        mAdapter.onFilterDone(mCurrentFilter);

        // Search in the user directories if a filter is already defined, except if the current user is external.
        if (!TextUtils.isEmpty(mCurrentFilter) && !DinsicUtils.isExternalTchapSession(mSession)) {
            startRemoteKnownContactsSearch(true);
        }

        // Hide Invite by email button for Tchap-secure, and for external users on Tchap-agent
        if (DinumUtilsKt.isSecure() || DinsicUtils.isExternalTchapSession(mSession)) {
            mInviteContactLayout.setVisibility(View.GONE);
        } else {
            mInviteContactLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // We launch a VectorRoomInviteMembersActivity activity to invite
                    // some non-tchap contacts by using their email
                    mActivity.createNewChat(VectorRoomInviteMembersActivity.ActionMode.SEND_INVITE, VectorRoomInviteMembersActivity.ContactsFilter.ALL_WITHOUT_TCHAP_USERS);
                }
            });
        }


        if (!ContactsManager.getInstance().isContactBookAccessRequested()) {
            PermissionsToolsKt.checkPermissions(PermissionsToolsKt.PERMISSIONS_FOR_MEMBERS_SEARCH, this, PermissionsToolsKt.PERMISSION_REQUEST_CODE);
        }

        initKnownContacts();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodesKt.TERMS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Launch again the request
            ContactsManager.getInstance().retrievePids();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mSession.getDataHandler().addListener(mEventsListener);

        ContactsManager.getInstance().addListener(this);

        // Local address book
        initContactsData();

        initContactsViews();

        mRecycler.addOnScrollListener(mScrollListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSession.isAlive()) {
            mSession.getDataHandler().removeListener(mEventsListener);
        }
        ContactsManager.getInstance().removeListener(this);

        mRecycler.removeOnScrollListener(mScrollListener);

        // cancel any search
        mSession.cancelUsersSearch();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionsToolsKt.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ContactsManager.getInstance().refreshLocalContactsSnapshot();
            } else {
                initContactsData();
            }

            // refresh the contact views
            // the placeholders might need to be updated
            initContactsViews();
        }
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected List<Room> getRooms() {
        return new ArrayList<>(mDirectChats);
    }

    @Override
    protected void onFilter(final String pattern, final OnFilterListener listener) {
        mAdapter.getFilter().filter(pattern, new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                boolean newSearch = TextUtils.isEmpty(mCurrentFilter) && !TextUtils.isEmpty(pattern);

                Log.i(LOG_TAG, "onFilterComplete " + count);
                if (listener != null) {
                    listener.onFilterDone(count);
                }

                // Search in the user directories except if the current user is external.
                if (!DinsicUtils.isExternalTchapSession(mSession)) {
                    startRemoteKnownContactsSearch(newSearch);
                } else {
                    // Search in known users
                    mAdapter.filterAccountKnownContacts(mCurrentFilter);
                }

            }
        });
    }

    @Override
    protected void onResetFilter() {
        mAdapter.getFilter().filter("", new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                Log.i(LOG_TAG, "onResetFilter " + count);
            }
        });
    }

    /*
     * *********************************************************************************************
     * UI management
     * *********************************************************************************************
     */

    /**
     * Prepare views
     */
    private void initViews() {
        int margin = (int) getResources().getDimension(R.dimen.item_decoration_left_margin);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));
        mRecycler.addItemDecoration(new SimpleDividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL, margin));
        mRecycler.addItemDecoration(new EmptyViewItemDecoration(getActivity(), DividerItemDecoration.VERTICAL, 40, 16, 14));
        mAdapter = new TchapContactAdapter(getActivity(), new TchapContactAdapter.OnSelectItemListener() {
            @Override
            public void onSelectItem(Room room, int position) {
                openRoom(room);
            }

            @Override
            public void onSelectItem(ParticipantAdapterItem contact, int position) {
                DinsicUtils.startDirectChat((VectorAppCompatActivity) getActivity(), mSession, contact);
            }
        }, this, this);
        mRecycler.setAdapter(mAdapter);
    }

    /*
     * *********************************************************************************************
     * Data management
     * *********************************************************************************************
     */

    /**
     * Fill the local address book and known contacts adapters with data
     * Display the Tchap users only
     */
    private void initContactsData() {
        ContactsManager.getInstance().retrievePids();

        if (!ContactsManager.getInstance().didPopulateLocalContacts()) {
            Log.d(LOG_TAG, "## initContactsData() : The local contacts are not yet populated");
            mLocalContacts.clear();
        } else if (mContactsSnapshotSession == -1 || mContactsSnapshotSession != ContactsManager.getInstance().getLocalContactsSnapshotSession()) {
            // First time on the screen or contact data outdated
            // Retrieve only the Tchap user contacts by getOnlyTchapUserContacts() method
            mLocalContacts.clear();
            mLocalContacts.addAll(getOnlyTchapUserContacts());
        } else {
            //clear contacts that come from directchats, i.e without contact id
            List<ParticipantAdapterItem> tobeRemoved = new ArrayList<>();
            for (ParticipantAdapterItem item : mLocalContacts) {
                if (item.mContact.getContactId() == "null") {
                    tobeRemoved.add(item);
                }
            }
            for (ParticipantAdapterItem item : tobeRemoved){
                mLocalContacts.remove(item);
            }
        }

        // Add the Tchap users extracted from the current discussions (direct chats).
        List<ParticipantAdapterItem> myDirectContacts = DinsicUtils.getContactsFromDirectChats(mSession);
        for (ParticipantAdapterItem myContact : myDirectContacts) {
            // Remove the item built from the local contact if any.
            // The item built from the direct chat data has the right avatar.
            DinsicUtils.removeParticipantIfExist(mLocalContacts, myContact);
            mLocalContacts.add(myContact);
        }
    }

    /**
     * Get the known contacts list, sort it by presence and give it to adapter
     */
    private void initKnownContacts() {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // do not sort anymore the full known participants list
                // as they are not displayed unfiltered
                // it saves a lot of times
                // eg with about 17000 items
                // sort requires about 2 seconds
                // sort a 1000 items subset during a search requires about 75ms
                mKnownContacts.clear();
                mKnownContacts.addAll(new ArrayList<>(VectorUtils.listKnownParticipants(mSession).values()));
                return null;
            }

            @Override
            protected void onPostExecute(Void args) {
                mAdapter.setKnownContacts(mKnownContacts);
            }
        };

        try {
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (final Exception e) {
            Log.e(LOG_TAG, "## initKnownContacts() failed " + e.getMessage());
            asyncTask.cancel(true);

            (new android.os.Handler(Looper.getMainLooper())).postDelayed(new Runnable() {
                @Override
                public void run() {
                    initKnownContacts();
                }
            }, 1000);
        }
    }

    /**
     * Display the public rooms loading view
     */
    private void showKnownContactLoadingView() {
        mAdapter.getSectionViewForSectionIndex(mAdapter.getSectionsCount() - 1).showLoadingView();
    }

    /**
     * Hide the public rooms loading view
     */
    private void hideKnownContactLoadingView() {
        mAdapter.getSectionViewForSectionIndex(mAdapter.getSectionsCount() - 1).hideLoadingView();
    }

    /**
     * Trigger a request to search known contacts.
     *
     * @param isNewSearch true if the search is a new one
     */
    private void startRemoteKnownContactsSearch(boolean isNewSearch) {
        if (!TextUtils.isEmpty(mCurrentFilter)) {

            // display the known contacts section
            if (isNewSearch) {
                mAdapter.setFilteredKnownContacts(new ArrayList<ParticipantAdapterItem>(), new ArrayList<ParticipantAdapterItem>(), mCurrentFilter);
                showKnownContactLoadingView();
            }

            final String fPattern = mCurrentFilter;

            // Search in the user directories by hiding the current user
            mSession.searchUsers(mCurrentFilter, MAX_KNOWN_CONTACTS_FILTER_COUNT, new HashSet<String>(Arrays.asList(mSession.getMyUserId())), new ApiCallback<SearchUsersResponse>() {
                @Override
                public void onSuccess(SearchUsersResponse searchUsersResponse) {
                    if (TextUtils.equals(fPattern, mCurrentFilter)) {
                        hideKnownContactLoadingView();

                        List<ParticipantAdapterItem> domainList = new ArrayList<>();
                        List<ParticipantAdapterItem> otherList = new ArrayList<>();

                        if (null != searchUsersResponse.results) {
                            String userDomain = DinsicUtils.getDomainFromDisplayName(mSession.getMyUser().displayname);
                            for (User user : searchUsersResponse.results) {
                                ParticipantAdapterItem item = new ParticipantAdapterItem(user);
                                if (TextUtils.equals(userDomain, DinsicUtils.getDomainFromDisplayName(user.displayname))) {
                                    domainList.add(item);
                                } else {
                                    otherList.add(item);
                                }
                            }
                        }

                        mAdapter.setKnownContactsExtraTitle(null);
                        mAdapter.setKnownContactsLimited((null != searchUsersResponse.limited) ? searchUsersResponse.limited : false);
                        mAdapter.setFilteredKnownContacts(domainList, otherList, mCurrentFilter);
                    }
                }

                private void onError(String errorMessage) {
                    Log.e(LOG_TAG, "## startRemoteKnownContactsSearch() : failed " + errorMessage);
                    //
                    if (TextUtils.equals(fPattern, mCurrentFilter)) {
                        hideKnownContactLoadingView();
                        mAdapter.setKnownContactsExtraTitle(TchapContactFragment.this.getContext().getString(R.string.offline));
                        mAdapter.filterAccountKnownContacts(mCurrentFilter);
                    }
                }

                @Override
                public void onNetworkError(Exception e) {
                    onError(e.getMessage());
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    onError(e.getMessage());
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    onError(e.getMessage());
                }
            });
        }
    }

    /*
     * *********************************************************************************************
     * Utils
     * *********************************************************************************************
     */

    /**
     * Retrieve the contacts using Tchap app
     *
     * @return
     */
    private List<ParticipantAdapterItem> getOnlyTchapUserContacts() {
        List<ParticipantAdapterItem> participants = new ArrayList<>();

        Collection<Contact> contacts = ContactsManager.getInstance().getLocalContactsSnapshot();
        mContactsSnapshotSession = ContactsManager.getInstance().getLocalContactsSnapshotSession();

        if (null != contacts) {
            for (Contact contact : contacts) {
                    // In case of several emails, we create a contact for each email linked to a Tchap account.
                    for (String email : contact.getEmails()) {
                        if (!TextUtils.isEmpty(email) && !ParticipantAdapterItem.isBlackedListed(email)) {
                            // Check whether a Tchap account is linked to this email.
                            Contact.MXID mxid = PIDsRetriever.getInstance().getMXID(email);

                            if (null != mxid) {
                                // Ignore the current user if he belongs to the local phone book.
                                if (mxid.mMatrixId.equals(mSession.getMyUserId())) {
                                    continue;
                                }

                                // Create a contact for this Tchap user
                                // TODO check whether there is an issue to use the same id for several dummy contacts
                                Contact dummyContact = new Contact(contact.getContactId());
                                // Use the email as display name for external users
                                if (DinsicUtils.isExternalTchapUser(mxid.mMatrixId)) {
                                    dummyContact.setDisplayName(email);
                                } else {
                                    // Use the local contact display name
                                    dummyContact.setDisplayName(contact.getDisplayName());
                                }
                                dummyContact.addEmailAdress(email);
                                dummyContact.setThumbnailUri(contact.getThumbnailUri());
                                ParticipantAdapterItem participant = new ParticipantAdapterItem(dummyContact);
                                participant.mUserId = mxid.mMatrixId;
                                participants.add(participant);
                            }
                        }
                    }
                }
            }

        return participants;
    }

    /*private List<ParticipantAdapterItem> getMatrixUsers() {
        List<ParticipantAdapterItem> matrixUsers = new ArrayList<>();
        for (ParticipantAdapterItem item : mLocalContacts) {
            if (!item.mContact.getMatrixIdMediums().isEmpty()) {
                matrixUsers.add(item);
            }
        }
        return matrixUsers;
    }*/

    /**
     * Init contacts views with data and update their display
     */
    private void initContactsViews() {
        mAdapter.setLocalContacts(mLocalContacts);
    }

    /*
     * *********************************************************************************************
     * Listeners
     * *********************************************************************************************
     */

    @Override
    public void onRoomResultUpdated(final HomeRoomsViewModel.Result result) {
        if (isResumed()) {
            initContactsData();
        }
    }

    @Override
    public void onRefresh() {
        initContactsData();
        initContactsViews();
    }

    @Override
    public void onPIDsUpdate() {
        // Retrieve only Tchap user contacts
        final List<ParticipantAdapterItem> newContactList = getOnlyTchapUserContacts();

        // Add the Tchap users extracted from the current discussions (direct chats).
        List<ParticipantAdapterItem> myDirectContacts = DinsicUtils.getContactsFromDirectChats(mSession);
        for (ParticipantAdapterItem myContact : myDirectContacts){
            // Remove the item built from the local contact if any.
            // The item built from the direct chat data has the right avatar.
            DinsicUtils.removeParticipantIfExist(newContactList, myContact);
            newContactList.add(myContact);
        }

        if (!mLocalContacts.containsAll(newContactList)) {
            mLocalContacts.clear();
            mLocalContacts.addAll(newContactList);
            initContactsViews();
        }
    }

    @Override
    public void onContactPresenceUpdate(Contact contact, String matrixId) {
        //TODO
    }

    @Override
    public void onIdentityServerTermsNotSigned(String token) {
        if (isAdded()) {
            startActivityForResult(ReviewTermsActivity.Companion.intent(getActivity(),
                    TermsManager.ServiceType.IdentityService, mSession.getIdentityServerManager().getIdentityServerUrl() /* Cannot be null */, token),
                    RequestCodesKt.TERMS_REQUEST_CODE);
        }
    }

    @Override
    public void onNoIdentityServerDefined() {

    }

    @Override
    public void onToggleDirectChat(String roomId, boolean isDirectChat) {
        if (!isDirectChat) {
            mAdapter.removeDirectChat(roomId);
        }
    }

    @Override
    public void onRoomLeft(String roomId) {
        mAdapter.removeDirectChat(roomId);
    }

    @Override
    public void onRoomForgot(String roomId) {
        mAdapter.removeDirectChat(roomId);
    }
}
