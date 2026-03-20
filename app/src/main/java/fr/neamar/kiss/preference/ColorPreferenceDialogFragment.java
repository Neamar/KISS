package fr.neamar.kiss.preference;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.colorpicker.ColorStateDrawable;

import fr.neamar.kiss.R;
import fr.neamar.kiss.UIColors;

public class ColorPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private ColorsAdapter colorsAdapter;

    public static DialogFragment newInstance(String key) {
        ColorPreferenceDialogFragment fragment = new ColorPreferenceDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public ColorPreference getPreference() {
        return (ColorPreference) super.getPreference();
    }

    public void onColorSelected(@ColorInt int color) {
        if (color != this.getPreference().getSelectedColor()) {
            this.getPreference().setSelectedColor(color);
            this.colorsAdapter.setSelectedColor(color);
        }

        // Close the dialog
        this.getDialog().dismiss();
    }

    private void selectButton(Button button) {
        Context context = requireContext();
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
        RecyclerView colors = view.findViewById(R.id.colors);
        this.colorsAdapter = new ColorsAdapter(ColorPreferenceDialogFragment.this.getPreference().getSelectedColor(), this::onColorSelected);
        colors.setAdapter(this.colorsAdapter);
        colors.setHasFixedSize(true);

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
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {

    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /**
     * Interface for a callback when a color square is selected.
     */
    public interface OnColorSelectedListener {

        /**
         * Called when a specific color square has been selected.
         */
        void onColorSelected(int color);
    }

    private static class ColorsAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final OnColorSelectedListener onColorSelectedListener;

        @ColorInt
        int selectedColor;

        public ColorsAdapter(@ColorInt int selectedColor, @Nullable OnColorSelectedListener onColorSelectedListener) {
            this.selectedColor = selectedColor;
            this.onColorSelectedListener = onColorSelectedListener;
            this.setHasStableIds(true);
        }

        public void setSelectedColor(@ColorInt int selectedColor) {
            this.selectedColor = selectedColor;
            notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            return UIColors.getColorList()[position];
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.color_picker_swatch, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int color = UIColors.getColorList()[position];
            ImageView mSwatchImage = holder.itemView.findViewById(R.id.color_picker_swatch);
            Drawable[] colorDrawable = new Drawable[]
                    {ResourcesCompat.getDrawable(holder.itemView.getContext().getResources(), R.drawable.color_picker_swatch, holder.itemView.getContext().getTheme())};
            mSwatchImage.setImageDrawable(new ColorStateDrawable(colorDrawable, color));

            ImageView mCheckmarkImage = holder.itemView.findViewById(R.id.color_picker_checkmark);
            if (color == selectedColor) {
                mCheckmarkImage.setVisibility(View.VISIBLE);
            } else {
                mCheckmarkImage.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (onColorSelectedListener != null) {
                    onColorSelectedListener.onColorSelected(color);
                }
            });
        }

        @Override
        public int getItemCount() {
            return UIColors.getColorList().length;
        }
    }
}
