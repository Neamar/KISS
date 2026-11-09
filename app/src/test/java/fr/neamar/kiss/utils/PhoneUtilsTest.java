package fr.neamar.kiss.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneUtilsTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "0612345678",
            "06 12 34 56 78",
            "06.12.34.56.78",
            "06-12-34-56-78",
            "+33612345678",
            "+33 6 12 34 56 78",
            "+1 (555) 123-4567",
            "(555) 123-4567",
            "*#06#",
            "*#*#4636#*#*",
            "*100#",
            "#31#0612345678",
            "*31#+33612345678",
            "**21*+33612345678#",
    })
    void acceptsPhoneNumbers(String query) {
        assertThat(PhoneUtils.isPhoneNumber(query), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "12",
            "abc",
            "12*3+4",
            "1+2",
            "1+2+3",
            "123+456",
            "++33612345678",
            "+33+612345678",
            "1*2/3",
            "12a34",
    })
    void rejectsNonPhoneNumbers(String query) {
        assertThat(PhoneUtils.isPhoneNumber(query), is(false));
    }
}
