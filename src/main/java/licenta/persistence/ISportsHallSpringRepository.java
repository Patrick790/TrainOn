package licenta.persistence;

import licenta.model.SportsHall;
import org.springframework.data.repository.CrudRepository;

import javax.sql.rowset.CachedRowSet;

public interface ISportsHallSpringRepository extends CrudRepository<SportsHall, Long> {
}
