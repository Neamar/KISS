package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;

public class NotificationPreference extends DialogPreference {
    public NotificationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            getContext().startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }
}
