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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/login")
@CrossOrigin(origins = "http://localhost:3000")
public class LoginRestController {

    private final IUserSpringRepository userSpringRepository;

    @Autowired
    private UserInfoService service;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    public LoginRestController(IUserSpringRepository userSpringRepository) {
        this.userSpringRepository = userSpringRepository;
    }

    @PostMapping
    public ResponseEntity<?> login(@RequestBody UserCredentials auth) {
        try {
            String email = auth.getEmail();
            String password = auth.getPassword();
            Optional<User> user = userSpringRepository.findByEmail(email);

            if (user.isPresent()) {
                UserCredentials.getInstance().setLoggedInUser(user.get());
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(404).body("User not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during login: " + e.getMessage());
        }
    }

    @PostMapping("/generateToken")
    public String authenticateAndGenerateToken(@RequestBody UserCredentials auth) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(auth.getEmail(), auth.getPassword()));

            if (authentication.isAuthenticated()) {
                return jwtService.generateToken(auth.getEmail());
            } else {
                throw new BadCredentialsException("Authentication failed");
            }
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid credentials");
        }
    }
}