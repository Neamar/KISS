package fr.neamar.kiss.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;

import fr.neamar.kiss.R;
import fr.neamar.kiss.preference.DefaultLauncherPreference;

public class AdvancedSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_advanced, rootKey);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            removePreference("enable-notifications");
        }

        setOnDialogClosedListener("enable-notifications", (pref, positiveResult) -> {
            if (positiveResult && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
        setOnDialogClosedListener("default-launcher", (pref, positiveResult) ->
                new DefaultLauncherPreference().onDialogClosed(getContext(), positiveResult));
        setOnDialogClosedListener("restart", (pref, positiveResult) -> {
            if (positiveResult) {
                System.exit(0);
            }
        });
    }
}
