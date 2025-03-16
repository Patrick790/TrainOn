package licenta.service;

import licenta.model.Reservation;
import licenta.persistence.IReservationSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
public class ReservationRestController {

    private final IReservationSpringRepository reservationSpringRepository;

    @Autowired
    public ReservationRestController(IReservationSpringRepository reservationSpringRepository) {
        this.reservationSpringRepository = reservationSpringRepository;
    }

    @GetMapping("/{id}")
    public Reservation getReservationById(@PathVariable Long id) {
        return reservationSpringRepository.findById(id).orElse(null);
    }

    @GetMapping
    public Iterable<Reservation> getAllReservations() {
        return reservationSpringRepository.findAll();
    }

    @PostMapping
    public Reservation createReservation(@RequestBody Reservation reservation) {
        return reservationSpringRepository.save(reservation);
    }

    @DeleteMapping("/{id}")
    public void deleteReservation(@PathVariable Long id) {
        reservationSpringRepository.deleteById(id);
    }


}
