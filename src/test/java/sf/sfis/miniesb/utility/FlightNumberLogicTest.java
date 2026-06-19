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
	@DisplayName("2-char airline (3rd char = digit): prefix=3, number=4 wide")
	void parse_twoCharAirline() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("TG123");

		assertThat(parts).isNotEmpty();
		assertThat(parts.get("prefix")).isEqualTo("TG "); // 2-char code + 1 space (width 3)
		assertThat(parts.get("number")).isEqualTo("123 "); // 3-digit + 1 space (width 4)
		assertThat(parts.get("suffix")).isEqualTo("");
	}

	@Test
	@DisplayName("trailing letter suffix: number zero-padded to 3, suffix kept")
	void parse_withSuffix() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("TG7A");

		assertThat(parts.get("prefix")).isEqualTo("TG ");
		assertThat(parts.get("number")).isEqualTo("7   "); // 7 ชิดซ้าย เว้น space ท้าย (ไม่เติม 0) = width 4
		assertThat(parts.get("suffix")).isEqualTo("A");
	}

	@Test
	@DisplayName("3rd char = letter → 3-char prefix, not padded")
	void parse_threeCharAirline() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("THA0456");

		assertThat(parts.get("prefix")).isEqualTo("THA"); // 3-char prefix unchanged
		assertThat(parts.get("number")).isEqualTo("0456"); // เก็บตามต้นฉบับ "0456" (ไม่ตัด 0) = width 4
		assertThat(parts.get("suffix")).isEqualTo("");
	}

	@Test
	@DisplayName("4-digit number: no trailing space (already width 4)")
	void parse_fourDigitNumber() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("TG1234");

		assertThat(parts.get("number")).isEqualTo("1234"); // 4 digits -> width 4, no pad
	}

	@Test
	@DisplayName("airline code with digit (B3) handled positionally without alc")
	void parse_airlineWithDigit() {
		Map<String, String> parts = TranformFidsAfttab.parseFlightNumber("B3701R");

		assertThat(parts.get("prefix")).isEqualTo("B3 "); // 3rd char '7' = digit → 2-char prefix
		assertThat(parts.get("number")).isEqualTo("701 ");
		assertThat(parts.get("suffix")).isEqualTo("R");
	}

	@Test
	@DisplayName("J-series cases: prefix = 2 chars + 3rd char if letter")
	void parse_jSeries() {
		// 3rd char = digit → 2-char prefix
		assertThat(TranformFidsAfttab.parseFlightNumber("J9365").get("prefix")).isEqualTo("J9 ");
		assertThat(TranformFidsAfttab.parseFlightNumber("J9365").get("number")).isEqualTo("365 ");
		assertThat(TranformFidsAfttab.parseFlightNumber("J52365").get("prefix")).isEqualTo("J5 ");
		assertThat(TranformFidsAfttab.parseFlightNumber("J52365").get("number")).isEqualTo("2365");
		// 3rd char = letter (T) → 3-char prefix
		assertThat(TranformFidsAfttab.parseFlightNumber("J5T365").get("prefix")).isEqualTo("J5T");
		assertThat(TranformFidsAfttab.parseFlightNumber("J5T365").get("number")).isEqualTo("365 ");
		assertThat(TranformFidsAfttab.parseFlightNumber("J5T2365").get("prefix")).isEqualTo("J5T");
		assertThat(TranformFidsAfttab.parseFlightNumber("J5T2365").get("number")).isEqualTo("2365");
	}

	@Test
	@DisplayName("unparseable input (too short / no flight digits) returns empty map")
	void parse_unparseable() {
		assertThat(TranformFidsAfttab.parseFlightNumber("TG")).isEmpty();     // สั้นกว่า 3 ตัว
		assertThat(TranformFidsAfttab.parseFlightNumber("ABCDE")).isEmpty();  // หลัง prefix ไม่มีตัวเลข
		assertThat(TranformFidsAfttab.parseFlightNumber(null)).isEmpty();
	}

	@Test
	@DisplayName("toFlno: fixed 9-char layout = airline(3) + number(4) + space + suffix/space")
	void toFlno_fixedLayout() {
		assertThat(TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("TG123"))).isEqualTo("TG 123   ");
		assertThat(TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("TG7A"))).isEqualTo("TG 7    A");
		assertThat(TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("TG1234"))).isEqualTo("TG 1234  ");
		assertThat(TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("J5T2365"))).isEqualTo("J5T2365  ");
		assertThat(TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("B3701R"))).isEqualTo("B3 701  R");
		// ทุกค่ายาว 9 ตัวเสมอ
		assertThat(TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("TG123"))).hasSize(9);
		assertThat(TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("J5T2365"))).hasSize(9);
	}

	@Test
	@DisplayName("formatJfno: แต่ละ entry 9-char (เหมือน FLNO) ต่อกันตรงๆ ไม่มี separator")
	void formatJfno_multiple() {
		String a = TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("TG123"));   // "TG 123   "
		String b = TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("8M338"));   // "8M 338   "
		assertThat(TranformFidsAfttab.formatJfno("TG123,8M338")).isEqualTo(a + b);
		assertThat(TranformFidsAfttab.formatJfno("TG123,8M338")).hasSize(18); // 2 × 9
	}

	@Test
	@DisplayName("formatJfno: unparseable kept as-is; null คืน null; ข้ามตัวว่าง")
	void formatJfno_edgeCases() {
		assertThat(TranformFidsAfttab.formatJfno("J5T2365")).isEqualTo("J5T2365  "); // 9-char เดี่ยว
		assertThat(TranformFidsAfttab.formatJfno("XX")).isEqualTo("XX");             // แยกไม่ได้ → ใช้ค่าเดิม
		String a = TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("TG123"));
		String b = TranformFidsAfttab.toFlno(TranformFidsAfttab.parseFlightNumber("8M338"));
		assertThat(TranformFidsAfttab.formatJfno("TG123, ,8M338")).isEqualTo(a + b); // ข้ามตัวว่าง, ไม่มี separator
		assertThat(TranformFidsAfttab.formatJfno(null)).isNull();
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
