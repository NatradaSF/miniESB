package sf.sfis.miniesb.service;

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
import sf.sfis.miniesb.MQArtemisProducer;
import sf.sfis.miniesb.esb.realtimeinbound.ADID;
import sf.sfis.miniesb.esb.realtimeinbound.MSG;

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
