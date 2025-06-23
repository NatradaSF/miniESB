package sf.sfis.miniesb.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.aodb.Envelope;
import sf.sfis.miniesb.aodb.Fault;
import sf.sfis.miniesb.aodb.IfAdexpmessage;
import sf.sfis.miniesb.controller.RedisController;
import sf.sfis.miniesb.esb.MSG.NACKDETAIL;
import sf.sfis.miniesb.esb.ObjectFactory;
import sf.sfis.miniesb.esb.realtimeoutbound.ACTIONTYPE;
import sf.sfis.miniesb.esb.realtimeoutbound.ADID;
import sf.sfis.miniesb.esb.realtimeoutbound.FLTI;
import sf.sfis.miniesb.esb.realtimeoutbound.MSG;
import sf.sfis.miniesb.esb.realtimeoutbound.MSG.MSGSTREAMOUT;
import sf.sfis.miniesb.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJFLIGHT;
import sf.sfis.miniesb.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJGENERIC;
import sf.sfis.miniesb.esb.realtimeoutbound.TIMEID;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.model.FidsAirport;
import sf.sfis.miniesb.repository.FidsAirportRepository;
import sf.sfis.miniesb.utility.DateTimeFormatHelper;
import sf.sfis.miniesb.utility.FieldInspector;
import sf.sfis.miniesb.utility.GetterAccess;
import sf.sfis.miniesb.utility.TranformFidsAfttab;

@Service
@RequiredArgsConstructor
public class ESBResponseService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ESBResponseService.class);
	private final DateTimeFormatHelper dateTimeFormatHelper;
	private final TranformFidsAfttab tranformFidsAfttab;
	private final FidsAfttabService fidsAfttabService;
	private final FidsCcatabService fidsCcatabService;
	private final FidsGateHistoryService fidsGateHistoryService;
	private final FidsFinalcallHistoryService fidsFinalcallHistoryService;
	private final FidsAirportRepository fidsAirportRepository;
	private final RedisController redisController;

	public void convertXMLtoObject(String xml) {
		try {
			LOGGER.info("ESBResponseService...");
			JAXBContext jaxbContext = JAXBContext.newInstance(Envelope.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			LOGGER.info(xml);

			Envelope envelope = (Envelope) unmarshaller.unmarshal(new StringReader(xml));
			String type = envelope.getHeader().getControl().getMessageType();
			String timestamp = dateTimeFormatHelper.convertLocalToUTC(envelope.getHeader().getControl().getTimestamp());
			String hopo = envelope.getHeader().getControl().getStation();
			String airport4 = "";
			Optional<FidsAirport> queryFidsAirport = fidsAirportRepository.findById(hopo);
			if (queryFidsAirport.isPresent()) {
				airport4 = queryFidsAirport.get().getApc4();
			}

			LOGGER.info("Message Type: " + type);
			StringWriter writer = new StringWriter();
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(envelope, writer);

//			TranformFidsAfttab tranformFidsAfttab = new TranformFidsAfttab();
			//Insert or Update all fields on FidsAfttab.
			FidsAfttab fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), "DATASET", "A");
			if (fidsAfttab != null) {
				fidsAfttab.setHopo(hopo);
				FieldInspector.replaceHoldWithEmpty(fidsAfttab);
				if(fidsAfttab.getUrno()!=null) {
					fidsAfttab.setDes3(hopo);
					fidsAfttab.setDes4(airport4);
					fidsAfttab = fidsAfttabService.saveFidsAfttab(fidsAfttab);
					if((fidsAfttab.getGtd1()!=null && fidsAfttab.getGtd1().length()>0)||(fidsAfttab.getGtd2()!=null && fidsAfttab.getGtd2().length()>0)) {
						fidsGateHistoryService.updateGateChangeHistory(fidsAfttab);
					}
					if(fidsAfttab.getRemp()!=null && fidsAfttab.getRemp().length()>0) {
						fidsFinalcallHistoryService.updateFinalCallHistory(fidsAfttab);
					}
				}
			}
			fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), "DATASET", "D");
			if (fidsAfttab != null) {
				fidsAfttab.setHopo(hopo);
				FieldInspector.replaceHoldWithEmpty(fidsAfttab);
				fidsCcatabService.updateCcatab(fidsAfttab);
				if(fidsAfttab.getUrno()!=null) {
					fidsAfttab.setOrg3(hopo);
					fidsAfttab.setOrg4(airport4);
					fidsAfttab = fidsAfttabService.saveFidsAfttab(fidsAfttab);
					if((fidsAfttab.getGtd1()!=null && fidsAfttab.getGtd1().length()>0)||(fidsAfttab.getGtd2()!=null && fidsAfttab.getGtd2().length()>0)) {
						fidsGateHistoryService.updateGateChangeHistory(fidsAfttab);
					}
					if(fidsAfttab.getRemp()!=null && fidsAfttab.getRemp().length()>0) {
						fidsFinalcallHistoryService.updateFinalCallHistory(fidsAfttab);
					}
				}
			}
			
			if (type.equalsIgnoreCase("UPDATE")) {//Send update fields to ESB by Web service.
				fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), type, "A");
				if (fidsAfttab != null) {
					fidsAfttab.setHopo(hopo);
					String xmlEsb = convertFidsAfftabtoEsb(timestamp, fidsAfttab);
					if(xmlEsb!=null) {
						LOGGER.info("Call Web service update arrival flight...");
						LOGGER.info(xmlEsb);
						callWebserviceUpdate(xmlEsb);
					}else {
						LOGGER.info("No data found for ESB update.");
					}
				}
				fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), type, "D");
				if (fidsAfttab != null) {
					fidsAfttab.setHopo(hopo);
					String xmlEsb = convertFidsAfftabtoEsb(timestamp, fidsAfttab);
					if(xmlEsb!=null) {
						LOGGER.info("Call Web service update departure flight...");
						LOGGER.info(xmlEsb);
						callWebserviceUpdate(xmlEsb);
					}else {
						LOGGER.info("No data found for ESB update.");
					}
				}
				//Save data to Redis.
				redisController.saveData(hopo);
			}else if (!type.equalsIgnoreCase("DATASET")) {//Send ACK or NACK to ESB by Web service.
				String contentBody = getContentBody(writer.toString());
				String xmlEsb = convertResponseMessagetoEsb(timestamp, envelope, contentBody);
				LOGGER.info("Call Web service response...");
				LOGGER.info(xmlEsb);
				callWebserviceResponse(xmlEsb);
			}
		} catch (JAXBException e) {
			LOGGER.error("convertXMLtoObject: ", e);
//			e.printStackTrace();
		} catch (Exception e) {
			LOGGER.error("convertXMLtoObject: ", e);
			e.printStackTrace();
		}
	}

	public String convertResponseMessagetoEsb(String updateTime, Envelope envelope, String bodyContent) {
		try {
			StringWriter writer = new StringWriter();
			ObjectFactory factory = new ObjectFactory();
			sf.sfis.miniesb.esb.MSG msgReturn = new sf.sfis.miniesb.esb.MSG();
			msgReturn.setMSGORIGIN(envelope.getHeader().getControl().getSender());
			msgReturn.setTIME(updateTime);
			msgReturn.setACKTYPE(envelope.getHeader().getControl().getMessageType());

			IfAdexpmessage ifAdexpmessage = envelope.getBody().getIfAdexpmessage();
			if (ifAdexpmessage != null) {
				String iamOriginalmessage = GetterAccess.get(ifAdexpmessage, p -> p.getIamOriginalmessage(),
						v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				LOGGER.info("AFTN Message : " + iamOriginalmessage);
				msgReturn.setMESSAGE(iamOriginalmessage);
			} else {
				msgReturn.setMESSAGE(bodyContent);
			}

			String type = envelope.getHeader().getControl().getMessageType();
			if (type.equals("NACK")) {
				Fault fault = envelope.getBody().getFault();
				NACKDETAIL nackdetail = new NACKDETAIL();
				nackdetail.setFaultcode(factory.createMSGNACKDETAILFaultcode(fault.getFaultcode()));
				nackdetail.setFaultactor(factory.createMSGNACKDETAILFaultactor(fault.getFaultactor()));
				nackdetail.setFaultstring(factory.createMSGNACKDETAILFaultstring(fault.getFaultstring()));
				nackdetail.setFaultdetail(factory.createMSGNACKDETAILFaultdetail(fault.getDetail()));
				msgReturn.setNACKDETAIL(factory.createMSGNACKDETAIL(nackdetail));
			}

			JAXBContext context = JAXBContext.newInstance(sf.sfis.miniesb.esb.MSG.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(msgReturn, writer);
//			LOGGER.info("convertResponseMessagetoEsb...");
//			LOGGER.info(writer.toString());
			return writer.toString();
		} catch (JAXBException e) {
			LOGGER.error("convertResponseMessagetoEsb: ", e);
//			e.printStackTrace();
		}
		return null;
	}

	public String convertFidsAfftabtoEsb(String updateTime, FidsAfttab fidsAfttab) {
		StringWriter writer = new StringWriter();
		MSG esbAfttab = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();
		MSG.MSGSTREAMOUT.INFOBJGENERIC infobjgeneric = new INFOBJGENERIC();
		infobjgeneric.setMESSAGETYPE("UFISFLTUD");
		infobjgeneric.setMESSAGEORIGIN("UFIS");
		infobjgeneric.setTIMEID(TIMEID.UTC);
		infobjgeneric.setTIMESTAMP(updateTime);
		infobjgeneric.setACTIONTYPE(ACTIONTYPE.U);
		infobjgeneric.setHOPO(fidsAfttab.getHopo());
		infobjgeneric.setURNO(fidsAfttab.getUrno() != null ? fidsAfttab.getUrno().toString() : null);
		infobjgeneric.setADID(ADID.valueOf(fidsAfttab.getAdid()));
		infobjgeneric.setHOPO(fidsAfttab.getHopo());
		infobjgeneric.setSTDT(infobjgeneric.getADID() == ADID.A ? fidsAfttab.getStoa() : fidsAfttab.getStod());
		infobjgeneric.setFLNO(fidsAfttab.getFlno().trim());
		infobjgeneric.setCSGN(fidsAfttab.getCsgn());
		infobjgeneric.setRKEY(fidsAfttab.getRkey() != null ? fidsAfttab.getRkey().toString() : null);
		infobjgeneric.setRTYP(fidsAfttab.getRtyp());

		MSG.MSGSTREAMOUT.INFOBJFLIGHT infobjflight = new INFOBJFLIGHT();
//		Set<String> copyFields = new HashSet<>(Arrays.asList("fpla" ,"fpld", "eldt", "etdi", "atot", "etot", "ctot", "etai", "tldt", "tmoa",
//				"rwya", "rwyd", "land", "airb", "ifra", "ifrd", "onbl", "ofbl", "acgt", "ttot", "tobt", "tsat", "asbt",
//				"remp", "ardt", "asrt", "asat"));
		copyMatchingFields(fidsAfttab.getFieldsNotNull(), fidsAfttab, infobjflight);
		
		if(!FieldInspector.allFieldsAreNull(infobjflight)){
			// Fix field for ESB
			infobjflight.setFLTI(FLTI.valueOf(fidsAfttab.getFlti()));
			infobjflight.setRKEY(fidsAfttab.getRkey().toString());
			infobjflight.setRTYP(fidsAfttab.getRtyp());
			
			infobjflight.setFLNO(infobjflight.getFLNO()!=null?infobjflight.getFLNO().trim():null);
			infobjflight.setFLTN(infobjflight.getFLTN()!=null?infobjflight.getFLTN().trim():null);

			// Different field between FIDS and ESB
			infobjflight.setSLOT(fidsAfttab.getCtot());

			msgstreamout.setINFOBJGENERIC(infobjgeneric);
			msgstreamout.setINFOBJFLIGHT(infobjflight);
			esbAfttab.setMSGSTREAMOUT(msgstreamout);
			try {
				JAXBContext context = JAXBContext.newInstance(MSG.class);
				Marshaller marshaller = context.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				marshaller.marshal(esbAfttab, writer);
				return writer.toString();
			} catch (JAXBException e) {
				LOGGER.error("convertFidsAfftabtoEsb: ", e);
//				e.printStackTrace();
			}
		}
		return null;
	}

	public void callWebserviceResponse(String xmlEsb) {
		// Create SOAP Connection
		SOAPConnectionFactory soapConnectionFactory;
		try {
			soapConnectionFactory = SOAPConnectionFactory.newInstance();

			SOAPConnection soapConnection = soapConnectionFactory.createConnection();

			// Define the endpoint URL
			String url = "http://esbv10:5555/ws/IFIMS.Service.CommonService:BKK_IFIMS_MessageResponse_WSD/IFIMS_Service_CommonService_BKK_IFIMS_MessageResponse_WSD_Port";

			// Create the SOAP Request
			SOAPMessage soapRequest = createSoapRequest(xmlEsb, url, "BKK_IFIMS_MessageResponse");

			// Send request and receive response
			SOAPMessage soapResponse = soapConnection.call(soapRequest, url);

			// Print response
//	        System.out.println("Response SOAP Message:");
//	        soapResponse.writeTo(System.out);

			soapConnection.close();
		} catch (UnsupportedOperationException e) {
			LOGGER.error("callWebserviceResponse: ", e);
//			e.printStackTrace();
		} catch (SOAPException e) {
			LOGGER.error("callWebserviceResponse: ", e);
//			e.printStackTrace();
		} catch (Exception e) {
			LOGGER.error("callWebserviceResponse: ", e);
//			e.printStackTrace();
		}
	}

	public void callWebserviceUpdate(String xmlEsb) {
		// Create SOAP Connection
		SOAPConnectionFactory soapConnectionFactory;
		try {
			soapConnectionFactory = SOAPConnectionFactory.newInstance();

			SOAPConnection soapConnection = soapConnectionFactory.createConnection();

			// Define the endpoint URL
			String url = "http://esbv10:5555/ws/IFIMS.Service.FlowService.Online.Publish.BKK:AODB_FlightOutbound_WSD/IFIMS_Service_FlowService_Online_Publish_BKK_AODB_FlightOutbound_WSD_Port";

			// Create the SOAP Request
			SOAPMessage soapRequest = createSoapRequest(xmlEsb, url, "AODB_FlightOutbound");

			// Send request and receive response
			SOAPMessage soapResponse = soapConnection.call(soapRequest, url);

			// Print response
//	        System.out.println("Response SOAP Message:");
//	        soapResponse.writeTo(System.out);

			soapConnection.close();
		} catch (UnsupportedOperationException e) {
			LOGGER.error("callWebserviceUpdate: ", e);
//			e.printStackTrace();
		} catch (SOAPException e) {
			LOGGER.error("callWebserviceUpdate: ", e);
//			e.printStackTrace();
		} catch (Exception e) {
			LOGGER.error("callWebserviceUpdate: ", e);
//			e.printStackTrace();
		}
	}

	private SOAPMessage createSoapRequest(String xmlEsb, String url, String elementName) throws Exception {
		// Create message
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();

		// SOAP Envelope
		SOAPPart soapPart = soapMessage.getSOAPPart();
		SOAPEnvelope envelope = soapPart.getEnvelope();

		String namespace = "tns";
		envelope.addNamespaceDeclaration(namespace, url);

		// SOAP Body
		SOAPBody soapBody = envelope.getBody();
		SOAPElement responseElement = soapBody.addChildElement(elementName, namespace);
		SOAPElement inputMsg = responseElement.addChildElement("inputMsg");
		inputMsg.addTextNode(xmlEsb);
		soapMessage.saveChanges();
		soapMessage.writeTo(System.out);
		return soapMessage;
	}

	public void copyMatchingFields(List<String> updateFields, Object source, Object target) {
		if (source == null || target == null) {
			throw new IllegalArgumentException("Source and target must not be null");
		}

		Class<?> sourceClass = source.getClass();
		Class<?> targetClass = target.getClass();

		for (Field sourceField : sourceClass.getDeclaredFields()) {
			sourceField.setAccessible(true);
			try {
				Object value = sourceField.get(source);
				if (value != null  && !value.toString().equals("") && updateFields.contains(sourceField.getName())) {
//					 หา field ชื่อเดียวกันใน target
					try {
						Field targetField = targetClass.getDeclaredField(sourceField.getName());
						targetField.setAccessible(true);
						if (targetField.getType().equals(sourceField.getType())) {
							targetField.set(target, value);
						}
					} catch (NoSuchFieldException ignore) {
						// ไม่มี field นี้ใน target — ข้ามไป
					}
				}
			} catch (IllegalAccessException e) {
				LOGGER.error("copyMatchingFields: ", e);
//				e.printStackTrace();
			}
		}
	}

	public String getContentBody(String xml) {
		String startTag = "<pl_turn>";
		String endTag = "</pl_turn>";

		int start = xml.indexOf(startTag);
		int end = xml.indexOf(endTag);

		if (start != -1 && end != -1) {
			return xml.substring(start, end + endTag.length()).replaceAll("(?m)^[ \t]*\r?\n", "");
		} else {
			return null;
		}
	}
}