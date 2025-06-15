package licenta.persistence;

import jakarta.transaction.Transactional;
import licenta.model.Reservation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface IReservationSpringRepository extends CrudRepository<Reservation, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Reservation r WHERE r.date >= :startDate AND r.date <= :endDate AND r.type = 'reservation'")
    int deleteReservationsByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

}
