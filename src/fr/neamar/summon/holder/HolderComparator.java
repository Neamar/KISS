package fr.neamar.summon.holder;

import java.util.Comparator;

public class HolderComparator implements Comparator<Holder> {
	@Override
	public int compare(Holder lhs, Holder rhs) {
		return (lhs.relevance < rhs.relevance ? 1
				: (lhs.relevance == rhs.relevance ? 0 : -1));
	}
}