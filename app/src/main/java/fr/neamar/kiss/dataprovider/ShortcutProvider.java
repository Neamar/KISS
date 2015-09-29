package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import fr.neamar.kiss.loader.LoadShortcutPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class ShortcutProvider extends Provider<ShortcutPojo> {

    public ShortcutProvider(Context context) {
        super(new LoadShortcutPojos(context));
    }

    @Override
    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        String shortcutNameLowerCased;
        for (ShortcutPojo shortcut : pojos) {
            relevance = 0;
            shortcutNameLowerCased = shortcut.nameNormalized;
            if (shortcutNameLowerCased.startsWith(query))
                relevance = 75;
            else if (shortcutNameLowerCased.contains(" " + query))
                relevance = 30;
            else if (shortcutNameLowerCased.contains(query))
                relevance = 1;
            else if (shortcutNameLowerCased.startsWith(query)) {
                // Also display for a search on "settings" for instance
                relevance = 4;
            }

            if (relevance > 0) {
                shortcut.displayName = shortcut.name.replaceFirst("(?i)(" + Pattern.quote(query) + ")", "{$1}");
                shortcut.relevance = relevance;
                results.add(shortcut);
            }
        }

        return results;
    }

    public void addShortcut(ShortcutPojo shortcut) {
        this.pojos.add(shortcut);
    }

    public ShortcutPojo createPojo(String name) {
        ShortcutPojo pojo = new ShortcutPojo();

        pojo.id = ShortcutPojo.SCHEME + name.toLowerCase();
        pojo.setName(name);

        return pojo;
    }

    public Pojo findById(String id) {
        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                pojo.displayName = pojo.name;
                return pojo;
            }
        }

        return null;
    }

}
