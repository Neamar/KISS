package fr.neamar.kiss.pojo;

/**
 * Comparator for comparing {@link Pojo} by name with reversed order.
 */
public class ReversedNameComparator extends NameComparator {

    @Override
    public int compare(Pojo lhs, Pojo rhs) {
        return super.compare(rhs, lhs);
    }
}
