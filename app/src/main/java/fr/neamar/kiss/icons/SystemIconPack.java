package fr.neamar.kiss.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import fr.neamar.kiss.ui.GoogleCalendarIcon;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.IconShape;
import fr.neamar.kiss.utils.PackageManagerUtils;
import fr.neamar.kiss.utils.UserHandle;

public class SystemIconPack implements IconPack<Void> {

    private final String packageName;
    private IconShape mAdaptiveShape = IconShape.SHAPE_SYSTEM;

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

    @NonNull
    public IconShape getAdaptiveShape() {
        return mAdaptiveShape;
    }

    public void setAdaptiveShape(@NonNull IconShape shape) {
        mAdaptiveShape = shape;
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        if (componentName.getPackageName().equals(GoogleCalendarIcon.GOOGLE_CALENDAR)) {
            Drawable drawable = GoogleCalendarIcon.getDrawable(ctx, componentName.getClassName());
            if (drawable != null)
                return drawable;
        }

        return PackageManagerUtils.getActivityIcon(ctx, componentName, userHandle);
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
