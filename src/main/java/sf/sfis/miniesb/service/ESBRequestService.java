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
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.ArtemisProducer;
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
import sf.sfis.miniesb.aodb.PlTurn.PtPdDeparture;
import sf.sfis.miniesb.aodb.PlTurn.PtPdDeparture.PlDeparture;
import sf.sfis.miniesb.aodb.PlTurn.PtPdDeparture.PlDeparture.PdRactAircrafttype;
import sf.sfis.miniesb.esb.realtimeinbound.ADID;
import sf.sfis.miniesb.esb.realtimeinbound.MSG;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.INFOBJGENERIC;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.BULKDATA;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJFLIGHT;
import sf.sfis.miniesb.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJVDGS;
import sf.sfis.miniesb.utility.FieldInspector;

@WebService
@Service
@RequiredArgsConstructor
public class ESBRequestService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ESBRequestService.class);
	private final ArtemisProducer artemisProducer;

	ObjectFactory factory = new ObjectFactory();

	@WebMethod
	public void requestAodbInbound(@WebParam(name = "aodbInbound") String xmlString) {
		LOGGER.info("request AODB Inbound...");
//		LOGGER.info(xmlString);
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(MSG.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			MSG msg = (MSG) unmarshaller.unmarshal(new StringReader(xmlString));
			INFOBJGENERIC infobjgeneric = msg.getMSGSTREAMIN().getINFOBJGENERIC();
			String systemType = infobjgeneric.getMESSAGEORIGIN();
			String hopo = infobjgeneric.getHOPO();
			ADID adid = infobjgeneric.getADID();
			String stdt = infobjgeneric.getSTDT();
			String csgn = infobjgeneric.getCSGN();
			String flno = infobjgeneric.getFLNO();

			Header header = new Header();
			Control control = new Control();
			control.setMessageId("localhost:2d970033:195f1193a55:-12d5");
			control.setMessageVersion("1.4");
			control.setMessageType("UPDATE");
			control.setConfirmType("ALL");
			control.setOriginator(systemType);
			control.setSender(systemType);
			control.setReceiver("aodb");
			control.setStation(hopo);
			control.setTimestamp(getLocalDate(infobjgeneric.getTIMESTAMP()));
			header.setControl(control);
			Body body = new Body();
			BULKDATA bulkdata = msg.getMSGSTREAMIN().getMSGOBJECTS().getBULKDATA();
			INFOBJFLIGHT infobjflight = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJFLIGHT();
			INFOBJVDGS infobjvdgs = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJVDGS();
			if (bulkdata != null) {
				String message = bulkdata.getAFTN().getCONTENT();
				body = setBulkData(body, message);
			} else if (infobjflight != null) {
				infobjflight.setSTOA(stdt);
				infobjflight.setSTOD(stdt);
				infobjflight.setCSGN(csgn);
				body = setFlight(adid, body, infobjflight);
			} else if (infobjvdgs != null) {
				body = setVdgs(adid, body, stdt, flno, infobjvdgs);
			}

			Envelope envelope = new Envelope();
			envelope.setHeader(header);
			envelope.setBody(body);

			// Marshal to XML String
			StringWriter writer = new StringWriter();
			JAXBContext context = JAXBContext.newInstance(Envelope.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(envelope, writer);

			if(systemType.equals("AFTN")) {
				LOGGER.info("Send to AQ_FROM_AFTN_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_AFTN_AOT_AOS_TST", writer.toString());
			}else if(systemType.equals("SITA")) {
				LOGGER.info("Send to AQ_FROM_SITA_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_SITA_AOT_AOS_TST", writer.toString());
			}else {
				LOGGER.info("Send to AQ_FROM_FIDS_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_FIDS_AOT_AOS_TST", writer.toString());
			}
			LOGGER.info(writer.toString());

		} catch (JAXBException e) {
			LOGGER.error("requestAodbInbound: ", e);
//			e.printStackTrace();
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
			plDeparture.setPdFlightnumber(factory.createPlTurnPtPdDeparturePlDeparturePdFlightnumber(getAodbpriostring(flno)));

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
			plArrival.setPaFlightnumber(factory.createPlTurnPtPaArrivalPlArrivalPaFlightnumber(getAodbpriostring(flno)));

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
//    	LOGGER.info(String.join(", ", fieldsNotNull));

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
				if (field.equals("csgn")) {
					plDeparture.setPdCallsign(factory
							.createPlTurnPtPdDeparturePlDeparturePdCallsign(getAodbpriostring(infobjflight.getCSGN())));
				} else if (field.equals("rwyd")) {
					plDeparture.setPdRrwyRunway(factory.createPlTurnPtPdDeparturePlDeparturePdRrwyRunway(
							getAodbpriostring(infobjflight.getRWYD())));
				} else if (field.equals("tsat") && !infobjflight.getTSAT().trim().equals("")) {
					plDeparture.setPdTsat(
							factory.createPlTurnPtPdDeparturePlDeparturePdTsat(getAodbDate(infobjflight.getTSAT())));
				} else if (field.equals("ctot")) {
					plDeparture.setPdCtot(
							factory.createPlTurnPtPdDeparturePlDeparturePdCtot(getAodbDate(infobjflight.getCTOT())));
				} else if (field.equals("stod")) {
					plDeparture.setPdSobt(
							factory.createPlTurnPtPdDeparturePlDeparturePdSobt(getAodbDate(infobjflight.getSTOD())));
				} else if (field.equals("asrt")) {
					plDeparture.setPdAsrt(
							factory.createPlTurnPtPdDeparturePlDeparturePdAsrt(getAodbDate(infobjflight.getASRT())));
				} else if (field.equals("asat")) {
					plDeparture.setPdAsat(
							factory.createPlTurnPtPdDeparturePlDeparturePdAsat(getAodbDate(infobjflight.getASAT())));
				} else if (field.equals("airb")) {
					plDeparture.setPdAtot(
							factory.createPlTurnPtPdDeparturePlDeparturePdAtot(getAodbDate(infobjflight.getAIRB())));
					plDeparture.setPdSobt(
							factory.createPlTurnPtPdDeparturePlDeparturePdSobt(getAodbDate(getCurrentDate())));
				} else if (field.equals("ifrd")) {
					plDeparture.setPdFlightrule(factory.createPlTurnPtPdDeparturePlDeparturePdFlightrule(
							getAodbpriostring(infobjflight.getIFRD())));
				} else if (field.equals("acgt")) {
					plDeparture.setPdAcgt(
							factory.createPlTurnPtPdDeparturePlDeparturePdAcgt(getAodbDate(infobjflight.getACGT())));
				} else if (field.equals("tobt")) {
					plDeparture.setPdTobt(
							factory.createPlTurnPtPdDeparturePlDeparturePdTobt(getAodbDate(infobjflight.getTOBT())));
				} else if (field.equals("aegt")) {
					plDeparture.setPdAegt(
							factory.createPlTurnPtPdDeparturePlDeparturePdAegt(getAodbDate(infobjflight.getAEGT())));
				} else if (field.equals("ardt")) {
					plDeparture.setPdDoorclosetime(
							factory.createPlTurnPtPdDeparturePlDeparturePdDoorclosetime(getAodbDate(infobjflight.getARDT())));
				} else if (field.equals("asbt")) {
					plDeparture.setPdArdt(
							factory.createPlTurnPtPdDeparturePlDeparturePdAsbt(getAodbDate(infobjflight.getASBT())));
				}

				PtPdDeparture ptPdDeparture = factory.createPlTurnPtPdDeparture();
				ptPdDeparture.getContent().add(factory.createPlTurnPtPdDeparturePlDeparture(plDeparture));
				plTurn.setPtPdDeparture(factory.createPlTurnPtPdDeparture(ptPdDeparture));
			} else if (plArrival != null) {
				if (field.equals("csgn")) {
					plArrival.setPaCallsign(factory
							.createPlTurnPtPaArrivalPlArrivalPaCallsign(getAodbpriostring(infobjflight.getCSGN())));
				} else if (field.equals("stoa")) {
					plArrival.setPaSibt(
							factory.createPlTurnPtPaArrivalPlArrivalPaSibt(getAodbDate(infobjflight.getSTOA())));
				} else if (field.equals("rwya")) {
					plArrival.setPaRrwyRunway(factory
							.createPlTurnPtPaArrivalPlArrivalPaRrwyRunway(getAodbpriostring(infobjflight.getRWYA())));
				} else if (field.equals("tldt")) {
					plArrival.setPaTldt(
							factory.createPlTurnPtPaArrivalPlArrivalPaTldt(getAodbDate(infobjflight.getTLDT())));
				} else if (field.equals("tmoa")) {
					plArrival.setPaFnlt(
							factory.createPlTurnPtPaArrivalPlArrivalPaFnlt(getAodbDate(infobjflight.getTMOA())));
				} else if (field.equals("land")) {
					plArrival.setPaAldt(
							factory.createPlTurnPtPaArrivalPlArrivalPaAldt(getAodbDate(infobjflight.getLAND())));
					plArrival.setPaSibt(
							factory.createPlTurnPtPaArrivalPlArrivalPaSibt(getAodbDate(getCurrentDate())));
				} else if (field.equals("ifra")) {
					plArrival.setPaFlightrule(factory
							.createPlTurnPtPaArrivalPlArrivalPaFlightrule(getAodbpriostring(infobjflight.getIFRA())));
				}

				PtPaArrival ptPaArrival = factory.createPlTurnPtPaArrival();
				ptPaArrival.getContent().add(factory.createPlTurnPtPaArrivalPlArrival(plArrival));
				plTurn.setPtPaArrival(factory.createPlTurnPtPaArrival(ptPaArrival));
			}
		}
		body.setPlTurn(plTurn);
		return body;
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
			if(!value.trim().equals("")) {
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
			LOGGER.error("getAodbDate: ", e);
//			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			LOGGER.error("getAodbDate: ", e);
//			e.printStackTrace();
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
//        System.out.println(formatted); // เช่น 20250524000000
        return formatted;
	}

	// For test only
	public static void main(String[] args) {
//		try {
//			Header header = new Header();
//			Control control = new Control();
//			control.setMessageId("localhost:2d970033:195f1193a55:-12d5");
//			control.setMessageVersion("1.4");
//			control.setMessageType("SUBSCRIBE");
//			control.setSender("FIDS");
//			control.setTimestamp("2025-04-01T21:32:00");
//			Request request = new Request();
//			request.setDatatype("pl_turn");
//			request.setStartTime("2025-05-09T00:00:00");
//			request.setEndTime("2025-05-09T23:59:59");
//			control.setRequest(request);
//			header.setControl(control);
//			Body body = new Body();
//
//			Envelope envelope = new Envelope();
//			envelope.setHeader(header);
//			envelope.setBody(body);
//
//			// Marshal to XML String
//			StringWriter writer = new StringWriter();
//			JAXBContext context = JAXBContext.newInstance(Envelope.class);
//			Marshaller marshaller = context.createMarshaller();
//			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//			marshaller.marshal(envelope, writer);
//			String xmlPayload = writer.toString();
//			ArtemisProducer ArtemisProducer = new ArtemisProducer();
//			ArtemisProducer.sendMessage("AQ_FROM_FIDS_AOT_AOS_TST", xmlPayload);
//
//		} catch (JAXBException e) {
//			e.printStackTrace();
//		}
	}
}