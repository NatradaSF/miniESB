package sf.sfis.miniesb.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.miniesb.model.FidsCcatab;

@Repository
public interface FidsCcatabRepository extends JpaRepository<FidsCcatab, BigDecimal> {
}
