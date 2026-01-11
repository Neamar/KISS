package fr.neamar.kiss.utils;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

public class TrimmingTextChangedListener implements TextWatcher {

    private final TextChangedCallback textChangedCallback;
    private final boolean executeCallbackOnEmpty;

    private String oldText = null;

    public TrimmingTextChangedListener(@NonNull TextChangedCallback textChangedCallback, boolean executeCallbackOnEmpty) {
        this.textChangedCallback = textChangedCallback;
        this.executeCallbackOnEmpty = executeCallbackOnEmpty;
    }

    public void afterTextChanged(Editable s) {
        int length = s.length();

        // trim all whitespaces from right
        int end = length;
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        // keep last whitespace after char if possible
        if (end > 0 && end < length) {
            end++;
        }

        // trim all whitespaces from left
        int start = 0;
        while (start < end && Character.isWhitespace(s.charAt(start))) {
            start++;
        }

        if (start > 0 || end < length) {
            s.replace(0, length, s.subSequence(start, end));
        } else {
            // compare with text from before change and execute callback if necessary
            String text = s.toString().trim();
            if (!text.equals(oldText) || (executeCallbackOnEmpty && text.isEmpty())) {
                textChangedCallback.execute(text);
            }
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // remember text before change
        oldText = s.toString().trim();
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    /**
     * Callback for when text has changed after trimming whitespaces
     */
    @FunctionalInterface
    public interface TextChangedCallback {
        /**
         * Execute callback when text has changed.
         *
         * @param changedText changed text
         */
        void execute(String changedText);
    }

}
