package sf.sfis.miniesb.aodb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Request {

    @XmlElement(name = "datatype", namespace = "urn:com.tsystems.ac.aodb")
    private String datatype;

    @XmlElement(name = "keep-subscription", namespace = "urn:com.tsystems.ac.aodb")
    private String keepSubscription;

    @XmlElement(name = "start-time", namespace = "urn:com.tsystems.ac.aodb")
    private String startTime;

    @XmlElement(name = "end-time", namespace = "urn:com.tsystems.ac.aodb")
    private String endTime;

	public String getDatatype() {
		return datatype;
	}

	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}

	public String getKeepSubscription() {
		return keepSubscription;
	}

	public void setKeepSubscription(String keepSubscription) {
		this.keepSubscription = keepSubscription;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
}
