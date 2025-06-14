//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2025.05.19 at 12:07:30 PM ICT 
//


package sf.sfis.miniesb.esb.filereadyout;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for FileTransfer_OUT complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FileTransfer_OUT">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MSG">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="BATCHFILE_OUT" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="INFOBJ_FILE_OUT">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;sequence>
 *                                       &lt;element name="ORIGIN" type="{}MESSAGEORIGIN"/>
 *                                       &lt;element name="FILENAME" type="{}FILENAME"/>
 *                                       &lt;element name="FILEACTION" type="{}FILEACTION"/>
 *                                       &lt;element name="TIMESTAMP" type="{}TIMESTAMP"/>
 *                                       &lt;element name="CONTENT_OUT" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                                     &lt;/sequence>
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FileTransfer_OUT", propOrder = {
    "msg"
})
public class FileTransferOUT {

    @XmlElement(name = "MSG", required = true)
    protected FileTransferOUT.MSG msg;

    /**
     * Gets the value of the msg property.
     * 
     * @return
     *     possible object is
     *     {@link FileTransferOUT.MSG }
     *     
     */
    public FileTransferOUT.MSG getMSG() {
        return msg;
    }

    /**
     * Sets the value of the msg property.
     * 
     * @param value
     *     allowed object is
     *     {@link FileTransferOUT.MSG }
     *     
     */
    public void setMSG(FileTransferOUT.MSG value) {
        this.msg = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="BATCHFILE_OUT" minOccurs="0">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="INFOBJ_FILE_OUT">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;sequence>
     *                             &lt;element name="ORIGIN" type="{}MESSAGEORIGIN"/>
     *                             &lt;element name="FILENAME" type="{}FILENAME"/>
     *                             &lt;element name="FILEACTION" type="{}FILEACTION"/>
     *                             &lt;element name="TIMESTAMP" type="{}TIMESTAMP"/>
     *                             &lt;element name="CONTENT_OUT" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *                           &lt;/sequence>
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                 &lt;/sequence>
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "batchfileout"
    })
    public static class MSG {

        @XmlElement(name = "BATCHFILE_OUT")
        protected FileTransferOUT.MSG.BATCHFILEOUT batchfileout;

        /**
         * Gets the value of the batchfileout property.
         * 
         * @return
         *     possible object is
         *     {@link FileTransferOUT.MSG.BATCHFILEOUT }
         *     
         */
        public FileTransferOUT.MSG.BATCHFILEOUT getBATCHFILEOUT() {
            return batchfileout;
        }

        /**
         * Sets the value of the batchfileout property.
         * 
         * @param value
         *     allowed object is
         *     {@link FileTransferOUT.MSG.BATCHFILEOUT }
         *     
         */
        public void setBATCHFILEOUT(FileTransferOUT.MSG.BATCHFILEOUT value) {
            this.batchfileout = value;
        }


        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected content contained within this class.
         * 
         * <pre>
         * &lt;complexType>
         *   &lt;complexContent>
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *       &lt;sequence>
         *         &lt;element name="INFOBJ_FILE_OUT">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;sequence>
         *                   &lt;element name="ORIGIN" type="{}MESSAGEORIGIN"/>
         *                   &lt;element name="FILENAME" type="{}FILENAME"/>
         *                   &lt;element name="FILEACTION" type="{}FILEACTION"/>
         *                   &lt;element name="TIMESTAMP" type="{}TIMESTAMP"/>
         *                   &lt;element name="CONTENT_OUT" type="{http://www.w3.org/2001/XMLSchema}string"/>
         *                 &lt;/sequence>
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *       &lt;/sequence>
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "infobjfileout"
        })
        public static class BATCHFILEOUT {

            @XmlElement(name = "INFOBJ_FILE_OUT", required = true)
            protected FileTransferOUT.MSG.BATCHFILEOUT.INFOBJFILEOUT infobjfileout;

            /**
             * Gets the value of the infobjfileout property.
             * 
             * @return
             *     possible object is
             *     {@link FileTransferOUT.MSG.BATCHFILEOUT.INFOBJFILEOUT }
             *     
             */
            public FileTransferOUT.MSG.BATCHFILEOUT.INFOBJFILEOUT getINFOBJFILEOUT() {
                return infobjfileout;
            }

            /**
             * Sets the value of the infobjfileout property.
             * 
             * @param value
             *     allowed object is
             *     {@link FileTransferOUT.MSG.BATCHFILEOUT.INFOBJFILEOUT }
             *     
             */
            public void setINFOBJFILEOUT(FileTransferOUT.MSG.BATCHFILEOUT.INFOBJFILEOUT value) {
                this.infobjfileout = value;
            }


            /**
             * <p>Java class for anonymous complex type.
             * 
             * <p>The following schema fragment specifies the expected content contained within this class.
             * 
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *       &lt;sequence>
             *         &lt;element name="ORIGIN" type="{}MESSAGEORIGIN"/>
             *         &lt;element name="FILENAME" type="{}FILENAME"/>
             *         &lt;element name="FILEACTION" type="{}FILEACTION"/>
             *         &lt;element name="TIMESTAMP" type="{}TIMESTAMP"/>
             *         &lt;element name="CONTENT_OUT" type="{http://www.w3.org/2001/XMLSchema}string"/>
             *       &lt;/sequence>
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "origin",
                "filename",
                "fileaction",
                "timestamp",
                "contentout"
            })
            public static class INFOBJFILEOUT {

                @XmlElement(name = "ORIGIN", required = true)
                protected String origin;
                @XmlElement(name = "FILENAME", required = true)
                protected String filename;
                @XmlElement(name = "FILEACTION", required = true)
                @XmlSchemaType(name = "string")
                protected FILEACTION fileaction;
                @XmlElement(name = "TIMESTAMP", required = true)
                protected String timestamp;
                @XmlElement(name = "CONTENT_OUT", required = true)
                protected String contentout;

                /**
                 * Gets the value of the origin property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getORIGIN() {
                    return origin;
                }

                /**
                 * Sets the value of the origin property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setORIGIN(String value) {
                    this.origin = value;
                }

                /**
                 * Gets the value of the filename property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getFILENAME() {
                    return filename;
                }

                /**
                 * Sets the value of the filename property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setFILENAME(String value) {
                    this.filename = value;
                }

                /**
                 * Gets the value of the fileaction property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link FILEACTION }
                 *     
                 */
                public FILEACTION getFILEACTION() {
                    return fileaction;
                }

                /**
                 * Sets the value of the fileaction property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link FILEACTION }
                 *     
                 */
                public void setFILEACTION(FILEACTION value) {
                    this.fileaction = value;
                }

                /**
                 * Gets the value of the timestamp property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getTIMESTAMP() {
                    return timestamp;
                }

                /**
                 * Sets the value of the timestamp property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setTIMESTAMP(String value) {
                    this.timestamp = value;
                }

                /**
                 * Gets the value of the contentout property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getCONTENTOUT() {
                    return contentout;
                }

                /**
                 * Sets the value of the contentout property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setCONTENTOUT(String value) {
                    this.contentout = value;
                }

            }

        }

    }

}
