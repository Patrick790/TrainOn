package licenta.service;

import licenta.model.ReservationProfile;
import licenta.model.SportsHall;
import licenta.model.User;
import licenta.persistence.IReservationProfileRepository;
import licenta.persistence.ISportsHallSpringRepository;
import licenta.persistence.IUserSpringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/reservationProfiles")
@CrossOrigin(origins = "http://localhost:3000")
public class ReservationProfileRestController {

    private final IReservationProfileRepository reservationProfileRepository;
    private final IUserSpringRepository userSpringRepository;
    private final ISportsHallSpringRepository sportsHallSpringRepository;
    private static final Logger logger = LoggerFactory.getLogger(ReservationProfileRestController.class);

    @Autowired
    public ReservationProfileRestController(
            IReservationProfileRepository reservationProfileRepository,
            IUserSpringRepository userSpringRepository,
            ISportsHallSpringRepository sportsHallSpringRepository) {
        this.reservationProfileRepository = reservationProfileRepository;
        this.userSpringRepository = userSpringRepository;
        this.sportsHallSpringRepository = sportsHallSpringRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllProfiles() {
        try {
            // Obținem utilizatorul curent autentificat
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Optional<User> userOpt = userSpringRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Returnăm profilurile utilizatorului
                List<ReservationProfile> userProfiles = reservationProfileRepository.findByUserId(user.getId());
                return ResponseEntity.ok(userProfiles);
            } else {
                return ResponseEntity.status(404).body("Utilizator negăsit");
            }
        } catch (Exception e) {
            logger.error("Error fetching profiles", e);
            return ResponseEntity.internalServerError().body("Eroare la obținerea profilurilor");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProfileById(@PathVariable Long id) {
        try {
            Optional<ReservationProfile> profileOpt = reservationProfileRepository.findById(id);
            if (profileOpt.isPresent()) {
                return ResponseEntity.ok(profileOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching profile", e);
            return ResponseEntity.internalServerError().body("Eroare la obținerea profilului");
        }
    }

    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody Map<String, Object> profileData) {
        try {
            // Obținem utilizatorul curent
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Optional<User> userOpt = userSpringRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Creăm profilul
                ReservationProfile profile = new ReservationProfile();
                profile.setName((String) profileData.get("name"));
                profile.setAgeCategory((String) profileData.get("ageCategory"));
                profile.setTimeInterval((String) profileData.get("timeInterval"));
                profile.setWeeklyBudget(new BigDecimal(profileData.get("weeklyBudget").toString()));
                profile.setCity((String) profileData.get("city"));
                profile.setUser(user);

                // Procesăm sălile selectate
                List<Integer> hallIds = (List<Integer>) profileData.get("selectedHalls");
                List<Long> longHallIds = new ArrayList<>();
                for (Integer id : hallIds) {
                    longHallIds.add(id.longValue());
                }

                List<SportsHall> halls = (List<SportsHall>) sportsHallSpringRepository.findAllById(longHallIds);
                profile.setSelectedHalls(halls);

                // Salvăm profilul
                ReservationProfile savedProfile = reservationProfileRepository.save(profile);
                return ResponseEntity.ok(savedProfile);
            } else {
                return ResponseEntity.status(404).body("Utilizator negăsit");
            }
        } catch (Exception e) {
            logger.error("Error creating profile", e);
            return ResponseEntity.internalServerError().body("Eroare la crearea profilului");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Map<String, Object> profileData) {
        try {
            Optional<ReservationProfile> profileOpt = reservationProfileRepository.findById(id);
            if (profileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ReservationProfile profile = profileOpt.get();

            // Actualizăm datele profilului
            if (profileData.containsKey("name")) {
                profile.setName((String) profileData.get("name"));
            }

            if (profileData.containsKey("ageCategory")) {
                profile.setAgeCategory((String) profileData.get("ageCategory"));
            }

            if (profileData.containsKey("timeInterval")) {
                profile.setTimeInterval((String) profileData.get("timeInterval"));
            }

            if (profileData.containsKey("weeklyBudget")) {
                profile.setWeeklyBudget(new BigDecimal(profileData.get("weeklyBudget").toString()));
            }

            if (profileData.containsKey("city")) {
                profile.setCity((String) profileData.get("city"));
            }

            // Actualizăm sălile selectate dacă există
            if (profileData.containsKey("selectedHalls")) {
                List<Integer> hallIds = (List<Integer>) profileData.get("selectedHalls");
                List<Long> longHallIds = new ArrayList<>();
                for (Integer hallId : hallIds) {
                    longHallIds.add(hallId.longValue());
                }

                List<SportsHall> halls = (List<SportsHall>) sportsHallSpringRepository.findAllById(longHallIds);
                profile.setSelectedHalls(halls);
            }

            // Salvăm profilul actualizat
            ReservationProfile updatedProfile = reservationProfileRepository.save(profile);
            return ResponseEntity.ok(updatedProfile);

        } catch (Exception e) {
            logger.error("Error updating profile", e);
            return ResponseEntity.internalServerError().body("Eroare la actualizarea profilului");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        try {
            if (reservationProfileRepository.existsById(id)) {
                reservationProfileRepository.deleteById(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting profile", e);
            return ResponseEntity.internalServerError().body("Eroare la ștergerea profilului");
        }
    }
}