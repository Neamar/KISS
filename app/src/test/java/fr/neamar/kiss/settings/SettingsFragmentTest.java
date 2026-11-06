package fr.neamar.kiss.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.neamar.kiss.R;

public class SettingsFragmentTest {
	private static final File XML_DIR = new File("src/main/res/xml");

	/**
	 * Default values are only written for XMLs listed in SettingsFragment.PREFERENCES_XML
	 */
	@ParameterizedTest
	@MethodSource("preferencesWithDefaultValues")
	public void testDefaultValuesAreWritten(String name) throws ReflectiveOperationException {
		int id = R.xml.class.getField(name).getInt(null);
		List<Integer> listed = Arrays.stream(SettingsFragment.PREFERENCES_XML).boxed().collect(Collectors.toList());
		assertThat(name + " is missing from SettingsFragment.PREFERENCES_XML", listed, hasItem(id));
	}

	@Test
	public void testPreferencesAreFound() throws IOException {
		assertThat(preferencesWithDefaultValues().collect(Collectors.toList()), not(empty()));
	}

	private static Stream<String> preferencesWithDefaultValues() throws IOException {
		File[] files = XML_DIR.listFiles((dir, name) -> name.startsWith("preferences") && name.endsWith(".xml"));
		if (files == null) {
			throw new IOException("Unable to list " + XML_DIR.getAbsolutePath());
		}
		return Arrays.stream(files)
				.filter(file -> {
					try {
						return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).contains("defaultValue");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
				.map(file -> file.getName().replace(".xml", ""));
	}
}
