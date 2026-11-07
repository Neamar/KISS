package fr.neamar.kiss.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class MimeTypeUtilsTest {

    @Test
    void trailingPhoneNumberIsExtracted() {
        assertThat(MimeTypeUtils.getTrailingPhoneNumber("Voice call +1 (234) 567-890"), equalTo("+1 (234) 567-890"));
        assertThat(MimeTypeUtils.getTrailingPhoneNumber("Message +33 6 12.34.56.78"), equalTo("+33 6 12.34.56.78"));
        assertThat(MimeTypeUtils.getTrailingPhoneNumber("0612345678"), equalTo("0612345678"));
    }

    @Test
    void noTrailingPhoneNumber() {
        assertThat(MimeTypeUtils.getTrailingPhoneNumber("john@example.com"), nullValue());
        assertThat(MimeTypeUtils.getTrailingPhoneNumber("Signal Message"), nullValue());
        assertThat(MimeTypeUtils.getTrailingPhoneNumber(""), nullValue());
        assertThat(MimeTypeUtils.getTrailingPhoneNumber(null), nullValue());
    }
}
