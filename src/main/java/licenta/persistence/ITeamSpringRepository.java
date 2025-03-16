package licenta.persistence;

import licenta.model.Team;
import org.springframework.data.repository.CrudRepository;

public interface ITeamSpringRepository extends CrudRepository<Team, Long> {
}
