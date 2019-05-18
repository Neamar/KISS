package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class NotificationPreference extends DialogPreference {
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    public NotificationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            getContext().startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }
}
