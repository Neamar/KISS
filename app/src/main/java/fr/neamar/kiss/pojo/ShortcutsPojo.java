package fr.neamar.kiss.pojo;

import android.graphics.Bitmap;

public class ShortcutsPojo extends PopupPojo {

    public static final String SCHEME = "shortcut://";

    public String resourceName;
    public String intentUri;
    public Bitmap icon;
}
