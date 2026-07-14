package sf.sfis.miniesb.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.springframework.stereotype.Service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sf.sfis.miniesb.MQArtemisProducer;
import sf.sfis.miniesb.aodb.Aodbduration;
import sf.sfis.miniesb.aodb.Aodbprioduration;
import sf.sfis.miniesb.aodb.Aodbpriostring;
import sf.sfis.miniesb.aodb.Aodbstring;
import sf.sfis.miniesb.aodb.Body;
import sf.sfis.miniesb.aodb.Control;
import sf.sfis.miniesb.aodb.Envelope;
import sf.sfis.miniesb.aodb.Header;
import sf.sfis.miniesb.aodb.IfAdexpmessage;
import sf.sfis.miniesb.aodb.ObjectFactory;
import sf.sfis.miniesb.aodb.PlTurn;
import sf.sfis.miniesb.aodb.PlTurn.PtPaArrival;
import sf.sfis.miniesb.aodb.PlTurn.PtPaArrival.PlArrival;
import sf.sfis.miniesb.aodb.PlTurn.PtPaArrival.PlArrival.PaRactAircrafttype;
import sf.sfis.miniesb.aodb.PlTurn.PtPaArrival.PlArrival.PlBaggagebeltList;
import sf.sfis.miniesb.aodb.PlTurn.PtPaArrival.PlArrival.PlBaggagebeltList.PlBaggagebelt;
import sf.sfis.miniesb.aodb.PlTurn.PtPdDeparture;
import sf.sfis.miniesb.aodb.PlTurn.PtPdDeparture.PlDeparture;
import sf.sfis.miniesb.aodb.PlTurn.PtPdDeparture.PlDeparture.PdRactAircrafttype;
import sf.sfis.miniesb.aodb.PlTurn.PtPdDeparture.PlDeparture.PlDeparturebeltList;
import sf.sfis.miniesb.aodb.PlTurn.PtPdDeparture.PlDeparture.PlDeparturebeltList.PlDeparturebelt;
import sf.sfis.miniesb.esb.realtimeinbound.ADID;
import sf.sfis.miniesb.esb.realtimeinbound.MSG;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.INFOBJGENERIC;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.BULKDATA;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJFLIGHT;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJMUINFO;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJVDGS;
import sf.sfis.miniesb.utility.FieldInspector;

@Slf4j
@WebService
@Service
@RequiredArgsConstructor
public class ESBRequestService {
	private final MQArtemisProducer artemisProducer;

	ObjectFactory factory = new ObjectFactory();

	// Dispatch tables: field name -> setter lambda — แทน if-else chain ใน setFlight (สร้างครั้งเดียว)
	private final Map<String, BiConsumer<PlDeparture, INFOBJFLIGHT>> departureSetters = buildDepartureSetters();
	private final Map<String, BiConsumer<PlArrival, INFOBJFLIGHT>> arrivalSetters = buildArrivalSetters();

	// Dedicated logger for inbound XML from ESB via webservice → logs/receive_esb/<hopo>/<queue>.log
	// (ใช้ logger ชื่อเดียวกับฝั่ง WebSphere MQ consumer จึงไปรวมในโฟลเดอร์ receive_esb เดียวกัน)
	private static final Logger receivedEsbLog = LoggerFactory.getLogger("RECEIVED_ESB_XML");
	private static final Pattern HOPO_PATTERN = Pattern.compile("<HOPO>\\s*([^<]*?)\\s*</HOPO>");

	private static String extractHopo(String xml) {
		Matcher m = HOPO_PATTERN.matcher(xml);
		return (m.find() && !m.group(1).isEmpty()) ? m.group(1) : "unknown";
	}

	// Cached JAXBContexts — thread-safe, built once instead of per message.
	private static final JAXBContext MSG_CTX = newContext(MSG.class);
	private static final JAXBContext ENVELOPE_CTX = newContext(Envelope.class);

	private static JAXBContext newContext(Class<?> clazz) {
		try {
			return JAXBContext.newInstance(clazz);
		} catch (JAXBException e) {
			throw new IllegalStateException("Failed to init JAXBContext for " + clazz.getName(), e);
		}
	}

	@WebMethod
	public void requestAodbInbound(@WebParam(name = "aodbInbound") String xmlString) {
		log.info("Received from WebService...");

		// log payload ขาเข้าทาง webservice ลงโฟลเดอร์ receive_esb เหมือนฝั่ง WebSphere MQ
		// (queue ไม่มีในกรณี webservice จึงใช้ป้าย "WS_request")
		MDC.put("recvEsbKey", extractHopo(xmlString) + "/inbound-WS");
		try {
			receivedEsbLog.info(xmlString);
		} finally {
			MDC.remove("recvEsbKey");
		}

		processXmlMessage(xmlString);
	}

	public void processXmlMessage(String xmlString) {
		log.info("request AODB Inbound...");
		//log.info(xmlString);
		try {
			JAXBContext jaxbContext = MSG_CTX;
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			MSG msg = (MSG) unmarshaller.unmarshal(new StringReader(xmlString));
			INFOBJGENERIC infobjgeneric = msg.getMSGSTREAMIN().getINFOBJGENERIC();
			String sender = "MINIESB";
			String receiver = "AOS";
			String systemType = infobjgeneric.getMESSAGEORIGIN();
			String hopo = infobjgeneric.getHOPO();
			ADID adid = infobjgeneric.getADID();
			String stdt = infobjgeneric.getSTDT();
			String csgn = infobjgeneric.getCSGN();
			String flno = infobjgeneric.getFLNO();

			Header header = new Header();
			Control control = new Control();
			/* control.setMessageId("localhost:2d970033:195f1193a55:-12d5"); */
			control.setMessageVersion("1.4");
			control.setMessageType("UPDATE");
			control.setConfirmType("ALL");
			control.setOriginator(systemType);
			control.setSender(sender);
			control.setReceiver(receiver);
			control.setStation(hopo);
			control.setTimestamp(getLocalDate(infobjgeneric.getTIMESTAMP()));
			header.setControl(control);
			Body body = new Body();
			BULKDATA bulkdata = msg.getMSGSTREAMIN().getMSGOBJECTS().getBULKDATA();
			INFOBJFLIGHT infobjflight = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJFLIGHT();
			INFOBJVDGS infobjvdgs = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJVDGS();
			JAXBElement<INFOBJMUINFO> muElement = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJMUINFO();
			INFOBJMUINFO infobjmuinfo = (muElement != null) ? muElement.getValue() : null;
			if (bulkdata != null) {
				String message = systemType.equals("AFTN") ? bulkdata.getAFTN().getCONTENT()
						: bulkdata.getSITA().getCONTENT();
				body = setBulkData(body, message);
			} else if (infobjflight != null) {
				infobjflight.setSTOA(stdt);
				infobjflight.setSTOD(stdt);
				infobjflight.setCSGN(csgn);
				body = setFlight(adid, body, infobjflight);
			} else if (infobjvdgs != null) {
				body = setVdgs(adid, body, stdt, flno, infobjvdgs);
			} else if (infobjmuinfo != null) {
				// BHS make-up unit info (INFOBJ_MUINFO) → carousel/belt แยกตาม ADID (A=arrival, D=departure)
				body = setBhs(adid, body, infobjmuinfo);
			}

			Envelope envelope = new Envelope();
			envelope.setHeader(header);
			envelope.setBody(body);

			// Marshal to XML String
			StringWriter writer = new StringWriter();
			JAXBContext context = ENVELOPE_CTX;
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(envelope, writer);

			if (systemType.equals("AFTN")) {
				log.info("Send to AQ_FROM_AFTN_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_AFTN_AOT_AOS_TST", hopo, writer.toString());
			} else if (systemType.equals("SITA")) {
				log.info("Send to AQ_FROM_SITA_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_SITA_AOT_AOS_TST", hopo, writer.toString());
			} else {
				log.info("Send to AQ_FROM_FIDS_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_FIDS_AOT_AOS_TST", hopo, writer.toString());
			}
			//log.info(writer.toString());

		} catch (JAXBException e) {
			log.error("requestAodbInbound: ", e);
			// e.printStackTrace();
		}
	}

	private Body setBulkData(Body body, String message) {
		IfAdexpmessage ifAdexpmessage = factory.createIfAdexpmessage();
		ifAdexpmessage.setIamOriginalmessage(factory.createIfAdexpmessageIamOriginalmessage(getAodbstring(message)));
		body.setIfAdexpmessage(ifAdexpmessage);
		return body;
	}

	private Body setVdgs(ADID adid, Body body, String stdt, String flno, INFOBJVDGS infobjvdgs) {
		PlTurn plTurn = factory.createPlTurn();
		PlArrival plArrival = null;
		PlDeparture plDeparture = null;

		if (adid == ADID.D) {
			plDeparture = factory.createPlTurnPtPdDeparturePlDeparture();
			plDeparture.setPdSobt(factory.createPlTurnPtPdDeparturePlDeparturePdSobt(getAodbDate(stdt)));
			plDeparture.setPdFlightnumber(
					factory.createPlTurnPtPdDeparturePlDeparturePdFlightnumber(getAodbpriostring(flno)));

			List<String> fieldsNotNull = FieldInspector.getNonNullFields(infobjvdgs.getVDGSDEP());
			for (String field : fieldsNotNull) {

				if (field.equals("pstd")) {
					plDeparture.setPdRstaStand(factory.createPlTurnPtPdDeparturePlDeparturePdRstaStand(
							getAodbpriostring(infobjvdgs.getVDGSDEP().getPSTD())));
				} else if (field.equals("act5")) {
					PdRactAircrafttype.RefAircrafttype depAircrafttype = factory
							.createPlTurnPtPdDeparturePlDeparturePdRactAircrafttypeRefAircrafttype();
					depAircrafttype.setRactIcaotype(
							factory.createPlTurnPtPdDeparturePlDeparturePdRactAircrafttypeRefAircrafttypeRactIcaotype(
									getAodbstring(infobjvdgs.getVDGSDEP().getACT5())));
					PdRactAircrafttype pdRactAircrafttype = factory
							.createPlTurnPtPdDeparturePlDeparturePdRactAircrafttype();
					pdRactAircrafttype.getContent().add(infobjvdgs.getVDGSDEP().getACT5());
					pdRactAircrafttype.getContent().add(factory
							.createPlTurnPtPdDeparturePlDeparturePdRactAircrafttypeRefAircrafttype(depAircrafttype));
					plDeparture.setPdRactAircrafttype(
							factory.createPlTurnPtPdDeparturePlDeparturePdRactAircrafttype(pdRactAircrafttype));
				} else if (field.equals("ofbl")) {
					plDeparture.setPdAobt(factory.createPlTurnPtPdDeparturePlDeparturePdAobt(
							getAodbDate(infobjvdgs.getVDGSDEP().getOFBL())));
				}
			}

			PtPdDeparture ptPdDeparture = factory.createPlTurnPtPdDeparture();
			ptPdDeparture.getContent().add(factory.createPlTurnPtPdDeparturePlDeparture(plDeparture));
			plTurn.setPtPdDeparture(factory.createPlTurnPtPdDeparture(ptPdDeparture));
		} else if (adid == ADID.A) {
			plArrival = factory.createPlTurnPtPaArrivalPlArrival();
			plArrival.setPaSibt(factory.createPlTurnPtPaArrivalPlArrivalPaSibt(getAodbDate(stdt)));
			plArrival
					.setPaFlightnumber(factory.createPlTurnPtPaArrivalPlArrivalPaFlightnumber(getAodbpriostring(flno)));

			List<String> fieldsNotNull = FieldInspector.getNonNullFields(infobjvdgs.getVDGSARR());
			for (String field : fieldsNotNull) {
				if (field.equals("psta")) {
					plArrival.setPaRstaStand(factory.createPlTurnPtPaArrivalPlArrivalPaRstaStand(
							getAodbpriostring(infobjvdgs.getVDGSARR().getPSTA())));
				} else if (field.equals("act5")) {
					PaRactAircrafttype.RefAircrafttype arrAircrafttype = factory
							.createPlTurnPtPaArrivalPlArrivalPaRactAircrafttypeRefAircrafttype();
					arrAircrafttype.setRactIcaotype(
							factory.createPlTurnPtPaArrivalPlArrivalPaRactAircrafttypeRefAircrafttypeRactIcaotype(
									getAodbstring(infobjvdgs.getVDGSARR().getACT5())));
					PaRactAircrafttype paRactAircrafttype = factory
							.createPlTurnPtPaArrivalPlArrivalPaRactAircrafttype();
					paRactAircrafttype.getContent().add(infobjvdgs.getVDGSARR().getACT5());
					paRactAircrafttype.getContent().add(
							factory.createPlTurnPtPaArrivalPlArrivalPaRactAircrafttypeRefAircrafttype(arrAircrafttype));
					plArrival.setPaRactAircrafttype(
							factory.createPlTurnPtPaArrivalPlArrivalPaRactAircrafttype(paRactAircrafttype));
				} else if (field.equals("onbl")) {
					plArrival.setPaAibt(factory
							.createPlTurnPtPaArrivalPlArrivalPaAibt(getAodbDate(infobjvdgs.getVDGSARR().getONBL())));
				}
			}

			PtPaArrival ptPaArrival = factory.createPlTurnPtPaArrival();
			ptPaArrival.getContent().add(factory.createPlTurnPtPaArrivalPlArrival(plArrival));
			plTurn.setPtPaArrival(factory.createPlTurnPtPaArrival(ptPaArrival));
		}
		body.setPlTurn(plTurn);
		return body;
	}

	private Body setFlight(ADID adid, Body body, INFOBJFLIGHT infobjflight) {
		List<String> fieldsNotNull = FieldInspector.getNonNullFields(infobjflight);
		// log.info(String.join(", ", fieldsNotNull));

		PlTurn plTurn = factory.createPlTurn();
		PlArrival plArrival = null;
		PlDeparture plDeparture = null;

		if (adid == ADID.A) {
			plArrival = factory.createPlTurnPtPaArrivalPlArrival();
		} else if (adid == ADID.D) {
			plDeparture = factory.createPlTurnPtPdDeparturePlDeparture();
		}

		for (String field : fieldsNotNull) {
			if (plDeparture != null) {
				BiConsumer<PlDeparture, INFOBJFLIGHT> setter = departureSetters.get(field);
				if (setter != null) {
					setter.accept(plDeparture, infobjflight);
				}
			} else if (plArrival != null) {
				BiConsumer<PlArrival, INFOBJFLIGHT> setter = arrivalSetters.get(field);
				if (setter != null) {
					setter.accept(plArrival, infobjflight);
				}
			}
		}

		// สร้าง wrapper ครั้งเดียวหลังลูป (เดิมสร้างซ้ำทุก field ในลูป — สิ้นเปลือง)
		// guard ด้วย !isEmpty เพื่อรักษาพฤติกรรมเดิม: ถ้าไม่มี field เลย จะไม่ใส่ departure/arrival
		if (!fieldsNotNull.isEmpty()) {
			if (plDeparture != null) {
				PtPdDeparture ptPdDeparture = factory.createPlTurnPtPdDeparture();
				ptPdDeparture.getContent().add(factory.createPlTurnPtPdDeparturePlDeparture(plDeparture));
				plTurn.setPtPdDeparture(factory.createPlTurnPtPdDeparture(ptPdDeparture));
			} else if (plArrival != null) {
				PtPaArrival ptPaArrival = factory.createPlTurnPtPaArrival();
				ptPaArrival.getContent().add(factory.createPlTurnPtPaArrivalPlArrival(plArrival));
				plTurn.setPtPaArrival(factory.createPlTurnPtPaArrival(ptPaArrival));
			}
		}
		body.setPlTurn(plTurn);
		return body;
	}

	/** field name -> วิธี set ค่าลง PlDeparture (แทน if-else เดิมใน setFlight ทุกเงื่อนไขเป๊ะ) */
	private Map<String, BiConsumer<PlDeparture, INFOBJFLIGHT>> buildDepartureSetters() {
		Map<String, BiConsumer<PlDeparture, INFOBJFLIGHT>> m = new HashMap<>();
		m.put("csgn", (d, f) -> d.setPdCallsign(
				factory.createPlTurnPtPdDeparturePlDeparturePdCallsign(getAodbpriostring(f.getCSGN()))));
		m.put("rwyd", (d, f) -> d.setPdRrwyRunway(
				factory.createPlTurnPtPdDeparturePlDeparturePdRrwyRunway(getAodbpriostring(f.getRWYD()))));
		m.put("tsat", (d, f) -> {
			if (!f.getTSAT().trim().equals("")) {
				d.setPdTsat(factory.createPlTurnPtPdDeparturePlDeparturePdTsat(getAodbDate(f.getTSAT())));
			}
		});
		m.put("ctot", (d, f) -> d.setPdCtot(
				factory.createPlTurnPtPdDeparturePlDeparturePdCtot(getAodbDate(f.getCTOT()))));
		m.put("stod", (d, f) -> d.setPdSobt(
				factory.createPlTurnPtPdDeparturePlDeparturePdSobt(getAodbDate(f.getSTOD()))));
		m.put("asrt", (d, f) -> d.setPdAsrt(
				factory.createPlTurnPtPdDeparturePlDeparturePdAsrt(getAodbDate(f.getASRT()))));
		m.put("asat", (d, f) -> d.setPdAsat(
				factory.createPlTurnPtPdDeparturePlDeparturePdAsat(getAodbDate(f.getASAT()))));
		m.put("airb", (d, f) -> {
			d.setPdAtot(factory.createPlTurnPtPdDeparturePlDeparturePdAtot(getAodbDate(f.getAIRB())));
			d.setPdSobt(factory.createPlTurnPtPdDeparturePlDeparturePdSobt(getAodbDate(getCurrentDate())));
		});
		m.put("ifrd", (d, f) -> d.setPdFlightrule(
				factory.createPlTurnPtPdDeparturePlDeparturePdFlightrule(getAodbpriostring(f.getIFRD()))));
		m.put("acgt", (d, f) -> d.setPdAcgt(
				factory.createPlTurnPtPdDeparturePlDeparturePdAcgt(getAodbDate(f.getACGT()))));
		m.put("tobt", (d, f) -> d.setPdTobt(
				factory.createPlTurnPtPdDeparturePlDeparturePdTobt(getAodbDate(f.getTOBT()))));
		m.put("aegt", (d, f) -> d.setPdAegt(
				factory.createPlTurnPtPdDeparturePlDeparturePdAegt(getAodbDate(f.getAEGT()))));
		m.put("ardt", (d, f) -> d.setPdDoorclosetime(
				factory.createPlTurnPtPdDeparturePlDeparturePdDoorclosetime(getAodbDate(f.getARDT()))));
		// หมายเหตุ: asbt ใช้ setPdArdt + wrapper PdAsbt ตามโค้ดเดิม (รักษาพฤติกรรมเดิม)
		m.put("asbt", (d, f) -> d.setPdArdt(
				factory.createPlTurnPtPdDeparturePlDeparturePdAsbt(getAodbDate(f.getASBT()))));
		return m;
	}

	/** field name -> วิธี set ค่าลง PlArrival (แทน if-else เดิมใน setFlight ทุกเงื่อนไขเป๊ะ) */
	private Map<String, BiConsumer<PlArrival, INFOBJFLIGHT>> buildArrivalSetters() {
		Map<String, BiConsumer<PlArrival, INFOBJFLIGHT>> m = new HashMap<>();
		m.put("csgn", (a, f) -> a.setPaCallsign(
				factory.createPlTurnPtPaArrivalPlArrivalPaCallsign(getAodbpriostring(f.getCSGN()))));
		m.put("stoa", (a, f) -> a.setPaSibt(
				factory.createPlTurnPtPaArrivalPlArrivalPaSibt(getAodbDate(f.getSTOA()))));
		m.put("rwya", (a, f) -> a.setPaRrwyRunway(
				factory.createPlTurnPtPaArrivalPlArrivalPaRrwyRunway(getAodbpriostring(f.getRWYA()))));
		m.put("tldt", (a, f) -> a.setPaTldt(
				factory.createPlTurnPtPaArrivalPlArrivalPaTldt(getAodbDate(f.getTLDT()))));
		m.put("tmoa", (a, f) -> a.setPaFnlt(
				factory.createPlTurnPtPaArrivalPlArrivalPaFnlt(getAodbDate(f.getTMOA()))));
		m.put("land", (a, f) -> {
			a.setPaAldt(factory.createPlTurnPtPaArrivalPlArrivalPaAldt(getAodbDate(f.getLAND())));
			a.setPaSibt(factory.createPlTurnPtPaArrivalPlArrivalPaSibt(getAodbDate(getCurrentDate())));
		});
		m.put("ifra", (a, f) -> a.setPaFlightrule(
				factory.createPlTurnPtPaArrivalPlArrivalPaFlightrule(getAodbpriostring(f.getIFRA()))));
		return m;
	}

	/**
	 * BHS make-up unit info (INFOBJ_MUINFO) → AODB pl_departurebelt (departure carousels).
	 * BAZ1/BAZ4 = carousel ID (สายพานที่ 1/2), BAO/BAC = เวลาเปิด/ปิด "จริง" → beginactual/endactual.
	 */
	private Body setBhs(ADID adid, Body body, INFOBJMUINFO mu) {
		PlTurn plTurn = factory.createPlTurn();
		if (adid == ADID.A) {
			// ADID = A → arrival reclaim belt (pl_baggagebelt, pbb_*)
			PlArrival plArrival = factory.createPlTurnPtPaArrivalPlArrival();
			PlBaggagebeltList beltList = factory.createPlTurnPtPaArrivalPlArrivalPlBaggagebeltList();
			addArrivalBelt(beltList, jaxbValue(mu.getBAZ1()), jaxbValue(mu.getBAO1()), jaxbValue(mu.getBAC1()));
			addArrivalBelt(beltList, jaxbValue(mu.getBAZ4()), jaxbValue(mu.getBAO4()), jaxbValue(mu.getBAC4()));
			plArrival.setPlBaggagebeltList(beltList);
			PtPaArrival ptPaArrival = factory.createPlTurnPtPaArrival();
			ptPaArrival.getContent().add(factory.createPlTurnPtPaArrivalPlArrival(plArrival));
			plTurn.setPtPaArrival(factory.createPlTurnPtPaArrival(ptPaArrival));
		} else if (adid == ADID.D) {
			// ADID = D → departure make-up belt (pl_departurebelt, pdb_*)
			PlDeparture plDeparture = factory.createPlTurnPtPdDeparturePlDeparture();
			PlDeparturebeltList beltList = factory.createPlTurnPtPdDeparturePlDeparturePlDeparturebeltList();
			addDepartureBelt(beltList, jaxbValue(mu.getBAZ1()), jaxbValue(mu.getBAO1()), jaxbValue(mu.getBAC1()));
			addDepartureBelt(beltList, jaxbValue(mu.getBAZ4()), jaxbValue(mu.getBAO4()), jaxbValue(mu.getBAC4()));
			plDeparture.setPlDeparturebeltList(beltList);
			PtPdDeparture ptPdDeparture = factory.createPlTurnPtPdDeparture();
			ptPdDeparture.getContent().add(factory.createPlTurnPtPdDeparturePlDeparture(plDeparture));
			plTurn.setPtPdDeparture(factory.createPlTurnPtPdDeparture(ptPdDeparture));
		}
		body.setPlTurn(plTurn);
		return body;
	}

	/**
	 * ADID=D: สร้าง pl_departurebelt 1 รายการ "เสมอ" (แม้ carousel id ว่าง) แล้วเพิ่มเข้า list
	 * — slot ว่าง = code ว่าง เพื่อให้ AODB อัพเดต/เคลียร์สายพานนั้นได้ (ลำดับใน list = ลำดับ carousel).
	 */
	private void addDepartureBelt(PlDeparturebeltList beltList, String carouselId, String openTime, String closeTime) {
		PlDeparturebelt belt = factory.createPlTurnPtPdDeparturePlDeparturePlDeparturebeltListPlDeparturebelt();
		belt.setPdbRdbDeparturebelt(factory
				.createPlTurnPtPdDeparturePlDeparturePlDeparturebeltListPlDeparturebeltPdbRdbDeparturebelt(
						getAodbstring(carouselId == null ? "" : carouselId.trim())));
		Aodbduration open = getAodbDurationMinute(openTime);
		if (open != null) {
			belt.setPdbBeginactual(factory
					.createPlTurnPtPdDeparturePlDeparturePlDeparturebeltListPlDeparturebeltPdbBeginactual(open));
		}
		Aodbduration close = getAodbDurationMinute(closeTime);
		if (close != null) {
			belt.setPdbEndactual(factory
					.createPlTurnPtPdDeparturePlDeparturePlDeparturebeltListPlDeparturebeltPdbEndactual(close));
		}
		beltList.getPlDeparturebelt().add(belt);
	}

	/**
	 * ADID=A: สร้าง pl_baggagebelt 1 รายการ "เสมอ" (แม้ carousel id ว่าง) แล้วเพิ่มเข้า list
	 * — slot ว่าง = code ว่าง เพื่อให้ AODB อัพเดต/เคลียร์สายพานนั้นได้ (ลำดับใน list = ลำดับ carousel).
	 */
	private void addArrivalBelt(PlBaggagebeltList beltList, String carouselId, String openTime, String closeTime) {
		PlBaggagebelt belt = factory.createPlTurnPtPaArrivalPlArrivalPlBaggagebeltListPlBaggagebelt();
		belt.setPbbRbbBaggagebelt(factory
				.createPlTurnPtPaArrivalPlArrivalPlBaggagebeltListPlBaggagebeltPbbRbbBaggagebelt(
						getAodbstring(carouselId == null ? "" : carouselId.trim())));
		Aodbduration open = getAodbDurationMinute(openTime);
		if (open != null) {
			belt.setPbbBeginactual(factory
					.createPlTurnPtPaArrivalPlArrivalPlBaggagebeltListPlBaggagebeltPbbBeginactual(open));
		}
		Aodbduration close = getAodbDurationMinute(closeTime);
		if (close != null) {
			belt.setPbbEndactual(factory
					.createPlTurnPtPaArrivalPlArrivalPlBaggagebeltListPlBaggagebeltPbbEndactual(close));
		}
		beltList.getPlBaggagebelt().add(belt);
	}

	private static String jaxbValue(JAXBElement<String> el) {
		return (el == null) ? null : el.getValue();
	}

	/** BHS ส่งเวลา 12 หลัก (yyyyMMddHHmm ไม่มีวินาที) → เติม "00" วินาที แล้วแปลงเป็น Aodbduration */
	private Aodbduration getAodbDurationMinute(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		try {
			Aodbduration aodbduration = factory.createAodbduration();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			Date d = sdf.parse(value.trim() + "00");
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(d);
			XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
			xmlCal.setFractionalSecond(null);
			xmlCal.setTimezone(0);
			aodbduration.setValue(xmlCal);
			return aodbduration;
		} catch (ParseException e) {
			log.error("getAodbDurationMinute: ", e);
		} catch (DatatypeConfigurationException e) {
			log.error("getAodbDurationMinute: ", e);
		}
		return null;
	}

	private Aodbstring getAodbstring(String value) {
		Aodbstring aodbstring = factory.createAodbstring();
		aodbstring.setValue(value);
		return aodbstring;
	}

	private Aodbpriostring getAodbpriostring(String value) {
		Aodbpriostring aodbstring = factory.createAodbpriostring();
		aodbstring.setValue(value);
		return aodbstring;
	}

	private Aodbprioduration getAodbDate(String value) {
		try {
			Aodbprioduration aodbprioduration = factory.createAodbprioduration();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			if (!value.trim().equals("")) {
				Date d = sdf.parse(value);
				GregorianCalendar cal = new GregorianCalendar();
				cal.setTime(d);
				XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
				xmlCal.setFractionalSecond(null);
				xmlCal.setTimezone(0);
				aodbprioduration.setValue(xmlCal);
				return aodbprioduration;
			}
		} catch (ParseException e) {
			log.error("getAodbDate: ", e);
			// e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			log.error("getAodbDate: ", e);
			// e.printStackTrace();
		}
		return null;
	}

	private String getLocalDate(String value) {
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		LocalDateTime utcDateTime = LocalDateTime.parse(value, inputFormatter);
		ZonedDateTime utcZoned = utcDateTime.atZone(ZoneOffset.UTC);
		ZonedDateTime localZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Bangkok"));
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
		String formatted = localZoned.format(outputFormatter);
		return formatted;
	}

	private static String getCurrentDate() {
		Calendar cal = Calendar.getInstance();
		// เซตเวลาเป็น 00:00:00
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		Date date = cal.getTime();
		// Format ที่ต้องการ
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String formatted = sdf.format(date);
		// System.out.println(formatted); // เช่น 20250524000000
		return formatted;
	}

	// For test only
	public static void main(String[] args) {
		// try {
		// Header header = new Header();
		// Control control = new Control();
		// control.setMessageId("localhost:2d970033:195f1193a55:-12d5");
		// control.setMessageVersion("1.4");
		// control.setMessageType("SUBSCRIBE");
		// control.setSender("FIDS");
		// control.setTimestamp("2025-04-01T21:32:00");
		// Request request = new Request();
		// request.setDatatype("pl_turn");
		// request.setStartTime("2025-05-09T00:00:00");
		// request.setEndTime("2025-05-09T23:59:59");
		// control.setRequest(request);
		// header.setControl(control);
		// Body body = new Body();
		//
		// Envelope envelope = new Envelope();
		// envelope.setHeader(header);
		// envelope.setBody(body);
		//
		// // Marshal to XML String
		// StringWriter writer = new StringWriter();
		// JAXBContext context = JAXBContext.newInstance(Envelope.class);
		// Marshaller marshaller = context.createMarshaller();
		// marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		// marshaller.marshal(envelope, writer);
		// String xmlPayload = writer.toString();
		// ArtemisProducer ArtemisProducer = new ArtemisProducer();
		// ArtemisProducer.sendMessage("AQ_FROM_FIDS_AOT_AOS_TST", xmlPayload);
		//
		// } catch (JAXBException e) {
		// e.printStackTrace();
		// }
	}
}