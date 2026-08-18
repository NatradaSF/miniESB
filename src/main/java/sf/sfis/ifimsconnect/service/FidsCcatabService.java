package sf.sfis.ifimsconnect.service;

import java.sql.SQLException;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sf.sfis.ifimsconnect.model.FidsAfttab;
import sf.sfis.ifimsconnect.model.FidsCcatab;
import sf.sfis.ifimsconnect.repository.FidsCcatabRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class FidsCcatabService {
	private final FidsCcatabRepository fidsCcatabRepository;
	
	@Transactional
	public FidsCcatab saveFidsCcatab(FidsCcatab fidsCcatab) {
		try {
			log.info(fidsCcatab.toString());
			fidsCcatab = fidsCcatabRepository.save(fidsCcatab);
		} catch (Exception e) {
			log.error("CKIC: ", fidsCcatab.getCkic());
			log.error("saveFidsCcatab: ", e);
		}
		return fidsCcatab;
	}
	
//	@Transactional
//	public void deleteCcatab(FidsCcatab fidsCcatab) {
//		try {
//			log.info("delete fidsCcatab: "+ fidsCcatab.getFlnu()+", "+fidsCcatab.getCkic());
//			fidsCcatabRepository.delete(fidsCcatab);
//		} catch (Exception e) {
//			log.error("deleteCcatab: ", e);
//		}
//	}
	
	public void updateCcatab(FidsAfttab fidsAfttab) throws SQLException {
		if(fidsAfttab.getLstFidsCcatab() != null) {
			for(FidsCcatab ccatab : fidsAfttab.getLstFidsCcatab()) {
				FidsCcatab fidsCcatab = new FidsCcatab();
				fidsCcatab.setFlnu(ccatab.getFlnu()!=null?ccatab.getFlnu():fidsAfttab.getUrno());
				fidsCcatab.setCkic(String.format("%-5s", ccatab.getCkic()));
				log.info("CKIC : "+ccatab.getCkic());
//				deleteCcatab(fidsCcatab);
				
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
				fidsCcatab.setCtyp(ccatab.getCtyp().equals("C")?"C":" ");
				fidsCcatab.setCkbs(ccatab.getCkbs());
			    fidsCcatab.setCkes(ccatab.getCkes());
			    fidsCcatab.setCkba(ccatab.getCkba());
			    fidsCcatab.setCkea(ccatab.getCkea());
				saveFidsCcatab(fidsCcatab);
			}
		}
	}

}
