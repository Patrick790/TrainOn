package licenta.persistence;

import jakarta.transaction.Transactional;
import licenta.model.ReservationProfile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface IReservationProfileRepository extends CrudRepository<ReservationProfile, Long> {

    /**
     * Găsește toate profilurile care aparțin unui utilizator specific
     * @param userId ID-ul utilizatorului
     * @return Lista de profiluri de rezervare
     */
    List<ReservationProfile> findByUserId(Long userId);
}