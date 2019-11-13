package fr.gouv.tchap.activity;


import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.matrix.androidsdk.core.Log;

import fr.gouv.tchap.version.TchapVersionCheckActivity;
import im.vector.R;
import im.vector.util.PreferencesManager;

public class AccessibilityServiceDetectionActivity extends AppCompatActivity {

    public static boolean isAccessibilityServiceActive(ContentResolver contentResolver) {
        int accessibilityEnabled = 0;

        try {
            accessibilityEnabled = Settings.Secure.getInt(contentResolver, android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (android.provider.Settings.SettingNotFoundException e) {
            Log.d("AccessibilityServiceDetectionActivity", "*** Security : accessibility settings not found ****");
        }

        return (accessibilityEnabled == 0) ? false : true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!PreferencesManager.detectAccessibilityService(this)) {
            finish(true);
            return;
        }


        if (!isAccessibilityServiceActive(getContentResolver())) {
            finish(true);
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage(R.string.accessibility_security_dialog_message)
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
                        PreferencesManager.putDetectAccessibilityService(AccessibilityServiceDetectionActivity.this, false);
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
    }

    private void finish(boolean startNext) {
        finish();
        if(startNext) {
            Intent start = new Intent(this, TchapVersionCheckActivity.class);
            startActivity(start);
        }
    }
}
