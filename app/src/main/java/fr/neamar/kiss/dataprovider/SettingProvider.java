package fr.neamar.kiss.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.R;
import fr.neamar.kiss.loader.LoadSettingPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.SettingPojo;

public class SettingProvider extends Provider<SettingPojo> {
    private final String settingName;

    public SettingProvider(Context context) {
        super(new LoadSettingPojos(context));
        settingName = context.getString(R.string.settings_prefix).toLowerCase();
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        String settingNameLowerCased;
        for (SettingPojo setting : pojos) {
            relevance = 0;
            settingNameLowerCased = setting.nameNormalized;
            if (settingNameLowerCased.startsWith(query))
                relevance = 10;
            else if (settingNameLowerCased.contains(" " + query))
                relevance = 5;
            else if (settingName.startsWith(query)) {
                // Also display for a search on "settings" for instance
                relevance = 4;
            }

            if (relevance > 0) {
                setting.displayName = setting.name.replaceFirst(
                        "(?i)(" + Pattern.quote(query) + ")", "{$1}");
                setting.relevance = relevance;
                results.add(setting);
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
}
