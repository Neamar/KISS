package fr.neamar.kiss.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class LoadAppPojosTest {
    private static final int SCHEME = "app://".length();

    private static Map<String, String> replaced(List<String> stored, List<String> installed) {
        return LoadAppPojos.getReplacedActivities(stored, installed, SCHEME, '/');
    }

    @Test
    void movesToNewActivityOfSamePackage() {
        assertThat(replaced(List.of("app://org.mozilla/Old"), List.of("app://org.mozilla/New", "app://other/Main")),
                equalTo(Map.of("app://org.mozilla/Old", "app://org.mozilla/New")));
    }

    @Test
    void keepsUserProfile() {
        assertThat(replaced(List.of("app://org.mozilla/Old/10"), List.of("app://org.mozilla/New", "app://org.mozilla/New/10")),
                equalTo(Map.of("app://org.mozilla/Old/10", "app://org.mozilla/New/10")));
    }

    @Test
    void customNameKeys() {
        assertThat(LoadAppPojos.getReplacedActivities(List.of("org.mozilla/Old#10"), List.of("org.mozilla/New", "org.mozilla/New#10"), 0, '#'),
                equalTo(Map.of("org.mozilla/Old#10", "org.mozilla/New#10")));
    }

    @Test
    void ignoresInstalledActivities() {
        assertThat(replaced(List.of("app://org.mozilla/Main"), List.of("app://org.mozilla/Main")), anEmptyMap());
    }

    @Test
    void ignoresPackagesWithSeveralActivities() {
        assertThat(replaced(List.of("app://org.mozilla/Old"), List.of("app://org.mozilla/A", "app://org.mozilla/B")), anEmptyMap());
    }

    @Test
    void doesNotOverwriteExistingData() {
        assertThat(replaced(List.of("app://org.mozilla/Old", "app://org.mozilla/New"), List.of("app://org.mozilla/New")), anEmptyMap());
    }

    @Test
    void movesOnlyOneOfSeveralRemovedActivities() {
        assertThat(replaced(Arrays.asList("app://org.mozilla/Old1", "app://org.mozilla/Old2"), List.of("app://org.mozilla/New")).size(), equalTo(1));
    }

    @Test
    void ignoresUninstalledPackages() {
        assertThat(replaced(List.of("app://gone/Main"), Collections.emptyList()), anEmptyMap());
    }
}
