package licenta.persistence;

import licenta.model.Feedback;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IFeedbackSpringRepository extends CrudRepository<Feedback, Long> {
    // Metodă pentru a obține toate feedback-urile pentru o sală specifică
    List<Feedback> findByHallId(Long hallId);

    // Metodă pentru a obține feedback-urile pentru o sală, ordonate după dată (descendent)
    List<Feedback> findByHallIdOrderByDateDesc(Long hallId);

    // Metodă pentru a obține feedback-urile pentru o sală, ordonate după rating (descendent)
    List<Feedback> findByHallIdOrderByRatingDesc(Long hallId);

    // Metodă pentru a obține feedback-urile pentru o sală, ordonate după rating (ascendent)
    List<Feedback> findByHallIdOrderByRatingAsc(Long hallId);
}
