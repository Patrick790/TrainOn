package licenta.service;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/fcfs")
@CrossOrigin(origins = "http://localhost:3000")
public class FcfsStatusController {

    private final IUserSpringRepository userSpringRepository;

    @Autowired
    public FcfsStatusController(IUserSpringRepository userSpringRepository) {
        this.userSpringRepository = userSpringRepository;
    }

    /**
     * Endpoint public pentru a verifica dacă FCFS este activat
     * Poate fi accesat de orice utilizator
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFcfsStatus() {
        try {
            // Verificăm statusul FCFS prin primul utilizator normal găsit
            // (toți utilizatorii normali ar trebui să aibă același status)
            Optional<User> normalUser = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                    .filter(user -> "user".equals(user.getUserType()))
                    .findFirst();

            boolean fcfsEnabled = normalUser.map(User::isFcfsEnabled).orElse(true); // Default activat dacă nu există utilizatori

            return ResponseEntity.ok(Map.of(
                    "fcfsEnabled", fcfsEnabled,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            // În caz de eroare, returnăm că FCFS este activat (comportament fail-safe)
            return ResponseEntity.ok(Map.of(
                    "fcfsEnabled", true,
                    "timestamp", System.currentTimeMillis(),
                    "note", "Error occurred, returning default enabled status"
            ));
        }
    }

    /**
     * Endpoint pentru a verifica dacă utilizatorul curent poate accesa FCFS
     * Include verificări specifice pentru tipul de utilizator
     */
    @GetMapping("/access-check")
    public ResponseEntity<Map<String, Object>> checkFcfsAccess(Authentication authentication) {
        try {
            // Verificăm dacă utilizatorul este autentificat
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(Map.of(
                        "canAccess", false,
                        "reason", "Nu sunteți autentificat",
                        "fcfsEnabled", false
                ));
            }

            // Obținem utilizatorul curent prin email-ul din authentication
            String userEmail = authentication.getName();
            Optional<User> currentUserOpt = userSpringRepository.findByEmail(userEmail);

            if (!currentUserOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "canAccess", false,
                        "reason", "Utilizatorul nu a fost găsit în sistem",
                        "fcfsEnabled", false
                ));
            }

            User currentUser = currentUserOpt.get();
            String userType = currentUser.getUserType();

            // Verificăm statusul global FCFS (din primul utilizator normal)
            Optional<User> normalUser = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                    .filter(user -> "user".equals(user.getUserType()))
                    .findFirst();

            boolean fcfsGloballyEnabled = normalUser.map(User::isFcfsEnabled).orElse(true);

            // Logica de acces bazată pe tipul de utilizator
            if ("admin".equals(userType)) {
                // Adminii pot accesa întotdeauna pentru management
                return ResponseEntity.ok(Map.of(
                        "canAccess", true,
                        "fcfsEnabled", fcfsGloballyEnabled,
                        "userType", userType,
                        "reason", fcfsGloballyEnabled ?
                                "Admin access - FCFS este activat pentru utilizatori" :
                                "Admin access - FCFS este dezactivat pentru utilizatori"
                ));
            } else if ("user".equals(userType)) {
                // Utilizatorii normali pot accesa doar dacă au FCFS activat
                boolean userCanAccess = currentUser.isFcfsEnabled();

                return ResponseEntity.ok(Map.of(
                        "canAccess", userCanAccess,
                        "fcfsEnabled", fcfsGloballyEnabled,
                        "userType", userType,
                        "reason", userCanAccess ?
                                "Puteți rezerva locurile rămase" :
                                "Rezervarea locurilor rămase a fost dezactivată pentru contul dumneavoastră"
                ));
            } else {
                // Pentru alte tipuri de utilizatori (hall_admin, etc.)
                return ResponseEntity.ok(Map.of(
                        "canAccess", false,
                        "fcfsEnabled", fcfsGloballyEnabled,
                        "userType", userType,
                        "reason", "Nu aveți permisiunea să accesați această funcționalitate"
                ));
            }

        } catch (Exception e) {
            // În caz de eroare, returnăm că utilizatorul poate accesa (fail-safe)
            return ResponseEntity.ok(Map.of(
                    "canAccess", true,
                    "fcfsEnabled", true,
                    "note", "Error occurred, returning default access granted"
            ));
        }
    }

    /**
     * Endpoint pentru a obține informații detaliate despre statusul FCFS pentru utilizatorul curent
     */
    @GetMapping("/user-status")
    public ResponseEntity<Map<String, Object>> getUserFcfsStatus(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(Map.of(
                        "error", "Nu sunteți autentificat"
                ));
            }

            String userEmail = authentication.getName();
            Optional<User> currentUserOpt = userSpringRepository.findByEmail(userEmail);

            if (!currentUserOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "error", "Utilizatorul nu a fost găsit"
                ));
            }

            User currentUser = currentUserOpt.get();

            // Statusul global FCFS
            Optional<User> normalUser = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                    .filter(user -> "user".equals(user.getUserType()))
                    .findFirst();

            boolean fcfsGloballyEnabled = normalUser.map(User::isFcfsEnabled).orElse(true);

            return ResponseEntity.ok(Map.of(
                    "userId", currentUser.getId(),
                    "userType", currentUser.getUserType(),
                    "userFcfsEnabled", currentUser.isFcfsEnabled(),
                    "globalFcfsEnabled", fcfsGloballyEnabled,
                    "canAccess", "admin".equals(currentUser.getUserType()) || currentUser.isFcfsEnabled(),
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Eroare la obținerea statusului utilizatorului: " + e.getMessage()
            ));
        }
    }
}