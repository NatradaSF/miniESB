<?xml version="1.0" encoding="UTF-8"?>
<!--
  แปลง inbound MSG (UFIS) → AODB Envelope โดยตรง (XML→XML) ทดแทนการ build JAXB ใน
  ESBRequestService (setFlight / setVdgs / setBhs / setBulkData). ผลลัพธ์ต้อง "เหมือน"
  โค้ด JAXB เดิมแบบ semantic (พิสูจน์ด้วย MsgToAodbXsltTest เทียบ golden ของโค้ดปัจจุบัน).

  หมายเหตุลำดับ element: JAXB marshal ลูก pl_departure/pl_arrival แบบ ALPHABETICAL ตามชื่อ
  element → XSL จึง emit เรียง alphabetical เช่นกัน (แต่ละ field ครอบ xsl:if ตามการมีอยู่).

  date transform (ให้ตรงกับ Java เดิม):
    - control timestamp : 14 หลัก UTC → +7h (Bangkok) → yyyy-MM-ddTHH:mm:ss.000  (getLocalDate)
    - flight/vdgs time  : 14 หลัก → yyyy-MM-ddTHH:mm:ssZ                          (getAodbDate)
    - belt time         : 12 หลัก → yyyy-MM-ddTHH:mm:00Z                          (getAodbDurationMinute)

  parameter:
    - currentDate : 14 หลัก (yyyyMMddHHmmss) แทน getCurrentDate() ของ Java (ส่งเข้ามาเพื่อ
      ให้ deterministic + testable) ใช้กับ field airb/land ที่ set SOBT/SIBT = วันปัจจุบัน 00:00
-->
<xsl:stylesheet version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:aodb="urn:com.tsystems.ac.aodb"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs">

  <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
  <xsl:strip-space elements="*"/>

  <xsl:param name="currentDate" select="''"/>

  <!-- ═══ date helpers ═══ -->

  <!-- control timestamp: 14 หลัก (UTC) → +7h → yyyy-MM-ddTHH:mm:ss.000 (ไม่มี Z) -->
  <xsl:template name="fmtControlTs">
    <xsl:param name="v"/>
    <xsl:variable name="t" select="normalize-space($v)"/>
    <xsl:variable name="iso" select="concat(substring($t,1,4),'-',substring($t,5,2),'-',substring($t,7,2),
                                            'T',substring($t,9,2),':',substring($t,11,2),':',substring($t,13,2))"/>
    <xsl:variable name="dt" select="xs:dateTime($iso) + xs:dayTimeDuration('PT7H')"/>
    <xsl:value-of select="format-dateTime($dt, '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]')"/>
  </xsl:template>

  <!-- flight/vdgs time: 14 หลัก → yyyy-MM-ddTHH:mm:ssZ (ว่างถ้า input ว่าง) -->
  <xsl:template name="fmtDate">
    <xsl:param name="v"/>
    <xsl:variable name="t" select="normalize-space($v)"/>
    <xsl:if test="$t != ''">
      <xsl:value-of select="concat(substring($t,1,4),'-',substring($t,5,2),'-',substring($t,7,2),
                                   'T',substring($t,9,2),':',substring($t,11,2),':',substring($t,13,2),'Z')"/>
    </xsl:if>
  </xsl:template>

  <!-- belt time: 12 หลัก → yyyy-MM-ddTHH:mm:00Z (ว่างถ้า input ว่าง) -->
  <xsl:template name="fmtBeltDate">
    <xsl:param name="v"/>
    <xsl:variable name="t" select="normalize-space($v)"/>
    <xsl:if test="$t != ''">
      <xsl:value-of select="concat(substring($t,1,4),'-',substring($t,5,2),'-',substring($t,7,2),
                                   'T',substring($t,9,2),':',substring($t,11,2),':00Z')"/>
    </xsl:if>
  </xsl:template>

  <!-- emit <name>fmtDate(v)</name> เฉพาะเมื่อ element ต้นทางมีอยู่ (present = ไม่ null ฝั่ง JAXB) -->
  <xsl:template name="dateField">
    <xsl:param name="name"/><xsl:param name="v"/>
    <xsl:if test="$v">
      <xsl:element name="{$name}"><xsl:call-template name="fmtDate"><xsl:with-param name="v" select="$v"/></xsl:call-template></xsl:element>
    </xsl:if>
  </xsl:template>

  <!-- emit <name>normalize? ...</name> (priostring/string ตรงๆ) เฉพาะเมื่อ element มีอยู่ -->
  <xsl:template name="strField">
    <xsl:param name="name"/><xsl:param name="v"/>
    <xsl:if test="$v">
      <xsl:element name="{$name}"><xsl:value-of select="$v"/></xsl:element>
    </xsl:if>
  </xsl:template>

  <!-- ═══ belt (bhs) ═══ -->
  <xsl:template name="depBelt">
    <xsl:param name="baz"/><xsl:param name="bao"/><xsl:param name="bac"/>
    <xsl:variable name="b"><xsl:call-template name="fmtBeltDate"><xsl:with-param name="v" select="$bao"/></xsl:call-template></xsl:variable>
    <xsl:variable name="e"><xsl:call-template name="fmtBeltDate"><xsl:with-param name="v" select="$bac"/></xsl:call-template></xsl:variable>
    <pl_departurebelt>
      <xsl:if test="$b != ''"><pdb_beginactual><xsl:value-of select="$b"/></pdb_beginactual></xsl:if>
      <xsl:if test="$e != ''"><pdb_endactual><xsl:value-of select="$e"/></pdb_endactual></xsl:if>
      <pdb_rdb_departurebelt><xsl:value-of select="normalize-space($baz)"/></pdb_rdb_departurebelt>
    </pl_departurebelt>
  </xsl:template>

  <xsl:template name="arrBelt">
    <xsl:param name="baz"/><xsl:param name="bao"/><xsl:param name="bac"/>
    <xsl:variable name="b"><xsl:call-template name="fmtBeltDate"><xsl:with-param name="v" select="$bao"/></xsl:call-template></xsl:variable>
    <xsl:variable name="e"><xsl:call-template name="fmtBeltDate"><xsl:with-param name="v" select="$bac"/></xsl:call-template></xsl:variable>
    <pl_baggagebelt>
      <xsl:if test="$b != ''"><pbb_beginactual><xsl:value-of select="$b"/></pbb_beginactual></xsl:if>
      <xsl:if test="$e != ''"><pbb_endactual><xsl:value-of select="$e"/></pbb_endactual></xsl:if>
      <pbb_rbb_baggagebelt><xsl:value-of select="normalize-space($baz)"/></pbb_rbb_baggagebelt>
    </pl_baggagebelt>
  </xsl:template>

  <!-- ═══ flight body (setFlight) — emit เรียง alphabetical ตาม JAXB ═══ -->
  <!-- $g = INFOBJ_GENERIC, $f = INFOBJ_FLIGHT. STOA/STOD/CSGN ฝั่ง Java inject จาก generic
       (stdt/csgn) เสมอ → ที่นี่ pd_callsign/pd_sobt (dep) และ pa_callsign/pa_sibt (arr) emit เสมอ -->

  <xsl:template name="flightDeparture">
    <xsl:param name="g"/><xsl:param name="f"/>
    <pl_departure>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_acgt'"/><xsl:with-param name="v" select="$f/ACGT"/></xsl:call-template>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_aegt'"/><xsl:with-param name="v" select="$f/AEGT"/></xsl:call-template>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_asat'"/><xsl:with-param name="v" select="$f/ASAT"/></xsl:call-template>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_asbt'"/><xsl:with-param name="v" select="$f/ASBT"/></xsl:call-template>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_asrt'"/><xsl:with-param name="v" select="$f/ASRT"/></xsl:call-template>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_atot'"/><xsl:with-param name="v" select="$f/AIRB"/></xsl:call-template>
      <!-- pd_callsign: emit เฉพาะเมื่อ generic มี CSGN (JAXB: CSGN=null → ตกจาก nonNullFields) -->
      <xsl:if test="$g/CSGN">
        <pd_callsign><xsl:value-of select="$g/CSGN"/></pd_callsign>
      </xsl:if>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_ctot'"/><xsl:with-param name="v" select="$f/CTOT"/></xsl:call-template>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_doorclosetime'"/><xsl:with-param name="v" select="$f/ARDT"/></xsl:call-template>
      <xsl:call-template name="strField"><xsl:with-param name="name" select="'pd_flightrule'"/><xsl:with-param name="v" select="$f/IFRD"/></xsl:call-template>
      <xsl:call-template name="strField"><xsl:with-param name="name" select="'pd_rrwy_runway'"/><xsl:with-param name="v" select="$f/RWYD"/></xsl:call-template>
      <!-- pd_sobt: emit เฉพาะเมื่อมี STDT (=stod) หรือ AIRB (=currentDate) — JAXB: ไม่มีทั้งคู่ → ไม่ emit -->
      <xsl:if test="$g/STDT or $f/AIRB">
        <pd_sobt>
          <xsl:choose>
            <xsl:when test="$f/AIRB">
              <xsl:call-template name="fmtDate"><xsl:with-param name="v" select="concat(substring($currentDate,1,8),'000000')"/></xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="fmtDate"><xsl:with-param name="v" select="$g/STDT"/></xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </pd_sobt>
      </xsl:if>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_tobt'"/><xsl:with-param name="v" select="$f/TOBT"/></xsl:call-template>
      <!-- tsat: emit เฉพาะเมื่อไม่ว่าง (ตรงกับ guard ใน Java) -->
      <xsl:if test="$f/TSAT and normalize-space($f/TSAT) != ''">
        <pd_tsat><xsl:call-template name="fmtDate"><xsl:with-param name="v" select="$f/TSAT"/></xsl:call-template></pd_tsat>
      </xsl:if>
    </pl_departure>
  </xsl:template>

  <xsl:template name="flightArrival">
    <xsl:param name="g"/><xsl:param name="f"/>
    <pl_arrival>
      <!-- pa_aldt: land → aldt ; และ land ทำให้ sibt = currentDate -->
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pa_aldt'"/><xsl:with-param name="v" select="$f/LAND"/></xsl:call-template>
      <xsl:if test="$g/CSGN">
        <pa_callsign><xsl:value-of select="$g/CSGN"/></pa_callsign>
      </xsl:if>
      <xsl:call-template name="strField"><xsl:with-param name="name" select="'pa_flightrule'"/><xsl:with-param name="v" select="$f/IFRA"/></xsl:call-template>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pa_fnlt'"/><xsl:with-param name="v" select="$f/TMOA"/></xsl:call-template>
      <xsl:call-template name="strField"><xsl:with-param name="name" select="'pa_rrwy_runway'"/><xsl:with-param name="v" select="$f/RWYA"/></xsl:call-template>
      <!-- pa_sibt: emit เฉพาะเมื่อมี STDT (=stoa) หรือ LAND (=currentDate) -->
      <xsl:if test="$g/STDT or $f/LAND">
        <pa_sibt>
          <xsl:choose>
            <xsl:when test="$f/LAND">
              <xsl:call-template name="fmtDate"><xsl:with-param name="v" select="concat(substring($currentDate,1,8),'000000')"/></xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="fmtDate"><xsl:with-param name="v" select="$g/STDT"/></xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </pa_sibt>
      </xsl:if>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pa_tldt'"/><xsl:with-param name="v" select="$f/TLDT"/></xsl:call-template>
    </pl_arrival>
  </xsl:template>

  <!-- ═══ vdgs body (setVdgs) ═══ -->
  <!-- order (จาก golden): aobt/aibt, flightnumber, rsta_stand, sobt/sibt, ract_aircrafttype (ท้ายสุด)
       pd_flightnumber = generic FLNO (ไม่ trim), pd_sobt/pa_sibt = generic STDT เสมอ -->
  <xsl:template name="vdgsDeparture">
    <xsl:param name="g"/><xsl:param name="v"/>
    <pl_departure>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pd_aobt'"/><xsl:with-param name="v" select="$v/OFBL"/></xsl:call-template>
      <pd_flightnumber><xsl:value-of select="$g/FLNO"/></pd_flightnumber>
      <xsl:call-template name="strField"><xsl:with-param name="name" select="'pd_rsta_stand'"/><xsl:with-param name="v" select="$v/PSTD"/></xsl:call-template>
      <pd_sobt><xsl:call-template name="fmtDate"><xsl:with-param name="v" select="$g/STDT"/></xsl:call-template></pd_sobt>
      <xsl:if test="$v/ACT5">
        <pd_ract_aircrafttype><xsl:value-of select="$v/ACT5"/><ref_aircrafttype><ract_icaotype><xsl:value-of select="$v/ACT5"/></ract_icaotype></ref_aircrafttype></pd_ract_aircrafttype>
      </xsl:if>
    </pl_departure>
  </xsl:template>

  <xsl:template name="vdgsArrival">
    <xsl:param name="g"/><xsl:param name="v"/>
    <pl_arrival>
      <xsl:call-template name="dateField"><xsl:with-param name="name" select="'pa_aibt'"/><xsl:with-param name="v" select="$v/ONBL"/></xsl:call-template>
      <pa_flightnumber><xsl:value-of select="$g/FLNO"/></pa_flightnumber>
      <xsl:call-template name="strField"><xsl:with-param name="name" select="'pa_rsta_stand'"/><xsl:with-param name="v" select="$v/PSTA"/></xsl:call-template>
      <pa_sibt><xsl:call-template name="fmtDate"><xsl:with-param name="v" select="$g/STDT"/></xsl:call-template></pa_sibt>
      <xsl:if test="$v/ACT5">
        <pa_ract_aircrafttype><xsl:value-of select="$v/ACT5"/><ref_aircrafttype><ract_icaotype><xsl:value-of select="$v/ACT5"/></ract_icaotype></ref_aircrafttype></pa_ract_aircrafttype>
      </xsl:if>
    </pl_arrival>
  </xsl:template>

  <!-- ═══ root ═══ -->
  <xsl:template match="/MSG">
    <xsl:variable name="g"    select="MSGSTREAM_IN/INFOBJ_GENERIC"/>
    <xsl:variable name="obj"  select="MSGSTREAM_IN/MSGOBJECTS"/>
    <xsl:variable name="adid" select="$g/ADID"/>
    <soap-env:Envelope>
      <soap-env:Header>
        <aodb:control>
          <aodb:message-version>1.4</aodb:message-version>
          <aodb:message-type>UPDATE</aodb:message-type>
          <aodb:confirm-type>NACK</aodb:confirm-type>
          <aodb:originator><xsl:value-of select="$g/MESSAGEORIGIN"/></aodb:originator>
          <aodb:timestamp><xsl:call-template name="fmtControlTs"><xsl:with-param name="v" select="$g/TIMESTAMP"/></xsl:call-template></aodb:timestamp>
          <aodb:sender>IFIMS</aodb:sender>
          <aodb:receiver>AOS</aodb:receiver>
          <aodb:station><xsl:value-of select="$g/HOPO"/></aodb:station>
        </aodb:control>
      </soap-env:Header>
      <soap-env:Body>
        <xsl:choose>
          <!-- ── BULKDATA (AFTN/SITA) → if_adexpmessage (setBulkData) ── -->
          <xsl:when test="$obj/BULKDATA">
            <if_adexpmessage>
              <iam_originalmessage>
                <xsl:choose>
                  <xsl:when test="$g/MESSAGEORIGIN = 'AFTN'"><xsl:value-of select="$obj/BULKDATA/AFTN/CONTENT"/></xsl:when>
                  <xsl:otherwise><xsl:value-of select="$obj/BULKDATA/SITA/CONTENT"/></xsl:otherwise>
                </xsl:choose>
              </iam_originalmessage>
            </if_adexpmessage>
          </xsl:when>
          <!-- ── INFOBJ_FLIGHT → pl_turn (setFlight) ── -->
          <xsl:when test="$obj/INFOBJ_FLIGHT">
            <pl_turn>
              <xsl:choose>
                <xsl:when test="$adid = 'D'">
                  <pt_pd_departure><xsl:call-template name="flightDeparture"><xsl:with-param name="g" select="$g"/><xsl:with-param name="f" select="$obj/INFOBJ_FLIGHT"/></xsl:call-template></pt_pd_departure>
                </xsl:when>
                <xsl:otherwise>
                  <pt_pa_arrival><xsl:call-template name="flightArrival"><xsl:with-param name="g" select="$g"/><xsl:with-param name="f" select="$obj/INFOBJ_FLIGHT"/></xsl:call-template></pt_pa_arrival>
                </xsl:otherwise>
              </xsl:choose>
            </pl_turn>
          </xsl:when>
          <!-- ── INFOBJ_VDGS → pl_turn (setVdgs) ── -->
          <xsl:when test="$obj/INFOBJ_VDGS">
            <pl_turn>
              <xsl:choose>
                <xsl:when test="$adid = 'D'">
                  <pt_pd_departure><xsl:call-template name="vdgsDeparture"><xsl:with-param name="g" select="$g"/><xsl:with-param name="v" select="$obj/INFOBJ_VDGS/VDGSDEP"/></xsl:call-template></pt_pd_departure>
                </xsl:when>
                <xsl:otherwise>
                  <pt_pa_arrival><xsl:call-template name="vdgsArrival"><xsl:with-param name="g" select="$g"/><xsl:with-param name="v" select="$obj/INFOBJ_VDGS/VDGSARR"/></xsl:call-template></pt_pa_arrival>
                </xsl:otherwise>
              </xsl:choose>
            </pl_turn>
          </xsl:when>
          <!-- ── INFOBJ_MUINFO → pl_turn (setBhs) ── -->
          <xsl:when test="$obj/INFOBJ_MUINFO">
            <xsl:variable name="mu" select="$obj/INFOBJ_MUINFO"/>
            <pl_turn>
              <xsl:choose>
                <xsl:when test="$adid = 'D'">
                  <pt_pd_departure><pl_departure><pl_departurebelt_list>
                    <xsl:call-template name="depBelt"><xsl:with-param name="baz" select="$mu/BAZ1"/><xsl:with-param name="bao" select="$mu/BAO1"/><xsl:with-param name="bac" select="$mu/BAC1"/></xsl:call-template>
                    <xsl:call-template name="depBelt"><xsl:with-param name="baz" select="$mu/BAZ4"/><xsl:with-param name="bao" select="$mu/BAO4"/><xsl:with-param name="bac" select="$mu/BAC4"/></xsl:call-template>
                  </pl_departurebelt_list></pl_departure></pt_pd_departure>
                </xsl:when>
                <xsl:otherwise>
                  <pt_pa_arrival><pl_arrival><pl_baggagebelt_list>
                    <xsl:call-template name="arrBelt"><xsl:with-param name="baz" select="$mu/BAZ1"/><xsl:with-param name="bao" select="$mu/BAO1"/><xsl:with-param name="bac" select="$mu/BAC1"/></xsl:call-template>
                    <xsl:call-template name="arrBelt"><xsl:with-param name="baz" select="$mu/BAZ4"/><xsl:with-param name="bao" select="$mu/BAO4"/><xsl:with-param name="bac" select="$mu/BAC4"/></xsl:call-template>
                  </pl_baggagebelt_list></pl_arrival></pt_pa_arrival>
                </xsl:otherwise>
              </xsl:choose>
            </pl_turn>
          </xsl:when>
        </xsl:choose>
      </soap-env:Body>
    </soap-env:Envelope>
  </xsl:template>

</xsl:stylesheet>
