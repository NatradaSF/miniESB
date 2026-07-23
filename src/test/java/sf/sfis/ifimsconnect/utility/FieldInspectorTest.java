package sf.sfis.ifimsconnect.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the reflection-based {@link FieldInspector} helpers
 * used during transform (tracking changed fields, null detection, HOLD blanking).
 */
class FieldInspectorTest {

	/** Minimal POJO covering the field types FieldInspector cares about. */
	static class Sample {
		public String a;
		public BigDecimal b;
		public Integer c; // neither String nor BigDecimal -> ignored by getNonNullFields
		public String hold;
	}

	@Test
	@DisplayName("getNonNullFields returns only non-null String/BigDecimal field names")
	void getNonNullFields_filtersByType() {
		Sample s = new Sample();
		s.a = "x";
		s.b = BigDecimal.ONE;
		s.c = 5; // ignored (Integer)
		s.hold = null; // ignored (null)

		assertThat(FieldInspector.getNonNullFields(s)).containsExactlyInAnyOrder("a", "b");
	}

	@Test
	@DisplayName("allFieldsAreNull: true for fresh object and for empty strings")
	void allFieldsAreNull_emptyTreatedAsNull() {
		assertThat(FieldInspector.allFieldsAreNull(new Sample())).isTrue();

		Sample emptyString = new Sample();
		emptyString.a = ""; // empty string counts as "null" per the helper's contract
		assertThat(FieldInspector.allFieldsAreNull(emptyString)).isTrue();
	}

	@Test
	@DisplayName("allFieldsAreNull: false once any field holds a real value")
	void allFieldsAreNull_falseWhenPopulated() {
		Sample s = new Sample();
		s.a = "value";
		assertThat(FieldInspector.allFieldsAreNull(s)).isFalse();
	}

	@Test
	@DisplayName("replaceHoldWithEmpty replaces only String fields equal to HOLD with a single space")
	void replaceHoldWithEmpty() {
		Sample s = new Sample();
		s.hold = "HOLD";
		s.a = "keep";

		FieldInspector.replaceHoldWithEmpty(s);

		assertThat(s.hold).isEqualTo(" ");
		assertThat(s.a).isEqualTo("keep");
	}
}
