package fr.neamar.kiss.result;

import android.content.pm.ResolveInfo;

public class ShortcutPopupResult extends AppPopupResult {
    public ResolveInfo shortcut;
    public ShortcutPopupResult(String packageName, String name, ResolveInfo shortcut) {
        super(packageName, name);
        this.shortcut = shortcut;
    }
}
