package licenta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import licenta.model.SportsHall;
import licenta.model.SportsHallImage;
import licenta.model.User;
import licenta.persistence.ISportsHallImageSpringRepository;
import licenta.persistence.ISportsHallSpringRepository;
import licenta.persistence.IUserSpringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sportsHalls")
@CrossOrigin(origins = "http://localhost:3000")
public class SportsHallRestController {

    private final ISportsHallSpringRepository sportsHallSpringRepository;
    private final ISportsHallImageSpringRepository sportsHallImageSpringRepository;
    private final IUserSpringRepository userSpringRepository;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(SportsHallRestController.class);

    @Autowired
    public SportsHallRestController(ISportsHallSpringRepository sportsHallSpringRepository,
                                    ISportsHallImageSpringRepository sportsHallImageSpringRepository,
                                    IUserSpringRepository userSpringRepository,
                                    ObjectMapper objectMapper) {
        this.sportsHallSpringRepository = sportsHallSpringRepository;
        this.sportsHallImageSpringRepository = sportsHallImageSpringRepository;
        this.userSpringRepository = userSpringRepository;
        this.objectMapper = objectMapper;
    }


    @GetMapping("/cities")
    public ResponseEntity<?> getAvailableCities() {
        try {
            List<String> cities = sportsHallSpringRepository.findDistinctCities();
            logger.info("Found {} distinct cities", cities.size());
            return ResponseEntity.ok(cities);
        } catch (Exception e) {
            logger.error("Error fetching available cities", e);
            return ResponseEntity.internalServerError().body("Eroare la obținerea orașelor: " + e.getMessage());
        }
    }

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<?> getSportsHallsByAdminId(@PathVariable Long adminId) {
        try {
            List<SportsHall> halls = sportsHallSpringRepository.findByAdminId(adminId);
            logger.info("Found {} sports halls for admin ID: {}", halls.size(), adminId);
            return ResponseEntity.ok(halls);
        } catch(Exception e) {
            logger.error("Error fetching sports halls for admin ID: {}", adminId, e);
            return ResponseEntity.internalServerError().body("Eroare la obtinerea salilor de sport: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllSportsHalls(@RequestParam(required = false) String city) {
        try {
            if (city != null && !city.trim().isEmpty()) {
                List<SportsHall> halls = sportsHallSpringRepository.findByCityOrderByName(city);
                logger.info("Found {} sports halls for city: {}", halls.size(), city);
                return ResponseEntity.ok(halls);
            } else {
                Iterable<SportsHall> allHalls = sportsHallSpringRepository.findAll();
                logger.info("Returning all sports halls");
                return ResponseEntity.ok(allHalls);
            }
        } catch (Exception e) {
            logger.error("Error fetching sports halls for city: {}", city, e);
            return ResponseEntity.internalServerError().body("Eroare la obținerea sălilor de sport: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public SportsHall getSportsHallById(@PathVariable Long id) {
        return sportsHallSpringRepository.findById(id).orElse(null);
    }

    @PostMapping
    public ResponseEntity<?> createSportsHall(
            @RequestParam("sportsHall") String sportsHallJson,
            @RequestParam Map<String, MultipartFile> files,
            @RequestParam Map<String, String> params) {

        logger.info("Creating a new sports hall");
        logger.info("Sports hall data: {}", sportsHallJson);
        logger.info("Number of params: {}", params.size());
        logger.info("Number of files: {}", files.size());

        try {
            // Parsam JSON-ul pentru a obtine obiectul SportsHall
            SportsHall sportsHall = objectMapper.readValue(sportsHallJson, SportsHall.class);
            logger.info("Deserialized sports hall: {}", sportsHall);

            sportsHall.setStatus(SportsHall.HallStatus.ACTIVE);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                Optional<User> userOpt = userSpringRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    sportsHall.setAdminId(user.getId());
                    logger.info("Setting owner ID: {} for sports hall", user.getId());
                } else {
                    logger.warn("No user found with email: {}", email);
                }
            } else {
                logger.warn("No authenticated user found when creating sports hall");
            }

            if (sportsHall.getCapacity() != null && sportsHall.getCapacity() <= 0) {
                sportsHall.setCapacity(1);
            }

            if (sportsHall.getTariff() != null && sportsHall.getTariff() < 0) {
                sportsHall.setTariff(0.0f);
            }

            SportsHall savedSportsHall = sportsHallSpringRepository.save(sportsHall);
            logger.info("Saved sports hall with ID: {}", savedSportsHall.getId());

            List<MultipartFile> imageFiles = new ArrayList<>();
            List<String> imageTypes = new ArrayList<>();

            for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
                if (entry.getKey().startsWith("images[")) {
                    String key = entry.getKey();
                    int index = Integer.parseInt(key.substring(7, key.length() - 1));

                    while (imageFiles.size() <= index) {
                        imageFiles.add(null);
                        imageTypes.add(null);
                    }

                    imageFiles.set(index, entry.getValue());
                }
            }

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("imageTypes[")) {
                    String key = entry.getKey();
                    int index = Integer.parseInt(key.substring(11, key.length() - 1));

                    while (imageTypes.size() <= index) {
                        imageTypes.add(null);
                    }

                    imageTypes.set(index, entry.getValue());
                }
            }

            for (int i = 0; i < imageFiles.size(); i++) {
                if (imageFiles.get(i) == null || imageTypes.get(i) == null) {
                    logger.error("Missing image or type at index {}", i);
                    return ResponseEntity.badRequest().body("Date lipsa pentru imaginea " + i);
                }
            }

            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile file = imageFiles.get(i);
                String type = imageTypes.get(i);

                logger.info("Processing image {} of type {}, size: {}",
                        i, type, file.getSize());

                SportsHallImage image = new SportsHallImage();
                image.setImagePath(file.getBytes());
                image.setDescription(type);
                image.setSportsHall(savedSportsHall);

                sportsHallImageSpringRepository.save(image);
                logger.info("Saved image of type: {}", type);
            }

            SportsHall result = sportsHallSpringRepository.findById(savedSportsHall.getId()).orElse(null);
            logger.info("Sports hall created successfully with {} images",
                    result != null ? result.getImages().size() : 0);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            logger.error("Error processing images", e);
            return ResponseEntity.internalServerError().body("Eroare la procesarea imaginilor: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error saving sports hall", e);
            return ResponseEntity.internalServerError().body("Eroare la salvarea sălii de sport: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSportsHall(
            @PathVariable Long id,
            @RequestParam("sportsHall") String sportsHallJson,
            @RequestParam(value = "deleteImageIds", required = false) String deleteImageIdsJson,
            @RequestParam Map<String, MultipartFile> files,
            @RequestParam Map<String, String> params) {

        logger.info("Updating sports hall with ID: {}", id);
        logger.info("Sports hall data: {}", sportsHallJson);
        logger.info("Number of params: {}", params.size());
        logger.info("Number of files: {}", files.size());
        logger.info("Images to delete: {}", deleteImageIdsJson);

        try {
            Optional<SportsHall> existingHallOpt = sportsHallSpringRepository.findById(id);
            if (existingHallOpt.isEmpty()) {
                logger.error("Sports hall with ID {} not found", id);
                return ResponseEntity.notFound().build();
            }

            SportsHall existingHall = existingHallOpt.get();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            logger.info("Current authenticated user email: {}", currentUserEmail);

            // Parsam JSON-ul pentru a obtine datele actualizate ale salii
            SportsHall updatedHall = objectMapper.readValue(sportsHallJson, SportsHall.class);
            updatedHall.setId(id); // Ne asiguram ca ID-ul rămâne același
            updatedHall.setAdminId(existingHall.getAdminId()); // Pastram admin-ul original

            if (updatedHall.getCapacity() != null && updatedHall.getCapacity() <= 0) {
                updatedHall.setCapacity(1);
            }

            if (updatedHall.getTariff() != null && updatedHall.getTariff() < 0) {
                updatedHall.setTariff(0.0f);
            }

            if (deleteImageIdsJson != null && !deleteImageIdsJson.isEmpty()) {
                List<Long> deleteImageIds = objectMapper.readValue(deleteImageIdsJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));

                if (!deleteImageIds.isEmpty()) {
                    for (Long imageId : deleteImageIds) {
                        Optional<SportsHallImage> imageOpt = sportsHallImageSpringRepository.findById(imageId);
                        if (imageOpt.isPresent() && imageOpt.get().getSportsHall().getId().equals(id)) {
                            sportsHallImageSpringRepository.deleteById(imageId);
                            logger.info("Deleted image with ID: {}", imageId);
                        } else {
                            logger.warn("Image with ID {} not found or not associated with hall {}", imageId, id);
                        }
                    }
                }
            }

            updatedHall.setImages(existingHall.getImages());

            boolean hasNewImages = false;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("newImageTypes[")) {
                    hasNewImages = true;
                    break;
                }
            }

            if (hasNewImages) {
                List<MultipartFile> newImageFiles = new ArrayList<>();
                List<String> newImageTypes = new ArrayList<>();

                for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
                    if (entry.getKey().startsWith("newImages[")) {
                        String key = entry.getKey();
                        int index = Integer.parseInt(key.substring(10, key.length() - 1));

                        while (newImageFiles.size() <= index) {
                            newImageFiles.add(null);
                            newImageTypes.add(null);
                        }

                        newImageFiles.set(index, entry.getValue());
                    }
                }

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (entry.getKey().startsWith("newImageTypes[")) {
                        String key = entry.getKey();
                        int index = Integer.parseInt(key.substring(14, key.length() - 1));

                        while (newImageTypes.size() <= index) {
                            newImageTypes.add(null);
                        }

                        newImageTypes.set(index, entry.getValue());
                    }
                }

                for (int i = 0; i < newImageFiles.size(); i++) {
                    if (newImageFiles.get(i) == null || newImageTypes.get(i) == null) {
                        logger.error("Missing new image or type at index {}", i);
                        return ResponseEntity.badRequest().body("Date lipsă pentru imaginea nouă " + i);
                    }
                }

                SportsHall savedHall = sportsHallSpringRepository.save(updatedHall);
                logger.info("Updated sports hall with ID: {}", savedHall.getId());

                for (int i = 0; i < newImageFiles.size(); i++) {
                    MultipartFile file = newImageFiles.get(i);
                    String type = newImageTypes.get(i);

                    logger.info("Processing new image {} of type {}, size: {}",
                            i, type, file.getSize());

                    SportsHallImage image = new SportsHallImage();
                    image.setImagePath(file.getBytes());
                    image.setDescription(type);
                    image.setSportsHall(savedHall);

                    sportsHallImageSpringRepository.save(image);
                    logger.info("Saved new image of type: {}", type);
                }
            } else {
                SportsHall savedHall = sportsHallSpringRepository.save(updatedHall);
                logger.info("Updated sports hall with ID: {} (no new images)", savedHall.getId());
            }

            SportsHall result = sportsHallSpringRepository.findById(id).orElse(null);
            logger.info("Sports hall updated successfully with {} images",
                    result != null && result.getImages() != null ? result.getImages().size() : 0);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            logger.error("Error processing images", e);
            return ResponseEntity.internalServerError().body("Eroare la procesarea imaginilor: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating sports hall", e);
            return ResponseEntity.internalServerError().body("Eroare la actualizarea sălii de sport: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSportsHall(@PathVariable Long id) {
        try {
            sportsHallSpringRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting sports hall with ID: {}", id, e);
            return ResponseEntity.internalServerError().body("Eroare la ștergerea sălii: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateHall(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!hasPermissionToManageHall(id, authentication)) {
                logger.warn("User {} does not have permission to manage hall {}",
                        authentication != null ? authentication.getName() : "null", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Nu aveți permisiunea să gestionați această sală");
            }

            Optional<SportsHall> hallOpt = sportsHallSpringRepository.findById(id);
            if (hallOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            SportsHall hall = hallOpt.get();
            hall.setStatus(SportsHall.HallStatus.INACTIVE);

            SportsHall savedHall = sportsHallSpringRepository.save(hall);
            logger.info("Deactivated sports hall with ID: {} by user: {}",
                    id, authentication.getName());

            return ResponseEntity.ok(savedHall);

        } catch (Exception e) {
            logger.error("Error deactivating sports hall with ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("Eroare la dezactivarea sălii: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateHall(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!hasPermissionToManageHall(id, authentication)) {
                logger.warn("User {} does not have permission to manage hall {}",
                        authentication != null ? authentication.getName() : "null", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Nu aveți permisiunea să gestionați această sală");
            }

            Optional<SportsHall> hallOpt = sportsHallSpringRepository.findById(id);
            if (hallOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            SportsHall hall = hallOpt.get();
            hall.setStatus(SportsHall.HallStatus.ACTIVE);

            SportsHall savedHall = sportsHallSpringRepository.save(hall);
            logger.info("Activated sports hall with ID: {} by user: {}",
                    id, authentication.getName());

            return ResponseEntity.ok(savedHall);

        } catch (Exception e) {
            logger.error("Error activating sports hall with ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body("Eroare la activarea sălii: " + e.getMessage());
        }
    }

    private boolean hasPermissionToManageHall(Long hallId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("No authentication found");
            return false;
        }

        logger.info("Checking permissions for user: {} with authorities: {}",
                authentication.getName(), authentication.getAuthorities());

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("admin"));

        if (isAdmin) {
            logger.info("User {} is admin, access granted", authentication.getName());
            return true;
        }

        boolean isHallAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("hall_admin"));

        if (isHallAdmin) {
            String userEmail = authentication.getName();
            Optional<User> userOpt = userSpringRepository.findByEmail(userEmail);

            if (userOpt.isPresent()) {
                Optional<SportsHall> hallOpt = sportsHallSpringRepository.findById(hallId);
                if (hallOpt.isPresent()) {
                    boolean isOwner = hallOpt.get().getAdminId().equals(userOpt.get().getId());
                    logger.info("User {} is hall_admin, owner check: {}", userEmail, isOwner);
                    return isOwner;
                } else {
                    logger.warn("Hall with ID {} not found", hallId);
                    return false;
                }
            } else {
                logger.warn("User with email {} not found", userEmail);
                return false;
            }
        }

        logger.warn("User {} does not have required permissions (authorities: {})",
                authentication.getName(), authentication.getAuthorities());
        return false;
    }

    // Endpoint pentru a obtine doar salile active
    @GetMapping("/active")
    public ResponseEntity<?> getActiveHalls(@RequestParam(required = false) String city) {
        try {
            List<SportsHall> activeHalls;
            if (city != null && !city.trim().isEmpty()) {
                activeHalls = sportsHallSpringRepository.findByCityAndStatusOrderByName(city, SportsHall.HallStatus.ACTIVE);
            } else {
                activeHalls = sportsHallSpringRepository.findByStatusOrderByName(SportsHall.HallStatus.ACTIVE);
            }

            logger.info("Found {} active sports halls", activeHalls.size());
            return ResponseEntity.ok(activeHalls);

        } catch (Exception e) {
            logger.error("Error fetching active sports halls", e);
            return ResponseEntity.internalServerError().body("Eroare la obținerea sălilor active: " + e.getMessage());
        }
    }

    @GetMapping("/names/suggestions")
    public ResponseEntity<?> getHallNamesSuggestions(@RequestParam String query) {
        try {
            if (query == null || query.trim().length() < 2) {
                return ResponseEntity.ok(List.of());
            }

            String cleanQuery = query.trim();
            List<String> suggestions = sportsHallSpringRepository.findHallNamesSuggestionsLimited(cleanQuery);

            logger.info("Found {} hall name suggestions for query: '{}'", suggestions.size(), cleanQuery);
            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            logger.error("Error fetching hall name suggestions for query: '{}'", query, e);
            return ResponseEntity.ok(List.of()); // Returnează listă goală în caz de eroare
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchSportsHalls(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) String query) {

        try {
            List<SportsHall> results = new ArrayList<>();

            logger.info("Search request - City: '{}', Sport: '{}', Query: '{}'", city, sport, query);

            if (query != null && !query.trim().isEmpty()) {
                // Prioritate 1: Cautare dupa nume (cu sau fara oras si sport)
                String searchQuery = query.trim();

                if (city != null && !city.trim().isEmpty() && sport != null && !sport.trim().isEmpty()) {
                    if ("inot".equalsIgnoreCase(sport.trim())) {
                        results = sportsHallSpringRepository.findByNameContainingAndCityIgnoreCase(searchQuery, city.trim())
                                .stream()
                                .filter(hall -> isSwimmingHall(hall.getType()))
                                .collect(Collectors.toList());
                    } else {
                        results = sportsHallSpringRepository.findByCityAndSportAndNameIncludingMultipurpose(
                                city.trim(), sport.trim(), searchQuery);
                    }
                    logger.info("Search by name + city + sport found {} results", results.size());

                    if (results.isEmpty()) {
                        results = sportsHallSpringRepository.findByNameContainingAndCityIgnoreCase(searchQuery, city.trim());
                        logger.info("Fallback search by name + city found {} results", results.size());
                    }
                } else if (city != null && !city.trim().isEmpty()) {
                    results = sportsHallSpringRepository.findByNameContainingAndCityIgnoreCase(searchQuery, city.trim());
                    logger.info("Search by name + city found {} results", results.size());
                } else {
                    results = sportsHallSpringRepository.findByNameContainingIgnoreCase(searchQuery);
                    logger.info("Search by name only found {} results", results.size());
                }

            } else if (city != null && !city.trim().isEmpty() && sport != null && !sport.trim().isEmpty()) {
                if ("inot".equalsIgnoreCase(sport.trim())) {
                    results = sportsHallSpringRepository.findSwimmingHallsByCity(city.trim());
                } else {
                    results = sportsHallSpringRepository.findByCityAndSportIncludingMultipurpose(
                            city.trim(), sport.trim());
                }
                logger.info("Search by city + sport found {} results", results.size());

            } else if (city != null && !city.trim().isEmpty()) {
                results = sportsHallSpringRepository.findByCityAndStatusOrderByName(city.trim(), SportsHall.HallStatus.ACTIVE);
                logger.info("Search by city only found {} results", results.size());

            } else if (sport != null && !sport.trim().isEmpty()) {
                if ("inot".equalsIgnoreCase(sport.trim())) {
                    results = sportsHallSpringRepository.findSwimmingHalls();
                } else {
                    results = sportsHallSpringRepository.findBySportIncludingMultipurpose(sport.trim());
                }
                logger.info("Search by sport only found {} results", results.size());

            } else {
                results = sportsHallSpringRepository.findByStatusOrderByName(SportsHall.HallStatus.ACTIVE);
                logger.info("Search with no criteria, returning all active halls: {} results", results.size());
            }

            if (!results.isEmpty()) {
                logger.info("First result example: Hall '{}' in '{}' for type '{}'",
                        results.get(0).getName(),
                        results.get(0).getCity(),
                        results.get(0).getType());
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            logger.error("Error during sports halls search", e);
            return ResponseEntity.internalServerError()
                    .body("Eroare la căutarea sălilor de sport: " + e.getMessage());
        }
    }

    private boolean isSwimmingHall(String type) {
        if (type == null) return false;
        String lowerType = type.toLowerCase();
        return lowerType.equals("inot") ||
                lowerType.contains("piscina") ||
                lowerType.contains("swimming") ||
                lowerType.contains("inot");
    }
}