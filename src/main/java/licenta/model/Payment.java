// Payment.java - Entitatea principală
package licenta.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
public class Payment extends BruteEntity<Long> {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency = "RON";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "stripe_charge_id")
    private String stripeChargeId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "description")
    private String description;

    @Column(name = "failure_reason")
    private String failureReason;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("payment")
    private List<PaymentReservation> paymentReservations = new ArrayList<>();

    // Constructors
    public Payment() {
        this.status = PaymentStatus.PENDING;
        this.currency = "RON";
    }

    public Payment(Long userId, BigDecimal totalAmount, PaymentMethod paymentMethod) {
        this();
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
    }

    // Helper methods
    public void addReservation(Reservation reservation) {
        PaymentReservation paymentReservation = new PaymentReservation();
        paymentReservation.setPayment(this);
        paymentReservation.setReservation(reservation);
        paymentReservations.add(paymentReservation);
    }

    public void markAsSucceeded(String stripePaymentIntentId, String stripeChargeId) {
        this.status = PaymentStatus.SUCCEEDED;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeChargeId = stripeChargeId;
        this.paymentDate = LocalDateTime.now();
    }

    public void markAsFailed(String failureReason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
    }

    // Getters și Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }

    public String getStripeChargeId() { return stripeChargeId; }
    public void setStripeChargeId(String stripeChargeId) { this.stripeChargeId = stripeChargeId; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public List<PaymentReservation> getPaymentReservations() { return paymentReservations; }
    public void setPaymentReservations(List<PaymentReservation> paymentReservations) { this.paymentReservations = paymentReservations; }
}