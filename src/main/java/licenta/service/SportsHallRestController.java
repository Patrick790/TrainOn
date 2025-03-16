package licenta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import licenta.model.SportsHall;
import licenta.model.SportsHallImage;
import licenta.persistence.ISportsHallImageSpringRepository;
import licenta.persistence.ISportsHallSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/sportsHalls")
public class SportsHallRestController {

    private final ISportsHallSpringRepository sportsHallSpringRepository;
    private final ISportsHallImageSpringRepository sportsHallImageSpringRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public SportsHallRestController(ISportsHallSpringRepository sportsHallSpringRepository,
                                    ISportsHallImageSpringRepository sportsHallImageSpringRepository,
                                    ObjectMapper objectMapper) {
        this.sportsHallSpringRepository = sportsHallSpringRepository;
        this.sportsHallImageSpringRepository = sportsHallImageSpringRepository;
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
            @RequestPart("sportsHall") String sportsHallJson,
            @RequestPart("images") List<MultipartFile> imageFiles,
            @RequestPart("imageTypes") List<String> imageTypes) {

        try {
            // Verificăm dacă numărul de fișiere corespunde cu numărul de tipuri
            if (imageFiles.size() != imageTypes.size()) {
                return ResponseEntity.badRequest().body("Numărul de imagini nu corespunde cu numărul de tipuri");
            }

            // Deserializăm manual JSON-ul
            SportsHall sportsHall = objectMapper.readValue(sportsHallJson, SportsHall.class);

            // Asigură-te că valorile numerice sunt procesate corect
            if (sportsHall.getCapacity() != null && sportsHall.getCapacity() <= 0) {
                sportsHall.setCapacity(1); // Setăm o valoare minimă validă
            }

            if (sportsHall.getTariff() != null && sportsHall.getTariff() < 0) {
                sportsHall.setTariff(0.0f);
            }

            // Salvăm sala fără imagini inițial
            SportsHall savedSportsHall = sportsHallSpringRepository.save(sportsHall);

            // Procesăm și adăugăm fiecare imagine
            for (int i = 0; i < imageFiles.size(); i++) {
                MultipartFile imageFile = imageFiles.get(i);
                String imageType = imageTypes.get(i);

                // Creăm obiectul imagine
                SportsHallImage image = new SportsHallImage();
                image.setImagePath(imageFile.getBytes());
                image.setDescription(imageType);
                image.setSportsHall(savedSportsHall);

                // Salvăm imaginea folosind repository-ul specific
                sportsHallImageSpringRepository.save(image);
            }

            // Reîncărcăm sala pentru a obține imaginile proaspăt salvate
            return ResponseEntity.ok(sportsHallSpringRepository.findById(savedSportsHall.getId()).orElse(null));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Eroare la procesarea imaginilor: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Eroare la salvarea sălii de sport: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public void deleteSportsHall(@PathVariable Long id) {
        sportsHallSpringRepository.deleteById(id);
    }
}