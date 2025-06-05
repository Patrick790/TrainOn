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
    public ResponseEntity<?> getAllProfiles(@RequestParam(required = false) Long userId) {
        try {
            // Obținem utilizatorul curent autentificat
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Optional<User> userOpt = userSpringRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User currentUser = userOpt.get();

                // Dacă s-a specificat un userId și utilizatorul curent este admin
                if (userId != null) {
                    // Verificăm dacă utilizatorul curent este admin sau hall_admin
                    if (!"admin".equals(currentUser.getUserType()) && !"hall_admin".equals(currentUser.getUserType())) {
                        return ResponseEntity.status(403).body("Acces interzis - doar adminii pot accesa această funcționalitate");
                    }

                    // Verificăm dacă utilizatorul țintă există
                    Optional<User> targetUserOpt = userSpringRepository.findById(userId);
                    if (targetUserOpt.isEmpty()) {
                        return ResponseEntity.status(404).body("Utilizatorul specificat nu a fost găsit");
                    }

                    // Returnăm profilurile utilizatorului specificat
                    List<ReservationProfile> userProfiles = reservationProfileRepository.findByUserId(userId);
                    return ResponseEntity.ok(userProfiles);
                } else {
                    // Dacă nu s-a specificat userId, returnăm profilurile utilizatorului curent
                    List<ReservationProfile> userProfiles = reservationProfileRepository.findByUserId(currentUser.getId());
                    return ResponseEntity.ok(userProfiles);
                }
            } else {
                return ResponseEntity.status(404).body("Utilizator negăsit");
            }
        } catch (Exception e) {
            logger.error("Error fetching profiles", e);
            return ResponseEntity.internalServerError().body("Eroare la obținerea profilurilor");
        }
    }

    // ENDPOINT PĂSTRAT PENTRU COMPATIBILITATE - pentru admin să obțină profilurile unui utilizator specific
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProfilesByUserId(@PathVariable Long userId) {
        try {
            // Verificăm dacă utilizatorul curent este admin
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            Optional<User> currentUserOpt = userSpringRepository.findByEmail(currentUserEmail);

            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Utilizator neautentificat");
            }

            User currentUser = currentUserOpt.get();

            // Verificăm dacă utilizatorul curent este admin sau hall_admin
            if (!"admin".equals(currentUser.getUserType()) && !"hall_admin".equals(currentUser.getUserType())) {
                return ResponseEntity.status(403).body("Acces interzis - doar adminii pot accesa această funcționalitate");
            }

            // Verificăm dacă utilizatorul țintă există
            Optional<User> targetUserOpt = userSpringRepository.findById(userId);
            if (targetUserOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Utilizatorul specificat nu a fost găsit");
            }

            // Returnăm profilurile utilizatorului specificat
            List<ReservationProfile> userProfiles = reservationProfileRepository.findByUserId(userId);
            return ResponseEntity.ok(userProfiles);

        } catch (Exception e) {
            logger.error("Error fetching profiles for user " + userId, e);
            return ResponseEntity.internalServerError().body("Eroare la obținerea profilurilor utilizatorului");
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

                // Verificăm dacă weeklyBudget există și nu este null
                if (profileData.get("weeklyBudget") != null && !profileData.get("weeklyBudget").toString().isEmpty()) {
                    profile.setWeeklyBudget(new BigDecimal(profileData.get("weeklyBudget").toString()));
                }

                profile.setCity((String) profileData.get("city"));
                profile.setUser(user);

                // Procesăm sălile selectate dacă există
                if (profileData.get("selectedHalls") != null) {
                    List<Integer> hallIds = (List<Integer>) profileData.get("selectedHalls");
                    if (!hallIds.isEmpty()) {
                        List<Long> longHallIds = new ArrayList<>();
                        for (Integer id : hallIds) {
                            longHallIds.add(id.longValue());
                        }

                        List<SportsHall> halls = (List<SportsHall>) sportsHallSpringRepository.findAllById(longHallIds);
                        profile.setSelectedHalls(halls);
                    }
                }

                // Salvăm profilul
                ReservationProfile savedProfile = reservationProfileRepository.save(profile);
                return ResponseEntity.ok(savedProfile);
            } else {
                return ResponseEntity.status(404).body("Utilizator negăsit");
            }
        } catch (Exception e) {
            logger.error("Error creating profile", e);
            return ResponseEntity.internalServerError().body("Eroare la crearea profilului: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Map<String, Object> profileData) {
        try {
            logger.info("Updating profile {} with data: {}", id, profileData);

            Optional<ReservationProfile> profileOpt = reservationProfileRepository.findById(id);
            if (profileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ReservationProfile profile = profileOpt.get();

            // Verificăm permisiunile - utilizatorul poate edita doar propriile profiluri,
            // în timp ce adminii pot edita orice profil
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            Optional<User> currentUserOpt = userSpringRepository.findByEmail(currentUserEmail);

            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Utilizator neautentificat");
            }

            User currentUser = currentUserOpt.get();

            // Verificăm dacă utilizatorul curent poate edita acest profil
            if (!profile.getUser().getId().equals(currentUser.getId()) &&
                    !"admin".equals(currentUser.getUserType()) &&
                    !"hall_admin".equals(currentUser.getUserType())) {
                return ResponseEntity.status(403).body("Nu aveți permisiunea să editați acest profil");
            }

            // Actualizăm toate câmpurile pentru toți utilizatorii (inclusiv adminii)
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
                Object budgetValue = profileData.get("weeklyBudget");
                if (budgetValue != null && !budgetValue.toString().isEmpty()) {
                    try {
                        profile.setWeeklyBudget(new BigDecimal(budgetValue.toString()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid weeklyBudget format: {}", budgetValue);
                        return ResponseEntity.badRequest().body("Format invalid pentru bugetul săptămânal");
                    }
                } else {
                    profile.setWeeklyBudget(null);
                }
            }

            if (profileData.containsKey("city")) {
                profile.setCity((String) profileData.get("city"));
            }

            // Actualizăm sălile selectate dacă există
            if (profileData.containsKey("selectedHalls")) {
                Object hallsData = profileData.get("selectedHalls");
                if (hallsData != null) {
                    List<Integer> hallIds = (List<Integer>) hallsData;
                    if (!hallIds.isEmpty()) {
                        List<Long> longHallIds = new ArrayList<>();
                        for (Integer hallId : hallIds) {
                            longHallIds.add(hallId.longValue());
                        }

                        List<SportsHall> halls = (List<SportsHall>) sportsHallSpringRepository.findAllById(longHallIds);
                        profile.setSelectedHalls(halls);
                    } else {
                        profile.setSelectedHalls(new ArrayList<>());
                    }
                }
            }

            // Salvăm profilul actualizat
            ReservationProfile updatedProfile = reservationProfileRepository.save(profile);
            logger.info("Profile {} updated successfully", id);
            return ResponseEntity.ok(updatedProfile);

        } catch (Exception e) {
            logger.error("Error updating profile " + id, e);
            return ResponseEntity.internalServerError().body("Eroare la actualizarea profilului: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        try {
            Optional<ReservationProfile> profileOpt = reservationProfileRepository.findById(id);
            if (profileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ReservationProfile profile = profileOpt.get();

            // Verificăm permisiunile - utilizatorul poate șterge doar propriile profiluri,
            // în timp ce adminii pot șterge orice profil
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            Optional<User> currentUserOpt = userSpringRepository.findByEmail(currentUserEmail);

            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Utilizator neautentificat");
            }

            User currentUser = currentUserOpt.get();

            // Verificăm dacă utilizatorul curent poate șterge acest profil
            if (!profile.getUser().getId().equals(currentUser.getId()) &&
                    !"admin".equals(currentUser.getUserType()) &&
                    !"hall_admin".equals(currentUser.getUserType())) {
                return ResponseEntity.status(403).body("Nu aveți permisiunea să ștergeți acest profil");
            }

            reservationProfileRepository.deleteById(id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error deleting profile", e);
            return ResponseEntity.internalServerError().body("Eroare la ștergerea profilului");
        }
    }
}