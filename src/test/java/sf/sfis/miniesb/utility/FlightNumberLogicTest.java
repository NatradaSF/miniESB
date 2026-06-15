package sf.sfis.miniesb.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the pure flight-number parsing logic in
 * {@link TranformFidsAfttab}. These pin the exact padding / formatting rules
 * so any accidental change to the parsing behaviour is caught before deploy.
 */
class FlightNumberLogicTest {

	@Test
	@DisplayName("parse with 2-letter airline code (alc2): prefix padded to 3, number padded to width")
	void parse_alc2_basic() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("TG123", "", "TG");

		assertThat(parts).isNotEmpty();
		assertThat(parts.get("prefix")).isEqualTo("TG "); // 2-letter code padded with one space
		assertThat(parts.get("number")).isEqualTo("123  "); // 3-digit number + 2 trailing spaces
		assertThat(parts.get("suffix")).isEqualTo("");
	}

	@Test
	@DisplayName("parse with trailing letter suffix: number zero-padded to 3, suffix kept")
	void parse_alc2_withSuffix() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("TG7A", "", "TG");

		assertThat(parts.get("prefix")).isEqualTo("TG ");
		assertThat(parts.get("number")).isEqualTo("007  "); // 7 -> 007
		assertThat(parts.get("suffix")).isEqualTo("A");
	}

	@Test
	@DisplayName("parse with 3-letter airline code (alc3): prefix not padded")
	void parse_alc3() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("THA0456", "THA", "TG");

		assertThat(parts.get("prefix")).isEqualTo("THA"); // 3-letter code unchanged
		assertThat(parts.get("number")).isEqualTo("456  "); // 0456 -> 456
		assertThat(parts.get("suffix")).isEqualTo("");
	}

	@Test
	@DisplayName("parse 4-digit number: only one trailing space")
	void parse_fourDigitNumber() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("TG1234", "", "TG");

		assertThat(parts.get("number")).isEqualTo("1234 "); // 4 digits -> single trailing space
	}

	@Test
	@DisplayName("flight number matching neither code returns empty map")
	void parse_noMatch() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("XY100", "THA", "TG");

		assertThat(parts).isEmpty();
	}

	@Test
	@DisplayName("getVial pads to fixed 120-char layout")
	void getVial_fixedLayout() {
		String vial = new TranformFidsAfttab(null, null).getVial("ABC", "WXYZ");

		assertThat(vial).startsWith(" ABCWXYZ");
		assertThat(vial).hasSize(120); // " " + via3(3) + via4(4) + 8 * 14-space pad
		assertThat(vial.substring(8)).isBlank(); // everything after the codes is whitespace
	}
}
