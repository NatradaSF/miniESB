package sf.sfis.miniesb.aodb;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class Header {

    @XmlElement(name = "control", namespace = "urn:com.tsystems.ac.aodb")
    private Control control;

    public Control getControl() { return control; }
    public void setControl(Control control) { this.control = control; }
}
