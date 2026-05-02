package fr.neamar.kiss.searcher;

import androidx.fragment.app.DialogFragment;

import fr.neamar.kiss.ui.ListPopup;

public interface QueryInterface {
    void temporarilyDisableTranscriptMode();

    void updateTranscriptMode(int transcriptMode);

    void beforeListChange();

    void afterListChange();

    void launchOccurred();

    void registerPopup(ListPopup popup);

    void showDialog(DialogFragment dialog);
}
