package fr.neamar.kiss;

import static android.view.HapticFeedbackConstants.LONG_PRESS;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherUserInfo;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.AdapterView;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.dataprovider.simpleprovider.SearchProvider;
import fr.neamar.kiss.forwarder.ForwarderManager;
import fr.neamar.kiss.pojo.SearchPojo;
import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.searcher.ApplicationsSearcher;
import fr.neamar.kiss.searcher.HistorySearcher;
import fr.neamar.kiss.searcher.QueryInterface;
import fr.neamar.kiss.searcher.QuerySearcher;
import fr.neamar.kiss.searcher.Searcher;
import fr.neamar.kiss.searcher.TagsSearcher;
import fr.neamar.kiss.searcher.UntaggedSearcher;
import fr.neamar.kiss.ui.AnimatedListView;
import fr.neamar.kiss.ui.BottomPullEffectView;
import fr.neamar.kiss.ui.KeyboardScrollHider;
import fr.neamar.kiss.ui.ListPopup;
import fr.neamar.kiss.ui.SearchEditText;
import fr.neamar.kiss.utils.Permission;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;

public class MainActivity extends Activity implements QueryInterface, KeyboardScrollHider.KeyboardHandler, View.OnTouchListener {

    public static final String START_LOAD = "fr.neamar.summon.START_LOAD";
    public static final String LOAD_OVER = "fr.neamar.summon.LOAD_OVER";
    public static final String FULL_LOAD_OVER = "fr.neamar.summon.FULL_LOAD_OVER";

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
     * Task launched on text change
     */
    private Searcher searchTask;

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

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        KissApplication.getApplication(this).initDataHandler();

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
        IntentFilter intentFilterFullLoadOver = new IntentFilter(FULL_LOAD_OVER);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //noinspection ConstantConditions
                if (intent.getAction().equalsIgnoreCase(LOAD_OVER)) {
                    updateSearchRecords();
                } else if (intent.getAction().equalsIgnoreCase(FULL_LOAD_OVER)) {
                    Log.v(TAG, "All providers are done loading.");

                    displayLoader(false);

                    // Run GC once to free all the garbage accumulated during provider initialization
                    System.gc();
                } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    if (intent.getAction().equalsIgnoreCase(Intent.ACTION_PROFILE_AVAILABLE)
                        || intent.getAction().equalsIgnoreCase(Intent.ACTION_PROFILE_UNAVAILABLE)) {
                        privateSpaceStateEvent(intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle.class));
                    }
                }

                // New provider might mean new favorites
                onFavoriteChange();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Since Android 33, we need to specify is the receiver is available from other applications
            // For some reasons, in our case, using RECEIVER_NOT_EXPORTED means we do not get the updates from our own services?!
            // So we export the receiver.
            // In practice, this means other apps can trigger a refresh of search results if they want by sending a broadcast.
            this.registerReceiver(mReceiver, intentFilterLoad, Context.RECEIVER_EXPORTED);
            this.registerReceiver(mReceiver, intentFilterLoadOver, Context.RECEIVER_EXPORTED);
            this.registerReceiver(mReceiver, intentFilterFullLoadOver, Context.RECEIVER_EXPORTED);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                IntentFilter intentFilterProfileAvailable = new IntentFilter(Intent.ACTION_PROFILE_AVAILABLE);
                IntentFilter intentFilterProfileUnAvailable = new IntentFilter(Intent.ACTION_PROFILE_UNAVAILABLE);
                this.registerReceiver(mReceiver, intentFilterProfileAvailable, Context.RECEIVER_EXPORTED);
                this.registerReceiver(mReceiver, intentFilterProfileUnAvailable, Context.RECEIVER_EXPORTED);
            }
        }
        else {
            this.registerReceiver(mReceiver, intentFilterLoad);
            this.registerReceiver(mReceiver, intentFilterLoadOver);
            this.registerReceiver(mReceiver, intentFilterFullLoadOver);
        }

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
         */
        displayLoader(true);

        // Add touch listener for history popup to root view
        findViewById(android.R.id.content).setOnTouchListener(this);

        // Add layout change listener for soft keyboard detection
        findViewById(android.R.id.content).getViewTreeObserver().addOnGlobalLayoutListener(() -> forwarderManager.onGlobalLayout());

        // add history popup touch listener to empty view (prevents it from not working there)
        this.emptyListView.setOnTouchListener(this);

        // Create adapter for records
        this.adapter = new RecordAdapter(this, new ArrayList<>());
        this.list.setAdapter(this.adapter);

        this.list.setOnItemClickListener((parent, v, position, id) -> adapter.onClick(position, v));

        this.list.setLongClickable(true);
        this.list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
                ((RecordAdapter) parent.getAdapter()).onLongClick(pos, v);
                return true;
            }
        });

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
        searchEditText.addTextChangedListener(new TextWatcher() {

            private String oldText = null;

            public void afterTextChanged(Editable s) {
                int length = s.length();

                // trim all whitespaces from right
                int end = length;
                while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
                    end--;
                }
                // keep last whitespace after char if possible
                if (end > 0 && end < length) {
                    end++;
                }

                // trim all whitespaces from left
                int start = 0;
                while (start < end && Character.isWhitespace(s.charAt(start))) {
                    start++;
                }

                if (start > 0 || end < length) {
                    s.replace(0, length, s.subSequence(start, end));
                } else {
                    // compare with text from before change and update search records if necessary
                    String text = s.toString().trim();
                    if (!text.equals(oldText) || text.isEmpty()) {
                        if (isViewingAllApps()) {
                            displayKissBar(false, false);
                        }
                        updateSearchRecords(false, text);
                        displayClearOnInput();
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // remember text before change
                oldText = s.toString().trim();
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


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
                (BottomPullEffectView) this.findViewById(R.id.listEdgeEffect)
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
            if ((android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM)
                || (getPrivateUser() == null)) {
                privateSpaceItem.setVisible(false);
            } else if (isPrivateSpaceUnlocked()) {
                privateSpaceItem.setTitle("Lock Private Space");
            } else {
                privateSpaceItem.setTitle("Unlock Private Space");
            }
        }

        forwarderManager.onCreateContextMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
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
    @SuppressLint("CommitPrefEdits")
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

        if (KissApplication.getApplication(this).getDataHandler().allProvidersHaveLoaded) {
            displayLoader(false);
            onFavoriteChange();
        }

        // We need to update the history in case an external event created new items
        // (for instance, installed a new app, got a phone call or simply clicked on a favorite)
        updateSearchRecords();
        displayClearOnInput();

        if (isViewingAllApps()) {
            displayKissBar(false);
        }

        forwarderManager.onResume();

        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        forwarderManager.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        forwarderManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.mReceiver);
        forwarderManager.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
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

    @Override
    public void onBackPressed() {
        if (mPopup != null) {
            mPopup.dismiss();
        } else if (isViewingAllApps()) {
            displayKissBar(false);
        } else {
            // If no kissmenu, empty the search bar
            // (this will trigger a new event if the search bar was already empty)
            // (which means pressing back in minimalistic mode with history displayed
            // will hide history again)
            clearSearchText();
        }

        // Calling super.onBackPressed() will quit the launcher, only do this if KISS is not the user's default home.
        if (!isKissDefaultLauncher()) {
            super.onBackPressed();
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
        if (keycode != KeyEvent.KEYCODE_BACK) {
            searchEditText.requestFocus();
            searchEditText.dispatchKeyEvent(e);
        }
        return super.onKeyDown(keycode, e);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
    public void onMenuButtonClicked(View menuButton) {
        // When the kiss bar is displayed, the button can still be clicked in a few areas (due to favorite margin)
        // To fix this, we discard any click event occurring when the kissbar is displayed
        if (!isViewingSearchResults()) {
            return;
        }
        if (!forwarderManager.onMenuButtonClicked(this.menuButton)) {
            this.menuButton.showContextMenu();
            this.menuButton.performHapticFeedback(LONG_PRESS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        forwarderManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
     * Clear text content when touching the cross button
     */
    @SuppressWarnings("UnusedParameters")
    public void onClearButtonClicked(View clearButton) {
        clearSearchText();
    }

    /**
     * Display KISS menu
     */
    public void onLauncherButtonClicked(View launcherButton) {
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
        if (searchEditText.getText().length() > 0) {
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

                int animationDuration = getResources().getInteger(
                        android.R.integer.config_longAnimTime);

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

    @RequiresApi(35)
    private UserHandle getPrivateUser() {
        final UserManager manager = (UserManager) this.getSystemService(Context.USER_SERVICE);
        assert manager != null;

        final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcher != null;

        List<UserHandle> users = launcher.getProfiles();

        UserHandle privateUser = null;
        for (UserHandle user : users) {
            if (Objects.requireNonNull(launcher.getLauncherUserInfo(user)).getUserType().equalsIgnoreCase(UserManager.USER_TYPE_PROFILE_PRIVATE)) {
                privateUser = user;
                break;
            }
        }
        return privateUser;
    }

    private boolean isPrivateSpaceUnlocked() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return false;
        }

        final UserManager manager = (UserManager) this.getSystemService(Context.USER_SERVICE);
        assert manager != null;

        UserHandle user = getPrivateUser();
        return !manager.isQuietModeEnabled(user);
    }

    @RequiresApi(35)
    private void switchPrivateSpaceState() {
        final UserManager manager = (UserManager) this.getSystemService(Context.USER_SERVICE);
        assert manager != null;

        UserHandle user = getPrivateUser();
        manager.requestQuietModeEnabled(!manager.isQuietModeEnabled(user), user);
    }

    @RequiresApi(35)
    private void privateSpaceStateEvent(UserHandle handle) {
        if (handle == null) {
            return;
        }

        final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        LauncherUserInfo info = launcher.getLauncherUserInfo(handle);
        if (info != null) {
            if (info.getUserType().equalsIgnoreCase(UserManager.USER_TYPE_PROFILE_PRIVATE)) {
                Log.d(TAG, "Private Space state changed");
                // TODO: Check if private space state changed and change app view accordingly
            }
        }
    }

    public void onFavoriteChange() {
        forwarderManager.onFavoriteChange();
    }

    public void displayKissBar(boolean display) {
        this.displayKissBar(display, true);
    }

    private void displayKissBar(boolean display, boolean clearSearchText) {
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
            resetTask();

            // Needs to be done after setting the text content to empty
            isDisplayingKissBar = true;

            runTask(new ApplicationsSearcher(MainActivity.this, false));

            // Reveal the bar
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int animationDuration = getResources().getInteger(
                        android.R.integer.config_shortAnimTime);

                Animator anim = ViewAnimationUtils.createCircularReveal(kissBar, cx, cy, 0, finalRadius);
                anim.setDuration(animationDuration);
                anim.start();
            }
            kissBar.setVisibility(View.VISIBLE);

            // Display the alphabet on the scrollbar (#926)
            list.setFastScrollEnabled(true);
        } else {
            isDisplayingKissBar = false;
            // Hide the bar
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int animationDuration = getResources().getInteger(
                        android.R.integer.config_shortAnimTime);

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
            } else {
                // No animation before Lollipop
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
    private void updateSearchRecords(boolean isRefresh, String query) {
        resetTask();
        dismissPopup();

        if (isRefresh && isViewingAllApps()) {
            // Refreshing while viewing all apps (for instance app installed or uninstalled in the background)
            runTask(new ApplicationsSearcher(this, isRefresh));
            return;
        }

        forwarderManager.updateSearchRecords(isRefresh, query);

        if (query.isEmpty()) {
            systemUiVisibilityHelper.resetScroll();
        } else {
            runTask(new QuerySearcher(this, query, isRefresh));
        }
    }

    public void runTask(Searcher task) {
        resetTask();
        searchTask = task;
        searchTask.executeOnExecutor(Searcher.SEARCH_THREAD);
    }

    public void resetTask() {
        if (searchTask != null) {
            searchTask.cancel(true);
            searchTask = null;
        }
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
        list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
        // Add a message to be processed after all current messages, to reset transcript mode to default
        list.post(() -> list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL));
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
        dialog.show(getFragmentManager(), "dialog");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        systemUiVisibilityHelper.onWindowFocusChanged(hasFocus);
        forwarderManager.onWindowFocusChanged(hasFocus);
    }


    public void showKeyboard() {
        searchEditText.requestFocus();
        searchEditText.setCursorVisible(true);
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;
        mgr.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);

        systemUiVisibilityHelper.onKeyboardVisibilityChanged(true);
    }

    @Override
    public void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            //noinspection ConstantConditions
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        systemUiVisibilityHelper.onKeyboardVisibilityChanged(false);
        dismissPopup();

        searchEditText.setCursorVisible(false);
        searchEditText.clearFocus();
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
        runTask(new TagsSearcher(this, tag));

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }

    public void showUntagged() {
        runTask(new UntaggedSearcher(this));

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }

    public void showHistory() {
        runTask(new HistorySearcher(this, false));

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
}
