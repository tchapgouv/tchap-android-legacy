package fr.gouv.tchap.activity;


import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import im.vector.R;
import im.vector.util.PreferencesManager;

public class NotificationListenerDetectionActivity extends AppCompatActivity {

    /**
     * Retrieves the display name of an application from its package name
     *
     * @param packagename the package name of the application
     * @param context     the context used to retrieve the package manager
     * @return the display name of the application identified by packagename, or "(unknown)" if not installed
     */
    public static String getAppName(String packagename, Context context) {
        final PackageManager pm = context.getPackageManager();

        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(packagename, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
        return applicationName;
    }

    /**
     * Retrieves the display name of the applications which currently expose an active NotificationListener service
     *
     * @param context
     * @param contentresolver
     * @return
     */
    public static String[] getListeningServiceAppName(Context context, ContentResolver contentresolver) {
        String flat = Settings.Secure.getString(contentresolver, "enabled_notification_listeners");


        if ((flat == null) || (flat.equals(""))) return new String[]{};

        String[] large_descriptions = flat.split(":");
        String[] appname_array = new String[large_descriptions.length];

        for (int i = 0; i < large_descriptions.length; i++) {
            String[] splitted = large_descriptions[i].split("/");
            String packagename = splitted[0];

            appname_array[i] = getAppName(packagename, context);
        }

        return appname_array;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Disable temporarily the notification listener check until verifying the behavior in all use cases.
        finish(true);
        return;

        /*
        StringBuilder string_builder = null;
        String message = "";

        String[] notificationListenerNames = new String[]{};

        if (!PreferencesManager.detectNotificationListener(this)) {
            finish(true);
            return;
        }

        notificationListenerNames = getListeningServiceAppName(getApplicationContext(), getContentResolver());

        if (notificationListenerNames.length == 0) {
            finish(true);
            return;
        }

        string_builder = new StringBuilder();

        message = getResources().getString(R.string.notification_security_dialog_message);


        for (String s : notificationListenerNames) {
            string_builder.append("\n").append("- ").append(s);
        }

        new AlertDialog.Builder(this)
                .setMessage(message + string_builder.toString())
                .setIcon(R.drawable.logo_transparent)
                .setTitle(R.string.security_dialog_title)
                .setPositiveButton(R.string.security_dialog_start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish(true);
                    }
                })
                .setNeutralButton(R.string.security_dialog_start_an_stop_detecting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferencesManager.putDetectNotificationListener(NotificationListenerDetectionActivity.this, false);
                        finish(true);
                    }
                })
                .setNegativeButton(R.string.security_dialog_stop, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish(false);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish(false);
                    }
                })
                .show();
        */
    }

    private void finish(boolean startNext) {
        finish();
        if (startNext) {
            Intent start = new Intent(this, AccessibilityServiceDetectionActivity.class);
            startActivity(start);
        }
    }
}
