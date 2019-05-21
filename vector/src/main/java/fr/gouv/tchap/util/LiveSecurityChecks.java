package fr.gouv.tchap.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import org.matrix.androidsdk.core.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.gouv.tchap.activity.AccessibilityServiceDetectionActivity;
import fr.gouv.tchap.activity.NotificationListenerDetectionActivity;
import im.vector.R;
import im.vector.util.PreferencesManager;

public class LiveSecurityChecks {

    // security state
    // ---------------
    // the variable are static to be remembered from one activity to the other to avoid
    // multiple notifications when going back from one activity to an other
    // stopped_before_starting is true if we went through onStop,Pause,...
    private static boolean stopped_before_starting;
    // contains the string returned by Settings.Secure.get(... "enabled_notification_listeners")
    private static String[] notificationListenerState_at_stop;
    //contains the state of the accessibility service
    private static boolean accessibilityServiceActive_at_stop;

    private Activity attachedActivity;

    public LiveSecurityChecks(Activity activityAttached) {
        stopped_before_starting = false;
        attachedActivity = activityAttached;
    }

    public void activityStopped() {
        Log.d(LiveSecurityChecks.class.getSimpleName(), "**** activityStopped ****");

        stopped_before_starting = true;
        notificationListenerState_at_stop = NotificationListenerDetectionActivity.getListeningServiceAppName(
                attachedActivity.getApplicationContext(),
                attachedActivity.getContentResolver());
        accessibilityServiceActive_at_stop = AccessibilityServiceDetectionActivity.isAccessibilityServiceActive(
                attachedActivity.getContentResolver());
    }

    public void checkOnActivityStart() {
        if (!stopped_before_starting)
            return;

        stopped_before_starting = false;

        checkAccessibilityChange();
        // Disable temporarily the check on the notification listener change until verifying the behavior in all use cases.
        //checkNotificationListenerChange();
    }

    protected void checkAccessibilityChange() {
        if (!PreferencesManager.detectAccessibilityService(attachedActivity))
            return;

        boolean accessibilityServiceActive_at_start = AccessibilityServiceDetectionActivity.isAccessibilityServiceActive(
                attachedActivity.getContentResolver());

        if ((!accessibilityServiceActive_at_stop) && accessibilityServiceActive_at_start) {
            new AlertDialog.Builder(attachedActivity)
                    .setMessage(R.string.accessibility_change_security_dialog_message)
                    .setIcon(R.drawable.logo_transparent)
                    .setTitle(R.string.security_dialog_title)
                    .setPositiveButton(R.string.security_dialog_continue, null)
                    .setNeutralButton(R.string.security_dialog_start_an_stop_detecting, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PreferencesManager.putDetectAccessibilityService(LiveSecurityChecks.this.attachedActivity, false);
                        }
                    })
                    .setNegativeButton(R.string.security_dialog_stop, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LiveSecurityChecks.this.attachedActivity.finishAffinity();
                        }
                    })
                    .show();
        }
    }

    protected void checkNotificationListenerChange() {
        if (!PreferencesManager.detectNotificationListener(attachedActivity))
            return;

        String[] notificationListenerState_at_start = NotificationListenerDetectionActivity.getListeningServiceAppName(
                attachedActivity.getApplicationContext(),
                attachedActivity.getContentResolver());

        ArrayList<String> newNotificationListeners = new ArrayList<String>();
        List<String> oldNotificationListeners = Arrays.asList(notificationListenerState_at_stop);

        for (String listenerAtStart : notificationListenerState_at_start) {
            if (!oldNotificationListeners.contains(listenerAtStart))
                newNotificationListeners.add(listenerAtStart);
        }

        if (newNotificationListeners.size() == 0)
            return;

        // notifies the user of new notification listener
        StringBuilder notificationMessageBuilder = new StringBuilder();

        for (String s : newNotificationListeners) {
            notificationMessageBuilder.append("\n").append("- ").append(s);
        }

        String message = attachedActivity.getResources().getString(R.string.notification_change_security_dialog_message) +
                notificationMessageBuilder.toString();

        new AlertDialog.Builder(attachedActivity)
                .setMessage(message)
                .setIcon(R.drawable.logo_transparent)
                .setTitle(R.string.security_dialog_title)
                .setPositiveButton(R.string.security_dialog_continue, null)
                .setNeutralButton(R.string.security_dialog_start_an_stop_detecting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferencesManager.putDetectNotificationListener(LiveSecurityChecks.this.attachedActivity, false);
                    }
                })
                .setNegativeButton(R.string.security_dialog_stop, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LiveSecurityChecks.this.attachedActivity.finishAffinity();
                    }
                }).show();
    }
}
