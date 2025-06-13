package sf.sfis.miniesb.service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.model.FidsFinalcallHistory;
import sf.sfis.miniesb.repository.FidsFinalcallHistoryRepository;

@Service
@RequiredArgsConstructor
public class FidsFinalcallHistoryService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FidsFinalcallHistoryService.class);
	private final FidsFinalcallHistoryRepository fidsFinalcallHistoryRepository;
	
	@Transactional
	public FidsFinalcallHistory saveFidsFinalcallHistory(FidsFinalcallHistory fidsFinalcallHistory) {
		try {
			fidsFinalcallHistory = fidsFinalcallHistoryRepository.save(fidsFinalcallHistory);
		} catch (Exception e) {
			LOGGER.error("saveFidsFinalcallHistory: ", e);
		}
		return fidsFinalcallHistory;
	}
	
	@Transactional
	public void deleteFidsFinalcallHistory(FidsFinalcallHistory fidsFinalcallHistory) {
		try {
			fidsFinalcallHistoryRepository.delete(fidsFinalcallHistory);
		} catch (Exception e) {
			LOGGER.error("deleteFidsFinalcallHistory: ", e);
		}
	}
	
	public void updateFinalCallHistory(FidsAfttab fidsAfttab) throws SQLException {
		FidsFinalcallHistory fidsFinalcallHistory = new FidsFinalcallHistory();
		fidsFinalcallHistory.setUrno(fidsAfttab.getUrno().toString());
		Optional<FidsFinalcallHistory> queryFidsFinalcallHistory = fidsFinalcallHistoryRepository.findById(fidsFinalcallHistory.getUrno());
		if (queryFidsFinalcallHistory.isPresent()) {
			if(!fidsAfttab.getRemp().equals("FNC")) {//Reset FNC 
				LOGGER.info("Reset Final Call for URNO "+fidsFinalcallHistory.getUrno());
				deleteFidsFinalcallHistory(fidsFinalcallHistory);
			}
		}else {
			if(fidsAfttab.getRemp().equals("FNC")) {//Reset FNC 
				LOGGER.info("Reset Final Call for URNO "+fidsFinalcallHistory.getUrno());
				fidsFinalcallHistory.setUpdateTime(Timestamp.from(Instant.now()));
				saveFidsFinalcallHistory(fidsFinalcallHistory);
			}
		}
	}
}
