package fr.neamar.kiss.pojo;

/**
 * Comparator for comparing {@link Pojo} by relevance.
 * If relevance is equal then compare by name with reversed order.
 */
public class RelevanceComparator extends ReversedNameComparator {

    @Override
    public int compare(Pojo lhs, Pojo rhs) {
        int result = Integer.compare(lhs.relevance, rhs.relevance);
        if (result != 0) {
            return result;
        }

        return super.compare(lhs, rhs);
    }
}
