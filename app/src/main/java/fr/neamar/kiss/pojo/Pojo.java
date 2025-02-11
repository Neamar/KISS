package fr.neamar.kiss.pojo;

import fr.neamar.kiss.normalizer.StringNormalizer;
import fr.neamar.kiss.utils.fuzzy.MatchInfo;

public abstract class Pojo {
    public static final String DEFAULT_ID = "(none)";

    // Globally unique ID.
    // Usually starts with provider scheme, e.g. "app://" or "contact://" to
    // ensure unique constraint
    public String id;
    // normalized name, for faster search
    public StringNormalizer.Result normalizedName = null;
    // Lower-cased name, for faster search
    //public String nameNormalized = "";
    // How relevant is this record ? The higher, the most probable it will be
    // displayed
    public int relevance = 0;
    // Name for this pojo, e.g. app name
    private String name = "";

    public Pojo(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    /**
     * Set the user-displayable name of this container
     * <p/>
     * When this method a searchable version of the name will be generated for the name and stored
     * as `nameNormalized`. Additionally a mapping from the positions in the searchable name
     * to the positions in the displayable name will be stored (as `namePositionMap`).
     *
     * @param name User-friendly name of this container
     */
    public void setName(String name) {
        if (name != null) {
            // Set the actual user-friendly name
            this.name = name;
            this.normalizedName = StringNormalizer.normalizeWithResult(this.name, false);
        } else {
            this.name = null;
            this.normalizedName = null;
        }
    }

    public void setName(String name, boolean generateNormalization) {
        if (generateNormalization) {
            setName(name);
        } else {
            this.name = name;
            this.normalizedName = null;
        }
    }

    /**
     * ID to use in the history
     * (may be different from the one used in the adapter for display)
     */
    public String getHistoryId() {
        return this.id;
    }

    /**
     * ID to use for favorites
     * (may be different from the one used in the adapter for display, or for history)
     */
    public String getFavoriteId() {
        return getHistoryId();
    }

    /**
     * Updates relevance of this pojo with score of given {@code matchInfo} if there is a match.
     *
     * @param matchInfo used for update
     * @param matched   flag to indicate if there was already a match before. If {@code matched} is false relevance is always set to {@link MatchInfo#score}, else it will be only set if {@link MatchInfo#score} is also higher than current value of relevance.
     * @return true, if {@link MatchInfo#match} is true and relevance was updated, else value of {@code matched} is returned as is
     */
    public boolean updateMatchingRelevance(MatchInfo matchInfo, boolean matched) {
        if (matchInfo.match && (!matched || matchInfo.score > relevance)) {
            this.relevance = matchInfo.score;
            return true;
        }
        return matched;
    }

    public boolean isDisabled() {
        return false;
    }
}
