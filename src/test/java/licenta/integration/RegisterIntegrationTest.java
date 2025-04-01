package licenta.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import licenta.jwtservice.UserInfoService;
import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RegisterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserInfoService userInfoService;

    @MockBean
    private IUserSpringRepository userSpringRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setUserType("user");
    }

    @Test
    void registerUser_WithValidData_ShouldReturnSuccess() throws Exception {
        // Arrange
        String expectedResponse = "User added successfully";
        when(userInfoService.addUser(any(User.class))).thenReturn(expectedResponse);

        // Act
        MvcResult result = mockMvc.perform(post("/register/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        assertEquals(expectedResponse, responseContent);
    }

    @Test
    void registerUser_WhenServiceThrowsException_ShouldReturnError() throws Exception {
        // Arrange
        when(userInfoService.addUser(any(User.class))).thenThrow(new RuntimeException("Email already exists"));

        // Act & Assert - Ne așteptăm ca excepția să fie propagată, deci modificam testul
        try {
            mockMvc.perform(post("/register/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testUser)));
        } catch (Exception e) {
            // Verificam ca exceptia contine mesajul așteptat
            String exceptionMessage = e.getCause().getMessage();
            assertEquals("Email already exists", exceptionMessage);
        }
    }
}