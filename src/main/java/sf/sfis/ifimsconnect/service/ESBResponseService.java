package sf.sfis.ifimsconnect.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sf.sfis.ifimsconnect.MQWebSphereProducer;
import sf.sfis.ifimsconnect.aodb.Envelope;
import sf.sfis.ifimsconnect.aodb.Fault;
import sf.sfis.ifimsconnect.aodb.IfAdexpmessage;
import sf.sfis.ifimsconnect.controller.RedisController;
import sf.sfis.ifimsconnect.esb.MSG.NACKDETAIL;
import sf.sfis.ifimsconnect.esb.ObjectFactory;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.ACTIONTYPE;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.ADID;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.FLTI;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJFLIGHT;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJGENERIC;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.TIMEID;
import sf.sfis.ifimsconnect.model.FidsAfttab;
import sf.sfis.ifimsconnect.utility.DateTimeFormatHelper;
import sf.sfis.ifimsconnect.utility.FieldInspector;
import sf.sfis.ifimsconnect.utility.GetterAccess;
import sf.sfis.ifimsconnect.utility.TranformFidsAfttab;

@Slf4j
@Service
@RequiredArgsConstructor
public class ESBResponseService {
	private final DateTimeFormatHelper dateTimeFormatHelper;
	private final TranformFidsAfttab tranformFidsAfttab;
	private final FidsAfttabService fidsAfttabService;
	private final FidsCcatabService fidsCcatabService;
	private final FidsGateHistoryService fidsGateHistoryService;
	private final FidsFinalcallHistoryService fidsFinalcallHistoryService;
	private final RedisController redisController;
	private final MQWebSphereProducer webSphereProducer;
	// Dedicated logger for outbound XML to ESB → routed to logs/outbound/<hopo>/<queue>.log
    private static final Logger sendEsbLog = LoggerFactory.getLogger("SEND_ESB_XML");

	// true = ส่ง flight update ออก ESB ผ่าน WebSphere MQ, false = ส่งผ่าน webservice (callWebserviceUpdate)
	// ใช้ flag เดียวกับที่ปิด/เปิด WebSphere MQ เพื่อไม่ให้ส่งซ้ำ และ prod (MQ ยังไม่มา) จะ fallback เป็น webservice
	@Value("${websphere.mq.enabled:true}")
	private boolean webSphereEnabled;

	// Cached JAXBContexts — thread-safe and expensive to build, so create once
	// instead
	// of per message. (Marshaller/Unmarshaller are not thread-safe and stay per
	// call.)
	private static final JAXBContext ENVELOPE_CTX = newContext(Envelope.class);
	private static final JAXBContext ESB_MSG_CTX = newContext(sf.sfis.ifimsconnect.esb.MSG.class);
	private static final JAXBContext OUT_MSG_CTX = newContext(MSG.class);

	private static JAXBContext newContext(Class<?> clazz) {
		try {
			return JAXBContext.newInstance(clazz);
		} catch (JAXBException e) {
			throw new IllegalStateException("Failed to init JAXBContext for " + clazz.getName(), e);
		}
	}

	public void convertXMLtoObject(String xml) {
		try {
			log.info("ESBResponseService...");
			JAXBContext jaxbContext = ENVELOPE_CTX;
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			Envelope envelope = (Envelope) unmarshaller.unmarshal(new StringReader(xml));
			String type = envelope.getHeader().getControl().getMessageType();
			String timestamp = dateTimeFormatHelper.convertLocalToUTC(envelope.getHeader().getControl().getTimestamp());
			String hopo = envelope.getHeader().getControl().getStation();
			String originator = envelope.getHeader().getControl().getOriginator();

			log.info("Message Type: " + type);
			StringWriter writer = new StringWriter();
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(envelope, writer);

			// TranformFidsAfttab tranformFidsAfttab = new TranformFidsAfttab();
			// Insert or Update all fields on FidsAfttab.
			FidsAfttab fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), "DATASET", hopo, "A");
			if (fidsAfttab != null) {
				if (fidsAfttab.getUrno() != null) {
					fidsAfttab = fidsAfttabService.saveFidsAfttab(fidsAfttab);
					if ((fidsAfttab.getGtd1() != null && fidsAfttab.getGtd1().length() > 0)
							|| (fidsAfttab.getGtd2() != null && fidsAfttab.getGtd2().length() > 0)) {
						fidsGateHistoryService.updateGateChangeHistory(fidsAfttab);
					}
					if (fidsAfttab.getRemp() != null && fidsAfttab.getRemp().length() > 0) {
						fidsFinalcallHistoryService.updateFinalCallHistory(fidsAfttab);
					}
				}
			}
			fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), "DATASET", hopo, "D");
			if (fidsAfttab != null) {
				;
				fidsCcatabService.updateCcatab(fidsAfttab);
				if (fidsAfttab.getUrno() != null) {
					fidsAfttab = fidsAfttabService.saveFidsAfttab(fidsAfttab);
					if ((fidsAfttab.getGtd1() != null && fidsAfttab.getGtd1().length() > 0)
							|| (fidsAfttab.getGtd2() != null && fidsAfttab.getGtd2().length() > 0)) {
						fidsGateHistoryService.updateGateChangeHistory(fidsAfttab);
					}
					if (fidsAfttab.getRemp() != null && fidsAfttab.getRemp().length() > 0) {
						fidsFinalcallHistoryService.updateFinalCallHistory(fidsAfttab);
					}
				}
			}

			if (type.equalsIgnoreCase("UPDATE")) {// Send update fields to ESB by Web service.
				fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), type, hopo, "A", originator);
				if (fidsAfttab != null) {
					String xmlEsb = convertFidsAfftabtoEsb(timestamp, originator, fidsAfttab);
					if (xmlEsb != null) {
						log.info("Update arrival flight to ESB...");
						// log.info(xmlEsb);
						sendFlightUpdate(hopo, fidsAfttab.getAction(), xmlEsb);
					} else {
						log.info("No data found for ESB update.");
					}
				}
				fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), type, hopo, "D", originator);
				if (fidsAfttab != null) {
					String xmlEsb = convertFidsAfftabtoEsb(timestamp, originator, fidsAfttab);
					if (xmlEsb != null) {
						log.info("Update departure flight to ESB...");
						// log.info(xmlEsb);
						sendFlightUpdate(hopo, fidsAfttab.getAction(), xmlEsb);
					} else {
						log.info("No data found for ESB update.");
					}
				}
				// Save data to Redis.
				redisController.saveData(hopo);
			} else if (!type.equalsIgnoreCase("DATASET")) {// Send ACK or NACK to ESB by Web service.
				// ส่ง Webservice ในกรณีที่ ESB Request อัพเดทข้อมูล Flight แล้ว TSystem ส่ง
				// ACK/NACK กลับมา
				String contentBody = getContentBody(writer.toString());
				String xmlEsb = convertResponseMessagetoEsb(timestamp, envelope, contentBody);
				// ACK (หรือ marshal ล้มเหลว) จะได้ xmlEsb == null → ไม่ต้องส่ง webservice
				// (ถ้าไม่เช็คจะเรียก callWebserviceResponse(null) แล้วโยน NPE ตอน addTextNode(null)
				//  ทำให้ได้ ERROR log ทุกครั้งที่มี ACK และ SOAPConnection ไม่ถูกปิด)
				if (xmlEsb != null) {
					log.info("Call Web service response NACK...");
					//log.info(xmlEsb);
					MDC.put("sendEsbKey", hopo + "/outbound-ACK_NACK");
					try {
						sendEsbLog.info(xmlEsb);
					} finally {
						MDC.remove("sendEsbKey");
					}
					callWebserviceResponse(xmlEsb);
				} else {
					log.info("ACK message ignored (no ESB response to send).");
				}
			}
		} catch (JAXBException e) {
			log.error("convertXMLtoObject: ", e);
			// e.printStackTrace();
		} catch (Exception e) {
			log.error("convertXMLtoObject: ", e);
			e.printStackTrace();
		}
	}

	public String convertResponseMessagetoEsb(String updateTime, Envelope envelope, String bodyContent) {
		try {
			StringWriter writer = new StringWriter();
			ObjectFactory factory = new ObjectFactory();
			sf.sfis.ifimsconnect.esb.MSG msgReturn = new sf.sfis.ifimsconnect.esb.MSG();
			msgReturn.setMSGORIGIN(envelope.getHeader().getControl().getSender());
			msgReturn.setTIME(updateTime);
			msgReturn.setACKTYPE(envelope.getHeader().getControl().getMessageType());

			IfAdexpmessage ifAdexpmessage = envelope.getBody().getIfAdexpmessage();
			if (ifAdexpmessage != null) {
				String iamOriginalmessage = GetterAccess.get(ifAdexpmessage, p -> p.getIamOriginalmessage(),
						v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
				// log.info("AFTN Message : " + iamOriginalmessage);
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
			} else {
				// ใช้สำหรับปิดการรับข้อมูลที่เป็น ACK
				return null;
			}

			JAXBContext context = ESB_MSG_CTX;
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(msgReturn, writer);
			// log.info("convertResponseMessagetoEsb...");
			// log.info(writer.toString());
			return writer.toString();
		} catch (JAXBException e) {
			log.error("convertResponseMessagetoEsb: ", e);
			// e.printStackTrace();
		}
		return null;
	}

	public String convertFidsAfftabtoEsb(String updateTime, String originator, FidsAfttab fidsAfttab) {
		StringWriter writer = new StringWriter();
		MSG esbAfttab = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();
		MSG.MSGSTREAMOUT.INFOBJGENERIC infobjgeneric = new INFOBJGENERIC();
		infobjgeneric.setMESSAGETYPE("UFISFLTUD");
		infobjgeneric.setMESSAGEORIGIN("AOS");
		infobjgeneric.setTIMEID(TIMEID.UTC);
		infobjgeneric.setTIMESTAMP(updateTime);
		infobjgeneric.setACTIONTYPE(fidsAfttab.getAction().equalsIgnoreCase("insert") ? ACTIONTYPE.I : ACTIONTYPE.U);
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
		// Set<String> copyFields = new HashSet<>(Arrays.asList("fpla" ,"fpld", "eldt",
		// "etdi", "atot", "etot", "ctot", "etai", "tldt", "tmoa",
		// "rwya", "rwyd", "land", "airb", "ifra", "ifrd", "onbl", "ofbl", "acgt",
		// "ttot", "tobt", "tsat", "asbt",
		// "remp", "ardt", "asrt", "asat"));
		copyMatchingFields(fidsAfttab.getFieldsNotNull(), fidsAfttab, infobjflight);

		// IDEP: ต้องส่ง TSAT ไป ESB เสมอ แม้ TSAT จะไม่ได้อยู่ในชุด field ที่เปลี่ยน (fieldsNotNull)
		if ("IDEP".equalsIgnoreCase(originator)
				&& fidsAfttab.getTsat() != null && !fidsAfttab.getTsat().trim().isEmpty()) {
			infobjflight.setTSAT(fidsAfttab.getTsat());
		}

		if (!FieldInspector.allFieldsAreNull(infobjflight)) {
			// Fix field for ESB
			infobjflight.setFLTI(FLTI.valueOf(fidsAfttab.getFlti()));
			infobjflight.setRKEY(fidsAfttab.getRkey().toString());
			infobjflight.setRTYP(fidsAfttab.getRtyp());

			infobjflight.setFLNO(infobjflight.getFLNO() != null ? infobjflight.getFLNO().trim() : null);
			infobjflight.setFLTN(infobjflight.getFLTN() != null ? infobjflight.getFLTN().trim() : null);

			// Different field between FIDS and ESB
			infobjflight.setSLOT(fidsAfttab.getCtot());

			msgstreamout.setINFOBJGENERIC(infobjgeneric);
			msgstreamout.setINFOBJFLIGHT(infobjflight);
			esbAfttab.setMSGSTREAMOUT(msgstreamout);
			try {
				JAXBContext context = OUT_MSG_CTX;
				Marshaller marshaller = context.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				marshaller.marshal(esbAfttab, writer);
				return writer.toString();
			} catch (JAXBException e) {
				log.error("convertFidsAfftabtoEsb: ", e);
				// e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * ส่ง flight UPDATE ออกไป ESB — เลือกช่องทางเดียวตาม flag webSphereEnabled:
	 * เปิด = WebSphere MQ, ปิด = webservice (callWebserviceUpdate) เพื่อไม่ให้ส่งซ้ำ.
	 */
	private void sendFlightUpdate(String hopo, String action, String xmlEsb) {
		if (webSphereEnabled) {
			//String queueName = "insert".equalsIgnoreCase(action) ? "UFIS_INSERT_FLIGHT_OUT" : "UFIS_FLIGHT_OUT";
			String queueName = "UFIS_FLIGHT_OUT_" + hopo.toUpperCase();
			if (hopo.equalsIgnoreCase("BKK")) {
				webSphereProducer.sendToMachine1(queueName, hopo, xmlEsb);
			} else {
				//queueName = queueName + "_" + hopo.toUpperCase();
				webSphereProducer.sendToMachine2(queueName, hopo, xmlEsb);
			}
		} else {
			// WebSphere MQ ปิดอยู่ → ส่งผ่าน webservice แทน (เก็บ payload ลง outbound ด้วย)
			MDC.put("sendEsbKey", hopo + "/outbound-WS");
			try {
				sendEsbLog.info(xmlEsb);
			} finally {
				MDC.remove("sendEsbKey");
			}
			callWebserviceUpdate(xmlEsb);
		}
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
			// System.out.println("Response SOAP Message:");
			// soapResponse.writeTo(System.out);

			soapConnection.close();
		} catch (UnsupportedOperationException e) {
			log.error("callWebserviceResponse: ", e);
			// e.printStackTrace();
		} catch (SOAPException e) {
			log.error("callWebserviceResponse: ", e);
			// e.printStackTrace();
		} catch (Exception e) {
			log.error("callWebserviceResponse: ", e);
			// e.printStackTrace();
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
			// System.out.println("Response SOAP Message:");
			// soapResponse.writeTo(System.out);

			soapConnection.close();
		} catch (UnsupportedOperationException e) {
			log.error("callWebserviceUpdate: ", e);
			// e.printStackTrace();
		} catch (SOAPException e) {
			log.error("callWebserviceUpdate: ", e);
			// e.printStackTrace();
		} catch (Exception e) {
			log.error("callWebserviceUpdate: ", e);
			// e.printStackTrace();
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
				if (value != null && !value.toString().equals("") && updateFields.contains(sourceField.getName())) {
					// หา field ชื่อเดียวกันใน target
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
				log.error("copyMatchingFields: ", e);
				// e.printStackTrace();
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