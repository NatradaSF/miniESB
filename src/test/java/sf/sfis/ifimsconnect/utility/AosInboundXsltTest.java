package sf.sfis.ifimsconnect.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.Xslt30Transformer;

/**
 * พิสูจน์ว่า aos_inbound.xsl (Saxon) ปั้น AODB Envelope ได้ "เหมือน" โค้ด JAXB เดิม
 * (ESBRequestService.setBhs) — เทียบกับ golden snapshot ที่บันทึกผลของโค้ดปัจจุบันไว้
 * ({@code bhs-departure.xml} / {@code bhs-arrival.xml}) แบบ semantic ด้วย XMLUnit
 * (ignore whitespace/formatting เพราะ JAXB กับ XSLT serialize ไม่เหมือนกัน byte ต่อ byte
 * แต่โครง/ค่า/namespace ต้องตรง).
 */
class AosInboundXsltTest {

	private static final Path SNAPSHOT_DIR = Paths.get("src", "test", "resources", "snapshots");

	// ใช้ input ตัวเดียวกับ ESBRequestServiceSnapshotTest.BHS_SAMPLE
	private static final String BHS_DEP = """
			<?xml version="1.0"?>
			<MSG>
			  <MSGSTREAM_IN>
			    <INFOBJ_GENERIC>
			      <MESSAGETYPE>WMMUUD</MESSAGETYPE>
			      <MESSAGEORIGIN>BHS</MESSAGEORIGIN>
			      <TIMEID>UTC</TIMEID>
			      <HOPO>BKK</HOPO>
			      <TIMESTAMP>20260512003022</TIMESTAMP>
			      <ACTIONTYPE>U</ACTIONTYPE>
			      <ADID>D</ADID>
			      <STDT>20260511162500</STDT>
			      <FLNO>8M 0373 </FLNO>
			    </INFOBJ_GENERIC>
			    <MSGOBJECTS>
			      <INFOBJ_MUINFO>
			        <BAZ1>05   </BAZ1>
			        <BAZ4>     </BAZ4>
			        <BAO1>202605111225</BAO1>
			        <BAC1>202605111730</BAC1>
			        <BAO4>            </BAO4>
			        <BAC4>            </BAC4>
			      </INFOBJ_MUINFO>
			    </MSGOBJECTS>
			  </MSGSTREAM_IN>
			</MSG>
			""";

	@Test
	@DisplayName("XSLT bhs departure == current JAXB output (semantic)")
	void bhsDepartureMatches() throws Exception {
		assertMatchesGolden(transform(BHS_DEP), "bhs-departure.xml");
	}

	@Test
	@DisplayName("XSLT bhs arrival == current JAXB output (semantic)")
	void bhsArrivalMatches() throws Exception {
		String arr = BHS_DEP.replace("<ADID>D</ADID>", "<ADID>A</ADID>");
		assertMatchesGolden(transform(arr), "bhs-arrival.xml");
	}

	// flight input = MSG ที่ ESBRequestServiceSnapshotTest.buildMsg สร้าง (generic + INFOBJ_FLIGHT)
	// STOA/STOD/CSGN ฝั่ง Java inject จาก generic → ไม่มีใน input XML (XSL อ่านจาก generic เอง)
	private static final String FLIGHT_DEP = """
			<MSG><MSGSTREAM_IN>
			  <INFOBJ_GENERIC>
			    <MESSAGEORIGIN>FIDS</MESSAGEORIGIN>
			    <HOPO>BKK</HOPO>
			    <ADID>D</ADID>
			    <STDT>20260615043000</STDT>
			    <CSGN>MMA338</CSGN>
			    <FLNO>8M338</FLNO>
			    <TIMESTAMP>20260615034320</TIMESTAMP>
			  </INFOBJ_GENERIC>
			  <MSGOBJECTS>
			    <INFOBJ_FLIGHT>
			      <CTOT>20260615041000</CTOT>
			      <ASRT>20260615043000</ASRT>
			      <TSAT>20260615041500</TSAT>
			      <RWYD>01</RWYD>
			    </INFOBJ_FLIGHT>
			  </MSGOBJECTS>
			</MSGSTREAM_IN></MSG>
			""";

	private static final String FLIGHT_ARR = """
			<MSG><MSGSTREAM_IN>
			  <INFOBJ_GENERIC>
			    <MESSAGEORIGIN>FIDS</MESSAGEORIGIN>
			    <HOPO>BKK</HOPO>
			    <ADID>A</ADID>
			    <STDT>20260615043000</STDT>
			    <CSGN>MMA338</CSGN>
			    <FLNO>8M338</FLNO>
			    <TIMESTAMP>20260615034320</TIMESTAMP>
			  </INFOBJ_GENERIC>
			  <MSGOBJECTS>
			    <INFOBJ_FLIGHT>
			      <TLDT>20260615033000</TLDT>
			      <RWYA>01</RWYA>
			      <IFRA>I</IFRA>
			    </INFOBJ_FLIGHT>
			  </MSGOBJECTS>
			</MSGSTREAM_IN></MSG>
			""";

	@Test
	@DisplayName("XSLT flight departure == current JAXB output (semantic)")
	void flightDepartureMatches() throws Exception {
		assertMatchesGolden(transform(FLIGHT_DEP), "esbreq-departure.xml");
	}

	@Test
	@DisplayName("XSLT flight arrival == current JAXB output (semantic)")
	void flightArrivalMatches() throws Exception {
		assertMatchesGolden(transform(FLIGHT_ARR), "esbreq-arrival.xml");
	}

	// vdgs/bulkdata input = raw MSG เหมือน ESBRequestServiceSnapshotTest
	private static final String VDGS_DEP = """
			<MSG><MSGSTREAM_IN>
			  <INFOBJ_GENERIC><MESSAGEORIGIN>VDGS</MESSAGEORIGIN><HOPO>BKK</HOPO><TIMESTAMP>20260615034320</TIMESTAMP>
			    <ADID>D</ADID><STDT>20260615043000</STDT><FLNO>8M 338</FLNO></INFOBJ_GENERIC>
			  <MSGOBJECTS><INFOBJ_VDGS><VDGSDEP>
			    <PSTD>514</PSTD><ACT5>B77L</ACT5><OFBL>20260615041000</OFBL>
			  </VDGSDEP></INFOBJ_VDGS></MSGOBJECTS>
			</MSGSTREAM_IN></MSG>
			""";

	private static final String BULK_SITA = """
			<MSG><MSGSTREAM_IN>
			  <INFOBJ_GENERIC><MESSAGEORIGIN>SITA</MESSAGEORIGIN><HOPO>BKK</HOPO><TIMESTAMP>20260615034320</TIMESTAMP><ADID>A</ADID></INFOBJ_GENERIC>
			  <MSGOBJECTS><BULKDATA><SITA><FILE_NAME>x.snd</FILE_NAME><CONTENT>HELLO SITA CONTENT</CONTENT></SITA></BULKDATA></MSGOBJECTS>
			</MSGSTREAM_IN></MSG>
			""";

	@Test
	@DisplayName("XSLT vdgs departure == current JAXB output (semantic)")
	void vdgsDepartureMatches() throws Exception {
		assertMatchesGolden(transform(VDGS_DEP), "vdgs-departure.xml");
	}

	@Test
	@DisplayName("XSLT vdgs arrival == current JAXB output (semantic)")
	void vdgsArrivalMatches() throws Exception {
		String arr = VDGS_DEP.replace("<ADID>D</ADID>", "<ADID>A</ADID>")
				.replace("<VDGSDEP>", "<VDGSARR>").replace("</VDGSDEP>", "</VDGSARR>")
				.replace("<PSTD>514</PSTD>", "<PSTA>G1</PSTA>")
				.replace("<OFBL>20260615041000</OFBL>", "<ONBL>20260615041000</ONBL>");
		assertMatchesGolden(transform(arr), "vdgs-arrival.xml");
	}

	@Test
	@DisplayName("XSLT bulkdata SITA == current JAXB output (semantic)")
	void bulkSitaMatches() throws Exception {
		assertMatchesGolden(transform(BULK_SITA), "bulk-sita.xml");
	}

	// flight departure ที่ "ไม่มี STDT" (เช่น ATC update ATCUPDM) — JAXB เดิม: STOD=null → ตกจาก
	// nonNullFields → ไม่ emit pd_sobt. XSL ต้องไม่ใส่ <pd_sobt/> ว่างมา (กัน regression ที่เคยเจอ)
	private static final String FLIGHT_NO_STDT = """
			<MSG><MSGSTREAM_IN>
			  <INFOBJ_GENERIC>
			    <MESSAGEORIGIN>ATCA</MESSAGEORIGIN><HOPO>CNX</HOPO><TIMESTAMP>20260512120043</TIMESTAMP>
			    <ADID>D</ADID><CSGN>AIQ4103</CSGN>
			  </INFOBJ_GENERIC>
			  <MSGOBJECTS><INFOBJ_FLIGHT><CTOT>20260512064700</CTOT></INFOBJ_FLIGHT></MSGOBJECTS>
			</MSGSTREAM_IN></MSG>
			""";

	@Test
	@DisplayName("TEMP: print arrival LAND no-STDT (currentDate=20260512...)")
	void tempPrintLand() throws Exception {
		String in = """
				<?xml version="1.0"?>
				<MSG><MSGSTREAM_IN>
				  <INFOBJ_GENERIC>
				    <MESSAGETYPE>ATCUPDM</MESSAGETYPE><MESSAGEORIGIN>ATCA</MESSAGEORIGIN>
				    <TIMEID>UTC</TIMEID><HOPO>BKK</HOPO><TIMESTAMP>20260512000148</TIMESTAMP>
				    <ADID>A</ADID><CSGN>AAR7435</CSGN>
				  </INFOBJ_GENERIC>
				  <MSGOBJECTS><INFOBJ_FLIGHT>
				    <LAND>20260511170147</LAND><RWYA>19</RWYA><IFRA>I</IFRA>
				  </INFOBJ_FLIGHT></MSGOBJECTS>
				</MSGSTREAM_IN></MSG>
				""";
		net.sf.saxon.s9api.Processor proc = new net.sf.saxon.s9api.Processor(false);
		net.sf.saxon.s9api.XsltExecutable exec;
		try (InputStream is = AosInboundXsltTest.class.getResourceAsStream("/aos_inbound.xsl")) {
			exec = proc.newXsltCompiler().compile(new StreamSource(is));
		}
		StringWriter sw = new StringWriter();
		net.sf.saxon.s9api.Serializer out = proc.newSerializer(sw);
		out.setOutputProperty(net.sf.saxon.s9api.Serializer.Property.INDENT, "yes");
		net.sf.saxon.s9api.Xslt30Transformer t = exec.load30();
		t.setStylesheetParameters(java.util.Map.of(
				new net.sf.saxon.s9api.QName("currentDate"),
				new net.sf.saxon.s9api.XdmAtomicValue("20260512000000")));
		t.transform(new StreamSource(new StringReader(in)), out);
		System.out.println("=====LAND_OUTPUT_START=====");
		System.out.println(sw.toString());
		System.out.println("=====LAND_OUTPUT_END=====");
	}

	@Test
	@DisplayName("flight departure ไม่มี STDT → ไม่ emit pd_sobt (ตรงกับ JAXB)")
	void flightNoStdtOmitsSobt() throws Exception {
		String out = transform(FLIGHT_NO_STDT);
		assertThat(out).contains("<pd_callsign>AIQ4103</pd_callsign>")
				.contains("<pd_ctot>2026-05-12T06:47:00Z</pd_ctot>")
				.doesNotContain("pd_sobt");   // STDT หาย → ต้องไม่มี pd_sobt เลย
	}

	private void assertMatchesGolden(String actual, String goldenName) throws Exception {
		String golden = new String(Files.readAllBytes(SNAPSHOT_DIR.resolve(goldenName)), StandardCharsets.UTF_8);
		Diff diff = DiffBuilder.compare(golden).withTest(actual)
				.ignoreWhitespace()
				.checkForSimilar()
				.build();
		assertThat(diff.hasDifferences())
				.as("XSLT output differs from current JAXB golden %s:%n%s%n--- actual ---%n%s",
						goldenName, diff.toString(), actual)
				.isFalse();
	}

	/** transform inbound MSG ผ่าน aos_inbound.xsl (Saxon s9api — รองรับ XSLT 2.0/3.0). */
	private static String transform(String inXml) throws Exception {
		Processor proc = new Processor(false);
		XsltCompiler comp = proc.newXsltCompiler();
		XsltExecutable exec;
		try (InputStream is = AosInboundXsltTest.class.getResourceAsStream("/aos_inbound.xsl")) {
			exec = comp.compile(new StreamSource(is));
		}
		StringWriter sw = new StringWriter();
		Serializer out = proc.newSerializer(sw);
		out.setOutputProperty(Serializer.Property.METHOD, "xml");
		out.setOutputProperty(Serializer.Property.INDENT, "yes");
		Xslt30Transformer t = exec.load30();
		t.transform(new StreamSource(new StringReader(inXml)), out);
		return sw.toString();
	}
}
