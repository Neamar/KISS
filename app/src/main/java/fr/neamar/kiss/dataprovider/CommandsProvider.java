package fr.neamar.kiss.dataprovider;

import fr.neamar.kiss.loader.LoadCommandsPojos;
import fr.neamar.kiss.pojo.CommandsPojo;
import fr.neamar.kiss.searcher.Searcher;

public class CommandsProvider extends Provider<CommandsPojo> {

    @Override
    public void reload() {
        super.reload();
        this.initialize(new LoadCommandsPojos(this));
    }

    @Override
    public void requestResults(String query, Searcher searcher) {

        //show only if query starts with kiss
        if (!query.startsWith("kiss")) {
            return;
        }

        for (CommandsPojo pojo : pojos) {
            if (pojo.getName().startsWith(query)) {
                pojo.relevance = 10;
                if (!searcher.addResult(pojo)) {
                    return;
                }
            }
        }
    }
}
