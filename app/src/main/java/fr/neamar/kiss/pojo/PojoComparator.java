package fr.neamar.kiss.pojo;

import java.util.Comparator;

public class PojoComparator implements Comparator<Pojo> {
    @Override
    public int compare(Pojo lhs, Pojo rhs) {
        return rhs.relevance - lhs.relevance;
    }
}