package licenta.persistence;

import licenta.model.SportsHall;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.sql.rowset.CachedRowSet;
import java.util.List;

public interface ISportsHallSpringRepository extends CrudRepository<SportsHall, Long> {
    List<SportsHall> findByAdminId(Long adminId);

    List<SportsHall> findByCityOrderByName(String city);

    @Query("SELECT DISTINCT sh.city FROM SportsHall sh WHERE sh.city IS NOT NULL ORDER BY sh.city")
    List<String> findDistinctCities();

    List<SportsHall> findByStatusOrderByName(SportsHall.HallStatus status);

    // Găsește sălile dintr-un oraș cu un anumit status
    List<SportsHall> findByCityAndStatusOrderByName(String city, SportsHall.HallStatus status);

    // Găsește sălile unui admin cu un anumit status
    List<SportsHall> findByAdminIdAndStatus(Long adminId, SportsHall.HallStatus status);

    // Găsește doar sălile active ale unui admin
    List<SportsHall> findByAdminIdAndStatusOrderByName(Long adminId, SportsHall.HallStatus status);
}
