package fr.neamar.kiss.preference;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.android.colorpicker.ColorPickerPalette;
import com.android.colorpicker.ColorPickerSwatch.OnColorSelectedListener;

import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;

public class ColorPreferenceDialogFragment extends PreferenceDialogFragmentCompat implements OnColorSelectedListener {

    private ColorPickerPalette palette;

    public static DialogFragment newInstance(String key) {
        ColorPreferenceDialogFragment fragment = new ColorPreferenceDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);

        return fragment;
    }

    protected void drawPalette() {
        if (this.palette != null) {
            this.palette.drawPalette(UIColors.getColorList(), this.getPreference().getSelectedColor());
        }
    }

    @Override
    public ColorPreference getPreference() {
        return (ColorPreference) super.getPreference();
    }

    @Override
    public void onColorSelected(@ColorInt int color) {
        if (color != this.getPreference().getSelectedColor()) {
            this.getPreference().setColor(color);

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

    @Nullable
    @Override
    protected View onCreateDialogView(@NonNull Context context) {
        // Create layout from bound resource
        final View view = super.onCreateDialogView(context);

        // Configure the color picker
        this.palette = view.findViewById(R.id.colorPicker);
        this.palette.init(ColorPickerPalette.SIZE_SMALL, 4, this);

        // Reconfigure color picker based on the available space
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean ignoreNextUpdate = false;

            public void onGlobalLayout() {
                if (this.ignoreNextUpdate) {
                    this.ignoreNextUpdate = false;
                    return;
                }

                // Calculate number of swatches to display
                int swatchSize = ColorPreferenceDialogFragment.this.getContext().getResources().getDimensionPixelSize(R.dimen.color_swatch_small);
                int swatchMargin = ColorPreferenceDialogFragment.this.getContext().getResources().getDimensionPixelSize(R.dimen.color_swatch_margins_small);
                ColorPreferenceDialogFragment.this.palette.init(ColorPickerPalette.SIZE_SMALL, view.getWidth() / (swatchSize + swatchMargin), ColorPreferenceDialogFragment.this);

                // Cause redraw and (by extension) also a layout recalculation
                this.ignoreNextUpdate = true;
                ColorPreferenceDialogFragment.this.drawPalette();
            }
        });

        // Bind click events from the custom color values
        Button buttonColorTransparentDark = view.findViewById(R.id.colorTransparentDark);
        buttonColorTransparentDark.setOnClickListener(v -> ColorPreferenceDialogFragment.this.onColorSelected(UIColors.COLOR_DARK_TRANSPARENT));

        Button buttonColorTransparentWhite = view.findViewById(R.id.colorTransparentWhite);
        buttonColorTransparentWhite.setOnClickListener(v -> ColorPreferenceDialogFragment.this.onColorSelected(UIColors.COLOR_LIGHT_TRANSPARENT));

        Button buttonColorTransparent = view.findViewById(R.id.colorTransparent);
        buttonColorTransparent.setOnClickListener(v -> ColorPreferenceDialogFragment.this.onColorSelected(UIColors.COLOR_TRANSPARENT));

        // show button for getting color from system if supported
        Button buttonColorSystem = view.findViewById(R.id.colorSystem);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            buttonColorSystem.setVisibility(View.VISIBLE);
            buttonColorSystem.setOnClickListener(v -> ColorPreferenceDialogFragment.this.onColorSelected(UIColors.COLOR_SYSTEM));
        } else {
            buttonColorSystem.setVisibility(View.GONE);
        }

        switch (ColorPreferenceDialogFragment.this.getPreference().getSelectedColor()) {
            case UIColors.COLOR_DARK_TRANSPARENT:
                this.selectButton(buttonColorTransparentDark);
                break;
            case UIColors.COLOR_LIGHT_TRANSPARENT:
                this.selectButton(buttonColorTransparentWhite);
                break;
            case UIColors.COLOR_TRANSPARENT:
                this.selectButton(buttonColorTransparent);
                break;
            case UIColors.COLOR_SYSTEM:
                this.selectButton(buttonColorSystem);
                break;
        }

        return view;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        // This will set the correct typeface for the extra items
        switch (ColorPreferenceDialogFragment.this.getPreference().getSelectedColor()) {
            case UIColors.COLOR_DARK_TRANSPARENT:
                Button buttonColorTransparentDark = view.findViewById(R.id.colorTransparentDark);
                this.selectButton(buttonColorTransparentDark);
                break;
            case UIColors.COLOR_LIGHT_TRANSPARENT:
                Button buttonColorTransparentWhite = view.findViewById(R.id.colorTransparentWhite);
                this.selectButton(buttonColorTransparentWhite);
                break;
            case UIColors.COLOR_TRANSPARENT:
                Button buttonColorTransparent = view.findViewById(R.id.colorTransparent);
                this.selectButton(buttonColorTransparent);
                break;
            case UIColors.COLOR_SYSTEM:
                Button buttonColorSystem = view.findViewById(R.id.colorSystem);
                this.selectButton(buttonColorSystem);
                break;
        }

        this.drawPalette();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {

    }
}
