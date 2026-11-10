package fr.neamar.kiss.settings;

import static fr.neamar.kiss.dataprovider.ProviderName.APPS;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import fr.neamar.kiss.R;
import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.utils.Permission;

public class HistorySettingsFragment extends BaseSettingsFragment {
    // Requested from this fragment so the result is delivered here while the screen is displayed
    private final ActivityResultLauncher<String> requestPhoneStatePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    setPhoneHistoryEnabled(true);
                } else {
                    // You don't want to give us permission, that's fine. Revert the toggle.
                    disablePhoneHistory(R.string.permission_denied);
                }
            });

    // Since Android 10, incoming calls are received as the call screening app
    private final ActivityResultLauncher<Intent> requestCallScreeningRole = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isCallScreeningRoleHeld()) {
                    disablePhoneHistory(R.string.enable_phone_call_screening_denied);
                }
            });

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Preference phoneHistory = findPreference("enable-phone-history");
            if (phoneHistory != null) {
                phoneHistory.setSummary(R.string.enable_phone_call_screening_summary);
            }
        }

        setOnDialogClosedListener("reset-excluded-from-history-apps", (pref, positiveResult) -> {
            if (positiveResult) {
                getPrefs().edit().putStringSet("excluded-apps-from-history", null).apply();
                getDataHandler().reload(APPS); // reload because it's cached in AppPojo#excludedFromHistory
                Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
            }
        });

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
                requestPhoneStatePermission.launch(Manifest.permission.READ_PHONE_STATE);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && enabled && !isCallScreeningRoleHeld()) {
            RoleManager roleManager = ContextCompat.getSystemService(requireContext(), RoleManager.class);
            requestCallScreeningRole.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private boolean isCallScreeningRoleHeld() {
        RoleManager roleManager = ContextCompat.getSystemService(requireContext(), RoleManager.class);
        return roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    /**
     * Revert the phone history toggle, explaining why
     */
    private void disablePhoneHistory(@StringRes int reason) {
        SwitchPreference p = findPreference("enable-phone-history");
        if (p != null) {
            p.setChecked(false);
        }
        Toast.makeText(requireContext(), reason, Toast.LENGTH_LONG).show();
    }
}
