package fr.neamar.kiss.settings;

import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import fr.neamar.kiss.R;
import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.utils.Permission;

public class HistorySettingsFragment extends BaseSettingsFragment {
    private static final int REQUEST_CALL_SCREENING_APP = 1;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_history, rootKey);

        int historyLength = getDataHandler().getHistoryLength();
        if (historyLength > 5) {
            Preference resetHistory = findPreference("reset-history");
            if (resetHistory != null) {
                resetHistory.setSummary(getString(R.string.items_title, historyLength));
            }
        }

        setOnDialogClosedListener("reset-history", (pref, positiveResult) -> {
            if (positiveResult) {
                getDataHandler().clearHistory();
                pref.setSummary(requireContext().getString(R.string.history_erased));
                Toast.makeText(getContext(), R.string.history_erased, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if ("enable-phone-history".equals(key)) {
            boolean enabled = sharedPreferences.getBoolean(key, false);
            if (enabled && !Permission.checkPermission(getContext(), Permission.PERMISSION_READ_PHONE_STATE)) {
                Permission.askPermission(Permission.PERMISSION_READ_PHONE_STATE, new Permission.PermissionResultListener() {
                    @Override
                    public void onGranted() {
                        setPhoneHistoryEnabled(true);
                    }

                    @Override
                    public void onDenied() {
                        // You don't want to give us permission, that's fine. Revert the toggle.
                        SwitchPreference p = findPreference(key);
                        if (p != null) {
                            p.setChecked(false);
                        }
                        Toast.makeText(getContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                setPhoneHistoryEnabled(enabled);
            }
        } else if ("enable-notification-history".equals(key)) {
            boolean enabled = sharedPreferences.getBoolean(key, false);
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        }
    }

    private void setPhoneHistoryEnabled(boolean enabled) {
        IncomingCallHandler.setEnabled(getContext(), enabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && enabled) {
            RoleManager roleManager = ContextCompat.getSystemService(requireContext(), RoleManager.class);
            Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
            startActivityForResult(intent, REQUEST_CALL_SCREENING_APP);
        }
    }
}
