package sf.sfis.ifimsconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import sf.sfis.ifimsconnect.MQArtemisProducer;
import sf.sfis.ifimsconnect.esb.realtimeinbound.ADID;
import sf.sfis.ifimsconnect.esb.realtimeinbound.MSG;

/**
 * Characterization (snapshot) test for {@link ESBRequestService#processXmlMessage(String)} —
 * the ESB-inbound MSG -> AODB Envelope mapping (setFlight). Pins the marshalled
 * AODB XML that gets sent to Artemis, so a refactor of setFlight can be proven
 * to produce byte-identical output.
 *
 * <p>Input is built as a JAXB object and marshalled (so element names are exact,
 * never guessed). Output is captured from a mocked MQArtemisProducer.
 *
 * <p>Inputs deliberately avoid the airb/land branches because those call
 * getCurrentDate() (non-deterministic). All other date fields are fixed strings.
 *
 * <p>Golden files are created on first run, compared afterwards (delete to regen).
 */
class ESBRequestServiceSnapshotTest {

	private static final Path SNAPSHOT_DIR = Paths.get("src", "test", "resources", "snapshots");

	@Test
	@DisplayName("processXmlMessage departure mapping matches snapshot")
	void departureMapping() throws Exception {
		MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJFLIGHT flt = new MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJFLIGHT();
		flt.setCTOT("20260615041000");
		flt.setASRT("20260615043000");
		flt.setTSAT("20260615041500");
		flt.setRWYD("01");
		verifySnapshot("esbreq-departure", runProcess(buildMsg(ADID.D, flt)));
	}

	@Test
	@DisplayName("processXmlMessage arrival mapping matches snapshot")
	void arrivalMapping() throws Exception {
		MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJFLIGHT flt = new MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJFLIGHT();
		flt.setTLDT("20260615033000");
		flt.setRWYA("01");
		flt.setIFRA("I");
		verifySnapshot("esbreq-arrival", runProcess(buildMsg(ADID.A, flt)));
	}

	private static final String BHS_SAMPLE = """
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
	@DisplayName("BHS make-up (ADID=D) maps to pl_departurebelt, matches snapshot")
	void bhsDeparture() throws Exception {
		verifySnapshot("bhs-departure", runProcessXml(BHS_SAMPLE));
	}

	@Test
	@DisplayName("BHS make-up (ADID=A) maps to pl_baggagebelt, matches snapshot")
	void bhsArrival() throws Exception {
		verifySnapshot("bhs-arrival", runProcessXml(BHS_SAMPLE.replace("<ADID>D</ADID>", "<ADID>A</ADID>")));
	}

	/**
	 * Tripwire: ปัจจุบัน setMessageId ใน processXmlMessage ถูก comment ไว้ → output ต้อง "ไม่มี"
	 * &lt;aodb:message-id&gt; (golden snapshot ทั้งหมดจึงไม่มี field นี้ด้วย).
	 *
	 * <p>ถ้าอนาคต "เปิดใช้ message-id" (uncomment control.setMessageId ใน
	 * {@link ESBRequestService#processXmlMessage}) เทสนี้จะ fail โดยตั้งใจ เพื่อบังคับให้:
	 * (1) เปลี่ยนเทสนี้เป็นยืนยันค่า message-id ที่คาดหวัง และ
	 * (2) regenerate golden snapshot ทั้ง 4 ไฟล์ (ลบไฟล์แล้วรันเทสใหม่).
	 */
	@Test
	@DisplayName("message-id absent by default (tripwire for when setMessageId is re-enabled)")
	void messageIdAbsentByDefault() {
		String out = runProcessXml(BHS_SAMPLE);
		assertThat(out).doesNotContain("<aodb:message-id>");
	}

	/** Runs processXmlMessage with a raw XML string, returns the XML sent to Artemis. */
	private String runProcessXml(String inputXml) {
		MQArtemisProducer producer = mock(MQArtemisProducer.class);
		ESBRequestService service = new ESBRequestService(producer);
		service.processXmlMessage(inputXml);
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(producer).sendMessage(anyString(), anyString(), captor.capture());
		return captor.getValue();
	}

	/** Marshals the input MSG, runs processXmlMessage, returns the XML sent to Artemis. */
	private String runProcess(MSG msg) throws Exception {
		StringWriter in = new StringWriter();
		Marshaller m = JAXBContext.newInstance(MSG.class).createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m.marshal(msg, in);

		MQArtemisProducer producer = mock(MQArtemisProducer.class);
		ESBRequestService service = new ESBRequestService(producer);
		service.processXmlMessage(in.toString());

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(producer).sendMessage(anyString(), anyString(), captor.capture());
		return captor.getValue();
	}

	private MSG buildMsg(ADID adid, MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJFLIGHT flight) {
		MSG.MSGSTREAMIN.INFOBJGENERIC generic = new MSG.MSGSTREAMIN.INFOBJGENERIC();
		generic.setMESSAGEORIGIN("FIDS");
		generic.setHOPO("BKK");
		generic.setADID(adid);
		generic.setSTDT("20260615043000");
		generic.setCSGN("MMA338");
		generic.setFLNO("8M338");
		generic.setTIMESTAMP("20260615034320");

		MSG.MSGSTREAMIN.MSGOBJECTS objects = new MSG.MSGSTREAMIN.MSGOBJECTS();
		objects.setINFOBJFLIGHT(flight);

		MSG.MSGSTREAMIN streamin = new MSG.MSGSTREAMIN();
		streamin.setINFOBJGENERIC(generic);
		streamin.setMSGOBJECTS(objects);

		MSG msg = new MSG();
		msg.setMSGSTREAMIN(streamin);
		return msg;
	}

	private void verifySnapshot(String name, String actual) throws Exception {
		Files.createDirectories(SNAPSHOT_DIR);
		Path golden = SNAPSHOT_DIR.resolve(name + ".xml");
		if (!Files.exists(golden)) {
			Files.write(golden, actual.getBytes(StandardCharsets.UTF_8));
			System.out.println("[snapshot] created " + golden.toAbsolutePath() + " — review & commit.");
			return;
		}
		String expected = new String(Files.readAllBytes(golden), StandardCharsets.UTF_8);
		assertThat(actual)
				.as("Snapshot '%s' changed: processXmlMessage output differs from committed golden (%s).", name, golden)
				.isEqualTo(expected);
	}
}
