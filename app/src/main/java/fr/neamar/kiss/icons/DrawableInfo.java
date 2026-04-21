package fr.neamar.kiss.icons;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface DrawableInfo {

    @Nullable
    Drawable getDrawable(Context context, @NonNull Resources resources, @NonNull String iconPackPackageName);

    @Nullable
    String getTextForSearch();

}
