package licenta.persistence;

import licenta.model.SportsHall;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ISportsHallSpringRepository extends CrudRepository<SportsHall, Long> {

    // ===== METODE EXISTENTE DE BAZĂ =====

    List<SportsHall> findByAdminId(Long adminId);

    List<SportsHall> findByCityOrderByName(String city);

    @Query("SELECT DISTINCT sh.city FROM SportsHall sh WHERE sh.city IS NOT NULL ORDER BY sh.city")
    List<String> findDistinctCities();

    List<SportsHall> findByStatusOrderByName(SportsHall.HallStatus status);

    List<SportsHall> findByCityAndStatusOrderByName(String city, SportsHall.HallStatus status);

    // Găsește sălile unui admin cu un anumit status
    List<SportsHall> findByAdminIdAndStatus(Long adminId, SportsHall.HallStatus status);

    // Găsește doar sălile active ale unui admin
    List<SportsHall> findByAdminIdAndStatusOrderByName(Long adminId, SportsHall.HallStatus status);

    // ===== METODE PENTRU AUTOCOMPLETE =====

    // Caută săli după nume (case-insensitive) pentru autocomplete
    @Query("SELECT sh.name FROM SportsHall sh WHERE LOWER(sh.name) LIKE LOWER(CONCAT('%', :query, '%')) AND sh.status = 'ACTIVE' ORDER BY sh.name")
    List<String> findHallNamesSuggestions(@Param("query") String query);

    // Caută săli după nume exact (pentru search final)
    @Query("SELECT sh FROM SportsHall sh WHERE LOWER(sh.name) LIKE LOWER(CONCAT('%', :name, '%')) AND sh.status = 'ACTIVE' ORDER BY sh.name")
    List<SportsHall> findByNameContainingIgnoreCase(@Param("name") String name);

    // Caută săli după nume și oraș (pentru search mai precis)
    @Query("SELECT sh FROM SportsHall sh WHERE LOWER(sh.name) LIKE LOWER(CONCAT('%', :name, '%')) AND LOWER(sh.city) = LOWER(:city) AND sh.status = 'ACTIVE' ORDER BY sh.name")
    List<SportsHall> findByNameContainingAndCityIgnoreCase(@Param("name") String name, @Param("city") String city);

    // Limitează numărul de sugestii pentru performanță
    @Query(value = "SELECT DISTINCT sh.name FROM SportsHall sh WHERE LOWER(sh.name) LIKE LOWER(CONCAT('%', :query, '%')) AND sh.status = 'ACTIVE' ORDER BY sh.name LIMIT 10", nativeQuery = true)
    List<String> findHallNamesSuggestionsLimited(@Param("query") String query);

    // ===== METODE ÎMBUNĂTĂȚITE PENTRU CĂUTARE DUPĂ ORAȘ ȘI SPORT =====

    // Caută săli după oraș și sport - include sălile multiple dar exclude înot pentru multiple
    @Query("SELECT sh FROM SportsHall sh WHERE " +
            "LOWER(sh.city) = LOWER(:city) AND " +
            "sh.status = 'ACTIVE' AND " +
            "(" +
            "  LOWER(sh.type) = LOWER(:sport) OR " +
            "  (" +
            "    (LOWER(sh.type) = 'multipla' OR " +
            "     LOWER(sh.type) LIKE '%polivalent%' OR " +
            "     LOWER(sh.type) LIKE '%multipurpose%' OR " +
            "     LOWER(sh.type) LIKE '%universal%' OR " +
            "     LOWER(sh.type) = 'sport') AND " +
            "    LOWER(:sport) != 'inot'" +
            "  )" +
            ") " +
            "ORDER BY sh.name")
    List<SportsHall> findByCityAndSportIncludingMultipurpose(@Param("city") String city, @Param("sport") String sport);

    // Căutare combinată: oraș, sport și nume (pentru search complet)
    @Query("SELECT sh FROM SportsHall sh WHERE " +
            "LOWER(sh.city) = LOWER(:city) AND " +
            "sh.status = 'ACTIVE' AND " +
            "(" +
            "  LOWER(sh.type) = LOWER(:sport) OR " +
            "  (" +
            "    (LOWER(sh.type) = 'multipla' OR " +
            "     LOWER(sh.type) LIKE '%polivalent%' OR " +
            "     LOWER(sh.type) LIKE '%multipurpose%' OR " +
            "     LOWER(sh.type) LIKE '%universal%' OR " +
            "     LOWER(sh.type) = 'sport') AND " +
            "    LOWER(:sport) != 'inot'" +
            "  )" +
            ") AND " +
            "LOWER(sh.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "ORDER BY sh.name")
    List<SportsHall> findByCityAndSportAndNameIncludingMultipurpose(@Param("city") String city, @Param("sport") String sport, @Param("name") String name);

    // Căutare doar după sport (fără oraș) - include multiple dar exclude înot pentru multiple
    @Query("SELECT sh FROM SportsHall sh WHERE " +
            "sh.status = 'ACTIVE' AND " +
            "(" +
            "  LOWER(sh.type) = LOWER(:sport) OR " +
            "  (" +
            "    (LOWER(sh.type) = 'multipla' OR " +
            "     LOWER(sh.type) LIKE '%polivalent%' OR " +
            "     LOWER(sh.type) LIKE '%multipurpose%' OR " +
            "     LOWER(sh.type) LIKE '%universal%' OR " +
            "     LOWER(sh.type) = 'sport') AND " +
            "    LOWER(:sport) != 'inot'" +
            "  )" +
            ") " +
            "ORDER BY sh.city, sh.name")
    List<SportsHall> findBySportIncludingMultipurpose(@Param("sport") String sport);

    // ===== METODE SPECIFICE PENTRU ÎNOT =====

    // Pentru a căuta exclusiv săli de înot (inclusiv polivalente cu înot)
    @Query("SELECT sh FROM SportsHall sh WHERE " +
            "sh.status = 'ACTIVE' AND " +
            "(" +
            "  LOWER(sh.type) = 'inot' OR " +
            "  LOWER(sh.type) LIKE '%piscina%' OR " +
            "  LOWER(sh.type) LIKE '%swimming%' OR " +
            "  LOWER(sh.type) LIKE '%inot%'" +
            ") " +
            "ORDER BY sh.city, sh.name")
    List<SportsHall> findSwimmingHalls();

    // Pentru căutare săli de înot după oraș
    @Query("SELECT sh FROM SportsHall sh WHERE " +
            "LOWER(sh.city) = LOWER(:city) AND " +
            "sh.status = 'ACTIVE' AND " +
            "(" +
            "  LOWER(sh.type) = 'inot' OR " +
            "  LOWER(sh.type) LIKE '%piscina%' OR " +
            "  LOWER(sh.type) LIKE '%swimming%' OR " +
            "  LOWER(sh.type) LIKE '%inot%'" +
            ") " +
            "ORDER BY sh.name")
    List<SportsHall> findSwimmingHallsByCity(@Param("city") String city);

    // Pentru căutare săli de înot după nume și oraș
    @Query("SELECT sh FROM SportsHall sh WHERE " +
            "LOWER(sh.city) = LOWER(:city) AND " +
            "sh.status = 'ACTIVE' AND " +
            "LOWER(sh.name) LIKE LOWER(CONCAT('%', :name, '%')) AND " +
            "(" +
            "  LOWER(sh.type) = 'inot' OR " +
            "  LOWER(sh.type) LIKE '%piscina%' OR " +
            "  LOWER(sh.type) LIKE '%swimming%' OR " +
            "  LOWER(sh.type) LIKE '%inot%'" +
            ") " +
            "ORDER BY sh.name")
    List<SportsHall> findSwimmingHallsByNameAndCity(@Param("name") String name, @Param("city") String city);

    // Pentru căutare săli de înot doar după nume (toate orașele)
    @Query("SELECT sh FROM SportsHall sh WHERE " +
            "sh.status = 'ACTIVE' AND " +
            "LOWER(sh.name) LIKE LOWER(CONCAT('%', :name, '%')) AND " +
            "(" +
            "  LOWER(sh.type) = 'inot' OR " +
            "  LOWER(sh.type) LIKE '%piscina%' OR " +
            "  LOWER(sh.type) LIKE '%swimming%' OR " +
            "  LOWER(sh.type) LIKE '%inot%'" +
            ") " +
            "ORDER BY sh.city, sh.name")
    List<SportsHall> findSwimmingHallsByName(@Param("name") String name);

    // ===== METODE UTILITARE PENTRU STATISTICI =====

    // Numărul total de săli active
    @Query("SELECT COUNT(sh) FROM SportsHall sh WHERE sh.status = 'ACTIVE'")
    Long countActiveHalls();

    // Numărul de săli active pe oraș
    @Query("SELECT COUNT(sh) FROM SportsHall sh WHERE sh.status = 'ACTIVE' AND LOWER(sh.city) = LOWER(:city)")
    Long countActiveHallsByCity(@Param("city") String city);

    // Numărul de săli active pe sport (incluzând multiple pentru alte sporturi decât înot)
    @Query("SELECT COUNT(sh) FROM SportsHall sh WHERE " +
            "sh.status = 'ACTIVE' AND " +
            "(" +
            "  LOWER(sh.type) = LOWER(:sport) OR " +
            "  (" +
            "    (LOWER(sh.type) = 'multipla' OR " +
            "     LOWER(sh.type) LIKE '%polivalent%' OR " +
            "     LOWER(sh.type) LIKE '%multipurpose%' OR " +
            "     LOWER(sh.type) LIKE '%universal%' OR " +
            "     LOWER(sh.type) = 'sport') AND " +
            "    LOWER(:sport) != 'inot'" +
            "  )" +
            ")")
    Long countActiveHallsBySport(@Param("sport") String sport);

    // Numărul de săli de înot
    @Query("SELECT COUNT(sh) FROM SportsHall sh WHERE " +
            "sh.status = 'ACTIVE' AND " +
            "(" +
            "  LOWER(sh.type) = 'inot' OR " +
            "  LOWER(sh.type) LIKE '%piscina%' OR " +
            "  LOWER(sh.type) LIKE '%swimming%' OR " +
            "  LOWER(sh.type) LIKE '%inot%'" +
            ")")
    Long countSwimmingHalls();

    // ===== METODE PENTRU TIPURI DE SPORT DISPONIBILE =====

    // Găsește toate tipurile de sport distincte (exclusiv multiple)
    @Query("SELECT DISTINCT sh.type FROM SportsHall sh WHERE " +
            "sh.status = 'ACTIVE' AND " +
            "sh.type IS NOT NULL AND " +
            "LOWER(sh.type) != 'multipla' AND " +
            "LOWER(sh.type) NOT LIKE '%polivalent%' AND " +
            "LOWER(sh.type) NOT LIKE '%multipurpose%' AND " +
            "LOWER(sh.type) NOT LIKE '%universal%' AND " +
            "LOWER(sh.type) != 'sport' " +
            "ORDER BY sh.type")
    List<String> findDistinctSportTypes();

    // Găsește toate tipurile distincte (incluzând polivalente)
    @Query("SELECT DISTINCT sh.type FROM SportsHall sh WHERE " +
            "sh.status = 'ACTIVE' AND " +
            "sh.type IS NOT NULL " +
            "ORDER BY sh.type")
    List<String> findAllDistinctTypes();
}