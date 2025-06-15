package licenta.service;

import licenta.model.ReservationProfile;
import licenta.model.SportsHall;
import licenta.model.User;
import licenta.model.PaymentMethod;
import licenta.model.CardPaymentMethod;
import licenta.persistence.IReservationProfileRepository;
import licenta.persistence.ISportsHallSpringRepository;
import licenta.persistence.IUserSpringRepository;
import licenta.persistence.ICardPaymentMethodSpringRepository;
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
    private final ICardPaymentMethodSpringRepository cardPaymentMethodRepository;
    private static final Logger logger = LoggerFactory.getLogger(ReservationProfileRestController.class);

    @Autowired
    public ReservationProfileRestController(
            IReservationProfileRepository reservationProfileRepository,
            IUserSpringRepository userSpringRepository,
            ISportsHallSpringRepository sportsHallSpringRepository,
            ICardPaymentMethodSpringRepository cardPaymentMethodRepository) {
        this.reservationProfileRepository = reservationProfileRepository;
        this.userSpringRepository = userSpringRepository;
        this.sportsHallSpringRepository = sportsHallSpringRepository;
        this.cardPaymentMethodRepository = cardPaymentMethodRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllProfiles(@RequestParam(required = false) Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Optional<User> userOpt = userSpringRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User currentUser = userOpt.get();

                if (userId != null) {
                    if (!"admin".equals(currentUser.getUserType()) && !"hall_admin".equals(currentUser.getUserType())) {
                        return ResponseEntity.status(403).body("Acces interzis - doar adminii pot accesa această funcționalitate");
                    }

                    Optional<User> targetUserOpt = userSpringRepository.findById(userId);
                    if (targetUserOpt.isEmpty()) {
                        return ResponseEntity.status(404).body("Utilizatorul specificat nu a fost găsit");
                    }

                    List<ReservationProfile> userProfiles = reservationProfileRepository.findByUserId(userId);
                    return ResponseEntity.ok(userProfiles);
                } else {
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProfilesByUserId(@PathVariable Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            Optional<User> currentUserOpt = userSpringRepository.findByEmail(currentUserEmail);

            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Utilizator neautentificat");
            }

            User currentUser = currentUserOpt.get();

            if (!"admin".equals(currentUser.getUserType()) && !"hall_admin".equals(currentUser.getUserType())) {
                return ResponseEntity.status(403).body("Acces interzis - doar adminii pot accesa această funcționalitate");
            }

            Optional<User> targetUserOpt = userSpringRepository.findById(userId);
            if (targetUserOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Utilizatorul specificat nu a fost găsit");
            }

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
            logger.info("Creating profile with data: {}", profileData);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Optional<User> userOpt = userSpringRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Creez profilul
                ReservationProfile profile = new ReservationProfile();
                profile.setName((String) profileData.get("name"));
                profile.setAgeCategory((String) profileData.get("ageCategory"));
                profile.setTimeInterval((String) profileData.get("timeInterval"));

                if (profileData.get("weeklyBudget") != null && !profileData.get("weeklyBudget").toString().isEmpty()) {
                    profile.setWeeklyBudget(new BigDecimal(profileData.get("weeklyBudget").toString()));
                }

                profile.setCity((String) profileData.get("city"));
                profile.setSport((String) profileData.get("sport")); // ADĂUGAT: procesarea sportului
                profile.setUser(user);

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

                processAutoPaymentSettings(profile, profileData, user);

                ReservationProfile savedProfile = reservationProfileRepository.save(profile);
                logger.info("Profile created successfully with id: {}", savedProfile.getId());
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

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            Optional<User> currentUserOpt = userSpringRepository.findByEmail(currentUserEmail);

            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Utilizator neautentificat");
            }

            User currentUser = currentUserOpt.get();

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

            // ADĂUGAT: Actualizarea sportului
            if (profileData.containsKey("sport")) {
                profile.setSport((String) profileData.get("sport"));
            }

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

            processAutoPaymentSettings(profile, profileData, currentUser);

            ReservationProfile updatedProfile = reservationProfileRepository.save(profile);
            logger.info("Profile {} updated successfully", id);
            return ResponseEntity.ok(updatedProfile);

        } catch (Exception e) {
            logger.error("Error updating profile " + id, e);
            return ResponseEntity.internalServerError().body("Eroare la actualizarea profilului: " + e.getMessage());
        }
    }

    private void processAutoPaymentSettings(ReservationProfile profile, Map<String, Object> profileData, User user) {
        try {
            // Procesăm autoPaymentEnabled
            if (profileData.containsKey("autoPaymentEnabled")) {
                Object autoPaymentEnabledObj = profileData.get("autoPaymentEnabled");
                boolean autoPaymentEnabled = false;

                if (autoPaymentEnabledObj instanceof Boolean) {
                    autoPaymentEnabled = (Boolean) autoPaymentEnabledObj;
                } else if (autoPaymentEnabledObj instanceof String) {
                    autoPaymentEnabled = Boolean.parseBoolean((String) autoPaymentEnabledObj);
                }

                profile.setAutoPaymentEnabled(autoPaymentEnabled);
                logger.info("Set autoPaymentEnabled to: {}", autoPaymentEnabled);

                if (autoPaymentEnabled) {
                    if (profileData.containsKey("autoPaymentMethod")) {
                        String paymentMethodStr = (String) profileData.get("autoPaymentMethod");
                        if ("CARD".equals(paymentMethodStr)) {
                            profile.setAutoPaymentMethod(PaymentMethod.CARD);
                        } else if ("CASH".equals(paymentMethodStr)) {
                            profile.setAutoPaymentMethod(PaymentMethod.CASH);
                        }
                        logger.info("Set autoPaymentMethod to: {}", paymentMethodStr);
                    }

                    if (profileData.containsKey("defaultCardPaymentMethodId")) {
                        Object cardIdObj = profileData.get("defaultCardPaymentMethodId");
                        if (cardIdObj != null && !cardIdObj.toString().isEmpty()) {
                            try {
                                Long cardId;
                                if (cardIdObj instanceof Integer) {
                                    cardId = ((Integer) cardIdObj).longValue();
                                } else if (cardIdObj instanceof Long) {
                                    cardId = (Long) cardIdObj;
                                } else {
                                    cardId = Long.parseLong(cardIdObj.toString());
                                }

                                Optional<CardPaymentMethod> cardOpt = cardPaymentMethodRepository.findById(cardId);
                                if (cardOpt.isPresent()) {
                                    CardPaymentMethod card = cardOpt.get();
                                    if (card.getUser().getId().equals(user.getId())) {
                                        profile.setDefaultCardPaymentMethod(card);
                                        logger.info("Set defaultCardPaymentMethod to: {}", cardId);
                                    } else {
                                        logger.warn("Card {} does not belong to user {}", cardId, user.getId());
                                    }
                                } else {
                                    logger.warn("Card with id {} not found", cardId);
                                }
                            } catch (NumberFormatException e) {
                                logger.warn("Invalid card ID format: {}", cardIdObj);
                            }
                        } else {
                            profile.setDefaultCardPaymentMethod(null);
                        }
                    }

                    if (profileData.containsKey("autoPaymentThreshold")) {
                        Object thresholdObj = profileData.get("autoPaymentThreshold");
                        if (thresholdObj != null && !thresholdObj.toString().isEmpty()) {
                            try {
                                BigDecimal threshold = new BigDecimal(thresholdObj.toString());
                                profile.setAutoPaymentThreshold(threshold);
                                logger.info("Set autoPaymentThreshold to: {}", threshold);
                            } catch (NumberFormatException e) {
                                logger.warn("Invalid autoPaymentThreshold format: {}", thresholdObj);
                            }
                        } else {
                            profile.setAutoPaymentThreshold(null);
                        }
                    }

                    if (profileData.containsKey("maxWeeklyAutoPayment")) {
                        Object maxWeeklyObj = profileData.get("maxWeeklyAutoPayment");
                        if (maxWeeklyObj != null && !maxWeeklyObj.toString().isEmpty()) {
                            try {
                                BigDecimal maxWeekly = new BigDecimal(maxWeeklyObj.toString());
                                profile.setMaxWeeklyAutoPayment(maxWeekly);
                                logger.info("Set maxWeeklyAutoPayment to: {}", maxWeekly);
                            } catch (NumberFormatException e) {
                                logger.warn("Invalid maxWeeklyAutoPayment format: {}", maxWeeklyObj);
                            }
                        } else {
                            profile.setMaxWeeklyAutoPayment(null);
                        }
                    }
                } else {
                    profile.setAutoPaymentMethod(null);
                    profile.setDefaultCardPaymentMethod(null);
                    profile.setAutoPaymentThreshold(null);
                    profile.setMaxWeeklyAutoPayment(null);
                    logger.info("Auto payment disabled, reset all auto payment settings");
                }
            }
        } catch (Exception e) {
            logger.error("Error processing auto payment settings", e);
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

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            Optional<User> currentUserOpt = userSpringRepository.findByEmail(currentUserEmail);

            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Utilizator neautentificat");
            }

            User currentUser = currentUserOpt.get();

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