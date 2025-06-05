package licenta.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "payment_reservations")
public class PaymentReservation extends BruteEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    @JsonIgnoreProperties("paymentReservations")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    @JsonIgnoreProperties("paymentReservations")
    private Reservation reservation;

    // Constructors
    public PaymentReservation() {}

    public PaymentReservation(Payment payment, Reservation reservation) {
        this.payment = payment;
        this.reservation = reservation;
    }

    // Getters și Setters
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
}