package fr.neamar.kiss.broadcast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import java.util.List;

class PackageAddedRemovedHandlerTest {
    private static final int SCHEME = "app://".length();

    private static String replaced(List<String> stored, String newKey) {
        return PackageAddedRemovedHandler.getReplacedKey(stored, newKey, SCHEME, '/');
    }

    @Test
    void findsOldActivityOfSamePackage() {
        assertThat(replaced(List.of("app://other/Main", "app://org.mozilla/Old"), "app://org.mozilla/New"), equalTo("app://org.mozilla/Old"));
    }

    @Test
    void keepsUserProfile() {
        assertThat(replaced(List.of("app://org.mozilla/Old", "app://org.mozilla/Old/10"), "app://org.mozilla/New/10"), equalTo("app://org.mozilla/Old/10"));
        assertThat(replaced(List.of("app://org.mozilla/Old/10"), "app://org.mozilla/New"), nullValue());
    }

    @Test
    void customNameKeys() {
        assertThat(PackageAddedRemovedHandler.getReplacedKey(List.of("org.mozilla/Old#10"), "org.mozilla/New#10", 0, '#'), equalTo("org.mozilla/Old#10"));
    }

    @Test
    void doesNotOverwriteExistingData() {
        assertThat(replaced(List.of("app://org.mozilla/Old", "app://org.mozilla/New"), "app://org.mozilla/New"), nullValue());
    }

    @Test
    void ignoresPackagesSharingAPrefix() {
        assertThat(replaced(List.of("app://org.mozilla.focus/Main", "contact://12"), "app://org.mozilla/New"), nullValue());
    }
}
