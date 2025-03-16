package licenta.persistence;

import licenta.model.Feedback;
import org.springframework.data.repository.CrudRepository;

public interface IFeedbackSpringRepository extends CrudRepository<Feedback, Long> {
}
