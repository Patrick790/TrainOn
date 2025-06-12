package licenta.service;

import licenta.model.Schedule;
import licenta.model.SportsHall;
import licenta.persistence.IScheduleSpringRepository;
import licenta.persistence.ISportsHallSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleRestControllerTest {

    @Mock
    private IScheduleSpringRepository scheduleRepository;

    @Mock
    private ISportsHallSpringRepository sportsHallRepository;

    private ScheduleRestController scheduleRestController;

    private SportsHall testHall;
    private Schedule testSchedule1;
    private Schedule testSchedule2;
    private Schedule testSchedule3;
    private List<Schedule> testSchedules;

    @BeforeEach
    void setUp() {
        scheduleRestController = new ScheduleRestController(scheduleRepository, sportsHallRepository);

        // Configurăm sala de test
        testHall = new SportsHall();
        testHall.setId(1L);
        testHall.setName("Test Sports Hall");
        testHall.setCity("Cluj-Napoca");

        // Configurăm programele de test
        testSchedule1 = new Schedule();
        testSchedule1.setId(1L);
        testSchedule1.setSportsHall(testHall);
        testSchedule1.setDayOfWeek(1); // Luni
        testSchedule1.setStartTime("07:00");
        testSchedule1.setEndTime("23:00");
        testSchedule1.setIsActive(true);

        testSchedule2 = new Schedule();
        testSchedule2.setId(2L);
        testSchedule2.setSportsHall(testHall);
        testSchedule2.setDayOfWeek(2); // Marți
        testSchedule2.setStartTime("08:00");
        testSchedule2.setEndTime("22:00");
        testSchedule2.setIsActive(true);

        testSchedule3 = new Schedule();
        testSchedule3.setId(3L);
        testSchedule3.setSportsHall(testHall);
        testSchedule3.setDayOfWeek(3); // Miercuri
        testSchedule3.setStartTime("09:00");
        testSchedule3.setEndTime("21:00");
        testSchedule3.setIsActive(false);

        testSchedules = Arrays.asList(testSchedule1, testSchedule2);
    }

    @Test
    void getSchedulesByHallId_WithExistingHall_ShouldReturnActiveSchedules() {
        // Arrange
        when(scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(1L, true))
                .thenReturn(testSchedules);

        // Act
        ResponseEntity<?> response = scheduleRestController.getSchedulesByHallId(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testSchedules, response.getBody());
        verify(scheduleRepository).findBySportsHallIdAndIsActiveOrderByDayOfWeek(1L, true);
    }

    @Test
    void getSchedulesByHallId_WithNoSchedules_ShouldReturnEmptyList() {
        // Arrange
        when(scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(1L, true))
                .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<?> response = scheduleRestController.getSchedulesByHallId(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.emptyList(), response.getBody());
        verify(scheduleRepository).findBySportsHallIdAndIsActiveOrderByDayOfWeek(1L, true);
    }

    @Test
    void getSchedulesByHallId_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(1L, true))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = scheduleRestController.getSchedulesByHallId(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la obținerea programului"));
        verify(scheduleRepository).findBySportsHallIdAndIsActiveOrderByDayOfWeek(1L, true);
    }

    @Test
    void createOrUpdateSchedules_WithExistingHall_ShouldCreateNewSchedules() {
        // Arrange
        when(sportsHallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(scheduleRepository.findBySportsHallIdOrderByDayOfWeek(1L)).thenReturn(testSchedules);

        List<Schedule> newSchedules = Arrays.asList(
                createScheduleData(1, "06:00", "22:00", true),
                createScheduleData(2, "07:00", "23:00", true)
        );

        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setId(System.currentTimeMillis()); // Simulează ID generat
            return schedule;
        });

        // Act
        ResponseEntity<?> response = scheduleRestController.createOrUpdateSchedules(1L, newSchedules);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Schedule> savedSchedules = (List<Schedule>) response.getBody();
        assertNotNull(savedSchedules);
        assertEquals(2, savedSchedules.size());

        verify(sportsHallRepository).findById(1L);
        verify(scheduleRepository).findBySportsHallIdOrderByDayOfWeek(1L);
        verify(scheduleRepository).deleteAll(testSchedules);
        verify(scheduleRepository).flush();
        verify(scheduleRepository, times(2)).save(any(Schedule.class));
    }

    @Test
    void createOrUpdateSchedules_WithNonExistingHall_ShouldReturnNotFound() {
        // Arrange
        when(sportsHallRepository.findById(999L)).thenReturn(Optional.empty());

        List<Schedule> newSchedules = Arrays.asList(createScheduleData(1, "07:00", "23:00", true));

        // Act
        ResponseEntity<?> response = scheduleRestController.createOrUpdateSchedules(999L, newSchedules);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(sportsHallRepository).findById(999L);
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createOrUpdateSchedules_WithNoExistingSchedules_ShouldCreateSchedules() {
        // Arrange
        when(sportsHallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(scheduleRepository.findBySportsHallIdOrderByDayOfWeek(1L)).thenReturn(Collections.emptyList());

        List<Schedule> newSchedules = Arrays.asList(createScheduleData(1, "07:00", "23:00", true));

        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setId(1L);
            return schedule;
        });

        // Act
        ResponseEntity<?> response = scheduleRestController.createOrUpdateSchedules(1L, newSchedules);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Schedule> savedSchedules = (List<Schedule>) response.getBody();
        assertNotNull(savedSchedules);
        assertEquals(1, savedSchedules.size());

        verify(scheduleRepository, never()).deleteAll(any());
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void createOrUpdateSchedules_WithNullIsActive_ShouldDefaultToTrue() {
        // Arrange
        when(sportsHallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(scheduleRepository.findBySportsHallIdOrderByDayOfWeek(1L)).thenReturn(Collections.emptyList());

        Schedule scheduleWithNullActive = createScheduleData(1, "07:00", "23:00", null);
        List<Schedule> newSchedules = Arrays.asList(scheduleWithNullActive);

        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setId(1L);
            return schedule;
        });

        // Act
        ResponseEntity<?> response = scheduleRestController.createOrUpdateSchedules(1L, newSchedules);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(scheduleRepository).save(argThat(schedule ->
                schedule.getIsActive() != null && schedule.getIsActive()));
    }

    @Test
    void createOrUpdateSchedules_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        List<Schedule> newSchedules = Arrays.asList(createScheduleData(1, "07:00", "23:00", true));

        // Act
        ResponseEntity<?> response = scheduleRestController.createOrUpdateSchedules(1L, newSchedules);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la salvarea programului"));
    }

    @Test
    void createDefaultSchedule_WithExistingHall_ShouldCreateDefaultSchedules() {
        // Arrange
        when(sportsHallRepository.findById(1L)).thenReturn(Optional.of(testHall));

        List<Schedule> defaultSchedules = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            Schedule schedule = new Schedule(testHall, day, "07:00", "23:00");
            schedule.setId((long) day);
            defaultSchedules.add(schedule);
        }

        when(scheduleRepository.findBySportsHallIdOrderByDayOfWeek(1L)).thenReturn(defaultSchedules);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setId(System.currentTimeMillis());
            return schedule;
        });

        // Act
        ResponseEntity<?> response = scheduleRestController.createDefaultSchedule(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Schedule> savedSchedules = (List<Schedule>) response.getBody();
        assertNotNull(savedSchedules);
        assertEquals(7, savedSchedules.size());

        verify(sportsHallRepository).findById(1L);
        verify(scheduleRepository).deleteBySportsHallId(1L);
        verify(scheduleRepository, times(7)).save(any(Schedule.class));
        verify(scheduleRepository).findBySportsHallIdOrderByDayOfWeek(1L);
    }

    @Test
    void createDefaultSchedule_WithNonExistingHall_ShouldReturnNotFound() {
        // Arrange
        when(sportsHallRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = scheduleRestController.createDefaultSchedule(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(sportsHallRepository).findById(999L);
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createDefaultSchedule_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = scheduleRestController.createDefaultSchedule(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la crearea programului standard"));
    }

    @Test
    void deleteSchedulesByHallId_WithValidId_ShouldReturnOk() {
        // Arrange
        doNothing().when(scheduleRepository).deleteBySportsHallId(1L);

        // Act
        ResponseEntity<?> response = scheduleRestController.deleteSchedulesByHallId(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(scheduleRepository).deleteBySportsHallId(1L);
    }

    @Test
    void deleteSchedulesByHallId_WithException_ShouldReturnInternalServerError() {
        // Arrange
        doThrow(new RuntimeException("Database error")).when(scheduleRepository).deleteBySportsHallId(1L);

        // Act
        ResponseEntity<?> response = scheduleRestController.deleteSchedulesByHallId(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la ștergerea programului"));
        verify(scheduleRepository).deleteBySportsHallId(1L);
    }

    @Test
    void deleteSchedulesByHallId_WithNonExistingId_ShouldStillReturnOk() {
        // Arrange
        doNothing().when(scheduleRepository).deleteBySportsHallId(999L);

        // Act
        ResponseEntity<?> response = scheduleRestController.deleteSchedulesByHallId(999L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(scheduleRepository).deleteBySportsHallId(999L);
    }

    // Helper method pentru a crea date de test
    private Schedule createScheduleData(Integer dayOfWeek, String startTime, String endTime, Boolean isActive) {
        Schedule schedule = new Schedule();
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setIsActive(isActive);
        return schedule;
    }
}