package sf.sfis.miniesb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.miniesb.model.FidsFinalcallHistory;

@Repository
public interface FidsFinalcallHistoryRepository extends JpaRepository<FidsFinalcallHistory, String> {
}
