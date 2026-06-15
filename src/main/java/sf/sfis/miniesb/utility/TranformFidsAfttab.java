package sf.sfis.miniesb.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltExecutable;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.model.FidsAirport;
import sf.sfis.miniesb.model.FidsCcatab;
import sf.sfis.miniesb.repository.FidsAirportRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranformFidsAfttab {

	// Loaded from the classpath (packaged inside the jar/war) — NOT a filesystem path,
	// so it resolves the same in dev and on the deployed server.
	private static final String XSL_RESOURCE = "/fids_afttab.xsl";
	private static final Pattern ISO_8601_Z = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
	private static final DateTimeFormatter YMD_HMS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	// Cached, thread-safe Saxon/Jackson objects. Compiling the XSL and building the
	// XmlMapper are expensive and the stylesheet never changes, so do it once instead
	// of per message. (The per-call Serializer and load30() transformer are not
	// thread-safe and are still created on each invocation.)
	private static final Processor SAXON_PROCESSOR = new Processor(false);
	private static volatile XsltExecutable xsltExecutable;
	private static final XmlMapper XML_MAPPER = buildXmlMapper();

	private final DateTimeFormatHelper dateTimeFormatHelper;
	private final FidsAirportRepository fidsAirportRepository;

	// ─── ENTRY POINT ───────────────────────────────────────────────────────
	public FidsAfttab convertPlTurntoAfftab(String xmlString, String actionType, String hopo, String adid) {
		try {
			Document doc = parseDocument(xmlString);
			XPath xpath = XPathFactory.newInstance().newXPath();

			String plTurn = (String) xpath.evaluate("//pl_turn", doc, XPathConstants.STRING);
			boolean hasArrival = !((String) xpath.evaluate("//pa_idseq", doc, XPathConstants.STRING)).isEmpty();
			boolean hasDeparture = !((String) xpath.evaluate("//pd_idseq", doc, XPathConstants.STRING)).isEmpty();

			if (plTurn.isEmpty()) {
				return buildCommonCounter(doc, hopo, adid);
			}
			return buildFlight(xmlString, doc, xpath, actionType, hopo, adid, hasArrival, hasDeparture);
		} catch (Exception e) {
			log.error("convertPlTurntoAfftab error: ", e);
			return null;
		}
	}

	// ─── COMMON COUNTER (XML without pl_turn) ──────────────────────────────
	private FidsAfttab buildCommonCounter(Document doc, String hopo, String adid) {
		if (!"D".equalsIgnoreCase(adid)) return null;
		FidsAfttab f = new FidsAfttab();
		f.setHopo(hopo);
		NodeList counterList = doc.getElementsByTagName("pl_desk");
		if (counterList.getLength() > 0) {
			f.setLstFidsCcatab(getCounters((Element) counterList.item(0), true));
		}
		return f;
	}

	// ─── FLIGHT MODE ───────────────────────────────────────────────────────
	private FidsAfttab buildFlight(String xmlString, Document doc, XPath xpath,
								   String actionType, String hopo, String adid,
								   boolean hasArrival, boolean hasDeparture) throws Exception {
		boolean isArrival = "A".equalsIgnoreCase(adid);
		if (isArrival && !hasArrival) return null;
		if (!isArrival && !hasDeparture) return null;

		Element flightElement = isArrival
			? (Element) doc.getElementsByTagName("pl_arrival").item(0)
			: (Element) doc.getElementsByTagName("pl_departure").item(0);
		String action = flightElement.getAttribute("action");

		if (!"DATASET".equalsIgnoreCase(actionType)
			&& !"update".equalsIgnoreCase(action) && !"insert".equalsIgnoreCase(action)) {
			return null;
		}

		// 1. XSL transform → action-filtered path mappings deserialised into FidsAfttab.
		FidsAfttab f = transformUsingSaxon(xmlString, actionType, adid);
		if (f == null) return null;
		f.setAdid(isArrival ? "A" : "D");
		f.setAction(action);

		// 2. Derived time fields (EIBT/ETAI/ETOA/LAND/AIRB/AXIT/AXOT/ONBL/OFBL/REMP)
		applyDerivedTimes(f);

		// 3. VIA from routing
		applyVial(f, xpath, doc, flightElement, hopo, isArrival, actionType);

		// 4. fieldsNotNull — captured before unconditional fixed paths so that
		//    URNO/RKEY/SIBT/SOBT/FLNO/CSGN/FLTI/ALC2/ALC3 are only tracked when
		//    they were set via the action-filtered XSL pass (matches legacy behaviour).
		if ("UPDATE".equalsIgnoreCase(actionType)) {
			f.setFieldsNotNull(FieldInspector.getNonNullFields(f));
			log.info("Fields " + (isArrival ? "arrival" : "departure") + " updated : " + f.getFieldsNotNull());
		}

		// 5. Unconditional fixed identity fields (set even when action attribute did not change)
		applyFixedPaths(f, doc, xpath, isArrival);

		// 6. Business derivations on fixed paths
		applyFlightNumber(f);
		applyFtyp(f);
		applyTrkn(f);

		// 7. Indexed nested structures
		applyBeltDetails(f, flightElement, isArrival);
		applyGateDetails(f, flightElement, isArrival);
		applyDelayReasons(f, flightElement);
		if (!isArrival) {
			f.setLstFidsCcatab(getCounters(flightElement, false));
		}

		// 8. Shared derivations (AURN, BAGS, DCD2, STOA, STOD, FLDA, DTD2, DOOA, DOOD)
		applySharedDerivations(f, isArrival);

		// 9. Airport lookup
		applyAirportLookup(f, hopo, isArrival);

		// 10. Outside per-adid: HOPO, HOLD→empty, RTYP, MTOW ceiling
		f.setHopo(hopo);
		FieldInspector.replaceHoldWithEmpty(f);
		if (hasArrival && hasDeparture) {
			f.setRtyp("J");
		} else if (hasArrival || hasDeparture) {
			f.setRtyp("S");
		}
		applyMtow(f);

		return f;
	}

	// ─── XSL TRANSFORMATION ────────────────────────────────────────────────
	public static FidsAfttab transformUsingSaxon(String xmlString, String actionType, String adid) throws Exception {
		StringWriter sw = new StringWriter();
		Serializer out = SAXON_PROCESSOR.newSerializer(sw);
		out.setOutputProperty(Serializer.Property.METHOD, "xml");
		out.setOutputProperty(Serializer.Property.INDENT, "yes");
		out.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");

		Xslt30Transformer trans = xsltExecutable().load30();
		Map<QName, XdmValue> params = new HashMap<>();
		params.put(new QName("syncMode"), new XdmAtomicValue(actionType));
		params.put(new QName("adidMode"), new XdmAtomicValue(adid));
		trans.setStylesheetParameters(params);
		trans.transform(new StreamSource(new StringReader(xmlString)), out);

		return XML_MAPPER.readValue(sw.toString(), FidsAfttab.class);
	}

	/** Compiles the stylesheet once (lazily) and reuses the immutable executable. */
	private static XsltExecutable xsltExecutable() throws Exception {
		XsltExecutable exec = xsltExecutable;
		if (exec == null) {
			synchronized (TranformFidsAfttab.class) {
				exec = xsltExecutable;
				if (exec == null) {
					try (InputStream is = TranformFidsAfttab.class.getResourceAsStream(XSL_RESOURCE)) {
						if (is == null) {
							throw new IllegalStateException("XSL resource not found on classpath: " + XSL_RESOURCE);
						}
						exec = SAXON_PROCESSOR.newXsltCompiler().compile(new StreamSource(is));
					}
					xsltExecutable = exec;
				}
			}
		}
		return exec;
	}

	/** Builds the date-aware XmlMapper once; ObjectMapper is thread-safe after setup. */
	private static XmlMapper buildXmlMapper() {
		XmlMapper xmlMapper = new XmlMapper();
		xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(String.class, new JsonDeserializer<String>() {
			@Override
			public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
				return convertDateStringIfNeeded(p.getText());
			}
		});
		xmlMapper.registerModule(module);
		return xmlMapper;
	}

	// ─── UNCONDITIONAL FIXED IDENTITY FIELDS ───────────────────────────────
	/**
	 * Sets URNO/RKEY/SIBT/SOBT/FLNO/CSGN/FLTI/ALC2/ALC3 without applying the
	 * descendant @action filter. These identity fields are required even when
	 * an UPDATE message did not flag them as insert/update, so downstream code
	 * (e.g. lookups by URNO, display formatting) keeps working.
	 */
	private void applyFixedPaths(FidsAfttab f, Document doc, XPath xpath, boolean isArrival) {
		String idseq = isArrival ? "/pl_arrival/pa_idseq" : "/pl_departure/pd_idseq";
		evaluateRawText(doc, xpath, idseq).ifPresent(v -> {
			try {
				f.setUrno(new BigDecimal(v));
			} catch (NumberFormatException e) {
				log.error(idseq + " is not BigDecimal.");
			}
		});
		evaluateRawText(doc, xpath, "/pl_turn/pt_idseq").ifPresent(v -> {
			try {
				f.setRkey(new BigDecimal(v));
			} catch (NumberFormatException e) {
				log.error("/pl_turn/pt_idseq is not BigDecimal.");
			}
		});

		if (isArrival) {
			evaluateRawText(doc, xpath, "/pl_arrival/pa_sibt").ifPresent(v -> f.setSibt(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_arrival/pa_flightnumber").ifPresent(v -> f.setFlno(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_arrival/pa_callsign").ifPresent(v -> f.setCsgn(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_arrival/pa_rctt_countrytype").ifPresent(v -> f.setFlti(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_arrival/pa_ral_airline/ref_airline/ral_2lc").ifPresent(v -> f.setAlc2(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_arrival/pa_ral_airline/ref_airline/ral_3lc").ifPresent(v -> f.setAlc3(convertDateStringIfNeeded(v)));
		} else {
			evaluateRawText(doc, xpath, "/pl_departure/pd_sobt").ifPresent(v -> f.setSobt(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_departure/pd_flightnumber").ifPresent(v -> f.setFlno(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_departure/pd_callsign").ifPresent(v -> f.setCsgn(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_departure/pd_rctt_countrytype").ifPresent(v -> f.setFlti(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_departure/pd_ral_airline/ref_airline/ral_2lc").ifPresent(v -> f.setAlc2(convertDateStringIfNeeded(v)));
			evaluateRawText(doc, xpath, "/pl_departure/pd_ral_airline/ref_airline/ral_3lc").ifPresent(v -> f.setAlc3(convertDateStringIfNeeded(v)));
		}
	}

	private Optional<String> evaluateRawText(Document doc, XPath xpath, String path) {
		try {
			String fixed = "string(//" + (path.startsWith("/") ? path.substring(1) : path);
			String textValue = (String) xpath.evaluate(fixed + "/text()[1])", doc, XPathConstants.STRING);
			if (textValue == null) return Optional.empty();
			String trimmed = textValue.trim();
			return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
		} catch (XPathExpressionException e) {
			log.error("XPath error for path: " + path, e);
			return Optional.empty();
		}
	}

	// ─── DERIVED BUSINESS LOGIC ────────────────────────────────────────────
	private void applyDerivedTimes(FidsAfttab f) {
		if (f.getAldt() != null) {
			f.setEibt(f.getAldt());
		} else {
			f.setRemp(f.getEibt() != null ? "    " : f.getRemp());
		}
		f.setEtai(f.getEibt());
		f.setEtoa(f.getEibt());
		f.setEtdi(f.getEobt());
		f.setEtod(f.getEobt());
		f.setLand(f.getAldt());
		f.setAirb(f.getAtot());
		f.setAxit(f.getExit());
		f.setAxot(f.getExot());
		f.setOnbl(f.getAibt());
		f.setOfbl(f.getAobt());
	}

	private void applyFlightNumber(FidsAfttab f) {
		String flno = f.getFlno();
		if (flno == null || flno.isEmpty()) return;

		Map<String, String> parts = parseFlightNumber(flno, f.getAlc3(), f.getAlc2());
		if (parts.isEmpty()) {
			f.setFlns("");
		} else {
			f.setFlno(parts.get("prefix") + parts.get("number") + parts.get("suffix"));
			f.setFlns(parts.get("suffix") != null ? parts.get("suffix") : "");
			f.setFltn(parts.get("number"));
		}
		if (f.getJfno() != null) {
			f.setJfno(f.getJfno().replace(",", " "));
		}
		if (f.getCsgn() != null && f.getFlns() != null) {
			f.setCsgn(f.getCsgn() + f.getFlns().trim());
		}
	}

	private void applyFtyp(FidsAfttab f) {
		String ftyp = f.getFtyp();
		if (ftyp == null) return;
		if (ftyp.equalsIgnoreCase("V")) {
			f.setFtyp("D");
		} else if (!ftyp.equalsIgnoreCase("X")) {
			f.setFtyp("O");
		}
	}

	private void applyTrkn(FidsAfttab f) {
		String trkn = f.getTrkn();
		if (trkn != null && !trkn.isEmpty() && trkn.length() > 4) {
			f.setTrkn(trkn.substring(2));
		}
	}

	private void applyMtow(FidsAfttab f) {
		String mtow = f.getMtow();
		if (mtow == null || mtow.trim().isEmpty()) return;
		try {
			int n = Integer.parseInt(mtow.trim());
			f.setMtow(Integer.toString((int) Math.ceil(n / 1000.0)));
		} catch (NumberFormatException ignored) {
		}
	}

	private void applyAirportLookup(FidsAfttab f, String hopo, boolean isArrival) {
		String apc4 = fidsAirportRepository.findById(hopo)
			.map(FidsAirport::getApc4)
			.orElse("");
		if (isArrival) {
			f.setDes3(hopo);
			f.setDes4(apc4);
		} else {
			f.setOrg3(hopo);
			f.setOrg4(apc4);
		}
	}

	private void applyVial(FidsAfttab f, XPath xpath, Document doc, Element flightElement,
						   String hopo, boolean isArrival, String actionType) {
		try {
			String route = (String) xpath.evaluate("//pt_routing", doc, XPathConstants.STRING);
			String[] airports = route.split("-");
			String via3 = "";
			for (int i = 0; i < airports.length; i++) {
				if (hopo.equals(airports[i])) {
					if (isArrival) {
						if (i > 1) via3 = airports[i - 1];
					} else {
						if (i < airports.length - 2) via3 = airports[i + 1];
					}
					break;
				}
			}

			NodeList routingNodes = flightElement.getElementsByTagName("pl_routing");
			for (int i = 0; i < routingNodes.getLength(); i++) {
				Node node = routingNodes.item(i);
				if (node == null) continue;
				String act = xpath.evaluate("prt_rap_refairport/ref_airport/rap_iata3lc/@action", node);
				boolean accept = "DATASET".equalsIgnoreCase(actionType)
					|| ("UPDATE".equalsIgnoreCase(actionType)
						&& ("update".equalsIgnoreCase(act) || "insert".equalsIgnoreCase(act)));
				if (!accept) continue;

				String iata = xpath.evaluate("prt_rap_refairport/ref_airport/rap_iata3lc", node);
				String icao = xpath.evaluate("prt_rap_refairport/ref_airport/rap_icao4lc", node);
				if (iata != null && !iata.isEmpty() && icao != null && !icao.isEmpty() && iata.equals(via3)) {
					f.setVia3(iata);
					f.setVia4(icao);
					f.setVial(getVial(iata, icao));
					f.setVian(routingNodes.getLength() > 0 ? "1" : "0");
					break;
				}
			}
		} catch (XPathExpressionException e) {
			log.error("applyVial error: ", e);
		}
	}

	private void applySharedDerivations(FidsAfttab f, boolean isArrival) {
		if (f.getRkey() != null) {
			f.setAurn(f.getRkey().toString());
		}
		f.setBags(f.getBagn());
		f.setDcd2(f.getDcd1());
		f.setStoa(f.getSibt());
		f.setStod(f.getSobt());
		f.setDtd2(f.getDtd1());

		// FLDA from SIBT (arrival) or SOBT (departure)
		String src = isArrival ? f.getSibt() : f.getSobt();
		if (src != null && src.length() >= 14 && !src.trim().isEmpty()) {
			try {
				f.setFlda(dateTimeFormatHelper.convertUTCToLocal(src).substring(0, 8));
			} catch (Exception e) {
				log.error("applyFlda parse error for value: " + src, e);
			}
		}

		// DOOA/DOOD + handle null cases
		if (f.getStoa() != null) {
			try {
				LocalDateTime ld = LocalDateTime.parse(f.getStoa(), YMD_HMS);
				OffsetDateTime utc = ld.atOffset(ZoneOffset.UTC);
				f.setDooa(Integer.toString(utc.getDayOfWeek().getValue()));
			} catch (Exception e) {
				log.error("DOOA parse error for STOA=" + f.getStoa(), e);
			}
		} else {
			f.setSibt(" ");
			f.setStoa(" ");
		}
		if (f.getStod() != null) {
			try {
				LocalDateTime ld = LocalDateTime.parse(f.getStod(), YMD_HMS);
				OffsetDateTime utc = ld.atOffset(ZoneOffset.UTC);
				f.setDood(Integer.toString(utc.getDayOfWeek().getValue()));
			} catch (Exception e) {
				log.error("DOOD parse error for STOD=" + f.getStod(), e);
			}
		} else {
			f.setSobt(" ");
			f.setStod(" ");
		}
	}

	// ─── INDEXED NESTED STRUCTURES ─────────────────────────────────────────
	private void applyBeltDetails(FidsAfttab f, Element flightElement, boolean isArrival) {
		NodeList nodes = flightElement.getElementsByTagName(isArrival ? "pl_baggagebelt" : "pl_departurebelt");
		String prefix = isArrival ? "pbb_" : "pdb_";
		String beltRefTag = prefix + (isArrival ? "rbb_baggagebelt" : "rdb_departurebelt");
		String tmbTag = prefix + (isArrival
			? "rbb_refbaggagebelt/ref_baggagebelt/rbb_rctt_countrytype"
			: "rdb_refbaggagebelt/ref_baggagebelt/rdb_rctt_countrytype");

		XPath xp = XPathFactory.newInstance().newXPath();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			if ("delete".equalsIgnoreCase(((Element) n).getAttribute("action"))) continue;
			try {
				String ba = convertDateStringIfNeeded(xp.evaluate(prefix + "beginactual", n));
				String bs = convertDateStringIfNeeded(xp.evaluate(prefix + "beginplan", n));
				String ea = convertDateStringIfNeeded(xp.evaluate(prefix + "endactual", n));
				String es = convertDateStringIfNeeded(xp.evaluate(prefix + "endplan", n));
				String blt = convertDateStringIfNeeded(xp.evaluate(beltRefTag, n));
				String tmb = convertDateStringIfNeeded(xp.evaluate(tmbTag, n));
				String bast = convertDateStringIfNeeded(xp.evaluate(prefix + "status", n));

				setDynamicValue(f, "b", i, "ba", ba);
				setDynamicValue(f, "b", i, "bs", bs);
				setDynamicValue(f, "b", i, "ea", ea);
				setDynamicValue(f, "b", i, "es", es);
				setDynamicValue(f, "bas", i, "", ba);
				setDynamicValue(f, "bao", i, "", bs);
				setDynamicValue(f, "bae", i, "", ea);
				setDynamicValue(f, "bac", i, "", es);
				setDynamicValue(f, "blt", i, "", blt);
				setDynamicValue(f, "tmb", i, "", tmb);
				setDynamicValue(f, "baz", i, "", blt);
				f.setBast(bast);
				f.setB1ba(f.getAibt());
			} catch (XPathExpressionException e) {
				log.error("applyBeltDetails error: ", e);
			}
		}
	}

	private void applyGateDetails(FidsAfttab f, Element flightElement, boolean isArrival) {
		NodeList gatesNodes = flightElement.getElementsByTagName(isArrival ? "pa_arrivalgates" : "pd_departuregates");
		String gts = gatesNodes.getLength() > 0 ? gatesNodes.item(0).getTextContent() : "";
		if (gts != null && !gts.isEmpty()) {
			List<String> lstGates = new ArrayList<>(new LinkedHashSet<>(Arrays.asList(gts.split(","))));
			String fieldPrefix = "gt" + (isArrival ? "a" : "d");
			for (int i = 0; i < lstGates.size(); i++) {
				setDynamicValue(f, fieldPrefix, i, "", lstGates.get(i));
			}
		}

		NodeList gateNodes = flightElement.getElementsByTagName(isArrival ? "pl_arrivalgate" : "pl_departuregate");
		String prefix = isArrival ? "pag_" : "pdg_";
		String fieldPrefix = "g" + (isArrival ? "a" : "d");
		XPath xp = XPathFactory.newInstance().newXPath();
		for (int i = 0; i < gateNodes.getLength(); i++) {
			Node n = gateNodes.item(i);
			try {
				setDynamicValue(f, fieldPrefix, i, "b", convertDateStringIfNeeded(xp.evaluate(prefix + "beginplan", n)));
				setDynamicValue(f, fieldPrefix, i, "x", convertDateStringIfNeeded(xp.evaluate(prefix + "beginactual", n)));
				setDynamicValue(f, fieldPrefix, i, "y", convertDateStringIfNeeded(xp.evaluate(prefix + "endactual", n)));
				setDynamicValue(f, fieldPrefix, i, "e", convertDateStringIfNeeded(xp.evaluate(prefix + "endplan", n)));
			} catch (XPathExpressionException e) {
				log.error("applyGateDetails error: ", e);
			}
		}
	}

	private void applyDelayReasons(FidsAfttab f, Element flightElement) {
		NodeList nodes = flightElement.getElementsByTagName("pl_delayreason");
		XPath xp = XPathFactory.newInstance().newXPath();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			try {
				String dcd = convertDateStringIfNeeded(xp.evaluate("pdlr_rdlr_delayreason", n));
				String dela = convertDateStringIfNeeded(xp.evaluate("pdlr_delay", n));
				setDynamicValue(f, "dcd", i, "", dcd);
				setDynamicValue(f, "dtd", i, "", dela);
				f.setDela(dela);
			} catch (XPathExpressionException e) {
				log.error("applyDelayReasons error: ", e);
			}
		}
	}

	private List<FidsCcatab> getCounters(Element element, boolean isCommon) {
		List<FidsCcatab> lst = new ArrayList<>();
		NodeList counterNodes = isCommon
			? new ImplementNodeList(Arrays.asList((Node) element))
			: element.getElementsByTagName("pl_desk");

		XPath xp = XPathFactory.newInstance().newXPath();
		for (int i = 0; i < counterNodes.getLength(); i++) {
			FidsCcatab c = new FidsCcatab();
			Node n = counterNodes.item(i);
			try {
				String ckic = convertDateStringIfNeeded(xp.evaluate("pdk_rcnt_counter", n));
				String ckbs = convertDateStringIfNeeded(xp.evaluate("pdk_beginplan", n));
				String ckes = convertDateStringIfNeeded(xp.evaluate("pdk_endplan", n));
				String ckba = convertDateStringIfNeeded(xp.evaluate("pdk_beginactual", n));
				String ckea = convertDateStringIfNeeded(xp.evaluate("pdk_endactual", n));
				if ("HOLD".equals(ckic)) continue;

				if (!isCommon) {
					String cnts = element.getElementsByTagName("pd_counters").item(0).getTextContent();
					Set<String> counters = !cnts.isEmpty()
						? new LinkedHashSet<>(Arrays.asList(cnts.split(",")))
						: new LinkedHashSet<>();
					String ctyp = counters.contains(ckic) ? " " : "C";
					if (ctyp.equals(" ")) {
						c.setCkic(ckic);
						c.setCtyp(ctyp);
						c.setCkbs(ckbs);
						c.setCkes(ckes);
						c.setCkba(ckba);
						c.setCkea(ckea);
						lst.add(c);
					}
				} else {
					String flnu = convertDateStringIfNeeded(xp.evaluate("pdk_idseq", n));
					String flno = convertDateStringIfNeeded(xp.evaluate(
						"pdk_rcnt_refmastercci/ref_counter/rcnt_ral_airline/ref_airline/ral_2lc", n));
					c.setFlnu(new BigDecimal(flnu));
					c.setFlno(String.format("%-9s", flno));
					c.setCkic(ckic);
					c.setCtyp("C");
					c.setCkbs(ckbs);
					c.setCkes(ckes);
					c.setCkba(ckba);
					c.setCkea(ckea);
					lst.add(c);
				}
			} catch (XPathExpressionException e) {
				log.error("getCounters error: ", e);
			}
		}
		return lst;
	}

	// ─── HELPERS ───────────────────────────────────────────────────────────
	private Document parseDocument(String xmlString) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xmlString)));
	}

	private static String convertDateStringIfNeeded(String input) {
		if (input == null) return null;
		return ISO_8601_Z.matcher(input).matches() ? input.replaceAll("[-:TZ]", "") : input;
	}

	public String getVial(String via3, String via4) {
		String pad = "              ";
		return " " + via3 + via4 + pad + pad + pad + pad + pad + pad + pad + pad;
	}

	public static Map<String, String> parseFlightNumber(String flightNumber, String alc3, String alc2) {
		String prefix = "";
		String number = "";
		String suffix = "";

		if (alc3 != null && !alc3.isEmpty() && flightNumber.startsWith(alc3)) {
			prefix = alc3;
			Matcher m = Pattern.compile("^(\\d{1,4})([A-Z]?)$").matcher(flightNumber.substring(alc3.length()));
			if (m.find()) {
				number = String.format("%03d", Integer.parseInt(m.group(1)));
				suffix = m.group(2);
			}
		} else if (alc2 != null && !alc2.isEmpty() && flightNumber.startsWith(alc2)) {
			prefix = alc2;
			Matcher m = Pattern.compile("^(\\d{1,4})([A-Z]?)$").matcher(flightNumber.substring(alc2.length()));
			if (m.find()) {
				if (number.length() < 3) {
					number = String.format("%03d", Integer.parseInt(m.group(1)));
				}
				suffix = m.group(2);
			}
		} else {
			return Collections.emptyMap();
		}
		if (prefix.length() == 2) prefix = prefix + " ";
		number = number + (number.length() == 4 ? " " : "  ");

		Map<String, String> map = new HashMap<>();
		map.put("prefix", prefix);
		map.put("number", number);
		map.put("suffix", suffix);
		return map;
	}

	public void setDynamicValue(FidsAfttab obj, String fieldPrefix, int index, String fieldSuffix, String value) {
		try {
			String fieldName = fieldPrefix + (index + 1) + fieldSuffix;
			Field field = FidsAfttab.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(obj, value);
		} catch (Exception e) {
			log.error("setDynamicValue: ", e);
		}
	}
}
