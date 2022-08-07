package fr.neamar.kiss;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
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
import android.view.Gravity;
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

import fr.neamar.kiss.forwarder.InterfaceTweaks;
import fr.neamar.kiss.utils.UserHandle;
import fr.neamar.kiss.utils.Utilities;

public class PickAppWidgetActivity extends Activity {
    private static final String TAG = "PickAppWidget";
    public static final String EXTRA_WIDGET_BIND_ALLOWED = "widgetBindAllowed";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InterfaceTweaks.applyTheme(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.widget_picker);
        UIColors.updateThemePrimaryColor(this);

        View progressContainer = findViewById(R.id.progressContainer);
        ListView listView = findViewById(android.R.id.list);

        progressContainer.setVisibility(View.VISIBLE);

        final Context context = getApplicationContext();
        final WidgetListAdapter adapter = new WidgetListAdapter();
        listView.setAdapter(adapter);
        final ArrayList<MenuItem> adapterList = new ArrayList<>();
        Utilities.runAsync(t -> {
            // get widget list
            ArrayList<WidgetInfo> widgetList = getWidgetList(context);

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
                info = ((ItemWidget) item).info;
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
    private static ArrayList<WidgetInfo> getWidgetList(@NonNull Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        List<AppWidgetProviderInfo> installedProviders = appWidgetManager.getInstalledProviders();
        ArrayList<WidgetInfo> infoArrayList = new ArrayList<>(installedProviders.size());
        PackageManager packageManager = context.getPackageManager();
        for (AppWidgetProviderInfo providerInfo : installedProviders) {
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
            infoArrayList.add(new WidgetInfo(appName, label, description, providerInfo));
        }
        return infoArrayList;
    }

    @WorkerThread
    private static Drawable getWidgetPreview(@NonNull Context context, @NonNull AppWidgetProviderInfo info) {
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

        private WidgetInfo(String app, String name, String description, AppWidgetProviderInfo appWidgetInfo) {
            this.appName = app;
            this.widgetName = name;
            this.widgetDesc = description;
            this.appWidgetInfo = appWidgetInfo;
        }
    }

    private interface MenuItem {
        @NonNull
        String getName();
    }

    private static class ItemTitle implements MenuItem {
        @NonNull
        private final String name;

        private ItemTitle(@NonNull String string) {
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

        public ItemWidget(@NonNull WidgetInfo info) {
            this.info = info;
        }

        @NonNull
        @Override
        public String getName() {
            return info.widgetName;
        }
    }

    private static class WidgetListAdapter extends BaseAdapter {
        private final ArrayList<MenuItem> mList = new ArrayList<>(0);

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
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget_picker_item, parent, false);
            } else {
                view = convertView;
            }
            ViewHolder holder = view.getTag() instanceof ViewHolder ? (ViewHolder) view.getTag() : new ViewHolder(view);

            MenuItem content = getItem(position);
            holder.setContent(content);

            return view;
        }
    }

    private static class ViewHolder {
        private final TextView textView;
        private Utilities.AsyncRun task = null;
        private Drawable mDrawable = null;

        ViewHolder(View itemView) {
            itemView.setTag(this);
            textView = itemView.findViewById(android.R.id.text1);
        }

        public void setContent(MenuItem content) {
            if (task != null) {
                task.cancel();
                task = null;
            }
            if (content instanceof ItemWidget) {
                final AppWidgetProviderInfo info = ((ItemWidget) content).info.appWidgetInfo;
                task = Utilities.runAsync(t -> {
                    Drawable icon = getWidgetPreview(textView.getContext(), info);
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
                    int size = textView.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
                    if (size < h)
                        size = h;
                    icon.setBounds(0, 0, Math.round(size * aspect), size);
                    textView.setCompoundDrawables(null, icon, null, null);
                });
                String description = ((ItemWidget) content).info.widgetDesc;
                if (!TextUtils.isEmpty(description))
                    textView.setText(content.getName() + "\n" + description);
                else
                    textView.setText(content.getName());
                textView.setGravity(Gravity.CENTER);
            } else {
                textView.setCompoundDrawables(null, null, null, null);
                textView.setText(content.getName());
                textView.setGravity(Gravity.START);
            }
        }
    }
}
