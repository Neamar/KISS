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
        if ( bIncreasing )
            return lhs.relevance - rhs.relevance;
        else
            return rhs.relevance - lhs.relevance;
    }
}
