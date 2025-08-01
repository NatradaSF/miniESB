package sf.sfis.miniesb.utility;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.model.FidsAirport;
import sf.sfis.miniesb.model.FidsCcatab;
import sf.sfis.miniesb.repository.FidsAirportRepository;

@Component
@RequiredArgsConstructor
public class TranformFidsAfttab {
	private final Logger LOGGER = LoggerFactory.getLogger(TranformFidsAfttab.class);
	private final DateTimeFormatHelper dateTimeFormatHelper;
	private final FidsAirportRepository fidsAirportRepository;
	FidsAfttab fidsAfttab = null;
	Document doc = null;
	XPath xpath = XPathFactory.newInstance().newXPath();

	private String convertDateStringIfNeeded(String input) {
		if (input == null)
			return null;
		// ตรวจสอบ pattern แบบ ISO 8601 เบื้องต้น เช่น 2024-03-28T11:36:00Z
		if (input.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
			return input.replaceAll("[-:TZ]", "");
		}
		return input;
	}

	public FidsAfttab convertPlTurntoAfftab(String xmlString, String actionType, String hopo, String adid) {
		try {
//			LOGGER.info("convertPlTurntoAfftab");
//			LOGGER.info(xmlString);
			fidsAfttab = null;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(new InputSource(new StringReader(xmlString)));

			String plTurn = (String) xpath.evaluate("//pl_turn", doc, XPathConstants.STRING);
			NodeList arrivalList = doc.getElementsByTagName("pl_arrival");
			NodeList departureList = doc.getElementsByTagName("pl_departure");
			String urnoArr = (String) xpath.evaluate("//pa_idseq", doc, XPathConstants.STRING);
			String urnoDep = (String) xpath.evaluate("//pd_idseq", doc, XPathConstants.STRING);
			boolean hasArrival = !urnoArr.equals("");
			boolean hasDeparture = !urnoDep.equals("");

//			LOGGER.info("plTurn : "+plTurn);
			if(!plTurn.equals("")) {//Flight
				if (adid.equalsIgnoreCase("A") && hasArrival) {
					Element arrivalElement = (Element) arrivalList.item(0);
					if (actionType.equalsIgnoreCase("DATASET") || (actionType.equalsIgnoreCase("UPDATE") && ("update".equalsIgnoreCase(arrivalElement.getAttribute("action")) || "insert".equalsIgnoreCase(arrivalElement.getAttribute("action"))))) {
						fidsAfttab = new FidsAfttab();
						fidsAfttab.setAdid("A");
						processPaths(adid,actionType);
						defineVial(arrivalElement,hopo,adid,actionType);

						if(actionType.equalsIgnoreCase("UPDATE")) {
							fidsAfttab.setFieldsNotNull(FieldInspector.getNonNullFields(fidsAfttab));
							LOGGER.info("Fields arrival updated : "+fidsAfttab.getFieldsNotNull());
						}
						
						fixPaths(arrivalElement,adid);
						String airport4 = "";
						Optional<FidsAirport> queryFidsAirport = fidsAirportRepository.findById(hopo);
						if (queryFidsAirport.isPresent()) {
							airport4 = queryFidsAirport.get().getApc4();
						}
						fidsAfttab.setDes3(hopo);
						fidsAfttab.setDes4(airport4);
					}
				} else if (adid.equalsIgnoreCase("D") && hasDeparture) {
					Element departureElement = (Element) departureList.item(0);
					if (actionType.equalsIgnoreCase("DATASET") || (actionType.equalsIgnoreCase("UPDATE") && ("update".equalsIgnoreCase(departureElement.getAttribute("action")) || "insert".equalsIgnoreCase(departureElement.getAttribute("action"))))) {
						fidsAfttab = new FidsAfttab();
						fidsAfttab.setAdid("D");
						processPaths(adid,actionType);
						defineVial(departureElement,hopo,adid,actionType);
						
						if(actionType.equalsIgnoreCase("UPDATE")) {
							fidsAfttab.setFieldsNotNull(FieldInspector.getNonNullFields(fidsAfttab));
							LOGGER.info("Fields departure updated : "+fidsAfttab.getFieldsNotNull());
						}
						
						fixPaths(departureElement,adid);
						String airport4 = "";
						Optional<FidsAirport> queryFidsAirport = fidsAirportRepository.findById(hopo);
						if (queryFidsAirport.isPresent()) {
							airport4 = queryFidsAirport.get().getApc4();
						}
						fidsAfttab.setOrg3(hopo);
						fidsAfttab.setOrg4(airport4);
					}
				} else {
					return null;
				}
				
				// ตรวจสอบ RTYP จาก tag pl_arrival และ pl_departure
				if(fidsAfttab!=null) {
					fidsAfttab.setHopo(hopo);
					FieldInspector.replaceHoldWithEmpty(fidsAfttab);
					if (hasArrival && hasDeparture) {
						fidsAfttab.setRtyp("J");
					} else if (hasArrival || hasDeparture) {
						fidsAfttab.setRtyp("S");
					}
					if(fidsAfttab.getMtow()!=null&&!fidsAfttab.getMtow().trim().equals("")) {
						String mtow = fidsAfttab.getMtow();
						int number = Integer.parseInt(mtow);
						int result = (int) Math.ceil((double) number / 1000);
						fidsAfttab.setMtow(Integer.toString(result));
					}
				}
			}else {//Common Counter
				if(adid.equalsIgnoreCase("D")) {
					fidsAfttab = new FidsAfttab();
					NodeList counterList = doc.getElementsByTagName("pl_desk");
					if (counterList.getLength() > 0) {
						Element counterElement = (Element) counterList.item(0);
	//					Element element = doc.getElementsByTagName("pl_desk").getLength()>0?(Element)doc.getElementsByTagName("pl_desk").item(0):null;
						List<FidsCcatab> lstFidsCcatab = getCounters(counterElement, true);
	//					LOGGER.info("lstFidsCcatab : "+lstFidsCcatab.size());
						fidsAfttab.setLstFidsCcatab(lstFidsCcatab);
					}
				}
			}
			return fidsAfttab;
		} catch (Exception e) {
			LOGGER.error("Log : ", e);
//			e.printStackTrace();
		}
		return null;
	}

	/* Get value of all paths on DATASET */
	/* Get value of update paths on UPDATE */
	private void processPaths(String adid, String actionType) {
		Map<String, BiConsumer<FidsAfttab, BigDecimal>> pathMapBigDecimal = adid.equalsIgnoreCase("A")
				? FidsAfttab.arrivalPathToSetterMapBigDecimal
				: FidsAfttab.departurePathToSetterMapBigDecimal;
		for (Map.Entry<String, BiConsumer<FidsAfttab, BigDecimal>> entry : pathMapBigDecimal.entrySet()) {
			String path = entry.getKey();
			try {
				String fixedPath = "string(//" + (path.startsWith("/") ? path.substring(1) : path);
				String textValue = (String) xpath.evaluate(fixedPath + "/text()[1])", doc, XPathConstants.STRING);
				String actionValue = (String) xpath.evaluate(fixedPath + "/@action)", doc, XPathConstants.STRING);
				if ((actionType.equalsIgnoreCase("DATASET") || (actionType.equalsIgnoreCase("UPDATE") && "update".equalsIgnoreCase(actionValue))) && textValue != null && !textValue.trim().isEmpty()) {
					String value = textValue.trim();
					try {
						Optional<BiConsumer<FidsAfttab, BigDecimal>> setterOpt = FidsAfttab
								.getSetterByPathBigDecimal(pathMapBigDecimal, path);
						setterOpt.ifPresent(setter -> setter.accept(fidsAfttab, new BigDecimal(value)));
					} catch (NumberFormatException e) {
						LOGGER.error(path+" is not BigDecimal. ");
					}
				}
			} catch (XPathExpressionException e) {
//				System.err.println("XPath error for path: " + path);
				LOGGER.error("XPath error for path: " + path, e);
//				e.printStackTrace();
			}
		}
		
		Map<String, BiConsumer<FidsAfttab, String>> pathMap = adid.equalsIgnoreCase("A") ? FidsAfttab.arrivalPathToSetterMap
				: FidsAfttab.departurePathToSetterMap;
		for (Map.Entry<String, BiConsumer<FidsAfttab, String>> entry : pathMap.entrySet()) {
			String path = entry.getKey();
			try {
				String fixedPath = "string(//" + (path.startsWith("/") ? path.substring(1) : path);
				String textValue = (String) xpath.evaluate(fixedPath + "/text()[1])", doc, XPathConstants.STRING);
				String actionValue = (String) xpath.evaluate(fixedPath + "/@action)", doc, XPathConstants.STRING);
				if ((actionType.equalsIgnoreCase("DATASET") || (actionType.equalsIgnoreCase("UPDATE") && "update".equalsIgnoreCase(actionValue))) && textValue != null && !textValue.trim().isEmpty()) {
					String value = textValue.trim();
					Optional<BiConsumer<FidsAfttab, String>> setterOpt = FidsAfttab.getSetterByPath(pathMap, path);
					String convertedText = convertDateStringIfNeeded(value);
					setterOpt.ifPresent(setter -> setter.accept(fidsAfttab, convertedText));
				}
			} catch (XPathExpressionException e) {
//				System.err.println("XPath error for path: " + path);
				LOGGER.error("XPath error for path: " + path, e);
//				e.printStackTrace();
			}
		}
		
		Map<String, BiConsumer<FidsAfttab, String>> pathMapDate = adid.equalsIgnoreCase("A") ? FidsAfttab.arrivalPathToSetterMapDate
				: FidsAfttab.departurePathToSetterMapDate;
		for (Map.Entry<String, BiConsumer<FidsAfttab, String>> entry : pathMapDate.entrySet()) {
			String path = entry.getKey();
			try {
				String fixedPath = "string(//" + (path.startsWith("/") ? path.substring(1) : path);
				String textValue = (String) xpath.evaluate(fixedPath + "/text()[1])", doc, XPathConstants.STRING);
				String actionValue = (String) xpath.evaluate(fixedPath + "/@action)", doc, XPathConstants.STRING);
				if ((actionType.equalsIgnoreCase("DATASET") || (actionType.equalsIgnoreCase("UPDATE") && "update".equalsIgnoreCase(actionValue))) && textValue != null && !textValue.trim().isEmpty()) {
					String value = textValue.trim();
					Optional<BiConsumer<FidsAfttab, String>> setterOpt = FidsAfttab.getSetterByPath(pathMapDate, path);
					String convertedText = convertDateStringIfNeeded(value);
					setterOpt.ifPresent(setter -> setter.accept(fidsAfttab, convertedText));
				}else if((actionType.equalsIgnoreCase("UPDATE") && "update".equalsIgnoreCase(actionValue)) && (textValue == null || textValue.trim().isEmpty())) {
					String value = " ";
					Optional<BiConsumer<FidsAfttab, String>> setterOpt = FidsAfttab.getSetterByPath(pathMapDate, path);
					String convertedText = convertDateStringIfNeeded(value);
					setterOpt.ifPresent(setter -> setter.accept(fidsAfttab, convertedText));
				}
			} catch (XPathExpressionException e) {
//				System.err.println("XPath error for path: " + path);
				LOGGER.error("XPath error for path: " + path, e);
//				e.printStackTrace();
			}
		}
		
		fidsAfttab.setEtai(fidsAfttab.getEibt());
		fidsAfttab.setEtoa(fidsAfttab.getEibt());
		fidsAfttab.setEtdi(fidsAfttab.getEobt());
		fidsAfttab.setEtod(fidsAfttab.getEobt());
		fidsAfttab.setLand(fidsAfttab.getAldt());
		fidsAfttab.setAirb(fidsAfttab.getAtot());
		fidsAfttab.setAxit(fidsAfttab.getExit());
		fidsAfttab.setAxot(fidsAfttab.getExot());
		fidsAfttab.setOnbl(fidsAfttab.getAibt());
		fidsAfttab.setOfbl(fidsAfttab.getAobt());
	}
	
	/* Get value for fix fields ex.URNO, RKEY, SOBT, SIBT, FLNO, CSGN, ALC2, ALC3 */
	/* Get value for list of fields ex.Belt data, Gate data*/
	/* Get value for shared fields ex.VIA3=ORG3, LAND=ALDT, AIRB=ATOT, ONBL=AIBT, OFBL=AOBT */
	private void fixPaths(Element element, String adid) {
		Set<String> bigDecimalFields = new LinkedHashSet<>(
				Arrays.asList("/pl_departure/pd_idseq", "/pl_arrival/pa_idseq",
						"/pl_turn/pt_idseq"));
		Map<String, BiConsumer<FidsAfttab, BigDecimal>> pathMapBigDecimal = adid.equalsIgnoreCase("A")
				? FidsAfttab.arrivalPathToSetterMapBigDecimal
				: FidsAfttab.departurePathToSetterMapBigDecimal;
		for (String path : bigDecimalFields) {
			try {
				String fixedPath = "string(//" + (path.startsWith("/") ? path.substring(1) : path);
				String textValue = (String) xpath.evaluate(fixedPath + "/text()[1])", doc, XPathConstants.STRING);
				if(textValue!=null) {
					String value = textValue.trim();
					try {
						Optional<BiConsumer<FidsAfttab, BigDecimal>> setterOpt = FidsAfttab
								.getSetterByPathBigDecimal(pathMapBigDecimal, path);
						setterOpt.ifPresent(setter -> setter.accept(fidsAfttab, new BigDecimal(value)));
					} catch (NumberFormatException e) {
						LOGGER.error(path+" is not BigDecimal. ");
					}
				}
			} catch (XPathExpressionException e) {
//				System.err.println("XPath error for path: " + path);
				LOGGER.error("XPath error for path: " + path, e);
//				e.printStackTrace();
			}
		}
		
		Set<String> stringFields = new LinkedHashSet<>(Arrays.asList("/pl_departure/pd_sobt", "/pl_arrival/pa_sibt",
				"/pl_departure/pd_flightnumber", "/pl_arrival/pa_flightnumber",
				"/pl_departure/pd_callsign", "/pl_arrival/pa_callsign",
				"/pl_departure/pd_rctt_countrytype", "/pl_arrival/pa_rctt_countrytype",
				"/pl_arrival/pa_ral_airline/ref_airline/ral_2lc", "/pl_departure/pd_ral_airline/ref_airline/ral_2lc",
				"/pl_arrival/pa_ral_airline/ref_airline/ral_3lc", "/pl_departure/pd_ral_airline/ref_airline/ral_3lc"));
		Map<String, BiConsumer<FidsAfttab, String>> pathMap = adid.equalsIgnoreCase("A") ? FidsAfttab.arrivalPathToSetterMap
				: FidsAfttab.departurePathToSetterMap;
		for (String path : stringFields) {
			try {
				String fixedPath = "string(//" + (path.startsWith("/") ? path.substring(1) : path);
				String textValue = (String) xpath.evaluate(fixedPath + "/text()[1])", doc, XPathConstants.STRING);
				String value = textValue.trim();
				Optional<BiConsumer<FidsAfttab, String>> setterOpt = FidsAfttab.getSetterByPath(pathMap, path);
				String convertedText = convertDateStringIfNeeded(value);
				setterOpt.ifPresent(setter -> setter.accept(fidsAfttab, convertedText));
			} catch (XPathExpressionException e) {
//				System.err.println("XPath error for path: " + path);
				LOGGER.error("XPath error for path: " + path, e);
//				e.printStackTrace();
			}
		}
		
		String flno = fidsAfttab.getFlno();
		if (flno != null && !flno.equals("")) {
			Map<String, String> parts = parseFlightNumber(flno, fidsAfttab.getAlc3(), fidsAfttab.getAlc2());
			fidsAfttab.setFlno(parts.isEmpty()?flno:parts.get("prefix")+parts.get("number")+parts.get("suffix"));
			fidsAfttab.setFlns(parts.get("suffix")!=null?parts.get("suffix"):"");
//			String[] lstJfno = fidsAfttab.getJfno() != null ? fidsAfttab.getJfno().split(",") : new String[0];
//			for (int i = 0; i < lstJfno.length; i++) {
//				parts = parseFlightNumber(flno, fidsAfttab.getAlc3(), fidsAfttab.getAlc2());
//	            System.out.println(lstJfno[i].trim());  // trim() เอาช่องว่างรอบๆ ออก
//	        }
			fidsAfttab.setJfno(fidsAfttab.getJfno() != null ? fidsAfttab.getJfno().replace(",", " ") : null);
			fidsAfttab.setCsgn(fidsAfttab.getCsgn() + fidsAfttab.getFlns().trim());
			fidsAfttab.setFltn(parts.get("number"));
			System.out.println("==================");
			System.out.println(fidsAfttab.getFlno());
		}
		
		String ftyp = fidsAfttab.getFtyp();
		if(ftyp != null) {
			if(ftyp.equalsIgnoreCase("V")) {
				ftyp = "D";
			}else if(!ftyp.equalsIgnoreCase("X")) {
				ftyp = "O";
			}
			fidsAfttab.setFtyp(ftyp);
		}
		
		String trkn = fidsAfttab.getTrkn();
		if(trkn != null && !trkn.equals("") && trkn.length()>4) {
			trkn = trkn.substring(2); // ตัดสองตัวแรกออก
			fidsAfttab.setTrkn(trkn);
		}

		NodeList beltNodes = adid.equalsIgnoreCase("A")?element.getElementsByTagName("pl_baggagebelt"):element.getElementsByTagName("pl_departurebelt");
		for (int i = 0; i < beltNodes.getLength(); i++) {
			Node beltNode = beltNodes.item(i);
			try {
				String prefix = adid.equalsIgnoreCase("A") ? "pbb_" : "pdb_";

			    String ba = convertDateStringIfNeeded(xpath.evaluate(prefix + "beginactual", beltNode));
			    String bs = convertDateStringIfNeeded(xpath.evaluate(prefix + "beginplan", beltNode));
			    String ea = convertDateStringIfNeeded(xpath.evaluate(prefix + "endactual", beltNode));
			    String es = convertDateStringIfNeeded(xpath.evaluate(prefix + "endplan", beltNode));
			    String blt = convertDateStringIfNeeded(xpath.evaluate(prefix + (adid.equalsIgnoreCase("A")?"rbb_baggagebelt":"rdb_departurebelt"), beltNode));
			    String tmb = convertDateStringIfNeeded(xpath.evaluate(prefix + (adid.equalsIgnoreCase("A")?"rbb_refbaggagebelt/ref_baggagebelt/rbb_rctt_countrytype":"rdb_refbaggagebelt/ref_baggagebelt/rdb_rctt_countrytype"), beltNode));
			    String bast = convertDateStringIfNeeded(xpath.evaluate(prefix + "status", beltNode));
			    
			    setDynamicValue(fidsAfttab, "b", i, "ba", ba);
			    setDynamicValue(fidsAfttab, "b", i, "bs", bs);
			    setDynamicValue(fidsAfttab, "b", i, "ea", ea);
			    setDynamicValue(fidsAfttab, "b", i, "es", es);
			    
			    setDynamicValue(fidsAfttab, "bas", i, "", ba);
			    setDynamicValue(fidsAfttab, "bao", i, "", bs);
			    setDynamicValue(fidsAfttab, "bae", i, "", ea);
			    setDynamicValue(fidsAfttab, "bac", i, "", es);
			    setDynamicValue(fidsAfttab, "blt", i, "", blt);
			    setDynamicValue(fidsAfttab, "tmb", i, "", tmb);
			    setDynamicValue(fidsAfttab, "baz", i, "", blt);
			    fidsAfttab.setBast(bast);
			} catch (XPathExpressionException e) {
				LOGGER.error("XPath error for beltNode: ", e);
//				e.printStackTrace();
			}
		}
		
		String gts = adid.equalsIgnoreCase("A")?element.getElementsByTagName("pa_arrivalgates").item(0).getTextContent():element.getElementsByTagName("pd_departuregates").item(0).getTextContent();
		Set<String> gates = !gts.equals("")?new LinkedHashSet<>(List.of(gts.split(","))):new LinkedHashSet<>();
		List<String> lstGates = new ArrayList<>(gates);
		for (int i = 0; i < lstGates.size(); i++) {
//			LOGGER.info(lstGates.get(i));
			setDynamicValue(fidsAfttab, "gt"+(adid.equalsIgnoreCase("A")?"a":"d"), i, "", lstGates.get(i));
		}
		
		NodeList gateNodes = adid.equalsIgnoreCase("A")?element.getElementsByTagName("pl_arrivalgate"):element.getElementsByTagName("pl_departuregate");
		for (int i = 0; i < gateNodes.getLength(); i++) {
			Node gateNode = gateNodes.item(i);
			try {
				String prefix = adid.equalsIgnoreCase("A") ? "pag_" : "pdg_";

			    String gax = convertDateStringIfNeeded(xpath.evaluate(prefix + "beginactual", gateNode));
			    String gab = convertDateStringIfNeeded(xpath.evaluate(prefix + "beginplan", gateNode));
			    String gay = convertDateStringIfNeeded(xpath.evaluate(prefix + "endactual", gateNode));
			    String gae = convertDateStringIfNeeded(xpath.evaluate(prefix + "endplan", gateNode));
//			    String gt = convertDateStringIfNeeded(xpath.evaluate(prefix + "rgt_gate", gateNode));
			    
			    setDynamicValue(fidsAfttab, "g"+(adid.equalsIgnoreCase("A")?"a":"d"), i, "b", gab);
			    setDynamicValue(fidsAfttab, "g"+(adid.equalsIgnoreCase("A")?"a":"d"), i, "x", gax);
			    setDynamicValue(fidsAfttab, "g"+(adid.equalsIgnoreCase("A")?"a":"d"), i, "y", gay);
			    setDynamicValue(fidsAfttab, "g"+(adid.equalsIgnoreCase("A")?"a":"d"), i, "e", gae);
//			    setDynamicValue(fidsAfttab, "gt"+(adid.equalsIgnoreCase("A")?"a":"d"), i, "", gt);
			} catch (XPathExpressionException e) {
				LOGGER.error("XPath error for gateNode: ", e);
//				e.printStackTrace();
			}
		}
		
		NodeList delayreasonNodes = adid.equalsIgnoreCase("A")?element.getElementsByTagName("pl_delayreason"):element.getElementsByTagName("pl_delayreason");
		for (int i = 0; i < delayreasonNodes.getLength(); i++) {
			Node delayreasonNode = delayreasonNodes.item(i);
			try {
				String prefix = adid.equalsIgnoreCase("A") ? "pdlr_" : "pdlr_";

			    String dcd = convertDateStringIfNeeded(xpath.evaluate(prefix + "rdlr_delayreason", delayreasonNode));
			    String dela = convertDateStringIfNeeded(xpath.evaluate(prefix + "delay", delayreasonNode));
			    
			    setDynamicValue(fidsAfttab, "dcd", i, "", dcd);
			    setDynamicValue(fidsAfttab, "dtd", i, "", dela);
			    fidsAfttab.setDela(dela);
			} catch (XPathExpressionException e) {
				LOGGER.error("XPath error for delayreasonNode: ", e);
//				e.printStackTrace();
			}
		}
		
		if(adid.equalsIgnoreCase("D")) {
			List<FidsCcatab> lstFidsCcatab = getCounters(element, false);
//			LOGGER.info("lstFidsCcatab : "+lstFidsCcatab.size());
			fidsAfttab.setLstFidsCcatab(lstFidsCcatab);
		}
		
		fidsAfttab.setAurn(fidsAfttab.getRkey().toString());
		fidsAfttab.setBags(fidsAfttab.getBagn());
//		fidsAfttab.setBlt2(fidsAfttab.getBlt1());
		fidsAfttab.setDcd2(fidsAfttab.getDcd1());
//		fidsAfttab.setOrg3(fidsAfttab.getVia3());
//		fidsAfttab.setOrg4(fidsAfttab.getVia4());
		fidsAfttab.setStoa(fidsAfttab.getSibt());
//		fidsAfttab.setDes3(fidsAfttab.getVia3());
//		fidsAfttab.setDes4(fidsAfttab.getVia4());
		fidsAfttab.setStod(fidsAfttab.getSobt());
		if(adid.equalsIgnoreCase("A")) {
			fidsAfttab.setFlda(dateTimeFormatHelper.convertUTCToLocal(fidsAfttab.getSibt()).substring(0, 8));
		}else {
			fidsAfttab.setFlda(dateTimeFormatHelper.convertUTCToLocal(fidsAfttab.getSobt()).substring(0, 8));
		}
		fidsAfttab.setDtd2(fidsAfttab.getDtd1());
//		fidsAfttab.setGta2(fidsAfttab.getGta1());
//		fidsAfttab.setTga1(fidsAfttab.getGta1());
//		fidsAfttab.setGtd2(fidsAfttab.getGtd1());
//		fidsAfttab.setTgd1(fidsAfttab.getGtd1());
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime localDateTime = null;
		if(fidsAfttab.getStoa()!=null) {
			localDateTime = LocalDateTime.parse(fidsAfttab.getStoa(), formatter);
	        // บอกว่าเวลานี้เป็น UTC
	        OffsetDateTime utcDateTime = localDateTime.atOffset(ZoneOffset.UTC);
	        DayOfWeek dayOfWeekUTC = utcDateTime.getDayOfWeek();
	        fidsAfttab.setDooa(Integer.toString(dayOfWeekUTC.getValue()));
		}else {
			fidsAfttab.setSibt(" ");
			fidsAfttab.setStoa(" ");
		}
		
		if(fidsAfttab.getStod()!=null) {
			localDateTime = LocalDateTime.parse(fidsAfttab.getStod(), formatter);
	        // บอกว่าเวลานี้เป็น UTC
	        OffsetDateTime utcDateTime = localDateTime.atOffset(ZoneOffset.UTC);
	        DayOfWeek dayOfWeekUTC = utcDateTime.getDayOfWeek();
	        fidsAfttab.setDood(Integer.toString(dayOfWeekUTC.getValue()));
		}else {
			fidsAfttab.setSobt(" ");
			fidsAfttab.setStod(" ");
		}
	}
	
	private void defineVial(Element element, String hopo, String adid, String actionType) {
		try {
			String route = (String) xpath.evaluate("//pt_routing", doc, XPathConstants.STRING);
//			LOGGER.info("==========="+route);
			String[] airports = route.split("-");
			String via3 = "";
			for (int i = 0; i < airports.length; i++) {
	            if (hopo.equals(airports[i])) {
	            	if(adid.equalsIgnoreCase("A")){
	            		if (i > 1) { // มากกว่า 1 airport ด้านหน้า BKK
		            		via3 = airports[i-1];
	                    }
	            	}else {
	            		if(i < airports.length - 2) {
		            		via3 = airports[i+1];
	            		}
	            	}
//        			LOGGER.info("==========="+adid+" : "+via3);
	                break; // ถ้าเจอ BKK แล้ว ไม่ต้อง loop ต่อ
	            }
	        }
			
			NodeList routingNodes = element.getElementsByTagName("pl_routing");
			String vial = null;
			for (int i = 0; i < routingNodes.getLength(); i++) {
//			Node routingNode = routingNodes.getLength()>0 ? routingNodes.item(0) : null;
//			if(adid.equalsIgnoreCase("A") && routingNodes.getLength()>1) {
				Node routingNode = routingNodes.item(i);
//			}
				if(routingNode!=null) {
					String actionValue = xpath.evaluate("prt_rap_refairport/ref_airport/rap_iata3lc/@action", routingNode);
					if(actionType.equalsIgnoreCase("DATASET") || (actionType.equalsIgnoreCase("UPDATE") && "update".equalsIgnoreCase(actionValue))) {
						String rapIata3lc = xpath.evaluate("prt_rap_refairport/ref_airport/rap_iata3lc", routingNode);
						String rapIcao4lc = xpath.evaluate("prt_rap_refairport/ref_airport/rap_icao4lc", routingNode);
						if (rapIata3lc != null && !rapIata3lc.isEmpty() && rapIcao4lc != null
								&& !rapIcao4lc.isEmpty() && rapIata3lc.equals(via3)) {
							vial = getVial(rapIata3lc, rapIcao4lc);
							
							fidsAfttab.setVia3(rapIata3lc);
							fidsAfttab.setVia4(rapIcao4lc);
							fidsAfttab.setVial(vial);
							fidsAfttab.setVian(routingNodes.getLength()>0? "1":"0");
//		        			LOGGER.info("==========="+vial);
							break;
						}
					}
				}
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
	}
	
	private List<FidsCcatab> getCounters(Element element, boolean isCommon) {
		List<FidsCcatab> lstFidsCcatab = new ArrayList<FidsCcatab>();
		NodeList counterNodes = null;
		if(isCommon) {
			Node counterNode = element;
			counterNodes = new ImplementNodeList(Arrays.asList(counterNode));
		}else {
			counterNodes = element.getElementsByTagName("pl_desk");
		}
		
//		LOGGER.info("counterNodes : "+counterNodes.getLength());
		for (int i = 0; i < counterNodes.getLength(); i++) {
			FidsCcatab fidsCcatab = new FidsCcatab();
			Node counterNode = counterNodes.item(i);
			try {
			    String ckic = convertDateStringIfNeeded(xpath.evaluate("pdk_rcnt_counter", counterNode));
			    String ckbs = convertDateStringIfNeeded(xpath.evaluate("pdk_beginplan", counterNode));
			    String ckes = convertDateStringIfNeeded(xpath.evaluate("pdk_endplan", counterNode));
			    String ckba = convertDateStringIfNeeded(xpath.evaluate("pdk_beginactual", counterNode));
			    String ckea = convertDateStringIfNeeded(xpath.evaluate("pdk_endactual", counterNode));
			    if(!ckic.equals("HOLD")) {
				    if(!isCommon) {
					    String cnts = element.getElementsByTagName("pd_counters").item(0).getTextContent();
					    Set<String> counters = !cnts.equals("")?new LinkedHashSet<>(List.of(cnts.split(","))):new LinkedHashSet<>();
					    LOGGER.info("ckic : "+ckic);
					    LOGGER.info("counters : "+counters);
					    String ctyp = counters.contains(ckic)?" ":"C";
					    if(ctyp.equals(" ")) {
						    fidsCcatab.setCkic(ckic);
						    fidsCcatab.setCtyp(ctyp);
						    fidsCcatab.setCkbs(ckbs);
						    fidsCcatab.setCkes(ckes);
						    fidsCcatab.setCkba(ckba);
						    fidsCcatab.setCkea(ckea);
						    lstFidsCcatab.add(fidsCcatab);
					    }
				    }else {
				    	String flnu = convertDateStringIfNeeded(xpath.evaluate("pdk_idseq", counterNode));
				    	String flno = convertDateStringIfNeeded(xpath.evaluate("pdk_rcnt_refmastercci/ref_counter/rcnt_ral_airline/ref_airline/ral_2lc", counterNode));
				    	fidsCcatab.setFlnu(new BigDecimal(flnu));
				    	fidsCcatab.setFlno(String.format("%-9s", flno));
					    fidsCcatab.setCkic(ckic);
					    fidsCcatab.setCtyp("C");
					    fidsCcatab.setCkbs(ckbs);
					    fidsCcatab.setCkes(ckes);
					    fidsCcatab.setCkba(ckba);
					    fidsCcatab.setCkea(ckea);
					    lstFidsCcatab.add(fidsCcatab);
				    }
			    }
			} catch (XPathExpressionException e) {
				LOGGER.error("XPath error for counterNode: ", e);
//				e.printStackTrace();
			}
		}
		return lstFidsCcatab;
	}

	public String getVial(String via3, String via4) {
		String SIBT = "              ";
		String EIBT = "              ";
		String ALDT = "              ";
		String AIBT = "              ";
		String SOBT = "              ";
		String EOBT = "              ";
		String AOBT = "              ";
		String ATOT = "              ";
		return " " + via3 + via4 + SIBT + EIBT + ALDT + AIBT + SOBT + EOBT + AOBT + ATOT;
	}

    public static Map<String, String> parseFlightNumber(String flightNumber, String alc3, String alc2) {
    	String prefix = "";
        String number = "";
        String suffix = "";
    	
	    if (alc3!=null&&!alc3.equals("")&&flightNumber.startsWith(alc3)) {
	    	prefix = alc3;
	        String rest = flightNumber.substring(alc3.length());
	        Matcher matcher = Pattern.compile("^(\\d{1,4})([A-Z]?)$").matcher(rest);
	        if (matcher.find()) {
	            number = String.format("%03d", Integer.parseInt(matcher.group(1)));
	            suffix = matcher.group(2);
	        }
	    }else if (alc2!=null&&!alc2.equals("")&&flightNumber.startsWith(alc2)) {
	        prefix = alc2;
	        String rest = flightNumber.substring(alc2.length());
	        Matcher matcher = Pattern.compile("^(\\d{1,4})([A-Z]?)$").matcher(rest);
	        if (matcher.find()) {
	        	// เติม 0 ด้านหน้าให้เลขครบ 3 หลักขึ้นไป
		        if (number.length() < 3) {
		            number = String.format("%03d", Integer.parseInt(matcher.group(1)));
		        }
	            suffix = matcher.group(2);
	        }
	    }else {
	    	return Collections.emptyMap(); // ถ้าไม่เข้า pattern
	    }
	        // ถ้า prefix มี 2 ตัว ให้เว้นวรรค 1 ช่อง
	        if (prefix.length() == 2) {
	        	prefix = prefix+" ";
	        }
	        
            if (number.length() == 4) {
            	number = number+" "; // เว้นวรรค 1 ช่องถ้าเลข 4 หลัก
            } else {
            	number = number+"  "; // เว้นวรรค 2 ช่องถ้าเลขไม่ถึง 4 หลัก
            }

	        Map<String, String> map = new HashMap<>();
	        map.put("prefix", prefix);
	        map.put("number", number);
	        map.put("suffix", suffix);
	        return map;
	}
	
	public void setDynamicValue(FidsAfttab obj, String fieldPrefix, int index, String fieldSuffix, String value) {
	    try {
	        // เช่น fieldName = b1ba, b2bs, ...
	        String fieldName = fieldPrefix + (index + 1) + fieldSuffix;
	        Field field = FidsAfttab.class.getDeclaredField(fieldName);
	        field.setAccessible(true);
	        field.set(obj, value);  // ต้องเป็น String
//	        LOGGER.info(fieldName+" : "+value);
	    } catch (Exception e) {
			LOGGER.error("setDynamicValue: ", e);
//	        System.err.println("Error setting field: " + e.getMessage());
	    }
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
//		File xmlFile = new File("update_new.xml");
//		String xmlContent = new String(Files.readAllBytes(Paths.get(xmlFile.getPath())), "UTF-8");
//		convertPlTurntoAfftab(xmlContent, "DATASET", "A");
//		ESBResponseService.convertFidsAfftabtoEsb("202505315212", fidsAfttab);
//		Map<String, String> parts = parseFlightNumber("8K803D","","8K");
//		String flno = parts.get("prefix")+parts.get("number")+parts.get("suffix");
//		System.out.println(flno);
//		System.out.println(parts);
	}
}