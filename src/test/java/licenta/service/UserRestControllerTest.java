package licenta.service;

import licenta.model.User;
import licenta.persistence.IUserSpringRepository;
import licenta.service.UserRestController.ChangePasswordRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserRestControllerTest {

    @Mock
    private IUserSpringRepository userSpringRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    private UserRestController userRestController;

    private User regularUser;
    private User adminUser;
    private User hallAdminUser;
    private User suspendedUser;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        userRestController = new UserRestController(userSpringRepository, passwordEncoder);

        // Configurăm utilizatorii de test
        regularUser = new User();
        regularUser.setId(1L);
        regularUser.setName("Regular User");
        regularUser.setEmail("user@example.com");
        regularUser.setPassword("encoded_password");
        regularUser.setUserType("user");
        regularUser.setAccountStatus("active");
        regularUser.setCreatedAt(new Date());
        regularUser.setFcfsEnabled(true);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("encoded_admin_password");
        adminUser.setUserType("admin");
        adminUser.setAccountStatus("active");
        adminUser.setCreatedAt(new Date());
        adminUser.setFcfsEnabled(true);

        hallAdminUser = new User();
        hallAdminUser.setId(3L);
        hallAdminUser.setName("Hall Admin");
        hallAdminUser.setEmail("halladmin@example.com");
        hallAdminUser.setPassword("encoded_hall_admin_password");
        hallAdminUser.setUserType("hall_admin");
        hallAdminUser.setAccountStatus("verified");
        hallAdminUser.setCreatedAt(new Date());
        hallAdminUser.setFcfsEnabled(true);

        suspendedUser = new User();
        suspendedUser.setId(4L);
        suspendedUser.setName("Suspended User");
        suspendedUser.setEmail("suspended@example.com");
        suspendedUser.setPassword("encoded_suspended_password");
        suspendedUser.setUserType("user");
        suspendedUser.setAccountStatus("suspended");
        suspendedUser.setCreatedAt(new Date());
        suspendedUser.setFcfsEnabled(true);

        testUsers = Arrays.asList(regularUser, adminUser, hallAdminUser, suspendedUser);
    }

    @Test
    void getUserById_WithExistingId_ShouldReturnUser() {
        // Arrange
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));

        // Act
        User result = userRestController.getUserById(1L);

        // Assert
        assertEquals(regularUser, result);
        verify(userSpringRepository).findById(1L);
    }

    @Test
    void getUserById_WithNonExistingId_ShouldReturnNull() {
        // Arrange
        when(userSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        User result = userRestController.getUserById(999L);

        // Assert
        assertNull(result);
        verify(userSpringRepository).findById(999L);
    }

    @Test
    void getAllUsers_WithoutAccountStatusFilter_ShouldReturnAllUsers() {
        // Arrange
        when(userSpringRepository.findAll()).thenReturn(testUsers);

        // Act
        Iterable<User> result = userRestController.getAllUsers(null);

        // Assert
        assertEquals(testUsers, result);
        verify(userSpringRepository).findAll();
    }

    @Test
    void getAllUsers_WithAccountStatusFilter_ShouldReturnFilteredUsers() {
        // Arrange
        when(userSpringRepository.findAll()).thenReturn(testUsers);

        // Act
        Iterable<User> result = userRestController.getAllUsers("active");

        // Assert
        List<User> resultList = new ArrayList<>();
        result.forEach(resultList::add);

        assertEquals(2, resultList.size());
        assertTrue(resultList.stream().allMatch(user -> "active".equals(user.getAccountStatus())));
        verify(userSpringRepository).findAll();
    }

    @Test
    void getAllUsers_WithEmptyAccountStatusFilter_ShouldReturnAllUsers() {
        // Arrange
        when(userSpringRepository.findAll()).thenReturn(testUsers);

        // Act
        Iterable<User> result = userRestController.getAllUsers("");

        // Assert
        assertEquals(testUsers, result);
        verify(userSpringRepository).findAll();
    }

    @Test
    void createUser_WithValidData_ShouldReturnCreatedUser() {
        // Arrange
        User newUser = new User();
        newUser.setName("New User");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("plaintext_password");
        newUser.setUserType("user");

        User savedUser = new User();
        savedUser.setId(5L);
        savedUser.setName("New User");
        savedUser.setEmail("newuser@example.com");
        savedUser.setPassword("encoded_password");
        savedUser.setUserType("user");
        savedUser.setAccountStatus("verified");
        savedUser.setCreatedAt(new Date());

        when(passwordEncoder.encode("plaintext_password")).thenReturn("encoded_password");
        when(userSpringRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userRestController.createUser(newUser);

        // Assert
        assertEquals(savedUser, result);
        verify(passwordEncoder).encode("plaintext_password");
        verify(userSpringRepository).save(any(User.class));
    }

    @Test
    void createUser_WithHallAdminType_ShouldSetVerifiedStatus() {
        // Arrange
        User newHallAdmin = new User();
        newHallAdmin.setName("New Hall Admin");
        newHallAdmin.setEmail("newhall@example.com");
        newHallAdmin.setPassword("plaintext_password");
        newHallAdmin.setUserType("hall_admin");

        when(passwordEncoder.encode("plaintext_password")).thenReturn("encoded_password");
        when(userSpringRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(6L);
            return user;
        });

        // Act
        User result = userRestController.createUser(newHallAdmin);

        // Assert
        assertEquals("verified", result.getAccountStatus());
        assertNotNull(result.getCreatedAt());
        verify(passwordEncoder).encode("plaintext_password");
    }

    @Test
    void deleteUser_WithValidId_ShouldDeleteUser() {
        // Arrange
        doNothing().when(userSpringRepository).deleteById(1L);

        // Act
        userRestController.deleteUser(1L);

        // Assert
        verify(userSpringRepository).deleteById(1L);
    }

    @Test
    void updateUser_WithExistingUser_ShouldReturnUpdatedUser() {
        // Arrange
        User updatedData = new User();
        updatedData.setName("Updated Name");
        updatedData.setEmail("updated@example.com");
        updatedData.setAddress("New Address");
        updatedData.setPassword("new_plaintext_password");

        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.encode("new_plaintext_password")).thenReturn("new_encoded_password");
        when(userSpringRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userRestController.updateUser(1L, updatedData);

        // Assert
        assertEquals("Updated Name", result.getName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("New Address", result.getAddress());
        assertEquals("new_encoded_password", result.getPassword());
        verify(passwordEncoder).encode("new_plaintext_password");
        verify(userSpringRepository).save(regularUser);
    }

    @Test
    void updateUser_WithNonExistingUser_ShouldReturnNull() {
        // Arrange
        User updatedData = new User();
        updatedData.setName("Updated Name");

        when(userSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        User result = userRestController.updateUser(999L, updatedData);

        // Assert
        assertNull(result);
        verify(userSpringRepository).findById(999L);
        verify(userSpringRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WithEmptyPassword_ShouldNotUpdatePassword() {
        // Arrange
        String originalPassword = regularUser.getPassword();
        User updatedData = new User();
        updatedData.setName("Updated Name");
        updatedData.setPassword(""); // Empty password

        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(userSpringRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userRestController.updateUser(1L, updatedData);

        // Assert
        assertEquals("Updated Name", result.getName());
        assertEquals(originalPassword, result.getPassword()); // Password should remain unchanged
        verify(passwordEncoder, never()).encode(anyString());
        verify(userSpringRepository).save(regularUser);
    }

    @Test
    void suspendUser_WithExistingUser_ShouldReturnSuccessMessage() {
        // Arrange
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(userSpringRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = userRestController.suspendUser(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Utilizatorul a fost suspendat cu succes", responseBody.get("message"));
        assertEquals("suspended", regularUser.getAccountStatus());
        verify(userSpringRepository).save(regularUser);
    }

    @Test
    void suspendUser_WithNonExistingUser_ShouldReturnNotFound() {
        // Arrange
        when(userSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = userRestController.suspendUser(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userSpringRepository).findById(999L);
        verify(userSpringRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_AsOwner_WithValidCurrentPassword_ShouldReturnSuccess() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("user"));

        when(authentication.getName()).thenReturn(regularUser.getEmail());
        doReturn(authorities).when(authentication).getAuthorities();
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.matches("current_password", regularUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("new_password", regularUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");
        when(userSpringRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = userRestController.changePassword(1L, request, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Parola a fost schimbată cu succes", responseBody.get("message"));
        assertEquals("encoded_new_password", regularUser.getPassword());
        verify(userSpringRepository).save(regularUser);
    }

    @Test
    void changePassword_AsOwner_WithInvalidCurrentPassword_ShouldReturnBadRequest() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("wrong_password", "new_password");

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("user"));

        when(authentication.getName()).thenReturn(regularUser.getEmail());
        doReturn(authorities).when(authentication).getAuthorities();
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.matches("wrong_password", regularUser.getPassword())).thenReturn(false);

        // Act
        ResponseEntity<?> response = userRestController.changePassword(1L, request, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Parola curentă este incorectă", responseBody.get("message"));
        verify(userSpringRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_AsAdmin_ShouldNotRequireCurrentPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("", "new_password");

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("admin"));

        when(authentication.getName()).thenReturn(adminUser.getEmail());
        doReturn(authorities).when(authentication).getAuthorities();
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.matches("new_password", regularUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");
        when(userSpringRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseEntity<?> response = userRestController.changePassword(1L, request, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Parola a fost schimbată cu succes", responseBody.get("message"));
        verify(passwordEncoder, never()).matches(anyString(), eq(regularUser.getPassword()));
    }

    @Test
    void changePassword_AsNonOwnerNonAdmin_ShouldReturnForbidden() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("user"));

        when(authentication.getName()).thenReturn("other@example.com");
        doReturn(authorities).when(authentication).getAuthorities();
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));

        // Act
        ResponseEntity<?> response = userRestController.changePassword(1L, request, authentication);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Nu aveți permisiunea să schimbați această parolă", responseBody.get("message"));
        verify(userSpringRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_WithNonExistingUser_ShouldReturnNotFound() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        when(authentication.getName()).thenReturn(regularUser.getEmail());
        when(userSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = userRestController.changePassword(999L, request, authentication);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userSpringRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_WithEmptyNewPassword_ShouldReturnBadRequest() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "");

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("user"));

        when(authentication.getName()).thenReturn(regularUser.getEmail());
        doReturn(authorities).when(authentication).getAuthorities();
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));

        // Act
        ResponseEntity<?> response = userRestController.changePassword(1L, request, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Parola curentă este incorectă", responseBody.get("message"));
    }

    @Test
    void changePassword_WithShortNewPassword_ShouldReturnBadRequest() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "123");

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("user"));

        when(authentication.getName()).thenReturn(regularUser.getEmail());
        doReturn(authorities).when(authentication).getAuthorities();
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.matches("current_password", regularUser.getPassword())).thenReturn(true);

        // Act
        ResponseEntity<?> response = userRestController.changePassword(1L, request, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Parola nouă trebuie să aibă cel puțin 6 caractere", responseBody.get("message"));
    }

    @Test
    void changePassword_WithSamePassword_ShouldReturnBadRequest() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "same_password");

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("user"));

        when(authentication.getName()).thenReturn(regularUser.getEmail());
        doReturn(authorities).when(authentication).getAuthorities();
        when(userSpringRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(passwordEncoder.matches("current_password", regularUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("same_password", regularUser.getPassword())).thenReturn(true);

        // Act
        ResponseEntity<?> response = userRestController.changePassword(1L, request, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Parola nouă trebuie să fie diferită de cea curentă", responseBody.get("message"));
    }

    @Test
    void changePassword_WithException_ShouldReturnInternalServerError() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("current_password", "new_password");

        // We only need stubs for method calls made *before* the exception is thrown.
        // The stub for getAuthorities() was removed because it's never called in this path.
        when(authentication.getName()).thenReturn(regularUser.getEmail());
        when(userSpringRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = userRestController.changePassword(1L, request, authentication);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(responseBody.get("message").toString().contains("A apărut o eroare la schimbarea parolei"));
    }
}