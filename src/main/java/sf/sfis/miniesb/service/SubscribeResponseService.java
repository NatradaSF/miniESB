package sf.sfis.miniesb.service;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.aodb.Envelope;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.utility.DateTimeFormatHelper;
import sf.sfis.miniesb.utility.TranformFidsAfttab;

@Service
@RequiredArgsConstructor
public class SubscribeResponseService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeResponseService.class);
	private final DateTimeFormatHelper dateTimeFormatHelper;
	private final TranformFidsAfttab tranformFidsAfttab;
	
	public void convertXMLtoObject(String xml) {
		try {
			LOGGER.info("SubscribeResponseService...");
			LOGGER.info(xml);
			JAXBContext jaxbContext = JAXBContext.newInstance(Envelope.class);
	        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
	        
	        Envelope envelope = (Envelope) unmarshaller.unmarshal(new StringReader(xml));
	        String type = envelope.getHeader().getControl().getMessageType();
			String timestamp = dateTimeFormatHelper.convertLocalToUTC(envelope.getHeader().getControl().getTimestamp());
			String hopo = envelope.getHeader().getControl().getStation();

			LOGGER.info("Message Type: " + type);
			StringWriter writer = new StringWriter();
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(envelope, writer);

//	        FidsAfttab fidsAfttab = convertPlTurntoAfttab(envelope);
	        FidsAfttab fidsAfttab = tranformFidsAfttab.convertPlTurntoAfftab(writer.toString(), "DATASET", "A");
	        
	    } catch (JAXBException e) {
        	LOGGER.error("convertXMLtoObject: ", e);
	    }
	}
	
//    private static FidsAfttab convertPlTurntoAfttab(Envelope envelope) {
//    	FidsAfttab fidsAfttab = null;
//        String hopo = envelope.getHeader().getControl().getStation();
//        String type = envelope.getHeader().getControl().getMessageType();
//        PlTurn plTurn = envelope.getBody().getPlTurn();
//        if(plTurn == null) {
//        	LOGGER.info("Data not found...");
//        	return fidsAfttab;
//        }
//        
//        fidsAfttab = new FidsAfttab();
//        String rtyp = plTurn.getId();
////        LOGGER.info("HOPO: "+hopo);
////        LOGGER.info("RTYP: "+rtyp);
//        
////        Arrival Datas
////        LOGGER.info("== Arrival Data ==");
//        List<PlArrival> lstPlArrival = JAXBHelper.extractElementsByLocalPart(plTurn.getPtPaArrival().getValue(), "pl_arrival", PlArrival.class);
//        for(PlArrival plArrival : lstPlArrival) {
//        	String urno = GetterAccess.get(plArrival, p -> p.getPaIdseq(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//        	if(urno.equals("")) {
//        		break;
//        	}
//            
//            if(type.equals("DATASET")) {
//            	
//            }else if(type.equals("UPDATE")){
//            	LOGGER.info("UPDATE URNO: " + urno);
//            }
//            
//        	String rkey = GetterAccess.get(plTurn, p -> p.getPtPaArrival(), v1 -> v1.getValue(), v2 -> v2.getContent().get(0), v -> v.toString().trim()).orElse("");
//	        String flno = GetterAccess.get(plArrival, p -> p.getPaFlightnumber(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String jfno = GetterAccess.get(plArrival, p -> p.getPaCodeshareflightnumbers(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String ttyp = GetterAccess.get(plArrival, p -> p.getPaRncNaturecode(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
////	        String org3or4 = GetterAccess.get(plArrival, p -> p.getPaRapPreviousairport(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        RefAirline airline = JAXBHelper.extractElementByLocalPart(plArrival.getPaRalAirline().getValue(), "ref_airline", RefAirline.class);
//	        String alc2 = GetterAccess.get(airline, p -> p.getRal2Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String alc3 = GetterAccess.get(airline, p -> p.getRal3Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String remp = GetterAccess.get(plArrival, p -> p.getPaPubliccomment(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String ftyp = GetterAccess.get(plArrival, p -> p.getPaRfstFlightstatus(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String csgn = GetterAccess.get(plArrival, p -> p.getPaCallsign(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String sibt = convertDate(GetterAccess.get(plArrival, p -> p.getPaSibt(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			String aldt = convertDate(GetterAccess.get(plArrival, p -> p.getPaAldt(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			String aibt = convertDate(GetterAccess.get(plArrival, p -> p.getPaAibt(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			String eibt = convertDate(GetterAccess.get(plArrival, p -> p.getPaEibt(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			RefAircrafttype refAircraftType = JAXBHelper.extractElementByLocalPart(plArrival.getPaRactAircrafttype().getValue(), "ref_aircrafttype", RefAircrafttype.class);
//			String act3 = GetterAccess.get(refAircraftType, p -> p.getRactIatatype(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String act5 = GetterAccess.get(refAircraftType, p -> p.getRactIcaotype(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        PaRapRefpreviousairport.RefAirport previousairport = JAXBHelper.extractElementByLocalPart(plArrival.getPaRapRefpreviousairport().getValue(), "ref_airport", PaRapRefpreviousairport.RefAirport.class);
//	        PaRapReforiginairport.RefAirport originairport = JAXBHelper.extractElementByLocalPart(plArrival.getPaRapReforiginairport().getValue(), "ref_airport", PaRapReforiginairport.RefAirport.class);
//	        String previousRapIata3Lc = GetterAccess.get(previousairport, p -> p.getRapIata3Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String originRapIata3Lc = GetterAccess.get(originairport, p -> p.getRapIata3Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String via3 = !previousRapIata3Lc.equals("") ? previousRapIata3Lc : originRapIata3Lc;
//	        String previousRapIcao4lc = GetterAccess.get(previousairport, p -> p.getRapIcao4Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String originRapIcao4lc = GetterAccess.get(originairport, p -> p.getRapIcao4Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String via4 = !previousRapIcao4lc.equals("") ? previousRapIcao4lc : originRapIcao4lc;
//	        RefAircraft refAircraft = JAXBHelper.extractElementByLocalPart(plArrival.getPaRacAircraft().getValue(), "ref_aircraft", RefAircraft.class);
//			String racRegistration = GetterAccess.get(refAircraft, p -> p.getRacRegistration(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String paRegistration = GetterAccess.get(plArrival, p -> p.getPaRegistration(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String regn = !paRegistration.equals("") ? paRegistration : racRegistration;
//			RefCountrytype refCountrytype = JAXBHelper.extractElementByLocalPart(plArrival.getPaRcttCountrytype().getValue(), "ref_countrytype", RefCountrytype.class);
//			String flti = GetterAccess.get(refCountrytype, p -> p.getRcttCode(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			RefServicetypecode refServicetypecode = JAXBHelper.extractElementByLocalPart(plArrival.getPaRstcRefservicetypecode().getValue(), "ref_servicetypecode", RefServicetypecode.class);
//			String sytp = GetterAccess.get(refServicetypecode, p -> p.getRstcRistcIatacode(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String psta = GetterAccess.get(plArrival, p -> p.getPaRstaStand(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//
////			String gta1or2 = GetterAccess.get(plArrival, p -> p.getPaArrivalgates(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("")
//			List<PlArrivalgate> lstPlArrivalgate = plArrival.getPlArrivalgateList().getPlArrivalgate();
//			String gta1 = "";
//			String ga1b = "";
//			String ga1e = "";
//			String gta2 = "";
//			String ga2b = "";
//			String ga2e = "";
//			if(lstPlArrivalgate.size() > 0) {
//				gta1 = GetterAccess.get(lstPlArrivalgate.get(0), p -> p.getPagRgtGate(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				ga1b = convertDate(GetterAccess.get(lstPlArrivalgate.get(0), p -> p.getPagBeginplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				ga1e = convertDate(GetterAccess.get(lstPlArrivalgate.get(0), p -> p.getPagEndplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			}
//			if(lstPlArrivalgate.size() > 1) {
//				gta2 = GetterAccess.get(lstPlArrivalgate.get(1), p -> p.getPagRgtGate(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				ga2b = convertDate(GetterAccess.get(lstPlArrivalgate.get(1), p -> p.getPagBeginplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				ga2e = convertDate(GetterAccess.get(lstPlArrivalgate.get(1), p -> p.getPagEndplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			}
//			
////			String blt1or2 = GetterAccess.get(plArrival, p -> p.getPaBaggagebelts(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			List<PlBaggagebelt> lstPlBaggagebelt = plArrival.getPlBaggagebeltList().getPlBaggagebelt();
//			String blt1 = "";
//			String b1ba = "";
//			String b1ea = "";
//			String b1bs = "";
//			String b1es = "";
//			String blt2 = "";
//			String b2ba = "";
//			String b2ea = "";
//			String b2bs = "";
//			String b2es = "";
//			if(lstPlBaggagebelt.size() > 0) {
//				blt1 = GetterAccess.get(lstPlBaggagebelt.get(0), p -> p.getPbbRbbBaggagebelt(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				b1ba = convertDate(GetterAccess.get(lstPlBaggagebelt.get(0), p -> p.getPbbBeginactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b1ea = convertDate(GetterAccess.get(lstPlBaggagebelt.get(0), p -> p.getPbbEndactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b1bs = convertDate(GetterAccess.get(lstPlBaggagebelt.get(0), p -> p.getPbbBeginplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b1es = convertDate(GetterAccess.get(lstPlBaggagebelt.get(0), p -> p.getPbbEndplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			}
//			if(lstPlBaggagebelt.size() > 1) {
//				blt2 = GetterAccess.get(lstPlBaggagebelt.get(1), p -> p.getPbbRbbBaggagebelt(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				b2ba = convertDate(GetterAccess.get(lstPlBaggagebelt.get(1), p -> p.getPbbBeginactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b2ea = convertDate(GetterAccess.get(lstPlBaggagebelt.get(1), p -> p.getPbbEndactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b2bs = convertDate(GetterAccess.get(lstPlBaggagebelt.get(1), p -> p.getPbbBeginplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b2es = convertDate(GetterAccess.get(lstPlBaggagebelt.get(1), p -> p.getPbbEndplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			}
//			
//	        fidsAfttab.setHopo(hopo);
//	        fidsAfttab.setRtyp(rtyp);
//	        fidsAfttab.setUrno(new BigDecimal(urno));
//	        fidsAfttab.setRkey(new BigDecimal(rkey));
//	        fidsAfttab.setFlno(flno);
//	        fidsAfttab.setJfno(jfno);
//	        fidsAfttab.setTtyp(ttyp);
//	        fidsAfttab.setOrg3(via3);
//	        fidsAfttab.setOrg4(via4);
//
//	        LOGGER.info("URNO: " + urno);
////	        LOGGER.info("RKEY: " + rkey);
////	        LOGGER.info("FLNO: " + flno);
////	        LOGGER.info("JFNO: " + jfno);
////	        LOGGER.info("TTYP: " + ttyp);
////	        LOGGER.info("ADID: A");
////			LOGGER.info("ALC2: " + alc2);
////			LOGGER.info("ALC3: " + alc3);
////			LOGGER.info("ACT3: " + act3);
////			LOGGER.info("ACT5: " + act5);
////			LOGGER.info("REMP: " + remp);
////			LOGGER.info("FTYP: " + ftyp);
////			LOGGER.info("CSGN: " + csgn);
////			LOGGER.info("ORG3,VIA3: " + via3);
////			LOGGER.info("ORG4,VIA4: " + via4);
////			LOGGER.info("REGN: " + regn);
//			LOGGER.info("STOA,SIBT: " + sibt + "("+plArrival.getPaSibt().getValue().getValue()+")");
////			LOGGER.info("LAND,ALDT: " + aldt + "("+plArrival.getPaAldt().getValue().getValue()+")");
////			LOGGER.info("ONBL,AIBT: " + aibt + "("+plArrival.getPaAibt().getValue().getValue()+")");
////			LOGGER.info("EIBT: "+ eibt);
////			LOGGER.info("FLTI: " + flti);
////			LOGGER.info("SYTP: " + sytp);
////			LOGGER.info("PSTA: " + psta);
////			LOGGER.info("GTA1: " + gta1);
////			LOGGER.info("GA1B: " + ga1b);
////			LOGGER.info("GA1E: " + ga1e);
////			LOGGER.info("GTA2: " + gta2);
////			LOGGER.info("GA2B: " + ga2b);
////			LOGGER.info("GA2E: " + ga2e);
////			LOGGER.info("BLT1: " + blt1);
////			LOGGER.info("B1BA: " + b1ba);
////			LOGGER.info("B1EA: " + b1ea);
////			LOGGER.info("B1BS: " + b1bs);
////			LOGGER.info("B1ES: " + b1es);
////			LOGGER.info("BLT2: " + blt2);
////			LOGGER.info("B2BA: " + b2ba);
////			LOGGER.info("B2EA: " + b2ea);
////			LOGGER.info("B2BS: " + b2bs);
////			LOGGER.info("B2ES: " + b2es);
//			
//			String vial = "";
//	        for (PlArrival.PlRoutingList.PlRouting plRouting : plArrival.getPlRoutingList().getPlRouting()) {
//	        	PrtRapRefairport.RefAirport refAirport = JAXBHelper.extractElementByLocalPart(plRouting.getPrtRapRefairport().getValue(), "ref_airport", RefAirport.class);
//	        	via3 = GetterAccess.get(refAirport, p -> p.getRapIata3Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        	via4 = GetterAccess.get(refAirport, p -> p.getRapIcao4Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        	vial = vial+getVial(via3,via4);
//	        }
//	        int vian = plArrival.getPlRoutingList().getPlRouting().size();
////        	LOGGER.info("VIAL: " + vial);
////        	LOGGER.info("VIAN: " + vian);
//	        
//        }
//		
////        Departure Datas
////        LOGGER.info("== Departure Data ==");
//        List<PlDeparture> lstPlDeparture = JAXBHelper.extractElementsByLocalPart(plTurn.getPtPdDeparture().getValue(), "pl_departure", PlDeparture.class);
//        for(PlDeparture plDeparture : lstPlDeparture) {
//        	String urno = GetterAccess.get(plDeparture, p -> p.getPdIdseq(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//        	if(urno.equals("")) {
//        		break;
//        	}
//            
//            if(type.equals("DATASET")) {
//            	
//            }else if(type.equals("UPDATE")){
//            	LOGGER.info("UPDATE URNO: " + urno);
//            }
//            
//			String rkey = GetterAccess.get(plTurn, p -> p.getPtPdDeparture(), v1 -> v1.getValue(), v2 -> v2.getContent().get(0), v -> v.toString().trim()).orElse("");
//	        String flno = GetterAccess.get(plDeparture, p -> p.getPdFlightnumber(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String jfno = GetterAccess.get(plDeparture, p -> p.getPdCodeshareflightnumbers(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
////	        String des3or4 = GetterAccess.get(plDeparture, p -> p.getPdRapDestinationairport(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String ttyp = GetterAccess.get(plDeparture, p -> p.getPdRncNaturecode(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        RefAirline airline = JAXBHelper.extractElementByLocalPart(plDeparture.getPdRalAirline().getValue(), "ref_airline", RefAirline.class);
//	        String alc2 = GetterAccess.get(airline, p -> p.getRal2Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String alc3 = GetterAccess.get(airline, p -> p.getRal3Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String remp = GetterAccess.get(plDeparture, p -> p.getPdPubliccomment(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String ftyp = GetterAccess.get(plDeparture, p -> p.getPdRfstFlightstatus(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String csgn = GetterAccess.get(plDeparture, p -> p.getPdCallsign(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String sobt = convertDate(GetterAccess.get(plDeparture, p -> p.getPdSobt(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			String atot = convertDate(GetterAccess.get(plDeparture, p -> p.getPdAtot(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			String aobt = convertDate(GetterAccess.get(plDeparture, p -> p.getPdAobt(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			String eobt = convertDate(GetterAccess.get(plDeparture, p -> p.getPdEobt(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			RefAircrafttype refAircraftType = JAXBHelper.extractElementByLocalPart(plDeparture.getPdRactAircrafttype().getValue(), "ref_aircrafttype", RefAircrafttype.class);
//			String act3 = GetterAccess.get(refAircraftType, p -> p.getRactIatatype(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String act5 = GetterAccess.get(refAircraftType, p -> p.getRactIcaotype(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        PdRapRefnextairport.RefAirport nextairport = JAXBHelper.extractElementByLocalPart(plDeparture.getPdRapRefnextairport().getValue(), "ref_airport", PdRapRefnextairport.RefAirport.class);
//	        PdRapRefdestinationairport.RefAirport destinationairport = JAXBHelper.extractElementByLocalPart(plDeparture.getPdRapRefdestinationairport().getValue(), "ref_airport", PdRapRefdestinationairport.RefAirport.class);
//	        String nextRapIata3Lc = GetterAccess.get(nextairport, p -> p.getRapIata3Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String destinationRapIata3Lc = GetterAccess.get(destinationairport, p -> p.getRapIata3Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String via3 = !nextRapIata3Lc.equals("") ? nextRapIata3Lc : destinationRapIata3Lc;
//	        String nextRapIcao4lc = GetterAccess.get(nextairport, p -> p.getRapIcao4Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String destinationRapIcao4lc = GetterAccess.get(destinationairport, p -> p.getRapIcao4Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        String via4 = !nextRapIcao4lc.equals("") ? nextRapIcao4lc : destinationRapIcao4lc;
//	        
//			RefAircraft refAircraft = JAXBHelper.extractElementByLocalPart(plDeparture.getPdRacAircraft().getValue(), "ref_aircraft", RefAircraft.class);
//			String racRegistration = GetterAccess.get(refAircraft, p -> p.getRacRegistration(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String paRegistration = GetterAccess.get(plDeparture, p -> p.getPdRegistration(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String regn = !paRegistration.equals("") ? paRegistration : racRegistration;
//			RefCountrytype refCountrytype = JAXBHelper.extractElementByLocalPart(plDeparture.getPdRcttCountrytype().getValue(), "ref_countrytype", RefCountrytype.class);
//			String flti = GetterAccess.get(refCountrytype, p -> p.getRcttCode(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			RefServicetypecode refServicetypecode = JAXBHelper.extractElementByLocalPart(plDeparture.getPdRstcRefservicetypecode().getValue(), "ref_servicetypecode", RefServicetypecode.class);
//			String sytp = GetterAccess.get(refServicetypecode, p -> p.getRstcRistcIatacode(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String pstd = GetterAccess.get(plDeparture, p -> p.getPdRstaStand(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			String fcal = convertDate(GetterAccess.get(plDeparture, p -> p.getPdSecondcall(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
////			String gta1or2 = GetterAccess.get(plDeparture, p -> p.getPdArrivalgates(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("")
//			String tobt = convertDate(GetterAccess.get(plDeparture, p -> p.getPdTobt(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			
//			List<PlDeparturegate> lstplDeparturegate = plDeparture.getPlDeparturegateList().getPlDeparturegate();
//			String gtd1 = "";
//			String gd1b = "";
//			String gd1e = "";
//			String gd1x = "";
//			String gd1y = "";
//			String gtd2 = "";
//			String gd2b = "";
//			String gd2e = "";
//			String gd2x = "";
//			String gd2y = "";
//			if(lstplDeparturegate.size() == 1) {
//				gtd1 = GetterAccess.get(lstplDeparturegate.get(0), p -> p.getPdgRgtGate(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				gd1b = convertDate(GetterAccess.get(lstplDeparturegate.get(0), p -> p.getPdgBeginplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				gd1e = convertDate(GetterAccess.get(lstplDeparturegate.get(0), p -> p.getPdgEndplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				gd1x = convertDate(GetterAccess.get(lstplDeparturegate.get(0), p -> p.getPdgBeginactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				gd1y = convertDate(GetterAccess.get(lstplDeparturegate.get(0), p -> p.getPdgEndactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			}
//			if(lstplDeparturegate.size() == 2) {
//				gtd2 = GetterAccess.get(lstplDeparturegate.get(1), p -> p.getPdgRgtGate(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				gd2b = convertDate(GetterAccess.get(lstplDeparturegate.get(1), p -> p.getPdgBeginplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				gd2e = convertDate(GetterAccess.get(lstplDeparturegate.get(1), p -> p.getPdgEndplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				gd2x = convertDate(GetterAccess.get(lstplDeparturegate.get(1), p -> p.getPdgBeginactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				gd2y = convertDate(GetterAccess.get(lstplDeparturegate.get(1), p -> p.getPdgEndactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			}
//			
////			String blt1or2 = GetterAccess.get(plDeparture, p -> p.getPdBaggagebelts(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//			List<PlDeparturebelt> lstPlDeparturebelt = plDeparture.getPlDeparturebeltList().getPlDeparturebelt();
//			String blt1 = "";
//			String b1ba = "";
//			String b1ea = "";
//			String b1bs = "";
//			String b1es = "";
//			String blt2 = "";
//			String b2ba = "";
//			String b2ea = "";
//			String b2bs = "";
//			String b2es = "";
//			if(lstPlDeparturebelt.size() > 0) {
//				blt1 = GetterAccess.get(lstPlDeparturebelt.get(0), p -> p.getPdbRdbDeparturebelt(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				b1ba = convertDate(GetterAccess.get(lstPlDeparturebelt.get(0), p -> p.getPdbBeginactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b1ea = convertDate(GetterAccess.get(lstPlDeparturebelt.get(0), p -> p.getPdbEndactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b1bs = convertDate(GetterAccess.get(lstPlDeparturebelt.get(0), p -> p.getPdbBeginplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b1es = convertDate(GetterAccess.get(lstPlDeparturebelt.get(0), p -> p.getPdbEndplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			}
//			if(lstPlDeparturebelt.size() > 1) {
//				blt2 = GetterAccess.get(lstPlDeparturebelt.get(1), p -> p.getPdbRdbDeparturebelt(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//				b2ba = convertDate(GetterAccess.get(lstPlDeparturebelt.get(1), p -> p.getPdbBeginactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b2ea = convertDate(GetterAccess.get(lstPlDeparturebelt.get(1), p -> p.getPdbEndactual(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b2bs = convertDate(GetterAccess.get(lstPlDeparturebelt.get(1), p -> p.getPdbBeginplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//				b2es = convertDate(GetterAccess.get(lstPlDeparturebelt.get(1), p -> p.getPdbEndplan(), v1 -> v1.getValue(), v2 -> v2.getValue()).orElse(null));
//			}
//			
//			LOGGER.info("URNO: " + urno);
////	        LOGGER.info("RKEY: " + rkey);
////	        LOGGER.info("FLNO: " + flno);
////	        LOGGER.info("JFNO: " + jfno);
////	        LOGGER.info("TTYP: " + ttyp);
////	        LOGGER.info("ADID: D");
////			LOGGER.info("ALC2: " + alc2);
////			LOGGER.info("ALC3: " + alc3);
////			LOGGER.info("ACT3: " + act3);
////			LOGGER.info("ACT5: " + act5);
////			LOGGER.info("REMP: " + remp);
////			LOGGER.info("FTYP: " + ftyp);
////			LOGGER.info("CSGN: " + csgn);
////			LOGGER.info("VIA3: " + via3);
////			LOGGER.info("VIA4: " + via4);
////	        LOGGER.info("DES3: " + via3);
////	        LOGGER.info("DES4: " + via4);
////			LOGGER.info("REGN: " + regn);
//			LOGGER.info("STOD,SOBT: " + sobt + "("+plDeparture.getPdSobt().getValue().getValue()+")");
////			LOGGER.info("EOBT,ETOD,ETDI: " + eobt);
////			LOGGER.info("AOBT: " + aobt);
////			LOGGER.info("ATOT,AIRB: " + atot);
////			LOGGER.info("FCAL: " + fcal);
////			LOGGER.info("TOBT: " + tobt);
////			LOGGER.info("FLTI: " + flti);
////			LOGGER.info("SYTP: " + sytp);
////			LOGGER.info("PSTD: " + pstd);
////			LOGGER.info("GTD1: " + gtd1);
////			LOGGER.info("GD1B: " + gd1b);
////			LOGGER.info("GD1E: " + gd1e);
////			LOGGER.info("GD1X: " + gd1x);
////			LOGGER.info("GD1Y: " + gd1y);
////			LOGGER.info("GTD2: " + gtd2);
////			LOGGER.info("GD2B: " + gd2b);
////			LOGGER.info("GD2E: " + gd2e);
////			LOGGER.info("GD2X: " + gd2x);
////			LOGGER.info("GD2Y: " + gd2y);
////			LOGGER.info("BLT1: " + blt1);
////			LOGGER.info("B1BA: " + b1ba);
////			LOGGER.info("B1EA: " + b1ea);
////			LOGGER.info("B1BS: " + b1bs);
////			LOGGER.info("B1ES: " + b1es);
////			LOGGER.info("BLT2: " + blt2);
////			LOGGER.info("B2BA: " + b2ba);
////			LOGGER.info("B2EA: " + b2ea);
////			LOGGER.info("B2BS: " + b2bs);
////			LOGGER.info("B2ES: " + b2es);
//			
//			String vial = "";
//	        for (PlDeparture.PlRoutingList.PlRouting plRouting : plDeparture.getPlRoutingList().getPlRouting()) {
//	        	PlDeparture.PlRoutingList.PlRouting.PrtRapRefairport.RefAirport refAirport = JAXBHelper.extractElementByLocalPart(plRouting.getPrtRapRefairport().getValue(), "ref_airport", PlDeparture.PlRoutingList.PlRouting.PrtRapRefairport.RefAirport.class);
//	        	via3 = GetterAccess.get(refAirport, p -> p.getRapIata3Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        	via4 = GetterAccess.get(refAirport, p -> p.getRapIcao4Lc(), v1 -> v1.getValue(), v2 -> v2.getValue(), v -> v.toString()).orElse("");
//	        	vial = vial+getVial(via3,via4);
//	        }
//	        int vian = plDeparture.getPlRoutingList().getPlRouting().size();
////        	LOGGER.info("VIAL: " + vial);
////        	LOGGER.info("VIAN: " + vian);
//        }
//        
//        return fidsAfttab;
//    }
    
    private static String convertDate(XMLGregorianCalendar calendar) {
    	if(calendar == null) {
    		return "";
    	}
    	ZonedDateTime date = calendar.toGregorianCalendar().toZonedDateTime()
    		    .withZoneSameInstant(ZoneId.of("UTC"));
    	String formatted = date.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    	return formatted;
    }
    
   public static String getVial(String via3,String via4) {
		String SIBT="              ";
		String EIBT="              ";
		String ALDT="              ";
		String AIBT="              ";
		String SOBT="              ";
		String EOBT="              ";
		String AOBT="              ";
		String ATOT="              ";
		return " "+via3+via4+SIBT+EIBT+ALDT+AIBT+SOBT+EOBT+AOBT+ATOT;
	}
    
    //For test only
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        try {
        	File xmlFile = new File("update.xml");
	        JAXBContext jaxbContext = JAXBContext.newInstance(Envelope.class);
	        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
	        
	        Envelope envelope = (Envelope) unmarshaller.unmarshal(xmlFile);
//	        FidsAfttab fidsAfttab = convertPlTurntoAfttab(envelope);
	        
	    } catch (JAXBException e) {
	        e.printStackTrace();
	    }
    }
}