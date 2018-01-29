package fr.neamar.kiss.searcher;

import fr.neamar.kiss.result.Result;
import fr.neamar.kiss.ui.ListPopup;

public interface QueryInterface {
    void launchOccurred(int index, Result result);
    boolean showRelevance();
    void registerPopup( ListPopup popup );
}
