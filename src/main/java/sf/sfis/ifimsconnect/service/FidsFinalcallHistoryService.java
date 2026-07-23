package sf.sfis.ifimsconnect.service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sf.sfis.ifimsconnect.model.FidsAfttab;
import sf.sfis.ifimsconnect.model.FidsFinalcallHistory;
import sf.sfis.ifimsconnect.repository.FidsFinalcallHistoryRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class FidsFinalcallHistoryService {
	private final FidsFinalcallHistoryRepository fidsFinalcallHistoryRepository;
	
	@Transactional
	public FidsFinalcallHistory saveFidsFinalcallHistory(FidsFinalcallHistory fidsFinalcallHistory) {
		try {
			fidsFinalcallHistory = fidsFinalcallHistoryRepository.save(fidsFinalcallHistory);
		} catch (Exception e) {
			log.error("saveFidsFinalcallHistory: ", e);
		}
		return fidsFinalcallHistory;
	}
	
	@Transactional
	public void deleteFidsFinalcallHistory(FidsFinalcallHistory fidsFinalcallHistory) {
		try {
			fidsFinalcallHistoryRepository.delete(fidsFinalcallHistory);
		} catch (Exception e) {
			log.error("deleteFidsFinalcallHistory: ", e);
		}
	}
	
	public void updateFinalCallHistory(FidsAfttab fidsAfttab) throws SQLException {
		FidsFinalcallHistory fidsFinalcallHistory = new FidsFinalcallHistory();
		fidsFinalcallHistory.setUrno(fidsAfttab.getUrno().toString());
		Optional<FidsFinalcallHistory> queryFidsFinalcallHistory = fidsFinalcallHistoryRepository.findById(fidsFinalcallHistory.getUrno());
		if (queryFidsFinalcallHistory.isPresent()) {
//			if(!fidsAfttab.getRemp().equals("FNC")) {//Reset FNC 
			if(!fidsAfttab.getRemp().equals("FNC")) {//Reset 2ND 
				log.info("Reset Final Call for URNO "+fidsFinalcallHistory.getUrno());
				deleteFidsFinalcallHistory(fidsFinalcallHistory);
			}
		}else {
//			if(fidsAfttab.getRemp().equals("FNC")) {//Reset FNC 
			if(fidsAfttab.getRemp().equals("FNC")) {//Reset 2ND 
				log.info("Reset Final Call for URNO "+fidsFinalcallHistory.getUrno());
				fidsFinalcallHistory.setUpdateTime(Timestamp.from(Instant.now()));
				saveFidsFinalcallHistory(fidsFinalcallHistory);
			}
		}
	}
}
