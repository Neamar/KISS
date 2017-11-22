package fr.neamar.kiss.pojo;

import java.util.Comparator;

public class PojoComparator implements Comparator<Pojo> {

    boolean bIncreasing;
    public PojoComparator()
    {
        super();
        bIncreasing = false;
    }

    public PojoComparator( boolean increasing )
    {
        this.bIncreasing = increasing;
    }

    @Override
    public int compare(Pojo lhs, Pojo rhs)
    {
        if ( lhs.relevance == rhs.relevance )
            return bIncreasing ? lhs.nameNormalized.compareTo( rhs.nameNormalized ) : rhs.nameNormalized.compareTo( lhs.nameNormalized );
        return bIncreasing ? lhs.relevance - rhs.relevance : rhs.relevance - lhs.relevance;
    }
}
