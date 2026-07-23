package sf.sfis.ifimsconnect.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import sf.sfis.ifimsconnect.model.FidsAfttab;

/**
 * Regression guard for {@link TranformFidsAfttab#applyVial} — the VIA/routing logic.
 * Calls applyVial directly (without the full XSL pipeline) on a small crafted pl_turn,
 * route = TPE-HDY-SHJ-BKK-CNX-TPE, hopo = BKK.
 *
 * Pins:
 *  - VIA list comes from the FULL route (not only the changed node) — arrival [HDY, SHJ], departure [CNX]
 *  - Vian = count, via3/via4 = airport adjacent to hopo, Vial = getVial of each VIA concatenated
 *  - UPDATE emits VIA only when a routing node changed; DATASET always; UPDATE-unchanged emits nothing
 */
class ApplyVialTest {

	private final TranformFidsAfttab t = new TranformFidsAfttab(null, null);

	/** one <pl_routing> entry; changed=true marks rap_iata3lc with action="update" */
	private static String node(String iata, String icao, boolean changed) {
		String act = changed ? " action=\"update\"" : "";
		return "<pl_routing><prt_rap_refairport><ref_airport>"
				+ "<rap_iata3lc" + act + ">" + iata + "</rap_iata3lc>"
				+ "<rap_icao4lc>" + icao + "</rap_icao4lc>"
				+ "</ref_airport></prt_rap_refairport></pl_routing>";
	}

	private static String routingList(boolean shjChanged, boolean cnxChanged) {
		return node("TPE", "RCTP", false) + node("HDY", "VTSH", false)
				+ node("SHJ", "OMSJ", shjChanged) + node("BKK", "VTBS", false)
				+ node("CNX", "VTCC", cnxChanged) + node("TPE", "RCTP", false);
	}

	/** pl_turn with route TPE-HDY-SHJ-BKK-CNX-TPE; arrival/departure each carry the routing list */
	private static String xml(boolean arrShjChanged, boolean depCnxChanged) {
		return "<pl_turn><pt_routingiata3lc>TPE-HDY-SHJ-BKK-CNX-TPE</pt_routingiata3lc>"
				+ "<pt_pa_arrival><pl_arrival>" + routingList(arrShjChanged, false) + "</pl_arrival></pt_pa_arrival>"
				+ "<pt_pd_departure><pl_departure>" + routingList(false, depCnxChanged) + "</pl_departure></pt_pd_departure>"
				+ "</pl_turn>";
	}

	private static Document parse(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}

	private FidsAfttab run(String xml, String tag, String hopo, boolean isArrival, String actionType) throws Exception {
		Document doc = parse(xml);
		XPath xpath = XPathFactory.newInstance().newXPath();
		Element flightElement = (Element) doc.getElementsByTagName(tag).item(0);
		FidsAfttab f = new FidsAfttab();
		t.applyVial(f, xpath, doc, flightElement, hopo, isArrival, actionType);
		return f;
	}

	@Test
	@DisplayName("arrival + routing changed → Vial มี HDY+SHJ ครบ (จาก route เต็ม), Vian=2, via ติด hopo=SHJ")
	void arrivalRoutingChanged() throws Exception {
		FidsAfttab f = run(xml(true, false), "pl_arrival", "BKK", true, "UPDATE");

		assertThat(f.getVian()).isEqualTo("2");
		assertThat(f.getVia3()).isEqualTo("SHJ"); // adjacent to hopo (last on arrival side)
		assertThat(f.getVia4()).isEqualTo("OMSJ");
		assertThat(f.getVial()).hasSize(240);                      // 2 × getVial(120)
		assertThat(f.getVial()).startsWith(" HDYVTSH");            // via แรก = HDY (ครบ ไม่ใช่แค่ตัวที่เปลี่ยน)
		assertThat(f.getVial().substring(120)).startsWith(" SHJOMSJ"); // via ที่สอง = SHJ
	}

	@Test
	@DisplayName("departure + routing changed → Vial=CNX, Vian=1")
	void departureRoutingChanged() throws Exception {
		FidsAfttab f = run(xml(false, true), "pl_departure", "BKK", false, "UPDATE");

		assertThat(f.getVian()).isEqualTo("1");
		assertThat(f.getVia3()).isEqualTo("CNX");
		assertThat(f.getVia4()).isEqualTo("VTCC");
		assertThat(f.getVial()).hasSize(120);
		assertThat(f.getVial()).startsWith(" CNXVTCC");
	}

	@Test
	@DisplayName("UPDATE + ไม่มี routing เปลี่ยน → ไม่ set VIA เลย (ไม่ส่ง ESB)")
	void updateNoRoutingChange_notSet() throws Exception {
		FidsAfttab f = run(xml(false, false), "pl_arrival", "BKK", true, "UPDATE");

		assertThat(f.getVian()).isNull();
		assertThat(f.getVial()).isNull();
		assertThat(f.getVia3()).isNull();
		assertThat(f.getVia4()).isNull();
	}

	@Test
	@DisplayName("DATASET → set VIA เสมอ แม้ไม่มี routing เปลี่ยน")
	void datasetAlwaysSet() throws Exception {
		FidsAfttab f = run(xml(false, false), "pl_arrival", "BKK", true, "DATASET");

		assertThat(f.getVian()).isEqualTo("2");
		assertThat(f.getVia3()).isEqualTo("SHJ");
		assertThat(f.getVial()).hasSize(240);
	}
}
