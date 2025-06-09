package sf.sfis.miniesb.service;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.model.FidsCcatab;
import sf.sfis.miniesb.repository.FidsCcatabRepository;

@Service
@RequiredArgsConstructor
public class FidsCcatabService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FidsCcatabService.class);
	private final FidsCcatabRepository fidsCcatabRepository;
	
	@Transactional
	public FidsCcatab saveFidsCcatab(FidsCcatab fidsCcatab) {
		try {
			fidsCcatab = fidsCcatabRepository.save(fidsCcatab);
			LOGGER.info(fidsCcatab.toString());
		} catch (Exception e) {
			LOGGER.error("saveFidsCcatab: ", e);
		}
		return fidsCcatab;
	}
	
	public void updateCcatab(FidsAfttab fidsAfttab) throws SQLException {
		if(fidsAfttab.getLstFidsCcatab() != null) {
			for(FidsCcatab ccatab : fidsAfttab.getLstFidsCcatab()) {
				FidsCcatab fidsCcatab = new FidsCcatab();
				fidsCcatab.setFlnu(fidsAfttab.getUrno());
				fidsCcatab.setFlno(fidsAfttab.getFlno());
				fidsCcatab.setHopo(fidsAfttab.getHopo());
				fidsCcatab.setAct3(fidsAfttab.getAct3());
				fidsCcatab.setStod(fidsAfttab.getStod());
				fidsCcatab.setLstu(fidsAfttab.getLstu());
				fidsCcatab.setCdat(fidsAfttab.getCdat());
				fidsCcatab.setPrfl(fidsAfttab.getPrfl());
				fidsCcatab.setStat(fidsAfttab.getStat());
				fidsCcatab.setUsec(fidsAfttab.getUsec());
				fidsCcatab.setUseu(fidsAfttab.getUseu());
				fidsCcatab.setCkic(ccatab.getCkic());
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
