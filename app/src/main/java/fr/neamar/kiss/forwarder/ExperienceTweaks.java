package fr.neamar.kiss.forwarder;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.searcher.HistorySearcher;
import fr.neamar.kiss.searcher.NullSearcher;
import fr.neamar.kiss.utils.LockAccessibilityService;

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

    private final Runnable displayKeyboardRunnable = mainActivity::showKeyboard;

    private View mainEmptyView;
    private final GestureDetector gd;
    private int lastHeight = 0;

    ExperienceTweaks(final MainActivity mainActivity) {
        super(mainActivity);

        // Lock launcher into portrait mode
        // Do it here (before initializing the view in onCreate) to make the transition as smooth as possible
        setRequestedOrientation(mainActivity, prefs);

        gd = new GestureDetector(mainActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // Double tap disabled: display history directly
                if (!prefs.getBoolean("double-tap", false)) {
                    if (prefs.getBoolean("history-onclick", false)) {
                        doAction("single-tap", "display-history");
                    } else if (isMinimalisticModeEnabledForFavorites()) {
                        doAction("single-tap", "display-favorites");
                    }
                }
                return super.onSingleTapUp(e);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // Double tap enabled: wait to confirm this is indeed a single tap, not a double tap
                if (prefs.getBoolean("double-tap", false)) {
                    if (prefs.getBoolean("history-onclick", false)) {
                        doAction("single-tap", "display-history");
                    } else if (isMinimalisticModeEnabledForFavorites()) {
                        doAction("single-tap", "display-favorites");
                    }
                }

                return super.onSingleTapConfirmed(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                doAction("gesture-long-press", prefs.getString("gesture-long-press", "do-nothing"));

                super.onLongPress(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                    builder.setMessage(R.string.enable_double_tap_to_lock);

                    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mainActivity.startActivity(intent);
                    });

                    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        dialog.dismiss();
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
                return super.onDoubleTap(e);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
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
                        doAction("gesture-up", prefs.getString("gesture-up", "display-keyboard"));
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
                        if (mainActivity.isViewingSearchResults()) {
                            mainActivity.displayKissBar(true);
                        }
                        break;
                    case "display-history":
                        // if minimalistic mode is enabled,
                        if (isMinimalisticModeEnabled()) {
                            // and we're currently in minimalistic mode with no results,
                            // and we're not looking at the app list
                            if (mainActivity.isViewingSearchResults() && TextUtils.isEmpty(mainActivity.searchEditText.getText())) {
                                if (mainActivity.list.getAdapter() == null || mainActivity.list.getAdapter().isEmpty()) {
                                    mainActivity.showHistory();
                                }
                            }
                        }

                        if (isMinimalisticModeEnabledForFavorites()) {
                            mainActivity.favoritesBar.setVisibility(View.VISIBLE);
                        }
                        break;
                    case "display-favorites":
                        // Not provided as an option for the gestures, but useful if you only want to display favorites on tap,
                        // not history.
                        mainActivity.favoritesBar.setVisibility(View.VISIBLE);
                        break;
                    case "display-menu":
                        mainActivity.openContextMenu(mainActivity.menuButton);
                        break;
                    case "go-to-homescreen":
                        mainActivity.displayKissBar(false);
                        if (!shouldShowKeyboard()) {
                            mainActivity.hideKeyboard();
                        }
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
                AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
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
    }

    void onCreate() {
        mainEmptyView = mainActivity.findViewById(R.id.main_empty);
    }

    void onResume() {
        adjustInputType();

        // Activity manifest specifies stateAlwaysHidden as windowSoftInputMode
        // so the keyboard will be hidden by default
        // we may want to display it if the setting is set
        if (shouldShowKeyboard()) {
            // Display keyboard
            mainActivity.showKeyboard();

            new Handler().postDelayed(displayKeyboardRunnable, 10);
            // For some weird reasons, keyboard may be hidden by the system
            // So we have to run this multiple time at different time
            // See https://github.com/Neamar/KISS/issues/119
            new Handler().postDelayed(displayKeyboardRunnable, 100);
            new Handler().postDelayed(displayKeyboardRunnable, 500);
        } else {
            // Not used (thanks windowSoftInputMode)
            // unless coming back from KISS settings
            mainActivity.hideKeyboard();
        }

        if (isMinimalisticModeEnabled()) {
            mainEmptyView.setVisibility(View.GONE);

            mainActivity.list.setVerticalScrollBarEnabled(false);
            mainActivity.searchEditText.setHint("");
        }
        if (prefs.getBoolean("pref-hide-circle", false)) {
            ((ImageView) mainActivity.launcherButton).setImageBitmap(null);
            ((ImageView) mainActivity.menuButton).setImageBitmap(null);
        }
    }

    void onTouch(MotionEvent event) {
        // Forward touch events to the gesture detector
        gd.onTouchEvent(event);
    }

    private float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    void onGlobalLayout() {
        // There's no easy way to check if a soft keyboard is visible in android, but it can be safely assumed that
        // if the root layout is significantly smaller than the screen, it's been resized for a keyboard. See here:
        // https://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android
        if (isMinimalisticModeEnabled() && prefs.getBoolean("history-onkeyboard", false) &&
                mainActivity.isViewingSearchResults() && TextUtils.isEmpty(mainActivity.searchEditText.getText())) {
            final View activityRootView = mainActivity.findViewById(android.R.id.content);
            int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
            if (heightDiff > dpToPx(mainActivity.getBaseContext(), 200)) {
                // If it's more than 200dp, it's most likely a keyboard.
                if (mainActivity.adapter == null || mainActivity.adapter.isEmpty()) {
                    mainActivity.showHistory();
                    mainActivity.displayClearOnInput();
                }
            } else {
                // we never want this triggered because the keyboard scroller did it
                if (mainActivity.adapter != null && !mainActivity.adapter.isEmpty() && !mainActivity.hider.isScrolled()) {

                    // Only apply changes when height changes, this avoids breakage when history-ontouch is enabled or scrolled
                    if (activityRootView.getHeight() != lastHeight) {
                        // reset edittext (hide history)
                        mainActivity.searchEditText.setText("");
                    }
                }
            }
            lastHeight = activityRootView.getHeight();
        }
    }

    void onWindowFocusChanged(boolean hasFocus) {
    }

    void onDisplayKissBar(boolean display) {
        if (isMinimalisticModeEnabledForFavorites() && !display) {
            mainActivity.favoritesBar.setVisibility(View.GONE);
        }
    }

    void updateSearchRecords(boolean isRefresh, String query) {
        if (query.isEmpty()) {
            if (isMinimalisticModeEnabled()) {
                mainActivity.runTask(new NullSearcher(mainActivity));
                // By default, help text is displayed -- not in minimalistic mode.
                mainEmptyView.setVisibility(View.GONE);

                if (isMinimalisticModeEnabledForFavorites()) {
                    mainActivity.favoritesBar.setVisibility(View.GONE);
                }
            } else {
                mainActivity.runTask(new HistorySearcher(mainActivity, isRefresh));
            }
        }
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
        @SuppressLint("WrongConstant") Object sbservice = mainActivity.getSystemService("statusbar");
        Class<?> statusbarManager;
        try {
            statusbarManager = Class.forName("android.app.StatusBarManager");
            Method showStatusBar;
            if (Build.VERSION.SDK_INT >= 17) {
                showStatusBar = statusbarManager.getMethod("expandNotificationsPanel");
            } else {
                showStatusBar = statusbarManager.getMethod("expand");
            }
            showStatusBar.invoke(sbservice);
        } catch (Exception e) {
            Log.e(TAG, "Unable to display notification drawer", e);
        }
    }

    protected void displayQuickSettings() {
        try {
            @SuppressLint("WrongConstant") Object sbservice = mainActivity.getSystemService("statusbar");
            Class.forName("android.app.StatusBarManager")
                    .getMethod("expandSettingsPanel")
                    .invoke(sbservice);
        } catch (Exception e) {
            Log.e(TAG, "Unable to display quick settings", e);
        }
    }

    protected boolean isMinimalisticModeEnabled() {
        return prefs.getBoolean("history-hide", false);
    }

    protected boolean isMinimalisticModeEnabledForFavorites() {
        return isMinimalisticModeEnabled() && prefs.getBoolean("favorites-hide", false) && prefs.getBoolean("enable-favorites-bar", true);
    }

    /**
     * Should we force the keyboard not to display suggestions?
     * (swiftkey is broken, see https://github.com/Neamar/KISS/issues/44)
     * (same for flesky: https://github.com/Neamar/KISS/issues/1263)
     */
    private boolean isNonCompliantKeyboard() {
        String currentKeyboard = Settings.Secure.getString(mainActivity.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD).toLowerCase(Locale.ROOT);
        return currentKeyboard.contains("swiftkey") || currentKeyboard.contains("flesky");
    }

    /**
     * Should the keyboard be displayed by default?
     */
    private boolean isKeyboardOnStartEnabled() {
        return prefs.getBoolean("display-keyboard", false);
    }

    /**
     * Should the keyboard be displayed?
     */
    protected boolean shouldShowKeyboard() {
        boolean isAssistant = "android.intent.action.ASSIST".equalsIgnoreCase(mainActivity.getIntent().getAction());
        return (isAssistant || isKeyboardOnStartEnabled());
    }

    /**
     * Should the keyboard autocomplete and suggest options
     */
    private boolean isSuggestionsEnabled() {
        return prefs.getBoolean("enable-suggestions-keyboard", false);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public static void setRequestedOrientation(Activity activity, SharedPreferences prefs) {
        if (prefs.getBoolean("force-portrait", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }
    }
}
