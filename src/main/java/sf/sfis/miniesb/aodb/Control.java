package sf.sfis.miniesb.aodb;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Control {

	@XmlElement(name = "message-id", namespace = "urn:com.tsystems.ac.aodb")
	private String messageId;

	@XmlElement(name = "message-version", namespace = "urn:com.tsystems.ac.aodb")
	private String messageVersion;

	@XmlElement(name = "message-type", namespace = "urn:com.tsystems.ac.aodb")
	private String messageType;

	@XmlElement(name = "confirm-type", namespace = "urn:com.tsystems.ac.aodb")
	private String confirmType;

	@XmlElement(name = "originator", namespace = "urn:com.tsystems.ac.aodb")
	private String originator;
	
	@XmlElement(name = "request", namespace = "urn:com.tsystems.ac.aodb")
	private Request request;

	@XmlElement(name = "timestamp", namespace = "urn:com.tsystems.ac.aodb")
	private String timestamp;

	@XmlElement(name = "sender", namespace = "urn:com.tsystems.ac.aodb")
	private String sender;

	@XmlElement(name = "receiver", namespace = "urn:com.tsystems.ac.aodb")
	private String receiver;

	@XmlElement(name = "station", namespace = "urn:com.tsystems.ac.aodb")
	private String station;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getMessageVersion() {
		return messageVersion;
	}

	public void setMessageVersion(String messageVersion) {
		this.messageVersion = messageVersion;
	}

	public String getConfirmType() {
		return confirmType;
	}

	public void setConfirmType(String confirmType) {
		this.confirmType = confirmType;
	}

	public String getOriginator() {
		return originator;
	}

	public void setOriginator(String originator) {
		this.originator = originator;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public String getStation() {
		return station;
	}

	public void setStation(String station) {
		this.station = station;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}
}
