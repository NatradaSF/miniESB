package sf.sfis.ifimsconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import sf.sfis.ifimsconnect.MQWebSphereProducer;
import sf.sfis.ifimsconnect.model.FidsAfttab;

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

	@Test
	@DisplayName("convertCountertoEsb fills INFOBJ_GENERIC from FidsAfttab, empty INFOBJ_COUNTER")
	void buildsCounterFromFidsAfttab() {
		FidsAfttab f = new FidsAfttab();
		f.setAction("update");
		f.setHopo("BKK");
		f.setAdid("D");
		f.setUrno(new java.math.BigDecimal("2009916148"));
		f.setFlno("JL 032 ");     // มี trailing space → ต้องถูก trim
		f.setStod("20260512024000");
		f.setCsgn("JAL32");
		f.setRtyp("J");

		String xml = service.convertCountertoEsb("20260512062532", f);

		assertThat(xml).isNotNull();
		// โครงหลักครบ
		assertThat(xml).contains("<MSGSTREAM_OUT>")
				.contains("<INFOBJ_GENERIC>")
				.contains("<INFOBJ_COUNTER>");
		// identity ของ counter
		assertThat(xml).contains("<MESSAGETYPE>UFISCHKUD</MESSAGETYPE>")
				.contains("<MESSAGEORIGIN>AOS</MESSAGEORIGIN>")
				.contains("<TIMESTAMP>20260512062532</TIMESTAMP>");
		// dynamic จาก FidsAfttab
		assertThat(xml).contains("<HOPO>BKK</HOPO>")
				.contains("<ADID>D</ADID>")
				.contains("<ACTIONTYPE>U</ACTIONTYPE>")
				.contains("<URNO>2009916148</URNO>")
				.contains("<FLNO>JL 032</FLNO>")          // trim แล้ว
				.contains("<STDT>20260512024000</STDT>")   // ADID=D → ใช้ stod
				.contains("<CSGN>JAL32</CSGN>")
				.contains("<RTYP>J</RTYP>");
		// INFOBJ_COUNTER — ค่าว่าง (empty tag) รอ backend map
		assertThat(xml).contains("<CKIC></CKIC>").contains("<FLNU></FLNU>");
		// CTYP ใส่ placeholder CTYP.D ไปก่อน
		assertThat(xml).contains("<CTYP>D</CTYP>");
	}

	@Test
	@DisplayName("convertGatetoEsb (ADID=A) fills INFOBJ_GENERIC + empty GATEARR")
	void buildsGateArrival() {
		FidsAfttab f = new FidsAfttab();
		f.setAction("update");
		f.setHopo("BKK");
		f.setAdid("A");
		f.setUrno(new java.math.BigDecimal("2009914253"));
		f.setFlno("VZ 221");
		f.setStoa("20260512072000");
		f.setCsgn("TVJ221");
		f.setRtyp("J");

		String xml = service.convertGatetoEsb("20260512062531", f);

		assertThat(xml).isNotNull();
		assertThat(xml).contains("<MESSAGETYPE>UFISGTDUD</MESSAGETYPE>")
				.contains("<MESSAGEORIGIN>AOS</MESSAGEORIGIN>")
				.contains("<ADID>A</ADID>")
				.contains("<FLNO>VZ 221</FLNO>")
				.contains("<STDT>20260512072000</STDT>");   // ADID=A → stoa
		// ADID=A → GATEARR (ไม่ใช่ GATEDEP) + field ว่าง
		assertThat(xml).contains("<INFOBJ_GATE>").contains("<GATEARR>")
				.doesNotContain("<GATEDEP>");
		assertThat(xml).contains("<GTA1></GTA1>").contains("<GA1B></GA1B>").contains("<GA1E></GA1E>");
	}

	@Test
	@DisplayName("convertGatetoEsb (ADID=D) uses GATEDEP instead of GATEARR")
	void buildsGateDeparture() {
		FidsAfttab f = new FidsAfttab();
		f.setAction("update");
		f.setHopo("BKK");
		f.setAdid("D");
		f.setFlno("JL 032");
		f.setStod("20260512024000");

		String xml = service.convertGatetoEsb("20260512062531", f);

		assertThat(xml).isNotNull();
		assertThat(xml).contains("<ADID>D</ADID>")
				.contains("<STDT>20260512024000</STDT>")   // ADID=D → stod
				.contains("<GATEDEP>").doesNotContain("<GATEARR>");
		assertThat(xml).contains("<GTD1></GTD1>").contains("<GD1B></GD1B>").contains("<GD1E></GD1E>");
	}

	@Test
	@DisplayName("convertBelttoEsb fills INFOBJ_GENERIC from FidsAfttab, empty INFOBJ_BELT")
	void buildsBelt() {
		FidsAfttab f = new FidsAfttab();
		f.setAction("update");
		f.setHopo("BKK");
		f.setAdid("A");
		f.setUrno(new java.math.BigDecimal("2009913881"));
		f.setFlno("3U 3937");
		f.setStoa("20260511171000");
		f.setCsgn("CSC3937");
		f.setRtyp("J");

		String xml = service.convertBelttoEsb("20260511170015", f);

		assertThat(xml).isNotNull();
		assertThat(xml).contains("<MESSAGETYPE>UFISBLTUD</MESSAGETYPE>")
				.contains("<MESSAGEORIGIN>AOS</MESSAGEORIGIN>")
				.contains("<ADID>A</ADID>")
				.contains("<FLNO>3U 3937</FLNO>")
				.contains("<STDT>20260511171000</STDT>");
		// INFOBJ_BELT — ค่าว่าง (empty tag) รอ backend map
		assertThat(xml).contains("<INFOBJ_BELT>")
				.contains("<BLT1></BLT1>").contains("<B1BS></B1BS>").contains("<B1ES></B1ES>");
	}

	@Test
	@DisplayName("convertAcpositiontoEsb (ADID=A) fills INFOBJ_GENERIC + empty ACPOSITIONARR")
	void buildsAcpositionArrival() {
		FidsAfttab f = new FidsAfttab();
		f.setAction("update");
		f.setHopo("BKK");
		f.setAdid("A");
		f.setUrno(new java.math.BigDecimal("2009913881"));
		f.setFlno("3U 3937");
		f.setStoa("20260511171000");
		f.setCsgn("CSC3937");
		f.setRtyp("J");

		String xml = service.convertAcpositiontoEsb("20260511170015", f);

		assertThat(xml).isNotNull();
		assertThat(xml).contains("<MESSAGETYPE>UFISPOSUD</MESSAGETYPE>")
				.contains("<MESSAGEORIGIN>AOS</MESSAGEORIGIN>")
				.contains("<ADID>A</ADID>")
				.contains("<FLNO>3U 3937</FLNO>")
				.contains("<STDT>20260511171000</STDT>");
		// ADID=A → ACPOSITIONARR + field ว่าง
		assertThat(xml).contains("<INFOBJ_ACPOSITION>").contains("<ACPOSITIONARR>")
				.doesNotContain("<ACPOSITIONDEP>");
		assertThat(xml).contains("<PSTA></PSTA>").contains("<PABS></PABS>").contains("<PAES></PAES>");
	}

	@Test
	@DisplayName("convertAcpositiontoEsb (ADID=D) uses ACPOSITIONDEP instead of ACPOSITIONARR")
	void buildsAcpositionDeparture() {
		FidsAfttab f = new FidsAfttab();
		f.setAction("update");
		f.setHopo("BKK");
		f.setAdid("D");
		f.setFlno("JL 032");
		f.setStod("20260512024000");

		String xml = service.convertAcpositiontoEsb("20260511170015", f);

		assertThat(xml).isNotNull();
		assertThat(xml).contains("<ADID>D</ADID>")
				.contains("<STDT>20260512024000</STDT>")
				.contains("<ACPOSITIONDEP>").doesNotContain("<ACPOSITIONARR>");
		assertThat(xml).contains("<PSTD></PSTD>").contains("<PDBS></PDBS>").contains("<PDES></PDES>");
	}

	/* @Test
	@DisplayName("convertTowingtoEsb fills INFOBJ_GENERIC, empty CONCAT/TOWINGS")
	void buildsTowing() {
		FidsAfttab f = new FidsAfttab();
		f.setAction("update");
		f.setHopo("BKK");
		f.setAdid("A");
		f.setUrno(new java.math.BigDecimal("2009910692"));
		f.setFlno("QF 023");
		f.setStoa("20260511094000");
		f.setCsgn("QFA23");
		f.setRtyp("S");

		String xml = service.convertTowingtoEsb("20260511170019", f);

		assertThat(xml).isNotNull();
		assertThat(xml).contains("<MESSAGETYPE>UFISTOWUD</MESSAGETYPE>")
				.contains("<MESSAGEORIGIN>AOS</MESSAGEORIGIN>")
				.contains("<FLNO>QF 023</FLNO>")
				.contains("<RTYP>S</RTYP>");
		// payload อยู่ใต้ CONCAT/TOWINGS — field ว่าง
		assertThat(xml).contains("<CONCAT>").contains("<TOWINGS>")
				.contains("<TOID></TOID>").contains("<TWTP></TWTP>").contains("<SCHE></SCHE>").contains("<SCHS></SCHS>");
	} */

	@Test
	@DisplayName("convertVdgstoEsb (ADID=D) uses ACTIONTYPE=U + empty VDGSDEP")
	void buildsVdgsDeparture() {
		FidsAfttab f = new FidsAfttab();
		f.setAction("update");
		f.setHopo("BKK");
		f.setAdid("D");
		f.setUrno(new java.math.BigDecimal("2009913942"));
		f.setFlno("BR 6004");
		f.setStod("20260511174500");
		f.setCsgn("EVA6004");
		f.setRtyp("J");

		String xml = service.convertVdgstoEsb("20260511170002", f);

		assertThat(xml).isNotNull();
		assertThat(xml).contains("<MESSAGETYPE>UFISVDGUD</MESSAGETYPE>")
				.contains("<MESSAGEORIGIN>AOS</MESSAGEORIGIN>")
				.contains("<ACTIONTYPE>U</ACTIONTYPE>")   // action=update → U (ปกติเหมือนคิวอื่น)
				.contains("<ADID>D</ADID>")
				.contains("<FLNO>BR 6004</FLNO>");
		// ADID=D → VDGSDEP + field ว่าง
		assertThat(xml).contains("<INFOBJ_VDGS>").contains("<VDGSDEP>")
				.doesNotContain("<VDGSARR>");
		assertThat(xml).contains("<PSTD></PSTD>").contains("<ACT5></ACT5>")
				.contains("<FTYP></FTYP>").contains("<TIFD></TIFD>");
	}

	@Test
	@DisplayName("convertSitatoEsb builds UFISSITA with minimal header + BULKDATA/SITA content")
	void buildsSita() {
		String xml = service.convertSitatoEsb("20260621012747", "BKK", "1351466.snd", "=PRIORITY\nQU\n=TEXT\nDOMESTIC");

		assertThat(xml).isNotNull();
		assertThat(xml).contains("<MESSAGETYPE>UFISSITA</MESSAGETYPE>")
				.contains("<MESSAGEORIGIN>AOS</MESSAGEORIGIN>")
				.contains("<ACTIONTYPE>I</ACTIONTYPE>")
				.contains("<HOPO>BKK</HOPO>");
		// SITA ไม่ผูกเที่ยวบิน → ไม่มี field flight ใน generic
		assertThat(xml).doesNotContain("<ADID>").doesNotContain("<FLNO>").doesNotContain("<URNO>");
		// payload อยู่ใต้ BULKDATA/SITA พร้อม FILE_NAME + CONTENT จริง
		assertThat(xml).contains("<BULKDATA>").contains("<SITA>")
				.contains("<FILE_NAME>1351466.snd</FILE_NAME>")
				.contains("DOMESTIC");
	}

	@Test
	@DisplayName("sendFileReady forwards XML verbatim to UFIS_TRIGGER_OUT (passthrough, no build)")
	void fileReadyPassthrough() {
		MQWebSphereProducer producer = mock(MQWebSphereProducer.class);
		ESBResponseService svc = new ESBResponseService(null, null, null, null, null, null, null, producer);
		ReflectionTestUtils.setField(svc, "webSphereEnabled", true);

		String xml = "<MSG><BATCHFILE_OUT><INFOBJ_FILE_OUT>"
				+ "<FILENAME>/FILES/HDYATC_DLYFLT_20260525100000.txt</FILENAME>"
				+ "</INFOBJ_FILE_OUT></BATCHFILE_OUT></MSG>";
		svc.sendFileReady("BKK", xml);

		// ลงคิว UFIS_TRIGGER_OUT_BKK (BKK → machine1) แบบ verbatim ไม่แตะ payload
		verify(producer).sendToMachine1("UFIS_TRIGGER_OUT_BKK", "BKK", xml);
	}
}
