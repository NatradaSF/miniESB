package sf.sfis.miniesb.model;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class FidsCcatabId implements Serializable {
	private static final long serialVersionUID = 1L;
	private BigDecimal flnu;
	private String ckic;
}
