package fr.neamar.kiss.result;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.net.URISyntaxException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import fr.neamar.kiss.DataHandler;
import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.pojo.ShortcutPojo;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.FuzzyScore;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.ShortcutUtil;
import fr.neamar.kiss.utils.SpaceTokenizer;
import fr.neamar.kiss.utils.Utilities;

public class ShortcutsResult extends Result {

    private static final String TAG = ShortcutsResult.class.getSimpleName();

    private final ShortcutPojo shortcutPojo;

    private Utilities.AsyncRun mLoadIconTask = null;

    ShortcutsResult(ShortcutPojo shortcutPojo) {
        super(shortcutPojo);
        this.shortcutPojo = shortcutPojo;
    }

    @NonNull
    @Override
    public View display(final Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_shortcut, parent);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        TextView shortcutName = view.findViewById(R.id.item_app_name);

        displayHighlighted(shortcutPojo.normalizedName, shortcutPojo.getName(), fuzzyScore, shortcutName, context);

        TextView tagsView = view.findViewById(R.id.item_app_tag);

        // Hide tags view if tags are empty
        if (shortcutPojo.getTags().isEmpty()) {
            tagsView.setVisibility(View.GONE);
        } else if (displayHighlighted(shortcutPojo.getNormalizedTags(), shortcutPojo.getTags(),
                fuzzyScore, tagsView, context) || prefs.getBoolean("tags-visible", true)) {
            tagsView.setVisibility(View.VISIBLE);
        } else {
            tagsView.setVisibility(View.GONE);
        }

        final ImageView shortcutIcon = view.findViewById(R.id.item_shortcut_icon);
        final ImageView appIcon = view.findViewById(R.id.item_app_icon);

        if (!prefs.getBoolean("icons-hide", false)) {
            if (mLoadIconTask != null) {
                mLoadIconTask.cancel();
            }

            boolean subIconVisible = prefs.getBoolean("subicon-visible", true);

            AtomicReference<Drawable> appDrawable = new AtomicReference<>(null);
            AtomicReference<Drawable> shortcutDrawable = new AtomicReference<>(null);

            // Prepare
            shortcutIcon.setImageResource(android.R.color.transparent);
            if (subIconVisible) {
                appIcon.setVisibility(View.VISIBLE);
                appIcon.setImageResource(android.R.color.transparent);
            } else {
                appIcon.setVisibility(View.GONE);
            }

            mLoadIconTask = Utilities.runAsync((task) -> {
                if (task == mLoadIconTask) {
                    // Retrieve icon for this shortcut
                    appDrawable.set(getAppDrawable(context));
                    shortcutDrawable.set(getDrawable(context));
                }
            }, (task) -> {
                if (!task.isCancelled() && task == mLoadIconTask) {
                    // set icons
                    if (shortcutDrawable.get() != null) {
                        shortcutIcon.setImageDrawable(shortcutDrawable.get());
                        if (subIconVisible) {
                            appIcon.setImageDrawable(appDrawable.get());
                        }
                    } else {
                        // No icon for this shortcut, use app icon
                        shortcutIcon.setImageDrawable(appDrawable.get());
                        if (subIconVisible) {
                            appIcon.setImageResource(android.R.drawable.ic_menu_send);
                        }
                    }
                }
            });
        } else {
            appIcon.setImageDrawable(null);
            shortcutIcon.setImageDrawable(null);
        }

        return view;
    }

    private Drawable getAppDrawable(Context context) {
        Drawable appDrawable = null;
        IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();

        if (shortcutPojo.isOreoShortcut()) {
            // Retrieve activity icon from oreo shortcut
            appDrawable = getDrawableFromOreoShortcut(context);
        }

        if (appDrawable == null) {
            // Retrieve activity icon by intent URI
            try {
                Intent intent = Intent.parseUri(shortcutPojo.intentUri, 0);
                ComponentName componentName = PackageManagerUtils.getComponentName(context, intent);
                if (componentName != null) {
                    appDrawable = iconsHandler.getDrawableIconForPackage(PackageManagerUtils.getLaunchingComponent(context, componentName), new fr.neamar.kiss.utils.UserHandle());
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Unable to get activity icon for '" + shortcutPojo.getName() + "'", e);
            } catch (URISyntaxException e) {
                Log.e(TAG, "Unable to parse uri for '" + shortcutPojo.getName() + "'", e);
            }
        }

        if (appDrawable == null) {
            // Retrieve app icon (no Oreo shortcut or a shortcut from an activity that was removed from an installed app)
            appDrawable = PackageManagerUtils.getApplicationIcon(context, shortcutPojo.packageName);
            if (appDrawable != null) {
                appDrawable = iconsHandler.applyIconMask(context, appDrawable);
            }
        }
        return appDrawable;
    }

    public Drawable getDrawable(Context context) {
        Drawable shortcutDrawable = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutInfo shortcutInfo = getShortCut(context);
            if (shortcutInfo != null) {
                final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcherApps != null;
                shortcutDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
            }
        }

        if (shortcutDrawable != null) {
            shortcutDrawable = DrawableUtils.getThemedDrawable(context, shortcutDrawable);
            shortcutDrawable = KissApplication.getApplication(context).getIconsHandler().applyIconMask(context, shortcutDrawable);
        }

        return shortcutDrawable;
    }


    @Override
    protected void doLaunch(Context context, View v) {
        if (shortcutPojo.isOreoShortcut()) {
            // Oreo shortcuts
            doOreoLaunch(context, v);
        } else {
            // Pre-oreo shortcuts
            try {
                Intent intent = Intent.parseUri(shortcutPojo.intentUri, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    intent.setSourceBounds(v.getClipBounds());
                }

                context.startActivity(intent);
            } catch (Exception e) {
                // Application was just removed?
                Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void doOreoLaunch(Context context, View v) {
        final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcherApps != null;

        // Only the default launcher is allowed to start shortcuts
        if (!launcherApps.hasShortcutHostPermission()) {
            Toast.makeText(context, context.getString(R.string.shortcuts_no_host_permission), Toast.LENGTH_LONG).show();
            return;
        }

        ShortcutInfo shortcutInfo = getShortCut(context);
        if (shortcutInfo != null) {
            try {
                launcherApps.startShortcut(shortcutInfo, v.getClipBounds(), null);
                return;
            } catch (ActivityNotFoundException | IllegalStateException ignored) {
            }
        }

        // Application removed? Invalid shortcut? Shortcut to an app on an unmounted SD card?
        Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private ShortcutInfo getShortCut(Context context) {
        return ShortcutUtil.getShortCut(context, shortcutPojo.packageName, shortcutPojo.getOreoId());
    }

    @TargetApi(Build.VERSION_CODES.O)
    private Drawable getDrawableFromOreoShortcut(Context context) {
        ShortcutInfo shortcutInfo = getShortCut(context);
        if (shortcutInfo != null && shortcutInfo.getActivity() != null) {
            UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            fr.neamar.kiss.utils.UserHandle user = new fr.neamar.kiss.utils.UserHandle(manager.getSerialNumberForUser(shortcutInfo.getUserHandle()), shortcutInfo.getUserHandle());
            IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
            return iconsHandler.getDrawableIconForPackage(shortcutInfo.getActivity(), user);
        }
        return null;
    }

    @Override
    ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, RecordAdapter parent, View parentView) {
        if (!this.shortcutPojo.isDynamic() || this.shortcutPojo.isPinned()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        }
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_tags_edit));
        adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        if (!this.shortcutPojo.isPinned() && this.shortcutPojo.isOreoShortcut()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_shortcut_pin));
        }
        if (this.shortcutPojo.isPinned()) {
            adapter.add(new ListPopup.Item(context, R.string.menu_shortcut_remove));
        }

        return inflatePopupMenu(adapter, context);
    }

    @Override
    boolean popupMenuClickHandler(Context context, RecordAdapter parent, int stringId, View parentView) {
        if (stringId == R.string.menu_shortcut_pin) {
            pinShortcut(context, shortcutPojo);
            return true;
        } else if (stringId == R.string.menu_shortcut_remove) {
            launchUninstall(context, shortcutPojo);
            // Also remove item, since it will be uninstalled
            parent.removeResult(context, this);
            return true;
        } else if (stringId == R.string.menu_tags_edit) {
            launchEditTagsDialog(context, shortcutPojo);
            return true;
        }
        return super.popupMenuClickHandler(context, parent, stringId, parentView);
    }

    private void launchEditTagsDialog(final Context context, final ShortcutPojo pojo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.tags_add_title));

        // Create the tag dialog
        final View v = View.inflate(context, R.layout.tags_dialog, null);
        final MultiAutoCompleteTextView tagInput = v.findViewById(R.id.tag_input);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, KissApplication.getApplication(context).getDataHandler().getTagsHandler().getAllTagsAsArray());
        tagInput.setTokenizer(new SpaceTokenizer());
        tagInput.setText(shortcutPojo.getTags());

        tagInput.setAdapter(adapter);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            dialog.dismiss();
            // Refresh tags for given app
            pojo.setTags(tagInput.getText().toString().trim().toLowerCase(Locale.ROOT));
            KissApplication.getApplication(context).getDataHandler().getTagsHandler().setTags(pojo.id, pojo.getTags());
            // Show toast message
            String msg = context.getResources().getString(R.string.tags_confirmation_added);
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
    }

    private void launchUninstall(Context context, ShortcutPojo shortcutPojo) {
        DataHandler dh = KissApplication.getApplication(context).getDataHandler();
        if (shortcutPojo.isOreoShortcut() && shortcutPojo.isPinned()) {
            dh.unpinShortcut(shortcutPojo);
        } else {
            dh.removeShortcut(shortcutPojo);
        }
    }

    private void pinShortcut(Context context, ShortcutPojo shortcutPojo) {
        DataHandler dataHandler = KissApplication.getApplication(context).getDataHandler();
        dataHandler.pinShortcut(shortcutPojo);
    }

}
