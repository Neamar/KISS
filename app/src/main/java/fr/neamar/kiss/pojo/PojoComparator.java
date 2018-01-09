package fr.neamar.kiss.pojo;

import java.util.Comparator;

public class PojoComparator implements Comparator<Pojo> {

    public PojoComparator()
    {
        super();
    }

    @Override
    public int compare(Pojo lhs, Pojo rhs)
    {
        if ( lhs.relevance == rhs.relevance )
            return lhs.nameNormalized.compareTo( rhs.nameNormalized );
        return lhs.relevance - rhs.relevance;
    }
}
