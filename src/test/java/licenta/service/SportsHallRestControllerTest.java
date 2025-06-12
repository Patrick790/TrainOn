package licenta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import licenta.model.SportsHall;
import licenta.model.SportsHallImage;
import licenta.model.User;
import licenta.persistence.ISportsHallImageSpringRepository;
import licenta.persistence.ISportsHallSpringRepository;
import licenta.persistence.IUserSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SportsHallRestControllerTest {

    @Mock
    private ISportsHallSpringRepository sportsHallSpringRepository;

    @Mock
    private ISportsHallImageSpringRepository sportsHallImageSpringRepository;

    @Mock
    private IUserSpringRepository userSpringRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private SportsHallRestController sportsHallRestController;

    private SportsHall testHall1;
    private SportsHall testHall2;
    private SportsHall testHall3;
    private User adminUser;
    private User hallAdminUser;
    private User regularUser;
    private List<SportsHall> testHalls;
    private List<String> testCities;

    @BeforeEach
    void setUp() {
        sportsHallRestController = new SportsHallRestController(
                sportsHallSpringRepository,
                sportsHallImageSpringRepository,
                userSpringRepository,
                objectMapper
        );

        // Reset SecurityContextHolder
        SecurityContextHolder.clearContext();

        // Configurăm utilizatorii de test
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@example.com");
        adminUser.setUserType("admin");

        hallAdminUser = new User();
        hallAdminUser.setId(2L);
        hallAdminUser.setEmail("halladmin@example.com");
        hallAdminUser.setUserType("hall_admin");

        regularUser = new User();
        regularUser.setId(3L);
        regularUser.setEmail("user@example.com");
        regularUser.setUserType("user");

        // Configurăm sălile de test
        testHall1 = new SportsHall();
        testHall1.setId(1L);
        testHall1.setName("Sala Test 1");
        testHall1.setCity("Cluj-Napoca");
        testHall1.setType("fotbal");
        testHall1.setStatus(SportsHall.HallStatus.ACTIVE);
        testHall1.setAdminId(2L); // hallAdminUser

        testHall2 = new SportsHall();
        testHall2.setId(2L);
        testHall2.setName("Sala Test 2");
        testHall2.setCity("Bucuresti");
        testHall2.setType("inot");
        testHall2.setStatus(SportsHall.HallStatus.ACTIVE);
        testHall2.setAdminId(2L);

        testHall3 = new SportsHall();
        testHall3.setId(3L);
        testHall3.setName("Sala Test 3");
        testHall3.setCity("Cluj-Napoca");
        testHall3.setType("baschet");
        testHall3.setStatus(SportsHall.HallStatus.INACTIVE);
        testHall3.setAdminId(1L); // adminUser

        testHalls = Arrays.asList(testHall1, testHall2, testHall3);
        testCities = Arrays.asList("Cluj-Napoca", "Bucuresti", "Timisoara");
    }

    private void mockAuthentication(User user) {
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(user.getUserType()));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAvailableCities_ShouldReturnCitiesList() {
        // Arrange
        when(sportsHallSpringRepository.findDistinctCities()).thenReturn(testCities);

        // Act
        ResponseEntity<?> response = sportsHallRestController.getAvailableCities();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testCities, response.getBody());
        verify(sportsHallSpringRepository).findDistinctCities();
    }

    @Test
    void getAvailableCities_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallSpringRepository.findDistinctCities()).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = sportsHallRestController.getAvailableCities();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la obținerea orașelor"));
    }

    @Test
    void getSportsHallsByAdminId_WithValidId_ShouldReturnHalls() {
        // Arrange
        List<SportsHall> adminHalls = Arrays.asList(testHall1, testHall2);
        when(sportsHallSpringRepository.findByAdminId(2L)).thenReturn(adminHalls);

        // Act
        ResponseEntity<?> response = sportsHallRestController.getSportsHallsByAdminId(2L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(adminHalls, response.getBody());
        verify(sportsHallSpringRepository).findByAdminId(2L);
    }

    @Test
    void getSportsHallsByAdminId_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallSpringRepository.findByAdminId(2L)).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = sportsHallRestController.getSportsHallsByAdminId(2L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la obtinerea salilor de sport"));
    }

    @Test
    void getAllSportsHalls_WithoutCity_ShouldReturnAllHalls() {
        // Arrange
        when(sportsHallSpringRepository.findAll()).thenReturn((Iterable<SportsHall>) testHalls);

        // Act
        ResponseEntity<?> response = sportsHallRestController.getAllSportsHalls(null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testHalls, response.getBody());
        verify(sportsHallSpringRepository).findAll();
    }

    @Test
    void getAllSportsHalls_WithCity_ShouldReturnFilteredHalls() {
        // Arrange
        List<SportsHall> clujHalls = Arrays.asList(testHall1, testHall3);
        when(sportsHallSpringRepository.findByCityOrderByName("Cluj-Napoca")).thenReturn(clujHalls);

        // Act
        ResponseEntity<?> response = sportsHallRestController.getAllSportsHalls("Cluj-Napoca");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(clujHalls, response.getBody());
        verify(sportsHallSpringRepository).findByCityOrderByName("Cluj-Napoca");
    }

    @Test
    void getAllSportsHalls_WithEmptyCity_ShouldReturnAllHalls() {
        // Arrange
        when(sportsHallSpringRepository.findAll()).thenReturn((Iterable<SportsHall>) testHalls);

        // Act
        ResponseEntity<?> response = sportsHallRestController.getAllSportsHalls("   ");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testHalls, response.getBody());
        verify(sportsHallSpringRepository).findAll();
    }

    @Test
    void getAllSportsHalls_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallSpringRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = sportsHallRestController.getAllSportsHalls(null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la obținerea sălilor de sport"));
    }

    @Test
    void getSportsHallById_WithExistingId_ShouldReturnHall() {
        // Arrange
        when(sportsHallSpringRepository.findById(1L)).thenReturn(Optional.of(testHall1));

        // Act
        SportsHall result = sportsHallRestController.getSportsHallById(1L);

        // Assert
        assertEquals(testHall1, result);
        verify(sportsHallSpringRepository).findById(1L);
    }

    @Test
    void getSportsHallById_WithNonExistingId_ShouldReturnNull() {
        // Arrange
        when(sportsHallSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        SportsHall result = sportsHallRestController.getSportsHallById(999L);

        // Assert
        assertNull(result);
        verify(sportsHallSpringRepository).findById(999L);
    }

    @Test
    void deleteSportsHall_WithValidId_ShouldReturnOk() {
        // Arrange
        doNothing().when(sportsHallSpringRepository).deleteById(1L);

        // Act
        ResponseEntity<?> response = sportsHallRestController.deleteSportsHall(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(sportsHallSpringRepository).deleteById(1L);
    }

    @Test
    void deleteSportsHall_WithException_ShouldReturnInternalServerError() {
        // Arrange
        doThrow(new RuntimeException("Delete failed")).when(sportsHallSpringRepository).deleteById(1L);

        // Act
        ResponseEntity<?> response = sportsHallRestController.deleteSportsHall(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la ștergerea sălii"));
    }

    @Test
    void getActiveHalls_WithoutCity_ShouldReturnActiveHalls() {
        // Arrange
        List<SportsHall> activeHalls = Arrays.asList(testHall1, testHall2);
        when(sportsHallSpringRepository.findByStatusOrderByName(SportsHall.HallStatus.ACTIVE)).thenReturn(activeHalls);

        // Act
        ResponseEntity<?> response = sportsHallRestController.getActiveHalls(null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(activeHalls, response.getBody());
        verify(sportsHallSpringRepository).findByStatusOrderByName(SportsHall.HallStatus.ACTIVE);
    }

    @Test
    void getActiveHalls_WithCity_ShouldReturnFilteredActiveHalls() {
        // Arrange
        List<SportsHall> clujActiveHalls = Arrays.asList(testHall1);
        when(sportsHallSpringRepository.findByCityAndStatusOrderByName("Cluj-Napoca", SportsHall.HallStatus.ACTIVE))
                .thenReturn(clujActiveHalls);

        // Act
        ResponseEntity<?> response = sportsHallRestController.getActiveHalls("Cluj-Napoca");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(clujActiveHalls, response.getBody());
        verify(sportsHallSpringRepository).findByCityAndStatusOrderByName("Cluj-Napoca", SportsHall.HallStatus.ACTIVE);
    }

    @Test
    void getActiveHalls_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallSpringRepository.findByStatusOrderByName(SportsHall.HallStatus.ACTIVE))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = sportsHallRestController.getActiveHalls(null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la obținerea sălilor active"));
    }

    @Test
    void getHallNamesSuggestions_WithValidQuery_ShouldReturnSuggestions() {
        // Arrange
        List<String> suggestions = Arrays.asList("Sala Test 1", "Sala Test 2");
        when(sportsHallSpringRepository.findHallNamesSuggestionsLimited("Sala")).thenReturn(suggestions);

        // Act
        ResponseEntity<?> response = sportsHallRestController.getHallNamesSuggestions("Sala");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(suggestions, response.getBody());
        verify(sportsHallSpringRepository).findHallNamesSuggestionsLimited("Sala");
    }

    @Test
    void getHallNamesSuggestions_WithShortQuery_ShouldReturnEmptyList() {
        // Arrange & Act
        ResponseEntity<?> response = sportsHallRestController.getHallNamesSuggestions("S");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.emptyList(), response.getBody());
        verify(sportsHallSpringRepository, never()).findHallNamesSuggestionsLimited(anyString());
    }

    @Test
    void getHallNamesSuggestions_WithNullQuery_ShouldReturnEmptyList() {
        // Arrange & Act
        ResponseEntity<?> response = sportsHallRestController.getHallNamesSuggestions(null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.emptyList(), response.getBody());
        verify(sportsHallSpringRepository, never()).findHallNamesSuggestionsLimited(anyString());
    }

    @Test
    void getHallNamesSuggestions_WithException_ShouldReturnEmptyList() {
        // Arrange
        when(sportsHallSpringRepository.findHallNamesSuggestionsLimited("Test"))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = sportsHallRestController.getHallNamesSuggestions("Test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.emptyList(), response.getBody());
    }

    @Test
    void searchSportsHalls_WithOnlyQuery_ShouldReturnResults() {
        // Arrange
        List<SportsHall> searchResults = Arrays.asList(testHall1);
        when(sportsHallSpringRepository.findByNameContainingIgnoreCase("Sala Test"))
                .thenReturn(searchResults);

        // Act
        ResponseEntity<?> response = sportsHallRestController.searchSportsHalls(null, null, "Sala Test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(searchResults, response.getBody());
        verify(sportsHallSpringRepository).findByNameContainingIgnoreCase("Sala Test");
    }

    @Test
    void searchSportsHalls_WithCityAndSport_ShouldReturnResults() {
        // Arrange
        List<SportsHall> searchResults = Arrays.asList(testHall1);
        when(sportsHallSpringRepository.findByCityAndSportIncludingMultipurpose("Cluj-Napoca", "fotbal"))
                .thenReturn(searchResults);

        // Act
        ResponseEntity<?> response = sportsHallRestController.searchSportsHalls("Cluj-Napoca", "fotbal", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(searchResults, response.getBody());
        verify(sportsHallSpringRepository).findByCityAndSportIncludingMultipurpose("Cluj-Napoca", "fotbal");
    }

    @Test
    void searchSportsHalls_WithSwimmingSport_ShouldReturnSwimmingHalls() {
        // Arrange
        List<SportsHall> swimmingHalls = Arrays.asList(testHall2);
        when(sportsHallSpringRepository.findSwimmingHalls()).thenReturn(swimmingHalls);

        // Act
        ResponseEntity<?> response = sportsHallRestController.searchSportsHalls(null, "inot", null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(swimmingHalls, response.getBody());
        verify(sportsHallSpringRepository).findSwimmingHalls();
    }

    @Test
    void searchSportsHalls_WithOnlyCity_ShouldReturnCityResults() {
        // Arrange
        List<SportsHall> cityResults = Arrays.asList(testHall1, testHall3);
        when(sportsHallSpringRepository.findByCityAndStatusOrderByName("Cluj-Napoca", SportsHall.HallStatus.ACTIVE))
                .thenReturn(cityResults);

        // Act
        ResponseEntity<?> response = sportsHallRestController.searchSportsHalls("Cluj-Napoca", null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(cityResults, response.getBody());
        verify(sportsHallSpringRepository).findByCityAndStatusOrderByName("Cluj-Napoca", SportsHall.HallStatus.ACTIVE);
    }

    @Test
    void searchSportsHalls_WithNoCriteria_ShouldReturnAllActiveHalls() {
        // Arrange
        List<SportsHall> allActiveHalls = Arrays.asList(testHall1, testHall2);
        when(sportsHallSpringRepository.findByStatusOrderByName(SportsHall.HallStatus.ACTIVE))
                .thenReturn(allActiveHalls);

        // Act
        ResponseEntity<?> response = sportsHallRestController.searchSportsHalls(null, null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(allActiveHalls, response.getBody());
        verify(sportsHallSpringRepository).findByStatusOrderByName(SportsHall.HallStatus.ACTIVE);
    }

    @Test
    void searchSportsHalls_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallSpringRepository.findByStatusOrderByName(SportsHall.HallStatus.ACTIVE))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = sportsHallRestController.searchSportsHalls(null, null, null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare la căutarea sălilor de sport"));
    }
}