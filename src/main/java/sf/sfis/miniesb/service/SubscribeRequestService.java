package sf.sfis.miniesb.service;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sf.sfis.miniesb.MQArtemisProducer;
import sf.sfis.miniesb.aodb.Body;
import sf.sfis.miniesb.aodb.Control;
import sf.sfis.miniesb.aodb.Envelope;
import sf.sfis.miniesb.aodb.Header;
import sf.sfis.miniesb.aodb.Request;

@Slf4j
@WebService
@Service
@RequiredArgsConstructor
public class SubscribeRequestService {
	private final MQArtemisProducer artemisProducer;

	// Cached JAXBContext — thread-safe, built once instead of per request.
	private static final JAXBContext ENVELOPE_CTX = newContext();

	private static JAXBContext newContext() {
		try {
			return JAXBContext.newInstance(Envelope.class);
		} catch (JAXBException e) {
			throw new IllegalStateException("Failed to init JAXBContext for Envelope", e);
		}
	}

	@WebMethod
	public void subscribe(@WebParam(name = "startTime") String starttime, @WebParam(name = "endTime") String endtime, @WebParam(name = "dataType") String dataType) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	        LocalDateTime today = LocalDateTime.now();
	        String currentDate = today.format(formatter);
	        		
			Header header = new Header();
			Control control = new Control();
			control.setMessageId("localhost:2d970033:195f1193a55:-12d5");
			control.setMessageVersion("1.4");
			control.setMessageType("SUBSCRIBE");
			control.setSender("FIDS");
			control.setTimestamp(currentDate);
			Request request = new Request();
			request.setDatatype(dataType);//"pl_turn","pl_desk"
//	        request.setStartTime("2025-05-09T00:00:00");
//	        request.setEndTime("2025-05-09T23:59:59");
			request.setStartTime(starttime);
			request.setEndTime(endtime);
			control.setRequest(request);
			header.setControl(control);
			Body body = new Body();

			Envelope envelope = new Envelope();
			envelope.setHeader(header);
			envelope.setBody(body);

			// Marshal to XML String
			StringWriter writer = new StringWriter();
			JAXBContext context = ENVELOPE_CTX;
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(envelope, writer);

			log.info("Subscribe "+dataType+" from "+starttime+" to "+endtime);
			//log.info(writer.toString());
			artemisProducer.sendMessage("AQ_FROM_FIDS_AOT_AOS_TST", "BKK", writer.toString());
			
		} catch (JAXBException e) {
        	log.error("subscribe: ", e);
		}
	}
	
	@WebMethod
	public void requestDataset(@WebParam(name = "startTime") String starttime, @WebParam(name = "endTime") String endtime, @WebParam(name = "dataType") String dataType) {
		//log.info("Request Dataset...");
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	        LocalDateTime today = LocalDateTime.now();
	        String currentDate = today.format(formatter);
	        
			Header header = new Header();
			Control control = new Control();
			control.setMessageId("localhost:2d970033:195f1193a55:-12d5");
			control.setMessageVersion("1.4");
			control.setMessageType("DATASET");
			control.setSender("FIDS");
			control.setTimestamp(currentDate);
			Request request = new Request();
			request.setDatatype(dataType);//"pl_turn","pl_desk"
			request.setKeepSubscription("y");
			request.setStartTime(starttime);
			request.setEndTime(endtime);
			control.setRequest(request);
			header.setControl(control);
			Body body = new Body();

			Envelope envelope = new Envelope();
			envelope.setHeader(header);
			envelope.setBody(body);

			// Marshal to XML String
			StringWriter writer = new StringWriter();
			JAXBContext context = ENVELOPE_CTX;
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(envelope, writer);

			log.info("Request "+dataType+" from "+starttime+" to "+endtime);
			//log.info(writer.toString());
			artemisProducer.sendMessage("AQ_FROM_FIDS_AOT_AOS_TST", "BKK", writer.toString());
		} catch (JAXBException e) {
        	log.error("requestDataset: ", e);
//			e.printStackTrace();
		}
	}

	//For test only
//	public static void main(String[] args) {
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
////			artemisProducer.sendMessage("AQ_FROM_FIDS_AOT_AOS_TST", xmlPayload);
//
//		} catch (JAXBException e) {
//			e.printStackTrace();
//		}
//	}
}