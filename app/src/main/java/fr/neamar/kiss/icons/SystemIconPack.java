package fr.neamar.kiss.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import fr.neamar.kiss.ui.GoogleCalendarIcon;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.UserHandle;

public class SystemIconPack implements IconPack<Void> {

    private static final String TAG = SystemIconPack.class.getSimpleName();
    private final String packageName;
    private int mAdaptiveShape = DrawableUtils.SHAPE_SYSTEM;

    public SystemIconPack(String packageName) {
        this.packageName = packageName;
    }

    public SystemIconPack() {
        this("default");
    }

    @NonNull
    @Override
    public String getPackPackageName() {
        return packageName;
    }

    @Override
    public void load(PackageManager packageManager) {
    }

    public int getAdaptiveShape() {
        return mAdaptiveShape;
    }

    public void setAdaptiveShape(int shape) {
        mAdaptiveShape = shape;
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName) {
        Drawable drawable = null;

        if (componentName.getPackageName().equals(GoogleCalendarIcon.GOOGLE_CALENDAR)) {
            drawable = GoogleCalendarIcon.getDrawable(ctx, componentName.getClassName());
            if (drawable != null)
                return drawable;
        }
        try {
            drawable = ctx.getPackageManager().getActivityIcon(componentName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find component " + componentName.toShortString(), e);
        }

        // This should never happen, let's just return the application icon
        if (drawable == null) {
            try {
                drawable = ctx.getPackageManager().getApplicationIcon(componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to find package " + componentName.getPackageName(), e);
            }
        }

        // This should never happen, let's just return the generic activity icon
        if (drawable == null)
            drawable = ctx.getPackageManager().getDefaultActivityIcon();

        return drawable;
    }

    @NonNull
    @Override
    public Drawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable icon, boolean fitInside, @ColorInt int backgroundColor) {
        return DrawableUtils.applyIconMaskShape(ctx, icon, mAdaptiveShape, fitInside, backgroundColor);
    }

    @Nullable
    @Override
    public Collection<Void> getDrawableList() {
        return null;
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull Void aVoid) {
        return null;
    }
}
