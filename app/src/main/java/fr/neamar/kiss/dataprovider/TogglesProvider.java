package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.R;
import fr.neamar.kiss.loader.LoadTogglesPojos;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.TogglesPojo;

public class TogglesProvider extends Provider<TogglesPojo> {
    private String toggleName;

    @Override
    public void reload() {
        this.initialize(new LoadTogglesPojos(this));

        toggleName = this.getString(R.string.toggles_prefix).toLowerCase();
    }

    public ArrayList<Pojo> getResults(String query) {
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        String toggleNameLowerCased;
        for (TogglesPojo toggle : pojos) {
            relevance = 0;
            toggleNameLowerCased = toggle.nameNormalized;
            if (toggleNameLowerCased.startsWith(query))
                relevance = 75;
            else if (toggleNameLowerCased.contains(" " + query))
                relevance = 30;
            else if (toggleNameLowerCased.contains(query))
                relevance = 1;
            else if (toggleName.startsWith(query)) {
                // Also display for a search on "settings" for instance
                relevance = 4;
            }

            if (relevance > 0) {
                toggle.displayName = toggle.name.replaceFirst(
                        "(?i)(" + Pattern.quote(query) + ")", "{$1}");
                toggle.relevance = relevance;
                results.add(toggle);
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
