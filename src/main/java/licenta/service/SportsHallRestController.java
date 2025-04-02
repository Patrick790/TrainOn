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
                                    ISportsHallImageSpringRepository sportsHallImageSpringRepository, IUserSpringRepository userSpringRepository,
                                    ObjectMapper objectMapper) {
        this.sportsHallSpringRepository = sportsHallSpringRepository;
        this.sportsHallImageSpringRepository = sportsHallImageSpringRepository;
        this.userSpringRepository = userSpringRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{id}")
    public SportsHall getSportsHallById(@PathVariable Long id) {
        return sportsHallSpringRepository.findById(id).orElse(null);
    }

    @GetMapping
    public Iterable<SportsHall> getAllSportsHalls() {
        return sportsHallSpringRepository.findAll();
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

            // Obținem utilizatorul curent autentificat
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

            // Asigura-te ca valorile numerice sunt procesate corect
            if (sportsHall.getCapacity() != null && sportsHall.getCapacity() <= 0) {
                sportsHall.setCapacity(1);
            }

            if (sportsHall.getTariff() != null && sportsHall.getTariff() < 0) {
                sportsHall.setTariff(0.0f);
            }

            // Salvam sala fara imagini initial
            SportsHall savedSportsHall = sportsHallSpringRepository.save(sportsHall);
            logger.info("Saved sports hall with ID: {}", savedSportsHall.getId());

            // Procesam imaginile si tipurile lor
            List<MultipartFile> imageFiles = new ArrayList<>();
            List<String> imageTypes = new ArrayList<>();

            // Extragem imaginile din Map-ul de fisiere
            for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
                if (entry.getKey().startsWith("images[")) {
                    String key = entry.getKey();
                    int index = Integer.parseInt(key.substring(7, key.length() - 1));

                    // Ne asiguram ca avem suficient spatiu in lista noastra
                    while (imageFiles.size() <= index) {
                        imageFiles.add(null);
                        imageTypes.add(null);
                    }

                    imageFiles.set(index, entry.getValue());
                }
            }

            // Extragem tipurile din Map-ul de parametri
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("imageTypes[")) {
                    String key = entry.getKey();
                    int index = Integer.parseInt(key.substring(11, key.length() - 1));

                    // Ne asiguram ca avem suficient spatiu în lista noastra
                    while (imageTypes.size() <= index) {
                        imageTypes.add(null);
                    }

                    imageTypes.set(index, entry.getValue());
                }
            }

            // Validam ca avem toate imaginile si tipurile lor
            for (int i = 0; i < imageFiles.size(); i++) {
                if (imageFiles.get(i) == null || imageTypes.get(i) == null) {
                    logger.error("Missing image or type at index {}", i);
                    return ResponseEntity.badRequest().body("Date lipsa pentru imaginea " + i);
                }
            }

            // Acum procesam fiecare imagine
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

            // Reincarcare sala pentru a obtine imaginile proaspat salvate
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
}