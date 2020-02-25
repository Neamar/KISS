package fr.neamar.kiss.searcher;

import android.widget.PopupWindow;

public interface QueryInterface {
    void temporarilyDisableTranscriptMode();
    void updateTranscriptMode(int transcriptMode);

    void launchOccurred();

    void registerPopup(PopupWindow popup);
}
