package sf.sfis.ifimsconnect.esb.realtimeinbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import sf.sfis.ifimsconnect.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJMANIFEST;

/**
 * ยืนยันว่า WMMANIFEST / INFOBJ_MANIFEST (มีอยู่ใน schema เดิม) ถูก unmarshal มากับ MSG
 * และอ่านได้ผ่าน {@code getINFOBJMANIFEST()} แบบเดียวกับ object ตัวอื่น (else-if path).
 */
class ManifestMessageTest {

	private static final String SAMPLE = """
			<?xml version="1.0"?>
			<MSG>
			  <MSGSTREAM_IN>
			    <INFOBJ_GENERIC>
			      <MESSAGETYPE>WMMANIFEST</MESSAGETYPE>
			      <MESSAGEORIGIN>PG</MESSAGEORIGIN>
			      <HOPO>BKK</HOPO>
			      <ADID>D</ADID>
			      <FLNO>PG 101</FLNO>
			    </INFOBJ_GENERIC>
			    <MSGOBJECTS>
			      <INFOBJ_MANIFEST>
			        <manifest>
			          <SENTDATE>20260512060132</SENTDATE>
			          <SENTBY>PG</SENTBY>
			          <AIRPORT>BKK</AIRPORT>
			          <FLIGHTNUMBER>PG 101</FLIGHTNUMBER>
			          <FLIGHTDATE>20260511230500</FLIGHTDATE>
			          <REGISTRATION>HSPPF</REGISTRATION>
			          <ADINDICATOR>D</ADINDICATOR>
			          <INTDOMINDICATOR>D</INTDOMINDICATOR>
			          <TYPE>TPM</TYPE>
			          <MESSAGE>BANGKOK AIRWAYS
			PASSENGER MANIFEST
			TOTAL PAX-137</MESSAGE>
			        </manifest>
			      </INFOBJ_MANIFEST>
			    </MSGOBJECTS>
			  </MSGSTREAM_IN>
			</MSG>
			""";

	@Test
	@DisplayName("MSG unmarshal exposes INFOBJ_MANIFEST via getINFOBJMANIFEST() with fields + MESSAGE")
	void msgExposesManifest() throws Exception {
		JAXBContext ctx = JAXBContext.newInstance(MSG.class);
		Unmarshaller u = ctx.createUnmarshaller();

		MSG msg = (MSG) u.unmarshal(new StringReader(SAMPLE));

		assertThat(msg.getMSGSTREAMIN().getINFOBJGENERIC().getMESSAGETYPE()).isEqualTo("WMMANIFEST");

		JAXBElement<INFOBJMANIFEST> manElement = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJMANIFEST();
		assertThat(manElement).isNotNull();

		INFOBJMANIFEST.Manifest manifest = manElement.getValue().getManifest();
		assertThat(manifest).isNotNull();
		assertThat(manifest.getFLIGHTNUMBER()).isEqualTo("PG 101");
		assertThat(manifest.getREGISTRATION()).isEqualTo("HSPPF");
		assertThat(manifest.getTYPE()).isEqualTo("TPM");
		assertThat(manifest.getADINDICATOR()).isEqualTo("D");
		assertThat(manifest.getMESSAGE()).contains("PASSENGER MANIFEST").contains("TOTAL PAX-137");
	}
}
