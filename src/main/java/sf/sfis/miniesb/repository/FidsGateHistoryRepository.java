package sf.sfis.miniesb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.miniesb.model.FidsGateHistory;

@Repository
public interface FidsGateHistoryRepository extends JpaRepository<FidsGateHistory, String> {
}
