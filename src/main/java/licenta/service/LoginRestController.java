package licenta.service;

import licenta.jwtservice.JwtService;
import licenta.jwtservice.UserInfoService;
import licenta.model.User;
import licenta.model.dtos.UserCredentials;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/login")
@CrossOrigin(origins = {"http://localhost:3000", "https://trainon.onrender.com"})
public class LoginRestController {

    private final IUserSpringRepository userSpringRepository;

    @Autowired
    private UserInfoService service;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    public LoginRestController(IUserSpringRepository userSpringRepository) {
        this.userSpringRepository = userSpringRepository;
    }

    @PostMapping
    public ResponseEntity<?> login(@RequestBody UserCredentials auth) {
        try {
            String email = auth.getEmail();
            String password = auth.getPassword();

            Optional<User> userOptional = userSpringRepository.findByEmail(email);

            if (!userOptional.isPresent()) {
                return ResponseEntity.status(401).body(createErrorResponse("Email sau parola incorectă"));
            }

            User user = userOptional.get();

            // Verifica parola folosind PasswordEncoder
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(401).body(createErrorResponse("Email sau parola incorectă"));
            }

            // Verifica statusul contului
            if ("rejected".equals(user.getAccountStatus())) {
                return ResponseEntity.status(403).body(createErrorResponse("Contul dumneavoastră a fost respins. Vă rugăm să contactați administratorul."));
            }

            if ("pending".equals(user.getAccountStatus())) {
                return ResponseEntity.status(403).body(createErrorResponse("Contul dumneavoastră este în așteptarea aprobării administratorului."));
            }

            if (!"active".equals(user.getAccountStatus()) && !"verified".equals(user.getAccountStatus())) {
                return ResponseEntity.status(403).body(createErrorResponse("Contul dumneavoastră nu este activ."));
            }

            UserCredentials.getInstance().setLoggedInUser(user);
            return ResponseEntity.ok(createSuccessResponse(user));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("Eroare în timpul autentificării: " + e.getMessage()));
        }
    }

    @PostMapping("/generateToken")
    public ResponseEntity<?> authenticateAndGenerateToken(@RequestBody UserCredentials auth) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(auth.getEmail(), auth.getPassword()));

            if (authentication.isAuthenticated()) {
                Optional<User> userOptional = userSpringRepository.findByEmail(auth.getEmail());
                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    if ("rejected".equals(user.getAccountStatus())) {
                        return ResponseEntity.status(403).body("Contul a fost respins");
                    }

                    if ("pending".equals(user.getAccountStatus())) {
                        return ResponseEntity.status(403).body("Contul este în așteptarea aprobării");
                    }
                }

                String token = jwtService.generateToken(auth.getEmail());
                return ResponseEntity.ok(token);
            } else {
                return ResponseEntity.status(401).body("Autentificare eșuată");
            }
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Email sau parola incorectă");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Eroare în timpul generării token-ului: " + e.getMessage());
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> createSuccessResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("userType", user.getUserType());
        response.put("accountStatus", user.getAccountStatus());
        response.put("teamType", user.getTeamType());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}