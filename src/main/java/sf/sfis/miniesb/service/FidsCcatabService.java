package sf.sfis.miniesb.service;

import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.model.FidsCcatab;
import sf.sfis.miniesb.model.FidsCcatabId;
import sf.sfis.miniesb.model.FidsFinalcallHistory;
import sf.sfis.miniesb.repository.FidsCcatabRepository;

@Service
@RequiredArgsConstructor
public class FidsCcatabService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FidsCcatabService.class);
	private final FidsCcatabRepository fidsCcatabRepository;
	
	@Transactional
	public FidsCcatab saveFidsCcatab(FidsCcatab fidsCcatab) {
		try {
			LOGGER.info(fidsCcatab.toString());
			fidsCcatab = fidsCcatabRepository.save(fidsCcatab);
		} catch (Exception e) {
			LOGGER.error("saveFidsCcatab: ", e);
		}
		return fidsCcatab;
	}
	
	@Transactional
	public void deleteCcatab(FidsCcatab fidsCcatab) {
		try {
			LOGGER.info("delete fidsCcatab: "+ fidsCcatab.getFlnu()+", "+fidsCcatab.getCkic());
			FidsCcatabId id = new FidsCcatabId();
			id.setFlnu(fidsCcatab.getFlnu());
			id.setCkic(fidsCcatab.getCkic());
			LOGGER.info("Record exists? " + fidsCcatabRepository.findById(id).isPresent());
//	        Optional<FidsCcatab> entity = fidsCcatabRepository.findById(id);
//	        entity.ifPresent(fidsCcatabRepository::delete);
			fidsCcatabRepository.delete(fidsCcatab);
		} catch (Exception e) {
			LOGGER.error("deleteCcatab: ", e);
		}
	}
	
	public void updateCcatab(FidsAfttab fidsAfttab) throws SQLException {
		if(fidsAfttab.getLstFidsCcatab() != null) {
			for(FidsCcatab ccatab : fidsAfttab.getLstFidsCcatab()) {
				FidsCcatab fidsCcatab = new FidsCcatab();
				fidsCcatab.setFlnu(ccatab.getFlnu()!=null?ccatab.getFlnu():fidsAfttab.getUrno());
				fidsCcatab.setCkic(ccatab.getCkic());
				LOGGER.info("CKIC : "+ccatab.getCkic());
				deleteCcatab(fidsCcatab);
				
				fidsCcatab.setFlno(ccatab.getFlno()!=null?ccatab.getFlno():fidsAfttab.getFlno());
				fidsCcatab.setHopo(fidsAfttab.getHopo());
				fidsCcatab.setAct3(fidsAfttab.getAct3());
				fidsCcatab.setStod(fidsAfttab.getStod());
				fidsCcatab.setLstu(fidsAfttab.getLstu());
				fidsCcatab.setCdat(fidsAfttab.getCdat());
				fidsCcatab.setPrfl(fidsAfttab.getPrfl());
				fidsCcatab.setStat(fidsAfttab.getStat());
				fidsCcatab.setUsec(fidsAfttab.getUsec());
				fidsCcatab.setUseu(fidsAfttab.getUseu());
				fidsCcatab.setCtyp(ccatab.getCtyp());
				fidsCcatab.setCkbs(ccatab.getCkbs());
			    fidsCcatab.setCkes(ccatab.getCkes());
			    fidsCcatab.setCkba(ccatab.getCkba());
			    fidsCcatab.setCkea(ccatab.getCkea());
				saveFidsCcatab(fidsCcatab);
			}
		}
	}

}
