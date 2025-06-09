package sf.sfis.miniesb.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "FIDS_CCATAB")
public class FidsCcatab implements Serializable {
	private static final long serialVersionUID = 1L;
	@Id
	@Column(name = "FLNU")
	private BigDecimal flnu;
	
	@Column(name = "ACT3", length = 20)
	private String act3;
	
	@Column(name = "CDAT", length = 14)
	private String cdat;
	
	@Column(name = "CGRU", length = 512)
	private String cgru;
	
	@Column(name = "CKBA", length = 14)
	private String ckba;
	
	@Column(name = "CKBS", length = 14)
	private String ckbs;
	
	@Column(name = "CKEA", length = 14)
	private String ckea;
	
	@Column(name = "CKES", length = 14)
	private String ckes;
	
	@Column(name = "CKIC", length = 5)
	private String ckic;
	
	@Column(name = "CKIF", length = 1)
	private String ckif;
	
	@Column(name = "CKIT", length = 1)
	private String ckit;
	
	@Column(name = "COIX")
	private BigDecimal coix;
	
	@Column(name = "COPN", length = 3)
	private String copn;
	
	@Column(name = "CTYP", length = 1)
	private String ctyp;
	
	@Column(name = "DISP", length = 60)
	private String disp;
	
	@Column(name = "FCKI", length = 1)
	private String fcki;
	
	@Column(name = "FLNO", length = 9)
	private String flno;
	
	@Column(name = "GHPU", length = 20)
	private String ghpu;
	
	@Column(name = "GHSU", length = 20)
	private String ghsu;
	
	@Column(name = "GPMU", length = 20)
	private String gpmu;
	
	@Column(name = "GRNU")
	private BigDecimal grnu;
	
	@Column(name = "GURN")
	private BigDecimal gurn;
	
	@Column(name = "HOME", length = 3)
	private String home;
	
	@Column(name = "HOPO", length = 3)
	private String hopo;
	
	@Column(name = "LARC", length = 4)
	private String larc;
	
	@Column(name = "LAST", length = 1)
	private String last;
	
	@Column(name = "LSTU", length = 14)
	private String lstu;
	
	@Column(name = "PRFL", length = 1)
	private String prfl;
	
	@Column(name = "REMA", length = 60)
	private String rema;
	
	@Column(name = "STAT", length = 10)
	private String stat;
	
	@Column(name = "STOD", length = 14)
	private String stod;

	@Column(name = "URNO")
	private BigDecimal urno;
	
	@Column(name = "USEC", length = 32)
	private String usec;
	
	@Column(name = "USEU", length = 32)
	private String useu;
}