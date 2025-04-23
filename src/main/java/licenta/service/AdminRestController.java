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
        // Nu adăuga manual Access-Control-Allow-Origin

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