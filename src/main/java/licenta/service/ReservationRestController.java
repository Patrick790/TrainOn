package licenta.service;

import licenta.model.Reservation;
import licenta.persistence.IReservationSpringRepository;
import licenta.persistence.ISportsHallSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/reservations")
public class ReservationRestController {

    private final IReservationSpringRepository reservationSpringRepository;
    private final ISportsHallSpringRepository sportsHallSpringRepository;

    @Autowired
    public ReservationRestController(
            IReservationSpringRepository reservationSpringRepository,
            ISportsHallSpringRepository sportsHallSpringRepository) {
        this.reservationSpringRepository = reservationSpringRepository;
        this.sportsHallSpringRepository = sportsHallSpringRepository;
    }

    @GetMapping("/{id}")
    public Reservation getReservationById(@PathVariable Long id) {
        return reservationSpringRepository.findById(id).orElse(null);
    }

    // ENDPOINT pentru a obtine rezervarile unui utilizator
    @GetMapping("/user/{userId}")
    public List<Reservation> getReservationsByUser(@PathVariable Long userId) {
        return StreamSupport.stream(reservationSpringRepository.findAll().spliterator(), false)
                .filter(reservation -> reservation.getUser() != null &&
                        reservation.getUser().getId().equals(userId))
                .filter(reservation -> "reservation".equals(reservation.getType())) // Doar rezervările, nu mentenanța
                .collect(Collectors.toList());
    }

    @GetMapping
    public Iterable<Reservation> getReservations(
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date date,
            @RequestParam(required = false) String type) {

        if (hallId == null && date == null && type == null) {
            return reservationSpringRepository.findAll();
        }

        Iterable<Reservation> allReservations = reservationSpringRepository.findAll();

        return StreamSupport.stream(allReservations.spliterator(), false)
                .filter(reservation -> hallId == null || (reservation.getHall() != null && reservation.getHall().getId().equals(hallId)))
                .filter(reservation -> date == null || dateEquals(reservation.getDate(), date))
                .filter(reservation -> type == null || type.equals(reservation.getType()))
                .collect(Collectors.toList());
    }

    // ENDPOINT pentru anularea unei rezervari
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable Long id) {
        try {
            Reservation reservation = reservationSpringRepository.findById(id).orElse(null);
            if (reservation == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            reservation.setStatus("CANCELLED");
            Reservation updatedReservation = reservationSpringRepository.save(reservation);

            return new ResponseEntity<>(updatedReservation, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean dateEquals(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }

        LocalDate localDate1 = new java.sql.Date(date1.getTime()).toLocalDate();
        LocalDate localDate2 = new java.sql.Date(date2.getTime()).toLocalDate();

        return localDate1.equals(localDate2);
    }

    @PostMapping
    public ResponseEntity<Reservation> createReservation(@RequestBody Reservation reservation) {
        try {
            if (reservation.getHall() == null || reservation.getHall().getId() == null ||
                    reservation.getDate() == null || reservation.getTimeSlot() == null ||
                    reservation.getType() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            boolean slotExists = StreamSupport.stream(reservationSpringRepository.findAll().spliterator(), false)
                    .anyMatch(r -> r.getHall() != null &&
                            r.getHall().getId().equals(reservation.getHall().getId()) &&
                            dateEquals(r.getDate(), reservation.getDate()) &&
                            r.getTimeSlot().equals(reservation.getTimeSlot()) &&
                            !"CANCELLED".equals(r.getStatus())); // Nu luăm în considerare rezervările anulate

            if (slotExists) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }

            Reservation savedReservation = reservationSpringRepository.save(reservation);
            return new ResponseEntity<>(savedReservation, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        try {
            if (!reservationSpringRepository.existsById(id)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            reservationSpringRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint specializat pentru obținerea rezervărilor de mentenanță pentru o săptămână
     */
    @GetMapping("/maintenance/week")
    public List<Reservation> getMaintenanceForWeek(
            @RequestParam Long hallId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date weekStart) {

        // Calculam sfarsitul saptamanii
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(weekStart);
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6);
        Date weekEnd = cal.getTime();

        return StreamSupport.stream(reservationSpringRepository.findAll().spliterator(), false)
                .filter(reservation -> reservation.getHall() != null &&
                        reservation.getHall().getId().equals(hallId))
                .filter(reservation -> "maintenance".equals(reservation.getType()))
                .filter(reservation -> {
                    if (reservation.getDate() == null) {
                        return false;
                    }

                    return !reservation.getDate().before(weekStart) &&
                            !reservation.getDate().after(weekEnd);
                })
                .collect(Collectors.toList());
    }
}