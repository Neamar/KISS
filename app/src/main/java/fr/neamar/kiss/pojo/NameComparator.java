package fr.neamar.kiss.pojo;

import java.util.Comparator;

/**
 * Comparator for comparing {@link Pojo} by name.
 */
public class NameComparator implements Comparator<Pojo> {
    @Override
    public int compare(Pojo lhs, Pojo rhs) {
        if (lhs.normalizedName != null && rhs.normalizedName != null) {
            int result = lhs.normalizedName.compareTo(rhs.normalizedName);
            if (result != 0) {
                return result;
            }
        }

        if (lhs.getName() != null && rhs.getName() != null) {
            int result = lhs.getName().compareTo(rhs.getName());
            if (result != 0) {
                return result;
            }
        }

        if (lhs.getUserHandle() != null && rhs.getUserHandle() != null) {
            return lhs.getUserHandle().compareTo(rhs.getUserHandle());
        }

        return 0;
    }
}
