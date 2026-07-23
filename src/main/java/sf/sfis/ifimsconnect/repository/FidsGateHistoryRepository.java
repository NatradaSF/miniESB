package sf.sfis.ifimsconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.ifimsconnect.model.FidsGateHistory;

@Repository
public interface FidsGateHistoryRepository extends JpaRepository<FidsGateHistory, String> {
}
