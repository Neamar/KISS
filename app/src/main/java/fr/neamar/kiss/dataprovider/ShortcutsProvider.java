package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;

import fr.neamar.kiss.loader.LoadShortcutsPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutsPojo;

public class ShortcutsProvider extends Provider<ShortcutsPojo> {

    @Override
    public void reload() {
        this.initialize(new LoadShortcutsPojos(this));
    }

    @Override
    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        int matchPositionStart;
        int matchPositionEnd;
        String shortcutNameLowerCased;

        final String queryWithSpace = " " + query;
        for (ShortcutsPojo shortcut : pojos) {
            relevance = 0;

            shortcut.displayName = shortcut.name;
            shortcut.displayTags = shortcut.tags;

            shortcutNameLowerCased = shortcut.nameNormalized;

            matchPositionEnd = 0;

            boolean matchedTags = false;
            int tagStart = 0;
            int tagEnd = 0;

            if (shortcutNameLowerCased.startsWith(query)) {
                relevance = 75;
                matchPositionStart = 0;
                matchPositionEnd   = query.length();
            }
            else if ((matchPositionStart = shortcutNameLowerCased.indexOf(queryWithSpace)) > -1) {
                relevance = 50;
                matchPositionEnd = matchPositionStart + queryWithSpace.length();
            }
            else if ((matchPositionStart = shortcutNameLowerCased.indexOf(query)) > -1) {
                relevance = 1;
                matchPositionEnd = matchPositionStart + query.length();
            }
            else {
                if (shortcut.tagsNormalized.startsWith(query)) {
                    relevance = 4 + query.length();
                }
                else if (shortcut.tagsNormalized.contains(query)) {
                    relevance = 3 + query.length();
                }
                if (relevance > 0) {
                    matchedTags = true;
                }
                tagStart = shortcut.tagsNormalized.indexOf(query);
                tagEnd = tagStart + query.length();

            }

            if (relevance > 0) {
                if (!matchedTags) {
                    shortcut.setDisplayNameHighlightRegion(matchPositionStart, matchPositionEnd);
                }
                else {
                    shortcut.setTagHighlight(tagStart, tagEnd);
                }
                shortcut.relevance = relevance;
                results.add(shortcut);
            }
        }

        return results;
    }

    public Pojo findById(String id) {

        for (Pojo pojo : pojos) {
            if (pojo.id.equals(id)) {
                pojo.displayName = pojo.name;
                pojo.displayTags = pojo.tags;
                return pojo;
            }
        }

        return null;
    }

    public Pojo findByName(String name) {
        for (Pojo pojo : pojos) {
            if (pojo.name.equals(name))
                return pojo;
        }
        return null;
    }


}
