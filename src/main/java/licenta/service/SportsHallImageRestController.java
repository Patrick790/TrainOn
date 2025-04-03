package licenta.service;

import licenta.model.SportsHallImage;
import licenta.persistence.ISportsHallImageSpringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/images")
@CrossOrigin(origins = "http://localhost:3000")
public class SportsHallImageRestController {

    private final ISportsHallImageSpringRepository sportsHallImageSpringRepository;
    private static final Logger logger = LoggerFactory.getLogger(SportsHallImageRestController.class);

    @Autowired
    public SportsHallImageRestController(ISportsHallImageSpringRepository sportsHallImageSpringRepository) {
        this.sportsHallImageSpringRepository = sportsHallImageSpringRepository;
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> getImageById(@PathVariable Long imageId) {
        try {
            Optional<SportsHallImage> imageOpt = sportsHallImageSpringRepository.findById(imageId);
            if (imageOpt.isPresent()) {
                SportsHallImage image = imageOpt.get();
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(image.getImage());
            } else {
                logger.warn("Image with ID {} not found", imageId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving image with ID: {}", imageId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<?> deleteImageById(@PathVariable Long imageId) {
        try {
            Optional<SportsHallImage> imageOpt = sportsHallImageSpringRepository.findById(imageId);

            if (imageOpt.isEmpty()) {
                logger.warn("Image with ID {} not found for deletion", imageId);
                return ResponseEntity.notFound().build();
            }

            SportsHallImage image = imageOpt.get();
            Long hallId = image.getSportsHall().getId();

            sportsHallImageSpringRepository.deleteById(imageId);
            logger.info("Successfully deleted image with ID: {}", imageId);

            return ResponseEntity.ok("Image deleted successfully");

        } catch (Exception e) {
            logger.error("Error deleting image with ID: {}", imageId, e);
            return ResponseEntity.internalServerError().body("Error deleting image: " + e.getMessage());
        }
    }
}
