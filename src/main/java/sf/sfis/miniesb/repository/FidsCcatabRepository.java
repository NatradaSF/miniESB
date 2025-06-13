package sf.sfis.miniesb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.miniesb.model.FidsCcatab;
import sf.sfis.miniesb.model.FidsCcatabId;

@Repository
public interface FidsCcatabRepository extends JpaRepository<FidsCcatab, FidsCcatabId> {
}
