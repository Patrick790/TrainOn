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

    // Modificări pentru metoda updateSportsHall în SportsHallRestController.java

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
            // Verificăm dacă sala există
            Optional<SportsHall> existingHallOpt = sportsHallSpringRepository.findById(id);
            if (existingHallOpt.isEmpty()) {
                logger.error("Sports hall with ID {} not found", id);
                return ResponseEntity.notFound().build();
            }

            SportsHall existingHall = existingHallOpt.get();

            // Obține utilizatorul autentificat (pentru a verifica permisiunile sau pentru logging)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            logger.info("Current authenticated user email: {}", currentUserEmail);

            // Parsăm JSON-ul pentru a obține datele actualizate ale sălii
            SportsHall updatedHall = objectMapper.readValue(sportsHallJson, SportsHall.class);
            updatedHall.setId(id); // Ne asigurăm că ID-ul rămâne același
            updatedHall.setAdminId(existingHall.getAdminId()); // Păstrăm admin-ul original

            // Asigurăm că valorile numerice sunt valide
            if (updatedHall.getCapacity() != null && updatedHall.getCapacity() <= 0) {
                updatedHall.setCapacity(1);
            }

            if (updatedHall.getTariff() != null && updatedHall.getTariff() < 0) {
                updatedHall.setTariff(0.0f);
            }

            // Ștergem imaginile marcate pentru ștergere doar dacă parametrul există
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

            // Adăugăm imaginile existente la sala de sport actualizată
            updatedHall.setImages(existingHall.getImages());

            // Verificăm dacă există imagini noi pentru a le procesa
            boolean hasNewImages = false;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("newImageTypes[")) {
                    hasNewImages = true;
                    break;
                }
            }

            if (hasNewImages) {
                // Procesăm imaginile noi
                List<MultipartFile> newImageFiles = new ArrayList<>();
                List<String> newImageTypes = new ArrayList<>();

                // Extragem imaginile din Map-ul de fișiere
                for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
                    if (entry.getKey().startsWith("newImages[")) {
                        String key = entry.getKey();
                        int index = Integer.parseInt(key.substring(10, key.length() - 1));

                        // Ne asigurăm că avem suficient spațiu în lista noastră
                        while (newImageFiles.size() <= index) {
                            newImageFiles.add(null);
                            newImageTypes.add(null);
                        }

                        newImageFiles.set(index, entry.getValue());
                    }
                }

                // Extragem tipurile din Map-ul de parametri
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (entry.getKey().startsWith("newImageTypes[")) {
                        String key = entry.getKey();
                        int index = Integer.parseInt(key.substring(14, key.length() - 1));

                        // Ne asigurăm că avem suficient spațiu în lista noastră
                        while (newImageTypes.size() <= index) {
                            newImageTypes.add(null);
                        }

                        newImageTypes.set(index, entry.getValue());
                    }
                }

                // Validăm că avem toate imaginile noi și tipurile lor
                for (int i = 0; i < newImageFiles.size(); i++) {
                    if (newImageFiles.get(i) == null || newImageTypes.get(i) == null) {
                        logger.error("Missing new image or type at index {}", i);
                        return ResponseEntity.badRequest().body("Date lipsă pentru imaginea nouă " + i);
                    }
                }

                // Salvăm sala actualizată
                SportsHall savedHall = sportsHallSpringRepository.save(updatedHall);
                logger.info("Updated sports hall with ID: {}", savedHall.getId());

                // Procesăm fiecare imagine nouă
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
                // Salvăm sala actualizată fără a procesa imagini noi
                SportsHall savedHall = sportsHallSpringRepository.save(updatedHall);
                logger.info("Updated sports hall with ID: {} (no new images)", savedHall.getId());
            }

            // Reîncărcăm sala pentru a obține configurația actualizată cu toate imaginile
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