package fr.neamar.summon.record;

import java.util.Comparator;

public class RecordComparator implements Comparator<Record> {
	@Override
	public int compare(Record lhs, Record rhs) {
		return (lhs.relevance < rhs.relevance ? 1 : (lhs.relevance == rhs.relevance ? 0 : -1));
	}
}