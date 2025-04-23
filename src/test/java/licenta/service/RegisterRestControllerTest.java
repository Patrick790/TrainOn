package licenta.service;

import licenta.jwtservice.UserInfoService;
import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegisterRestControllerTest {

    @Mock
    private IUserSpringRepository userSpringRepository;
    @Mock
    private UserInfoService userInfoService;
    private RegisterRestController registerRestController;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Initializare manuala a controller-ului pentru a injecta mock-urile corect
        registerRestController = new RegisterRestController(userSpringRepository);

        // Setare manuala a dependintelor pt UserInfoService folosind reflexia
        try {
            java.lang.reflect.Field serviceField = RegisterRestController.class.getDeclaredField("service");
            serviceField.setAccessible(true);
            serviceField.set(registerRestController, userInfoService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set mock fields via reflection", e);
        }

        // Configurarea datelor de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setUserType("user");

    }

    @Test
    void addNewUser_ShouldCallServiceAndReturnResult() {
        // Arrange
        String expectedResponse = "User added successfully";
        when(userInfoService.addUser(testUser)).thenReturn(expectedResponse);

        // Act
        String response = String.valueOf(registerRestController.addNewUser(testUser));

        // Assert
        assertEquals(expectedResponse, response);
        verify(userInfoService, times(1)).addUser(testUser);
    }

    @Test
    void addNewUser_WhenServiceThrowsException_ShouldPropagateException() {
        // Arrange
        when(userInfoService.addUser(testUser)).thenThrow(new RuntimeException("Email already exists"));

        // Act & Assert
        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            registerRestController.addNewUser(testUser);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userInfoService, times(1)).addUser(testUser);
    }

}
