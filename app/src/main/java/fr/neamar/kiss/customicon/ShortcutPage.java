package fr.neamar.kiss.customicon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;

public class ShortcutPage extends CustomShapePage {
    private final ShortcutRecord mShortcutRecord;

    ShortcutPage(CharSequence name, View view, ShortcutRecord shortcutRecord) {
        super(name, view);
        mShortcutRecord = shortcutRecord;
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        super.setupView(context, iconClickListener, iconLongClickListener);

        final Drawable defaultIcon;
        // default icon
        {
            Bitmap bitmap = ShortcutUtil.getInitialIcon(context, mShortcutRecord.dbId);
            defaultIcon = new BitmapDrawable(context.getResources(), bitmap);
            Drawable drawable = TBApplication.iconsHandler(context).applyShortcutMask(context, bitmap);
            ShapedIconInfo iconInfo = new StaticEntryPage.DefaultIconInfo(context.getString(R.string.default_static_icon, mShortcutRecord.displayName), drawable);
            iconInfo.textId = R.string.default_icon;
            mShapedIconAdapter.addItem(iconInfo);
        }

        // add background
        {
            Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, defaultIcon, mShape, mScale, mBackground);
            ShapedIconInfo iconInfo = new NamedIconInfo("", shapedDrawable, defaultIcon);
            mShapedIconAdapter.addItem(iconInfo);
        }

        // this will call generateTextIcons
        mLettersView.setText(mShortcutRecord.displayName);
    }

}
