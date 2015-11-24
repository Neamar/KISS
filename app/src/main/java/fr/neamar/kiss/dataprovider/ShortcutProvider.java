package fr.neamar.kiss.dataprovider;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Bitmap.CompressFormat;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ShortcutRecord;
import fr.neamar.kiss.loader.LoadShortcutPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.ShortcutPojo;

public class ShortcutProvider extends Provider<ShortcutPojo> {
    
    Context context = null;

    public ShortcutProvider(Context context) {        
        super(new LoadShortcutPojos(context));
        this.context = context;
    }

    @Override
    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        int matchPositionStart;
        int matchPositionEnd;
        String shortcutNameLowerCased;
        
        final String queryWithSpace = " " + query;
        for (ShortcutPojo shortcut : pojos) {
            relevance = 0;
            shortcutNameLowerCased = shortcut.nameNormalized;
            
            matchPositionEnd = 0;
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
            
            if (relevance > 0) {
                shortcut.setDisplayNameHighlightRegion(matchPositionStart, matchPositionEnd);
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
