package fr.neamar.kiss.pojo;

import java.util.Comparator;

public class PojoComparator implements Comparator<Pojo> {

    public PojoComparator() {
        super();
    }

    @Override
    public int compare(Pojo lhs, Pojo rhs) {
        if (lhs.relevance == rhs.relevance) {
            if (lhs.normalizedName != null && rhs.normalizedName != null)
                return lhs.normalizedName.compareTo(rhs.normalizedName);
            else
                return lhs.name.compareTo(rhs.name);
        }
        return lhs.relevance - rhs.relevance;
    }
}
