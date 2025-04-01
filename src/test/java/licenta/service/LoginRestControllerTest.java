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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private Authentication authentication;

    private LoginRestController loginRestController;

    private UserCredentials validCredentials;
    private UserCredentials invalidCredentials;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Inițializăm manual controller-ul pentru a injecta mock-urile corect
        loginRestController = new LoginRestController(userSpringRepository);

        // Setăm manual dependențele pentru a rezolva problema cu @Autowired
        // Folosim reflexia pentru a seta câmpurile
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

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setUserType("user");
    }

    @Test
    void login_WithValidCredentials_ShouldReturnOkWithUser() {
        // Arrange
        when(userSpringRepository.findByEmail(validCredentials.getEmail())).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<?> response = loginRestController.login(validCredentials);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser, response.getBody());
    }

    @Test
    void login_WithInvalidEmail_ShouldReturnNotFound() {
        // Arrange
        when(userSpringRepository.findByEmail(invalidCredentials.getEmail())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = loginRestController.login(invalidCredentials);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody());
    }

    @Test
    void login_WithExceptionThrown_ShouldReturnInternalServerError() {
        // Arrange
        when(userSpringRepository.findByEmail(validCredentials.getEmail())).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = loginRestController.login(validCredentials);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error during login"));
    }

    @Test
    void authenticateAndGenerateToken_WithValidCredentials_ShouldReturnToken() {
        // Arrange
        String expectedToken = "valid.jwt.token";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(jwtService.generateToken(validCredentials.getEmail())).thenReturn(expectedToken);

        // Act
        String token = loginRestController.authenticateAndGenerateToken(validCredentials);

        // Assert
        assertEquals(expectedToken, token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(validCredentials.getEmail());
    }

    @Test
    void authenticateAndGenerateToken_WithInvalidCredentials_ShouldThrowBadCredentialsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            loginRestController.authenticateAndGenerateToken(invalidCredentials);
        });
    }

    @Test
    void authenticateAndGenerateToken_WithAuthenticationFailure_ShouldThrowBadCredentialsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            loginRestController.authenticateAndGenerateToken(validCredentials);
        });
    }
}