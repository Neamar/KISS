package fr.neamar.kiss.db;

import androidx.annotation.NonNull;

public enum HistoryMode {
    RECENCY("recency"),
    FRECENCY("frecency"),
    FREQUENCY("frequency"),
    ADAPTIVE("adaptive"),
    TIME("time"),
    ALPHABETICALLY("alphabetically");

    private final String id;

    HistoryMode(String id) {
        this.id = id;
    }

    @NonNull
    public static HistoryMode valueById(String id) {
        for (HistoryMode historyMode : values()) {
            if (historyMode.id.equals(id)) {
                return historyMode;
            }
        }
        return RECENCY;
    }
}
