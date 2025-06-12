package licenta.service;

import licenta.model.*;
import licenta.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationProfileRestControllerTest {

    @Mock
    private IReservationProfileRepository reservationProfileRepository;

    @Mock
    private IUserSpringRepository userSpringRepository;

    @Mock
    private ISportsHallSpringRepository sportsHallSpringRepository;

    @Mock
    private ICardPaymentMethodSpringRepository cardPaymentMethodRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private ReservationProfileRestController reservationProfileRestController;

    private User regularUser;
    private User adminUser;
    private User hallAdminUser;
    private ReservationProfile testProfile1;
    private ReservationProfile testProfile2;
    private SportsHall testHall1;
    private SportsHall testHall2;
    private CardPaymentMethod testCard;

    @BeforeEach
    void setUp() {
        reservationProfileRestController = new ReservationProfileRestController(
                reservationProfileRepository,
                userSpringRepository,
                sportsHallSpringRepository,
                cardPaymentMethodRepository
        );

        // Configurăm utilizatorii de test
        regularUser = new User();
        regularUser.setId(1L);
        regularUser.setEmail("user@example.com");
        regularUser.setName("Regular User");
        regularUser.setUserType("user");

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin User");
        adminUser.setUserType("admin");

        hallAdminUser = new User();
        hallAdminUser.setId(3L);
        hallAdminUser.setEmail("halladmin@example.com");
        hallAdminUser.setName("Hall Admin User");
        hallAdminUser.setUserType("hall_admin");

        // Configurăm sălile de test
        testHall1 = new SportsHall();
        testHall1.setId(100L);
        testHall1.setName("Sala Test 1");

        testHall2 = new SportsHall();
        testHall2.setId(200L);
        testHall2.setName("Sala Test 2");

        // Configurăm cardul de test
        testCard = new CardPaymentMethod();
        testCard.setId(1L);
        testCard.setUser(regularUser);
        testCard.setStripePaymentMethodId("pm_test_123");
        testCard.setCardLastFour("1234");
        testCard.setCardBrand("visa");
        testCard.setCardExpMonth(12);
        testCard.setCardExpYear(2025);
        testCard.setCardholderName("Test User");
        testCard.setIsActive(true);
        testCard.setIsDefault(true);

        // Configurăm profilurile de test
        testProfile1 = new ReservationProfile();
        testProfile1.setId(1L);
        testProfile1.setName("Profil Test 1");
        testProfile1.setUser(regularUser);
        testProfile1.setAgeCategory("adult");
        testProfile1.setTimeInterval("morning");
        testProfile1.setWeeklyBudget(new BigDecimal("200.00"));
        testProfile1.setCity("Cluj-Napoca");
        testProfile1.setSport("football");
        testProfile1.setSelectedHalls(Arrays.asList(testHall1, testHall2));
        testProfile1.setAutoPaymentEnabled(true);
        testProfile1.setAutoPaymentMethod(PaymentMethod.CARD);
        testProfile1.setDefaultCardPaymentMethod(testCard);

        testProfile2 = new ReservationProfile();
        testProfile2.setId(2L);
        testProfile2.setName("Profil Test 2");
        testProfile2.setUser(adminUser);
        testProfile2.setAgeCategory("senior");
        testProfile2.setTimeInterval("evening");
        testProfile2.setWeeklyBudget(new BigDecimal("300.00"));
        testProfile2.setCity("Bucuresti");
        testProfile2.setSport("basketball");
    }

    private void mockAuthentication(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userSpringRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAllProfiles_AsRegularUserWithoutUserId_ShouldReturnOwnProfiles() {
        // Arrange
        mockAuthentication(regularUser);
        List<ReservationProfile> userProfiles = Arrays.asList(testProfile1);
        when(reservationProfileRepository.findByUserId(regularUser.getId())).thenReturn(userProfiles);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getAllProfiles(null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userProfiles, response.getBody());
        verify(reservationProfileRepository).findByUserId(regularUser.getId());
    }

    @Test
    void getAllProfiles_AsRegularUserWithUserId_ShouldReturnForbidden() {
        // Arrange
        mockAuthentication(regularUser);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getAllProfiles(2L);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Acces interzis - doar adminii pot accesa această funcționalitate", response.getBody());
    }

    @Test
    void getAllProfiles_AsAdminWithUserId_ShouldReturnTargetUserProfiles() {
        // Arrange
        mockAuthentication(adminUser);
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        List<ReservationProfile> userProfiles = Arrays.asList(testProfile1);
        when(reservationProfileRepository.findByUserId(1L)).thenReturn(userProfiles);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getAllProfiles(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userProfiles, response.getBody());
        verify(reservationProfileRepository).findByUserId(1L);
    }

    @Test
    void getAllProfiles_AsAdminWithInvalidUserId_ShouldReturnNotFound() {
        // Arrange
        mockAuthentication(adminUser);
        when(userSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getAllProfiles(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Utilizatorul specificat nu a fost găsit", response.getBody());
    }

    @Test
    void getAllProfiles_WithException_ShouldReturnInternalServerError() {
        // Arrange
        mockAuthentication(regularUser);
        when(reservationProfileRepository.findByUserId(anyLong())).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getAllProfiles(null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Eroare la obținerea profilurilor", response.getBody());
    }

    @Test
    void getProfilesByUserId_AsAdminWithValidUserId_ShouldReturnProfiles() {
        // Arrange
        mockAuthentication(adminUser);
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        List<ReservationProfile> userProfiles = Arrays.asList(testProfile1);
        when(reservationProfileRepository.findByUserId(1L)).thenReturn(userProfiles);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getProfilesByUserId(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userProfiles, response.getBody());
    }

    @Test
    void getProfilesByUserId_AsRegularUser_ShouldReturnForbidden() {
        // Arrange
        mockAuthentication(regularUser);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getProfilesByUserId(2L);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Acces interzis - doar adminii pot accesa această funcționalitate", response.getBody());
    }

    @Test
    void getProfileById_WithExistingId_ShouldReturnProfile() {
        // Arrange
        when(reservationProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile1));

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getProfileById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testProfile1, response.getBody());
    }

    @Test
    void getProfileById_WithNonExistingId_ShouldReturnNotFound() {
        // Arrange
        when(reservationProfileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = reservationProfileRestController.getProfileById(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createProfile_WithValidData_ShouldReturnCreatedProfile() {
        // Arrange
        mockAuthentication(regularUser);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "Nou Profil");
        profileData.put("ageCategory", "adult");
        profileData.put("timeInterval", "morning");
        profileData.put("weeklyBudget", "250.00");
        profileData.put("city", "Cluj-Napoca");
        profileData.put("sport", "tennis");
        profileData.put("selectedHalls", Arrays.asList(100, 200));
        profileData.put("autoPaymentEnabled", true);
        profileData.put("autoPaymentMethod", "CARD");
        profileData.put("defaultCardPaymentMethodId", 1);

        List<Long> hallIds = Arrays.asList(100L, 200L);
        when(sportsHallSpringRepository.findAllById(hallIds)).thenReturn(Arrays.asList(testHall1, testHall2));
        when(cardPaymentMethodRepository.findById(1L)).thenReturn(Optional.of(testCard));

        ReservationProfile savedProfile = new ReservationProfile();
        savedProfile.setId(1L);
        savedProfile.setName("Nou Profil");
        savedProfile.setUser(regularUser);
        when(reservationProfileRepository.save(any(ReservationProfile.class))).thenReturn(savedProfile);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.createProfile(profileData);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(savedProfile, response.getBody());
        verify(reservationProfileRepository).save(any(ReservationProfile.class));
    }

    @Test
    void createProfile_WithoutWeeklyBudget_ShouldCreateProfileSuccessfully() {
        // Arrange
        mockAuthentication(regularUser);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "Profil Fără Buget");
        profileData.put("ageCategory", "adult");
        profileData.put("timeInterval", "morning");
        profileData.put("weeklyBudget", null);
        profileData.put("city", "Cluj-Napoca");
        profileData.put("sport", "tennis");

        ReservationProfile savedProfile = new ReservationProfile();
        savedProfile.setId(1L);
        when(reservationProfileRepository.save(any(ReservationProfile.class))).thenReturn(savedProfile);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.createProfile(profileData);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationProfileRepository).save(any(ReservationProfile.class));
    }

    @Test
    void createProfile_WithException_ShouldReturnInternalServerError() {
        // Arrange
        mockAuthentication(regularUser);
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "Test Profile");

        when(reservationProfileRepository.save(any(ReservationProfile.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = reservationProfileRestController.createProfile(profileData);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la crearea profilului"));
    }

    @Test
    void updateProfile_AsOwner_ShouldReturnUpdatedProfile() {
        // Arrange
        mockAuthentication(regularUser);
        when(reservationProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile1));

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "Profil Actualizat");
        profileData.put("ageCategory", "senior");
        profileData.put("weeklyBudget", "350.00");
        profileData.put("sport", "basketball");

        ReservationProfile updatedProfile = new ReservationProfile();
        updatedProfile.setId(1L);
        updatedProfile.setName("Profil Actualizat");
        when(reservationProfileRepository.save(any(ReservationProfile.class))).thenReturn(updatedProfile);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.updateProfile(1L, profileData);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedProfile, response.getBody());
        verify(reservationProfileRepository).save(any(ReservationProfile.class));
    }

    @Test
    void updateProfile_AsNonOwner_ShouldReturnForbidden() {
        // Arrange
        mockAuthentication(regularUser);
        when(reservationProfileRepository.findById(2L)).thenReturn(Optional.of(testProfile2));

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "Profil Actualizat");

        // Act
        ResponseEntity<?> response = reservationProfileRestController.updateProfile(2L, profileData);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Nu aveți permisiunea să editați acest profil", response.getBody());
    }

    @Test
    void updateProfile_AsAdmin_ShouldReturnUpdatedProfile() {
        // Arrange
        mockAuthentication(adminUser);
        when(reservationProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile1));

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "Profil Actualizat de Admin");

        ReservationProfile updatedProfile = new ReservationProfile();
        updatedProfile.setId(1L);
        updatedProfile.setName("Profil Actualizat de Admin");
        when(reservationProfileRepository.save(any(ReservationProfile.class))).thenReturn(updatedProfile);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.updateProfile(1L, profileData);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedProfile, response.getBody());
    }

    @Test
    void updateProfile_WithInvalidWeeklyBudget_ShouldReturnBadRequest() {
        // Arrange
        mockAuthentication(regularUser);
        when(reservationProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile1));

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("weeklyBudget", "invalid_amount");

        // Act
        ResponseEntity<?> response = reservationProfileRestController.updateProfile(1L, profileData);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Format invalid pentru bugetul săptămânal", response.getBody());
    }

    @Test
    void updateProfile_WithAutoPaymentSettings_ShouldUpdateSuccessfully() {
        // Arrange
        mockAuthentication(regularUser);
        when(reservationProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile1));
        when(cardPaymentMethodRepository.findById(1L)).thenReturn(Optional.of(testCard));

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("autoPaymentEnabled", true);
        profileData.put("autoPaymentMethod", "CARD");
        profileData.put("defaultCardPaymentMethodId", 1);
        profileData.put("autoPaymentThreshold", "50.00");
        profileData.put("maxWeeklyAutoPayment", "500.00");

        ReservationProfile updatedProfile = new ReservationProfile();
        updatedProfile.setId(1L);
        when(reservationProfileRepository.save(any(ReservationProfile.class))).thenReturn(updatedProfile);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.updateProfile(1L, profileData);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationProfileRepository).save(any(ReservationProfile.class));
    }

    @Test
    void deleteProfile_AsOwner_ShouldReturnOk() {
        // Arrange
        mockAuthentication(regularUser);
        when(reservationProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile1));
        doNothing().when(reservationProfileRepository).deleteById(1L);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.deleteProfile(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationProfileRepository).deleteById(1L);
    }

    @Test
    void deleteProfile_AsNonOwner_ShouldReturnForbidden() {
        // Arrange
        mockAuthentication(regularUser);
        when(reservationProfileRepository.findById(2L)).thenReturn(Optional.of(testProfile2));

        // Act
        ResponseEntity<?> response = reservationProfileRestController.deleteProfile(2L);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Nu aveți permisiunea să ștergeți acest profil", response.getBody());
    }

    @Test
    void deleteProfile_AsAdmin_ShouldReturnOk() {
        // Arrange
        mockAuthentication(adminUser);
        when(reservationProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile1));
        doNothing().when(reservationProfileRepository).deleteById(1L);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.deleteProfile(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationProfileRepository).deleteById(1L);
    }

    @Test
    void deleteProfile_WithException_ShouldReturnInternalServerError() {
        // Arrange
        mockAuthentication(regularUser);
        when(reservationProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile1));
        doThrow(new RuntimeException("Database error")).when(reservationProfileRepository).deleteById(1L);

        // Act
        ResponseEntity<?> response = reservationProfileRestController.deleteProfile(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Eroare la ștergerea profilului", response.getBody());
    }
}