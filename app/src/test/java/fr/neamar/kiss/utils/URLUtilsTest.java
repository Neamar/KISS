package fr.neamar.kiss.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class URLUtilsTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "kiss.neamar.fr",
            "example.com",
            "example.co.uk",
            "www.example.com/",
            "example.com/path?q=1",
            "example.com:8080/path",
            "example.com#anchor",
            "https://example.com",
            "https://example.com/path?q=1",
            "https://www.google.com/search?q=%s",
            "http://my-site.example.org:8080/a/b",
            "bücher.de",
            "https://bücher.de/path",
            "xn--bcher-kva.de",
            "192.168.1.1",
            "192.168.1.1/admin",
            "http://10.0.0.1:8080",
            "http://10.0.0.1:8080/path",
            "1password.com",
            "web.3dprint",
    })
    void matchesUrls(String query) {
        assertThat(URLUtils.matchesUrlPattern(query), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "kiss",
            "localhost",
            "localhost:8080",
            "example.",
            ".com",
            "0.1+0.2",
            "1.5",
            "2.5*3",
            "1.2.3",
            "10.0.0",
            "3.14159",
            "v1.2",
            "1.2.3.4.5",
            "http://0.1+0.2",
            "example.com is great",
            "foo bar.com",
    })
    void doesNotMatchNonUrls(String query) {
        assertThat(URLUtils.matchesUrlPattern(query), is(false));
    }
}
