package sf.sfis.miniesb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sf.sfis.miniesb.model.FidsAirport;

@Repository
public interface FidsAirportRepository extends JpaRepository<FidsAirport, String> {
}
