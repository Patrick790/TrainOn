package licenta.jwtservice;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(TEST_EMAIL);
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        // Act
        String token = jwtService.generateToken(TEST_EMAIL);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // Arrange
        String token = jwtService.generateToken(TEST_EMAIL);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertEquals(TEST_EMAIL, username);
    }

    @Test
    void extractExpiration_ShouldReturnFutureDate() {
        // Arrange
        String token = jwtService.generateToken(TEST_EMAIL);

        // Act
        Date expirationDate = jwtService.extractExpiration(token);

        // Assert
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Arrange
        String token = jwtService.generateToken(TEST_EMAIL);

        // Act
        boolean isValid = jwtService.validateToken(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithDifferentUsername_ShouldReturnFalse() {
        // Arrange
        String token = jwtService.generateToken("different@example.com");

        // Act
        boolean isValid = jwtService.validateToken(token, userDetails);

        // Assert
        assertFalse(isValid);

    }

    @Test
    void extractClaim_ShouldExtractClaimCorrectly() {
        // Arrange
        String token = jwtService.generateToken(TEST_EMAIL);

        // Act
        String subject = jwtService.extractClaim(token, Claims::getSubject);

        // Assert
        assertEquals(TEST_EMAIL, subject);
    }

    @Test
    void tokenExpiration_ShouldBeSetTo24Hours() {
        // Arrange
        String token = jwtService.generateToken(TEST_EMAIL);
        Date expirationDate = jwtService.extractExpiration(token);
        Date now = new Date();

        // Calculate the difference in milliseconds
        long diffInMillis = expirationDate.getTime() - now.getTime();
        //         // Convert to hours (with a small margin of error for test execution time)
        long diffInHours = diffInMillis / (1000 * 60 * 60);

        // Assert that the expiration is close to 24 hours
        // We use a range like 23-24 hours to account for the time taken during test execution
        assertTrue(diffInHours >= 23 && diffInHours <= 24,
                "Token expiration should be set to approximately 24 hours, but was: " + diffInHours + " hours");
    }
}
