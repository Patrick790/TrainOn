package licenta.persistence;

import licenta.model.SportsHall;
import org.springframework.data.repository.CrudRepository;

import javax.sql.rowset.CachedRowSet;
import java.util.List;

public interface ISportsHallSpringRepository extends CrudRepository<SportsHall, Long> {
    List<SportsHall> findByAdminId(Long adminId);
}
