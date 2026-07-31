package sf.sfis.ifimsconnect.esb.figurein;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

/**
 * ชนิดข้อมูลสำหรับ element {@code INFOBJ_FIGURE} (WMFIGURE load figures) ที่รับจาก
 * Queue UFIS_FIGURE_IN. คลาสนี้เป็นเพียง "ตัวเก็บชนิด" (type holder) — ตัว
 * {@link InfobjFigure} ถูกอ้างอิงจาก {@code MSG.MSGSTREAMIN.MSGOBJECTS.infobjfigure}
 * จึงอ่านได้ผ่าน {@code msg.getMSGSTREAMIN().getMSGOBJECTS().getINFOBJFIGURE()}
 * แบบเดียวกับ BULKDATA / INFOBJ_VDGS / INFOBJ_MUINFO ตัวอื่น.
 *
 * ค่าทั้งหมดเก็บเป็น {@link String} โดยตั้งใจ ("รับค่าดิบก่อน") เพื่อไม่ให้ parse ล้ม
 * จาก field ว่าง/รูปแบบไม่ตรง และเปิดทางให้ object หลังบ้านค่อย map/แปลงชนิดภายหลัง.
 */
public final class FigureMessageIn {

    private FigureMessageIn() {
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InfobjFigure {
        @XmlElement(name = "figure")
        private Figure figure;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Figure {
        @XmlElement(name = "SENTDATE")             private String sentdate;
        @XmlElement(name = "SENTBY")               private String sentby;
        @XmlElement(name = "AIRPORT")              private String airport;
        @XmlElement(name = "FLIGHTNUMBER")         private String flightnumber;
        @XmlElement(name = "FLIGHTDATE")           private String flightdate;
        @XmlElement(name = "REGISTRATION")         private String registration;
        @XmlElement(name = "ADINDICATOR")          private String adindicator;
        @XmlElement(name = "INTDOMINDICATOR")      private String intdomindicator;
        @XmlElement(name = "AIRLINECODE3")         private String airlinecode3;
        @XmlElement(name = "FLIGHTNATURE")         private String flightnature;
        @XmlElement(name = "PAXDISEMBARKINTL")     private String paxdisembarkintl;
        @XmlElement(name = "PAXDISEMBARKDOM")      private String paxdisembarkdom;
        @XmlElement(name = "PAXTRANSFERREDINTL")   private String paxtransferredintl;
        @XmlElement(name = "PAXTRANSFERREDDOM")    private String paxtransferreddom;
        @XmlElement(name = "PAXTRANSIT1")          private String paxtransit1;
        @XmlElement(name = "PAXFIRSTCLASSINTL")    private String paxfirstclassintl;
        @XmlElement(name = "PAXFIRSTCLASSDOM")     private String paxfirstclassdom;
        @XmlElement(name = "PAXBUSINESSCLASSINTL") private String paxbusinessclassintl;
        @XmlElement(name = "PAXBUSINESSCLASSDOM")  private String paxbusinessclassdom;
        @XmlElement(name = "PAXINFANTSINTL")       private String paxinfantsintl;
        @XmlElement(name = "PAXINFANTSDOM")        private String paxinfantsdom;
        @XmlElement(name = "FREIGHTINOUTBOUNDINTL") private String freightinoutboundintl;
        @XmlElement(name = "FREIGHTINOUTBOUNDDOM")  private String freightinoutbounddom;
        @XmlElement(name = "FREIGHTTRANSIT1")      private String freighttransit1;
        @XmlElement(name = "MAILINOUTBOUNDINTL")   private String mailinoutboundintl;
        @XmlElement(name = "MAILINOUTBOUNDDOM")    private String mailinoutbounddom;
        @XmlElement(name = "MAILTRANSIT1")         private String mailtransit1;
        @XmlElement(name = "PILOT")                private String pilot;
        @XmlElement(name = "CREW")                 private String crew;
        @XmlElement(name = "PAXFORBIDDENINTL")     private String paxforbiddenintl;
        @XmlElement(name = "PAXFORBIDDENDOM")      private String paxforbiddendom;
        @XmlElement(name = "PAXVVIPINTL")          private String paxvvipintl;
        @XmlElement(name = "PAXVVIPDOM")           private String paxvvipdom;
        @XmlElement(name = "PAXOTHERSINTL")        private String paxothersintl;
        @XmlElement(name = "PAXOTHERSDOM")         private String paxothersdom;
        @XmlElement(name = "PAXRETURNINTL")        private String paxreturnintl;
        @XmlElement(name = "PAXRETURNDOM")         private String paxreturndom;
        @XmlElement(name = "PAXCIQ")               private String paxciq;
        @XmlElement(name = "PAXTRANSFERCIQ")       private String paxtransferciq;
        @XmlElement(name = "PAXLASTPORTCIQ")       private String paxlastportciq;

        /** รายการ leg ปลายทาง (โครงสร้าง {@code <root><port>...</port></root>}). */
        @XmlElement(name = "root")
        private Root root;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Root {
        /** เผื่อกรณีหลาย leg — map เป็น list ไว้ก่อน (ตัวอย่างจริงมี port เดียว). */
        @XmlElement(name = "port")
        private List<Port> port;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Port {
        @XmlElement(name = "PAXDISEMBARK")    private String paxdisembark;
        @XmlElement(name = "PAXTRANSIT2")     private String paxtransit2;
        @XmlElement(name = "FREIGHTUNLOADED") private String freightunloaded;
        @XmlElement(name = "FREIGHTTRANSIT2") private String freighttransit2;
        @XmlElement(name = "MAILUNLOADED")    private String mailunloaded;
        @XmlElement(name = "MAILTRANSIT3")    private String mailtransit3;
        @XmlElement(name = "FLIGHTFROM")      private String flightfrom;
        @XmlElement(name = "FLIGHTTO")        private String flightto;
    }
}
