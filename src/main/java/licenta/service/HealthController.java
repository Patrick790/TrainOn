package licenta.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://trainon.onrender.com"})
public class HealthController {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "TrainOn API");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("port", serverPort);
        response.put("profile", activeProfile);
        return response;
    }

    @GetMapping("/api")
    public Map<String, Object> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "TrainOn API is running successfully");
        response.put("status", "OK");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("endpoints", Map.of(
                "health", "/health",
                "login", "/login",
                "register", "/register"
        ));
        return response;
    }

    @GetMapping("/booking-prioritization/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Controller is working!");
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "response", "pong",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}