    package licenta.persistence;

    import jakarta.transaction.Transactional;
    import licenta.model.Schedule;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Modifying;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.CrudRepository;

    import java.util.List;

    public interface IScheduleSpringRepository extends JpaRepository<Schedule, Long> {

        List<Schedule> findBySportsHallIdAndIsActiveOrderByDayOfWeek(Long sportsHallId, Boolean isActive);
        List<Schedule> findBySportsHallIdOrderByDayOfWeek(Long sportsHallId);

        @Modifying
        @Transactional
        @Query("DELETE FROM Schedule s WHERE s.sportsHall.id = ?1")
        void deleteBySportsHallId(Long sportsHallId);
    }