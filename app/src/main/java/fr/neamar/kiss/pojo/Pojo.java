package fr.neamar.kiss.pojo;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.normalizer.StringNormalizer;

public abstract class Pojo {
    // Globally unique ID.
    // Usually starts with provider scheme, e.g. "app://" or "contact://" to
    // ensure unique constraint
    public String id = "(none)";
    // normalized name, for faster search
    public StringNormalizer.Result normalizedName = null;
    // Lower-cased name, for faster search
    //public String nameNormalized = "";
    // How relevant is this record ? The higher, the most probable it will be
    // displayed
    public int relevance = 0;
    // Name for this pojo, e.g. app name
    String name = "";

    public List<Pair<Integer, Integer>> nameMatchPositions = new ArrayList<>();

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

    public void clearNameHighlight() {
        nameMatchPositions.clear();
    }

    /**
     * Set which area of the display name should marked as highlighted.
     * <p/>
     * The start and end positions should be offsets in the normalized string and will be converted
     * to their non-normalized positions before they are used.
     *
     * @param positionNormalizedStart Highlighting start position in normalized name
     * @param positionNormalizedEnd   Highlighting end position in normalized name
     */
    public void setNameHighlight(int positionNormalizedStart, int positionNormalizedEnd) {
        clearNameHighlight();
        setHighlight(nameMatchPositions, normalizedName, positionNormalizedStart, positionNormalizedEnd);
    }

    public void setNameHighlight(List<Pair<Integer, Integer>> positions) {
        clearNameHighlight();
        setHighlight(nameMatchPositions, normalizedName, positions);
    }

    protected void setHighlight(List<Pair<Integer, Integer>> matchPositions, StringNormalizer.Result string,
                                int positionNormalizedStart, int positionNormalizedEnd) {
        int positionStart = string.mapPosition(positionNormalizedStart);
        int positionEnd = string.mapPosition(positionNormalizedEnd);

        matchPositions.add(new Pair<Integer, Integer>(positionStart, positionEnd));
    }

    protected void setHighlight(List<Pair<Integer, Integer>> matchPositions, StringNormalizer.Result string,
                                List<Pair<Integer, Integer>> positions) {
        for (Pair<Integer, Integer> position : positions) {
            int positionStart = string.mapPosition(position.first);
            int positionEnd = string.mapPosition(position.second);
            matchPositions.add(new Pair<Integer, Integer>(positionStart, positionEnd));
        }
    }
}
