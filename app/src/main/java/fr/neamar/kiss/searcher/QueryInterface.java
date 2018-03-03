package fr.neamar.kiss.searcher;

import fr.neamar.kiss.ui.ListPopup;

public interface QueryInterface {
    void launchOccurred();

    boolean showRelevance();

    void registerPopup(ListPopup popup);
}
