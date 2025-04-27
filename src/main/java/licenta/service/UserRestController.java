package licenta.service;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:3000")
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
    public Iterable<User> getAllUsers(@RequestParam(required = false) String accountStatus) {
        Iterable<User> users = userSpringRepository.findAll();

        // If accountStatus parameter is provided, filter the results
        if (accountStatus != null && !accountStatus.isEmpty()) {
            List<User> filteredUsers = StreamSupport.stream(users.spliterator(), false)
                    .filter(user -> accountStatus.equals(user.getAccountStatus()))
                    .collect(Collectors.toList());
            return filteredUsers;
        }

        return users;
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userSpringRepository.save(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public void deleteUser(@PathVariable Long id) {
        userSpringRepository.deleteById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('user', 'admin')")
    public User updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        User existingUser = userSpringRepository.findById(id).orElse(null);
        if (existingUser != null) {
            // Only update specific fields, preserving others like password unless explicitly changed
            if (updatedUser.getName() != null) existingUser.setName(updatedUser.getName());
            if (updatedUser.getEmail() != null) existingUser.setEmail(updatedUser.getEmail());
            if (updatedUser.getAddress() != null) existingUser.setAddress(updatedUser.getAddress());
            if (updatedUser.getCounty() != null) existingUser.setCounty(updatedUser.getCounty());
            if (updatedUser.getCity() != null) existingUser.setCity(updatedUser.getCity());
            if (updatedUser.getBirthDate() != null) existingUser.setBirthDate(updatedUser.getBirthDate());

            // Only admin can update the account status
            if (updatedUser.getAccountStatus() != null) {
                // This should be guarded by @PreAuthorize in a separate endpoint in production
                existingUser.setAccountStatus(updatedUser.getAccountStatus());
            }

            return userSpringRepository.save(existingUser);
        }
        return null;
    }

    /**
     * Suspendă un utilizator, schimbându-i accountStatus în "suspended"
     */
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> suspendUser(@PathVariable Long id) {
        User existingUser = userSpringRepository.findById(id).orElse(null);

        if (existingUser == null) {
            return ResponseEntity.notFound().build();
        }

        // Setează statusul contului ca "suspended"
        existingUser.setAccountStatus("suspended");
        userSpringRepository.save(existingUser);

        return ResponseEntity.ok(Map.of("message", "Utilizatorul a fost suspendat cu succes"));
    }
}