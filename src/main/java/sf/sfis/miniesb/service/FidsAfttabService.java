package sf.sfis.miniesb.service;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.repository.FidsAfttabRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class FidsAfttabService {
	private final FidsAfttabRepository fidsAfttabRepository;
	
	@Transactional
	public FidsAfttab saveFidsAfttab(FidsAfttab fidsAfttab) {
		try {
			log.info(fidsAfttab.toString());
			fidsAfttab = fidsAfttabRepository.save(fidsAfttab);
		} catch (Exception e) {
			log.error("saveFidsAfttab: ", e);
		}
		return fidsAfttab;
	}

}
