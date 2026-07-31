package sf.sfis.ifimsconnect.esb.figurein;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import sf.sfis.ifimsconnect.esb.realtimeinbound.MSG;

/**
 * ยืนยันว่า WMFIGURE / INFOBJ_FIGURE จาก UFIS_FIGURE_IN ถูก unmarshal มากับ MSG
 * และอ่านได้ผ่าน {@code getINFOBJFIGURE()} แบบเดียวกับ object ตัวอื่น (else-if path).
 *
 * ข้อความจริงบนคิวห่อด้วย {@code <MSG><MSGSTREAM_IN>...} เหมือน inbound ตัวอื่นทุกตัว.
 */
class FigureMessageInTest {

	private static final String SAMPLE = """
			<?xml version="1.0"?>
			<MSG>
			  <MSGSTREAM_IN>
			    <INFOBJ_GENERIC>
			      <MESSAGETYPE>WMFIGURE</MESSAGETYPE>
			      <MESSAGEORIGIN>PG</MESSAGEORIGIN>
			      <TIMEID>UTC</TIMEID>
			      <HOPO>BKK</HOPO>
			      <TIMESTAMP>20260512060039</TIMESTAMP>
			      <ADID>D</ADID>
			      <STDT>20260511230500</STDT>
			      <FLNO>PG 101</FLNO>
			    </INFOBJ_GENERIC>
			    <MSGOBJECTS>
			      <INFOBJ_FIGURE>
			        <figure>
			          <SENTBY>PG</SENTBY>
			          <AIRPORT>BKK</AIRPORT>
			          <FLIGHTNUMBER>PG 101</FLIGHTNUMBER>
			          <FLIGHTDATE>20260511230500</FLIGHTDATE>
			          <REGISTRATION>HSPPF</REGISTRATION>
			          <ADINDICATOR>D</ADINDICATOR>
			          <INTDOMINDICATOR>D</INTDOMINDICATOR>
			          <AIRLINECODE3>BKP</AIRLINECODE3>
			          <FLIGHTNATURE>05</FLIGHTNATURE>
			          <PAXDISEMBARKINTL>0</PAXDISEMBARKINTL>
			          <PAXDISEMBARKDOM>136</PAXDISEMBARKDOM>
			          <CREW>6</CREW>
			          <PILOT>JAKAPONG</PILOT>
			          <root>
			            <port>
			              <PAXDISEMBARK>137</PAXDISEMBARK>
			              <FLIGHTFROM>BKK</FLIGHTFROM>
			              <FLIGHTTO>USM</FLIGHTTO>
			            </port>
			          </root>
			        </figure>
			      </INFOBJ_FIGURE>
			    </MSGOBJECTS>
			  </MSGSTREAM_IN>
			</MSG>
			""";

	@Test
	@DisplayName("MSG unmarshal exposes INFOBJ_FIGURE via getINFOBJFIGURE() with header intact")
	void msgExposesFigure() throws Exception {
		JAXBContext ctx = JAXBContext.newInstance(MSG.class);
		Unmarshaller u = ctx.createUnmarshaller();

		MSG msg = (MSG) u.unmarshal(new StringReader(SAMPLE));

		// header ยังอ่านได้ปกติ (routing เดิมใช้ MESSAGETYPE/ADID ได้)
		assertThat(msg.getMSGSTREAMIN().getINFOBJGENERIC().getMESSAGETYPE()).isEqualTo("WMFIGURE");

		FigureMessageIn.InfobjFigure infobjfigure =
				msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJFIGURE();
		assertThat(infobjfigure).isNotNull();

		FigureMessageIn.Figure figure = infobjfigure.getFigure();
		assertThat(figure).isNotNull();
		assertThat(figure.getFlightnumber()).isEqualTo("PG 101");
		assertThat(figure.getRegistration()).isEqualTo("HSPPF");
		assertThat(figure.getPaxdisembarkdom()).isEqualTo("136");
		assertThat(figure.getPaxdisembarkintl()).isEqualTo("0");
		assertThat(figure.getCrew()).isEqualTo("6");
		assertThat(figure.getPilot()).isEqualTo("JAKAPONG");

		assertThat(figure.getRoot()).isNotNull();
		assertThat(figure.getRoot().getPort()).hasSize(1);
		assertThat(figure.getRoot().getPort().get(0).getPaxdisembark()).isEqualTo("137");
		assertThat(figure.getRoot().getPort().get(0).getFlightto()).isEqualTo("USM");
	}

	@Test
	@DisplayName("other inbound object (no INFOBJ_FIGURE) → getINFOBJFIGURE() is null")
	void noFigureReturnsNull() throws Exception {
		String noFigure = """
				<MSG><MSGSTREAM_IN>
				  <INFOBJ_GENERIC><MESSAGETYPE>ATC</MESSAGETYPE><HOPO>BKK</HOPO></INFOBJ_GENERIC>
				  <MSGOBJECTS/>
				</MSGSTREAM_IN></MSG>
				""";
		JAXBContext ctx = JAXBContext.newInstance(MSG.class);
		MSG msg = (MSG) ctx.createUnmarshaller().unmarshal(new StringReader(noFigure));

		assertThat(msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJFIGURE()).isNull();
	}
}
