package sf.sfis.ifimsconnect.service;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.stream.StreamSource;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.Xslt30Transformer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sf.sfis.ifimsconnect.MQArtemisProducer;
import sf.sfis.ifimsconnect.esb.figurein.FigureMessageIn;
import sf.sfis.ifimsconnect.esb.realtimeinbound.MSG;
import sf.sfis.ifimsconnect.esb.realtimeinbound.MSG.MSGSTREAMIN.INFOBJGENERIC;
import sf.sfis.ifimsconnect.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJEQUIPMENT;
import sf.sfis.ifimsconnect.esb.realtimeinbound.MSG.MSGSTREAMIN.MSGOBJECTS.INFOBJMANIFEST;

@Slf4j
@WebService
@Service
@RequiredArgsConstructor
public class ESBRequestService {
	private final MQArtemisProducer artemisProducer;

	// Dedicated logger for inbound XML from ESB via webservice → logs/receive_esb/<hopo>/<queue>.log
	// (ใช้ logger ชื่อเดียวกับฝั่ง WebSphere MQ consumer จึงไปรวมในโฟลเดอร์ receive_esb เดียวกัน)
	private static final Logger receivedEsbLog = LoggerFactory.getLogger("RECEIVED_ESB_XML");
	private static final Pattern HOPO_PATTERN = Pattern.compile("<HOPO>\\s*([^<]*?)\\s*</HOPO>");

	private static String extractHopo(String xml) {
		Matcher m = HOPO_PATTERN.matcher(xml);
		return (m.find() && !m.group(1).isEmpty()) ? m.group(1) : "unknown";
	}

	// Cached JAXBContext — thread-safe, built once instead of per message (ใช้อ่าน generic + terminal handlers).
	private static final JAXBContext MSG_CTX = newContext(MSG.class);

	private static JAXBContext newContext(Class<?> clazz) {
		try {
			return JAXBContext.newInstance(clazz);
		} catch (JAXBException e) {
			throw new IllegalStateException("Failed to init JAXBContext for " + clazz.getName(), e);
		}
	}

	// ─── Saxon XSLT: แปลง MSG → AODB Envelope (aos_inbound.xsl) แทน build+marshal JAXB ───
	private static final Processor SAXON = new Processor(false);
	private static final String XSL_RESOURCE = "/aos_inbound.xsl";
	private static volatile XsltExecutable xsltExecutable;

	/** compile stylesheet ครั้งเดียว (lazy) แล้ว reuse — XsltExecutable เป็น immutable/thread-safe. */
	private static XsltExecutable xsltExecutable() throws Exception {
		XsltExecutable exec = xsltExecutable;
		if (exec == null) {
			synchronized (ESBRequestService.class) {
				exec = xsltExecutable;
				if (exec == null) {
					try (InputStream is = ESBRequestService.class.getResourceAsStream(XSL_RESOURCE)) {
						if (is == null) {
							throw new IllegalStateException("XSL resource not found on classpath: " + XSL_RESOURCE);
						}
						exec = SAXON.newXsltCompiler().compile(new StreamSource(is));
					}
					xsltExecutable = exec;
				}
			}
		}
		return exec;
	}

	/**
	 * แปลง inbound MSG XML → AODB Envelope XML ด้วย aos_inbound.xsl. ส่ง currentDate (วันปัจจุบัน
	 * 00:00) เป็น parameter แทน getCurrentDate() เดิม (field airb/land ใช้ตั้ง SOBT/SIBT).
	 * คืน null ถ้า transform ล้มเหลว.
	 */
	private String transformToAos(String xmlString) {
		try {
			StringWriter sw = new StringWriter();
			Serializer out = SAXON.newSerializer(sw);
			out.setOutputProperty(Serializer.Property.METHOD, "xml");
			out.setOutputProperty(Serializer.Property.INDENT, "yes");
			Xslt30Transformer trans = xsltExecutable().load30();
			trans.setStylesheetParameters(java.util.Map.of(new QName("currentDate"),
					new XdmAtomicValue(getCurrentDate())));
			trans.transform(new StreamSource(new StringReader(xmlString)), out);
			return sw.toString();
		} catch (Exception e) {
			log.error("transformToAos: ", e);
			return null;
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
		log.info("request AOS Inbound...");
		//log.info(xmlString);
		try {
			MSG msg = (MSG) MSG_CTX.createUnmarshaller().unmarshal(new StringReader(xmlString));
			INFOBJGENERIC infobjgeneric = msg.getMSGSTREAMIN().getINFOBJGENERIC();
			String systemType = infobjgeneric.getMESSAGEORIGIN();
			String hopo = infobjgeneric.getHOPO();

			// terminal handlers — รับเข้า object ของเราเอง (ไม่แปลงเป็น AODB / ไม่ส่งต่อคิว AOS)
			FigureMessageIn.InfobjFigure infobjfigure = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJFIGURE();
			if (infobjfigure != null) {
				handleFigureMessage(infobjfigure);
				return;
			}
			JAXBElement<INFOBJMANIFEST> manElement = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJMANIFEST();
			if (manElement != null && manElement.getValue() != null) {
				handleManifestMessage(manElement.getValue());
				return;
			}
			JAXBElement<INFOBJEQUIPMENT> eqpElement = msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJEQUIPMENT();
			if (eqpElement != null && eqpElement.getValue() != null) {
				handleEquipmentMessage(eqpElement.getValue());
				return;
			}

			// แปลง MSG → AODB Envelope ด้วย XSLT (aos_inbound.xsl) แทน build+marshal JAXB เดิม
			// (setFlight/setVdgs/setBhs/setBulkData) — ผลลัพธ์เหมือนเดิมแบบ semantic
			// (พิสูจน์ด้วย AosInboundXsltTest + ESBRequestServiceSnapshotTest)
			String out = transformToAos(xmlString);
			if (out == null) {
				return;
			}

			if (systemType.equals("AFTN")) {
				log.info("Send to AQ_FROM_AFTN_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_AFTN_AOT_AOS_TST", hopo, out);
			} else if (systemType.equals("SITA")) {
				log.info("Send to AQ_FROM_SITA_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_SITA_AOT_AOS_TST", hopo, out);
			} else {
				log.info("Send to AQ_FROM_FIDS_AOT_AOS_TST...");
				artemisProducer.sendMessage("AQ_FROM_FIDS_AOT_AOS_TST", hopo, out);
			}

		} catch (JAXBException e) {
			log.error("requestAodbInbound: ", e);
			// e.printStackTrace();
		}
	}

	/**
	 * รับข้อความ WMFIGURE (load figures) จาก UFIS_FIGURE_IN ที่ถูก unmarshal มากับ MSG แล้ว
	 * (element INFOBJ_FIGURE). ขั้นนี้ทำแค่ "รับค่า" — log ให้เห็นว่าอ่านได้ครบ. การ map เข้า
	 * object หลังบ้าน (ยังไม่มีตัวรองรับ) และการ persist/forward ค่อยต่อยอดที่จุด TODO ด้านล่าง.
	 */
	private void handleFigureMessage(FigureMessageIn.InfobjFigure infobjfigure) {
		FigureMessageIn.Figure figure = (infobjfigure != null) ? infobjfigure.getFigure() : null;
		if (figure == null) {
			log.warn("INFOBJ_FIGURE received but <figure> element is missing");
			return;
		}

		log.info("Received WMFIGURE: flight={} date={} reg={} ad={} intdom={} paxDisembark(dom/intl)={}/{} crew={}",
				figure.getFlightnumber(), figure.getFlightdate(), figure.getRegistration(),
				figure.getAdindicator(), figure.getIntdomindicator(),
				figure.getPaxdisembarkdom(), figure.getPaxdisembarkintl(), figure.getCrew());

		// TODO: map `figure` (+ figure.getRoot().getPort()) เข้า object หลังบ้านของเรา
		//       แล้ว persist/ส่งต่อ ตาม spec ที่จะกำหนดภายหลัง
	}

	/**
	 * รับข้อความ WMMANIFEST (passenger manifest) จาก UFIS_MANIFEST_IN ที่ถูก unmarshal
	 * มากับ MSG แล้ว (element INFOBJ_MANIFEST — มีอยู่ใน schema เดิม). ขั้นนี้ทำแค่ "รับค่า"
	 * — log ให้เห็นว่าอ่านได้. การ map เข้า object หลังบ้าน (ยังไม่มีตัวรองรับ) และการ
	 * persist/forward ค่อยต่อยอดที่จุด TODO ด้านล่าง. หมายเหตุ: MESSAGE เป็นข้อความยาว
	 * (manifest ทั้งใบ) จึง log แค่ความยาว ไม่ log เนื้อทั้งก้อนกันท่วม log.
	 */
	private void handleManifestMessage(INFOBJMANIFEST infobjmanifest) {
		INFOBJMANIFEST.Manifest manifest = (infobjmanifest != null) ? infobjmanifest.getManifest() : null;
		if (manifest == null) {
			log.warn("INFOBJ_MANIFEST received but <manifest> element is missing");
			return;
		}

		log.info("Received WMMANIFEST: flight={} date={} reg={} ad={} type={} messageLen={}",
				manifest.getFLIGHTNUMBER(), manifest.getFLIGHTDATE(), manifest.getREGISTRATION(),
				manifest.getADINDICATOR(), manifest.getTYPE(),
				(manifest.getMESSAGE() != null) ? manifest.getMESSAGE().length() : 0);

		// TODO: map `manifest` (โดยเฉพาะ manifest.getMESSAGE() ที่เป็น text ทั้งใบ) เข้า
		//       object หลังบ้านของเรา แล้ว persist/ส่งต่อ ตาม spec ที่จะกำหนดภายหลัง
	}

	/**
	 * รับข้อความ APSBLK (equipment/belt usage) จาก UFIS_EQUIPMENT_IN ที่ถูก unmarshal มากับ
	 * MSG แล้ว (element INFOBJ_EQUIPMENT — มีอยู่ใน schema เดิม). ขั้นนี้ทำแค่ "รับค่า" — log
	 * ค่าใน PLB/USAGE (BGRP/STATUS/DATETIME). การ map เข้า object หลังบ้าน (ยังไม่มีตัวรองรับ)
	 * และการ persist/forward ค่อยต่อยอดที่จุด TODO ด้านล่าง.
	 */
	private void handleEquipmentMessage(INFOBJEQUIPMENT infobjequipment) {
		INFOBJEQUIPMENT.PLB plb = (infobjequipment != null) ? infobjequipment.getPLB() : null;
		INFOBJEQUIPMENT.PLB.USAGE usage = (plb != null) ? plb.getUSAGE() : null;
		if (usage == null) {
			log.warn("INFOBJ_EQUIPMENT received but PLB/USAGE element is missing");
			return;
		}

		log.info("Received APSBLK equipment: bgrp={} status={} datetime={}",
				usage.getBGRP(), usage.getSTATUS(), usage.getDATETIME());

		// TODO: map `usage` (PLB/USAGE: BGRP/STATUS/DATETIME) เข้า object หลังบ้านของเรา
		//       แล้ว persist/ส่งต่อ ตาม spec ที่จะกำหนดภายหลัง
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