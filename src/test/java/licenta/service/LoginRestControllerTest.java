package licenta.service;

import licenta.jwtservice.JwtService;
import licenta.jwtservice.UserInfoService;
import licenta.model.User;
import licenta.model.dtos.UserCredentials;
import licenta.persistence.IUserSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginRestControllerTest {

    @Mock
    private IUserSpringRepository userSpringRepository;

    @Mock
    private UserInfoService userInfoService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    private LoginRestController loginRestController;

    private UserCredentials validCredentials;
    private UserCredentials invalidCredentials;
    private User activeUser;
    private User pendingUser;
    private User rejectedUser;

    @BeforeEach
    void setUp() {
        // Inițializăm manual controller-ul
        loginRestController = new LoginRestController(userSpringRepository);

        // Setăm manual dependențele folosind reflexia
        try {
            java.lang.reflect.Field authManagerField = LoginRestController.class.getDeclaredField("authenticationManager");
            authManagerField.setAccessible(true);
            authManagerField.set(loginRestController, authenticationManager);

            java.lang.reflect.Field jwtServiceField = LoginRestController.class.getDeclaredField("jwtService");
            jwtServiceField.setAccessible(true);
            jwtServiceField.set(loginRestController, jwtService);

            java.lang.reflect.Field userInfoServiceField = LoginRestController.class.getDeclaredField("service");
            userInfoServiceField.setAccessible(true);
            userInfoServiceField.set(loginRestController, userInfoService);

            java.lang.reflect.Field passwordEncoderField = LoginRestController.class.getDeclaredField("passwordEncoder");
            passwordEncoderField.setAccessible(true);
            passwordEncoderField.set(loginRestController, passwordEncoder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set mock fields via reflection", e);
        }

        // Configurăm date de test
        validCredentials = new UserCredentials();
        validCredentials.setEmail("test@example.com");
        validCredentials.setPassword("password123");

        invalidCredentials = new UserCredentials();
        invalidCredentials.setEmail("invalid@example.com");
        invalidCredentials.setPassword("wrongpassword");

        // User activ
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("test@example.com");
        activeUser.setName("Test User");
        activeUser.setPassword("encoded_password");
        activeUser.setUserType("user");
        activeUser.setAccountStatus("active");
        activeUser.setTeamType("team1");

        // User în așteptare
        pendingUser = new User();
        pendingUser.setId(2L);
        pendingUser.setEmail("pending@example.com");
        pendingUser.setPassword("encoded_password");
        pendingUser.setAccountStatus("pending");

        // User respins
        rejectedUser = new User();
        rejectedUser.setId(3L);
        rejectedUser.setEmail("rejected@example.com");
        rejectedUser.setPassword("encoded_password");
        rejectedUser.setAccountStatus("rejected");
    }

    @Test
    void login_WithValidCredentialsAndActiveUser_ShouldReturnSuccessResponse() {
        // Arrange
        when(userSpringRepository.findByEmail(validCredentials.getEmail())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(validCredentials.getPassword(), activeUser.getPassword())).thenReturn(true);

        try (MockedStatic<UserCredentials> mockedStatic = mockStatic(UserCredentials.class)) {
            UserCredentials mockInstance = mock(UserCredentials.class);
            mockedStatic.when(UserCredentials::getInstance).thenReturn(mockInstance);

            // Act
            ResponseEntity<?> response = loginRestController.login(validCredentials);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            assertTrue((Boolean) responseBody.get("success"));
            assertEquals(activeUser.getId(), responseBody.get("id"));
            assertEquals(activeUser.getName(), responseBody.get("name"));
            assertEquals(activeUser.getEmail(), responseBody.get("email"));
            assertEquals(activeUser.getUserType(), responseBody.get("userType"));
            assertEquals(activeUser.getAccountStatus(), responseBody.get("accountStatus"));
            assertEquals(activeUser.getTeamType(), responseBody.get("teamType"));

            verify(mockInstance).setLoggedInUser(activeUser);
        }
    }

    @Test
    void login_WithInvalidEmail_ShouldReturnUnauthorized() {
        // Arrange
        when(userSpringRepository.findByEmail(invalidCredentials.getEmail())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = loginRestController.login(invalidCredentials);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Email sau parola incorectă", responseBody.get("message"));
    }

    @Test
    void login_WithInvalidPassword_ShouldReturnUnauthorized() {
        // Arrange
        when(userSpringRepository.findByEmail(validCredentials.getEmail())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(validCredentials.getPassword(), activeUser.getPassword())).thenReturn(false);

        // Act
        ResponseEntity<?> response = loginRestController.login(validCredentials);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Email sau parola incorectă", responseBody.get("message"));
    }

    @Test
    void login_WithPendingUser_ShouldReturnForbidden() {
        // Arrange
        UserCredentials pendingCredentials = new UserCredentials();
        pendingCredentials.setEmail("pending@example.com");
        pendingCredentials.setPassword("password123");

        when(userSpringRepository.findByEmail(pendingCredentials.getEmail())).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.matches(pendingCredentials.getPassword(), pendingUser.getPassword())).thenReturn(true);

        // Act
        ResponseEntity<?> response = loginRestController.login(pendingCredentials);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Contul dumneavoastră este în așteptarea aprobării administratorului.", responseBody.get("message"));
    }

    @Test
    void login_WithRejectedUser_ShouldReturnForbidden() {
        // Arrange
        UserCredentials rejectedCredentials = new UserCredentials();
        rejectedCredentials.setEmail("rejected@example.com");
        rejectedCredentials.setPassword("password123");

        when(userSpringRepository.findByEmail(rejectedCredentials.getEmail())).thenReturn(Optional.of(rejectedUser));
        when(passwordEncoder.matches(rejectedCredentials.getPassword(), rejectedUser.getPassword())).thenReturn(true);

        // Act
        ResponseEntity<?> response = loginRestController.login(rejectedCredentials);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertEquals("Contul dumneavoastră a fost respins. Vă rugăm să contactați administratorul.", responseBody.get("message"));
    }

    @Test
    void login_WithExceptionThrown_ShouldReturnInternalServerError() {
        // Arrange
        when(userSpringRepository.findByEmail(validCredentials.getEmail())).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = loginRestController.login(validCredentials);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertTrue(responseBody.get("message").toString().contains("Eroare în timpul autentificării"));
    }

    @Test
    void authenticateAndGenerateToken_WithValidCredentials_ShouldReturnToken() {
        // Arrange
        String expectedToken = "valid.jwt.token";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userSpringRepository.findByEmail(validCredentials.getEmail())).thenReturn(Optional.of(activeUser));
        when(jwtService.generateToken(validCredentials.getEmail())).thenReturn(expectedToken);

        // Act
        ResponseEntity<?> response = loginRestController.authenticateAndGenerateToken(validCredentials);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedToken, response.getBody());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(validCredentials.getEmail());
    }

    @Test
    void authenticateAndGenerateToken_WithInvalidCredentials_ShouldReturnUnauthorized() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act
        ResponseEntity<?> response = loginRestController.authenticateAndGenerateToken(invalidCredentials);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Email sau parola incorectă", response.getBody());
    }

    @Test
    void authenticateAndGenerateToken_WithPendingUser_ShouldReturnForbidden() {
        // Arrange
        UserCredentials pendingCredentials = new UserCredentials();
        pendingCredentials.setEmail("pending@example.com");
        pendingCredentials.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userSpringRepository.findByEmail(pendingCredentials.getEmail())).thenReturn(Optional.of(pendingUser));

        // Act
        ResponseEntity<?> response = loginRestController.authenticateAndGenerateToken(pendingCredentials);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Contul este în așteptarea aprobării", response.getBody());
    }

    @Test
    void authenticateAndGenerateToken_WithRejectedUser_ShouldReturnForbidden() {
        // Arrange
        UserCredentials rejectedCredentials = new UserCredentials();
        rejectedCredentials.setEmail("rejected@example.com");
        rejectedCredentials.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userSpringRepository.findByEmail(rejectedCredentials.getEmail())).thenReturn(Optional.of(rejectedUser));

        // Act
        ResponseEntity<?> response = loginRestController.authenticateAndGenerateToken(rejectedCredentials);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Contul a fost respins", response.getBody());
    }

    @Test
    void authenticateAndGenerateToken_WithAuthenticationFailure_ShouldReturnUnauthorized() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        ResponseEntity<?> response = loginRestController.authenticateAndGenerateToken(validCredentials);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Autentificare eșuată", response.getBody());
    }

    @Test
    void authenticateAndGenerateToken_WithException_ShouldReturnInternalServerError() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<?> response = loginRestController.authenticateAndGenerateToken(validCredentials);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Eroare în timpul generării token-ului"));
    }
}