package licenta.service;

import jakarta.transaction.Transactional;
import licenta.model.Schedule;
import licenta.model.SportsHall;
import licenta.persistence.IScheduleSpringRepository;
import licenta.persistence.ISportsHallSpringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/schedules")
@CrossOrigin(origins = "http://localhost:3000")
public class ScheduleRestController {

    private final IScheduleSpringRepository scheduleRepository;
    private final ISportsHallSpringRepository sportsHallRepository;
    private static final Logger logger = LoggerFactory.getLogger(ScheduleRestController.class);

    @Autowired
    public ScheduleRestController(IScheduleSpringRepository scheduleRepository,
                                  ISportsHallSpringRepository sportsHallRepository) {
        this.scheduleRepository = scheduleRepository;
        this.sportsHallRepository = sportsHallRepository;
    }

    @GetMapping("/hall/{hallId}")
    public ResponseEntity<?> getSchedulesByHallId(@PathVariable Long hallId) {
        try {
            List<Schedule> schedules = scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(hallId, true);
            logger.info("Found {} schedules for hall ID: {}", schedules.size(), hallId);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            logger.error("Error fetching schedules for hall ID: {}", hallId, e);
            return ResponseEntity.internalServerError().body("Eroare la obținerea programului: " + e.getMessage());
        }
    }

    @PostMapping("/hall/{hallId}")
    @Transactional
    public ResponseEntity<?> createOrUpdateSchedules(@PathVariable Long hallId, @RequestBody List<Schedule> schedules) {
        try {
            // Verificăm dacă sala există
            Optional<SportsHall> hallOpt = sportsHallRepository.findById(hallId);
            if (hallOpt.isEmpty()) {
                logger.error("Sports hall with ID {} not found", hallId);
                return ResponseEntity.notFound().build();
            }

            SportsHall hall = hallOpt.get();

            List<Schedule> existingSchedules = scheduleRepository.findBySportsHallIdOrderByDayOfWeek(hallId);
            if (!existingSchedules.isEmpty()) {
                scheduleRepository.deleteAll(existingSchedules);
                scheduleRepository.flush();
            }

            List<Schedule> savedSchedules = new ArrayList<>();
            for (Schedule schedule : schedules) {
                Schedule newSchedule = new Schedule();
                newSchedule.setSportsHall(hall);
                newSchedule.setDayOfWeek(schedule.getDayOfWeek());
                newSchedule.setStartTime(schedule.getStartTime());
                newSchedule.setEndTime(schedule.getEndTime());
                newSchedule.setIsActive(schedule.getIsActive() != null ? schedule.getIsActive() : true);

                Schedule saved = scheduleRepository.save(newSchedule);
                savedSchedules.add(saved);
            }

            logger.info("Successfully saved {} schedules for hall ID: {}", savedSchedules.size(), hallId);
            return ResponseEntity.ok(savedSchedules);

        } catch (Exception e) {
            logger.error("Error saving schedules for hall ID: {}", hallId, e);
            return ResponseEntity.internalServerError().body("Eroare la salvarea programului: " + e.getMessage());
        }
    }

    @PostMapping("/hall/{hallId}/default")
    @Transactional
    public ResponseEntity<?> createDefaultSchedule(@PathVariable Long hallId) {
        try {
            Optional<SportsHall> hallOpt = sportsHallRepository.findById(hallId);
            if (hallOpt.isEmpty()) {
                logger.error("Sports hall with ID {} not found", hallId);
                return ResponseEntity.notFound().build();
            }

            SportsHall hall = hallOpt.get();

            scheduleRepository.deleteBySportsHallId(hallId);

            for (int day = 1; day <= 7; day++) {
                Schedule schedule = new Schedule(hall, day, "07:00", "23:00");
                scheduleRepository.save(schedule);
            }

            List<Schedule> savedSchedules = scheduleRepository.findBySportsHallIdOrderByDayOfWeek(hallId);
            logger.info("Created default schedule for hall ID: {}", hallId);
            return ResponseEntity.ok(savedSchedules);

        } catch (Exception e) {
            logger.error("Error creating default schedule for hall ID: {}", hallId, e);
            return ResponseEntity.internalServerError().body("Eroare la crearea programului standard: " + e.getMessage());
        }
    }

    @DeleteMapping("/hall/{hallId}")
    @Transactional
    public ResponseEntity<?> deleteSchedulesByHallId(@PathVariable Long hallId) {
        try {
            scheduleRepository.deleteBySportsHallId(hallId);
            logger.info("Deleted all schedules for hall ID: {}", hallId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting schedules for hall ID: {}", hallId, e);
            return ResponseEntity.internalServerError().body("Eroare la ștergerea programului: " + e.getMessage());
        }
    }
}