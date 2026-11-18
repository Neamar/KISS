package fr.neamar.kiss.searcher;

import androidx.fragment.app.DialogFragment;

import fr.neamar.kiss.ui.ListPopup;

public interface QueryInterface {
    /**
     * Called when a new result set is displayed (not on a refresh): the most relevant results are at the top
     */
    void scrollToTop();

    void launchOccurred();

    void registerPopup(ListPopup popup);

    void showDialog(DialogFragment dialog);
}
