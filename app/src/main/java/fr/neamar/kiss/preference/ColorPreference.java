package fr.neamar.kiss.preference;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;

import androidx.annotation.ColorInt;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerPalette;
import com.android.colorpicker.ColorPickerSwatch.OnColorSelectedListener;

import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;


public class ColorPreference extends DialogPreference implements OnColorSelectedListener {
    private final int COLOR_TRANSPARENT = 0x00000000;
    private final int COLOR_LIGHT_TRANSPARENT = 0xAAFFFFFF;
    private final int COLOR_DARK_TRANSPARENT = 0xAA000000;
    private ColorPickerPalette palette;

    private int selectedColor;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setDialogLayoutResource(R.layout.pref_color);

        // Optionally override default color value with value from preference XML
        this.selectedColor = UIColors.COLOR_DEFAULT;
        if (attrs != null) {
            String value = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "defaultValue");
            if (value != null) {
                this.selectedColor = Color.parseColor(value);
            }
        }
    }

    private void drawPalette() {
        if (this.palette != null) {
            this.palette.drawPalette(UIColors.COLOR_LIST, this.selectedColor);
        }
    }

    @Override
    public void onColorSelected(int color) {
        if (color != this.selectedColor) {
            if (!this.callChangeListener(color)) {
                return;
            }

            this.selectedColor = color;
            this.persistString(String.format("#%08X", this.selectedColor));

            // Redraw palette to show checkmark on newly selected color before dismissing
            this.drawPalette();
        }

        // Close the dialog
        this.getDialog().dismiss();
    }

    private void selectButton(Button button) {
        Context context = getContext();
        TypedValue tv = new TypedValue();
        boolean found = context.getTheme().resolveAttribute(android.R.attr.textColor, tv, true);
        @ColorInt int primaryColor = found ? tv.data : Color.BLACK;

        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(primaryColor);
    }

    @Override
    protected View onCreateDialogView() {
        // Create layout from bound resource
        final View view = super.onCreateDialogView();

        // Configure the color picker
        this.palette = view.findViewById(R.id.colorPicker);
        this.palette.init(ColorPickerDialog.SIZE_SMALL, 4, this);

        // Reconfigure color picker based on the available space
        view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            private boolean ignoreNextUpdate = false;

            public void onGlobalLayout() {
                if (this.ignoreNextUpdate) {
                    this.ignoreNextUpdate = false;
                    return;
                }

                // Calculate number of swatches to display
                int swatchSize = ColorPreference.this.palette.getResources().getDimensionPixelSize(R.dimen.color_swatch_small);
                ColorPreference.this.palette.init(ColorPickerDialog.SIZE_SMALL, (view.getWidth() - (swatchSize * 2 / 3)) / swatchSize, ColorPreference.this);


                // Cause redraw and (by extension) also a layout recalculation
                this.ignoreNextUpdate = true;
                ColorPreference.this.drawPalette();
            }
        });

        // Bind click events from the custom color values
        Button button1 = view.findViewById(R.id.colorTransparentDark);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ColorPreference.this.onColorSelected(COLOR_DARK_TRANSPARENT);
            }
        });

        Button button2 = view.findViewById(R.id.colorTransparentWhite);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ColorPreference.this.onColorSelected(COLOR_LIGHT_TRANSPARENT);
            }
        });

        Button button3 = view.findViewById(R.id.colorTransparent);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ColorPreference.this.onColorSelected(COLOR_TRANSPARENT);
            }
        });

        if(ColorPreference.this.selectedColor == COLOR_DARK_TRANSPARENT) this.selectButton(button1);
        if(ColorPreference.this.selectedColor == COLOR_LIGHT_TRANSPARENT) this.selectButton(button2);
        if(ColorPreference.this.selectedColor == COLOR_TRANSPARENT) this.selectButton(button3);

        return view;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        android.util.Log.i("ColorPreference", "View Width:  " + view.getWidth() + " | " + view.getMeasuredWidth());
        // Set selected color value based on the actual color value currently used
        // (but fall back to default from XML)
        this.selectedColor = Color.parseColor(
                this.getPersistedString(String.format("#%08X", this.selectedColor))
        );

        // This will set the correct typeface for the extra items
        if(ColorPreference.this.selectedColor == COLOR_DARK_TRANSPARENT) {
            Button button = view.findViewById(R.id.colorTransparentDark);
            selectButton(button);
        }
        if(ColorPreference.this.selectedColor == COLOR_LIGHT_TRANSPARENT) {
            Button button = view.findViewById(R.id.colorTransparentWhite);
            selectButton(button);
        }
        if(ColorPreference.this.selectedColor == COLOR_TRANSPARENT) {
            Button button = view.findViewById(R.id.colorTransparent);
            selectButton(button);
        }

        this.drawPalette();
    }
}
