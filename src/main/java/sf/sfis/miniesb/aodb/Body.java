package sf.sfis.miniesb.aodb;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Body {

    @XmlElement(name = "pl_turn")
    private PlTurn plTurn;
    
    @XmlElement(name = "pl_desk")
    private PlDesk plDesk;
    
    @XmlElement(name = "if_adexpmessage")
    private IfAdexpmessage ifAdexpmessage;
    
	@XmlElement(name = "Fault", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
	private Fault fault;
    
    public PlTurn getPlTurn() {
        return plTurn;
    }

    public void setPlTurn(PlTurn plTurn) {
        this.plTurn = plTurn;
    }

	public PlDesk getPlDesk() {
		return plDesk;
	}

	public void setPlDesk(PlDesk plDesk) {
		this.plDesk = plDesk;
	}

	public IfAdexpmessage getIfAdexpmessage() {
		return ifAdexpmessage;
	}

	public void setIfAdexpmessage(IfAdexpmessage ifAdexpmessage) {
		this.ifAdexpmessage = ifAdexpmessage;
	}

	public Fault getFault() {
		return fault;
	}

	public void setFault(Fault fault) {
		this.fault = fault;
	}
}