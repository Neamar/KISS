package fr.neamar.kiss.result;

import static fr.neamar.kiss.dataprovider.ProviderName.APPS;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.dataprovider.ShortcutsProvider;
import fr.neamar.kiss.icons.IconPack;
import fr.neamar.kiss.loader.PackageNameFilter;
import fr.neamar.kiss.notification.NotificationListener;
import fr.neamar.kiss.pojo.AppPojo;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public class AppResult extends ResultWithTags<AppPojo> {

    private static final String TAG = AppResult.class.getSimpleName();
    private volatile Drawable icon = null;

    private final QueryInterface queryInterface;

    AppResult(QueryInterface queryInterface, @NonNull AppPojo pojo) {
        super(pojo);
        this.queryInterface = queryInterface;
    }

    @NonNull
    @Override
    public View display(final Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null) {
            view = inflateFromId(context, R.layout.item_app, parent);
        }

        TextView appName = view.findViewById(R.id.item_app_name);

        displayHighlighted(pojo.normalizedName, pojo.getName(), fuzzyScore, appName, context);

        TextView tagsView = view.findViewById(R.id.item_app_tag);
        displayTags(context, fuzzyScore, tagsView);

        final ImageView appIcon = view.findViewById(R.id.item_app_icon);
        if (!isHideIcons(context)) {
            this.setAsyncDrawable(appIcon);
        } else {
            appIcon.setImageDrawable(null);
        }

        displayNotificationDot(context, view, false);
        displayShortcutsButton(context, view);

        return view;
    }

    private void displayShortcutsButton(Context context, View view) {
        View button = view.findViewById(R.id.item_app_action_shortcuts);
        List<ShortcutPojo> shortcuts = getShortcuts(context);
        if (shortcuts.isEmpty()) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }

        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(v -> showShortcutsPopup(context, v, shortcuts));
    }

    private void showShortcutsPopup(Context context, View anchor, List<ShortcutPojo> shortcuts) {
        Map<String, Integer> ranks = getShortcutRanks(context);
        List<ShortcutPojo> sorted = new ArrayList<>(shortcuts);
        // Dynamic, then static, then pinned shortcuts, and best rank last within each group: the menu opens upward, so the bottom is closest to the finger
        sorted.sort(Comparator.comparing(AppResult::getShortcutGroup)
                .thenComparing(shortcut -> ranks.getOrDefault(shortcut.getOreoId(), 0), Comparator.reverseOrder()));

        // null entry is the divider between groups
        List<ShortcutsResult> items = new ArrayList<>();
        for (ShortcutPojo shortcut : sorted) {
            if (!items.isEmpty() && getShortcutGroup(items.get(items.size() - 1).pojo) != getShortcutGroup(shortcut)) {
                items.add(null);
            }
            items.add(new ShortcutsResult(shortcut, pojo));
        }

        ListPopup menu = new ListPopup(context);
        DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
        boolean canPin = !PackageManagerUtils.isPrivateProfile(context, pojo.userHandle);

        // Shortcuts are named "App: Shortcut" for search, the app is redundant here
        String prefix = PackageManagerUtils.getLabel(context, pojo.packageName, pojo.userHandle) + ": ";

        ArrayAdapter<ShortcutsResult> adapter = new ArrayAdapter<ShortcutsResult>(context, R.layout.popup_shortcut_item, android.R.id.text1, items) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                ShortcutsResult item = getItem(position);
                if (item == null) {
                    return LayoutInflater.from(context).inflate(R.layout.popup_divider, parent, false);
                }
                View view = LayoutInflater.from(context).inflate(R.layout.popup_shortcut_item, parent, false);
                String name = item.pojo.getName();
                ((TextView) view.findViewById(android.R.id.text1)).setText(name.startsWith(prefix) ? name.substring(prefix.length()) : name);
                item.setAsyncDrawable(view.findViewById(android.R.id.icon));

                ImageButton pinButton = view.findViewById(R.id.popup_shortcut_pin);
                boolean pinned = item.pojo.isPinned();
                if (canPin && (pinned || item.pojo.isOreoShortcut())) {
                    pinButton.setImageResource(pinned ? R.drawable.ic_shortcut_unpin : R.drawable.ic_shortcut_pin);
                    pinButton.setContentDescription(context.getString(pinned ? R.string.menu_shortcut_remove : R.string.menu_shortcut_pin));
                    pinButton.setOnClickListener(v -> {
                        if (pinned) {
                            dataHandler.unpinShortcut(item.pojo);
                        } else {
                            dataHandler.pinShortcut(item.pojo);
                        }
                        menu.dismiss();
                    });
                } else {
                    pinButton.setVisibility(View.GONE);
                }
                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                return getItem(position) != null;
            }
        };

        menu.setAdapter(adapter);
        // Launching through the shortcut's own Result keeps history consistent with shortcuts found via search
        menu.setOnItemClickListener((a, itemView, position) -> items.get(position).launch(context, itemView, queryInterface));
        queryInterface.registerPopup(menu);
        menu.show(anchor);
        // Long menus scroll: start from the bottom, where the best shortcuts are
        menu.getContentView().post(() -> ((ScrollView) menu.getContentView()).fullScroll(View.FOCUS_DOWN));
    }

    private static int getShortcutGroup(ShortcutPojo shortcut) {
        return shortcut.isPinned() ? 2 : shortcut.isDynamic() ? 0 : 1;
    }

    /**
     * @return rank by shortcut id, lower is more important
     */
    private Map<String, Integer> getShortcutRanks(Context context) {
        Map<String, Integer> ranks = new HashMap<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                for (ShortcutInfo info : ShortcutUtil.getShortcuts(context, pojo.packageName)) {
                    if (info.getUserHandle().equals(pojo.userHandle.getRealHandle())) {
                        ranks.put(info.getId(), info.getRank());
                    }
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Unable to get shortcut ranks", e);
            }
        }
        return ranks;
    }

    private List<ShortcutPojo> getShortcuts(Context context) {
        List<ShortcutPojo> shortcuts = new ArrayList<>();
        ShortcutsProvider provider = KissApplication.getApplication(context).getDataHandler().getShortcutsProvider();
        if (provider == null) {
            return shortcuts;
        }
        // ponytail: linear scan of every shortcut for each displayed row, index by package in the provider if it shows up in profiles
        for (ShortcutPojo shortcut : provider.getPojos()) {
            if (shortcut.packageName.equals(pojo.packageName) && shortcut.getUserHandle().getRealHandle().equals(pojo.userHandle.getRealHandle())) {
                shortcuts.add(shortcut);
            }
        }
        return shortcuts;
    }

    @Override
    public void inflateFavorite(@NonNull Context context, @NonNull View favoriteView) {
        super.inflateFavorite(context, favoriteView);
        displayNotificationDot(context, favoriteView, true);
    }

    private void displayNotificationDot(Context context, View view, boolean isFavorite) {
        String packageKey = getPackageKey();

        SharedPreferences notificationPrefs = context.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
        ImageView notificationView = view.findViewById(R.id.item_notification_dot);
        notificationView.setVisibility(notificationPrefs.contains(packageKey) ? View.VISIBLE : View.GONE);
        notificationView.setTag(packageKey);
        int dotColor = UIColors.getNotificationDotColor(context, isFavorite);
        notificationView.setColorFilter(dotColor);
    }

    private String getPackageKey() {
        return pojo.getPackageKey();
    }

    @Override
    protected void buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter) {
        super.buildPopupMenu(context, adapter);

        adapter.add(new ListPopup.Item(context, R.string.menu_exclude));
        adapter.add(new ListPopup.Item(context, R.string.menu_app_rename));
        if (!pojo.isDisabled()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_details));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_app_store));

        boolean uninstallDisabled = pojo.isDisabled();
        if (!uninstallDisabled) {
            // app installed under /system can't be uninstalled
            ApplicationInfo ai = PackageManagerUtils.getApplicationInfo(context, this.pojo.packageName, this.pojo.userHandle);
            // Need to AND the flags with SYSTEM:
            uninstallDisabled = ai != null && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }

        if (!uninstallDisabled) {
            UserManager userManager = ContextCompat.getSystemService(context, UserManager.class);
            Bundle restrictions = userManager.getUserRestrictions(pojo.userHandle.getRealHandle());
            uninstallDisabled = restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false)
                    || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false);
        }

        if (!uninstallDisabled) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_uninstall));
        }

        // append root menu if available
        if (KissApplication.getApplication(context).getRootHandler().isRootActivated()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_app_hibernate));
        }
    }

    @Override
    protected boolean popupMenuClickHandler(final Context context, final RecordAdapter parent, int stringId, View parentView) {
        if (stringId == R.string.menu_app_details) {
            launchAppDetails(context);
            return true;
        } else if (stringId == R.string.menu_app_store) {
            launchAppStore(context);
            return true;
        } else if (stringId == R.string.menu_app_uninstall) {
            launchUninstall(context);
            return true;
        } else if (stringId == R.string.menu_app_hibernate) {
            hibernate(context);
            return true;
        } else if (stringId == R.string.menu_exclude) {
            final int EXCLUDE_HISTORY_ID = 0;
            final int EXCLUDE_KISS_ID = 1;
            PopupMenu popupExcludeMenu = new PopupMenu(context, parentView);
            //Adding menu items
            popupExcludeMenu.getMenu().add(EXCLUDE_HISTORY_ID, Menu.NONE, Menu.NONE, R.string.menu_exclude_history);
            popupExcludeMenu.getMenu().add(EXCLUDE_KISS_ID, Menu.NONE, Menu.NONE, R.string.menu_exclude_kiss);
            //registering popup with OnMenuItemClickListener
            popupExcludeMenu.setOnMenuItemClickListener(item -> {
                switch (item.getGroupId()) {
                    case EXCLUDE_HISTORY_ID:
                        excludeFromHistory(context, pojo);
                        return true;
                    case EXCLUDE_KISS_ID:
                        excludeFromKiss(context, pojo, parent);
                        return true;
                }

                return true;
            });

            popupExcludeMenu.show();
            return true;
        } else if (stringId == R.string.menu_app_rename) {
            launchRenameDialog(context, parent, pojo);
            return true;
        }

        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private void excludeFromHistory(Context context, AppPojo pojo) {
        // add to excluded from history app list
        KissApplication.getApplication(context).getDataHandler().addToExcludedFromHistory(pojo);
        // remove from history
        removeFromHistory(context);
        // inform user
        Toast.makeText(context, R.string.excluded_app_history_added, Toast.LENGTH_LONG).show();
    }

    private void excludeFromKiss(Context context, AppPojo pojo, final RecordAdapter parent) {
        // remove item since it will be hidden
        parent.removeResult(AppResult.this);

        KissApplication.getApplication(context).getDataHandler().addToExcluded(pojo);
        Toast.makeText(context, R.string.excluded_app_list_added, Toast.LENGTH_LONG).show();
    }

    private void launchRenameDialog(final Context context, RecordAdapter parent, final AppPojo app) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(context.getResources().getString(R.string.app_rename_title));

        builder.setView(R.layout.rename_dialog);

        builder.setPositiveButton(R.string.custom_name_rename, (dialog, which) -> {
            EditText input = ((AlertDialog) dialog).findViewById(R.id.rename);
            dialog.dismiss();

            // Set new name
            String newName = input.getText().toString().trim();
            DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
            dataHandler.updateApp(app, a -> a.setName(newName));
            dataHandler.renameApp(app.getComponentName(), newName);

            // Show toast message
            String msg = context.getResources().getString(R.string.app_rename_confirmation, newName);
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            setTranscriptModeAlwaysScroll(parent);
        });
        builder.setNegativeButton(R.string.custom_name_set_default, (dialog, which) -> {
            dialog.dismiss();

            KissApplication.getApplication(context).getDataHandler().removeRenameApp(getComponentName());

            // Set initial name
            String name = PackageManagerUtils.getLabel(context, new ComponentName(app.packageName, app.activityName), app.userHandle);
            if (name != null) {
                KissApplication.getApplication(context).getDataHandler().updateApp(app, a -> a.setName(name));

                // Show toast message
                String msg = context.getResources().getString(R.string.app_rename_confirmation, name);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
            setTranscriptModeAlwaysScroll(parent);
        });
        builder.setNeutralButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            setTranscriptModeAlwaysScroll(parent);
        });
        setTranscriptModeDisabled(parent);
        AlertDialog dialog = builder.create();
        dialog.show();
        // call after dialog got inflated (show call)
        ((TextView) dialog.findViewById(R.id.rename)).setText(app.getName());
    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context) {
        LauncherApps launcher = ContextCompat.getSystemService(context, LauncherApps.class);
        assert launcher != null;
        launcher.startAppDetailsActivity(getClassName(), pojo.userHandle.getRealHandle(), null, null);
    }

    private void launchAppStore(Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pojo.packageName)));
        } catch (ActivityNotFoundException anfe) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pojo.packageName)));
            } catch (ActivityNotFoundException e) {
                // Neither a store nor a browser is installed or enabled
                Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void hibernate(Context context) {
        String msg = context.getResources().getString(R.string.toast_hibernate_completed);
        if (!KissApplication.getApplication(context).getRootHandler().hibernateApp(pojo.packageName)) {
            msg = context.getResources().getString(R.string.toast_hibernate_error);
        } else {
            KissApplication.getApplication(context).getDataHandler().reload(new PackageNameFilter(pojo.packageName), APPS);
        }

        Toast.makeText(context, String.format(msg, pojo.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Open an activity to uninstall the current package
     */
    private void launchUninstall(Context context) {
        Intent intent = new Intent(Intent.ACTION_DELETE,
                Uri.fromParts("package", pojo.packageName, null));
        intent.putExtra(Intent.EXTRA_USER, pojo.userHandle.getRealHandle());
        context.startActivity(intent);
    }

    @Override
    boolean isDrawableCached() {
        return icon != null;
    }

    @Override
    void setDrawableCache(Drawable drawable) {
        icon = drawable;
    }

    @Override
    public Drawable getDrawable(Context context) {
        if (icon == null) {
            synchronized (this) {
                if (icon == null) {
                    IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
                    icon = iconsHandler.getDrawableIconForPackage(context, getClassName(), this.pojo.userHandle);
                }
            }
        }
        DrawableUtils.setDisabled(icon, this.pojo.isDisabled());
        return icon;
    }

    @Override
    public void doLaunch(Context context, View v) {
        try {
            LauncherApps launcher = ContextCompat.getSystemService(context, LauncherApps.class);
            assert launcher != null;
            Rect sourceBounds = null;
            Bundle opts = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // We're on a modern Android and can display activity animations
                // If AppResult, find the icon
                View potentialIcon = v.findViewById(R.id.item_app_icon);
                if (potentialIcon == null) {
                    // If favorite, find the icon
                    potentialIcon = v.findViewById(R.id.favorite);
                }

                if (potentialIcon != null) {
                    sourceBounds = getViewBounds(potentialIcon);

                    // If we got an icon, we create options to get a nice animation
                    opts = ActivityOptions.makeClipRevealAnimation(potentialIcon, 0, 0, potentialIcon.getMeasuredWidth(), potentialIcon.getMeasuredHeight()).toBundle();
                }
            }

            launcher.startMainActivity(getClassName(), pojo.userHandle.getRealHandle(), sourceBounds, opts);
        } catch (ActivityNotFoundException | NullPointerException | SecurityException e) {
            Log.w(TAG, "Unable to launch activity", e);
            // Application was just removed?
            // (null pointer exception can be thrown on Lollipop+ when app is missing)
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    protected Rect getViewBounds(@Nullable View view) {
        if (view == null) {
            return null;
        }

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    public String getComponentName() {
        return pojo.getComponentName();
    }

    public ComponentName getClassName() {
        return pojo.getComponent();
    }

    @Override
    protected boolean isAllowedAsFavorite() {
        return true;
    }

    @Override
    protected boolean canRemoveFromHistory(Context context) {
        return true;
    }

    @Override
    protected boolean canHaveCustomIcon(Context context, IconPack iconPack) {
        return true;
    }
}
