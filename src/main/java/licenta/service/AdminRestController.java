package licenta.service;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminRestController {

    private final IUserSpringRepository userSpringRepository;

    @Autowired
    public AdminRestController(IUserSpringRepository userSpringRepository) {
        this.userSpringRepository = userSpringRepository;
    }

    // Get all team registrations with the specified status
    @GetMapping("/team-registrations")
    @PreAuthorize("hasAuthority('admin')")
    public List<User> getTeamRegistrations(@RequestParam(required = false) String status) {
        Iterable<User> allUsers = userSpringRepository.findAll();

        // Filter to only include team registrations
        List<User> filteredUsers = StreamSupport.stream(allUsers.spliterator(), false)
                .filter(user -> user.getTeamType() != null && !user.getTeamType().isEmpty())
                .filter(user -> {
                    // Verificăm explicit statusul cerut
                    if (status == null) {
                        return true; // Include toate înregistrările dacă nu este specificat un status
                    }

                    String accountStatus = user.getAccountStatus();

                    // Doar dacă statusul solicitat este "pending" și accountStatus este null
                    // vom trata acei utilizatori ca fiind "pending" (pentru compatibilitate)
                    if ("pending".equals(status) && accountStatus == null) {
                        return true;
                    }

                    // Altfel, verificăm exact potrivirea statusului
                    return status.equals(accountStatus);
                })
                .collect(Collectors.toList());

        return filteredUsers;
    }

    // Approve a team registration
    @PostMapping("/team-registrations/{id}/approve")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> approveTeamRegistration(@PathVariable Long id) {
        Optional<User> userOpt = userSpringRepository.findById(id);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify this is a team registration
            if (user.getTeamType() == null || user.getTeamType().isEmpty()) {
                return ResponseEntity.badRequest().body("This user is not a team");
            }

            user.setAccountStatus("verified");
            userSpringRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Team registration approved successfully"));
        }

        return ResponseEntity.notFound().build();
    }

    // Reject a team registration
    @PostMapping("/team-registrations/{id}/reject")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> rejectTeamRegistration(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        String rejectionReason = payload.get("rejectionReason");

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Rejection reason is required");
        }

        Optional<User> userOpt = userSpringRepository.findById(id);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify this is a team registration
            if (user.getTeamType() == null || user.getTeamType().isEmpty()) {
                return ResponseEntity.badRequest().body("This user is not a team");
            }

            user.setAccountStatus("rejected");
            user.setRejectionReason(rejectionReason);
            userSpringRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Team registration rejected successfully"));
        }

        return ResponseEntity.notFound().build();
    }

    // FCFS: Get FCFS status - verifică primul utilizator normal
    @GetMapping("/fcfs-status")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Map<String, Object>> getFcfsStatus() {
        // Verificăm statusul FCFS prin primul utilizator normal găsit
        Optional<User> normalUser = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                .filter(user -> "user".equals(user.getUserType()))
                .findFirst();

        if (normalUser.isPresent()) {
            boolean fcfsEnabled = normalUser.get().isFcfsEnabled();

            // Contorizăm câți utilizatori au FCFS activat/dezactivat
            long totalUsers = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                    .filter(user -> "user".equals(user.getUserType()))
                    .count();

            long enabledUsers = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                    .filter(user -> "user".equals(user.getUserType()) && user.isFcfsEnabled())
                    .count();

            return ResponseEntity.ok(Map.of(
                    "fcfsEnabled", fcfsEnabled,
                    "message", fcfsEnabled ?
                            "FCFS este activat pentru utilizatori" :
                            "FCFS este dezactivat pentru utilizatori",
                    "totalUsers", totalUsers,
                    "enabledUsers", enabledUsers,
                    "disabledUsers", totalUsers - enabledUsers
            ));
        }

        // Dacă nu avem utilizatori normali, returnăm statusul default
        return ResponseEntity.ok(Map.of(
                "fcfsEnabled", true,
                "message", "Nu există utilizatori normali în sistem",
                "totalUsers", 0,
                "enabledUsers", 0,
                "disabledUsers", 0
        ));
    }

    // FCFS: Toggle FCFS pentru TOȚI utilizatorii normali
    @PostMapping("/fcfs-toggle")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Map<String, Object>> toggleFcfsVisibility() {
        try {
            // Găsim toți utilizatorii cu userType = "user"
            List<User> normalUsers = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                    .filter(user -> "user".equals(user.getUserType()))
                    .collect(Collectors.toList());

            if (normalUsers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Nu există utilizatori normali în sistem pentru a gestiona FCFS"
                ));
            }

            // Determinăm statusul curent (din primul utilizator)
            boolean currentStatus = normalUsers.get(0).isFcfsEnabled();
            boolean newStatus = !currentStatus;

            // Actualizăm TOȚI utilizatorii normali cu noul status
            normalUsers.forEach(user -> user.setFcfsEnabled(newStatus));
            userSpringRepository.saveAll(normalUsers);

            String message = newStatus ?
                    "FCFS (Rezervarea locurilor rămase) a fost ACTIVAT pentru toți utilizatorii" :
                    "FCFS (Rezervarea locurilor rămase) a fost DEZACTIVAT pentru toți utilizatorii";

            return ResponseEntity.ok(Map.of(
                    "fcfsEnabled", newStatus,
                    "message", message,
                    "previousStatus", currentStatus,
                    "updatedUsers", normalUsers.size(),
                    "affectedUserIds", normalUsers.stream().map(User::getId).collect(Collectors.toList())
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Eroare la actualizarea statusului FCFS: " + e.getMessage()
                    ));
        }
    }

    // FCFS: Set FCFS status explicit pentru TOȚI utilizatorii normali
    @PostMapping("/fcfs-status")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Map<String, Object>> setFcfsStatus(@RequestBody Map<String, Boolean> payload) {
        try {
            Boolean enabled = payload.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Parametrul 'enabled' este obligatoriu"
                ));
            }

            // Găsim toți utilizatorii cu userType = "user"
            List<User> normalUsers = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                    .filter(user -> "user".equals(user.getUserType()))
                    .collect(Collectors.toList());

            if (normalUsers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Nu există utilizatori normali în sistem pentru a gestiona FCFS"
                ));
            }

            // Determinăm statusul curent
            boolean previousStatus = normalUsers.get(0).isFcfsEnabled();

            // Actualizăm TOȚI utilizatorii normali cu statusul specificat
            normalUsers.forEach(user -> user.setFcfsEnabled(enabled));
            userSpringRepository.saveAll(normalUsers);

            String message = enabled ?
                    "FCFS (Rezervarea locurilor rămase) a fost ACTIVAT pentru toți utilizatorii" :
                    "FCFS (Rezervarea locurilor rămase) a fost DEZACTIVAT pentru toți utilizatorii";

            // Calculăm dacă s-a schimbat ceva
            boolean changed = previousStatus != enabled;

            return ResponseEntity.ok(Map.of(
                    "fcfsEnabled", enabled,
                    "message", message,
                    "previousStatus", previousStatus,
                    "changed", changed,
                    "updatedUsers", normalUsers.size(),
                    "affectedUserIds", normalUsers.stream().map(User::getId).collect(Collectors.toList())
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Eroare la setarea statusului FCFS: " + e.getMessage()
                    ));
        }
    }

    // FCFS: Endpoint pentru a obține statistici detaliate
    @GetMapping("/fcfs-statistics")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Map<String, Object>> getFcfsStatistics() {
        try {
            List<User> allUsers = StreamSupport.stream(userSpringRepository.findAll().spliterator(), false)
                    .collect(Collectors.toList());

            List<User> normalUsers = allUsers.stream()
                    .filter(user -> "user".equals(user.getUserType()))
                    .collect(Collectors.toList());

            List<User> adminUsers = allUsers.stream()
                    .filter(user -> "admin".equals(user.getUserType()))
                    .collect(Collectors.toList());

            List<User> enabledNormalUsers = normalUsers.stream()
                    .filter(User::isFcfsEnabled)
                    .collect(Collectors.toList());

            List<User> disabledNormalUsers = normalUsers.stream()
                    .filter(user -> !user.isFcfsEnabled())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "totalUsers", allUsers.size(),
                    "normalUsers", normalUsers.size(),
                    "adminUsers", adminUsers.size(),
                    "enabledNormalUsers", enabledNormalUsers.size(),
                    "disabledNormalUsers", disabledNormalUsers.size(),
                    "fcfsGlobalStatus", normalUsers.isEmpty() ? true : normalUsers.get(0).isFcfsEnabled(),
                    "lastUpdated", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Eroare la obținerea statisticilor FCFS: " + e.getMessage()
                    ));
        }
    }

    // Endpoint pentru a servi certificatul ca imagine
    @GetMapping("/team-registrations/{id}/certificate")
    public ResponseEntity<byte[]> getTeamCertificate(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verifică autorizarea
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> userOpt = userSpringRepository.findById(id);
        if (!userOpt.isPresent() || userOpt.get().getCertificate() == null) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        byte[] certificateData = user.getCertificate();

        if (certificateData.length == 0) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        String contentType = detectContentType(certificateData);
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setCacheControl("max-age=3600");

        System.out.println("Serving certificate for team " + id + ", type: " + contentType);
        return new ResponseEntity<>(certificateData, headers, HttpStatus.OK);
    }

    // Metodă pentru a detecta tipul de conținut
    private String detectContentType(byte[] data) {
        if (data.length > 4) {
            // JPEG file magic number
            if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
                return "image/jpeg";
            }
            // PNG file magic number
            if (data[0] == (byte) 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
                return "image/png";
            }
            // PDF file signature
            if (data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F') {
                return "application/pdf";
            }
        }
        return "application/octet-stream"; // Default fallback
    }
}