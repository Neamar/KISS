package fr.neamar.kiss;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.ArrayList;

import fr.neamar.kiss.holder.Holder;
import fr.neamar.kiss.record.Record;
import fr.neamar.kiss.task.UpdateRecords;

public class MainActivity extends ListActivity implements QueryInterface {

    public static String START_LOAD = "fr.neamar.summon.START_LOAD";
    public static String LOAD_OVER = "fr.neamar.summon.LOAD_OVER";
    public static String FULL_LOAD_OVER = "fr.neamar.summon.FULL_LOAD_OVER";
    public static String NB_PROVIDERS = "nb_providers";
    private BroadcastReceiver mReceiver;

    /**
     * Adapter to display records
     */
    public RecordAdapter adapter;

    /**
     * Search text in the view
     */
    private EditText searchEditText;

    /**
     * Task launched on text change
     */
    private UpdateRecords updateRecords;

    /**
     * Store user preferences
     */
    SharedPreferences prefs;

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Initialize UI
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        super.onCreate(savedInstanceState);

        IntentFilter intentFilter = new IntentFilter(START_LOAD);
        IntentFilter intentFilterBis = new IntentFilter(LOAD_OVER);
        IntentFilter intentFilterTer = new IntentFilter(FULL_LOAD_OVER);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(LOAD_OVER)) {
                    updateRecords(searchEditText.getText().toString());
                } else if (intent.getAction().equalsIgnoreCase(FULL_LOAD_OVER)) {
                    displayLoader(true);
                } else if (intent.getAction().equalsIgnoreCase(START_LOAD)) {
                    displayLoader(false);
                }
            }
        };

        this.registerReceiver(mReceiver, intentFilter);
        this.registerReceiver(mReceiver, intentFilterBis);
        this.registerReceiver(mReceiver, intentFilterTer);
        KissApplication.initDataHandler(this);

        // Initialize preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.main);

        // Create adapter for records
        adapter = new RecordAdapter(this, this, R.layout.item_app, new ArrayList<Record>());
        setListAdapter(adapter);

        this.searchEditText = (EditText) findViewById(R.id.searchEditText);

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
                updateRecords(s.toString());
                displayClearOnInput();
            }
        });

        // On validate, launch first record
        searchEditText.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                RecordAdapter adapter = ((RecordAdapter) getListView().getAdapter());

                adapter.onClick(adapter.getCount() - 1, v);

                return true;
            }
        });

        // Clear text content when touching the cross button
        ImageView clearButton = (ImageView) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEditText.setText("");
            }
        });

        // Clear text content when touching the cross button
        ImageView menuButton = (ImageView) findViewById(R.id.menuButton);
        registerForContextMenu(menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.showContextMenu();
            }
        });

        final int[] favsIds = new int[]{R.id.favorite0, R.id.favorite1, R.id.favorite2, R.id.favorite3};
        final int tryToRetrieve = favsIds.length + 2;

        View.OnClickListener favoriteListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Holder holder = KissApplication.getDataHandler(MainActivity.this).getFavorites(MainActivity.this, tryToRetrieve)
                        .get(Integer.parseInt((String) view.getTag()));
                Record record = Record.fromHolder(MainActivity.this, holder);
                record.fastLaunch(MainActivity.this);
            }
        };

        // Register the listener for each buttons
        for (int favid : favsIds) {
            findViewById(favid).setOnClickListener(favoriteListener);
        }

        // Clear text content when touching the cross button
        final ImageView launcherButton = (ImageView) findViewById(R.id.launcherButton);
        launcherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View kissMenu = findViewById(R.id.main_kissbar);
                // get the center for the clipping circle
                int cx = (launcherButton.getLeft() + launcherButton.getRight()) / 2;
                int cy = (launcherButton.getTop() + launcherButton.getBottom()) / 2;

                // get the final radius for the clipping circle
                int finalRadius = Math.max(kissMenu.getWidth(), kissMenu.getHeight());

                // Reveal the bar
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Animator anim =
                            ViewAnimationUtils.createCircularReveal(kissMenu, cx, cy, 0, finalRadius);
                    kissMenu.setVisibility(View.VISIBLE);
                    anim.start();
                } else {
                    // No animation before Lollipop
                    kissMenu.setVisibility(View.VISIBLE);
                }

                // Retrieve favorites. Try to retrieve more, since some favorites may be undisplayable (e.g. search queries)
                ArrayList<Holder> favorites_holder = KissApplication.getDataHandler(MainActivity.this)
                        .getFavorites(MainActivity.this, tryToRetrieve);

                if (favorites_holder.size() == 0) {
                    Toast toast = Toast.makeText(MainActivity.this, getString(R.string.no_favorites), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, 20);
                    toast.show();
                    return;
                }

                // Don't look for items after favIds length, we won't be able to display them
                for (int i = 0; i < Math.min(favsIds.length, favorites_holder.size()); i++) {
                    Holder holder = favorites_holder.get(i);
                    ImageView image = (ImageView) findViewById(favsIds[i]);

                    Record record = Record.fromHolder(MainActivity.this, holder);
                    Drawable drawable = record.getDrawable(MainActivity.this);
                    if (drawable != null)
                        image.setImageDrawable(drawable);
                    image.setVisibility(View.VISIBLE);
                }

                // Hide empty favorites holder (not enough favorites yet)
                for(int i = favorites_holder.size(); i < favsIds.length; i++) {
                    findViewById(favsIds[i]).setVisibility(View.GONE);
                }

                hideKeyboard();
            }
        });

        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View arg1, int pos, long id) {
                ((RecordAdapter) parent.getAdapter()).onLongClick(pos);
                return true;
            }
        });

        // Hide the "X" before the text field, instead displaying the menu button
        displayClearOnInput();

        // Apply effects depending on current Android version
        applyDesignTweaks();
    }

    /**
     * Apply some tweaks to the design, depending on the current SDK version
     */
    public void applyDesignTweaks() {
        final View menuButton = findViewById(R.id.menuButton);
        final View clearButton = findViewById(R.id.clearButton);
        final View launcherButton = findViewById(R.id.launcherButton);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);

            menuButton.setBackgroundResource(outValue.resourceId);
            clearButton.setBackgroundResource(outValue.resourceId);
            launcherButton.setBackgroundResource(outValue.resourceId);
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);

            // Clicking on menu button should display a focused rectangle
            menuButton.setBackgroundResource(outValue.resourceId);
            // Barely visible on the backbutton, since it disappears instant. Can be seen on long click though
            clearButton.setBackgroundResource(outValue.resourceId);
            launcherButton.setBackgroundResource(outValue.resourceId);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        adapter.onClick(position, v);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    /**
     * Empty text field on resume and show keyboard
     */
    protected void onResume() {
        if (prefs.getBoolean("layout-updated", false)) {
            // Restart current activity to refresh view, since some preferences
            // may require using a new UI
            prefs.edit().putBoolean("layout-updated", false).commit();
            Intent i = getApplicationContext().getPackageManager().getLaunchIntentForPackage(
                    getApplicationContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(i);
        }

        updateRecords(searchEditText.getText().toString());
        displayClearOnInput();

        if (prefs.getBoolean("display-keyboard", false)) {
            // Display keyboard
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    searchEditText.requestFocus();
                    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 10);
        } else {
            // Display keyboard
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideKeyboard();
                    searchEditText.clearFocus();
                }
            }, 10);
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister our receiver
        this.unregisterReceiver(this.mReceiver);
    }

    @Override
    public void onBackPressed() {
        // Is the kiss menu visible?
        View kissMenu = findViewById(R.id.main_kissbar);
        if (kissMenu.getVisibility() == View.VISIBLE) {
            kissMenu.setVisibility(View.GONE);
        } else {
            // If no kissmenu, empty the search bar
            searchEditText.setText("");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
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
        inflater.inflate(R.menu.menu_settings, menu);

        return true;
    }

    protected boolean displayClearOnInput() {
        final View clearButton = findViewById(R.id.clearButton);
        final View menuButton = findViewById(R.id.menuButton);
        if (searchEditText.getText().length() > 0) {
            clearButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.INVISIBLE);
            return true;
        } else {
            clearButton.setVisibility(View.INVISIBLE);
            menuButton.setVisibility(View.VISIBLE);
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    protected void displayLoader(Boolean loaded) {
        final View loaderBar = findViewById(R.id.loaderBar);
        final View launcherButton = findViewById(R.id.launcherButton);

        int animationDuration = getResources().getInteger(
                android.R.integer.config_longAnimTime);

        if (loaded) {
            launcherButton.setVisibility(View.VISIBLE);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                // Animate transition from loader to launch button
                launcherButton.setAlpha(0);
                launcherButton.animate()
                        .alpha(1f)
                        .setDuration(animationDuration)
                        .setListener(null);
                loaderBar.animate()
                        .alpha(0f)
                        .setDuration(animationDuration)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                loaderBar.setVisibility(View.GONE);
                                loaderBar.setAlpha(1);
                            }
                        });
            }
        } else {
            launcherButton.setVisibility(View.GONE);
            loaderBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This function gets called on changes. It will ask all the providers for
     * datas
     *
     * @param query
     */

    public void updateRecords(String query) {
        if (updateRecords != null) {
            updateRecords.cancel(true);
        }
        updateRecords = new UpdateRecords(this);
        updateRecords.execute(query);
    }

    public void resetTask() {
        updateRecords = null;
    }

    /**
     * Call this function when we're leaving the activity We can't use
     * onPause(), since it may be called for a configuration change
     */
    public void launchOccured() {
        // We made a choice on the list,
        // now we can cleanup the filter:
        searchEditText.setText("");
    }

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
}
