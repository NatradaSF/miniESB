package sf.sfis.miniesb.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.miniesb.model.FidsAfttab;

@Repository
public interface FidsAfttabRepository extends JpaRepository<FidsAfttab, BigDecimal> {
}
