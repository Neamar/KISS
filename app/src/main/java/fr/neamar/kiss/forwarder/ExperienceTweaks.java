package fr.neamar.kiss.forwarder;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.ui.KeyboardManager;
import fr.neamar.kiss.utils.LockAccessibilityService;
import fr.neamar.kiss.utils.Log;

// Deals with any settings in the "User Experience" setting sub-screen
public class ExperienceTweaks extends Forwarder {
    /**
     * InputType that behaves as if the consuming IME is a standard-obeying
     * soft-keyboard
     * <p>
     * *Auto Complete* means "we're handling auto-completion ourselves". Then
     * we ignore whatever the IME thinks we should display.
     */
    private final static int INPUT_TYPE_STANDARD = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    /**
     * InputType that behaves as if the consuming IME is SwiftKey
     * <p>
     * *Visible Password* fields will break many non-Latin IMEs and may show
     * unexpected behaviour in numerous ways. (#454, #517)
     */
    private final static int INPUT_TYPE_WORKAROUND = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
    private static final String TAG = ExperienceTweaks.class.getSimpleName();

    private View mainEmptyView;
    private final GestureDetector gd;
    private final KeyboardManager keyboardManager;

    ExperienceTweaks(final MainActivity mainActivity) {
        super(mainActivity);

        // Lock launcher into landscape mode (desktop emulator)
        // Do it here (before initializing the view in onCreate) to make the transition as smooth as possible
        setRequestedOrientation(mainActivity, prefs);

        gd = new GestureDetector(mainActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                doAction("gesture-long-press", prefs.getString("gesture-long-press", "display-apps"));

                super.onLongPress(e);
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    return super.onDoubleTap(e);
                }
                if (!prefs.getBoolean("double-tap", false)) {
                    return super.onDoubleTap(e);
                }

                if (isAccessibilityServiceEnabled(mainActivity)) {
                    Intent intent = new Intent(LockAccessibilityService.ACTION_LOCK, null, mainActivity, LockAccessibilityService.class);
                    mainActivity.startService(intent);
                } else {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mainActivity);
                    builder.setMessage(R.string.enable_double_tap_to_lock);

                    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mainActivity.startActivity(intent);
                    });

                    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

                    AlertDialog alert = builder.create();
                    alert.show();
                }
                return super.onDoubleTap(e);
            }

            @Override
            public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null) {
                    return false;
                }
                float directionY = e2.getY() - e1.getY();
                float directionX = e2.getX() - e1.getX();
                if (Math.abs(directionX) > Math.abs(directionY)) {
                    if (directionX > 0) {
                        doAction("gesture-right", prefs.getString("gesture-right", "display-apps"));
                    } else {
                        doAction("gesture-left", prefs.getString("gesture-left", "display-apps"));
                    }
                } else {
                    if (directionY > 0) {
                        doAction("gesture-down", prefs.getString("gesture-down", "display-notifications"));
                    } else {
                        doAction("gesture-up", prefs.getString("gesture-up", "do-nothing"));
                    }
                }
                return true;
            }

            private void doAction(String source, String action) {
                switch (action) {
                    case "display-notifications":
                        displayNotificationDrawer();
                        break;
                    case "display-quicksettings":
                        displayQuickSettings();
                        break;
                    case "display-keyboard":
                        mainActivity.showKeyboard();
                        break;
                    case "hide-keyboard":
                        mainActivity.hideKeyboard();
                        break;
                    case "display-apps":
                        // The app grid is the home screen: go back to it
                        mainActivity.clearSearchText();
                        break;
                    case "display-menu":
                        mainActivity.openContextMenu(mainActivity.menuButton);
                        break;
                    case "go-to-homescreen":
                        // Opens the default launcher; when that's KISS, onNewIntent() resets the search
                        Intent home = new Intent(Intent.ACTION_MAIN);
                        home.addCategory(Intent.CATEGORY_HOME);
                        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mainActivity.startActivity(home);
                        break;
                    case "launch-pojo": {
                        String launchId = prefs.getString(source + "-launch-id", "");
                        Pojo item = KissApplication.getApplication(mainActivity).getDataHandler().getItemById(launchId);
                        if (item != null) {
                            Result<?> result = Result.fromPojo(mainActivity, item);
                            result.fastLaunch(mainActivity, ExperienceTweaks.this.mainEmptyView);
                        }
                        break;
                    }
                }
            }

            /**
             * Are we allowed to run our AccessibilityService?
             */
            private boolean isAccessibilityServiceEnabled(Context context) {
                AccessibilityManager am = ContextCompat.getSystemService(context, AccessibilityManager.class);
                if (am == null) {
                    return false;
                }

                List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

                for (AccessibilityServiceInfo enabledService : enabledServices) {
                    ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
                    if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(LockAccessibilityService.class.getName()))
                        return true;
                }

                return false;
            }
        });

        keyboardManager = new KeyboardManager();
    }

    void onCreate() {
        mainEmptyView = mainActivity.findViewById(R.id.main_empty);
    }

    void onResume() {
        keyboardManager.registerKeyboardListener(
                mainActivity.findViewById(android.R.id.content),
                false,
                mainActivity::onKeyboardVisibilityChanged);

        adjustInputType();

        // Never show the soft keyboard on its own: on MSI App Player it fights with the key mapping (WASD, shortcuts...).
        // It only shows when the user clicks the search field.
        mainActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // Also drops the focus of the search field when coming back home
        mainActivity.hideKeyboard();
        if (prefs.getBoolean("pref-hide-circle", false)) {
            ((ImageView) mainActivity.launcherButton).setImageBitmap(null);
            mainActivity.menuButton.setImageBitmap(null);
        }
    }

    void onTouch(MotionEvent event) {
        // Forward touch events to the gesture detector
        gd.onTouchEvent(event);
    }

    void updateSearchRecords(String query) {
        if (TextUtils.isEmpty(query)) {
            // Home screen: grid of all installed apps
            mainActivity.search(Searcher.Type.APPLICATION, null, false);
        }

        setFavoritesBarVisible(query);
    }

    private void setFavoritesBarVisible(CharSequence query) {
        mainActivity.setFavoritesBarVisible(TextUtils.isEmpty(query));
    }

    // Ensure the keyboard uses the right input method
    private void adjustInputType() {
        int currentInputType = mainActivity.searchEditText.getInputType();
        int requiredInputType;

        if (isSuggestionsEnabled()) {
            requiredInputType = InputType.TYPE_CLASS_TEXT;
        } else {
            if (isNonCompliantKeyboard()) {
                requiredInputType = INPUT_TYPE_WORKAROUND;
            } else {
                requiredInputType = INPUT_TYPE_STANDARD;
            }
        }
        if (currentInputType != requiredInputType) {
            mainActivity.searchEditText.setInputType(requiredInputType);
        }
    }

    // Super hacky code to display notification drawer
    // Can (and will) break in any Android release.
    protected void displayNotificationDrawer() {
        try {
            @SuppressLint("WrongConstant")
            Object sbservice = mainActivity.getSystemService("statusbar");
            Class.forName("android.app.StatusBarManager")
                    .getMethod("expandNotificationsPanel")
                    .invoke(sbservice);
        } catch (Exception e) {
            Log.e(TAG, "Unable to display notification drawer", e);
        }
    }

    protected void displayQuickSettings() {
        try {
            @SuppressLint("WrongConstant")
            Object sbservice = mainActivity.getSystemService("statusbar");
            Class.forName("android.app.StatusBarManager")
                    .getMethod("expandSettingsPanel")
                    .invoke(sbservice);
        } catch (Exception e) {
            Log.e(TAG, "Unable to display quick settings", e);
        }
    }

    /**
     * Should we force the keyboard not to display suggestions?
     * (swiftkey is broken, see <a href="https://github.com/Neamar/KISS/issues/44">https://github.com/Neamar/KISS/issues/44</a>)
     * (same for flesky: <a href="https://github.com/Neamar/KISS/issues/1263">https://github.com/Neamar/KISS/issues/1263</a>)
     */
    private boolean isNonCompliantKeyboard() {
        String currentKeyboard = Settings.Secure.getString(mainActivity.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD).toLowerCase(Locale.ROOT);
        return currentKeyboard.contains("swiftkey") || currentKeyboard.contains("flesky");
    }

    /**
     * Should the keyboard autocomplete and suggest options
     */
    private boolean isSuggestionsEnabled() {
        return prefs.getBoolean("enable-suggestions-keyboard", false);
    }

    public static void setRequestedOrientation(Activity activity, SharedPreferences prefs) {
        // MSI App Player runs on a PC monitor: always landscape (16:9 or stretched 4:3)
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
    }

    public void onPause() {
        keyboardManager.unregisterKeyboardListener();
    }

    public void onFavoriteChange() {
        setFavoritesBarVisible(mainActivity.searchEditText.getText());
    }
}
