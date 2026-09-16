package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;

import fr.neamar.kiss.R;

/**
 * An {@link EditTextPreference} that validates the entered integer is within
 * a configured {@code [minValue, maxValue]} range before persisting.
 * <p>
 * When the user enters a value outside the range, the value is clamped to
 * the nearest bound and a toast informs the user.
 * <p>
 * Custom XML attributes (defined in {@code res/values/attrs.xml}):
 * <ul>
 *     <li>{@code app:minValue} — inclusive lower bound (default {@link Integer#MIN_VALUE})</li>
 *     <li>{@code app:maxValue} — inclusive upper bound (default {@link Integer#MAX_VALUE})</li>
 * </ul>
 */
public class RangeEditTextPreference extends EditTextPreference {

    private int minValue = Integer.MIN_VALUE;
    private int maxValue = Integer.MAX_VALUE;

    public RangeEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public RangeEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public RangeEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RangeEditTextPreference(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RangeEditTextPreference);
            try {
                minValue = a.getInteger(R.styleable.RangeEditTextPreference_minValue, Integer.MIN_VALUE);
                maxValue = a.getInteger(R.styleable.RangeEditTextPreference_maxValue, Integer.MAX_VALUE);
            } finally {
                a.recycle();
            }
        }

        // Validate before persisting
        setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(preference instanceof RangeEditTextPreference)) {
                return true;
            }
            RangeEditTextPreference rangePref = (RangeEditTextPreference) preference;
            String valueStr = (String) newValue;
            try {
                int value = Integer.parseInt(valueStr);
                int clamped = Math.max(rangePref.minValue, Math.min(rangePref.maxValue, value));
                if (clamped != value) {
                    Toast.makeText(context,
                            context.getString(R.string.range_preference_clamped, rangePref.minValue, rangePref.maxValue),
                            Toast.LENGTH_SHORT).show();
                    // Persist the clamped value and update the summary
                    rangePref.setText(String.valueOf(clamped));
                    return false; // Prevent default persist; we already persisted the clamped value
                }
                return true; // Allow normal persist
            } catch (NumberFormatException e) {
                Toast.makeText(context,
                        context.getString(R.string.range_preference_invalid),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }
}
