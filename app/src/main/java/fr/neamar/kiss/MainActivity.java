package fr.neamar.kiss;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.view.HapticFeedbackConstants.LONG_PRESS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;
import fr.neamar.kiss.forwarder.ForwarderManager;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.searcher.SearchHandler;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.ui.AnimatedListView;
import fr.neamar.kiss.ui.KeyboardScrollHider;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.ui.SearchEditText;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.Permission;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;
import fr.neamar.kiss.utils.TrimmingTextChangedListener;

public class MainActivity extends AppCompatActivity implements QueryInterface, KeyboardScrollHider.KeyboardHandler, View.OnTouchListener {

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
     * Receive events from providers
     */
    private BroadcastReceiver mReceiver;

    /**
     * View for the Search text
     */
    public SearchEditText searchEditText;

    /**
     * Main list view
     */
    public AnimatedListView list;
    public View listContainer;
    /**
     * View to display when list is empty
     */
    public View emptyListView;
    /**
     * Utility for automatically hiding the keyboard when scrolling down
     */
    public KeyboardScrollHider hider;

    /**
     * The ViewGroup that wraps the buttons at the right hand side of the searchEditText
     */
    public ViewGroup rightHandSideButtonsWrapper;
    /**
     * Menu button
     */
    public View menuButton;
    /**
     * Kiss bar
     */
    public View kissBar;
    /**
     * Favorites bar. Can be either the favorites within the KISS bar,
     * or the external favorites bar (default)
     */
    public ViewGroup favoritesBar;
    /**
     * Progress bar displayed when loading
     */
    private View loaderSpinner;

    /**
     * The ViewGroup that wraps the buttons at the left hand side of the searchEditText
     */
    public ViewGroup leftHandSideButtonsWrapper;
    /**
     * Launcher button, can be clicked to display all apps
     */
    public View launcherButton;

    /**
     * Launcher button's white counterpart, which appears when launcher button is clicked
     */
    public View whiteLauncherButton;
    /**
     * "X" button to empty the search field
     */
    public View clearButton;

    /**
     * SystemUiVisibility helper
     */
    private SystemUiVisibilityHelper systemUiVisibilityHelper;

    /**
     * Is the KISS bar currently displayed?
     * (flag updated before animation is over)
     */
    private boolean isDisplayingKissBar = false;

    private PopupWindow mPopup;

    private ForwarderManager forwarderManager;
    private Permission permissionManager;
    private OnBackPressedCallback onBackPressedCallback;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        /*
         * Initialize preferences
         */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /*
         * Initialize all forwarders
         */
        forwarderManager = new ForwarderManager(this);
        permissionManager = new Permission(this);

        /*
         * Initialize data handler and start loading providers
         */
        IntentFilter intentFilterLoad = new IntentFilter(START_LOAD);
        IntentFilter intentFilterLoadOver = new IntentFilter(LOAD_OVER);
        IntentFilter intentFilterRefresh = new IntentFilter(REFRESH_FAVORITES);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (REFRESH_FAVORITES.equals(intent.getAction())) {
                    onFavoriteChange();
                    if (prefs.getBoolean("exclude-favorites-history", false)) {
                        // update search to reflect favorite change, if the "exclude favorites" option is active
                        updateSearchRecords();
                    }
                } else if (LOAD_OVER.equalsIgnoreCase(intent.getAction())) {
                    updateSearchRecords();
                    if (!KissApplication.getApplication(context).getDataHandler().isAllProvidersLoaded()) {
                        displayLoader(true);
                    } else {
                        Log.v(TAG, "All providers are done loading.");

                        displayLoader(false);

                        // Run GC once to free all the garbage accumulated during provider initialization
                        System.gc();
                    }
                    // New provider might mean new favorites
                    onFavoriteChange();
                } else if (START_LOAD.equalsIgnoreCase(intent.getAction())) {
                    displayLoader(true);
                    // New provider might mean new favorites
                    onFavoriteChange();
                }
            }
        };

        this.onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainActivity.this.handleOnBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(onBackPressedCallback);

        ContextCompat.registerReceiver(this, mReceiver, intentFilterLoad, ContextCompat.RECEIVER_EXPORTED);
        ContextCompat.registerReceiver(this, mReceiver, intentFilterLoadOver, ContextCompat.RECEIVER_EXPORTED);
        ContextCompat.registerReceiver(this, mReceiver, intentFilterRefresh, ContextCompat.RECEIVER_EXPORTED);

        /*
         * Set the view and store all useful components
         */
        setContentView(R.layout.main);
        this.list = this.findViewById(android.R.id.list);
        this.listContainer = (View) this.list.getParent();
        this.emptyListView = this.findViewById(android.R.id.empty);
        this.kissBar = findViewById(R.id.mainKissbar);
        this.rightHandSideButtonsWrapper = findViewById(R.id.rightHandSideButtonsWrapper);
        this.menuButton = findViewById(R.id.menuButton);
        this.searchEditText = findViewById(R.id.searchEditText);
        this.loaderSpinner = findViewById(R.id.loaderBar);
        this.leftHandSideButtonsWrapper = findViewById(R.id.leftHandSideButtonsWrapper);
        this.launcherButton = findViewById(R.id.launcherButton);
        this.whiteLauncherButton = findViewById(R.id.whiteLauncherButton);
        this.clearButton = findViewById(R.id.clearButton);

        /*
         * Initialize components behavior
         * Note that a lot of behaviors are also initialized through the forwarderManager.onCreate() call.
         * {@code initDataHandler} must be called after {@link MainActivity#displayLoader(boolean)} and after {@link MainActivity#mReceiver} is registered.
         * If {@code dataHandler} is already existing at this point this may result in undefined behaviour.
         */
        displayLoader(true);
        KissApplication.getApplication(this).initDataHandler();

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

        // Display KISS menu
        launcherButton.setOnClickListener(this::onLauncherButtonClicked);
        whiteLauncherButton.setOnClickListener(this::onLauncherButtonClicked);

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

                forwarderManager.onDataSetChanged();

            }
        });

        // Listen to changes
        searchEditText.addTextChangedListener(new TrimmingTextChangedListener(true, (changedText) -> {
            if (isViewingAllApps()) {
                displayKissBar(false, false);
            }
            updateSearchRecords(false, changedText);
            displayClearOnInput();
        }));

        // Fixes bug when dropping onto a textEdit widget which can cause a NPE
        // This fix should be on ALL TextEdit Widgets !!!
        // See : https://stackoverflow.com/a/23483957
        searchEditText.setOnDragListener((v, event) -> true);

        // On validate, launch first record
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.R.id.closeButton) {
                systemUiVisibilityHelper.onKeyboardVisibilityChanged(false);
                if (mPopup != null) {
                    mPopup.dismiss();
                    return true;
                }
                systemUiVisibilityHelper.onKeyboardVisibilityChanged(false);
                hider.fixScroll();
                return false;
            }

            if (prefs.getBoolean("always-default-web-search-on-enter", false)) {
                SearchPojo pojo = SearchProvider.getDefaultSearch(v.getText().toString(), MainActivity.this, prefs);
                if (pojo != null) {
                    Result.fromPojo(MainActivity.this, pojo).fastLaunch(MainActivity.this, null);
                }
            } else {
                adapter.onClick(adapter.getCount() - 1, v);
            }

            return true;
        });

        registerForContextMenu(menuButton);

        // When scrolling down on the list,
        // Hide the keyboard.
        this.hider = new KeyboardScrollHider(this,
                this.list,
                this.findViewById(R.id.listEdgeEffect)
        );
        this.hider.start();

        // Enable/disable phone broadcast receiver
        IncomingCallHandler.setEnabled(this, prefs.getBoolean("enable-phone-history", false));

        // Hide the "X" after the text field, instead displaying the menu button
        displayClearOnInput();

        systemUiVisibilityHelper = new SystemUiVisibilityHelper(this);

        // For devices with hardware keyboards, give focus to search field.
        if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY || getResources().getConfiguration().keyboard == Configuration.KEYBOARD_12KEY) {
            searchEditText.requestFocus();
        }

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

        forwarderManager.onCreateContextMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        forwarderManager.onStart();
    }

    /**
     * Restart if required,
     * Hide the kissbar by default
     */
    protected void onResume() {
        Log.d(TAG, "onResume()");

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

        if (KissApplication.getApplication(this).getDataHandler().isAllProvidersLoaded()) {
            displayLoader(false);
            onFavoriteChange();
        }

        // We need to update the history in case an external event created new items
        // (for instance, installed a new app, got a phone call or simply clicked on a favorite)
        updateSearchRecords(false, searchEditText.getText().toString());
        displayClearOnInput();

        if (isViewingAllApps()) {
            displayKissBar(false);
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
        onBackPressedCallback.remove();
        this.unregisterReceiver(this.mReceiver);
        forwarderManager.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        //Set the intent so KISS can tell when it was launched as an assistant
        setIntent(intent);

        // This is called when the user press Home again while already browsing MainActivity
        // onResume() will be called right after, hiding the kissbar if any.
        // http://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)
        // Animation can't happen in this method, since the activity is not resumed yet, so they'll happen in the onResume()
        // https://github.com/Neamar/KISS/issues/569
        if (!TextUtils.isEmpty(searchEditText.getText())) {
            Log.i(TAG, "Clearing search field");
            clearSearchText();
        }

        // Hide kissbar when coming back to kiss
        if (isViewingAllApps()) {
            displayKissBar(false);
        }

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
        } else if (isViewingAllApps()) {
            displayKissBar(false);
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
        if (!e.isSystem()) {
            searchEditText.requestFocus();
            searchEditText.dispatchKeyEvent(e);
        }
        return super.onKeyDown(keycode, e);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (forwarderManager.onOptionsItemSelected(item)) {
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.settings) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
            return true;
        } else if (itemId == R.id.wallpaper) {
            hideKeyboard();
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            startActivity(Intent.createChooser(intent, getString(R.string.menu_wallpaper)));
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
        // When the kiss bar is displayed, the button can still be clicked in a few areas (due to favorite margin)
        // To fix this, we discard any click event occurring when the kissbar is displayed
        if (!isViewingSearchResults()) {
            return;
        }
        if (!forwarderManager.onMenuButtonClicked(menuButton)) {
            menuButton.showContextMenu();
            menuButton.performHapticFeedback(LONG_PRESS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        forwarderManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
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

    /**
     * Display KISS menu
     */
    private void onLauncherButtonClicked(View launcherButton) {
        // Display or hide the kiss bar, according to current view tag (showMenu / hideMenu).
        displayKissBar(launcherButton.getTag().equals("showMenu"));
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
                edit.setCursorVisible(!isKeyboardOpen);
            }
        }
        if (mPopup != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            dismissPopup();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    public void displayClearOnInput() {
        if (!TextUtils.isEmpty(searchEditText.getText())) {
            clearButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.INVISIBLE);
        } else {
            clearButton.setVisibility(View.INVISIBLE);
            menuButton.setVisibility(View.VISIBLE);
        }
    }

    public void displayLoader(boolean display) {
        if (!display) {
            // Do not display animation if launcher button is already visible
            if (launcherButton.getVisibility() != View.VISIBLE) {
                launcherButton.setVisibility(View.VISIBLE);

                int animationDuration = getResources().getInteger(android.R.integer.config_longAnimTime);

                // Animate transition from loader to launch button
                launcherButton.animate()
                        .alpha(1f)
                        .setDuration(animationDuration)
                        .setListener(null);
                loaderSpinner.animate()
                        .alpha(0f)
                        .setDuration(animationDuration)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                loaderSpinner.setVisibility(View.GONE);
                            }
                        });
            }
        } else {
            launcherButton.animate().cancel();
            launcherButton.setAlpha(0);
            launcherButton.setVisibility(View.INVISIBLE);

            loaderSpinner.animate().cancel();
            loaderSpinner.setAlpha(1);
            loaderSpinner.setVisibility(View.VISIBLE);
        }
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

    public void displayKissBar(boolean display) {
        this.displayKissBar(display, true);
    }

    protected void displayKissBar(boolean display, boolean clearSearchText) {
        dismissPopup();
        // get the center for the clipping circle
        ViewGroup launcherButtonWrapper = (ViewGroup) launcherButton.getParent();
        int cx = (launcherButtonWrapper.getLeft() + launcherButtonWrapper.getRight()) / 2;
        int cy = (launcherButtonWrapper.getTop() + launcherButtonWrapper.getBottom()) / 2;

        // get the final radius for the clipping circle
        int finalRadius = Math.max(kissBar.getWidth(), kissBar.getHeight());

        if (display) {
            // Display the app list
            if (!TextUtils.isEmpty(searchEditText.getText())) {
                clearSearchText();
            }
            cancelSearch();

            // Needs to be done after setting the text content to empty
            isDisplayingKissBar = true;

            updateSearchRecords(false, searchEditText.getText().toString());

            // Reveal the bar
            int animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

            Animator anim = ViewAnimationUtils.createCircularReveal(kissBar, cx, cy, 0, finalRadius);
            anim.setDuration(animationDuration);
            anim.start();
            kissBar.setVisibility(View.VISIBLE);

            // Display the alphabet on the scrollbar (#926)
            list.setFastScrollEnabled(true);
        } else {
            isDisplayingKissBar = false;
            // Hide the bar
            int animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

            try {
                Animator anim = ViewAnimationUtils.createCircularReveal(kissBar, cx, cy, finalRadius, 0);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        kissBar.setVisibility(View.GONE);
                        super.onAnimationEnd(animation);
                    }
                });
                anim.setDuration(animationDuration);
                anim.start();
            } catch (IllegalStateException e) {
                // If the view hasn't been laid out yet, we can't animate it
                kissBar.setVisibility(View.GONE);
            }

            if (clearSearchText) {
                clearSearchText();
            }

            // Do not display the alphabetical scrollbar (#926)
            // They only make sense when displaying apps alphabetically, not for searching
            list.setFastScrollEnabled(false);
        }

        forwarderManager.onDisplayKissBar(display);
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
        dismissPopup();

        if (isRefresh) {
            // Refreshing (for instance app installed or uninstalled in the background, profile unlocked, ...)
            search(Searcher.Type.APPLICATION, query, true);
            return;
        }

        forwarderManager.updateSearchRecords(query);

        if (TextUtils.isEmpty(query)) {
            systemUiVisibilityHelper.resetScroll();
        } else {
            search(Searcher.Type.QUERY, query, false);
        }
    }

    public void search(@NonNull Searcher.Type type, String query, boolean isRefresh) {
        SearchHandler.getInstance().search(type, this, query, isRefresh);
    }

    private void cancelSearch() {
        SearchHandler.getInstance().cancelSearch();
    }

    /**
     * transcriptMode on the listView decides when to scroll back to the first item.
     * The value we have by default, TRANSCRIPT_MODE_ALWAYS_SCROLL, means that on every new search,
     * (actually, on any change to the listview's adapter items)
     * scroll is reset to the bottom, which makes sense as we want the most relevant search results
     * to be visible first (searching for "ab" after "a" should reset the scroll).
     * However, when updating an existing result set (for instance to remove a record, add a tag,
     * etc.), we don't want the scroll to be reset. When this happens, we temporarily disable
     * the scroll mode.
     * However, we need to be careful here: the PullView system we use actually relies on
     * TRANSCRIPT_MODE_ALWAYS_SCROLL being active. So we add a new message in the queue to change
     * back the transcript mode once we've rendered the change.
     * <p>
     * (why is PullView dependent on this? When you show the keyboard, no event is being dispatched
     * to our application, but if we don't reset the scroll when the keyboard appears then you
     * could be looking at an element that isn't the latest one as you start scrolling down
     * [which will hide the keyboard] and start a very ugly animation revealing items currently
     * hidden. Fairly easy to test, remove the transcript mode from the XML and the .post() here,
     * then scroll in your history, display the keyboard and scroll again on your history)
     */
    @Override
    public void temporarilyDisableTranscriptMode() {
        updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
        // Add a message to be processed after all current messages, to reset transcript mode to default
        list.post(() -> updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL));
    }

    /**
     * Force  set transcript mode.
     * Be careful when using this, it's almost always better to use temporarilyDisableTranscriptMode()
     * unless you need to deal with the keyboard appearing for something else than a search.
     * Always make sure you call this function twice, once to disable, and once to re-enable
     *
     * @param transcriptMode new transcript mode to set on the list
     */
    @Override
    public void updateTranscriptMode(int transcriptMode) {
        list.setTranscriptMode(transcriptMode);
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
        } else if (isViewingAllApps()) {
            displayKissBar(false);
        }
    }

    public void registerPopup(ListPopup popup) {
        if (mPopup == popup)
            return;
        dismissPopup();
        mPopup = popup;
        popup.setVisibilityHelper(systemUiVisibilityHelper);
        popup.setOnDismissListener(() -> MainActivity.this.mPopup = null);
        hider.fixScroll();
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
        forwarderManager.onWindowFocusChanged(hasFocus);
    }


    public void showKeyboard() {
        if (searchEditText.requestFocus()) {
            searchEditText.setCursorVisible(true);
            InputMethodManager mgr = ContextCompat.getSystemService(this, InputMethodManager.class);
            mgr.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            systemUiVisibilityHelper.onKeyboardVisibilityChanged(true);
        }
    }

    @Override
    public void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = ContextCompat.getSystemService(this, InputMethodManager.class);
            //noinspection ConstantConditions
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            systemUiVisibilityHelper.onKeyboardVisibilityChanged(false);
        }

        dismissPopup();

        if (view == searchEditText) {
            searchEditText.setCursorVisible(false);
            searchEditText.clearFocus();
        }
    }

    @Override
    public void applyScrollSystemUi() {
        systemUiVisibilityHelper.applyScrollSystemUi();
    }

    /**
     * Check if history / search or app list is visible
     *
     * @return true of history, false on app list
     */
    public boolean isViewingSearchResults() {
        return !isDisplayingKissBar;
    }

    public boolean isViewingAllApps() {
        return isDisplayingKissBar;
    }

    public void beforeListChange() {
        list.prepareChangeAnim();
    }

    public void afterListChange() {
        list.animateChange();
    }

    public void dismissPopup() {
        if (mPopup != null)
            mPopup.dismiss();
    }

    public void showMatchingTags(String tag) {
        search(Searcher.Type.TAGGED, tag, false);

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }

    public void showUntagged() {
        search(Searcher.Type.UNTAGGED, null, false);

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }

    public void showHistory() {
        search(Searcher.Type.HISTORY, null, false);

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
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
        Log.d(TAG, "onConfigurationChanged, uiMode = " + (newConfig.uiMode & UI_MODE_NIGHT_MASK));
    }
}
