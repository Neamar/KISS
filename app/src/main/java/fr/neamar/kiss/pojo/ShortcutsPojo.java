package fr.neamar.kiss.pojo;

import android.graphics.Bitmap;

public class ShortcutsPojo extends Pojo {

    public static final String SCHEME = "shortcut://";

    public String packageName;
    public String resourceName;
    public String intentUri;
    public Bitmap icon;

}
