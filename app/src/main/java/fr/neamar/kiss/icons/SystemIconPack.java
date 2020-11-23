package fr.neamar.kiss.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

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
    public Drawable getComponentDrawable(String componentName) {
        return null;
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        Drawable drawable = null;

        if (componentName.getPackageName().equals(GoogleCalendarIcon.GOOGLE_CALENDAR)) {
            drawable = GoogleCalendarIcon.getDrawable(ctx, componentName.getClassName());
            if (drawable != null)
                return drawable;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
                for (LauncherActivityInfo info : icons) {
                    if (info.getComponentName().equals(componentName)) {
                        drawable = info.getBadgedIcon(0);
                        break;
                    }
                }

                // This should never happen, let's just return the first icon
                if (drawable == null)
                    drawable = icons.get(0).getBadgedIcon(0);
            } else {
                drawable = ctx.getPackageManager().getActivityIcon(componentName);
            }
        } catch (PackageManager.NameNotFoundException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to find component " + componentName.toString() + e);
        }
        return drawable;
    }

    @NonNull
    @Override
    public Drawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable icon, boolean fitInside) {
        return DrawableUtils.applyIconMaskShape(ctx, icon, mAdaptiveShape, fitInside);
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
