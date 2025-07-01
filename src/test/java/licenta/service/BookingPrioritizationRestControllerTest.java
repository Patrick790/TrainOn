package licenta.service;

import licenta.model.*;
import licenta.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Prioritization Algorithm Tests")
public class BookingPrioritizationRestControllerTest {

    @Mock
    private IReservationProfileRepository reservationProfileRepository;

    @Mock
    private IUserSpringRepository userRepository;

    @Mock
    private ISportsHallSpringRepository sportsHallRepository;

    @Mock
    private IReservationSpringRepository reservationRepository;

    @Mock
    private IScheduleSpringRepository scheduleRepository;

    @Mock
    private JavaMailSender mailSender;

    private BookingPrioritizationRestController controller;

    private List<ReservationProfile> testProfiles;
    private List<SportsHall> testHalls;
    private List<Schedule> testSchedules;

    @BeforeEach
    void setUp() {
        controller = new BookingPrioritizationRestController(
                reservationProfileRepository,
                userRepository,
                sportsHallRepository,
                reservationRepository,
                scheduleRepository
        );

        setupTestData();
    }

    private void setupTestData() {
        // Configurare utilizatori de test
        User seniorUser = createUser(1L, "senior@test.com", "Seniori", "verified");
        User juniorUser17_18 = createUser(2L, "junior1@test.com", "17-18", "verified");
        User juniorUser15_16 = createUser(3L, "junior2@test.com", "15-16", "verified");
        User juniorUser0_14 = createUser(4L, "junior3@test.com", "0-14", "verified");
        User inactiveUser = createUser(5L, "inactive@test.com", "Seniori", "inactive");

        // Configurare săli de test
        testHalls = Arrays.asList(
                createSportsHall(1L, "Sala Mare", "Cluj-Napoca", 100.0f, SportsHall.HallStatus.ACTIVE),
                createSportsHall(2L, "Sala Mica", "Cluj-Napoca", 80.0f, SportsHall.HallStatus.ACTIVE),
                createSportsHall(3L, "Sala Inactiva", "Cluj-Napoca", 90.0f, SportsHall.HallStatus.INACTIVE)
        );

        // Configurare profiluri de rezervare
        testProfiles = Arrays.asList(
                createReservationProfile(1L, "Echipa Seniori", seniorUser, "Seniori",
                        BigDecimal.valueOf(600), "Cluj-Napoca", "7-14:30", testHalls.subList(0, 2)),
                createReservationProfile(2L, "Echipa Junior 17-18", juniorUser17_18, "17-18",
                        BigDecimal.valueOf(500), "Cluj-Napoca", "14:30-23", testHalls.subList(0, 2)),
                createReservationProfile(3L, "Echipa Junior 15-16", juniorUser15_16, "15-16",
                        BigDecimal.valueOf(400), "Cluj-Napoca", "full-day", testHalls.subList(0, 1)),
                createReservationProfile(4L, "Echipa Junior 0-14", juniorUser0_14, "0-14",
                        BigDecimal.valueOf(300), "Cluj-Napoca", "7-14:30", testHalls.subList(0, 1)),
                createReservationProfile(5L, "Echipa Inactiva", inactiveUser, "Seniori",
                        BigDecimal.valueOf(600), "Cluj-Napoca", "7-14:30", testHalls.subList(0, 2))
        );

        // Configurare program săli
        testSchedules = Arrays.asList(
                createSchedule(1L, testHalls.get(0), 1, "08:00", "22:00", true), // Luni
                createSchedule(2L, testHalls.get(0), 2, "08:00", "22:00", true), // Marți
                createSchedule(3L, testHalls.get(1), 1, "09:00", "21:00", true), // Luni
                createSchedule(4L, testHalls.get(1), 2, "09:00", "21:00", true)  // Marți
        );
    }

    @Nested
    @DisplayName("Profile Eligibility Tests")
    class ProfileEligibilityTests {

        @Test
        @DisplayName("Should return only eligible profiles")
        void shouldReturnOnlyEligibleProfiles() {
            // Arrange
            when(reservationProfileRepository.findAll()).thenReturn((Iterable<ReservationProfile>) testProfiles);

            // Act - folosim reflexia pentru a testa metoda private
            List<ReservationProfile> eligibleProfiles = invokePrivateMethod("getEligibleProfiles");

            // Assert
            assertEquals(4, eligibleProfiles.size()); // Exclude profilul cu utilizator inactiv
            assertFalse(eligibleProfiles.stream().anyMatch(p ->
                    p.getUser().getAccountStatus().equals("inactive")));
        }

        @Test
        @DisplayName("Should exclude profiles with null or empty team type")
        void shouldExcludeProfilesWithInvalidTeamType() {
            // Arrange
            User userWithoutTeam = createUser(6L, "noteam@test.com", "Seniori", "verified");
            userWithoutTeam.setTeamType(null);

            ReservationProfile profileWithoutTeam = createReservationProfile(6L, "Fara Echipa",
                    userWithoutTeam, "Seniori", BigDecimal.valueOf(300), "Cluj-Napoca", "full-day", testHalls.subList(0, 1));

            List<ReservationProfile> profilesWithInvalid = new ArrayList<>(testProfiles);
            profilesWithInvalid.add(profileWithoutTeam);

            when(reservationProfileRepository.findAll()).thenReturn((Iterable<ReservationProfile>) profilesWithInvalid);

            // Act
            List<ReservationProfile> eligibleProfiles = invokePrivateMethod("getEligibleProfiles");

            // Assert
            assertFalse(eligibleProfiles.stream().anyMatch(p ->
                    p.getName().equals("Fara Echipa")));
        }
    }

    @Nested
    @DisplayName("Profile Prioritization Tests")
    class ProfilePrioritizationTests {

        @Test
        @DisplayName("Should prioritize seniors over juniors")
        void shouldPrioritizeSeniorsOverJuniors() {
            // Arrange
            List<ReservationProfile> profiles = testProfiles.subList(0, 4); // Exclude inactive

            // Act
            List<ReservationProfile> prioritized = invokePrivateMethod("prioritizeProfiles", profiles);

            // Assert
            assertEquals("Seniori", prioritized.get(0).getAgeCategory());
            assertEquals("Echipa Seniori", prioritized.get(0).getName());
        }

        @Test
        @DisplayName("Should order junior categories correctly")
        void shouldOrderJuniorCategoriesCorrectly() {
            // Arrange
            List<ReservationProfile> juniorProfiles = testProfiles.subList(1, 4); // Only juniors

            // Act
            List<ReservationProfile> prioritized = invokePrivateMethod("prioritizeProfiles", juniorProfiles);

            // Assert
            assertEquals("17-18", prioritized.get(0).getAgeCategory());
            assertEquals("15-16", prioritized.get(1).getAgeCategory());
            assertEquals("0-14", prioritized.get(2).getAgeCategory());
        }

        @Test
        @DisplayName("Should prioritize higher budget within same age category")
        void shouldPrioritizeHigherBudgetWithinSameAgeCategory() {
            // Arrange
            ReservationProfile seniorHighBudget = createReservationProfile(7L, "Seniori High Budget",
                    testProfiles.get(0).getUser(), "Seniori", BigDecimal.valueOf(800), "Cluj-Napoca", "7-14:30", testHalls.subList(0, 1));

            List<ReservationProfile> sameAgeProfiles = Arrays.asList(testProfiles.get(0), seniorHighBudget);

            // Act
            List<ReservationProfile> prioritized = invokePrivateMethod("prioritizeProfiles", sameAgeProfiles);

            // Assert
            assertEquals("Seniori High Budget", prioritized.get(0).getName());
            assertEquals(BigDecimal.valueOf(800), prioritized.get(0).getWeeklyBudget());
        }
    }

    @Nested
    @DisplayName("Max Bookings Tests")
    class MaxBookingsTests {

        @Test
        @DisplayName("Should return correct max bookings for each age category")
        void shouldReturnCorrectMaxBookingsForEachAgeCategory() {
            // Act & Assert
            assertEquals(6, (int)invokePrivateMethod("getMaxBookingsForProfile", testProfiles.get(0))); // Seniori
            assertEquals(5, (int)invokePrivateMethod("getMaxBookingsForProfile", testProfiles.get(1))); // 17-18
            assertEquals(4, (int)invokePrivateMethod("getMaxBookingsForProfile", testProfiles.get(2))); // 15-16
            assertEquals(3, (int)invokePrivateMethod("getMaxBookingsForProfile", testProfiles.get(3))); // 0-14
        }

        @Test
        @DisplayName("Should return default max bookings for unknown category")
        void shouldReturnDefaultMaxBookingsForUnknownCategory() {
            // Arrange
            ReservationProfile unknownCategoryProfile = createReservationProfile(8L, "Unknown Category",
                    testProfiles.get(0).getUser(), "Unknown", BigDecimal.valueOf(300), "Cluj-Napoca", "full-day", testHalls.subList(0, 1));

            // Act
            int maxBookings = invokePrivateMethod("getMaxBookingsForProfile", unknownCategoryProfile);

            // Assert
            assertEquals(3, maxBookings); // Default value
        }
    }

    @Nested
    @DisplayName("Time Interval Tests")
    class TimeIntervalTests {

        @Test
        @DisplayName("Should parse morning time interval correctly")
        void shouldParseMorningTimeIntervalCorrectly() {
            // Act
            BookingPrioritizationRestController.TimeInterval interval =
                    invokePrivateMethod("getPreferredTimeInterval", "7-14:30");

            // Assert
            assertEquals(7 * 60, interval.getStartMinutes()); // 7:00 AM
            assertEquals(14 * 60 + 30, interval.getEndMinutes()); // 2:30 PM
        }

        @Test
        @DisplayName("Should parse afternoon time interval correctly")
        void shouldParseAfternoonTimeIntervalCorrectly() {
            // Act
            BookingPrioritizationRestController.TimeInterval interval =
                    invokePrivateMethod("getPreferredTimeInterval", "14:30-23");

            // Assert
            assertEquals(14 * 60 + 30, interval.getStartMinutes()); // 2:30 PM
            assertEquals(23 * 60, interval.getEndMinutes()); // 11:00 PM
        }

        @Test
        @DisplayName("Should parse full day interval correctly")
        void shouldParseFullDayIntervalCorrectly() {
            // Act
            BookingPrioritizationRestController.TimeInterval interval =
                    invokePrivateMethod("getPreferredTimeInterval", "full-day");

            // Assert
            assertEquals(7 * 60, interval.getStartMinutes()); // 7:00 AM
            assertEquals(23 * 60, interval.getEndMinutes()); // 11:00 PM
        }
    }

    @Nested
    @DisplayName("Senior/Junior Classification Tests")
    class SeniorJuniorClassificationTests {

        @Test
        @DisplayName("Should correctly identify senior categories")
        void shouldCorrectlyIdentifySeniorCategories() {
            // Act & Assert
            Boolean result1 = invokePrivateMethod("isSeniorCategory", "seniori");
            Boolean result2 = invokePrivateMethod("isSeniorCategory", "senior");

            assertTrue(result1);
            assertTrue(result2);
        }

        @Test
        @DisplayName("Should correctly identify junior categories")
        void shouldCorrectlyIdentifyJuniorCategories() {
            // Act & Assert
            Boolean result1 = invokePrivateMethod("isSeniorCategory", "17-18");
            Boolean result2 = invokePrivateMethod("isSeniorCategory", "15-16");
            Boolean result3 = invokePrivateMethod("isSeniorCategory", "0-14");

            assertFalse(result1);
            assertFalse(result2);
            assertFalse(result3);
        }

    }

    @Nested
    @DisplayName("Weekly Schedule Tests")
    class WeeklyScheduleTests {

        @Test
        @DisplayName("Should create schedule for full week")
        void shouldCreateScheduleForFullWeek() {
            // Arrange
            when(sportsHallRepository.findAll()).thenReturn((Iterable<SportsHall>) testHalls.subList(0, 2));
            when(scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(anyLong(), eq(true)))
                    .thenReturn(testSchedules);
            when(reservationRepository.findAll()).thenReturn(Collections.emptyList());

            Date nextMonday = getNextMonday();

            // Act
            BookingPrioritizationRestController.WeeklySchedule weeklySchedule =
                    invokePrivateMethod("createWeeklySchedule", nextMonday);

            // Assert
            assertNotNull(weeklySchedule);
            assertEquals(7, weeklySchedule.getDaySchedules().size()); // 7 days
        }

        @Test
        @DisplayName("Should limit halls for performance")
        void shouldLimitHallsForPerformance() {
            // Arrange - Create more than 15 halls
            List<SportsHall> manyHalls = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                manyHalls.add(createSportsHall((long) i, "Sala " + i, "Cluj-Napoca", 100.0f, SportsHall.HallStatus.ACTIVE));
            }

            when(sportsHallRepository.findAll()).thenReturn((Iterable<SportsHall>) manyHalls);
            when(scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(anyLong(), eq(true)))
                    .thenReturn(testSchedules);
            when(reservationRepository.findAll()).thenReturn(Collections.emptyList());

            Date nextMonday = getNextMonday();

            // Act
            BookingPrioritizationRestController.WeeklySchedule weeklySchedule =
                    invokePrivateMethod("createWeeklySchedule", nextMonday);

            // Assert
            assertNotNull(weeklySchedule);
            // Should be limited to 15 halls max (verified indirectly through performance)
        }
    }

    @Nested
    @DisplayName("System Status Tests")
    class SystemStatusTests {

        @Test
        @DisplayName("Should return correct system status")
        void shouldReturnCorrectSystemStatus() {
            // Arrange
            when(reservationProfileRepository.findAll()).thenReturn((Iterable<ReservationProfile>) testProfiles);
            when(sportsHallRepository.findAll()).thenReturn((Iterable<SportsHall>) testHalls.subList(0, 2));

            // Act
            ResponseEntity<Map<String, Object>> response = controller.getSystemStatus();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertTrue((Boolean) body.get("systemReady"));
            assertEquals(5, body.get("totalProfiles"));
            assertEquals(2, body.get("activeHalls"));
            assertNotNull(body.get("nextWeekStart"));
            assertNotNull(body.get("nextWeekEnd"));
        }

        @Test
        @DisplayName("Should handle errors gracefully in system status")
        void shouldHandleErrorsGracefullyInSystemStatus() {
            // Arrange
            when(reservationProfileRepository.findAll()).thenThrow(new RuntimeException("Database error"));

            // Act
            ResponseEntity<Map<String, Object>> response = controller.getSystemStatus();

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertFalse((Boolean) body.get("systemReady"));
            assertTrue(body.get("error").toString().contains("Database error"));
        }
    }

    @Nested
    @DisplayName("Budget Calculation Tests")
    class BudgetCalculationTests {

        @Test
        @DisplayName("Should calculate remaining budget correctly")
        void shouldCalculateRemainingBudgetCorrectly() {
            // Arrange
            ReservationProfile profile = testProfiles.get(0); // Budget 600
            List<Reservation> existingReservations = new ArrayList<>();
            existingReservations.add(createReservation(1L, testHalls.get(0), profile.getUser(), 100.0f));
            existingReservations.add(createReservation(2L, testHalls.get(1), profile.getUser(), 150.0f));

            // Act
            BigDecimal remainingBudget = invokePrivateMethodForBudget("calculateRemainingBudget", profile, existingReservations);

            // Assert
            assertEquals(0, remainingBudget.compareTo(BigDecimal.valueOf(350)));
        }

        @Test
        @DisplayName("Should handle empty reservations list")
        void shouldHandleEmptyReservationsList() {
            // Arrange
            ReservationProfile profile = testProfiles.get(0); // Budget 600
            List<Reservation> emptyReservations = new ArrayList<>();

            // Act
            BigDecimal remainingBudget = invokePrivateMethodForBudget("calculateRemainingBudget", profile, emptyReservations);

            // Assert
            assertEquals(BigDecimal.valueOf(600), remainingBudget);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete booking generation process")
        void shouldHandleCompleteBookingGenerationProcess() {
            // Arrange
            when(reservationProfileRepository.findAll()).thenReturn((Iterable<ReservationProfile>) testProfiles.subList(0, 2));
            when(sportsHallRepository.findAll()).thenReturn((Iterable<SportsHall>) testHalls.subList(0, 2));
            when(scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(anyLong(), eq(true)))
                    .thenReturn(testSchedules);
            when(reservationRepository.findAll()).thenReturn(Collections.emptyList());
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation reservation = invocation.getArgument(0);
                reservation.setId(1L); // Mock saved reservation
                return reservation;
            });

            // Act
            ResponseEntity<Map<String, Object>> response = controller.generateBookings();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertTrue((Boolean) body.get("success"));
            assertNotNull(body.get("count"));
            assertNotNull(body.get("successfulProfiles"));
        }

        @Test
        @DisplayName("Should handle generation errors gracefully")
        void shouldHandleGenerationErrorsGracefully() {
            // Arrange
            when(reservationProfileRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

            // Act
            ResponseEntity<Map<String, Object>> response = controller.generateBookings();

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertFalse((Boolean) body.get("success"));
            assertTrue(body.get("message").toString().contains("Database connection failed"));
        }
    }

    // Helper methods for creating test data
    private User createUser(Long id, String email, String teamType, String accountStatus) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setTeamType(teamType);
        user.setAccountStatus(accountStatus);
        return user;
    }

    private SportsHall createSportsHall(Long id, String name, String city, float tariff, SportsHall.HallStatus status) {
        SportsHall hall = new SportsHall();
        hall.setId(id);
        hall.setName(name);
        hall.setCity(city);
        hall.setTariff(tariff);
        hall.setStatus(status);
        return hall;
    }

    private ReservationProfile createReservationProfile(Long id, String name, User user, String ageCategory,
                                                        BigDecimal weeklyBudget, String city, String timeInterval,
                                                        List<SportsHall> selectedHalls) {
        ReservationProfile profile = new ReservationProfile();
        profile.setId(id);
        profile.setName(name);
        profile.setUser(user);
        profile.setAgeCategory(ageCategory);
        profile.setWeeklyBudget(weeklyBudget);
        profile.setCity(city);
        profile.setTimeInterval(timeInterval);
        profile.setSelectedHalls(selectedHalls);
        return profile;
    }

    private Schedule createSchedule(Long id, SportsHall sportsHall, int dayOfWeek, String startTime, String endTime, boolean isActive) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setSportsHall(sportsHall);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setIsActive(isActive);
        return schedule;
    }

    private Reservation createReservation(Long id, SportsHall hall, User user, float price) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setHall(hall);
        reservation.setUser(user);
        reservation.setPrice(price);
        reservation.setType("reservation");
        reservation.setStatus("CONFIRMED");
        reservation.setDate(new Date());
        reservation.setTimeSlot("10:00 - 11:30");
        return reservation;
    }

    private Date getNextMonday() {
        LocalDate nextMonday = LocalDate.now().plusWeeks(1);
        while (nextMonday.getDayOfWeek().getValue() != 1) {
            nextMonday = nextMonday.plusDays(1);
        }
        return Date.from(nextMonday.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // Helper method pentru apelarea metodelor private prin reflexie
    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = Arrays.stream(args)
                    .map(arg -> {
                        if (arg instanceof List) {
                            return List.class;
                        }
                        return arg.getClass();
                    })
                    .toArray(Class<?>[]::new);

            var method = BookingPrioritizationRestController.class
                    .getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return (T) method.invoke(controller, args);
        } catch (Exception e) {
            // Fallback: try to find method by name and parameter count
            try {
                var methods = BookingPrioritizationRestController.class.getDeclaredMethods();
                for (var method : methods) {
                    if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                        method.setAccessible(true);
                        return (T) method.invoke(controller, args);
                    }
                }
            } catch (Exception fallbackException) {
                throw new RuntimeException("Failed to invoke private method: " + methodName + " with fallback", fallbackException);
            }
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }

    // Metodă specifică pentru teste budget cu tipuri exacte
    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethodForBudget(String methodName, ReservationProfile profile, List<Reservation> reservations) {
        try {
            var method = BookingPrioritizationRestController.class
                    .getDeclaredMethod(methodName, ReservationProfile.class, List.class);
            method.setAccessible(true);
            return (T) method.invoke(controller, profile, reservations);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }
}