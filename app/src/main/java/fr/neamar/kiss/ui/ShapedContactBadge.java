package fr.neamar.kiss.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.utils.DrawableUtils;

public class ShapedContactBadge extends QuickContactBadge {

    public ShapedContactBadge(Context context) {
        super(context);
    }

    public ShapedContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShapedContactBadge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        if (drawable != null) {
            drawable = DrawableUtils.getThemedDrawable(getContext(), drawable);
            drawable = getShapedDrawable(getContext(), drawable);
        }
        super.setImageDrawable(drawable);
    }

    public static Drawable getShapedDrawable(@NonNull Context context, @NonNull Drawable drawable) {
        return KissApplication.getApplication(context).getIconsHandler().applyContactMask(context, drawable);
    }
}
