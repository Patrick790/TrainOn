package licenta.persistence;

import licenta.model.Feedback;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface IFeedbackSpringRepository extends CrudRepository<Feedback, Long> {
    // Metoda pentru a obtine toate feedback-urile pentru o sala specifica
    List<Feedback> findByHallId(Long hallId);

    // Metodă pentru a obtine feedback-urile pentru o sala, ordonate dupa data (descendent)
    List<Feedback> findByHallIdOrderByDateDesc(Long hallId);

    // Metoda pentru a obtine feedback-urile pentru o sala, ordonate dupa rating (descendent)
    List<Feedback> findByHallIdOrderByRatingDesc(Long hallId);

    // Metoda pentru a obtine feedback-urile pentru o sala, ordonate dupa rating (ascendent)
    List<Feedback> findByHallIdOrderByRatingAsc(Long hallId);
}
