package licenta.service;

import licenta.model.Feedback;
import licenta.persistence.IFeedbackSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedbacks")
@CrossOrigin(origins = "http://localhost:3000")
public class FeedbackRestController {

    private final IFeedbackSpringRepository feedbackSpringRepository;

    @Autowired
    public FeedbackRestController(IFeedbackSpringRepository feedbackSpringRepository) {
        this.feedbackSpringRepository = feedbackSpringRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Feedback> getFeedbackById(@PathVariable Long id) {
        return feedbackSpringRepository.findById(id)
                .map(feedback -> ResponseEntity.ok().body(feedback))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Feedback>> getAllFeedbacks(
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) String sort) {

        List<Feedback> feedbacks;

        if (hallId != null) {
            // Dacă avem un ID de sală, filtrăm doar recenziile pentru acea sală
            if (sort != null) {
                switch (sort) {
                    case "date":
                        feedbacks = feedbackSpringRepository.findByHallIdOrderByDateDesc(hallId);
                        break;
                    case "rating_desc":
                        feedbacks = feedbackSpringRepository.findByHallIdOrderByRatingDesc(hallId);
                        break;
                    case "rating_asc":
                        feedbacks = feedbackSpringRepository.findByHallIdOrderByRatingAsc(hallId);
                        break;
                    default:
                        feedbacks = feedbackSpringRepository.findByHallId(hallId);
                }
            } else {
                feedbacks = feedbackSpringRepository.findByHallId(hallId);
            }
        } else {
            // Altfel, returnăm toate recenziile
            feedbacks = (List<Feedback>) feedbackSpringRepository.findAll();
        }

        return ResponseEntity.ok().body(feedbacks);
    }

    @PostMapping
    public ResponseEntity<Feedback> createFeedback(@RequestBody Feedback feedback) {
        try {
            Feedback savedFeedback = feedbackSpringRepository.save(feedback);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFeedback);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Feedback> updateFeedback(@PathVariable Long id, @RequestBody Feedback feedback) {
        if (!feedbackSpringRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        feedback.setId(id);
        Feedback updatedFeedback = feedbackSpringRepository.save(feedback);
        return ResponseEntity.ok().body(updatedFeedback);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long id) {
        if (!feedbackSpringRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        feedbackSpringRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}