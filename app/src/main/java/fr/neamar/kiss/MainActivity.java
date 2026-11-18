package fr.neamar.kiss;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.view.HapticFeedbackConstants.LONG_PRESS;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.forwarder.ForwarderManager;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.searcher.SearchHandler;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.settings.SettingsFragment;
import fr.neamar.kiss.ui.BlockableGridView;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.ui.SearchEditText;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.Permission;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;
import fr.neamar.kiss.utils.TrimmingTextChangedListener;

public class MainActivity extends AppCompatActivity implements QueryInterface, View.OnTouchListener {

    public static final String START_LOAD = "fr.neamar.summon.START_LOAD";
    public static final String LOAD_OVER = "fr.neamar.summon.LOAD_OVER";
    public static final String REFRESH_FAVORITES = "fr.neamar.summon.REFRESH_FAVORITES";

    protected static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Adapter to display records
     */
    public RecordAdapter adapter;

    /**
     * Store user preferences
     */
    public SharedPreferences prefs;

    /**
     * View for the Search text
     */
    public SearchEditText searchEditText;

    /**
     * Main grid of results (apps when the search is empty)
     */
    public BlockableGridView list;
    public View listContainer;
    /**
     * View to display when list is empty
     */
    public View emptyListView;
    /**
     * The ViewGroup that wraps the buttons at the right hand side of the searchEditText
     */
    public ViewGroup rightHandSideButtonsWrapper;
    /**
     * Menu button
     */
    public ImageView menuButton;
    /**
     * Favorites bar, docked at the bottom of the screen
     */
    public RecyclerView favoritesBar;
    /**
     * Progress bar displayed when loading
     */
    private View loaderSpinner;

    /**
     * The ViewGroup that wraps the buttons at the left hand side of the searchEditText
     */
    public ViewGroup leftHandSideButtonsWrapper;
    /**
     * Launcher button, can be clicked to go back to the app grid
     */
    // TODO: Check what's wrong with launcherButton. It is an ImageView, but it's not visible anymore if declared as ImageView here.
    public View launcherButton;
    /**
     * "X" button to empty the search field
     */
    public ImageView clearButton;

    /**
     * SystemUiVisibility helper
     */
    private SystemUiVisibilityHelper systemUiVisibilityHelper;

    /**
     * Should keyboard be shown on focus of window?
     */
    Boolean showKeyboardOnFocus = null;

    /**
     * The system may drop a keyboard request made while a window transition runs (e.g. when coming back to KISS):
     * ask again a few times until the keyboard is visible.
     */
    private static final int KEYBOARD_RETRY_DELAY = 1000;
    private static final int KEYBOARD_RETRIES = 2;
    private int keyboardRetries = 0;
    private final Runnable retryShowKeyboard = this::retryShowKeyboard;

    private PopupWindow mPopup;

    private ForwarderManager forwarderManager;
    private Permission permissionManager;
    private OnBackPressedCallback onBackPressedCallback;
    /**
     * Night mode the UI was built with
     */
    private int nightMode;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // During a full backup restore, Android may start the process in restricted mode with a plain Application.
        // The system kills that process once the restore is over and the launcher is started again normally.
        if (!(getApplication() instanceof KissApplication)) {
            Log.w(TAG, "Not running in KissApplication, process is likely in restricted mode (backup/restore)");
            finish();
            return;
        }

        /*
         * Initialize preferences
         */
        SettingsFragment.setDefaultValues(this, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        nightMode = getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK;

        /*
         * Initialize all forwarders
         */
        forwarderManager = new ForwarderManager(this);
        permissionManager = new Permission(this);

        this.onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainActivity.this.handleOnBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);

        /*
         * Initialize data handler and start loading providers
         */
        KissApplication.getApplication(this).getDataHandler().setMainActivity(this);

        /*
         * Set the view and store all useful components
         */
        setContentView(R.layout.main);
        this.list = this.findViewById(android.R.id.list);
        this.listContainer = (View) this.list.getParent();
        this.emptyListView = this.findViewById(android.R.id.empty);
        this.rightHandSideButtonsWrapper = findViewById(R.id.rightHandSideButtonsWrapper);
        this.menuButton = findViewById(R.id.menuButton);
        this.searchEditText = findViewById(R.id.searchEditText);
        this.loaderSpinner = findViewById(R.id.loaderBar);
        this.leftHandSideButtonsWrapper = findViewById(R.id.leftHandSideButtonsWrapper);
        this.launcherButton = findViewById(R.id.launcherButton);
        this.clearButton = findViewById(R.id.clearButton);

        /*
         * Initialize components behavior
         * Note that a lot of behaviors are also initialized through the forwarderManager.onCreate() call.
         */
        displayLoader(true);

        // Add touch listener for history popup to root view
        findViewById(android.R.id.content).setOnTouchListener(this);

        // add history popup touch listener to empty view (prevents it from not working there)
        this.emptyListView.setOnTouchListener(this);

        // Create adapter for records
        this.adapter = new RecordAdapter(this, new ArrayList<>());
        this.list.setAdapter(this.adapter);

        this.list.setOnItemClickListener((parent, v, position, id) -> adapter.onClick(position, v));
        this.list.setOnItemLongClickListener((parent, v, pos, id) -> {
            adapter.onLongClick(pos, v);
            return true;
        });

        // Clear text content when touching the cross button
        clearButton.setOnClickListener(v -> clearSearchText());

        // Display menu
        menuButton.setOnClickListener(this::onMenuButtonClicked);

        // Go back to the app grid
        launcherButton.setOnClickListener(v -> clearSearchText());

        // Display empty list view when having no results
        this.adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.isEmpty()) {
                    // Display help text when no results available
                    listContainer.setVisibility(View.GONE);
                    emptyListView.setVisibility(View.VISIBLE);
                } else {
                    // Otherwise, display results
                    listContainer.setVisibility(View.VISIBLE);
                    emptyListView.setVisibility(View.GONE);
                }

            }
        });

        // Listen to changes
        searchEditText.addTextChangedListener(new TrimmingTextChangedListener(true, (changedText) -> updateSearchRecords(false, changedText)));

        // Fixes bug when dropping onto a textEdit widget which can cause a NPE
        // This fix should be on ALL TextEdit Widgets !!!
        // See : https://stackoverflow.com/a/23483957
        searchEditText.setOnDragListener((v, event) -> true);

        // On validate, launch first record
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            // Back key, forwarded on key up by SearchEditText
            if (actionId == android.R.id.closeButton) {
                if (mPopup != null) {
                    mPopup.dismiss();
                    return true;
                }
                return false;
            }

            // Hardware keyboards send both key down and key up for Enter: only act once
            if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) {
                return true;
            }

            // Launch the most relevant result, displayed first in the grid
            if (!adapter.isEmpty()) {
                adapter.onClick(0, v);
            }

            return true;
        });

        registerForContextMenu(menuButton);

        // Hide the "X" after the text field, instead displaying the menu button
        displayClearOnInput();

        systemUiVisibilityHelper = new SystemUiVisibilityHelper(this, false);

        // Never give the focus to the search field on start: the grid gets it instead,
        // so that the soft keyboard doesn't show up and arrow keys / Enter navigate between apps.
        list.requestFocus();

        /*
         * Defer everything else to the forwarders
         */
        forwarderManager.onCreate();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem privateSpaceItem = menu.findItem(R.id.private_space);
        if (privateSpaceItem != null) {
            UserHandle privateUser = getPrivateUser();
            if (privateUser == null) {
                privateSpaceItem.setVisible(false);
            } else if (isPrivateSpaceUnlocked(privateUser)) {
                privateSpaceItem.setTitle(R.string.lock_private_space);
            } else {
                privateSpaceItem.setTitle(R.string.unlock_private_space);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return onOptionsItemSelected(item);
    }

    /**
     * Restart if required
     */
    protected void onResume() {
        if (prefs.getBoolean("require-layout-update", false)) {
            super.onResume();
            Log.i(TAG, "Restarting app after setting changes");
            // Restart current activity to refresh view, since some preferences
            // may require using a new UI
            prefs.edit().putBoolean("require-layout-update", false).apply();
            this.recreate();
            return;
        }

        dismissPopup();
        permissionManager.askPendingPermissions();

        if (KissApplication.getApplication(this).getDataHandler().isAllProvidersLoaded()) {
            displayLoader(false);
            onFavoriteChange();
        }

        // No full refresh here: apps are only reloaded when a package is added / removed / changed
        // (see PackageAddedRemovedHandler and LauncherAppsCallback), the grid is already up to date.
        if (adapter.isEmpty() && !SearchHandler.getInstance().isSearchRunning()) {
            updateSearchRecords(false, searchEditText.getText().toString());
        }

        forwarderManager.onResume();

        // Pasting shared text via intent-filter into kiss search bar
        Intent receivedIntent = getIntent();
        String receivedIntentAction = receivedIntent.getAction();
        String receivedIntentType = receivedIntent.getType();
        if (Intent.ACTION_SEND.equals(receivedIntentAction) && "text/plain".equals(receivedIntentType)) {
            hideKeyboard();
            String sharedText = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
            // making sure the shared text is not an empty string
            if (sharedText != null && !TextUtils.isEmpty(sharedText.trim())) {
                searchEditText.setText(sharedText);
            } else {
                Toast.makeText(this, R.string.shared_text_empty, Toast.LENGTH_SHORT).show();
            }
        }

        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        forwarderManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (forwarderManager == null) {
            // onCreate() bailed out early, nothing to clean up
            return;
        }
        onBackPressedCallback.remove();
        KissApplication.getApplication(this).getDataHandler().removeMainActivity(this);
    }

    /**
     * Receive events from DataHandler and providers, see {@link DataHandler#notifyMainActivity(String)}
     */
    public void onDataEvent(String action) {
        if (REFRESH_FAVORITES.equals(action)) {
            onFavoriteChange();
            if (prefs.getBoolean("exclude-favorites-history", true)) {
                // update search to reflect favorite change, if the "exclude favorites" option is active
                updateSearchRecords();
            }
        } else if (LOAD_OVER.equals(action)) {
            updateSearchRecords();
            if (!KissApplication.getApplication(this).getDataHandler().isAllProvidersLoaded()) {
                displayLoader(true);
            } else {
                Log.v(TAG, "All providers are done loading.");

                displayLoader(false);
            }
            // New provider might mean new favorites
            onFavoriteChange();
        } else if (START_LOAD.equals(action)) {
            displayLoader(true);
            // New provider might mean new favorites
            onFavoriteChange();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (forwarderManager == null) {
            // onCreate() bailed out early, activity is finishing
            return;
        }

        //Set the intent so KISS can tell when it was launched as an assistant
        setIntent(intent);

        // This is called when the user press Home again while already browsing MainActivity
        // onResume() will be called right after.
        // http://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)
        // Animation can't happen in this method, since the activity is not resumed yet, so they'll happen in the onResume()
        // https://github.com/Neamar/KISS/issues/569
        if (!TextUtils.isEmpty(searchEditText.getText())) {
            Log.i(TAG, "Clearing search field");
            clearSearchText();
        }

        // Back to the first apps when coming back home
        scrollToTop();

        // Close the backButton context menu
        closeContextMenu();
    }

    public void clearSearchText() {
        searchEditText.setText("");
        searchEditText.setCursorVisible(false);
    }

    private void handleOnBackPressed() {
        if (mPopup != null) {
            mPopup.dismiss();
        } else if (!TextUtils.isEmpty(searchEditText.getText()) || isKissDefaultLauncher()) {
            // If no kissmenu, empty the search bar
            // (this will trigger a new event if the search bar was already empty)
            // (which means pressing back in minimalistic mode with history displayed
            // will hide history again)
            clearSearchText();
        } else {
            // close activity only when not default launcher and searchEditText is empty
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keycode, @NonNull KeyEvent e) {
        if (keycode == KeyEvent.KEYCODE_MENU) {
            // For devices with a physical menu button, we still want to display *our* contextual menu
            menuButton.showContextMenu();
            menuButton.performHapticFeedback(LONG_PRESS);
            return true;
        }
        // Type-to-search with a physical keyboard: only printable characters go to the search field,
        // arrows / Enter / Escape keep navigating the grid.
        if (!e.isSystem() && e.isPrintingKey() && !e.isCtrlPressed() && !e.isAltPressed() && !searchEditText.hasFocus()) {
            searchEditText.requestFocus();
            searchEditText.setCursorVisible(true);
            searchEditText.dispatchKeyEvent(e);
            return true;
        }
        return super.onKeyDown(keycode, e);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.settings) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
            return true;
        } else if (itemId == R.id.preferences) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.private_space) {
            switchPrivateSpaceState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Display menu, on short or long press.
     *
     * @param menuButton "kebab" menu (3 dots)
     */
    private void onMenuButtonClicked(View menuButton) {
        menuButton.showContextMenu();
        menuButton.performHapticFeedback(LONG_PRESS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (forwarderManager.onTouch(view, event)) {
            return true;
        }

        if (view.getId() == searchEditText.getId() && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            searchEditText.performClick();
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof SearchEditText) {
                SearchEditText edit = ((SearchEditText) v);
                Rect outR = new Rect();
                edit.getGlobalVisibleRect(outR);
                boolean isKeyboardOpen = !outR.contains((int) ev.getRawX(), (int) ev.getRawY());
                if (prefs.getBoolean("hide-keyboard", false) && isKeyboardOpen) {
                    edit.postDelayed(this::hideKeyboard, 100);
                } else {
                    edit.setCursorVisible(!isKeyboardOpen);
                }
            }
        }
        if (mPopup != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            dismissPopup();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void displayClearOnInput() {
        if (!TextUtils.isEmpty(searchEditText.getText())) {
            clearButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.INVISIBLE);
        } else {
            clearButton.setVisibility(View.INVISIBLE);
            menuButton.setVisibility(View.VISIBLE);
        }
    }

    public void displayLoader(boolean display) {
        // No fade animation: switch instantly between the loader and the launcher button
        launcherButton.setVisibility(display ? View.INVISIBLE : View.VISIBLE);
        loaderSpinner.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    private UserHandle getPrivateUser() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            final LauncherApps launcher = ContextCompat.getSystemService(this, LauncherApps.class);
            assert launcher != null;

            List<UserHandle> users = launcher.getProfiles();

            for (UserHandle user : users) {
                if (PackageManagerUtils.isPrivateProfile(launcher, user)) {
                    return user;
                }
            }
        }
        return null;
    }

    private boolean isPrivateSpaceUnlocked(UserHandle privateUser) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            final UserManager manager = ContextCompat.getSystemService(this, UserManager.class);
            return !manager.isQuietModeEnabled(privateUser);
        }
        return false;
    }

    private void switchPrivateSpaceState() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            UserHandle user = getPrivateUser();
            if (user != null) {
                final UserManager manager = ContextCompat.getSystemService(this, UserManager.class);
                manager.requestQuietModeEnabled(!manager.isQuietModeEnabled(user), user);
            }
        }
    }

    private void onFavoriteChange() {
        forwarderManager.onFavoriteChange();
    }

    public void updateSearchRecords() {
        updateSearchRecords(true, searchEditText.getText().toString());
    }

    /**
     * This function gets called on query changes.
     * It will ask all the providers for data
     * This function is not called for non search-related changes! Have a look at onDataSetChanged() if that's what you're looking for :)
     *
     * @param isRefresh whether the query is refreshing the existing result, or is a completely new query
     * @param query     the query on which to search
     */
    protected void updateSearchRecords(boolean isRefresh, String query) {
        cancelSearch();

        if (isRefresh) {
            // Refreshing (for instance app installed or uninstalled in the background, profile unlocked, ...)
            // Keep the menu the user may have opened: other apps can change their shortcuts at any time
            // (the type and query of the last search are reused)
            search(Searcher.Type.APPLICATION, query, true);
            return;
        }

        dismissPopup();

        forwarderManager.updateSearchRecords(query);

        if (TextUtils.isEmpty(query)) {
            systemUiVisibilityHelper.resetScroll();
            displayClearOnInput();
        } else {
            search(Searcher.Type.QUERY, query, false);
        }
    }

    public void search(@NonNull Searcher.Type type, String query, boolean isRefresh) {
        SearchHandler.getInstance().search(type, this, query, isRefresh);
        displayClearOnInput();
    }

    private void cancelSearch() {
        SearchHandler.getInstance().cancelSearch();
    }

    @Override
    public void scrollToTop() {
        if (list != null && !list.getAdapter().isEmpty()) {
            list.setSelection(0);
        }
    }

    /**
     * Call this function when we're leaving the activity after clicking a search result
     * to clear the search list.
     * We can't use onPause(), since it may be called for a configuration change
     */
    @Override
    public void launchOccurred() {
        // We selected an item on the list,
        // now we can cleanup the filter:
        if (!TextUtils.isEmpty(searchEditText.getText())) {
            clearSearchText();
            displayClearOnInput();
            hideKeyboard();
        }
    }

    public void registerPopup(ListPopup popup) {
        if (mPopup == popup)
            return;
        dismissPopup();
        mPopup = popup;
        popup.setVisibilityHelper(systemUiVisibilityHelper);
        popup.setOnDismissListener(() -> MainActivity.this.mPopup = null);
    }

    @Override
    public void showDialog(DialogFragment dialog) {
        final View resultLayout = findViewById(R.id.resultLayout);
        if (dialog instanceof CustomIconDialog) {
            // We assume the mResultLayout was visible
            resultLayout.setVisibility(View.GONE);
            ((CustomIconDialog) dialog).setOnDismissListener(dlg -> {
                resultLayout.setVisibility(View.VISIBLE);
                // force icon reload by searching again; is there any better way?
                updateSearchRecords();
            });
        }
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        systemUiVisibilityHelper.onWindowFocusChanged(hasFocus);
        if (showKeyboardOnFocus != null) {
            if (showKeyboardOnFocus) {
                showKeyboard();
            } else {
                hideKeyboard();
            }
        }
    }

    /**
     * Focus {@link #searchEditText} and show keyboard
     * <p>
     * Uses {@link WindowCompat} instead of {@link InputMethodManager} here
     * For details see <a href="https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility#ShowReliably">online docu</a>.
     */
    public void showKeyboard() {
        if (!hasWindowFocus()) {
            showKeyboardOnFocus = true;
            return;
        }
        showKeyboardOnFocus = null;

        if (searchEditText.requestFocus()) {
            searchEditText.setCursorVisible(true);
            WindowCompat.getInsetsController(getWindow(), searchEditText).show(WindowInsetsCompat.Type.ime());
            searchEditText.removeCallbacks(retryShowKeyboard);
            if (!isKeyboardVisible()) {
                keyboardRetries = KEYBOARD_RETRIES;
                searchEditText.postDelayed(retryShowKeyboard, KEYBOARD_RETRY_DELAY);
            }
        }
    }

    private void retryShowKeyboard() {
        if (keyboardRetries-- > 0 && hasWindowFocus() && searchEditText.hasFocus() && !isKeyboardVisible()) {
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), searchEditText);
            // The system may still consider the keyboard requested although it dropped the request: showing it again
            // would do nothing, hide it first so that a new request is sent
            controller.hide(WindowInsetsCompat.Type.ime());
            controller.show(WindowInsetsCompat.Type.ime());
            searchEditText.postDelayed(retryShowKeyboard, KEYBOARD_RETRY_DELAY);
        }
    }

    private boolean isKeyboardVisible() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(searchEditText);
        // Unknown before Android 6: assume it is, rather than hiding and showing it again for nothing
        return insets == null || insets.isVisible(WindowInsetsCompat.Type.ime());
    }

    public void hideKeyboard() {
        showKeyboardOnFocus = null;
        searchEditText.removeCallbacks(retryShowKeyboard);

        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = ContextCompat.getSystemService(this, InputMethodManager.class);
            //noinspection ConstantConditions
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        dismissPopup();

        if (view == searchEditText) {
            searchEditText.setCursorVisible(false);
            searchEditText.clearFocus();
            // Give the focus back to the grid (keyboard / gamepad navigation)
            list.requestFocus();
        }
    }

    public void dismissPopup() {
        if (mPopup != null) {
            mPopup.dismiss();
        }
    }

    public boolean isKissDefaultLauncher() {
        String homePackage;
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = getPackageManager();
            final ResolveInfo mInfo = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            homePackage = mInfo.activityInfo.packageName;
        } catch (Exception e) {
            homePackage = "unknown";
        }

        return homePackage.equals(this.getPackageName());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        forwarderManager.onConfigurationChanged(newConfig);
        int nightMode = newConfig.uiMode & UI_MODE_NIGHT_MASK;
        Log.d(TAG, "onConfigurationChanged, uiMode = " + nightMode);
        // Only a theme change needs a new UI: plugging a keyboard / mouse must not reload the grid
        if (nightMode != this.nightMode) {
            recreate();
        }
    }

    public void setFavoritesBarVisible(boolean isVisible) {
        if (isVisible && (favoritesBar.getAdapter() == null || favoritesBar.getAdapter().getItemCount() > 0)) {
            favoritesBar.setVisibility(View.VISIBLE);
        } else {
            favoritesBar.setVisibility(View.GONE);
        }
    }

    public void onKeyboardVisibilityChanged(boolean keyboardIsVisible) {
        // The system handled the request: don't show the keyboard again if the user hides it
        searchEditText.removeCallbacks(retryShowKeyboard);
        systemUiVisibilityHelper.onKeyboardVisibilityChanged(keyboardIsVisible);
    }
}
