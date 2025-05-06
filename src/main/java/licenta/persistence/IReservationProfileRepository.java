package licenta.persistence;

import licenta.model.ReservationProfile;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IReservationProfileRepository extends CrudRepository<ReservationProfile, Long> {

    /**
     * Găsește toate profilurile care aparțin unui utilizator specific
     * @param userId ID-ul utilizatorului
     * @return Lista de profiluri de rezervare
     */
    List<ReservationProfile> findByUserId(Long userId);
}