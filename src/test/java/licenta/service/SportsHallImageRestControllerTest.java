package licenta.service;

import licenta.model.SportsHall;
import licenta.model.SportsHallImage;
import licenta.persistence.ISportsHallImageSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SportsHallImageRestControllerTest {

    @Mock
    private ISportsHallImageSpringRepository sportsHallImageSpringRepository;

    private SportsHallImageRestController sportsHallImageRestController;

    private SportsHallImage testImage1;
    private SportsHallImage testImage2;
    private SportsHall testHall;
    private byte[] testImageData;

    @BeforeEach
    void setUp() {
        sportsHallImageRestController = new SportsHallImageRestController(sportsHallImageSpringRepository);

        // Configurăm sala de test
        testHall = new SportsHall();
        testHall.setId(1L);
        testHall.setName("Test Sports Hall");
        testHall.setCity("Cluj-Napoca");

        // Configurăm datele imaginii de test
        testImageData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        // Configurăm imaginile de test
        testImage1 = new SportsHallImage();
        testImage1.setId(1L);
        testImage1.setImagePath(testImageData);
        testImage1.setDescription("interior");
        testImage1.setSportsHall(testHall);

        testImage2 = new SportsHallImage();
        testImage2.setId(2L);
        testImage2.setImagePath(new byte[]{11, 12, 13, 14, 15});
        testImage2.setDescription("exterior");
        testImage2.setSportsHall(testHall);
    }

    @Test
    void getImageById_WithExistingId_ShouldReturnImageData() {
        // Arrange
        when(sportsHallImageSpringRepository.findById(1L)).thenReturn(Optional.of(testImage1));

        // Act
        ResponseEntity<byte[]> response = sportsHallImageRestController.getImageById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertArrayEquals(testImageData, response.getBody());
        verify(sportsHallImageSpringRepository).findById(1L);
    }

    @Test
    void getImageById_WithNonExistingId_ShouldReturnNotFound() {
        // Arrange
        when(sportsHallImageSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<byte[]> response = sportsHallImageRestController.getImageById(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(sportsHallImageSpringRepository).findById(999L);
    }

    @Test
    void getImageById_WithRepositoryException_ShouldReturnNotFound() {
        // Arrange
        when(sportsHallImageSpringRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<byte[]> response = sportsHallImageRestController.getImageById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(sportsHallImageSpringRepository).findById(1L);
    }

    @Test
    void getImageById_WithNullImageData_ShouldReturnImageWithNullBody() {
        // Arrange
        SportsHallImage imageWithNullData = new SportsHallImage();
        imageWithNullData.setId(3L);
        imageWithNullData.setImagePath(null);
        imageWithNullData.setDescription("test");
        imageWithNullData.setSportsHall(testHall);

        when(sportsHallImageSpringRepository.findById(3L)).thenReturn(Optional.of(imageWithNullData));

        // Act
        ResponseEntity<byte[]> response = sportsHallImageRestController.getImageById(3L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertNull(response.getBody());
        verify(sportsHallImageSpringRepository).findById(3L);
    }

    @Test
    void getImageById_WithEmptyImageData_ShouldReturnEmptyArray() {
        // Arrange
        SportsHallImage imageWithEmptyData = new SportsHallImage();
        imageWithEmptyData.setId(4L);
        imageWithEmptyData.setImagePath(new byte[0]);
        imageWithEmptyData.setDescription("empty");
        imageWithEmptyData.setSportsHall(testHall);

        when(sportsHallImageSpringRepository.findById(4L)).thenReturn(Optional.of(imageWithEmptyData));

        // Act
        ResponseEntity<byte[]> response = sportsHallImageRestController.getImageById(4L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().length);
        verify(sportsHallImageSpringRepository).findById(4L);
    }

    @Test
    void deleteImageById_WithExistingId_ShouldReturnSuccessMessage() {
        // Arrange
        when(sportsHallImageSpringRepository.findById(1L)).thenReturn(Optional.of(testImage1));
        doNothing().when(sportsHallImageSpringRepository).deleteById(1L);

        // Act
        ResponseEntity<?> response = sportsHallImageRestController.deleteImageById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Image deleted successfully", response.getBody());
        verify(sportsHallImageSpringRepository).findById(1L);
        verify(sportsHallImageSpringRepository).deleteById(1L);
    }

    @Test
    void deleteImageById_WithNonExistingId_ShouldReturnNotFound() {
        // Arrange
        when(sportsHallImageSpringRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = sportsHallImageRestController.deleteImageById(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(sportsHallImageSpringRepository).findById(999L);
        verify(sportsHallImageSpringRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteImageById_WithRepositoryFindException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallImageSpringRepository.findById(1L)).thenThrow(new RuntimeException("Database connection error"));

        // Act
        ResponseEntity<?> response = sportsHallImageRestController.deleteImageById(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error deleting image"));
        assertTrue(response.getBody().toString().contains("Database connection error"));
        verify(sportsHallImageSpringRepository).findById(1L);
        verify(sportsHallImageSpringRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteImageById_WithRepositoryDeleteException_ShouldReturnInternalServerError() {
        // Arrange
        when(sportsHallImageSpringRepository.findById(1L)).thenReturn(Optional.of(testImage1));
        doThrow(new RuntimeException("Delete operation failed")).when(sportsHallImageSpringRepository).deleteById(1L);

        // Act
        ResponseEntity<?> response = sportsHallImageRestController.deleteImageById(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error deleting image"));
        assertTrue(response.getBody().toString().contains("Delete operation failed"));
        verify(sportsHallImageSpringRepository).findById(1L);
        verify(sportsHallImageSpringRepository).deleteById(1L);
    }

    @Test
    void deleteImageById_WithImageHavingSportsHall_ShouldDeleteSuccessfully() {
        // Arrange
        SportsHallImage imageWithHall = new SportsHallImage();
        imageWithHall.setId(5L);
        imageWithHall.setImagePath(testImageData);
        imageWithHall.setDescription("test image");
        imageWithHall.setSportsHall(testHall);

        when(sportsHallImageSpringRepository.findById(5L)).thenReturn(Optional.of(imageWithHall));
        doNothing().when(sportsHallImageSpringRepository).deleteById(5L);

        // Act
        ResponseEntity<?> response = sportsHallImageRestController.deleteImageById(5L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Image deleted successfully", response.getBody());
        verify(sportsHallImageSpringRepository).findById(5L);
        verify(sportsHallImageSpringRepository).deleteById(5L);
    }

    @Test
    void deleteImageById_WithLargeImageId_ShouldHandleCorrectly() {
        // Arrange
        Long largeId = Long.MAX_VALUE;
        when(sportsHallImageSpringRepository.findById(largeId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = sportsHallImageRestController.deleteImageById(largeId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(sportsHallImageSpringRepository).findById(largeId);
        verify(sportsHallImageSpringRepository, never()).deleteById(anyLong());
    }

    @Test
    void getImageById_WithLargeImageData_ShouldReturnCorrectly() {
        // Arrange
        byte[] largeImageData = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeImageData.length; i++) {
            largeImageData[i] = (byte) (i % 256);
        }

        SportsHallImage largeImage = new SportsHallImage();
        largeImage.setId(7L);
        largeImage.setImagePath(largeImageData);
        largeImage.setDescription("large image");
        largeImage.setSportsHall(testHall);

        when(sportsHallImageSpringRepository.findById(7L)).thenReturn(Optional.of(largeImage));

        // Act
        ResponseEntity<byte[]> response = sportsHallImageRestController.getImageById(7L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(largeImageData.length, response.getBody().length);
        assertArrayEquals(largeImageData, response.getBody());
        verify(sportsHallImageSpringRepository).findById(7L);
    }

    @Test
    void deleteImageById_MultipleCallsWithSameId_ShouldHandleCorrectly() {
        // Arrange
        when(sportsHallImageSpringRepository.findById(1L))
                .thenReturn(Optional.of(testImage1))
                .thenReturn(Optional.empty());
        doNothing().when(sportsHallImageSpringRepository).deleteById(1L);

        // Act - First call
        ResponseEntity<?> firstResponse = sportsHallImageRestController.deleteImageById(1L);

        // Act - Second call
        ResponseEntity<?> secondResponse = sportsHallImageRestController.deleteImageById(1L);

        // Assert
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());
        assertEquals("Image deleted successfully", firstResponse.getBody());

        assertEquals(HttpStatus.NOT_FOUND, secondResponse.getStatusCode());

        verify(sportsHallImageSpringRepository, times(2)).findById(1L);
        verify(sportsHallImageSpringRepository, times(1)).deleteById(1L);
    }
}