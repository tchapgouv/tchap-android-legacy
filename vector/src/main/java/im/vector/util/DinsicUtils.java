package im.vector.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;

import org.matrix.androidsdk.util.Log;

import java.util.List;

import im.vector.R;
import im.vector.activity.LoginActivity;
import im.vector.contacts.Contact;

/**
 * Created by cloud on 1/22/18.
 */

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


}
