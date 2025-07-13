package fr.neamar.kiss;

import static android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import fr.neamar.kiss.forwarder.ExperienceTweaks;
import fr.neamar.kiss.forwarder.InterfaceTweaks;
import fr.neamar.kiss.utils.UserHandle;
import fr.neamar.kiss.utils.Utilities;

public class PickAppWidgetActivity extends Activity {
    private static final String TAG = PickAppWidgetActivity.class.getSimpleName();
    public static final String EXTRA_WIDGET_BIND_ALLOWED = "widgetBindAllowed";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Lock launcher into portrait mode
        // Do it here to make the transition as smooth as possible
        ExperienceTweaks.setRequestedOrientation(this, prefs);

        InterfaceTweaks.applySettingsTheme(this, prefs);
        setContentView(R.layout.widget_picker);

        View progressContainer = findViewById(R.id.progressContainer);
        ListView listView = findViewById(android.R.id.list);

        progressContainer.setVisibility(View.VISIBLE);

        final Context context = getApplicationContext();
        final WidgetListAdapter adapter = new WidgetListAdapter();
        listView.setAdapter(adapter);
        final List<MenuItem> adapterList = new ArrayList<>();
        Utilities.runAsync(t -> {
            // get widget list
            List<WidgetInfo> widgetList = getWidgetList(context);

            // sort list
            Collections.sort(widgetList, (o1, o2) -> {
                return o1.appName.compareTo(o2.appName);
            });

            // assuming the list is sorted by apps, add titles with app name
            String lastApp = null;
            for (WidgetInfo item : widgetList) {
                if (!item.appName.equals(lastApp)) {
                    lastApp = item.appName;
                    adapterList.add(new ItemTitle(item.appName));
                }
                adapterList.add(new ItemWidget(item));
            }
        }, t -> {
            progressContainer.setVisibility(View.GONE);
            adapter.setItems(adapterList);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getAdapter().getItem(position);
            WidgetInfo info = null;
            if (item instanceof ItemWidget)
                info = ((ItemWidget) item).getInfo();
            if (info == null)
                return;
            Intent intent = getIntent();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
            if (appWidgetId != 0) {
                boolean bindAllowed = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.appWidgetInfo.getProfile(), info.appWidgetInfo.provider, null);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.appWidgetInfo.provider);
                }

                intent.putExtra(EXTRA_WIDGET_BIND_ALLOWED, bindAllowed);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.appWidgetInfo.provider);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.appWidgetInfo.getProfile());
                }
                setResult(RESULT_OK, intent);
            } else {
                setResult(RESULT_CANCELED, intent);
            }
            finish();
        });
    }

    @WorkerThread
    private static List<WidgetInfo> getWidgetList(@NonNull Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        List<AppWidgetProviderInfo> installedProviders = appWidgetManager.getInstalledProviders();
        List<WidgetInfo> infoList = new ArrayList<>(installedProviders.size());
        PackageManager packageManager = context.getPackageManager();
        for (AppWidgetProviderInfo providerInfo : installedProviders) {
            if (!isHiddenFromPicker(providerInfo)) {
                // get widget name
                String label = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    label = providerInfo.loadLabel(packageManager);
                }
                if (label == null) {
                    label = providerInfo.label;
                }

                // get widget description
                String description = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    CharSequence desc = providerInfo.loadDescription(context);
                    if (desc != null)
                        description = desc.toString();
                }

                String appName = providerInfo.provider.getPackageName();
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(providerInfo.provider.getPackageName(), 0);
                    appName = appInfo.loadLabel(packageManager).toString();
                } catch (Exception e) {
                    Log.e(TAG, "get `" + providerInfo.provider.getPackageName() + "` label");
                }
                infoList.add(new WidgetInfo(appName, label, description, providerInfo));
            }
        }
        return infoList;
    }

    private static boolean isHiddenFromPicker(AppWidgetProviderInfo providerInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (providerInfo.widgetFeatures & WIDGET_FEATURE_HIDE_FROM_PICKER) != 0;
        }
        return false;
    }

    @WorkerThread
    protected static Drawable getWidgetPreview(@NonNull Context context, @NonNull AppWidgetProviderInfo info) {
        Drawable preview = null;
        final int density = context.getResources().getDisplayMetrics().densityDpi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preview = info.loadPreviewImage(context, density);
        }
        if (preview != null)
            return preview;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preview = info.loadIcon(context, density);
        }
        if (preview != null)
            return preview;

        Resources resources = null;
        try {
            resources = context.getPackageManager().getResourcesForApplication(info.provider.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "getResourcesForApplication " + info.provider.getPackageName(), e);
        }
        if (resources != null) {
            try {
                preview = resources.getDrawableForDensity(info.previewImage, density);
            } catch (Resources.NotFoundException ignored) {
                //ignored
            }
            if (preview != null)
                return preview;

            try {
                preview = resources.getDrawableForDensity(info.icon, density);
            } catch (Resources.NotFoundException ignored) {
                //ignored
            }
            if (preview != null)
                return preview;
        }

        final UserHandle userHandle;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            userHandle = new UserHandle(context, info.getProfile());
        } else
            userHandle = new UserHandle();
        return KissApplication.getApplication(context).getIconsHandler().getDrawableIconForPackage(info.provider, userHandle);
    }

    private static class WidgetInfo {
        final String appName;
        final String widgetName;
        final String widgetDesc;
        final AppWidgetProviderInfo appWidgetInfo;
        int cachedIconWidth = 0;
        int cachedIconHeight = 0;

        protected WidgetInfo(String app, String name, String description, AppWidgetProviderInfo appWidgetInfo) {
            this.appName = app;
            this.widgetName = name;
            this.widgetDesc = description;
            this.appWidgetInfo = appWidgetInfo;
        }
    }

    public interface MenuItem {
        @NonNull
        String getName();
    }

    private static class ItemTitle implements MenuItem {
        @NonNull
        private final String name;

        protected ItemTitle(@NonNull String string) {
            this.name = string;
        }

        @NonNull
        @Override
        public String getName() {
            return name;
        }
    }

    private static class ItemWidget implements MenuItem {
        private final WidgetInfo info;

        protected ItemWidget(@NonNull WidgetInfo info) {
            this.info = info;
        }

        @NonNull
        @Override
        public String getName() {
            return info.widgetName;
        }

        public WidgetInfo getInfo() {
            return info;
        }
    }

    private static class WidgetListAdapter extends BaseAdapter {
        private final List<MenuItem> mList = new ArrayList<>(0);

        protected WidgetListAdapter() {
            super();
        }

        public void setItems(Collection<MenuItem> list) {
            mList.clear();
            mList.addAll(list);
            notifyDataSetChanged();
        }

        @Override
        public boolean isEnabled(int position) {
            return !(getItem(position) instanceof ItemTitle);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public MenuItem getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof ItemTitle ? 1 : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            ViewHolder holder = null;
            final int viewType = getItemViewType(position);
            if (convertView != null && convertView.getTag() instanceof ViewHolder)
                holder = (ViewHolder) convertView.getTag();
            if (holder != null && holder.getViewType() == viewType) {
                view = convertView;
            } else {
                int layout = R.layout.widget_picker_item;
                if (viewType == 1)
                    layout = R.layout.widget_picker_item_title;
                view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
                holder = new ViewHolder(viewType, view);
            }

            MenuItem content = getItem(position);
            holder.setContent(content);

            return view;
        }
    }

    public static class ViewHolder {
        private final int mViewType;
        private final TextView textView;
        private Utilities.AsyncRun task = null;
        private Drawable mDrawable = null;

        protected ViewHolder(int viewType, View itemView) {
            itemView.setTag(this);
            mViewType = viewType;
            textView = itemView.findViewById(android.R.id.text1);
        }

        public int getViewType() {
            return mViewType;
        }

        public void setContent(MenuItem content) {
            if (task != null) {
                task.cancel();
                task = null;
            }
            CharSequence text = content.getName();
            if (content instanceof ItemWidget) {
                final WidgetInfo widgetInfo = ((ItemWidget) content).getInfo();
                // set cached icon size to help ListView scrolling
                {
                    int w = widgetInfo.cachedIconWidth;
                    int h = widgetInfo.cachedIconHeight;
                    Drawable icon = new ColorDrawable(0);
                    icon.setBounds(0, 0, w, h);
                    textView.setCompoundDrawables(null, icon, null, null);
                }
                task = Utilities.runAsync(t -> {
                    Drawable icon = PickAppWidgetActivity.getWidgetPreview(textView.getContext(), widgetInfo.appWidgetInfo);
                    synchronized (ViewHolder.this) {
                        mDrawable = icon;
                    }
                }, t -> {
                    if (t.isCancelled())
                        return;
                    final Drawable icon;
                    synchronized (ViewHolder.this) {
                        icon = mDrawable != null ? mDrawable : new ColorDrawable(0);
                    }
                    int w = icon.getIntrinsicWidth();
                    int h = icon.getIntrinsicHeight();
                    float aspect = (w > 0 && h > 0) ? (w / (float) h) : 1f;
                    int minHeight = textView.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
                    if (h < minHeight) {
                        h = minHeight;
                        w = Math.round(h * aspect);
                    }
                    int maxWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
                    if (w > maxWidth) {
                        w = maxWidth;
                        h = Math.round(w / aspect);
                    }
                    widgetInfo.cachedIconWidth = w;
                    widgetInfo.cachedIconHeight = h;
                    icon.setBounds(0, 0, w, h);
                    textView.setCompoundDrawables(null, icon, null, null);
                });

                String description = ((ItemWidget) content).getInfo().widgetDesc;
                if (!TextUtils.isEmpty(description))
                    text = content.getName() + "\n" + description;
            }

            textView.setText(text);
        }
    }
}
