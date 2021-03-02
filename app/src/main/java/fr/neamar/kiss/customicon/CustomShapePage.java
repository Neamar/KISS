package fr.neamar.kiss.customicon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.R;
import fr.neamar.kiss.utils.DrawableUtils;
import fr.neamar.kiss.utils.ViewHolderAdapter;
import fr.neamar.kiss.utils.ViewHolderListAdapter;

class CustomShapePage extends PageAdapter.Page {
    protected ShapedIconAdapter mShapesAdapter;
    protected ShapedIconAdapter mShapedIconAdapter;
    protected TextView mLettersView;
    protected int mShape;
    protected float mScale = 1.f;
    protected int mBackground;
    private int mLetters;

    CustomShapePage(CharSequence name, View view) {
        super(name, view);
        final Context ctx = view.getContext();
        mShape = KissApplication.getApplication(ctx).getIconsHandler().getSystemIconPack().getAdaptiveShape();
        mLetters = UIColors.getContactActionColor(ctx);
        mBackground = UIColors.getIconBackground(ctx);
        if (mShape == DrawableUtils.SHAPE_SYSTEM)
            mShape = DrawableUtils.SHAPE_SQUARE;
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        mLettersView = pageView.findViewById(R.id.letters);
        mLettersView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                generateTextIcons(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // shape list toggle
        setupToggle(R.id.shapeGridToggle, R.id.shapeGrid);
        // scale bar toggle
        setupToggle(R.id.scaleBarToggle, R.id.scaleBar);
        // letters toggle
        setupToggle(R.id.lettersToggle, R.id.lettersGroup);

        // shapes list
        {
            GridView shapeGridView = pageView.findViewById(R.id.shapeGrid);
            mShapesAdapter = new ShapedIconAdapter();
            shapeGridView.setAdapter(mShapesAdapter);
            shapeGridView.setOnItemClickListener((parent, view, position, id) -> {
                Activity activity = Utilities.getActivity(view);
                if (activity == null)
                    return;

                Object objItem = parent.getAdapter().getItem(position);
                if (!(objItem instanceof NamedIconInfo) || ((NamedIconInfo) objItem).getPreview() == null)
                    return;
                CharSequence name = ((NamedIconInfo) objItem).name;
                for (int shape : DrawableUtils.SHAPE_LIST) {
                    if (name.equals(DrawableUtils.shapeName(activity, shape))) {
                        mShape = shape;
                        break;
                    }
                }
                reshapeIcons(activity);
            });
            TBApplication.ui(context).setResultListPref(shapeGridView);
        }

        // icons we are customizing
        {
            GridView gridView = pageView.findViewById(R.id.iconGrid);
            mShapedIconAdapter = new ShapedIconAdapter();
            gridView.setAdapter(mShapedIconAdapter);
            if (iconClickListener != null)
                gridView.setOnItemClickListener((parent, view, position, id) -> {
                    Object item = parent.getAdapter().getItem(position);
                    if (item instanceof ShapedIconInfo && ((ShapedIconInfo) item).getPreview() != null)
                        iconClickListener.onItemClick(parent.getAdapter(), view, position);
                });
            TBApplication.ui(context).setResultListPref(gridView);
        }

        // scale bar
        {
            SeekBar seekBar = pageView.findViewById(R.id.scaleBar);
            seekBar.setMax(200);
            seekBar.setProgress((int) (100.f * mScale));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mScale = 0.01f * seekBar.getProgress();
                    reshapeIcons(seekBar.getContext());
                }
            });
        }

        final float colorPreviewRadius = context.getResources().getDimension(R.dimen.color_preview_radius);
        final int colorPreviewBorder = UISizes.dp2px(context, 1);
        final int colorPreviewSize = context.getResources().getDimensionPixelSize(R.dimen.color_preview_size);

        // shape background color chooser
        {
            TextView colorView = pageView.findViewById(R.id.backgroundColor);
            {
                Drawable drawable = UIColors.getPreviewDrawable(mBackground, colorPreviewBorder, colorPreviewRadius);
                drawable.setBounds(0, 0, colorPreviewSize, colorPreviewSize);
                colorView.setCompoundDrawables(null, null, drawable, null);
            }
            colorView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                launchCustomColorDialog(ctx, mBackground, color -> {
                    mBackground = color;
                    Activity activity = Utilities.getActivity(v);
                    if (activity == null)
                        return;
                    Drawable drawable = UIColors.getPreviewDrawable(mBackground, colorPreviewBorder, colorPreviewRadius);
                    drawable.setBounds(0, 0, colorPreviewSize, colorPreviewSize);
                    colorView.setCompoundDrawables(null, null, drawable, null);
                    generateShapes(activity);
                    reshapeIcons(activity);
                });
            });
        }

        // letter color chooser
        {
            TextView colorView = pageView.findViewById(R.id.lettersColor);
            {
                Drawable drawable = UIColors.getPreviewDrawable(mLetters, colorPreviewBorder, colorPreviewRadius);
                drawable.setBounds(0, 0, colorPreviewSize, colorPreviewSize);
                colorView.setCompoundDrawables(null, null, drawable, null);
            }
            colorView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                launchCustomColorDialog(ctx, mLetters, color -> {
                    mLetters = color;
                    Activity activity = Utilities.getActivity(v);
                    if (activity == null)
                        return;
                    Drawable drawable = UIColors.getPreviewDrawable(mLetters, colorPreviewBorder, colorPreviewRadius);
                    drawable.setBounds(0, 0, colorPreviewSize, colorPreviewSize);
                    colorView.setCompoundDrawables(null, null, drawable, null);
                    generateTextIcons(mLettersView.getText());
                });
            });
        }

        generateShapes(context);
    }


    private void setupToggle(@IdRes int toggleTextView, @IdRes int viewToToggle) {
        TextView textView = pageView.findViewById(toggleTextView);
        textView.setOnClickListener(v -> {
            View view = pageView.findViewById(viewToToggle);
            if ("hide".equals(v.getTag())) {
                view.setVisibility(View.GONE);
                ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
                v.setTag("show");
            } else {
                view.setVisibility(View.VISIBLE);
                ((TextView) v).setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_up_float, 0);
                v.setTag("hide");
            }
        });
        if (textView.getTag() == null) {
            textView.setTag("hide");
            textView.performClick();
        }
    }


    public void addIcon(@NonNull String name, @NonNull Drawable drawable) {
        Context context = pageView.getContext();
        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, mShape, mScale, mBackground);
        NamedIconInfo iconInfo = new NamedIconInfo(name, shapedDrawable, drawable);
        mShapedIconAdapter.addItem(iconInfo);
    }

    private void addTextIcon(CharSequence name, @NonNull TextDrawable icon) {
        final Context ctx = pageView.getContext();
        final ShapedIconAdapter adapter = mShapedIconAdapter;

        icon.setTextColor(mLetters);
        Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, mShape, mScale, mBackground);
        adapter.addItem(new LetterIconInfo(name, shapedIcon, icon));
    }

    private void generateTextIcons(@Nullable CharSequence text) {
        final ShapedIconAdapter adapter = mShapedIconAdapter;

        // remove all TextDrawable icons
        for (Iterator<ShapedIconInfo> iterator = adapter.getList().iterator(); iterator.hasNext(); ) {
            ShapedIconInfo info = iterator.next();
            if (info instanceof LetterIconInfo)
                iterator.remove();
        }
        adapter.notifyDataSetChanged();

        final StringBuilder name = new StringBuilder();
        final int length = Utilities.codePointsLength(text);
        int pos = 0;
        if (length >= 1) {
            name.appendCodePoint(Character.codePointAt(text, pos));
            TextDrawable icon = new CodePointDrawable(text);
            addTextIcon(name.toString(), icon);
        }
        // two characters
        if (length >= 2) {
            pos = Utilities.getNextCodePointIndex(text, pos);
            name.appendCodePoint(Character.codePointAt(text, pos));
            TextDrawable icon = TwoCodePointDrawable.fromText(text, false);
            addTextIcon(name.toString(), icon);
        }
        if (length >= 2) {
            TextDrawable icon = TwoCodePointDrawable.fromText(text, true);
            addTextIcon(name.toString(), icon);
        }
        // three characters
        if (length >= 3) {
            pos = Utilities.getNextCodePointIndex(text, pos);
            name.appendCodePoint(Character.codePointAt(text, pos));
            TextDrawable icon = FourCodePointDrawable.fromText(text, true);
            addTextIcon(name.toString(), icon);
        }
        // four characters
        if (length >= 4) {
            pos = Utilities.getNextCodePointIndex(text, pos);
            name.appendCodePoint(Character.codePointAt(text, pos));
        }
        if (length >= 3) {
            TextDrawable icon = FourCodePointDrawable.fromText(text, false);
            addTextIcon(name.toString(), icon);
        }
    }

    private void generateShapes(Context context) {
        final ShapedIconAdapter adapter = mShapesAdapter;
        adapter.getList().clear();
        adapter.notifyDataSetChanged();
        Drawable drawable = new ColorDrawable(mBackground);
        for (int shape : DrawableUtils.SHAPE_LIST) {
            String name = DrawableUtils.shapeName(context, shape);
            Drawable shapedDrawable;
            if (shape == DrawableUtils.SHAPE_SYSTEM) {
                shapedDrawable = new ColorDrawable(Color.TRANSPARENT);
            } else {
                shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, shape);
            }
            NamedIconInfo iconInfo = new NamedIconInfo(name, shapedDrawable, null);
            adapter.addItem(iconInfo);
        }
    }

    private void reshapeIcons(Context context) {
        //generateTextIcons(null);
        for (ListIterator<ShapedIconInfo> iterator = mShapedIconAdapter.getList().listIterator(); iterator.hasNext(); ) {
            ShapedIconInfo iconInfo = iterator.next();
            if (iconInfo.textId == R.string.icon_pack_loading)
                continue;
            if (iconInfo.textId == R.string.default_icon)
                continue;
            ShapedIconInfo newInfo = iconInfo.reshape(context, mShape, mScale, mBackground);
            iterator.set(newInfo);
        }
        mShapedIconAdapter.notifyDataSetChanged();
        //generateTextIcons(mLettersView.getText());
    }

    interface OnColorChanged {
        void onColorChanged(int color);
    }

    private static void launchCustomColorDialog(@Nullable Context context, int selectedColor, @NonNull OnColorChanged listener) {
//        Activity activity = Utilities.getActivity(context);
//        if (!(activity instanceof AppCompatActivity))
//            return;
//
//        Context themeWrapper = UITheme.getDialogThemedContext(context);
//        DialogView dialogView = new DialogView(themeWrapper);
//
//        dialogView.init(selectedColor, (AppCompatActivity) activity);
//        dialogView.setWithAlpha(true);
//
//        DialogInterface.OnClickListener buttonListener = (dialog, which) -> {
//            if (which == DialogInterface.BUTTON_POSITIVE) {
//                listener.onColorChanged(dialogView.getColor());
//            }
//            dialog.dismiss();
//        };
//
//        final AlertDialog.Builder builder = new AlertDialog.Builder(themeWrapper)
//                .setPositiveButton(android.R.string.ok, buttonListener)
//                .setNegativeButton(android.R.string.cancel, buttonListener);
//        builder.setView(dialogView);
//        DialogBuilder.setButtonBarBackground(builder.show());
    }

    static class LetterIconInfo extends NamedIconInfo {

        LetterIconInfo(CharSequence name, Drawable icon, Drawable text) {
            super(name, icon, text);
        }

        @Override
        ShapedIconInfo reshape(Context context, int shape, float scale, int background) {
            Drawable drawable = DrawableUtils.applyIconMaskShape(context, originalDrawable, shape, scale, background);
            return new LetterIconInfo(name, drawable, originalDrawable);
        }
    }

    static class DefaultIconInfo extends ShapedIconInfo {

        DefaultIconInfo(Drawable icon) {
            super(icon, icon);
        }

        @Override
        Drawable getIcon() {
            return null;
        }
    }

    static class NamedIconInfo extends ShapedIconInfo {
        final CharSequence name;

        NamedIconInfo(CharSequence name, Drawable icon, Drawable origin) {
            super(icon, origin);
            this.name = name;
        }

        @Override
        ShapedIconInfo reshape(Context context, int shape, float scale, int background) {
            Drawable drawable = DrawableUtils.applyIconMaskShape(context, originalDrawable, shape, scale, background);
            return new NamedIconInfo(name, drawable, originalDrawable);
        }

        @Nullable
        @Override
        CharSequence getText() {
            return name;
        }
    }

    static class ShapedIconInfo {
        final Drawable originalDrawable;
        final Drawable iconDrawable;
        @StringRes
        int textId;

        ShapedIconInfo(Drawable icon, Drawable origin) {
            iconDrawable = icon;
            originalDrawable = origin;
        }

        ShapedIconInfo reshape(Context context, int shape, float scale, int background) {
            Drawable drawable = DrawableUtils.applyIconMaskShape(context, originalDrawable, shape, scale, background);
            ShapedIconInfo shapedIconInfo = new ShapedIconInfo(drawable, originalDrawable);
            shapedIconInfo.textId = textId;
            return shapedIconInfo;
        }

        Drawable getIcon() {
            return iconDrawable;
        }

        public Drawable getPreview() {
            return iconDrawable;
        }

        @Nullable
        CharSequence getText() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShapedIconInfo that = (ShapedIconInfo) o;
            return ((iconDrawable == that.iconDrawable) || (iconDrawable != null && iconDrawable.equals(that.iconDrawable))) &&
                    textId == that.textId;
        }

        @Override
        public int hashCode() {
            return 31 * iconDrawable.hashCode() + textId;
        }
    }

    public static class ShapedIconVH extends ViewHolderAdapter.ViewHolder<ShapedIconInfo> {
        View root;
        ImageView icon;
        TextView text1;

        public ShapedIconVH(View view) {
            super(view);
            root = view;
            icon = view.findViewById(android.R.id.icon);
            text1 = view.findViewById(android.R.id.text1);
        }

        @Override
        protected void setContent(ShapedIconInfo content, int position, @NonNull ViewHolderAdapter<ShapedIconInfo, ? extends ViewHolderAdapter.ViewHolder<ShapedIconInfo>> adapter) {
            // set icon
            Drawable preview = content.getPreview();
            icon.setImageDrawable(preview);
            icon.setVisibility(preview == null ? View.GONE : View.VISIBLE);
            if (preview instanceof Animatable)
                ((Animatable) preview).start();

            //set text
            CharSequence text = content.getText();
            if (text != null)
                text1.setText(text);
            else
                text1.setText(content.textId);
        }
    }

    static class ShapedIconAdapter extends ViewHolderListAdapter<ShapedIconInfo, ShapedIconVH> {
        protected ShapedIconAdapter() {
            super(ShapedIconVH.class, R.layout.item_grid, new ArrayList<>());
        }

        List<ShapedIconInfo> getList() {
            return mList;
        }

        void removeItem(ShapedIconInfo item) {
            mList.remove(item);
            notifyDataSetChanged();
        }
    }
}
