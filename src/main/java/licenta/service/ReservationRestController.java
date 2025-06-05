package licenta.service;

import licenta.model.Reservation;
import licenta.persistence.IReservationSpringRepository;
import licenta.persistence.ISportsHallSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // ENDPOINT NOU - pentru a obține rezervările unui utilizator
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

        // Dacă nu avem parametri, returnăm toate rezervările
        if (hallId == null && date == null && type == null) {
            return reservationSpringRepository.findAll();
        }

        // Filtrăm rezervările în funcție de parametrii furnizați
        Iterable<Reservation> allReservations = reservationSpringRepository.findAll();

        return StreamSupport.stream(allReservations.spliterator(), false)
                .filter(reservation -> hallId == null || (reservation.getHall() != null && reservation.getHall().getId().equals(hallId)))
                .filter(reservation -> date == null || dateEquals(reservation.getDate(), date))
                .filter(reservation -> type == null || type.equals(reservation.getType()))
                .collect(Collectors.toList());
    }

    // ENDPOINT NOU - pentru anularea unei rezervări
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable Long id) {
        try {
            Reservation reservation = reservationSpringRepository.findById(id).orElse(null);
            if (reservation == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Setăm statusul la CANCELLED în loc să ștergem rezervarea
            reservation.setStatus("CANCELLED");
            Reservation updatedReservation = reservationSpringRepository.save(reservation);

            return new ResponseEntity<>(updatedReservation, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Metodă utilitară pentru a compara doar data (fără oră)
    private boolean dateEquals(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }

        // Comparăm doar anul, luna și ziua
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        int year1 = cal1.get(java.util.Calendar.YEAR);
        int month1 = cal1.get(java.util.Calendar.MONTH);
        int day1 = cal1.get(java.util.Calendar.DAY_OF_MONTH);

        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTime(date2);
        int year2 = cal2.get(java.util.Calendar.YEAR);
        int month2 = cal2.get(java.util.Calendar.MONTH);
        int day2 = cal2.get(java.util.Calendar.DAY_OF_MONTH);

        return year1 == year2 && month1 == month2 && day1 == day2;
    }

    @PostMapping
    public ResponseEntity<Reservation> createReservation(@RequestBody Reservation reservation) {
        try {
            // Verificăm dacă este completă
            if (reservation.getHall() == null || reservation.getHall().getId() == null ||
                    reservation.getDate() == null || reservation.getTimeSlot() == null ||
                    reservation.getType() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Verificăm dacă există deja o rezervare pentru această dată, slot și sală
            boolean slotExists = StreamSupport.stream(reservationSpringRepository.findAll().spliterator(), false)
                    .anyMatch(r -> r.getHall() != null &&
                            r.getHall().getId().equals(reservation.getHall().getId()) &&
                            dateEquals(r.getDate(), reservation.getDate()) &&
                            r.getTimeSlot().equals(reservation.getTimeSlot()));

            if (slotExists) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }

            // Salvăm rezervarea
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

        // Calculăm sfârșitul săptămânii
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(weekStart);
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6);
        Date weekEnd = cal.getTime();

        return StreamSupport.stream(reservationSpringRepository.findAll().spliterator(), false)
                .filter(reservation -> reservation.getHall() != null &&
                        reservation.getHall().getId().equals(hallId))
                .filter(reservation -> "maintenance".equals(reservation.getType()))
                .filter(reservation -> {
                    // Verificăm dacă data rezervării este în intervalul săptămânii
                    if (reservation.getDate() == null) {
                        return false;
                    }

                    return !reservation.getDate().before(weekStart) &&
                            !reservation.getDate().after(weekEnd);
                })
                .collect(Collectors.toList());
    }
}