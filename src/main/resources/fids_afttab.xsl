<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"
	exclude-result-prefixes="soap-env">

	<xsl:output method="xml" indent="yes" encoding="UTF-8"/>
	<!-- syncMode: DATASET = emit every mapped field; UPDATE = emit only when descendant @action is insert/update -->
	<xsl:param name="syncMode" select="'UPDATE'"/>
	<!-- adidMode: A = produce arrival record; D = produce departure record -->
	<xsl:param name="adidMode" select="'A'"/>

	<!-- ============================================================
	     HELPER TEMPLATES
	     ============================================================ -->

	<!-- Action-filtered emit. Skip when not DATASET and no descendant action=insert/update, or value is empty.
	     HOLD values are passed through so FieldInspector.replaceHoldWithEmpty can normalise them to " " in Java
	     (matches legacy setter-map behaviour).
	     The "node" parameter is supplied via <xsl:with-param><xsl:copy-of select="//path"/></xsl:with-param>, so
	     $node is a result-tree fragment whose immediate children are the copies of every matching element.
	     We pick the first child with non-empty text so multi-match XPaths (e.g. several pl_stand under pl_stand_list)
	     yield the same value the legacy Java code obtained via string(//path/text()[1]). -->
	<xsl:template name="getValue">
		<xsl:param name="tagName"/>
		<xsl:param name="node"/>
		<xsl:variable name="picked" select="$node/*[normalize-space() != ''][1]"/>
		<xsl:variable name="isSetting">
			<xsl:choose>
				<xsl:when test="$syncMode = 'DATASET'">true</xsl:when>
				<xsl:when test="$node//@action = 'insert' or $node//@action = 'update'">true</xsl:when>
				<xsl:otherwise>false</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:if test="$isSetting = 'true' and normalize-space($picked) != ''">
			<xsl:element name="{$tagName}">
				<xsl:value-of select="$picked"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>

	<xsl:template match="/">
		<FidsAfttab>

			<!-- ============================================================
			     ACTION-FILTERED FIXED IDENTITY FIELDS
			     Note: URNO/RKEY/SIBT/SOBT/FLNO/CSGN/FLTI/ALC2/ALC3 also have an
			     unconditional Java fallback in TranformFidsAfttab.applyFixedPaths
			     so they are still set on UPDATE messages without descendant
			     action=insert/update. Tracked in fieldsNotNull only when XSL
			     emits them here (matches original setter map behaviour).
			     ============================================================ -->

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'urno'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'">
							<xsl:copy-of select="//pl_arrival/pa_idseq"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="//pl_departure/pd_idseq"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'rkey'"/>
				<xsl:with-param name="node">
					<xsl:copy-of select="//pl_turn/pt_idseq"/>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'flno'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'">
							<xsl:copy-of select="//pl_arrival/pa_flightnumber"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="//pl_departure/pd_flightnumber"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'csgn'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'">
							<xsl:copy-of select="//pl_arrival/pa_callsign"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="//pl_departure/pd_callsign"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'flti'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'">
							<xsl:copy-of select="//pl_arrival/pa_rctt_countrytype/ref_countrytype/rctt_code"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="//pl_departure/pd_rctt_countrytype/ref_countrytype/rctt_code"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'alc2'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'">
							<xsl:copy-of select="//pl_arrival/pa_ral_airline/ref_airline/ral_2lc"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="//pl_departure/pd_ral_airline/ref_airline/ral_2lc"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'alc3'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'">
							<xsl:copy-of select="//pl_arrival/pa_ral_airline/ref_airline/ral_3lc"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="//pl_departure/pd_ral_airline/ref_airline/ral_3lc"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:if test="$adidMode = 'A'">
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'sibt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_sibt"/>
					</xsl:with-param>
				</xsl:call-template>
			</xsl:if>

			<xsl:if test="$adidMode = 'D'">
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'sobt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_sobt"/>
					</xsl:with-param>
				</xsl:call-template>
			</xsl:if>

			<!-- RTYP is computed in Java (TranformFidsAfttab.buildFlight) after fieldsNotNull capture,
			     so it does not pollute the change-tracking list. -->

			<!-- ============================================================
			     ACTION-FILTERED COMMON FIELDS (both A and D variants)
			     ============================================================ -->

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'jfno'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_codeshareflightnumbers"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_codeshareflightnumbers"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'ftyp'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_rfst_flightstatus"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_rfst_flightstatus"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'styp'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_rstc_refservicetypecode/ref_servicetypecode/rstc_ristc_iatacode"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_rstc_refservicetypecode/ref_servicetypecode/rstc_ristc_iatacode"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'act3'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_ract_aircrafttype/ref_aircrafttype/ract_iatatype"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_ract_aircrafttype/ref_aircrafttype/ract_iatatype"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'act5'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_ract_aircrafttype/ref_aircrafttype/ract_icaotype"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_ract_aircrafttype/ref_aircrafttype/ract_icaotype"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'acti'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_ract_aircrafttype/ref_aircrafttype/ract_internalcode"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_ract_aircrafttype/ref_aircrafttype/ract_internalcode"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'regn'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_registration"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_registration"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'ttyp'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_rnc_naturecode"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_rnc_naturecode"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'mtow'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_rac_aircraft/ref_aircraft/rac_mtow"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_rac_aircraft/ref_aircraft/rac_mtow"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'remp'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pa_rfst_refflightstatus/ref_flightstatus/rfst_code3l"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pd_rfst_refflightstatus/ref_flightstatus/rfst_code3l"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'rem1'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_opscomment"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_opscomment"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'tifd'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_bibt"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_bobt"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'bagn'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_arrivalloadstatistics_list/pl_arrivalloadstatistics/pals_baggagecount"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_departureloadstatistics_list/pl_departureloadstatistics/pdls_baggagecount"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'bags'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_arrivalloadstatistics_list/pl_arrivalloadstatistics/pals_baggagecount"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_departureloadstatistics_list/pl_departureloadstatistics/pdls_baggagecount"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'bagw'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_arrivalloadstatistics_list/pl_arrivalloadstatistics/pals_baggageweight"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_departureloadstatistics_list/pl_departureloadstatistics/pdls_baggageweight"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'cdat'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_createtime"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_createtime"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'cgot'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_arrivalloadstatistics_list/pl_arrivalloadstatistics/pals_cargoweight"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_departureloadstatistics_list/pl_departureloadstatistics/pdls_cargoweight"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'dcd1'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_delayreasons"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_delayreasons"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'dcd2'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_delayreasons"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_delayreasons"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'hdll'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_handlingagent_list/pl_handlingagent/pha_rha_handlingagent"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_handlingagent_list/pl_handlingagent/pha_rha_handlingagent"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'lstu'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_modtime"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_modtime"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'mail'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_arrivalloadstatistics_list/pl_arrivalloadstatistics/pals_mailweight"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_departureloadstatistics_list/pl_departureloadstatistics/pdls_mailweight"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'nose'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_seats"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_seats"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'nxti'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_nextinfotime"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_nextinfotime"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'paid'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_cashind"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_cashind"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'pax1'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_arrivalloadstatistics_list/pl_arrivalloadstatistics/pals_paxf"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_departureloadstatistics_list/pl_departureloadstatistics/pdls_paxf"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'pax2'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_arrivalloadstatistics_list/pl_arrivalloadstatistics/pals_paxc"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_departureloadstatistics_list/pl_departureloadstatistics/pdls_paxc"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'pax3'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pl_arrivalloadstatistics_list/pl_arrivalloadstatistics/pals_paxy"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pl_departureloadstatistics_list/pl_departureloadstatistics/pdls_paxy"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'paxf'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_transferpax"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_transferpax"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'paxi'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_transitpax"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_transitpax"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'paxt'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_totalpax"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_totalpax"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'ssrc'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/paa_ssrcode"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pad_ssrcode"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'trkn'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_externalflightnumber"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_externalflightnumber"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'useu'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_moduser"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_moduser"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'etot'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_arrival/pa_atotoutstation"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departure/pd_etot"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<!-- Belt: only b3ba/b3bs/b3ea/b3es are emitted here because the legacy
			     setter map tracked only those. b1/b2 belt fields, blt1-3, tmb1-3, and
			     bast are set unconditionally in applyBeltDetails (Java) so they are
			     intentionally excluded from fieldsNotNull. -->
			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'b3ba'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_baggagebelt[3]/pbb_beginactual"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departurebelt[3]/pdb_beginactual"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'b3bs'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_baggagebelt[3]/pbb_beginplan"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departurebelt[3]/pdb_beginplan"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'b3ea'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_baggagebelt[3]/pbb_endactual"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departurebelt[3]/pdb_endactual"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="getValue">
				<xsl:with-param name="tagName" select="'b3es'"/>
				<xsl:with-param name="node">
					<xsl:choose>
						<xsl:when test="$adidMode = 'A'"><xsl:copy-of select="//pl_baggagebelt[3]/pbb_endplan"/></xsl:when>
						<xsl:otherwise><xsl:copy-of select="//pl_departurebelt[3]/pdb_endplan"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>

			<!-- ============================================================
			     ARRIVAL-ONLY FIELDS (action-filtered)
			     ============================================================ -->
			<xsl:if test="$adidMode = 'A'">

				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'org3'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_rap_reforiginairport/ref_airport/rap_iata3lc"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'org4'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_rap_reforiginairport/ref_airport/rap_icao4lc"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'psta'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_rsta_stand"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'divr'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_diversiontime"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'raco'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pl_atcarrival_list/pl_atcarrival/paa_firstcontact"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'fpla'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_fplactivationtime"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'bbaa'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_onbridge"/>
					</xsl:with-param>
				</xsl:call-template>
				<!-- Legacy setter map registers pa_firt then pa_fnlt for setTmoa, so the
				     second entry (pa_fnlt) wins when both are non-empty. -->
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'tmoa'"/>
					<xsl:with-param name="node">
						<xsl:choose>
							<xsl:when test="//pl_arrival/pa_fnlt != ''">
								<xsl:copy-of select="//pl_arrival/pa_fnlt"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:copy-of select="//pl_arrival/pa_firt"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:with-param>
				</xsl:call-template>
				<!-- Legacy setter map registers pa_rrwy_runway then paa_runway, so the
				     second (paa_runway) wins when both are non-empty. -->
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'rwya'"/>
					<xsl:with-param name="node">
						<xsl:choose>
							<xsl:when test="//pl_arrival/pl_atcarrival_list/pl_atcarrival/paa_runway != ''">
								<xsl:copy-of select="//pl_arrival/pl_atcarrival_list/pl_atcarrival/paa_runway"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:copy-of select="//pl_arrival/pa_rrwy_runway"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'paba'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_stand/pst_beginactual"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'pabs'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_stand/pst_beginplan"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'paea'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_stand/pst_endactual"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'paes'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_stand/pst_endplan"/>
					</xsl:with-param>
				</xsl:call-template>

				<!-- Arrival gates tokens -->
				<xsl:variable name="gtaNode" select="//pa_arrivalgates"/>
				<xsl:variable name="gtaTokens" select="tokenize($gtaNode, ',')"/>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'gta1'"/>
					<xsl:with-param name="node">
						<field action="{$gtaNode/@action}"><xsl:value-of select="$gtaTokens[1]"/></field>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'gta2'"/>
					<xsl:with-param name="node">
						<field action="{$gtaNode/@action}"><xsl:value-of select="$gtaTokens[2]"/></field>
					</xsl:with-param>
				</xsl:call-template>

				<!-- Arrival time fields -->
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'aibt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_aibt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'aldt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_aldt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'eibt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_pibt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'eldt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_eldt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'exit'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_exit"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'star'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_star"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'tldt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_tldt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'vipa'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_vipind"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'trma'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pa_rtrm_terminal"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'ifra'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pl_atcarrival_list/pl_atcarrival/paa_flightrule"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'acor'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_arrival/pl_atcarrival/paa_firstcontact"/>
					</xsl:with-param>
				</xsl:call-template>
			</xsl:if>

			<!-- ============================================================
			     DEPARTURE-ONLY FIELDS (action-filtered)
			     ============================================================ -->
			<xsl:if test="$adidMode = 'D'">

				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'des3'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_rap_refdestinationairport/ref_airport/rap_iata3lc"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'des4'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_rap_refdestinationairport/ref_airport/rap_icao4lc"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'pstd'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_rsta_stand"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'ctot'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_ctot"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'deld'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_delay"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'fpld'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_fplactivationtime"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'bbfa'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_offbridge"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'fcal'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_secondcall"/>
					</xsl:with-param>
				</xsl:call-template>
				<!-- Legacy setter map registers pd_rrwy_runway then pad_runway, so the
				     second (pad_runway) wins when both are non-empty. -->
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'rwyd'"/>
					<xsl:with-param name="node">
						<xsl:choose>
							<xsl:when test="//pl_departure/pl_atc_departure/pad_runway != ''">
								<xsl:copy-of select="//pl_departure/pl_atc_departure/pad_runway"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:copy-of select="//pl_departure/pd_rrwy_runway"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'pdbs'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_stand/pst_beginactual"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'pdea'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_stand/pst_beginplan"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'pdes'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_stand/pst_endplan"/>
					</xsl:with-param>
				</xsl:call-template>

				<!-- Departure gates tokens -->
				<xsl:variable name="gtdNode" select="//pd_departuregates"/>
				<xsl:variable name="gtdTokens" select="tokenize($gtdNode, ',')"/>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'gtd1'"/>
					<xsl:with-param name="node">
						<field action="{$gtdNode/@action}"><xsl:value-of select="$gtdTokens[1]"/></field>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'gtd2'"/>
					<xsl:with-param name="node">
						<field action="{$gtdNode/@action}"><xsl:value-of select="$gtdTokens[2]"/></field>
					</xsl:with-param>
				</xsl:call-template>

				<!-- Departure time fields -->
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'aobt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_aobt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'atot'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_atot"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'eobt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_pobt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'exot'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_exot"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'aght'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_aght"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'tobc'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_tobtchanges"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'vipd'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_vipind"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'trmd'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_rtrm_terminal"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'ifrd'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_flightrule"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'acgt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_acgt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'aczt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_aczt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'aegt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_aegt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'aezt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_aezt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'ardt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_ardt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'arzt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_arzt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'asat'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_asat"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'asbt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_asbt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'asrt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_asrt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'eczt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_eczt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'eezt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_eezt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'erzt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_erzt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'rtot'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_rtot"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'stot'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_stot"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'tobt'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_tobt"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'tsat'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_tsat"/>
					</xsl:with-param>
				</xsl:call-template>
				<xsl:call-template name="getValue">
					<xsl:with-param name="tagName" select="'ttot'"/>
					<xsl:with-param name="node">
						<xsl:copy-of select="//pl_departure/pd_ttot"/>
					</xsl:with-param>
				</xsl:call-template>
			</xsl:if>

		</FidsAfttab>
	</xsl:template>
</xsl:stylesheet>
