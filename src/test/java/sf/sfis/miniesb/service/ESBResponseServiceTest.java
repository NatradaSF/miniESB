package sf.sfis.miniesb.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link ESBResponseService#getContentBody(String)} — the
 * pure string extraction of the &lt;pl_turn&gt; fragment. Dependencies are not
 * used by this method, so the service is built with nulls.
 */
class ESBResponseServiceTest {

	private final ESBResponseService service =
			new ESBResponseService(null, null, null, null, null, null, null, null);

	@Test
	@DisplayName("getContentBody extracts the pl_turn fragment inclusive of tags")
	void extractsPlTurnFragment() {
		String xml = "<root>\n<pl_turn>payload</pl_turn>\n</root>";

		assertThat(service.getContentBody(xml)).isEqualTo("<pl_turn>payload</pl_turn>");
	}

	@Test
	@DisplayName("getContentBody strips blank lines inside the fragment")
	void stripsBlankLines() {
		String xml = "<pl_turn>\n   \n<a>1</a>\n</pl_turn>";

		assertThat(service.getContentBody(xml)).isEqualTo("<pl_turn>\n<a>1</a>\n</pl_turn>");
	}

	@Test
	@DisplayName("getContentBody returns null when no pl_turn element is present")
	void returnsNullWhenAbsent() {
		assertThat(service.getContentBody("<root><other/></root>")).isNull();
	}
}
