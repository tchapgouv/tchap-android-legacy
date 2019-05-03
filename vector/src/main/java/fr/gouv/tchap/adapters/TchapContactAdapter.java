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

package fr.gouv.tchap.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.matrix.androidsdk.MXPatterns;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.User;
import org.matrix.androidsdk.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.gouv.tchap.util.DinsicUtils;
import im.vector.R;
import im.vector.adapters.AbsAdapter;
import im.vector.adapters.AdapterSection;
import im.vector.adapters.KnownContactsAdapterSection;
import im.vector.adapters.ParticipantAdapterItem;
import im.vector.adapters.RoomViewHolder;
import im.vector.contacts.ContactsManager;
import im.vector.settings.VectorLocale;
import im.vector.util.RoomUtils;
import im.vector.util.VectorUtils;

public class TchapContactAdapter extends AbsAdapter {

    private static final String LOG_TAG = TchapContactAdapter.class.getSimpleName();

    private static final int TYPE_HEADER_LOCAL_CONTACTS = 0;

    private static final int TYPE_CONTACT = 1;

    private final AdapterSection<Room> mDirectChatsSection;
    private final AdapterSection<ParticipantAdapterItem> mLocalContactsSection;
    private final KnownContactsAdapterSection mKnownContactsSection;

    private final OnSelectItemListener mListener;

    private final String mNoContactAccessPlaceholder;
    private final String mNoResultPlaceholder;

    /*
     * *********************************************************************************************
     * Constructor
     * *********************************************************************************************
     */

    public TchapContactAdapter(final Context context, final OnSelectItemListener listener, final RoomInvitationListener invitationListener, final MoreRoomActionListener moreActionListener) {
        super(context, invitationListener, moreActionListener);
        mListener = listener;

        // ButterKnife.bind(this); cannot be applied here
        mNoContactAccessPlaceholder = context.getString(R.string.no_contact_access_placeholder);
        mNoResultPlaceholder = context.getString(R.string.no_result_placeholder);

        mDirectChatsSection = new AdapterSection<>(context, context.getString(R.string.direct_chats_header), -1,
                R.layout.adapter_item_room_view, TYPE_HEADER_DEFAULT, TYPE_ROOM, new ArrayList<Room>(), RoomUtils.getRoomsDateComparator(mSession, false));
        mDirectChatsSection.setEmptyViewPlaceholder(context.getString(R.string.no_conversation_placeholder), context.getString(R.string.no_result_placeholder));

        // use the gouv comparator to show in priority matrix and agent users
        mLocalContactsSection = new AdapterSection<>(
                context,
                context.getString(R.string.local_address_book_header),
                R.layout.adapter_local_contacts_sticky_header_subview,
                R.layout.adapter_item_contact_view,
                TYPE_HEADER_LOCAL_CONTACTS,
                TYPE_CONTACT,
                new ArrayList<ParticipantAdapterItem>(),
                ParticipantAdapterItem.tchapAlphaComparator);
        mLocalContactsSection.setEmptyViewPlaceholder(!ContactsManager.getInstance().isContactBookAccessAllowed() ? mNoContactAccessPlaceholder : mNoResultPlaceholder);

        mKnownContactsSection = new KnownContactsAdapterSection(
                context,
                context.getString(R.string.user_directory_header),
                -1,
                R.layout.adapter_item_contact_view,
                TYPE_HEADER_DEFAULT,
                TYPE_CONTACT,
                new ArrayList<ParticipantAdapterItem>(),
                null);
        mKnownContactsSection.setEmptyViewPlaceholder(null, context.getString(R.string.no_result_placeholder));
        mKnownContactsSection.setIsHiddenWhenNoFilter(true);

        addSection(mLocalContactsSection);
        addSection(mKnownContactsSection);
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected RecyclerView.ViewHolder createSubViewHolder(ViewGroup viewGroup, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

        View itemView;

        if (viewType == TYPE_HEADER_LOCAL_CONTACTS) {
            //TODO replace by a empty view ?
            itemView = inflater.inflate(R.layout.adapter_section_header_local, viewGroup, false);
            itemView.setBackgroundColor(Color.MAGENTA);
            return new HeaderViewHolder(itemView);
        } else {
            switch (viewType) {
                case TYPE_ROOM:
                    itemView = inflater.inflate(R.layout.adapter_item_room_view, viewGroup, false);
                    return new RoomViewHolder(itemView);
                case TYPE_CONTACT:
                    itemView = inflater.inflate(R.layout.adapter_item_contact_view, viewGroup, false);
                    return new ContactViewHolder(itemView);
            }
        }
        return null;
    }

    @Override
    protected void populateViewHolder(int viewType, RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewType) {
            case TYPE_HEADER_LOCAL_CONTACTS:
                // Local header
                final HeaderViewHolder headerViewHolder = (HeaderViewHolder) viewHolder;
                for (Pair<Integer, AdapterSection> adapterSection : getSectionsArray()) {
                    if (adapterSection.first == position) {
                        headerViewHolder.populateViews(adapterSection.second);
                        break;
                    }
                }
                break;
            case TYPE_ROOM:
                final RoomViewHolder roomViewHolder = (RoomViewHolder) viewHolder;
                final Room room = (Room) getItemForPosition(position);
                roomViewHolder.populateViews(mContext, mSession, room, true, false, mMoreRoomActionListener);
                roomViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onSelectItem(room, -1);
                    }
                });
                break;
            case TYPE_CONTACT:
                final ContactViewHolder contactViewHolder = (ContactViewHolder) viewHolder;
                final ParticipantAdapterItem item = (ParticipantAdapterItem) getItemForPosition(position);
                contactViewHolder.populateViews(item, position);
                break;
        }
    }

    @Override
    protected int applyFilter(String pattern) {
        int nbResults = 0;
        nbResults += filterRoomSection(mDirectChatsSection, pattern);
        nbResults += filterLocalContacts(pattern);

        // if there is no pattern, use the local search
        if (TextUtils.isEmpty(pattern)) {
            nbResults += filterKnownContacts(pattern);
        }
        return nbResults;
    }

    /*
     * *********************************************************************************************
     * Public methods
     * *********************************************************************************************
     */

    public void setRooms(final List<Room> rooms) {
        mDirectChatsSection.setItems(rooms, mCurrentFilterPattern);
        if (!TextUtils.isEmpty(mCurrentFilterPattern)) {
            filterRoomSection(mDirectChatsSection, String.valueOf(mCurrentFilterPattern));
        }
        updateSections();
    }

    public void setLocalContacts(final List<ParticipantAdapterItem> localContacts) {
        // updates the placeholder according to the local contacts permissions
        mLocalContactsSection.setEmptyViewPlaceholder(!ContactsManager.getInstance().isContactBookAccessAllowed() ? mNoContactAccessPlaceholder : mNoResultPlaceholder);

        // If the current user is external, remove the external users from the provided list
        if (DinsicUtils.isExternalTchapSession(mSession)) {
            final List<ParticipantAdapterItem> cleanedLocalContacts = removeExternalTchapUsers(localContacts);
            mLocalContactsSection.setItems(cleanedLocalContacts, mCurrentFilterPattern);
        } else {
            mLocalContactsSection.setItems(localContacts, mCurrentFilterPattern);
        }

        if (!TextUtils.isEmpty(mCurrentFilterPattern)) {
            filterLocalContacts(String.valueOf(mCurrentFilterPattern));
        }
        updateSections();
    }

    public void setKnownContacts(final List<ParticipantAdapterItem> knownContacts) {
        // If the current user is external, remove the external users from the provided list
        if (DinsicUtils.isExternalTchapSession(mSession)) {
            final List<ParticipantAdapterItem> cleanedKnownContacts = removeExternalTchapUsers(knownContacts);
            mKnownContactsSection.setItems(cleanedKnownContacts, mCurrentFilterPattern);
        } else {
            mKnownContactsSection.setItems(knownContacts, mCurrentFilterPattern);
        }

        if (!TextUtils.isEmpty(mCurrentFilterPattern)) {
            filterKnownContacts(String.valueOf(mCurrentFilterPattern));
        } else {
            filterKnownContacts(null);
        }
        updateSections();
    }

    public void setFilteredKnownContacts(List<ParticipantAdapterItem> filteredKnownContacts, String pattern) {
        Collections.sort(filteredKnownContacts, ParticipantAdapterItem.getComparator(mSession));
        mKnownContactsSection.setFilteredItems(filteredKnownContacts, pattern);
        updateSections();
    }

    public void setKnownContactsLimited(boolean isLimited) {
        mKnownContactsSection.setIsLimited(isLimited);
    }

    public void setKnownContactsExtraTitle(String extraTitle) {
        mKnownContactsSection.setCustomHeaderExtra(extraTitle);
    }

    /**
     * Update the known contact corresponding to the given user id
     *
     * @param user
     */
    public void updateKnownContact(final User user) {
        int headerPos = getSectionHeaderPosition(mKnownContactsSection) + 1;
        List<ParticipantAdapterItem> knownContacts = mKnownContactsSection.getFilteredItems();
        for (int i = 0; i < knownContacts.size(); i++) {
            ParticipantAdapterItem item = knownContacts.get(i);
            if (TextUtils.equals(user.user_id, item.mUserId)) {
                notifyItemChanged(headerPos + i);
            }
        }
    }

    /*
     * *********************************************************************************************
     * Private methods
     * *********************************************************************************************
     */

    /**
     * Remove the potential external tchap users from the provided list of contacts.
     * Each item of the contacts list is supposed here to have a non-null "mUserId".
     *
     * @param contacts
     * @return the cleaned list
     */
    private List<ParticipantAdapterItem> removeExternalTchapUsers(final List<ParticipantAdapterItem> contacts) {
        List<ParticipantAdapterItem> result = new ArrayList<>();
        for (ParticipantAdapterItem item : contacts) {
            if (!DinsicUtils.isExternalTchapUser(item.mUserId)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Filter the local contacts with the given pattern
     *
     * @param pattern
     * @return nb of items matching the filter
     */
    private int filterLocalContacts(final String pattern) {
        if (!TextUtils.isEmpty(pattern)) {
            List<ParticipantAdapterItem> filteredLocalContacts = new ArrayList<>();
            final String formattedPattern = pattern.toLowerCase(VectorLocale.INSTANCE.getApplicationLocale()).trim();

            List<ParticipantAdapterItem> sectionItems = new ArrayList<>(mLocalContactsSection.getItems());
            for (final ParticipantAdapterItem item : sectionItems) {
                if (item.startsWith(formattedPattern)) {
                    filteredLocalContacts.add(item);
                }
            }
            mLocalContactsSection.setFilteredItems(filteredLocalContacts, pattern);
        } else {
            mLocalContactsSection.resetFilter();
        }

        return mLocalContactsSection.getFilteredItems().size();
    }

    /**
     * Filter the known contacts known by this account.
     *
     * @param pattern the pattern to search
     */
    public void filterAccountKnownContacts(final String pattern) {
        filterKnownContacts(pattern);
        updateSections();
    }

    /**
     * Filter the known contacts with the given pattern
     *
     * @param pattern
     * @return nb of items matching the filter
     */
    private int filterKnownContacts(final String pattern) {
        List<ParticipantAdapterItem> filteredKnownContacts = new ArrayList<>();
        if (!TextUtils.isEmpty(pattern)) {
            final String formattedPattern = pattern.trim().toLowerCase(VectorLocale.INSTANCE.getApplicationLocale());
            List<ParticipantAdapterItem> sectionItems = new ArrayList<>(mKnownContactsSection.getItems());
            for (final ParticipantAdapterItem item : sectionItems) {
                if (item.startsWith(formattedPattern)) {
                    filteredKnownContacts.add(item);
                }
            }

        }

        // The sort is done in the adapter to save loading time
        // see ContactFragment.initKnownContacts
        Collections.sort(filteredKnownContacts, ParticipantAdapterItem.getComparator(mSession));
        mKnownContactsSection.setFilteredItems(filteredKnownContacts, pattern);

        setKnownContactsLimited(false);
        setKnownContactsExtraTitle(null);

        return filteredKnownContacts.size();
    }

    /**
     * Remove the room of the given id from the adapter
     *
     * @param roomId
     */
    public void removeDirectChat(final String roomId) {
        Room room = mSession.getDataHandler().getRoom(roomId);
        if (mDirectChatsSection.removeItem(room)) {
            updateSections();
        }
    }

    /*
     * *********************************************************************************************
     * View holder
     * *********************************************************************************************
     */

    class ContactViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.contact_avatar)
        ImageView vContactAvatar;

        @BindView(R.id.contact_status)
        ImageView vContactStatus;

        @BindView(R.id.contact_name)
        TextView vContactName;

        @BindView(R.id.contact_domain)
        TextView vContactDomain;

        private ContactViewHolder(final View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        private void populateViews(final ParticipantAdapterItem participant, final int position) {
            if (null == participant) {
                Log.e(LOG_TAG, "## populateViews() : null participant");
                return;
            }

            if (position >= getItemCount()) {
                Log.e(LOG_TAG, "## populateViews() : position out of bound " + position + " / " + getItemCount());
                return;
            }

            // Tchap users are displayed differently than no Tchap users
            if (participant.isMatrixUser()) {
                vContactAvatar.clearColorFilter();

                // display the participant's domain
                // TODO External users : domain visibility should be GONE
                String domainName = DinsicUtils.getDomainFromDisplayName(participant.mDisplayName);

                if (null == domainName || domainName.isEmpty()) {
                    // sanity check
                    if (null != participant.mContact && !participant.mContact.getEmails().isEmpty()) {
                        // We extract the domain of this tchap user from the his email
                        String emailAddress = participant.mContact.getEmails().get(0);
                        String[] components2 = emailAddress.split("@");

                        if (components2.length > 1) {
                            String domain = components2[1].substring(0,components2[1].indexOf("."));
                            String formattedDomain;
                            if (domain.length() > 1) {
                                formattedDomain = domain.substring(0, 1).toUpperCase() + domain.substring(1);
                            } else {
                                formattedDomain = domain.substring(0, 1).toUpperCase();
                            }
                            domainName = formattedDomain;
                        }
                    }
                }
                vContactDomain.setVisibility(View.VISIBLE);
                vContactDomain.setText(domainName);

                vContactName.setTypeface(null, Typeface.BOLD);
                vContactName.setText(DinsicUtils.getNameFromDisplayName(participant.mDisplayName));
            } else {
                final int semiTransparentGrey = Color.argb(155, 185, 185, 185);
                vContactAvatar.setColorFilter(semiTransparentGrey);
                vContactDomain.setVisibility(View.GONE);
                vContactName.setTypeface(null, Typeface.ITALIC);
                vContactName.setText(participant.mDisplayName);
            }

            // display the participant's avatar
            participant.displayAvatar(mSession, vContactAvatar);

            // Check whether tchap user are online
            if (MXPatterns.PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER.matcher(participant.mUserId).matches()) {
                loadContactPresence(vContactStatus, participant, position);
            } else {
                vContactStatus.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onSelectItem(participant, -1);
                }
            });
        }

        /**
         * Get the presence for the given contact
         *
         * @param imageView
         * @param item
         * @param position
         */
        private void loadContactPresence(final ImageView imageView,
                                         final ParticipantAdapterItem item,
                                         final int position) {
            final boolean presence = VectorUtils.isUserOnline(mContext, mSession, item.mUserId, new SimpleApiCallback<Void>() {
                @Override
                public void onSuccess(Void info) {
                    if (imageView != null) {
                        imageView.setVisibility(VectorUtils.isUserOnline(mContext, mSession, item.mUserId, null) ? View.VISIBLE : View.GONE);
                        notifyItemChanged(position);
                    }
                }
            });
            imageView.setVisibility(presence ? View.VISIBLE : View.GONE);
        }
    }

    /*
     * *********************************************************************************************
     * Inner classes
     * *********************************************************************************************
     */

    public interface OnSelectItemListener {
        void onSelectItem(Room item, int position);

        void onSelectItem(ParticipantAdapterItem item, int position);
    }
}
