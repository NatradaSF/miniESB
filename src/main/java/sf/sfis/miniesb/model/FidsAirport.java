package sf.sfis.miniesb.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "FIDS_AIRPORT")
public class FidsAirport implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name = "APC3", length = 3)
	private String apc3;
	
	@Column(name = "APC4", length = 4)
	private String  apc4;
	
	@Column(name = "APSN", length = 200)
	private String  apsn;
	
	@Column(name = "APSN2", length = 500)
	private String  apsn2;
	
	@Column(name = "APSN3", length = 500)
	private String  apsn3;
	
	@Column(name = "WEATHER", length = 20)
	private String  weather;
}