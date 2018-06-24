/*
 * Copyright 2015 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.adapters;

import android.content.Context;
import android.media.ExifInterface;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.adapters.MessageRow;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.db.MXMediasCache;
import org.matrix.androidsdk.rest.model.crypto.EncryptedFileInfo;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.message.FileMessage;
import org.matrix.androidsdk.rest.model.message.ImageMessage;
import org.matrix.androidsdk.rest.model.message.Message;
import org.matrix.androidsdk.rest.model.message.VideoMessage;
import org.matrix.androidsdk.util.JsonUtils;

import fr.gouv.tchap.media.AntiVirusScanStatus;
import fr.gouv.tchap.model.MediaScan;
import im.vector.R;
import im.vector.util.VectorUtils;

/**
 * An adapter which display a files search result
 */
public class VectorSearchFilesListAdapter extends VectorMessagesAdapter {

    // display the room name in the result view
    private final boolean mDisplayRoomName;

    public VectorSearchFilesListAdapter(MXSession session, Context context, boolean displayRoomName, MXMediasCache mediasCache) {
        super(session, context, mediasCache);

        mDisplayRoomName = displayRoomName;
        setNotifyOnChange(true);
    }


    protected boolean mergeView(Event event, int position, boolean shouldBeMerged) {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.adapter_item_vector_search_file_by_name, parent, false);
        }

        if (!mSession.isAlive()) {
            return convertView;
        }

        MessageRow row = getItem(position);
        Event event = row.getEvent();

        Message message = JsonUtils.toMessage(event.getContent());

        // common info
        String url = null;
        String thumbUrl = null;
        Long mediaSize = null;
        int avatarId = R.drawable.filetype_attachment;
        EncryptedFileInfo encryptedFileInfo = null;
        EncryptedFileInfo encryptedFileThumbnailInfo = null;

        if (Message.MSGTYPE_IMAGE.equals(message.msgtype)) {
            ImageMessage imageMessage = JsonUtils.toImageMessage(event.getContent());
            url = imageMessage.getUrl();
            thumbUrl = imageMessage.getThumbnailUrl();
            if (null == thumbUrl) {
                thumbUrl = url;
            }

            if (null != imageMessage.info) {
                mediaSize = imageMessage.info.size;
            }

            if ("image/gif".equals(imageMessage.getMimeType())) {
                avatarId = R.drawable.filetype_gif;
            } else {
                avatarId = R.drawable.filetype_image;
            }

            encryptedFileInfo = imageMessage.file;
            if (null != imageMessage.info) {
                encryptedFileThumbnailInfo = imageMessage.info.thumbnail_file;
            }
        } else if (Message.MSGTYPE_VIDEO.equals(message.msgtype)) {
            VideoMessage videoMessage = JsonUtils.toVideoMessage(event.getContent());
            url = videoMessage.getUrl();
            thumbUrl = videoMessage.getThumbnailUrl();

            if (null != videoMessage.info) {
                mediaSize = videoMessage.info.size;
            }

            avatarId = R.drawable.filetype_video;

            encryptedFileInfo = videoMessage.file;
            if (null != videoMessage.info) {
                encryptedFileThumbnailInfo = videoMessage.info.thumbnail_file;
            }

        } else if (Message.MSGTYPE_FILE.equals(message.msgtype) || Message.MSGTYPE_AUDIO.equals(message.msgtype)) {
            FileMessage fileMessage = JsonUtils.toFileMessage(event.getContent());
            url = fileMessage.getUrl();
            encryptedFileInfo = fileMessage.file;
            if (null != fileMessage.info) {
                mediaSize = fileMessage.info.size;
            }

            avatarId = Message.MSGTYPE_AUDIO.equals(message.msgtype) ? R.drawable.filetype_audio : R.drawable.filetype_attachment;
        }

        // thumbnail
        ImageView thumbnailView = convertView.findViewById(R.id.file_search_thumbnail);
        thumbnailView.setImageResource(R.drawable.ic_notification_privacy_warning); // TODO set the right icon if any

        // Check whether the media is trusted
        if (null != url) {
            boolean isTrusted = false;
            MediaScan mediaScan;
            AntiVirusScanStatus antiVirusScanStatus = AntiVirusScanStatus.UNKNOWN;
            int scanDrawable = R.drawable.ic_notification_privacy_warning;

            if (null != mMediaScanManager) {
                if (null != encryptedFileInfo) {
                    mediaScan = mMediaScanManager.scanEncryptedMedia(encryptedFileInfo);
                } else {
                    mediaScan = mMediaScanManager.scanUnencryptedMedia(url);
                }
                antiVirusScanStatus = mediaScan.getAntiVirusScanStatus();
            }

            switch (antiVirusScanStatus) {
                case IN_PROGRESS:
                    scanDrawable = R.drawable.tchap_scanning;
                    break;
                case TRUSTED:
                    // Check the thumbnail url (if any)
                    if (null != thumbUrl) {
                        if (null != encryptedFileThumbnailInfo) {
                            mediaScan = mMediaScanManager.scanEncryptedMedia(encryptedFileThumbnailInfo);
                        } else {
                            mediaScan = mMediaScanManager.scanUnencryptedMedia(thumbUrl);
                        }

                        antiVirusScanStatus = mediaScan.getAntiVirusScanStatus();

                        switch (antiVirusScanStatus) {
                            case IN_PROGRESS:
                                scanDrawable = R.drawable.tchap_scanning;
                                break;
                            case TRUSTED:
                                isTrusted = true;
                                break;
                            case INFECTED:
                                scanDrawable = R.drawable.tchap_danger;
                                break;
                        }
                    } else {
                        isTrusted = true;
                    }
                    break;
                case INFECTED:
                    scanDrawable = R.drawable.tchap_danger;
                    break;
            }

            if (isTrusted) {
                // Set the default media avatar
                thumbnailView.setImageResource(avatarId);

                if (null != thumbUrl) {
                    // detect if the media is encrypted
                    if (null == encryptedFileThumbnailInfo) {
                        int size = getContext().getResources().getDimensionPixelSize(R.dimen.member_list_avatar_size);
                        mSession.getMediasCache().loadAvatarThumbnail(mSession.getHomeServerConfig(), thumbnailView, thumbUrl, size);
                    } else {
                        mSession.getMediasCache().loadBitmap(mSession.getHomeServerConfig(), thumbnailView, thumbUrl, 0, ExifInterface.ORIENTATION_UNDEFINED, null, encryptedFileThumbnailInfo);
                    }
                }
            } else {
                // If the media scan result is not available or if the media is infected,
                // Don't display the thumbnail and display a placeholder icon according to the scan status
                thumbnailView.setImageResource(scanDrawable);
            }
        }

        // filename
        TextView filenameTextView = convertView.findViewById(R.id.file_search_filename);
        filenameTextView.setText(message.body);

        // room and date&time
        TextView roomNameTextView = convertView.findViewById(R.id.file_search_room_name);
        String info = "";
        if (mDisplayRoomName) {
            Room room = mSession.getDataHandler().getStore().getRoom(event.roomId);

            if (null != room) {
                info += VectorUtils.getRoomDisplayName(mContext, mSession, room);
                info += " - ";
            }
        }

        info += AdapterUtils.tsToString(mContext, event.getOriginServerTs(), false);
        roomNameTextView.setText(info);

        // file size
        TextView fileSizeTextView = convertView.findViewById(R.id.search_file_size);

        if ((null != mediaSize) && (mediaSize > 1)) {
            fileSizeTextView.setText(Formatter.formatFileSize(mContext, mediaSize));
        } else {
            fileSizeTextView.setText("");
        }

        return convertView;
    }
}
