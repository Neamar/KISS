package fr.neamar.kiss.searcher;

import fr.neamar.kiss.ui.ListPopup;

public interface QueryInterface {
    void temporarilyDisableTranscriptMode();

    void launchOccurred();

    void registerPopup(ListPopup popup);
}
