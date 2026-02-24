package fr.neamar.kiss.result;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import fr.neamar.kiss.IconsHandler;
import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;
import fr.neamar.kiss.pojo.TagDummyPojo;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public class TagDummyResult extends Result<TagDummyPojo> {
    private static volatile Drawable gBackground = null;

    private volatile Drawable icon = null;

    TagDummyResult(@NonNull TagDummyPojo pojo) {
        super(pojo);
    }

    private Drawable getShape(Context context) {
        if (gBackground == null) {
            synchronized (TagDummyResult.class) {
                if (gBackground == null) {
                    IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
                    gBackground = iconsHandler.getBackgroundDrawable(getBackgroundColor(context));
                }
            }
        }

        return gBackground;
    }

    private boolean isShapeCached() {
        return gBackground != null;
    }

    public static void resetShape() {
        gBackground = null;
    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_search, parent);

        ImageView image = view.findViewById(R.id.item_search_icon);
        TextView searchText = view.findViewById(R.id.item_search_text);

        this.setAsyncDrawable(image);
        searchText.setText(pojo.getName());

        image.setColorFilter(getThemeFillColor(context), PorterDuff.Mode.SRC_IN);
        return view;
    }

    @NonNull
    @Override
    public View inflateFavorite(@NonNull Context context, @NonNull ViewGroup parent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("pref-fav-tags-drawable", false)) {
            return super.inflateFavorite(context, parent);
        } else {
            View favoriteView = LayoutInflater.from(context).inflate(R.layout.favorite_tag, parent, false);
            ImageView favoriteIcon = favoriteView.findViewById(android.R.id.background);
            setAsyncDrawable(favoriteIcon, R.drawable.ic_launcher_white, true, this::isShapeCached, this::getShape, drawable -> {});

            boolean largeSearchBar = sharedPreferences.getBoolean("large-search-bar", false);
            int barSize = context.getResources().getDimensionPixelSize(largeSearchBar ? R.dimen.large_bar_height : R.dimen.bar_height);
            int codepoint = pojo.getName().codePointAt(0);
            String glyph = new String(Character.toChars(codepoint));

            TextView favoriteText = favoriteView.findViewById(android.R.id.text1);
            favoriteText.setVisibility(View.VISIBLE);
            favoriteText.setTextColor(getTextColor(context));
            favoriteText.setText(glyph);
            favoriteText.setTextSize(TypedValue.COMPLEX_UNIT_PX, barSize / 2.f);

            favoriteView.setContentDescription(pojo.getName());
            return favoriteView;
        }
    }

    @Override
    public Drawable getDrawable(Context context) {
        if (icon == null) {
            synchronized (this) {
                if (icon == null) {
                    IconsHandler iconsHandler = KissApplication.getApplication(context).getIconsHandler();
                    icon = iconsHandler.getDrawableIconForCodepoint(pojo.getName().codePointAt(0), getTextColor(context), getBackgroundColor(context));
                }
            }
        }
        return icon;
    }

    @Override
    boolean isDrawableCached() {
        return icon != null;
    }

    @Override
    void setDrawableCache(Drawable drawable) {
        icon = drawable;
    }

    @Override
    protected void doLaunch(Context context, View v) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).showMatchingTags(pojo.getName());
        }
    }

    @ColorInt
    private int getBackgroundColor(Context context) {
        if (DrawableUtils.hasThemedIcons() &&
                DrawableUtils.isThemedIconEnabled(context)) {
            return UIColors.getIconColors(context)[0];
        } else {
            return Color.WHITE;
        }
    }

    @ColorInt
    private int getTextColor(Context context) {
        if (DrawableUtils.hasThemedIcons() &&
                DrawableUtils.isThemedIconEnabled(context)) {
            return UIColors.getIconColors(context)[1];
        } else {
            return UIColors.getPrimaryColor(context);
        }
    }
}
