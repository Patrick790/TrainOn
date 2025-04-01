package licenta.integration;

import licenta.jwtservice.JwtService;
import licenta.model.User;
import licenta.model.dtos.UserCredentials;
import licenta.persistence.IUserSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IUserSpringRepository userRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    private User testUser;
    private UserCredentials validCredentials;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setUserType("user");

        validCredentials = new UserCredentials();
        validCredentials.setEmail("test@example.com");
        validCredentials.setPassword("password123");

        // Configurare mock-uri
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtService.generateToken(anyString())).thenReturn("test.jwt.token");
    }

    @Test
    void login_WithValidCredentials_ShouldReturnUserAndOkStatus() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCredentials)))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        User responseUser = objectMapper.readValue(responseContent, User.class);

        assertEquals(testUser.getId(), responseUser.getId());
        assertEquals(testUser.getEmail(), responseUser.getEmail());
        assertEquals(testUser.getUserType(), responseUser.getUserType());
    }

    @Test
    void generateToken_WithValidCredentials_ShouldReturnToken() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(post("/login/generateToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCredentials)))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String responseToken = result.getResponse().getContentAsString();

        assertNotNull(responseToken);
        assertEquals("test.jwt.token", responseToken);
    }

    @Test
    void login_WithInvalidEmail_ShouldReturnNotFound() throws Exception {
        // Arrange
        UserCredentials invalidCredentials = new UserCredentials();
        invalidCredentials.setEmail("nonexistent@example.com");
        invalidCredentials.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCredentials)))
                .andExpect(status().isNotFound());
    }
}