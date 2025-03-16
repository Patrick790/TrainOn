package licenta.persistence;

import licenta.model.Reservation;
import org.springframework.data.repository.CrudRepository;

public interface IReservationSpringRepository extends CrudRepository<Reservation, Long> {

}
