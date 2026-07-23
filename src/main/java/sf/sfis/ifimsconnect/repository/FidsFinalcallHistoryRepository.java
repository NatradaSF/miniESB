package sf.sfis.ifimsconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.ifimsconnect.model.FidsFinalcallHistory;

@Repository
public interface FidsFinalcallHistoryRepository extends JpaRepository<FidsFinalcallHistory, String> {
}
