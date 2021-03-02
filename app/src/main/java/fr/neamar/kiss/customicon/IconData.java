package fr.neamar.kiss.customicon;

import android.graphics.drawable.Drawable;

import rocks.tbog.tblauncher.icons.IconPackXML;

class IconData {
    final IconPackXML.DrawableInfo drawableInfo;
    final IconPackXML iconPack;

    IconData(IconPackXML iconPack, IconPackXML.DrawableInfo drawableInfo) {
        this.iconPack = iconPack;
        this.drawableInfo = drawableInfo;
    }

    Drawable getIcon() {
        return iconPack.getDrawable(drawableInfo);
    }
}
