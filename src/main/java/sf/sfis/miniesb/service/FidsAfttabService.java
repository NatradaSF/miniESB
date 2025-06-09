package sf.sfis.miniesb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.repository.FidsAfttabRepository;

@Service
@RequiredArgsConstructor
public class FidsAfttabService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FidsAfttabService.class);
	private final FidsAfttabRepository fidsAfttabRepository;
	
	@Transactional
	public FidsAfttab saveFidsAfttab(FidsAfttab fidsAfttab) {
		try {
			fidsAfttab = fidsAfttabRepository.save(fidsAfttab);
			LOGGER.info(fidsAfttab.toString());
		} catch (Exception e) {
			LOGGER.error("saveFidsAfttab: ", e);
		}
		return fidsAfttab;
	}

}
