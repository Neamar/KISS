package fr.neamar.kiss.forwarder;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.ui.WidgetHost;

class Widgets extends Forwarder {
    private static final int REQUEST_APPWIDGET_PICKED = 9;
    private static final int REQUEST_APPWIDGET_CONFIGURED = 5;

    private static final int APPWIDGET_HOST_ID = 442;

    private static final String WIDGET_PREF_KEY = "widgets-conf";

    /**
     * Widgets fields
     */
    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    /**
     * View widgets are added to
     */
    private ViewGroup widgetArea;

    Widgets(MainActivity mainActivity) {
        super(mainActivity);
    }

    void onCreate() {
        // Initialize widget manager and host, restore widgets
        mAppWidgetManager = AppWidgetManager.getInstance(mainActivity);
        mAppWidgetHost = new WidgetHost(mainActivity, APPWIDGET_HOST_ID);
        widgetArea = mainActivity.findViewById(R.id.widgetLayout);

        restoreWidgets();

        // Start listening for widget update
        mAppWidgetHost.startListening();
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_APPWIDGET_CONFIGURED:
                    Log.i("Widgets", "Widget configured");
                    break;
                case REQUEST_APPWIDGET_PICKED:
                    configureAppWidget(data);
                    break;
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null && (requestCode == REQUEST_APPWIDGET_CONFIGURED || requestCode == REQUEST_APPWIDGET_PICKED)) {
            // if widget was not selected, delete it
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                // find widget views for appWidgetId
                List<View> viewsToRemove = new ArrayList<>();
                for (int i = 0; i < widgetArea.getChildCount(); i++) {
                    AppWidgetHostView view = (AppWidgetHostView) widgetArea.getChildAt(i);
                    if (view.getAppWidgetId() == appWidgetId) {
                        viewsToRemove.add(view);
                    }
                }
                // remove view
                for (View viewToRemove : viewsToRemove) {
                    widgetArea.removeView(viewToRemove);
                }
                // delete widget id
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_widget) {
            // request widget picker, a selection will lead to a call of onActivityResult
            int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
            Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            mainActivity.startActivityForResult(pickIntent, REQUEST_APPWIDGET_PICKED);
            return true;
        }

        return false;
    }

    void onCreateContextMenu(ContextMenu menu) {
        if (!prefs.getBoolean("history-hide", false)) {
            menu.findItem(R.id.add_widget).setVisible(false);
        }
    }

    void onDataSetChanged() {
        if (widgetArea.getChildCount() > 0 && mainActivity.adapter.isEmpty()) {
            // when a widget is displayed the empty list would prevent touches on the widget
            mainActivity.emptyListView.setVisibility(View.GONE);
        }
    }

    private void serializeState() {
        List<String> builder = new ArrayList<>(widgetArea.getChildCount());
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            AppWidgetHostView view = (AppWidgetHostView) widgetArea.getChildAt(i);
            int appWidgetId = view.getAppWidgetId();
            int lineSize = getLineSize(view);
            builder.add(appWidgetId + "-" + lineSize);
        }

        String pref = TextUtils.join(";", builder);
        prefs.edit().putString(WIDGET_PREF_KEY, pref).apply();
    }

    /**
     * Display all widgets based on state
     */
    @SuppressWarnings("StringSplitter")
    private void restoreWidgets() {
        // only add widgets if in minimal mode
        if (!prefs.getBoolean("history-hide", false)) {
            return;
        }

        // remove empty list view when using widgets, this would block touches on the widget
        mainActivity.emptyListView.setVisibility(View.GONE);
        widgetArea.removeAllViews();
        String widgetsConfString = prefs.getString(WIDGET_PREF_KEY, "");
        String[] widgetsConf = widgetsConfString.split(";");
        Set<Integer> idsUsed = new HashSet<>();
        for (String widgetConf : widgetsConf) {
            if (widgetConf.isEmpty()) {
                continue;
            }
            String[] conf = widgetConf.split("-");
            int id = Integer.parseInt(conf[0]);
            int lineSize = Integer.parseInt(conf[1]);
            idsUsed.add(id);
            addWidget(id, lineSize);
        }

        // kill zombie widgets
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int[] hostWidgetIds = mAppWidgetHost.getAppWidgetIds();
            for (int hostWidgetId : hostWidgetIds) {
                if (!idsUsed.contains(hostWidgetId)) {
                    mAppWidgetHost.deleteAppWidgetId(hostWidgetId);
                }
            }
        }
    }

    /**
     * Retrieve a view for specified widget,
     * add context menu to it
     *
     * @param appWidgetId id of widget to add
     * @param lineSize    height of widget given in lines
     */
    private void addWidget(int appWidgetId, int lineSize) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null) {
            Log.i("Widget", "Unable to retrieve widget by id " + appWidgetId);
            return;
        }

        AppWidgetHostView hostView = mAppWidgetHost.createView(mainActivity, appWidgetId, appWidgetInfo);

        int height = (int) (lineSize * getLineHeight());
        hostView.setAppWidget(appWidgetId, appWidgetInfo);
        setWidgetSize(hostView, height, appWidgetInfo);

        hostView.setLongClickable(true);
        hostView.setOnLongClickListener(v -> {
            final AppWidgetHostView widgetWithMenuCurrentlyDisplayed = (AppWidgetHostView) v;
            final AppWidgetProviderInfo currentAppWidgetInfo = mAppWidgetManager.getAppWidgetInfo(widgetWithMenuCurrentlyDisplayed.getAppWidgetId());

            PopupMenu popup = new PopupMenu(mainActivity, v);
            popup.inflate(R.menu.menu_widget);

            Menu menu = popup.getMenu();
            // Disable items that can't be triggered
            final ViewGroup parent = (ViewGroup) widgetWithMenuCurrentlyDisplayed.getParent();
            if (parent.indexOfChild(widgetWithMenuCurrentlyDisplayed) == 0) {
                menu.findItem(R.id.move_up).setVisible(false);
            }
            if (parent.indexOfChild(widgetWithMenuCurrentlyDisplayed) == parent.getChildCount() - 1) {
                menu.findItem(R.id.move_down).setVisible(false);
            }
            int decreasedLineHeight = getDecreasedLineHeight(widgetWithMenuCurrentlyDisplayed);
            if (preventResizeWidget(decreasedLineHeight, currentAppWidgetInfo)) {
                menu.findItem(R.id.decrease_size).setVisible(false);
            }
            int increasedLineHeight = getIncreasedLineHeight(widgetWithMenuCurrentlyDisplayed);
            if (preventResizeWidget(increasedLineHeight, currentAppWidgetInfo)) {
                menu.findItem(R.id.increase_size).setVisible(false);
            }

            popup.setOnMenuItemClickListener(item -> {
                popup.dismiss();
                switch (item.getItemId()) {
                    case R.id.remove_widget:
                        parent.removeView(widgetWithMenuCurrentlyDisplayed);
                        mAppWidgetHost.deleteAppWidgetId(widgetWithMenuCurrentlyDisplayed.getAppWidgetId());
                        serializeState();
                        return true;
                    case R.id.increase_size: {
                        int newHeight = getIncreasedLineHeight(widgetWithMenuCurrentlyDisplayed);
                        resizeWidget(widgetWithMenuCurrentlyDisplayed, newHeight);
                        return true;
                    }
                    case R.id.decrease_size: {
                        int newHeight = getDecreasedLineHeight(widgetWithMenuCurrentlyDisplayed);
                        resizeWidget(widgetWithMenuCurrentlyDisplayed, newHeight);
                        return true;
                    }
                    case R.id.move_up: {
                        int currentIndex = parent.indexOfChild(widgetWithMenuCurrentlyDisplayed);
                        if (currentIndex >= 1) {
                            parent.removeViewAt(currentIndex);
                            parent.addView(widgetWithMenuCurrentlyDisplayed, currentIndex - 1);
                            serializeState();
                            return true;
                        }
                        break;
                    }
                    case R.id.move_down: {
                        int currentIndex = parent.indexOfChild(widgetWithMenuCurrentlyDisplayed);
                        if (currentIndex < parent.getChildCount() - 1) {
                            parent.removeViewAt(currentIndex);
                            parent.addView(widgetWithMenuCurrentlyDisplayed, currentIndex + 1);
                            serializeState();
                            return true;
                        }
                        break;
                    }
                }

                return false;
            });

            popup.show();
            return true;
        });

        widgetArea.addView(hostView);
    }

    /**
     * @param hostView host view of widget
     * @return decreased line height of host view
     */
    private int getDecreasedLineHeight(AppWidgetHostView hostView) {
        int lineSize = getLineSize(hostView) - 1;
        return (int) (lineSize * getLineHeight());
    }

    /**
     * @param hostView host view of widget
     * @return increased line height of host view
     */
    private int getIncreasedLineHeight(AppWidgetHostView hostView) {
        int lineSize = getLineSize(hostView) + 1;
        return (int) (lineSize * getLineHeight());
    }

    /**
     * Set new height to host view of widget.
     *
     * @param hostView host view for widget
     * @param height   height of widget
     */
    private void setWidgetSize(AppWidgetHostView hostView, int height, AppWidgetProviderInfo appWidgetInfo) {
        hostView.setMinimumHeight(height);
        hostView.setMinimumWidth(Math.min(appWidgetInfo.minWidth, appWidgetInfo.minResizeWidth));
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        hostView.setLayoutParams(params);
    }

    /**
     * @param hostView host view of widget
     * @param height   new height of widget
     */
    private void resizeWidget(AppWidgetHostView hostView, int height) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(hostView.getAppWidgetId());
        if (preventResizeWidget(height, appWidgetInfo)) {
            return;
        }
        setWidgetSize(hostView, height, appWidgetInfo);
        serializeState();
    }

    /**
     * Check if resize of widget is prevented.
     *
     * @param height        new height of widget
     * @param appWidgetInfo
     * @return true, if widget cannot be resized to given height
     */
    private boolean preventResizeWidget(int height, AppWidgetProviderInfo appWidgetInfo) {
        return height <= 0 || appWidgetInfo == null || height < Math.min(appWidgetInfo.minHeight, appWidgetInfo.minResizeHeight);
    }

    /**
     * Adds widget to Activity and persists it in prefs to be able to restore it
     *
     * @param data Intent holding widget id to add
     */
    private void addAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        // calculate already used lines
        int usedLines = 0;
        for (int i = 0; i < widgetArea.getChildCount(); i++) {
            View view = widgetArea.getChildAt(i);
            usedLines += getLineSize(view);
        }
        // calculate max available lines
        int maxVisibleLines = (int) Math.ceil(widgetArea.getHeight() / getLineHeight());

        // calculate new line size
        float minWidgetHeight = appWidgetInfo.minHeight;
        float lineHeight = getLineHeight();
        int lineSize = Math.max(1, Math.min(maxVisibleLines - usedLines, (int) Math.ceil(minWidgetHeight / lineHeight)));

        addWidget(appWidgetId, lineSize);

        serializeState();
    }

    /**
     * Check if widget needs configuration and display configuration view if necessary,
     * otherwise just add the widget
     *
     * @param data Intent holding widget id to configure
     */
    private void configureAppWidget(Intent data) {
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        // Add the widget
        addAppWidget(data);

        if (appWidgetInfo.configure != null) {
            // Launch over to configure widget, if needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mAppWidgetHost.startAppWidgetConfigureActivityForResult(mainActivity, appWidgetId, 0, REQUEST_APPWIDGET_CONFIGURED, null);
            }
            else {
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidgetInfo.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                try {
                    mainActivity.startActivityForResult(intent, REQUEST_APPWIDGET_CONFIGURED);
                } catch(SecurityException e) {
                    Toast.makeText(mainActivity,  "KISS doesn't have permission to setup this widget. Believe this is a bug? Please open an issue at https://github.com/Neamar/KISS/issues", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * @param view
     * @return calculated line size of given view
     */
    private int getLineSize(View view) {
        return Math.round(view.getLayoutParams().height / getLineHeight());
    }

    /**
     * @return line height in pixel
     */
    private float getLineHeight() {
        float dip = 50f;
        Resources r = mainActivity.getResources();

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    public void onDestroy() {
        mAppWidgetHost.stopListening();
    }
}
