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
            return lhs.getName().compareTo(rhs.getName());
        }

        return 0;
    }
}
