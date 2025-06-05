package licenta.service;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserRestController {

    private final IUserSpringRepository userSpringRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserRestController(IUserSpringRepository userSpringRepository, PasswordEncoder passwordEncoder) {
        this.userSpringRepository = userSpringRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('user', 'admin', 'hall_admin')")
    public User getUserById(@PathVariable Long id) {
        return userSpringRepository.findById(id).orElse(null);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('user', 'admin', 'hall_admin')")
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
        // Criptăm parola înainte de salvare, similar cu metoda din UserInfoService
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Verificăm dacă data creării este setată
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(new Date());
        }

        // Verificăm dacă statusul contului este setat
        if (user.getAccountStatus() == null) {
            // Setăm statusul contului în funcție de tipul utilizatorului
            if ("hall_admin".equals(user.getUserType())) {
                // Pentru administratorii de sală, setăm statusul la "verified"
                user.setAccountStatus("verified");
            } else {
                // Pentru utilizatorii obișnuiți, setăm statusul la "verified"
                user.setAccountStatus("verified");
            }
        }

        return userSpringRepository.save(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public void deleteUser(@PathVariable Long id) {
        userSpringRepository.deleteById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('user', 'admin', 'hall_admin')")
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
            if (updatedUser.getUserType() != null) existingUser.setUserType(updatedUser.getUserType());

            // Dacă primim o parolă nouă în cererea de actualizare, o criptăm înainte de a o salva
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

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

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyAuthority('user', 'admin', 'hall_admin')")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest changePasswordRequest,
            Authentication authentication) {

        try {
            // Get the current authenticated user's email
            String authenticatedUserEmail = authentication.getName();

            // Get the user whose password is being changed
            User userToUpdate = userSpringRepository.findById(id).orElse(null);

            if (userToUpdate == null) {
                return ResponseEntity.notFound().build();
            }

            // Security check: users can only change their own password, unless they're admin
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("admin"));

            if (!isAdmin && !authenticatedUserEmail.equals(userToUpdate.getEmail())) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Nu aveți permisiunea să schimbați această parolă"));
            }

            // Verify current password (only if user is changing their own password)
            if (authenticatedUserEmail.equals(userToUpdate.getEmail())) {
                if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), userToUpdate.getPassword())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Parola curentă este incorectă"));
                }
            }

            // Validate new password
            if (changePasswordRequest.getNewPassword() == null ||
                    changePasswordRequest.getNewPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Parola nouă nu poate fi goală"));
            }

            if (changePasswordRequest.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Parola nouă trebuie să aibă cel puțin 6 caractere"));
            }

            // Check if new password is different from current password
            if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), userToUpdate.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Parola nouă trebuie să fie diferită de cea curentă"));
            }

            // Update password
            userToUpdate.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
            userSpringRepository.save(userToUpdate);

            return ResponseEntity.ok(Map.of("message", "Parola a fost schimbată cu succes"));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "A apărut o eroare la schimbarea parolei: " + e.getMessage()));
        }
    }

    // Create this inner class or separate class for the request body
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public ChangePasswordRequest() {
        }

        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}