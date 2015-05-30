package fr.neamar.kiss.pojo;

import java.util.Comparator;

public class HolderComparator implements Comparator<Pojo> {
	@Override
	public int compare(Pojo lhs, Pojo rhs) {
		return (lhs.relevance < rhs.relevance ? 1 : (lhs.relevance == rhs.relevance ? 0 : -1));
	}
}