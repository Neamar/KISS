package fr.neamar.kiss;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;

import fr.neamar.kiss.adapter.RecordAdapter;
import fr.neamar.kiss.broadcast.IncomingCallHandler;
import fr.neamar.kiss.forwarder.ForwarderManager;
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
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.SystemUiVisibilityHelper;

import static android.view.HapticFeedbackConstants.LONG_PRESS;

public class MainActivity extends Activity implements QueryInterface, KeyboardScrollHider.KeyboardHandler, View.OnTouchListener, Searcher.DataObserver {

    public static final String START_LOAD = "fr.neamar.summon.START_LOAD";
    public static final String LOAD_OVER = "fr.neamar.summon.LOAD_OVER";
    public static final String FULL_LOAD_OVER = "fr.neamar.summon.FULL_LOAD_OVER";

    private static final String TAG = "MainActivity";

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
    private KeyboardScrollHider hider;

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
    private View clearButton;

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

    /**
     * Called when the activity is first created.
     */
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
                }

                // New provider might mean new favorites
                onFavoriteChange();
            }
        };

        this.registerReceiver(mReceiver, intentFilterLoad);
        this.registerReceiver(mReceiver, intentFilterLoadOver);
        this.registerReceiver(mReceiver, intentFilterFullLoadOver);

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

        // add history popup touch listener to empty view (prevents on not working there)
        this.emptyListView.setOnTouchListener(this);

        // Create adapter for records
        this.adapter = new RecordAdapter(this, new ArrayList<Result>());
        this.list.setAdapter(this.adapter);

        this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                adapter.onClick(position, v);
            }
        });

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
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isViewingAllApps()) {
                    displayKissBar(false, false);
                }
                String text = s.toString();
                updateSearchRecords(text);
                displayClearOnInput();
            }
        });


        // Fixes bug when dropping onto a textEdit widget which can cause a NPE
        // This fix should be on ALL TextEdit Widgets !!!
        // See : https://stackoverflow.com/a/23483957
        searchEditText.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                return true;
            }
        });


        // On validate, launch first record
        searchEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
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
                RecordAdapter adapter = ((RecordAdapter) list.getAdapter());

                adapter.onClick(adapter.getCount() - 1, v);

                return true;
            }
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
        PackageManagerUtils.enableComponent(this, IncomingCallHandler.class, prefs.getBoolean("enable-phone-history", false));

        // Hide the "X" after the text field, instead displaying the menu button
        displayClearOnInput();

        systemUiVisibilityHelper = new SystemUiVisibilityHelper(this);

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
        forwarderManager.onCreateContextMenu(menu, v, menuInfo);
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This is called when the user press Home again while already browsing MainActivity
        // onResume() will be called right after, hiding the kissbar if any.
        // http://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)
        // Animation can't happen in this method, since the activity is not resumed yet, so they'll happen in the onResume()
        // https://github.com/Neamar/KISS/issues/569
        if (!searchEditText.getText().toString().isEmpty()) {
            Log.i(TAG, "Clearing search field");
            searchEditText.setText("");
        }

        // Hide kissbar when coming back to kiss
        if (isViewingAllApps()) {
            displayKissBar(false);
        }

        // Close the backButton context menu
        closeContextMenu();
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
            searchEditText.setText("");
        }
        // No call to super.onBackPressed(), since this would quit the launcher.
    }

    @Override
    public boolean onKeyDown(int keycode, @NonNull KeyEvent e) {
        if (keycode == KeyEvent.KEYCODE_MENU) {
            // For devices with a physical menu button, we still want to display *our* contextual menu
            menuButton.showContextMenu();
            menuButton.performHapticFeedback(LONG_PRESS);
            return true;
        }

        return super.onKeyDown(keycode, e);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (forwarderManager.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                return true;
            case R.id.wallpaper:
                hideKeyboard();
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, getString(R.string.menu_wallpaper)));
                return true;
            case R.id.preferences:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        forwarderManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (forwarderManager.onTouch(view, event)) {
            return true;
        }

        if (view.getId() == searchEditText.getId()) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                searchEditText.performClick();
            }
        }
        return true;
    }

    /**
     * Clear text content when touching the cross button
     */
    @SuppressWarnings("UnusedParameters")
    public void onClearButtonClicked(View clearButton) {
        searchEditText.setText("");
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
        if (mPopup != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            dismissPopup();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void displayClearOnInput() {
        if (searchEditText.getText().length() > 0) {
            clearButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.INVISIBLE);
        } else {
            clearButton.setVisibility(View.INVISIBLE);
            menuButton.setVisibility(View.VISIBLE);
        }
    }

    public void displayLoader(Boolean display) {
        int animationDuration = getResources().getInteger(
                android.R.integer.config_longAnimTime);

        // Do not display animation if launcher button is already visible
        if (!display && launcherButton.getVisibility() == View.INVISIBLE) {
            launcherButton.setVisibility(View.VISIBLE);

            // Animate transition from loader to launch button
            launcherButton.setAlpha(0);
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
                            loaderSpinner.setAlpha(1);
                        }
                    });
        } else if (display) {
            launcherButton.setVisibility(View.INVISIBLE);
            loaderSpinner.setVisibility(View.VISIBLE);
        }
    }

    public void onFavoriteChange() {
        forwarderManager.onFavoriteChange();
    }

    private void displayKissBar(Boolean display) {
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
            if (searchEditText.getText().length() != 0) {
                searchEditText.setText("");
            }
            resetTask();

            // Needs to be done after setting the text content to empty
            isDisplayingKissBar = true;

            searchTask = new ApplicationsSearcher(MainActivity.this);
            searchTask.executeOnExecutor(Searcher.SEARCH_THREAD);

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
                searchEditText.setText("");
            }

            // Do not display the alphabetical scrollbar (#926)
            // They only make sense when displaying apps alphabetically, not for searching
            list.setFastScrollEnabled(false);
        }

        forwarderManager.onDisplayKissBar(display);
    }

    public void updateSearchRecords() {
        updateSearchRecords(searchEditText.getText().toString());
    }

    /**
     * This function gets called on query changes.
     * It will ask all the providers for data
     * This function is not called for non search-related changes! Have a look at onDataSetChanged() if that's what you're looking for :)
     *
     * @param query the query on which to search
     */
    private void updateSearchRecords(String query) {
        resetTask();
        dismissPopup();

        forwarderManager.updateSearchRecords(query);

        if (query.isEmpty()) {
            systemUiVisibilityHelper.resetScroll();
        } else {
            runTask(new QuerySearcher(this, query));
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
     * Call this function when we're leaving the activity after clicking a search result
     * to clear the search list.
     * We can't use onPause(), since it may be called for a configuration change
     */
    @Override
    public void launchOccurred() {
        // We selected an item on the list,
        // now we can cleanup the filter:
        if (!searchEditText.getText().toString().isEmpty()) {
            searchEditText.setText("");
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
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                MainActivity.this.mPopup = null;
            }
        });
        hider.fixScroll();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        systemUiVisibilityHelper.onWindowFocusChanged(hasFocus);
        forwarderManager.onWindowFocusChanged(hasFocus);
    }


    public void showKeyboard() {
        searchEditText.requestFocus();
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

    @Override
    public void beforeListChange() {
        list.prepareChangeAnim();
    }

    @Override
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
        runTask(new HistorySearcher(this));

        clearButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }
}
