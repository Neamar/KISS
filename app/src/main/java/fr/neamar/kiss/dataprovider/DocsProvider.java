package fr.neamar.kiss.dataprovider;

import java.util.ArrayList;
import java.util.regex.Pattern;

import fr.neamar.kiss.R;
import fr.neamar.kiss.loader.LoadDocsPojos;
import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.pojo.DocsPojo;
import fr.neamar.kiss.pojo.Pojo;

/**
 * Created by cuneytcarikci on 03/08/16.
 * Provide all files
 */
public class DocsProvider extends Provider<DocsPojo> {
    private String docName;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void reload() {
        this.initialize(new LoadDocsPojos(this));

        docName = this.getString(R.string.docs_prefix).toLowerCase();
    }

    @Override
    public ArrayList<Pojo> getResults(String query) {
        query = StringNormalizer.normalize(query);
        ArrayList<Pojo> results = new ArrayList<>();

        int relevance;
        String docNameLowerCased;
        // Search docs with composed names, e.g "new_doc"
        // (not part of the StringNormalizer class, since we want to keep dashes on other providers)
        query = query.replaceAll("_", " ");
        for (DocsPojo doc : pojos) {
            relevance = 0;
            docNameLowerCased = doc.nameNormalized;
            if (docNameLowerCased.startsWith(query))
                relevance = 40;
            else if (docNameLowerCased.contains(" " + query))
                relevance = 20;
            else if (StringNormalizer.normalize(docName).startsWith(query)) {
                // Also display for a search on "docs" for instance
                relevance = 16;
            }

            if (relevance > 0) {
                doc.displayName = doc.name.replaceFirst(
                        "(?i)(" + Pattern.quote(query) + ")", "{$1}");
                doc.relevance = relevance;
                results.add(doc);
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
