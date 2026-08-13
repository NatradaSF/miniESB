package sf.sfis.ifimsconnect.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

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
import sf.sfis.ifimsconnect.esb.realtimeoutbound.CTYP;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.FLTI;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.BULKDATA;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.CONCAT;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJACPOSITION;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJBELT;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJCOUNTER;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJFLIGHT;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJGATE;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJGENERIC;
import sf.sfis.ifimsconnect.esb.realtimeoutbound.MSG.MSGSTREAMOUT.INFOBJVDGS;
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
	// Dedicated logger for outbound XML to ESB → routed to
	// logs/outbound/<hopo>/<queue>.log
	private static final Logger sendEsbLog = LoggerFactory.getLogger("SEND_ESB_XML");

	// true = ส่ง flight update ออก ESB ผ่าน WebSphere MQ (IBM MQ อย่างเดียว)
	// false = ส่งผ่าน webservice (callWebserviceUpdate) แทน
	// ใช้ flag เดียวกับที่ปิด/เปิด WebSphere MQ เพื่อไม่ให้ส่งซ้ำ และ prod (MQ
	// ยังไม่มา) จะ fallback เป็น webservice
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
						sendToOutboundQueue("UFIS_FLIGHT_OUT", hopo, xmlEsb);
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
						sendToOutboundQueue("UFIS_FLIGHT_OUT", hopo, xmlEsb);
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
				// (ถ้าไม่เช็คจะเรียก callWebserviceResponse(null) แล้วโยน NPE ตอน
				// addTextNode(null)
				// ทำให้ได้ ERROR log ทุกครั้งที่มี ACK และ SOAPConnection ไม่ถูกปิด)
				if (xmlEsb != null) {
					log.info("Call Web service response NACK...");
					// log.info(xmlEsb);
					MDC.put("sendEsbKey", hopo + "/outbound-NACK");
					try {
						sendEsbLog.info(xmlEsb);
					} finally {
						MDC.remove("sendEsbKey");
					}
					//callWebserviceResponse(xmlEsb);
					sendEmailResponse(xmlEsb, hopo);
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
		// INFOBJ_GENERIC — ใช้ helper ร่วมกับ counter/gate (แก้ logic ที่เดียว)
		MSG.MSGSTREAMOUT.INFOBJGENERIC infobjgeneric = buildOutboundGeneric("UFISFLTUD", updateTime, fidsAfttab);

		MSG.MSGSTREAMOUT.INFOBJFLIGHT infobjflight = new INFOBJFLIGHT();
		// Set<String> copyFields = new HashSet<>(Arrays.asList("fpla" ,"fpld", "eldt",
		// "etdi", "atot", "etot", "ctot", "etai", "tldt", "tmoa",
		// "rwya", "rwyd", "land", "airb", "ifra", "ifrd", "onbl", "ofbl", "acgt",
		// "ttot", "tobt", "tsat", "asbt",
		// "remp", "ardt", "asrt", "asat"));
		copyMatchingFields(fidsAfttab.getFieldsNotNull(), fidsAfttab, infobjflight);

		// IDEP: ต้องส่ง TSAT ไป ESB เสมอ แม้ TSAT จะไม่ได้อยู่ในชุด field ที่เปลี่ยน
		// (fieldsNotNull)
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
	 * ปั้น XML สำหรับคิว UFIS_COUNTER_OUT (UFISCHKUD — check-in counter update).
	 * ส่วน INFOBJ_GENERIC ตั้งค่าจาก {@link FidsAfttab} แบบเดียวกับ
	 * {@link #convertFidsAfftabtoEsb}
	 * (dynamic ตามเที่ยวบิน) — ต่างกันแค่ MESSAGETYPE/MESSAGEORIGIN ที่เป็น
	 * identity ของ counter.
	 * ส่วน INFOBJ_COUNTER ยังไม่มี source ใน FidsAfttab จึงตั้งเป็นค่าว่าง ""
	 * ไปก่อน (รอ backend map).
	 * หมายเหตุ: CTYP เป็น enum ตั้งค่าว่างไม่ได้ จึงปล่อย null (tag จะไม่ถูก
	 * marshal ออกมา).
	 */
	public String convertCountertoEsb(String updateTime, FidsAfttab fidsAfttab) {
		StringWriter writer = new StringWriter();
		MSG esb = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();

		// INFOBJ_GENERIC — dynamic จาก FidsAfttab (identity counter = UFISCHKUD)
		MSG.MSGSTREAMOUT.INFOBJGENERIC generic = buildOutboundGeneric("UFISCHKUD", updateTime, fidsAfttab);

		// --- INFOBJ_COUNTER: ยังไม่มี source ใน FidsAfttab → ค่าว่าง "" (รอ backend
		// map) ---
		// CTYP เป็น enum ตั้งค่าว่างไม่ได้ → ใส่ placeholder CTYP.D ไปก่อน (รอ backend
		// map)
		MSG.MSGSTREAMOUT.INFOBJCOUNTER counter = new INFOBJCOUNTER();
		counter.setCKIC("");
		counter.setCTYP(CTYP.D);
		counter.setCKIT("");
		counter.setCKBS("");
		counter.setCKES("");
		counter.setDISP("");
		counter.setFLNU("");
		counter.setURNO("");

		msgstreamout.setINFOBJGENERIC(generic);
		msgstreamout.setINFOBJCOUNTER(counter);
		esb.setMSGSTREAMOUT(msgstreamout);
		try {
			Marshaller marshaller = OUT_MSG_CTX.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(esb, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("convertCountertoEsb: ", e);
		}
		return null;
	}

	/**
	 * ส่ง counter เข้าคิว UFIS_COUNTER_OUT_&lt;hopo&gt; โดย INFOBJ_GENERIC ดึงจาก
	 * FidsAfttab
	 * (INFOBJ_COUNTER ยังว่างรอ backend map). เรียกจากภายนอกได้ (controller/test) —
	 * ยังไม่ได้ผูกเข้า flow convertXMLtoObject อัตโนมัติ.
	 */
	public void sendEmptyCounter(String updateTime, FidsAfttab fidsAfttab) {
		String xmlEsb = convertCountertoEsb(updateTime, fidsAfttab);
		if (xmlEsb != null) {
			sendToOutboundQueue("UFIS_COUNTER_OUT", fidsAfttab.getHopo(), xmlEsb);
		}
	}

	/**
	 * ปั้น XML สำหรับคิว UFIS_GATE_OUT (UFISGTDUD — gate update).
	 * INFOBJ_GENERIC ดึงจาก FidsAfttab (ดู {@link #buildOutboundGeneric}).
	 * INFOBJ_GATE เลือก GATEARR/GATEDEP ตาม ADID — field ค่าว่าง "" ไปก่อน (รอ
	 * backend map).
	 */
	public String convertGatetoEsb(String updateTime, FidsAfttab fidsAfttab) {
		StringWriter writer = new StringWriter();
		MSG esb = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();

		MSG.MSGSTREAMOUT.INFOBJGENERIC generic = buildOutboundGeneric("UFISGTDUD", updateTime, fidsAfttab);

		// --- INFOBJ_GATE: เลือก arr/dep ตาม ADID, field ค่าว่าง "" (รอ backend map)
		// ---
		MSG.MSGSTREAMOUT.INFOBJGATE gate = new INFOBJGATE();
		if (generic.getADID() == ADID.A) {
			INFOBJGATE.GATEARR arr = new INFOBJGATE.GATEARR();
			arr.setGTA1("");
			arr.setGA1B("");
			arr.setGA1E("");
			gate.setGATEARR(arr);
		} else {
			INFOBJGATE.GATEDEP dep = new INFOBJGATE.GATEDEP();
			dep.setGTD1("");
			dep.setGD1B("");
			dep.setGD1E("");
			gate.setGATEDEP(dep);
		}

		msgstreamout.setINFOBJGENERIC(generic);
		msgstreamout.setINFOBJGATE(gate);
		esb.setMSGSTREAMOUT(msgstreamout);
		try {
			Marshaller marshaller = OUT_MSG_CTX.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(esb, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("convertGatetoEsb: ", e);
		}
		return null;
	}

	/**
	 * ส่ง gate เข้าคิว UFIS_GATE_OUT_&lt;hopo&gt; (INFOBJ_GATE ยังว่างรอ backend
	 * map).
	 * เรียกจากภายนอกได้ — ยังไม่ได้ผูกเข้า flow convertXMLtoObject อัตโนมัติ.
	 */
	public void sendEmptyGate(String updateTime, FidsAfttab fidsAfttab) {
		String xmlEsb = convertGatetoEsb(updateTime, fidsAfttab);
		if (xmlEsb != null) {
			sendToOutboundQueue("UFIS_GATE_OUT", fidsAfttab.getHopo(), xmlEsb);
		}
	}

	/**
	 * ปั้น XML สำหรับคิว UFIS_BELT_OUT (UFISBLTUD — baggage belt update).
	 * INFOBJ_GENERIC ดึงจาก FidsAfttab (ดู {@link #buildOutboundGeneric}).
	 * INFOBJ_BELT ยังไม่มี source ใน FidsAfttab → field ค่าว่าง "" ไปก่อน (รอ
	 * backend map).
	 */
	public String convertBelttoEsb(String updateTime, FidsAfttab fidsAfttab) {
		StringWriter writer = new StringWriter();
		MSG esb = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();

		MSG.MSGSTREAMOUT.INFOBJGENERIC generic = buildOutboundGeneric("UFISBLTUD", updateTime, fidsAfttab);

		// --- INFOBJ_BELT: ยังไม่มี source ใน FidsAfttab → ค่าว่าง "" (รอ backend map)
		// ---
		MSG.MSGSTREAMOUT.INFOBJBELT belt = new INFOBJBELT();
		belt.setBLT1("");
		belt.setB1BS("");
		belt.setB1ES("");

		msgstreamout.setINFOBJGENERIC(generic);
		msgstreamout.setINFOBJBELT(belt);
		esb.setMSGSTREAMOUT(msgstreamout);
		try {
			Marshaller marshaller = OUT_MSG_CTX.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(esb, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("convertBelttoEsb: ", e);
		}
		return null;
	}

	/**
	 * ส่ง belt เข้าคิว UFIS_BELT_OUT_&lt;hopo&gt; (INFOBJ_BELT ยังว่างรอ backend
	 * map).
	 * เรียกจากภายนอกได้ — ยังไม่ได้ผูกเข้า flow convertXMLtoObject อัตโนมัติ.
	 */
	public void sendEmptyBelt(String updateTime, FidsAfttab fidsAfttab) {
		String xmlEsb = convertBelttoEsb(updateTime, fidsAfttab);
		if (xmlEsb != null) {
			sendToOutboundQueue("UFIS_BELT_OUT", fidsAfttab.getHopo(), xmlEsb);
		}
	}

	/**
	 * ปั้น XML สำหรับคิว UFIS_ACPOSITION_OUT (UFISPOSUD — aircraft position/stand
	 * update).
	 * INFOBJ_GENERIC ดึงจาก FidsAfttab (ดู {@link #buildOutboundGeneric}).
	 * INFOBJ_ACPOSITION เลือก ACPOSITIONARR/ACPOSITIONDEP ตาม ADID — field ค่าว่าง
	 * "" ไปก่อน (รอ backend map).
	 */
	public String convertAcpositiontoEsb(String updateTime, FidsAfttab fidsAfttab) {
		StringWriter writer = new StringWriter();
		MSG esb = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();

		MSG.MSGSTREAMOUT.INFOBJGENERIC generic = buildOutboundGeneric("UFISPOSUD", updateTime, fidsAfttab);

		// --- INFOBJ_ACPOSITION: เลือก arr/dep ตาม ADID, field ค่าว่าง "" (รอ backend
		// map) ---
		MSG.MSGSTREAMOUT.INFOBJACPOSITION pos = new INFOBJACPOSITION();
		if (generic.getADID() == ADID.A) {
			INFOBJACPOSITION.ACPOSITIONARR arr = new INFOBJACPOSITION.ACPOSITIONARR();
			arr.setPSTA("");
			arr.setPABS("");
			arr.setPAES("");
			pos.setACPOSITIONARR(arr);
		} else {
			INFOBJACPOSITION.ACPOSITIONDEP dep = new INFOBJACPOSITION.ACPOSITIONDEP();
			dep.setPSTD("");
			dep.setPDBS("");
			dep.setPDES("");
			pos.setACPOSITIONDEP(dep);
		}

		msgstreamout.setINFOBJGENERIC(generic);
		msgstreamout.setINFOBJACPOSITION(pos);
		esb.setMSGSTREAMOUT(msgstreamout);
		try {
			Marshaller marshaller = OUT_MSG_CTX.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(esb, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("convertAcpositiontoEsb: ", e);
		}
		return null;
	}

	/**
	 * ส่ง acposition เข้าคิว UFIS_ACPOSITION_OUT_&lt;hopo&gt; (INFOBJ_ACPOSITION
	 * ยังว่างรอ backend map).
	 * เรียกจากภายนอกได้ — ยังไม่ได้ผูกเข้า flow convertXMLtoObject อัตโนมัติ.
	 */
	public void sendEmptyAcposition(String updateTime, FidsAfttab fidsAfttab) {
		String xmlEsb = convertAcpositiontoEsb(updateTime, fidsAfttab);
		if (xmlEsb != null) {
			sendToOutboundQueue("UFIS_ACPOSITION_OUT", fidsAfttab.getHopo(), xmlEsb);
		}
	}

	/**
	 * ปั้น XML สำหรับคิว UFIS_OTHERS_OUT — towing (UFISTOWUD). ต่างจากคิวอื่นตรงที่
	 * payload
	 * อยู่ใต้ {@code CONCAT > TOWINGS} (ไม่ใช่ INFOBJ_XXX). INFOBJ_GENERIC ดึงจาก
	 * FidsAfttab
	 * (ดู {@link #buildOutboundGeneric}); TOWINGS (TOID/TWTP/SCHE) ค่าว่าง ""
	 * ไปก่อน (รอ backend map).
	 */
	public String convertTowingtoEsb(String updateTime, FidsAfttab fidsAfttab) {
		StringWriter writer = new StringWriter();
		MSG esb = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();

		MSG.MSGSTREAMOUT.INFOBJGENERIC generic = buildOutboundGeneric("UFISTOWUD", updateTime, fidsAfttab);

		// --- CONCAT/TOWINGS: ยังไม่มี source ใน FidsAfttab → ค่าว่าง "" (รอ backend
		// map) ---
		MSG.MSGSTREAMOUT.CONCAT concat = new CONCAT();
		CONCAT.TOWINGS towings = new CONCAT.TOWINGS();
		towings.setTOID(fidsAfttab.getToid());
		towings.setTWTP("T");
		towings.setSCHE("");
		towings.setSCHS("");
		concat.setTOWINGS(towings);

		msgstreamout.setINFOBJGENERIC(generic);
		msgstreamout.setCONCAT(concat);
		esb.setMSGSTREAMOUT(msgstreamout);
		try {
			Marshaller marshaller = OUT_MSG_CTX.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(esb, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("convertTowingtoEsb: ", e);
		}
		return null;
	}

	/**
	 * ส่ง towing เข้าคิว UFIS_OTHERS_OUT_&lt;hopo&gt; (CONCAT/TOWINGS ยังว่างรอ
	 * backend map).
	 * เรียกจากภายนอกได้ — ยังไม่ได้ผูกเข้า flow convertXMLtoObject อัตโนมัติ.
	 */
	public void sendEmptyTowing(String updateTime, FidsAfttab fidsAfttab) {
		String xmlEsb = convertTowingtoEsb(updateTime, fidsAfttab);
		if (xmlEsb != null) {
			sendToOutboundQueue("UFIS_OTHERS_OUT", fidsAfttab.getHopo(), xmlEsb);
		}
	}

	/**
	 * ปั้น XML สำหรับคิว UFIS_OTHERS_OUT — SITA bulk file (UFISSITA).
	 * ต่างจากคิวอื่นตรงที่ SITA
	 * เป็นไฟล์ ไม่ผูกเที่ยวบิน → INFOBJ_GENERIC มีแค่ header ขั้นต่ำ (ไม่ใช้
	 * {@link #buildOutboundGeneric})
	 * และ payload เป็น {@code BULKDATA > SITA} (FILE_NAME + CONTENT
	 * ที่ส่งค่าจริงเข้ามา).
	 * หมายเหตุ: ACTIONTYPE = I (SITA เป็นไฟล์ใหม่) ตาม sample.
	 */
	public String convertSitatoEsb(String updateTime, String hopo, String fileName, String content) {
		StringWriter writer = new StringWriter();
		MSG esb = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();

		// INFOBJ_GENERIC — header ขั้นต่ำ (SITA ไม่ผูกเที่ยวบิน จึงไม่มี
		// URNO/ADID/FLNO/...)
		MSG.MSGSTREAMOUT.INFOBJGENERIC generic = new INFOBJGENERIC();
		generic.setMESSAGETYPE("UFISSITA");
		generic.setMESSAGEORIGIN("AOS");
		generic.setTIMEID(TIMEID.UTC);
		generic.setTIMESTAMP(updateTime);
		generic.setACTIONTYPE(ACTIONTYPE.I);
		generic.setHOPO(hopo);

		// BULKDATA/SITA — payload จริง (ไฟล์)
		MSG.MSGSTREAMOUT.BULKDATA bulkdata = new BULKDATA();
		BULKDATA.SITA sita = new BULKDATA.SITA();
		sita.setFILENAME(fileName);
		sita.setCONTENT(content);
		bulkdata.setSITA(sita);

		msgstreamout.setINFOBJGENERIC(generic);
		msgstreamout.setBULKDATA(bulkdata);
		esb.setMSGSTREAMOUT(msgstreamout);
		try {
			Marshaller marshaller = OUT_MSG_CTX.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(esb, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("convertSitatoEsb: ", e);
		}
		return null;
	}

	/**
	 * ส่ง SITA bulk file เข้าคิว UFIS_OTHERS_OUT_&lt;hopo&gt; (ใช้คิวร่วมกับ
	 * towing).
	 */
	public void sendSita(String updateTime, String hopo, String fileName, String content) {
		String xmlEsb = convertSitatoEsb(updateTime, hopo, fileName, content);
		if (xmlEsb != null) {
			sendToOutboundQueue("UFIS_OTHERS_OUT", hopo, xmlEsb);
		}
	}

	/**
	 * ปั้น XML สำหรับคิว UFIS_TRIGGER_OUT — VDGS (UFISVDGUD).
	 * INFOBJ_GENERIC ดึงจาก FidsAfttab (ดู {@link #buildOutboundGeneric}) —
	 * ACTIONTYPE เป็น
	 * U/I ปกติเหมือนคิวอื่น. INFOBJ_VDGS เลือก VDGSARR/VDGSDEP ตาม ADID — field
	 * ค่าว่าง "" ไปก่อน (รอ backend map).
	 */
	public String convertVdgstoEsb(String updateTime, FidsAfttab fidsAfttab) {
		StringWriter writer = new StringWriter();
		MSG esb = new MSG();
		MSG.MSGSTREAMOUT msgstreamout = new MSGSTREAMOUT();

		MSG.MSGSTREAMOUT.INFOBJGENERIC generic = buildOutboundGeneric("UFISVDGUD", updateTime, fidsAfttab);

		// --- INFOBJ_VDGS: เลือก arr/dep ตาม ADID, field ค่าว่าง "" (รอ backend map)
		// ---
		MSG.MSGSTREAMOUT.INFOBJVDGS vdgs = new INFOBJVDGS();
		if (generic.getADID() == ADID.A) {
			INFOBJVDGS.VDGSARR arr = new INFOBJVDGS.VDGSARR();
			arr.setPSTA("");
			arr.setACT5("");
			arr.setFTYP("");
			vdgs.setVDGSARR(arr);
		} else {
			INFOBJVDGS.VDGSDEP dep = new INFOBJVDGS.VDGSDEP();
			dep.setPSTD("");
			dep.setACT5("");
			dep.setFTYP("");
			dep.setTIFD("");
			vdgs.setVDGSDEP(dep);
		}

		msgstreamout.setINFOBJGENERIC(generic);
		msgstreamout.setINFOBJVDGS(vdgs);
		esb.setMSGSTREAMOUT(msgstreamout);
		try {
			Marshaller marshaller = OUT_MSG_CTX.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(esb, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("convertVdgstoEsb: ", e);
		}
		return null;
	}

	/**
	 * ส่ง VDGS เข้าคิว UFIS_TRIGGER_OUT_&lt;hopo&gt; (INFOBJ_VDGS ยังว่างรอ backend
	 * map).
	 * เรียกจากภายนอกได้ — ยังไม่ได้ผูกเข้า flow convertXMLtoObject อัตโนมัติ.
	 */
	public void sendEmptyVdgs(String updateTime, FidsAfttab fidsAfttab) {
		String xmlEsb = convertVdgstoEsb(updateTime, fidsAfttab);
		if (xmlEsb != null) {
			sendToOutboundQueue("UFIS_TRIGGER_OUT", fidsAfttab.getHopo(), xmlEsb);
		}
	}

	/**
	 * ส่งต่อ File Ready เข้าคิว UFIS_TRIGGER_OUT_&lt;hopo&gt; แบบ passthrough — รับ
	 * XML ที่ปั้นมา
	 * เรียบร้อยแล้วจากต้นทาง แล้วลงคิวต่อเลย ไม่ parse/ไม่ build/ไม่เก็บ backend
	 * (iFIMSConnect เป็น
	 * แค่ตัวกลางรับ-ส่งต่อ). ใช้คิวร่วมกับ VDGS.
	 *
	 * @param hopo   สำหรับ routing/ชื่อคิวเท่านั้น (payload ไม่มี hopo)
	 * @param xmlEsb XML File Ready ที่พร้อมส่ง (ปั้นมาจากต้นทางแล้ว)
	 */
	public void sendFileReady(String hopo, String xmlEsb) {
		if (xmlEsb != null) {
			sendToOutboundQueue("UFIS_TRIGGER_OUT", hopo, xmlEsb);
		}
	}

	/**
	 * สร้าง INFOBJ_GENERIC (ส่วนหัว) สำหรับ outbound message ทุกคิว โดยดึงค่าจาก
	 * {@link FidsAfttab}
	 * แบบเดียวกับ {@link #convertFidsAfftabtoEsb} — ต่างกันแค่ MESSAGETYPE ที่เป็น
	 * identity ของ
	 * แต่ละ message. MESSAGEORIGIN ใช้ "AOS" เหมือนกันทุกคิว.
	 */
	private MSG.MSGSTREAMOUT.INFOBJGENERIC buildOutboundGeneric(String messageType, String updateTime,
			FidsAfttab fidsAfttab) {
		MSG.MSGSTREAMOUT.INFOBJGENERIC generic = new INFOBJGENERIC();
		generic.setMESSAGETYPE(messageType);
		generic.setMESSAGEORIGIN("AOS");
		generic.setTIMEID(TIMEID.UTC);
		generic.setTIMESTAMP(updateTime);
		generic.setACTIONTYPE(fidsAfttab.getAction().equalsIgnoreCase("insert") ? ACTIONTYPE.I : ACTIONTYPE.U);
		generic.setHOPO(fidsAfttab.getHopo());
		generic.setURNO(fidsAfttab.getUrno() != null ? fidsAfttab.getUrno().toString() : null);
		generic.setADID(ADID.valueOf(fidsAfttab.getAdid()));
		generic.setSTDT(generic.getADID() == ADID.A ? fidsAfttab.getStoa() : fidsAfttab.getStod());
		generic.setFLNO(fidsAfttab.getFlno() != null ? fidsAfttab.getFlno().trim() : null);
		generic.setCSGN(fidsAfttab.getCsgn());
		generic.setRKEY(fidsAfttab.getRkey() != null ? fidsAfttab.getRkey().toString() : null);
		generic.setRTYP(fidsAfttab.getRtyp());
		return generic;
	}

	/**
	 * ส่ง XML ออก ESB สำหรับคิว outbound ใดๆ (flight/counter/gate/...) —
	 * เลือกช่องทางเดียวตาม
	 * flag webSphereEnabled:
	 * <ul>
	 * <li>true = WebSphere MQ, route ตาม hopo (BKK→machine1, อื่น→machine2);
	 * producer เก็บ log payload ลง outbound-&lt;queue&gt;.log ให้เอง</li>
	 * <li>false = webservice ({@link #callWebserviceUpdate}) + เก็บ log payload ลง
	 * outbound-WS</li>
	 * </ul>
	 *
	 * <p>
	 * ⚠️ caveat: callWebserviceUpdate ยิงไป endpoint ของ flight
	 * (AODB_FlightOutbound) —
	 * ถ้า webSphereEnabled=false ตอนส่ง counter/gate จะไป endpoint นี้ด้วย
	 * (ไม่ตรงชนิด message).
	 * ใน prod ที่ใช้ MQ (flag=true) ไม่มีปัญหา; ถ้าจะใช้ WS จริงกับคิวอื่นต้องเพิ่ม
	 * endpoint แยก.
	 *
	 * @param queueBase ชื่อคิวฐาน (ไม่ต้องมี suffix hopo) เช่น "UFIS_GATE_OUT" —
	 *                  method จะต่อ
	 *                  "_&lt;HOPO&gt;" ให้เอง เป็น "UFIS_GATE_OUT_BKK"
	 */
	private void sendToOutboundQueue(String queueBase, String hopo, String xmlEsb) {
		String queueName = queueBase + "_" + hopo;
		if (webSphereEnabled) {
			if (hopo.equalsIgnoreCase("BKK")) {
				webSphereProducer.sendToMachine1(queueName, hopo, xmlEsb);
			} else {
				webSphereProducer.sendToMachine2(queueName, hopo, xmlEsb);
			}
		} else {
			// webSphereEnabled=false → ส่งผ่าน webservice แทน (เก็บ payload ลง outbound
			// ด้วย)
			MDC.put("sendEsbKey", hopo + "/outbound-WS");
			try {
				sendEsbLog.info(xmlEsb);
			} finally {
				MDC.remove("sendEsbKey");
			}
			callWebserviceUpdate(xmlEsb);
		}
	}

	/* public void callWebserviceResponse(String xmlEsb) {
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
	} */

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

	private void sendEmailResponse(String xmlEsb, String hopo) {
		String host = "192.168.10.11";
		String port = "1025";
		String toEmail = "pocwm1@esbv10.co.th";
		String fromEmail = "ifimsconnect@sfis.co.th";

		Properties properties = new Properties();
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", port);
		properties.put("mail.smtp.auth", "false"); // หาก SMTP 1025 ไม่ต้องล็อกอิน
		properties.put("mail.smtp.starttls.enable", "false"); // สำหรับพอร์ต 1025 (MailHog/Dev Server) มักจะไม่ใช้ TLS

		Session session = Session.getInstance(properties);

		try {
			MimeMessage message = new MimeMessage(session);
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
			message.setSubject("NACK - Error during process "+hopo+" AODB inbound message");
			message.setFrom(new InternetAddress(fromEmail, "iFIMSConnect", "UTF-8"));

			// ส่งเนื้อหา XML เข้าไปใน Body ของอีเมล
			message.setText(xmlEsb, "UTF-8", "xml");

			Transport.send(message);
			log.info("Successfully sent NACK email to: {}", toEmail);
		} catch (MessagingException e) {
			log.error("Failed to send NACK email", e);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
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