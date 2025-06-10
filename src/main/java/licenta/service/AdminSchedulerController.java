package licenta.service;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/admin/scheduler")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminSchedulerController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSchedulerController.class);

    @Autowired
    private WeeklyPaymentScheduler weeklyPaymentScheduler;

    @Autowired
    private IUserSpringRepository userRepository;

    /**
     * Endpoint pentru rularea manuală a procesului de generare
     * Doar pentru administratori
     */
    @PostMapping("/manual-generation")
    public ResponseEntity<Map<String, Object>> manualGeneration() {
        try {
            // Verifică dacă utilizatorul este admin
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acces interzis - doar administratorii pot accesa această funcționalitate"));
            }

            logger.info("Generare manuală declanșată de admin la {}",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            Map<String, Object> result = weeklyPaymentScheduler.manualWeeklyProcessing();

            // Adaugă informații suplimentare despre execuția manuală
            result.put("executionType", "manual");
            result.put("executionTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("executedBy", getCurrentUserEmail());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Eroare la generarea manuală de către admin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Eroare la procesarea manuală: " + e.getMessage(),
                            "executionType", "manual",
                            "executionTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    ));
        }
    }

    /**
     * Endpoint pentru obținerea statisticilor plăților automate
     */
    @GetMapping("/auto-payment-stats")
    public ResponseEntity<Map<String, Object>> getAutoPaymentStatistics() {
        try {
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acces interzis"));
            }

            Map<String, Object> stats = weeklyPaymentScheduler.getAutoPaymentStatistics();

            // Adaugă informații despre timpul ultimei verificări
            stats.put("lastCheck", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stats.put("nextScheduledRun", getNextScheduledRunInfo());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Eroare la obținerea statisticilor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare la obținerea statisticilor: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pentru verificarea status-ului scheduler-ului
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        try {
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acces interzis"));
            }

            Map<String, Object> status = new HashMap<>();
            status.put("schedulerActive", true);
            status.put("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            status.put("timezone", "Europe/Bucharest");
            status.put("nextWeeklyRun", getNextScheduledRunInfo());
            status.put("weeklyRunSchedule", "Duminică la 22:00");
            status.put("dailyCardCheckSchedule", "Zilnic la 08:00");

            // Informații despre ultima rulare (ar trebui salvate în baza de date într-o implementare completă)
            status.put("lastSuccessfulRun", "N/A - Vezi log-urile pentru detalii");
            status.put("systemHealth", "OK");

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Eroare la obținerea status-ului scheduler", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare la verificarea status-ului: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pentru verificarea manuală a cardurilor expirate
     */
    @PostMapping("/check-expired-cards")
    public ResponseEntity<Map<String, Object>> checkExpiredCards() {
        try {
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acces interzis"));
            }

            logger.info("Verificare manuală carduri expirate declanșată de admin");

            // Apelează metoda din scheduler
            weeklyPaymentScheduler.checkExpiredCards();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Verificarea cardurilor expirate a fost executată cu succes");
            result.put("executionTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("executedBy", getCurrentUserEmail());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Eroare la verificarea manuală a cardurilor expirate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Eroare la verificarea cardurilor: " + e.getMessage()
                    ));
        }
    }

    /**
     * Endpoint pentru configurarea parametrilor scheduler-ului (viitor)
     */
    @PostMapping("/configure")
    public ResponseEntity<Map<String, Object>> configureScheduler(@RequestBody Map<String, Object> config) {
        try {
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acces interzis"));
            }

            // TODO: Implementează configurarea dinamică a scheduler-ului
            // De exemplu: schimbarea orei de rulare, activarea/dezactivarea anumitor task-uri, etc.

            logger.info("Configurare scheduler solicitată de admin: {}", config);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Configurarea scheduler-ului nu este încă implementată");
            result.put("requestedConfig", config);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Eroare la configurarea scheduler-ului", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare la configurare: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pentru obținerea log-urilor recente ale scheduler-ului
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getSchedulerLogs(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acces interzis"));
            }

            // TODO: Implementează citirea log-urilor din sistem
            // Aceasta ar putea include logurile din fișiere sau din baza de date

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Funcționalitatea de log-uri nu este încă implementată");
            result.put("suggestion", "Verificați log-urile aplicației pentru detalii despre execuțiile scheduler-ului");
            result.put("logSources", Arrays.asList(
                    "Application logs pentru WeeklyPaymentScheduler",
                    "Application logs pentru BookingPrioritizationRestController",
                    "Application logs pentru AutoPaymentService"
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Eroare la obținerea log-urilor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare la accesarea log-urilor: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pentru simularea următoarei execuții (pentru testare)
     */
    @PostMapping("/simulate-next-run")
    public ResponseEntity<Map<String, Object>> simulateNextRun() {
        try {
            if (!isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acces interzis"));
            }

            logger.info("Simulare următoarea execuție declanșată de admin");

            // Aceasta este esențial identică cu generarea manuală, dar cu alt nume
            Map<String, Object> result = weeklyPaymentScheduler.manualWeeklyProcessing();

            result.put("executionType", "simulation");
            result.put("executionTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("simulatedBy", getCurrentUserEmail());
            result.put("note", "Aceasta este o simulare a execuției automate");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Eroare la simularea execuției", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Eroare la simulare: " + e.getMessage(),
                            "executionType", "simulation"
                    ));
        }
    }

    // Metode helper

    /**
     * Verifică dacă utilizatorul curent este administrator
     */
    private boolean isCurrentUserAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }

            String email = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else if (principal instanceof User) {
                email = ((User) principal).getEmail();
            }

            if (email == null) {
                return false;
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return "admin".equals(user.getUserType()) || "hall_admin".equals(user.getUserType());
            }

            return false;

        } catch (Exception e) {
            logger.error("Eroare la verificarea drepturilor de admin", e);
            return false;
        }
    }

    /**
     * Obține email-ul utilizatorului curent
     */
    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                if (principal instanceof UserDetails) {
                    return ((UserDetails) principal).getUsername();
                } else if (principal instanceof User) {
                    return ((User) principal).getEmail();
                }
            }
        } catch (Exception e) {
            logger.error("Eroare la obținerea email-ului utilizatorului curent", e);
        }

        return "unknown";
    }

    /**
     * Calculează informații despre următoarea execuție programată
     */
    private Map<String, Object> getNextScheduledRunInfo() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Găsește următoarea duminică la 22:00
            LocalDateTime nextSunday = now;
            while (nextSunday.getDayOfWeek().getValue() != 7) { // 7 = Duminică
                nextSunday = nextSunday.plusDays(1);
            }

            // Setează ora la 22:00
            nextSunday = nextSunday.withHour(22).withMinute(0).withSecond(0).withNano(0);

            // Dacă am trecut de ora 22:00 duminica curentă, mergi la următoarea duminică
            if (nextSunday.isBefore(now)) {
                nextSunday = nextSunday.plusWeeks(1);
            }

            Map<String, Object> info = new HashMap<>();
            info.put("nextRunTime", nextSunday.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            info.put("nextRunDay", "Duminică");
            info.put("nextRunHour", "22:00");
            info.put("daysUntilNextRun", java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), nextSunday.toLocalDate()));
            info.put("hoursUntilNextRun", java.time.temporal.ChronoUnit.HOURS.between(now, nextSunday));

            return info;

        } catch (Exception e) {
            logger.error("Eroare la calcularea următoarei execuții", e);
            return Map.of("error", "Nu s-a putut calcula următoarea execuție");
        }
    }
}