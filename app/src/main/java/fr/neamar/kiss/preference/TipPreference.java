package fr.neamar.kiss.preference;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import fr.neamar.kiss.R;

/**
 * Non-clickable help text displayed among the settings, set with app:summary
 */
public class TipPreference extends Preference {

    public TipPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
        setIcon(R.drawable.setting_info);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        summary.setTypeface(summary.getTypeface(), Typeface.ITALIC);
        // The drawable is tinted white for the toolbar, match the text instead
        ((ImageView) holder.findViewById(android.R.id.icon)).setColorFilter(summary.getCurrentTextColor());
    }
}
