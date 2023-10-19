package fr.neamar.kiss.pojo;

import java.util.Comparator;

/**
 * Comparator for comparing {@link Pojo} by name.
 */
public class NameComparator implements Comparator<Pojo> {
    @Override
    public int compare(Pojo lhs, Pojo rhs) {
        int result;
        if (lhs.normalizedName != null && rhs.normalizedName != null) {
            if ((result = lhs.normalizedName.compareTo(rhs.normalizedName)) != 0) {
                return result;
            }
        }
        if (lhs.getName() != null && rhs.getName() != null) {
            if ((result = lhs.getName().compareTo(rhs.getName())) != 0) {
                return result;
            }
        }
        return 0;
    }
}
