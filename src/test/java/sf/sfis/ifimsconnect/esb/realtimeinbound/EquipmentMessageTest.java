package sf.sfis.ifimsconnect.esb.realtimeinbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import sf.sfis.ifimsconnect.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJEQUIPMENT;

/**
 * ยืนยันว่า APSBLK / INFOBJ_EQUIPMENT (มีอยู่ใน schema เดิม) ถูก unmarshal มากับ MSG
 * และอ่านค่าใน PLB/USAGE ได้ผ่าน {@code getINFOBJEQUIPMENT()} (else-if path).
 */
class EquipmentMessageTest {

	private static final String SAMPLE = """
			<?xml version="1.0"?>
			<MSG>
			  <MSGSTREAM_IN>
			    <INFOBJ_GENERIC>
			      <MESSAGETYPE>APSBLK</MESSAGETYPE>
			      <MESSAGEORIGIN>SATAMS</MESSAGEORIGIN>
			      <HOPO>BKK</HOPO>
			      <ADID>A</ADID>
			      <FLNO>TG 304</FLNO>
			    </INFOBJ_GENERIC>
			    <MSGOBJECTS>
			      <INFOBJ_EQUIPMENT>
			        <PLB>
			          <USAGE>
			            <BGRP>S101</BGRP>
			            <STATUS>1</STATUS>
			            <DATETIME>20260511134400</DATETIME>
			          </USAGE>
			        </PLB>
			      </INFOBJ_EQUIPMENT>
			    </MSGOBJECTS>
			  </MSGSTREAM_IN>
			</MSG>
			""";

	@Test
	@DisplayName("MSG unmarshal exposes INFOBJ_EQUIPMENT PLB/USAGE via getINFOBJEQUIPMENT()")
	void msgExposesEquipment() throws Exception {
		JAXBContext ctx = JAXBContext.newInstance(MSG.class);
		Unmarshaller u = ctx.createUnmarshaller();

		MSG msg = (MSG) u.unmarshal(new StringReader(SAMPLE));

		assertThat(msg.getMSGSTREAMIN().getINFOBJGENERIC().getMESSAGETYPE()).isEqualTo("APSBLK");

		JAXBElement<INFOBJEQUIPMENT> eqpElement = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJEQUIPMENT();
		assertThat(eqpElement).isNotNull();

		INFOBJEQUIPMENT.PLB.USAGE usage = eqpElement.getValue().getPLB().getUSAGE();
		assertThat(usage).isNotNull();
		assertThat(usage.getBGRP()).isEqualTo("S101");
		assertThat(usage.getSTATUS()).isEqualTo("1");
		assertThat(usage.getDATETIME()).isEqualTo("20260511134400");
	}
}
