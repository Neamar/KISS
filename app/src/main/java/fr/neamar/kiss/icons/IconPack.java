package fr.neamar.kiss.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import fr.neamar.kiss.utils.UserHandle;

public interface IconPack<DrawableInfo> {

    @NonNull
    String getPackPackageName();

    @Nullable
    Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle);

    @NonNull
    Drawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable icon, boolean fitInside, @ColorInt int backgroundColor);

    @Nullable
    Collection<DrawableInfo> getDrawableList();

    @Nullable
    Drawable getDrawable(@NonNull DrawableInfo drawableInfo);
}
