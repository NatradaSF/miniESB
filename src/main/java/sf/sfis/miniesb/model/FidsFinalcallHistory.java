package sf.sfis.miniesb.model;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Entity
@Data
@Table(name = "FIDS_FINALCALL_HISTORY")
public class FidsFinalcallHistory implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name = "URNO", length = 20)
	private String urno;
	
	@Column(name = "UPDATE_TIME", length = 6)
	@Temporal(TemporalType.TIMESTAMP)
	private Date  updateTime;
}