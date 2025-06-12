package licenta.service;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FcfsStatusControllerTest {

    @Mock
    private IUserSpringRepository userSpringRepository;

    @Mock
    private Authentication authentication;

    private FcfsStatusController fcfsStatusController;

    private User normalUserFcfsEnabled;
    private User normalUserFcfsDisabled;
    private User adminUser;
    private User hallAdminUser;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        fcfsStatusController = new FcfsStatusController(userSpringRepository);

        // Configurăm utilizatorii de test
        normalUserFcfsEnabled = new User();
        normalUserFcfsEnabled.setId(1L);
        normalUserFcfsEnabled.setEmail("user1@example.com");
        normalUserFcfsEnabled.setUserType("user");
        normalUserFcfsEnabled.setFcfsEnabled(true);

        normalUserFcfsDisabled = new User();
        normalUserFcfsDisabled.setId(2L);
        normalUserFcfsDisabled.setEmail("user2@example.com");
        normalUserFcfsDisabled.setUserType("user");
        normalUserFcfsDisabled.setFcfsEnabled(false);

        adminUser = new User();
        adminUser.setId(3L);
        adminUser.setEmail("admin@example.com");
        adminUser.setUserType("admin");
        adminUser.setFcfsEnabled(true);

        hallAdminUser = new User();
        hallAdminUser.setId(4L);
        hallAdminUser.setEmail("halladmin@example.com");
        hallAdminUser.setUserType("hall_admin");
        hallAdminUser.setFcfsEnabled(true);

        testUsers = Arrays.asList(normalUserFcfsEnabled, normalUserFcfsDisabled, adminUser, hallAdminUser);
    }

    @Test
    void getFcfsStatus_WithNormalUserFcfsEnabled_ShouldReturnTrue() {
        // Arrange
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getFcfsStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("fcfsEnabled"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void getFcfsStatus_WithAllNormalUsersFcfsDisabled_ShouldReturnFalse() {
        // Arrange
        normalUserFcfsEnabled.setFcfsEnabled(false);
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getFcfsStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("fcfsEnabled"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void getFcfsStatus_WithNoNormalUsers_ShouldReturnDefaultTrue() {
        // Arrange
        List<User> usersWithoutNormal = Arrays.asList(adminUser, hallAdminUser);
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) usersWithoutNormal);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getFcfsStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("fcfsEnabled"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void getFcfsStatus_WithException_ShouldReturnDefaultTrue() {
        // Arrange
        when(userSpringRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getFcfsStatus();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("fcfsEnabled"));
        assertNotNull(body.get("timestamp"));
        assertEquals("Error occurred, returning default enabled status", body.get("note"));
    }

    @Test
    void checkFcfsAccess_WithNullAuthentication_ShouldReturnCannotAccess() {
        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.checkFcfsAccess(null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("canAccess"));
        assertEquals("Nu sunteți autentificat", body.get("reason"));
        assertFalse((Boolean) body.get("fcfsEnabled"));
    }

    @Test
    void checkFcfsAccess_WithUnauthenticatedUser_ShouldReturnCannotAccess() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.checkFcfsAccess(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("canAccess"));
        assertEquals("Nu sunteți autentificat", body.get("reason"));
        assertFalse((Boolean) body.get("fcfsEnabled"));
    }

    @Test
    void checkFcfsAccess_WithNonExistentUser_ShouldReturnCannotAccess() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userSpringRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.checkFcfsAccess(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("canAccess"));
        assertEquals("Utilizatorul nu a fost găsit în sistem", body.get("reason"));
        assertFalse((Boolean) body.get("fcfsEnabled"));
    }

    @Test
    void checkFcfsAccess_AsAdmin_ShouldReturnCanAccess() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(adminUser.getEmail());
        when(userSpringRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.checkFcfsAccess(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("canAccess"));
        assertTrue((Boolean) body.get("fcfsEnabled"));
        assertEquals("admin", body.get("userType"));
        assertTrue(body.get("reason").toString().contains("Admin access"));
    }

    @Test
    void checkFcfsAccess_AsNormalUserWithFcfsEnabled_ShouldReturnCanAccess() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(normalUserFcfsEnabled.getEmail());
        when(userSpringRepository.findByEmail(normalUserFcfsEnabled.getEmail())).thenReturn(Optional.of(normalUserFcfsEnabled));
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.checkFcfsAccess(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("canAccess"));
        assertTrue((Boolean) body.get("fcfsEnabled"));
        assertEquals("user", body.get("userType"));
        assertEquals("Puteți rezerva locurile rămase", body.get("reason"));
    }

    @Test
    void checkFcfsAccess_AsNormalUserWithFcfsDisabled_ShouldReturnCannotAccess() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(normalUserFcfsDisabled.getEmail());
        when(userSpringRepository.findByEmail(normalUserFcfsDisabled.getEmail())).thenReturn(Optional.of(normalUserFcfsDisabled));
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.checkFcfsAccess(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("canAccess"));
        assertTrue((Boolean) body.get("fcfsEnabled")); // Global status is still true
        assertEquals("user", body.get("userType"));
        assertEquals("Rezervarea locurilor rămase a fost dezactivată pentru contul dumneavoastră", body.get("reason"));
    }

    @Test
    void checkFcfsAccess_AsHallAdmin_ShouldReturnCannotAccess() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(hallAdminUser.getEmail());
        when(userSpringRepository.findByEmail(hallAdminUser.getEmail())).thenReturn(Optional.of(hallAdminUser));
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.checkFcfsAccess(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("canAccess"));
        assertTrue((Boolean) body.get("fcfsEnabled"));
        assertEquals("hall_admin", body.get("userType"));
        assertEquals("Nu aveți permisiunea să accesați această funcționalitate", body.get("reason"));
    }

    @Test
    void checkFcfsAccess_WithException_ShouldReturnDefaultAccess() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(normalUserFcfsEnabled.getEmail());
        when(userSpringRepository.findByEmail(normalUserFcfsEnabled.getEmail())).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.checkFcfsAccess(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("canAccess"));
        assertTrue((Boolean) body.get("fcfsEnabled"));
        assertEquals("Error occurred, returning default access granted", body.get("note"));
    }

    @Test
    void getUserFcfsStatus_WithNullAuthentication_ShouldReturnError() {
        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getUserFcfsStatus(null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Nu sunteți autentificat", body.get("error"));
    }

    @Test
    void getUserFcfsStatus_WithUnauthenticatedUser_ShouldReturnError() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getUserFcfsStatus(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Nu sunteți autentificat", body.get("error"));
    }

    @Test
    void getUserFcfsStatus_WithNonExistentUser_ShouldReturnError() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userSpringRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getUserFcfsStatus(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Utilizatorul nu a fost găsit", body.get("error"));
    }

    @Test
    void getUserFcfsStatus_WithValidUser_ShouldReturnUserStatus() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(normalUserFcfsEnabled.getEmail());
        when(userSpringRepository.findByEmail(normalUserFcfsEnabled.getEmail())).thenReturn(Optional.of(normalUserFcfsEnabled));
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getUserFcfsStatus(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(normalUserFcfsEnabled.getId(), body.get("userId"));
        assertEquals(normalUserFcfsEnabled.getUserType(), body.get("userType"));
        assertTrue((Boolean) body.get("userFcfsEnabled"));
        assertTrue((Boolean) body.get("globalFcfsEnabled"));
        assertTrue((Boolean) body.get("canAccess"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void getUserFcfsStatus_WithAdminUser_ShouldReturnAdminStatus() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(adminUser.getEmail());
        when(userSpringRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getUserFcfsStatus(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(adminUser.getId(), body.get("userId"));
        assertEquals("admin", body.get("userType"));
        assertTrue((Boolean) body.get("userFcfsEnabled"));
        assertTrue((Boolean) body.get("globalFcfsEnabled"));
        assertTrue((Boolean) body.get("canAccess")); // Admin can always access
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void getUserFcfsStatus_WithUserFcfsDisabled_ShouldReturnDisabledStatus() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(normalUserFcfsDisabled.getEmail());
        when(userSpringRepository.findByEmail(normalUserFcfsDisabled.getEmail())).thenReturn(Optional.of(normalUserFcfsDisabled));
        when(userSpringRepository.findAll()).thenReturn((Iterable<User>) testUsers);

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getUserFcfsStatus(authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(normalUserFcfsDisabled.getId(), body.get("userId"));
        assertEquals("user", body.get("userType"));
        assertFalse((Boolean) body.get("userFcfsEnabled"));
        assertTrue((Boolean) body.get("globalFcfsEnabled")); // Global is still enabled
        assertFalse((Boolean) body.get("canAccess")); // User cannot access
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void getUserFcfsStatus_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(normalUserFcfsEnabled.getEmail());
        when(userSpringRepository.findByEmail(normalUserFcfsEnabled.getEmail())).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<Map<String, Object>> response = fcfsStatusController.getUserFcfsStatus(authentication);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("Eroare la obținerea statusului utilizatorului"));
        assertTrue(body.get("error").toString().contains("Database error"));
    }
}