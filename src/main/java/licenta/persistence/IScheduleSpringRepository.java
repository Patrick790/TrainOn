package licenta.persistence;

import jakarta.transaction.Transactional;
import licenta.model.Schedule;
import licenta.model.SportsHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IScheduleSpringRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findBySportsHallIdAndIsActiveOrderByDayOfWeek(Long sportsHallId, Boolean isActive);
    List<Schedule> findBySportsHallIdOrderByDayOfWeek(Long sportsHallId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Schedule s WHERE s.sportsHall.id = ?1")
    void deleteBySportsHallId(Long sportsHallId);


    /**
     * Gaseste programul pentru o sala si o zi specifica folosind ID-ul salii
     */
    @Query("SELECT s FROM Schedule s WHERE s.sportsHall.id = :hallId AND s.dayOfWeek = :dayOfWeek AND s.isActive = :isActive")
    Optional<Schedule> findBySportsHallIdAndDayOfWeekAndIsActive(@Param("hallId") Long hallId,
                                                                 @Param("dayOfWeek") Integer dayOfWeek,
                                                                 @Param("isActive") Boolean isActive);

    /**
     * Gaseste toate programele active pentru o zi specifica a saptamanii
     */
    @Query("SELECT s FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek AND s.isActive = true ORDER BY s.sportsHall.id")
    List<Schedule> findByDayOfWeekAndIsActiveTrueOrderBySportsHall(@Param("dayOfWeek") Integer dayOfWeek);

    /**
     * Verifica daca o sala are program definit pentru o zi specifica
     */
    @Query("SELECT COUNT(s) > 0 FROM Schedule s WHERE s.sportsHall.id = :hallId AND s.dayOfWeek = :dayOfWeek AND s.isActive = true")
    boolean existsBySportsHallIdAndDayOfWeekAndIsActiveTrue(@Param("hallId") Long hallId, @Param("dayOfWeek") Integer dayOfWeek);

    /**
     * Gaseste toate programele active
     */
    @Query("SELECT s FROM Schedule s WHERE s.isActive = true ORDER BY s.sportsHall.id ASC, s.dayOfWeek ASC")
    List<Schedule> findByIsActiveTrueOrderBySportsHallAscDayOfWeekAsc();

    /**
     * Gaseste orarele pentru o sală care se suprapun cu un interval orar
     */
    @Query("SELECT s FROM Schedule s WHERE s.sportsHall.id = :hallId AND s.isActive = true AND " +
            "s.dayOfWeek = :dayOfWeek AND " +
            "((:startTime >= s.startTime AND :startTime < s.endTime) OR " +
            "(:endTime > s.startTime AND :endTime <= s.endTime) OR " +
            "(:startTime <= s.startTime AND :endTime >= s.endTime))")
    List<Schedule> findOverlappingSchedulesByHallId(@Param("hallId") Long hallId,
                                                    @Param("dayOfWeek") Integer dayOfWeek,
                                                    @Param("startTime") String startTime,
                                                    @Param("endTime") String endTime);

    /**
     * Gaseste toate salile care sunt deschise intr-o zi specifica (returneaza ID-urile)
     */
    @Query("SELECT DISTINCT s.sportsHall.id FROM Schedule s WHERE s.dayOfWeek = :dayOfWeek AND s.isActive = true")
    List<Long> findHallIdsOpenOnDay(@Param("dayOfWeek") Integer dayOfWeek);

    /**
     * Gaseste programele unei sali care contin un anumit slot orar
     */
    @Query("SELECT s FROM Schedule s WHERE s.sportsHall.id = :hallId AND s.isActive = true AND " +
            "s.dayOfWeek = :dayOfWeek AND :timeSlot >= s.startTime AND :timeSlot < s.endTime")
    List<Schedule> findSchedulesContainingTimeSlotByHallId(@Param("hallId") Long hallId,
                                                           @Param("dayOfWeek") Integer dayOfWeek,
                                                           @Param("timeSlot") String timeSlot);

    /**
     * Contorizeaza programele active pentru o sala
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.sportsHall.id = :hallId AND s.isActive = true")
    long countBySportsHallIdAndIsActiveTrue(@Param("hallId") Long hallId);

    /**
     * Gaseste toate programele pentru o lista de sali (folosind ID-urile)
     */
    @Query("SELECT s FROM Schedule s WHERE s.sportsHall.id IN :hallIds AND s.isActive = true ORDER BY s.sportsHall.id, s.dayOfWeek")
    List<Schedule> findByHallIdsAndIsActiveTrue(@Param("hallIds") List<Long> hallIds);

    /**
     * Gaseste programele cu ore specifice
     */
    @Query("SELECT s FROM Schedule s WHERE s.isActive = true AND " +
            "s.startTime = :startTime AND s.endTime = :endTime")
    List<Schedule> findByWorkingHours(@Param("startTime") String startTime,
                                      @Param("endTime") String endTime);

    /**
     * Dezactiveaza toate programele unei sali
     */
    @Modifying
    @Transactional
    @Query("UPDATE Schedule s SET s.isActive = false WHERE s.sportsHall.id = :hallId")
    void deactivateAllForHallId(@Param("hallId") Long hallId);

    /**
     * Gaseste programele care se termina dupa o anumita ora
     */
    @Query("SELECT s FROM Schedule s WHERE s.isActive = true AND s.endTime >= :endTime")
    List<Schedule> findByIsActiveTrueAndEndTimeGreaterThanEqual(@Param("endTime") String endTime);

    /**
     * Gaseste programele care incep înainte de o anumită ora
     */
    @Query("SELECT s FROM Schedule s WHERE s.isActive = true AND s.startTime <= :startTime")
    List<Schedule> findByIsActiveTrueAndStartTimeLessThanEqual(@Param("startTime") String startTime);
}