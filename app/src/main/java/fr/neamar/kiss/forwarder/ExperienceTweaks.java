package fr.neamar.kiss.forwarder;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.searcher.HistorySearcher;
import fr.neamar.kiss.searcher.NullSearcher;
import fr.neamar.kiss.searcher.Searcher;

// Deals with any settings in the "User Experience" setting sub-screen
class ExperienceTweaks extends Forwarder {
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

    private final Runnable displayKeyboardRunnable = mainActivity::showKeyboard;

    private View mainEmptyView;
    private final GestureDetector gd;

    ExperienceTweaks(final MainActivity mainActivity) {
        super(mainActivity);

        // Lock launcher into portrait mode
        // Do it here (before initializing the view in onCreate) to make the transition as smooth as possible
        if (prefs.getBoolean("force-portrait", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            } else {
                mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

        gd = new GestureDetector(mainActivity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // if minimalistic mode is enabled,
                // and we want to display history on touch
                if (isMinimalisticModeEnabled() && prefs.getBoolean("history-onclick", false)) {
                    // and we're currently in minimalistic mode with no results,
                    // and we're not looking at the app list
                    if (mainActivity.isViewingSearchResults() && mainActivity.searchEditText.getText().toString().isEmpty()) {
                        if (mainActivity.list.getAdapter() == null || mainActivity.list.getAdapter().isEmpty()) {
                            mainActivity.runTask(new HistorySearcher(mainActivity));
                        }
                    }
                }

                if (isMinimalisticModeEnabledForFavorites()) {
                    mainActivity.favoritesBar.setVisibility(View.VISIBLE);
                }

                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float directionY = e2.getY() - e1.getY();
                float directionX = e2.getX() - e1.getX();
                if (Math.abs(directionX) > Math.abs(directionY)) {
                    return false;
                }
                if (!isGesturesEnabled()) {
                    return false;
                }
                if (directionY > 0) {
                    // Fling down: display notifications
                    displayNotificationDrawer();
                } else {
                    // Fling up: display keyboard
                    mainActivity.showKeyboard();
                }
                return true;
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
        if (isKeyboardOnStartEnabled()) {
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

    void onTouch(View view, MotionEvent event) {
        // Forward touch events to the gesture detector
        gd.onTouchEvent(event);
    }

    void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && isKeyboardOnStartEnabled()) {
            mainActivity.showKeyboard();
        }
    }

    void onDisplayKissBar(Boolean display) {
        if (isMinimalisticModeEnabledForFavorites()) {
            if (display) {
                mainActivity.favoritesBar.setVisibility(View.VISIBLE);
            } else {
                mainActivity.favoritesBar.setVisibility(View.GONE);
            }
        }

        if (!display && isKeyboardOnStartEnabled()) {
            // Display keyboard
            mainActivity.showKeyboard();
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
                Searcher searcher = new HistorySearcher(mainActivity);
                searcher.setRefresh(isRefresh);
                mainActivity.runTask(searcher);
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
    @SuppressLint("PrivateApi")
    @SuppressWarnings("CatchAndPrintStackTrace")
    private void displayNotificationDrawer() {
        @SuppressLint("WrongConstant") Object sbservice = mainActivity.getSystemService("statusbar");
        Class<?> statusbarManager = null;
        try {
            statusbarManager = Class.forName("android.app.StatusBarManager");
            Method showStatusBar;
            if (Build.VERSION.SDK_INT >= 17) {
                showStatusBar = statusbarManager.getMethod("expandNotificationsPanel");
            } else {
                showStatusBar = statusbarManager.getMethod("expand");
            }
            showStatusBar.invoke(sbservice);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
    }

    private boolean isMinimalisticModeEnabled() {
        return prefs.getBoolean("history-hide", false);
    }

    private boolean isMinimalisticModeEnabledForFavorites() {
        return prefs.getBoolean("history-hide", false) && prefs.getBoolean("favorites-hide", false);
    }

    /**
     * Should we force the keyboard not to display suggestions?
     * (swiftkey is broken, see https://github.com/Neamar/KISS/issues/44)
     * (same for flesky: https://github.com/Neamar/KISS/issues/1263)
     */
    private boolean isNonCompliantKeyboard() {
        String currentKeyboard = Settings.Secure.getString(mainActivity.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD).toLowerCase();
        return currentKeyboard.contains("swiftkey") || currentKeyboard.contains("flesky");
    }

    /**
     * Should the keyboard be displayed by default?
     */
    private boolean isKeyboardOnStartEnabled() {
        return prefs.getBoolean("display-keyboard", false);
    }

    /**
     * Should the keyboard autocomplete and suggest options
     */
    private boolean isSuggestionsEnabled() {
        return prefs.getBoolean("enable-suggestions-keyboard", false);
    }

    private boolean isGesturesEnabled() {
        return prefs.getBoolean("enable-gestures", true);
    }

}
