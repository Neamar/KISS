package fr.neamar.kiss;

import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;
import fr.neamar.kiss.dataprovider.simpleprovider.TagsProvider;
import fr.neamar.kiss.forwarder.InterfaceTweaks;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.preference.AddSearchProviderPreference;
import fr.neamar.kiss.preference.AddSearchProviderPreferenceDialogFragment;
import fr.neamar.kiss.preference.ColorPreference;
import fr.neamar.kiss.preference.ColorPreferenceDialogFragment;
import fr.neamar.kiss.preference.DefaultLauncherPreference;
import fr.neamar.kiss.preference.DefaultSearchProviderSelectPreference;
import fr.neamar.kiss.preference.DialogShowingPreference;
import fr.neamar.kiss.preference.DialogShowingPreferenceDialogFragment;
import fr.neamar.kiss.preference.ExportSettingsPreference;
import fr.neamar.kiss.preference.ImportSettingsPreference;
import fr.neamar.kiss.preference.LaunchPojoSelectPreference;
import fr.neamar.kiss.preference.SelectCustomSearchProvidersPreference;
import fr.neamar.kiss.searcher.QuerySearcher;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.Permission;
import fr.neamar.kiss.utils.ShortcutUtil;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {
    private static final String TAG = SettingsFragment.class.getSimpleName();
    private static final int REQUEST_CALL_SCREENING_APP = 1;
    private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";

    private static final List<String> PREF_LISTS_WITH_DEPENDENCY = Arrays.asList(
            "gesture-up", "gesture-down",
            "gesture-left", "gesture-right",
            "gesture-long-press"
    );

    private SharedPreferences prefs;

    private Permission permissionManager;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        setPreferencesFromResource(R.xml.preferences, rootKey);

        if (prefs.getStringSet("selected-search-provider-names", null) == null) {
            // If null, it means this setting has never been accessed before
            // In this case, null != [] ([] happens when the user manually unselected every single option)
            // So, when null, we know it's the first time opening this setting and we can write the default value.
            // note: other preferences are initialized automatically in MainActivity.onCreate() from the preferences XML,
            // but this preference isn't defined in the XML so can't be initialized that easily.
            prefs.edit().putStringSet("selected-search-provider-names", SearchProvider.getSelectedSearchProviders(prefs)).apply();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            removePreference("gestures-holder", "double-tap");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            removePreference("colors-section", "black-notification-icons");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            removePreference("advanced", "enable-notifications");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            removePreference("icons-section", DrawableUtils.KEY_THEMED_ICONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            removePreference("colors-section", "notification-bar-color");
        }
        if (!ShortcutUtil.canDeviceShowShortcuts()) {
            removePreference("exclude_apps_category", "edit-excluded-app-shortcuts");
            removePreference("exclude_apps_category", "reset-excluded-app-shortcuts");
            removePreference("search-providers", "enable-shortcuts");
            removePreference("search-providers", "reset-shortcuts");
        }

        updateItemsToRun();
        fixSummaries();

        permissionManager = new Permission(getActivity());
    }

    private void updateItemsToRun() {
        for (String key : PREF_LISTS_WITH_DEPENDENCY) {
            updateItemToRun(key);
        }
    }

    private void updateItemToRun(String key) {
        LaunchPojoSelectPreference preference = findPreference(key + "-launch-id");
        if (preference != null) {
            String value = prefs.getString(key, null);
            boolean isLaunchEnabled = "launch-pojo".equals(value);
            preference.setEnabled(isLaunchEnabled);
            preference.setVisible(isLaunchEnabled);
        }
    }

    private void removePreference(String parentKey, String key) {
        PreferenceGroup p = findPreference(parentKey);
        if (p != null) {
            Preference c = p.findPreference(key);
            if (c != null) {
                p.removePreference(c);
            } else {
                Log.d(TAG, "Preference to remove not found: " + parentKey + "/" + key);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            KissApplication.getApplication(requireContext()).getIconsHandler().onPrefChanged(sharedPreferences, key);

            if (PREF_LISTS_WITH_DEPENDENCY.contains(key)) {
                updateItemToRun(key);
            }

            if (key.equalsIgnoreCase("available-search-providers")) {
                refreshSelectSearchProvider();
                refreshDefaultSearchProvider();
                getDataHandler().reloadSearchProvider();
            } else if (key.equalsIgnoreCase("selected-search-provider-names")) {
                refreshDefaultSearchProvider();
                getDataHandler().reloadSearchProvider();
            } else if (key.equalsIgnoreCase("enable-phone-history")) {
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
            } else if (key.equalsIgnoreCase("primary-color")) {
                UIColors.clearPrimaryColorCache();
            } else if (key.equalsIgnoreCase("number-of-display-elements")) {
                QuerySearcher.clearMaxResultCountCache();
            } else if (key.equalsIgnoreCase("default-search-provider")) {
                getDataHandler().reloadSearchProvider();
            } else if ("pref-fav-tags-list".equals(key)) {
                getDataHandler().reloadTags();

                // after we edit the fav tags list update DataHandler
                Set<String> favTags = sharedPreferences.getStringSet(key, Collections.emptySet());
                DataHandler dh = getDataHandler();
                List<Pojo> favoritesPojo = dh.getFavorites();
                for (Pojo pojo : favoritesPojo)
                    if (pojo instanceof TagDummyPojo && !favTags.contains(pojo.getName()))
                        dh.removeFromFavorites(pojo.id);
                for (String tagName : favTags)
                    dh.addToFavorites(TagsProvider.generateUniqueId(tagName));
            } else if ("exclude-favorites-apps".equals(key)) {
                getDataHandler().reloadApps();
            } else if ("enable-notification-history".equals(key)) {
                boolean enabled = sharedPreferences.getBoolean(key, false);
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                }
            } else if ("selected-contact-mime-types".equals(key)) {
                getDataHandler().reloadContactsProvider();
            } else if ("theme".equals(key)) {
                InterfaceTweaks.setDefaultNightMode(requireContext());
            }
        }
    }

    private void refreshSelectSearchProvider() {
        SelectCustomSearchProvidersPreference preference = findPreference("selected-search-provider-names");
        if (preference != null) {
            preference.refresh();
        }
    }

    private void refreshDefaultSearchProvider() {
        DefaultSearchProviderSelectPreference preference = findPreference("default-search-provider");
        if (preference != null) {
            preference.refresh();
        }
    }

    protected void setPhoneHistoryEnabled(boolean enabled) {
        IncomingCallHandler.setEnabled(getContext(), enabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && enabled) {
            RoleManager roleManager = ContextCompat.getSystemService(requireContext(), RoleManager.class);
            Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
            startActivityForResult(intent, REQUEST_CALL_SCREENING_APP);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void fixSummaries() {
        int historyLength = getDataHandler().getHistoryLength();
        if (historyLength > 5) {
            Preference resetHistory = findPreference("reset-history");
            if (resetHistory != null) {
                resetHistory.setSummary(String.format(getString(R.string.items_title), historyLength));
            }
        }

        // Only display "rate the app" preference if the user has been using KISS long enough to enjoy it ;)
        Preference rateApp = findPreference("rate-app");
        if (rateApp != null) {
            if (historyLength < 300) {
                getPreferenceScreen().removePreference(rateApp);
            } else {
                rateApp.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + getContext().getApplicationContext().getPackageName()));
                    startActivity(intent);

                    return true;
                });
            }
        }
    }

    /**
     * Override to catch an exception which can crash whole app.
     * This exception can occure when entries are added to/removed from preferences dynamically.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link Preference} with the key, or null.
     * @see PreferenceFragmentCompat#findPreference(CharSequence)
     */
    @Nullable
    @Override
    public <T extends Preference> T findPreference(@NonNull CharSequence key) {
        try {
            return super.findPreference(key);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to find preference for key:" + key);
            return null;
        }
    }

    private DataHandler getDataHandler() {
        return KissApplication.getApplication(requireContext()).getDataHandler();
    }

    @Override
    public boolean onPreferenceDisplayDialog(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        DialogFragment dialogFragment = null;
        if (pref instanceof DialogShowingPreference) {
            dialogFragment = DialogShowingPreferenceDialogFragment.newInstance(pref.getKey(), this::onDialogClosed);
        } else if (pref instanceof ColorPreference) {
            dialogFragment = ColorPreferenceDialogFragment.newInstance(pref.getKey());
        } else if (pref instanceof AddSearchProviderPreference) {
            dialogFragment = AddSearchProviderPreferenceDialogFragment.newInstance(pref.getKey());
        }

        if (dialogFragment != null) {
            // check if dialog is already showing
            if (getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
                return true;
            }
            dialogFragment.setTargetFragment(caller, 0);
            dialogFragment.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
            return true;
        }

        return false;
    }

    private void onDialogClosed(Preference pref, boolean positiveResult) {
        switch (pref.getKey()) {
            case "reset-history":
                if (positiveResult) {
                    KissApplication.getApplication(requireContext()).getDataHandler().clearHistory();
                    pref.setSummary(requireContext().getString(R.string.history_erased));
                    Toast.makeText(getContext(), R.string.history_erased, Toast.LENGTH_LONG).show();
                }
                break;
            case "reset-search-providers":
                if (positiveResult) {
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                            .remove("available-search-providers").apply();
                    KissApplication.getApplication(requireContext()).getDataHandler().reloadSearchProvider();
                    Toast.makeText(getContext(), R.string.search_provider_reset_done_desc, Toast.LENGTH_LONG).show();
                }
                break;
            case "reset-excluded-apps":
                if (positiveResult) {
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                            .putStringSet("excluded-apps", null).apply();
                    KissApplication.getApplication(requireContext()).getDataHandler().reloadApps();
                    Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
                }
                break;
            case "reset-excluded-from-history-apps":
                if (positiveResult) {
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                            .putStringSet("excluded-apps-from-history", null).apply();
                    KissApplication.getApplication(requireContext()).getDataHandler().reloadApps(); // reload because it's cached in AppPojo#excludedFromHistory
                    Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
                }
                break;
            case "reset-excluded-app-shortcuts":
                if (positiveResult) {
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                            .putStringSet(DataHandler.PREF_KEY_EXCLUDED_SHORTCUT_APPS, null).apply();
                    DataHandler dataHandler = KissApplication.getApplication(requireContext()).getDataHandler();
                    // Reload shortcuts to refresh the shortcuts shown in KISS
                    dataHandler.reloadShortcuts();
                    // Reload apps since the `AppPojo.isExcludedShortcuts` value also needs to be refreshed
                    dataHandler.reloadApps();
                    Toast.makeText(getContext(), R.string.excluded_app_list_erased, Toast.LENGTH_LONG).show();
                }
                break;
            case "reset-favorites":
                if (positiveResult) {
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                            .putString("favorite-apps-list", "").apply();

                    try {
                        KissApplication.getApplication(requireContext()).getDataHandler().reloadApps();
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Unable to reset favorites", e);
                    }

                    Toast.makeText(getContext(), R.string.favorites_erased, Toast.LENGTH_LONG).show();
                }
                break;
            case "reset-shortcuts":
                if (positiveResult && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Remove all shortcuts
                    ShortcutUtil.removeAllShortcuts(getContext());
                    // Build all shortcuts
                    ShortcutUtil.addAllShortcuts(getContext());
                    Toast.makeText(getContext(), R.string.regenerate_shortcuts_done, Toast.LENGTH_LONG).show();
                }
                break;
            case "enable-notifications":
                if (positiveResult && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                }
                break;
            case "default-launcher":
                new DefaultLauncherPreference().onDialogClosed(getContext(), positiveResult);
                break;
            case "export-settings":
                new ExportSettingsPreference().onDialogClosed(getContext(), positiveResult);
                break;
            case "import-settings":
                new ImportSettingsPreference().onDialogClosed(getContext(), positiveResult);
                break;
            case "restart":
                if (positiveResult) {
                    System.exit(0);
                }
                break;
        }
    }
}
