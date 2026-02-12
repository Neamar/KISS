package fr.neamar.kiss.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.preference.DialogPreference;

import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;

public class ColorPreference extends DialogPreference {

    @ColorInt
    private int selectedColor = UIColors.COLOR_DEFAULT;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setDialogLayoutResource(R.layout.pref_color);
    }

    public void setColor(@ColorInt int color) {
        this.selectedColor = color;
        this.persistString(UIColors.colorToString(this.selectedColor));
        this.callChangeListener(selectedColor);
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            setColor(Color.parseColor(getPersistedString(UIColors.colorToString(UIColors.COLOR_DEFAULT))));
        } else {
            setColor(defaultValue instanceof String ? Color.parseColor((String) defaultValue) : UIColors.COLOR_DEFAULT);
        }
    }
}
