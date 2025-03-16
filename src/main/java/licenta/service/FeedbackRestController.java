package licenta.service;

import licenta.model.Feedback;
import licenta.persistence.IFeedbackSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackRestController {

    private final IFeedbackSpringRepository feedbackSpringRepository;

    @Autowired
    public FeedbackRestController(IFeedbackSpringRepository feedbackSpringRepository) {
        this.feedbackSpringRepository = feedbackSpringRepository;
    }

    @GetMapping("/{id}")
    public Feedback getFeedbackById(@PathVariable Long id) {
        return feedbackSpringRepository.findById(id).orElse(null);
    }

    @GetMapping
    public Iterable<Feedback> getAllFeedbacks() {
        return feedbackSpringRepository.findAll();
    }

    @PostMapping
    public Feedback createFeedback(@RequestBody Feedback feedback) {
        return feedbackSpringRepository.save(feedback);
    }
}