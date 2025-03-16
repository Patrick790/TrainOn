package licenta.persistence;

import licenta.model.SportsHallImage;
import org.springframework.data.repository.CrudRepository;

public interface ISportsHallImageSpringRepository extends CrudRepository<SportsHallImage, Long> {
}
