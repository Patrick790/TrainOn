package licenta.service;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserRestController {

    private final IUserSpringRepository userSpringRepository;

    @Autowired
    public UserRestController(IUserSpringRepository userSpringRepository) {
        this.userSpringRepository = userSpringRepository;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public User getUserById(@PathVariable Long id) {
        return userSpringRepository.findById(id).orElse(null);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public Iterable<User> getAllUsers() {
        return userSpringRepository.findAll();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userSpringRepository.save(user);
    }


    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userSpringRepository.deleteById(id);
    }

}
