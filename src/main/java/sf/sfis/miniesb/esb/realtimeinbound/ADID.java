//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2025.05.19 at 01:20:59 PM ICT 
//


package sf.sfis.miniesb.esb.realtimeinbound;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ADID.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ADID"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;whiteSpace value="preserve"/&gt;
 *     &lt;enumeration value="A"/&gt;
 *     &lt;enumeration value="D"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ADID")
@XmlEnum
public enum ADID {

    A,
    D;

    public String value() {
        return name();
    }

    public static ADID fromValue(String v) {
        return valueOf(v);
    }

}
