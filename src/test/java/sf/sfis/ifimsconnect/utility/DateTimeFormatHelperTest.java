package sf.sfis.ifimsconnect.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link DateTimeFormatHelper}. The helper hardcodes
 * UTC and Asia/Bangkok (UTC+7, no DST), so results are fully deterministic
 * and independent of the machine's system time zone.
 */
class DateTimeFormatHelperTest {

	private final DateTimeFormatHelper helper = new DateTimeFormatHelper();

	@Test
	@DisplayName("convertUTCToLocal adds 7 hours (UTC -> Asia/Bangkok)")
	void utcToLocal() {
		assertThat(helper.convertUTCToLocal("20250524000000")).isEqualTo("20250524070000");
	}

	@Test
	@DisplayName("convertUTCToLocal rolls over to next day")
	void utcToLocal_dayRollover() {
		assertThat(helper.convertUTCToLocal("20250524200000")).isEqualTo("20250525030000");
	}

	@Test
	@DisplayName("convertLocalToUTC subtracts 7 hours (Asia/Bangkok -> UTC)")
	void localToUtc() {
		assertThat(helper.convertLocalToUTC("2025-05-24T07:00:00")).isEqualTo("20250524000000");
	}

	@Test
	@DisplayName("convertLocalToUTC rolls back to previous day")
	void localToUtc_dayRollback() {
		assertThat(helper.convertLocalToUTC("2025-05-24T03:00:00")).isEqualTo("20250523200000");
	}

	@Test
	@DisplayName("calculateTime adds minutes within the same day")
	void calculateTime_sameDay() {
		assertThat(helper.calculateTime("20250524000000", 30)).isEqualTo("20250524003000");
	}

	@Test
	@DisplayName("calculateTime adds minutes across midnight")
	void calculateTime_acrossMidnight() {
		assertThat(helper.calculateTime("20250524235000", 30)).isEqualTo("20250525002000");
	}
}
