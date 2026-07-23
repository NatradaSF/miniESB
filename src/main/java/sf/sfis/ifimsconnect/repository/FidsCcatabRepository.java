package sf.sfis.ifimsconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.ifimsconnect.model.FidsCcatab;
import sf.sfis.ifimsconnect.model.FidsCcatabId;

@Repository
public interface FidsCcatabRepository extends JpaRepository<FidsCcatab, FidsCcatabId> {
}
