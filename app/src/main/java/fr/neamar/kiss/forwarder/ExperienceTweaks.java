package fr.neamar.kiss.forwarder;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.regex.Pattern;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.searcher.HistorySearcher;
import fr.neamar.kiss.searcher.NullSearcher;

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

    private final Runnable displayKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            showKeyboard();
        }
    };

    ExperienceTweaks(MainActivity mainActivity) {
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
    }

    void onCreate() {
        adjustInputType(null);
    }

    void onResume() {
        // Activity manifest specifies stateAlwaysHidden as windowSoftInputMode
        // so the keyboard will be hidden by default
        // we may want to display it if the setting is set
        if (isKeyboardOnStartEnabled()) {
            // Display keyboard
            showKeyboard();

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
    }

    void onTouch(View view, MotionEvent event) {
        //if motion movement ends
        if ((event.getAction() == MotionEvent.ACTION_CANCEL) || (event.getAction() == MotionEvent.ACTION_UP)) {
            // and minimalistic mode is enabled,
            // and we want to display history on touch
            if (isMinimalisticModeEnabled() && prefs.getBoolean("history-onclick", false)) {
                // and we're currently in minimalistic mode with no results,
                // and we're not looking at the app list
                if ((mainActivity.isViewingSearchResults()) && (mainActivity.searchEditText.getText().toString().isEmpty())) {
                    if ((mainActivity.list.getAdapter() == null) || (mainActivity.list.getAdapter().isEmpty())) {
                        mainActivity.runTask(new HistorySearcher(mainActivity));
                    }
                }
            }

            if (isMinimalisticModeEnabledForFavorites()) {
                mainActivity.favoritesBar.setVisibility(View.VISIBLE);
            }
        }
    }

    void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && isKeyboardOnStartEnabled()) {
            showKeyboard();
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
            showKeyboard();
        }
    }

    void updateRecords(String query) {
        adjustInputType(query);

        if (query.isEmpty()) {
            if (isMinimalisticModeEnabled()) {
                mainActivity.list.setVerticalScrollBarEnabled(false);
                mainActivity.searchEditText.setHint("");
                mainActivity.runTask(new NullSearcher(mainActivity));
                //Hide default scrollview
                mainActivity.findViewById(R.id.main_empty).setVisibility(View.GONE);

                if (isMinimalisticModeEnabledForFavorites()) {
                    mainActivity.favoritesBar.setVisibility(View.GONE);
                }
            } else {
                mainActivity.list.setVerticalScrollBarEnabled(true);
                mainActivity.searchEditText.setHint(R.string.ui_search_hint);
                mainActivity.runTask(new HistorySearcher(mainActivity));
                //Show default scrollview
                mainActivity.findViewById(R.id.main_empty).setVisibility(View.VISIBLE);
            }
        }
    }

    // Ensure the keyboard uses the right input method
    private void adjustInputType(String currentText) {
        int currentInputType = mainActivity.searchEditText.getInputType();
        int requiredInputType;

        if (currentText != null && Pattern.matches("[+]\\d+", currentText)) {
            requiredInputType = InputType.TYPE_CLASS_PHONE;
        } else if (isNonCompliantKeyboard()) {
            requiredInputType = INPUT_TYPE_WORKAROUND;
        } else {
            requiredInputType = INPUT_TYPE_STANDARD;
        }
        if (currentInputType != requiredInputType) {
            mainActivity.searchEditText.setInputType(requiredInputType);
        }
    }

    private void showKeyboard() {
        mainActivity.searchEditText.requestFocus();
        InputMethodManager mgr = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;
        mgr.showSoftInput(mainActivity.searchEditText, InputMethodManager.SHOW_IMPLICIT);

        mainActivity.systemUiVisibilityHelper.onKeyboardVisibilityChanged(true);
    }

    private boolean isMinimalisticModeEnabled() {
        return prefs.getBoolean("history-hide", false);
    }

    private boolean isMinimalisticModeEnabledForFavorites() {
        return prefs.getBoolean("history-hide", false) && prefs.getBoolean("favorites-hide", false);
    }

    /**
     * Should we force the keyboard not to display suggestions?
     * (swiftkey)
     */
    private boolean isNonCompliantKeyboard() {
        return prefs.getBoolean("enable-keyboard-workaround", false);
    }

    /**
     * Should the keyboard be displayed by default?
     */
    private boolean isKeyboardOnStartEnabled() {
        return prefs.getBoolean("display-keyboard", false);
    }
}
