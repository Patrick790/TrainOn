package licenta.service;

import licenta.model.Team;
import licenta.persistence.ITeamSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teams")
public class TeamRestController {

    private final ITeamSpringRepository teamSpringRepository;

    @Autowired
    public TeamRestController(ITeamSpringRepository teamSpringRepository) {
        this.teamSpringRepository = teamSpringRepository;
    }

    @GetMapping("/{id}")
    public Team getTeamById(@PathVariable Long id) {
        return teamSpringRepository.findById(id).orElse(null);
    }

    @GetMapping
    public Iterable<Team> getAllTeams() {
        return teamSpringRepository.findAll();
    }

    @PostMapping
    public Team createTeam(@RequestBody Team team) {
        return teamSpringRepository.save(team);
    }

    @DeleteMapping("/{id}")
    public void deleteTeam(@PathVariable Long id) {
        teamSpringRepository.deleteById(id);
    }
}
