package fr.neamar.kiss.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class PhoneUtilsTest {

    @Test
    void trailingPhoneNumberIsExtracted() {
        assertThat(PhoneUtils.getTrailingPhoneNumber("Voice call +1 (234) 567-890"), equalTo("+1 (234) 567-890"));
        assertThat(PhoneUtils.getTrailingPhoneNumber("Message +33 6 12.34.56.78"), equalTo("+33 6 12.34.56.78"));
        assertThat(PhoneUtils.getTrailingPhoneNumber("0612345678"), equalTo("0612345678"));
    }

    @Test
    void noTrailingPhoneNumber() {
        assertThat(PhoneUtils.getTrailingPhoneNumber("john@example.com"), nullValue());
        assertThat(PhoneUtils.getTrailingPhoneNumber("Signal Message"), nullValue());
        assertThat(PhoneUtils.getTrailingPhoneNumber(""), nullValue());
        assertThat(PhoneUtils.getTrailingPhoneNumber(null), nullValue());
    }
}
