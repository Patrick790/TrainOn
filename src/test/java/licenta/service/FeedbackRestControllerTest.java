package licenta.service;

import licenta.model.Feedback;
import licenta.model.SportsHall;
import licenta.model.User;
import licenta.persistence.IFeedbackSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeedbackRestControllerTest {

    @Mock
    private IFeedbackSpringRepository feedbackSpringRepository;

    private FeedbackRestController feedbackRestController;

    private Feedback testFeedback1;
    private Feedback testFeedback2;
    private Feedback testFeedback3;
    private List<Feedback> testFeedbackList;
    private SportsHall testHall1;
    private SportsHall testHall2;
    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        feedbackRestController = new FeedbackRestController(feedbackSpringRepository);

        // Configurăm obiectele auxiliare
        testHall1 = new SportsHall();
        testHall1.setId(100L);
        testHall1.setName("Sala Test 1");

        testHall2 = new SportsHall();
        testHall2.setId(200L);
        testHall2.setName("Sala Test 2");

        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setName("User 1");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setName("User 2");

        testUser3 = new User();
        testUser3.setId(3L);
        testUser3.setName("User 3");

        // Configurăm date de test
        testFeedback1 = new Feedback();
        testFeedback1.setId(1L);
        testFeedback1.setHall(testHall1);
        testFeedback1.setUser(testUser1);
        testFeedback1.setRating(5);
        testFeedback1.setComment("Excelent!");
        testFeedback1.setDate(new Date(System.currentTimeMillis() - 86400000)); // acum - 1 zi

        testFeedback2 = new Feedback();
        testFeedback2.setId(2L);
        testFeedback2.setHall(testHall1);
        testFeedback2.setUser(testUser2);
        testFeedback2.setRating(4);
        testFeedback2.setComment("Foarte bun");
        testFeedback2.setDate(new Date(System.currentTimeMillis() - 172800000)); // acum - 2 zile

        testFeedback3 = new Feedback();
        testFeedback3.setId(3L);
        testFeedback3.setHall(testHall2);
        testFeedback3.setUser(testUser3);
        testFeedback3.setRating(3);
        testFeedback3.setComment("Acceptabil");
        testFeedback3.setDate(new Date(System.currentTimeMillis() - 259200000)); // acum - 3 zile

        testFeedbackList = Arrays.asList(testFeedback1, testFeedback2, testFeedback3);
    }

    @Test
    void getFeedbackById_WithExistingId_ShouldReturnFeedback() {
        // Arrange
        when(feedbackSpringRepository.findById(1L)).thenReturn(Optional.of(testFeedback1));

        // Act
        ResponseEntity<Feedback> response = feedbackRestController.getFeedbackById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testFeedback1, response.getBody());
        verify(feedbackSpringRepository).findById(1L);
    }

    @Test
    void getFeedbackById_WithNonExistingId_ShouldReturnNotFound() {
        // Arrange
        when(feedbackSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Feedback> response = feedbackRestController.getFeedbackById(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(feedbackSpringRepository).findById(999L);
    }

    @Test
    void getAllFeedbacks_WithoutParameters_ShouldReturnAllFeedbacks() {
        // Arrange
        when(feedbackSpringRepository.findAll()).thenReturn(testFeedbackList);

        // Act
        ResponseEntity<List<Feedback>> response = feedbackRestController.getAllFeedbacks(null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testFeedbackList, response.getBody());
        verify(feedbackSpringRepository).findAll();
    }

    @Test
    void getAllFeedbacks_WithHallIdOnly_ShouldReturnFilteredFeedbacks() {
        // Arrange
        List<Feedback> filteredFeedbacks = Arrays.asList(testFeedback1, testFeedback2);
        when(feedbackSpringRepository.findByHallId(100L)).thenReturn(filteredFeedbacks);

        // Act
        ResponseEntity<List<Feedback>> response = feedbackRestController.getAllFeedbacks(100L, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(filteredFeedbacks, response.getBody());
        verify(feedbackSpringRepository).findByHallId(100L);
    }

    @Test
    void getAllFeedbacks_WithHallIdAndDateSort_ShouldReturnSortedFeedbacks() {
        // Arrange
        List<Feedback> sortedFeedbacks = Arrays.asList(testFeedback1, testFeedback2);
        when(feedbackSpringRepository.findByHallIdOrderByDateDesc(100L)).thenReturn(sortedFeedbacks);

        // Act
        ResponseEntity<List<Feedback>> response = feedbackRestController.getAllFeedbacks(100L, "date");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sortedFeedbacks, response.getBody());
        verify(feedbackSpringRepository).findByHallIdOrderByDateDesc(100L);
    }

    @Test
    void getAllFeedbacks_WithHallIdAndRatingDescSort_ShouldReturnSortedFeedbacks() {
        // Arrange
        List<Feedback> sortedFeedbacks = Arrays.asList(testFeedback1, testFeedback2);
        when(feedbackSpringRepository.findByHallIdOrderByRatingDesc(100L)).thenReturn(sortedFeedbacks);

        // Act
        ResponseEntity<List<Feedback>> response = feedbackRestController.getAllFeedbacks(100L, "rating_desc");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sortedFeedbacks, response.getBody());
        verify(feedbackSpringRepository).findByHallIdOrderByRatingDesc(100L);
    }

    @Test
    void getAllFeedbacks_WithHallIdAndRatingAscSort_ShouldReturnSortedFeedbacks() {
        // Arrange
        List<Feedback> sortedFeedbacks = Arrays.asList(testFeedback2, testFeedback1);
        when(feedbackSpringRepository.findByHallIdOrderByRatingAsc(100L)).thenReturn(sortedFeedbacks);

        // Act
        ResponseEntity<List<Feedback>> response = feedbackRestController.getAllFeedbacks(100L, "rating_asc");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sortedFeedbacks, response.getBody());
        verify(feedbackSpringRepository).findByHallIdOrderByRatingAsc(100L);
    }

    @Test
    void getAllFeedbacks_WithHallIdAndInvalidSort_ShouldReturnUnsortedFeedbacks() {
        // Arrange
        List<Feedback> filteredFeedbacks = Arrays.asList(testFeedback1, testFeedback2);
        when(feedbackSpringRepository.findByHallId(100L)).thenReturn(filteredFeedbacks);

        // Act
        ResponseEntity<List<Feedback>> response = feedbackRestController.getAllFeedbacks(100L, "invalid_sort");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(filteredFeedbacks, response.getBody());
        verify(feedbackSpringRepository).findByHallId(100L);
    }

    @Test
    void createFeedback_WithValidFeedback_ShouldReturnCreatedFeedback() {
        // Arrange
        Feedback newFeedback = new Feedback();
        newFeedback.setHall(testHall1);
        newFeedback.setUser(testUser1);
        newFeedback.setRating(5);
        newFeedback.setComment("Nou feedback");
        newFeedback.setDate(new Date());

        Feedback savedFeedback = new Feedback();
        savedFeedback.setId(4L);
        savedFeedback.setHall(testHall1);
        savedFeedback.setUser(testUser1);
        savedFeedback.setRating(5);
        savedFeedback.setComment("Nou feedback");
        savedFeedback.setDate(newFeedback.getDate());

        when(feedbackSpringRepository.save(newFeedback)).thenReturn(savedFeedback);

        // Act
        ResponseEntity<Feedback> response = feedbackRestController.createFeedback(newFeedback);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(savedFeedback, response.getBody());
        verify(feedbackSpringRepository).save(newFeedback);
    }

    @Test
    void createFeedback_WithException_ShouldReturnInternalServerError() {
        // Arrange
        Feedback newFeedback = new Feedback();
        when(feedbackSpringRepository.save(any(Feedback.class))).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<Feedback> response = feedbackRestController.createFeedback(newFeedback);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(feedbackSpringRepository).save(newFeedback);
    }

    @Test
    void updateFeedback_WithExistingId_ShouldReturnUpdatedFeedback() {
        // Arrange
        Feedback updatedFeedback = new Feedback();
        updatedFeedback.setHall(testHall1);
        updatedFeedback.setUser(testUser1);
        updatedFeedback.setRating(4);
        updatedFeedback.setComment("Feedback actualizat");
        updatedFeedback.setDate(new Date());

        Feedback savedFeedback = new Feedback();
        savedFeedback.setId(1L);
        savedFeedback.setHall(testHall1);
        savedFeedback.setUser(testUser1);
        savedFeedback.setRating(4);
        savedFeedback.setComment("Feedback actualizat");
        savedFeedback.setDate(updatedFeedback.getDate());

        when(feedbackSpringRepository.existsById(1L)).thenReturn(true);
        when(feedbackSpringRepository.save(any(Feedback.class))).thenReturn(savedFeedback);

        // Act
        ResponseEntity<Feedback> response = feedbackRestController.updateFeedback(1L, updatedFeedback);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(savedFeedback, response.getBody());
        assertEquals(1L, updatedFeedback.getId()); // Verificăm că ID-ul a fost setat
        verify(feedbackSpringRepository).existsById(1L);
        verify(feedbackSpringRepository).save(updatedFeedback);
    }

    @Test
    void updateFeedback_WithNonExistingId_ShouldReturnNotFound() {
        // Arrange
        Feedback updatedFeedback = new Feedback();
        when(feedbackSpringRepository.existsById(999L)).thenReturn(false);

        // Act
        ResponseEntity<Feedback> response = feedbackRestController.updateFeedback(999L, updatedFeedback);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(feedbackSpringRepository).existsById(999L);
        verify(feedbackSpringRepository, never()).save(any(Feedback.class));
    }

    @Test
    void deleteFeedback_WithExistingId_ShouldReturnNoContent() {
        // Arrange
        when(feedbackSpringRepository.existsById(1L)).thenReturn(true);
        doNothing().when(feedbackSpringRepository).deleteById(1L);

        // Act
        ResponseEntity<Void> response = feedbackRestController.deleteFeedback(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(feedbackSpringRepository).existsById(1L);
        verify(feedbackSpringRepository).deleteById(1L);
    }

    @Test
    void deleteFeedback_WithNonExistingId_ShouldReturnNotFound() {
        // Arrange
        when(feedbackSpringRepository.existsById(999L)).thenReturn(false);

        // Act
        ResponseEntity<Void> response = feedbackRestController.deleteFeedback(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(feedbackSpringRepository).existsById(999L);
        verify(feedbackSpringRepository, never()).deleteById(anyLong());
    }
}