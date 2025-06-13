package sf.sfis.miniesb.model;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "FIDS_GATE_HISTORY")
public class FidsGateHistory implements Serializable {
	private static final long serialVersionUID = 1L;
	@Id
	@Column(name = "URNO", length = 20)
	private String urno;
	
	@Column(name = "UPDATE_TIME", length = 6)
	private OffsetDateTime updateTime;

	@Column(name = "NEWGATE1", length = 20)
	private String newgate1;

	@Column(name = "OLDGATE1", length = 20)
	private String oldgate1;

	@Column(name = "NEWGATE2", length = 20)
	private String newgate2;

	@Column(name = "OLDGATE2", length = 20)
	private String oldgate2;

	@Column(name = "FLNO", length = 9)
	private String flno;

	@Column(name = "HOPO", length = 3)
	private String hopo;

	@Column(name = "SOBT", length = 14)
	private String sobt;
}