package im.vector.util;

import android.app.AlertDialog;
import android.content.DialogInterface;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import org.matrix.androidsdk.MXSession;

import java.util.Iterator;
import java.util.List;

import im.vector.Matrix;
import im.vector.R;
import im.vector.activity.LoginActivity;
import im.vector.adapters.ParticipantAdapterItem;


/**
 * Created by cloud on 1/22/18.
 */

public class DinsicUtils {
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

}
