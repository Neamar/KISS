package fr.neamar.kiss;

import static android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.neamar.kiss.forwarder.ExperienceTweaks;
import fr.neamar.kiss.forwarder.InterfaceTweaks;
import fr.neamar.kiss.utils.Log;
import fr.neamar.kiss.utils.UserHandle;
import fr.neamar.kiss.utils.Utilities;

public class PickAppWidgetActivity extends AppCompatActivity {
    private static final String TAG = PickAppWidgetActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Lock launcher into portrait mode
        // Do it here to make the transition as smooth as possible
        ExperienceTweaks.setRequestedOrientation(this, prefs);

        InterfaceTweaks.applyTheme(this, prefs);
        setContentView(R.layout.widget_picker);

        View progressContainer = findViewById(R.id.progressContainer);
        progressContainer.setVisibility(View.VISIBLE);
        RecyclerView widgetView = findViewById(R.id.widgetView);
        final WidgetListAdapter adapter = new WidgetListAdapter();
        widgetView.setAdapter(adapter);
        final Context context = getApplicationContext();
        Utilities.<List<WidgetInfo>>runAsync(t -> {
            // get widget list
            List<WidgetInfo> widgetList = getWidgetList(context);

            // sort list
            Collections.sort(widgetList, Comparator.comparing(o -> o.appName));

            // assuming the list is sorted by apps, add titles with app name
            List<WidgetInfo> adapterList = new ArrayList<>();
            String lastApp = null;
            for (WidgetInfo item : widgetList) {
                if (!item.appName.equals(lastApp)) {
                    lastApp = item.appName;
                    adapterList.add(new WidgetInfo(item.appName, item.appName, null, null));
                }
                adapterList.add(item);
            }
            return adapterList;
        }, (task, adapterList) -> {
            progressContainer.setVisibility(View.GONE);
            adapter.setItems(adapterList);
        });
    }

    private void onWidgetSelected(@NonNull AppWidgetProviderInfo appWidgetInfo) {
        Intent intent = new Intent();
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, appWidgetInfo.getProfile());
        setResult(RESULT_OK, intent);
        finish();
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
                String label = providerInfo.loadLabel(packageManager);
                if (label == null) {
                    label = providerInfo.label;
                }
                if (label == null) {
                    label = providerInfo.provider.flattenToShortString();
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
        final int density = context.getResources().getDisplayMetrics().densityDpi;
        Drawable preview = info.loadPreviewImage(context, density);
        if (preview != null)
            return preview;

        preview = info.loadIcon(context, density);
        if (preview != null)
            return preview;

        final UserHandle userHandle = new UserHandle(context, info.getProfile());
        return KissApplication.getApplication(context).getIconsHandler().getDrawableIconForPackage(context, info.provider, userHandle);
    }

    /**
     * A row of the picker: either a widget, or the title of an app (without appWidgetInfo)
     */
    private static class WidgetInfo {
        final String appName;
        final String widgetName;
        final String widgetDesc;
        @Nullable
        final AppWidgetProviderInfo appWidgetInfo;
        int cachedIconWidth = 0;
        int cachedIconHeight = 0;

        protected WidgetInfo(String app, String name, String description, @Nullable AppWidgetProviderInfo appWidgetInfo) {
            this.appName = app;
            this.widgetName = name;
            this.widgetDesc = description;
            this.appWidgetInfo = appWidgetInfo;
        }
    }

    private class WidgetListAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<WidgetInfo> mList = new ArrayList<>(0);

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = R.layout.widget_picker_item;
            if (viewType == 1)
                layout = R.layout.widget_picker_item_title;
            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WidgetInfo content = mList.get(position);
            holder.setContent(content);
            holder.textView.setOnClickListener(v -> {
                if (content.appWidgetInfo != null) {
                    onWidgetSelected(content.appWidgetInfo);
                }
            });
        }

        public void setItems(Collection<WidgetInfo> list) {
            mList.clear();
            mList.addAll(list);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mList.get(position).appWidgetInfo == null ? 1 : 0;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private Utilities.AsyncRun<Drawable> task = null;

        protected ViewHolder(View itemView) {
            super(itemView);
            itemView.setTag(this);
            textView = itemView.findViewById(android.R.id.text1);
        }

        private void setContent(WidgetInfo widgetInfo) {
            if (task != null) {
                task.cancel();
                task = null;
            }
            CharSequence text = widgetInfo.widgetName;
            if (widgetInfo.appWidgetInfo != null) {
                // set cached icon size to help ListView scrolling
                {
                    int w = widgetInfo.cachedIconWidth;
                    int h = widgetInfo.cachedIconHeight;
                    Drawable icon = new ColorDrawable(Color.TRANSPARENT);
                    icon.setBounds(0, 0, w, h);
                    textView.setCompoundDrawables(null, icon, null, null);
                }
                task = Utilities.runAsync(t -> PickAppWidgetActivity.getWidgetPreview(textView.getContext(), widgetInfo.appWidgetInfo), (t, icon) -> {
                    if (t.isCancelled()) {
                        return;
                    }
                    int w = icon.getIntrinsicWidth();
                    int h = icon.getIntrinsicHeight();
                    float aspect = (w > 0 && h > 0) ? (w / (float) h) : 1f;
                    int minHeight = textView.getResources().getDimensionPixelSize(R.dimen.result_icon_size);
                    if (h < minHeight) {
                        h = minHeight;
                        w = Math.round(h * aspect);
                    }
                    int maxWidth = textView.getWidth() - textView.getPaddingStart() - textView.getPaddingEnd();
                    if (w > maxWidth) {
                        w = maxWidth;
                        h = Math.round(w / aspect);
                    }
                    widgetInfo.cachedIconWidth = w;
                    widgetInfo.cachedIconHeight = h;
                    icon.setBounds(0, 0, w, h);
                    textView.setCompoundDrawables(null, icon, null, null);
                });

                if (!TextUtils.isEmpty(widgetInfo.widgetDesc))
                    text = widgetInfo.widgetName + "\n" + widgetInfo.widgetDesc;
            }

            textView.setText(text);
        }
    }
}
