package licenta.persistence;

import licenta.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface IReservationSpringRepository extends JpaRepository<Reservation, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Reservation r WHERE r.date >= :startDate AND r.date <= :endDate AND r.type = 'reservation'")
    int deleteReservationsByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date BETWEEN :startDate AND :endDate AND r.type = 'reservation'")
    long countByDateBetweenAndType(@Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("type") String type);

    List<Reservation> findByDateBetweenAndType(Date startDate, Date endDate, String type);
}