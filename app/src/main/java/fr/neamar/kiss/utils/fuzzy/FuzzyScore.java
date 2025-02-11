package fr.neamar.kiss.utils.fuzzy;

public interface FuzzyScore {

    FuzzyScore setFullWordBonus(int full_word_bonus);

    FuzzyScore setAdjacencyBonus(int adjacency_bonus);

    FuzzyScore setSeparatorBonus(int separator_bonus);

    FuzzyScore setCamelBonus(int camel_bonus);

    FuzzyScore setLeadingLetterPenalty(int leading_letter_penalty);

    FuzzyScore setMaxLeadingLetterPenalty(int max_leading_letter_penalty);

    FuzzyScore setUnmatchedLetterPenalty(int unmatched_letter_penalty);

    FuzzyScore setFirstLetterBonus(int first_letter_bonus);

    MatchInfo match(CharSequence text);

    MatchInfo match(int[] text);
}
