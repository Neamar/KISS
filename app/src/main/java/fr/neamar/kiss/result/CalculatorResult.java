package fr.neamar.kiss.result;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Collections;

import fr.neamar.kiss.R;
import fr.neamar.kiss.icons.IconPack;
import fr.neamar.kiss.pojo.CalculatorPojo;
import fr.neamar.kiss.utils.ClipboardUtils;
import fr.neamar.kiss.utils.fuzzy.FuzzyScore;

public class CalculatorResult extends Result<CalculatorPojo> {

    private volatile Drawable icon = null;

    CalculatorResult(@NonNull CalculatorPojo pojo) {
        super(pojo);
    }

    @NonNull
    @Override
    public View display(Context context, View view, @NonNull ViewGroup parent, FuzzyScore fuzzyScore) {
        if (view == null)
            view = inflateFromId(context, R.layout.item_calculator, parent);

        TextView searchText = view.findViewById(R.id.item_search_text);
        ImageView image = view.findViewById(R.id.item_search_icon);

        if (isHideIcons(context)) {
            image.setImageDrawable(null);
        } else {
            this.setAsyncDrawable(image);
        }

        String text = pojo.query;
        int pos = text.indexOf("=");
        displayHighlighted(text, Collections.singletonList(new Pair<>(pos, text.length())), searchText, context);

        return view;
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
    public Drawable getDrawable(Context context) {
        if (icon == null) {
            synchronized (this) {
                if (icon == null) {
                    icon = getThemedDrawable(context, pojo, R.drawable.ic_functions);
                }
            }
        }
        return icon;
    }

    @Override
    public void doLaunch(Context context, View v) {
        ClipboardUtils.setClipboard(context, pojo.query.substring(pojo.query.indexOf("=") + 2));
        Toast.makeText(context, R.string.copy_confirmation, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected boolean isAllowedAsFavorite() {
        return false;
    }

    @Override
    protected boolean canHaveCustomIcon(Context context, IconPack iconPack) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean makeThemedIcon() {
        return true;
    }
}
