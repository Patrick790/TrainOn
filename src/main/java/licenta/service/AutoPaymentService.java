package licenta.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import licenta.model.*;
import licenta.persistence.IPaymentSpringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AutoPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(AutoPaymentService.class);

    @Autowired
    private IPaymentSpringRepository paymentRepository;

    /**
     * Procesează plata automată pentru un profil de rezervare
     */
    public PaymentResult processAutoPayment(ReservationProfile profile, BigDecimal totalAmount, User user) {
        try {
            // Verificări de validitate
            if (!profile.getAutoPaymentEnabled()) {
                return PaymentResult.failure("Plata automată nu este activată pentru acest profil");
            }

            CardPaymentMethod defaultCard = profile.getDefaultCardPaymentMethod();
            if (defaultCard == null || !defaultCard.getIsActive() || defaultCard.isExpired()) {
                return PaymentResult.failure("Nu există un card valid pentru plata automată");
            }

            // Verifică limitele de plată
            if (!profile.canProcessAutoPayment(totalAmount)) {
                return PaymentResult.failure("Suma depășește limitele configurate pentru plata automată");
            }

            // Creează Payment în baza de date
            Payment payment = createPendingPayment(user.getId(), totalAmount, profile);

            try {
                // Procesează plata cu Stripe
                PaymentIntent paymentIntent = createStripePaymentIntent(payment, defaultCard, totalAmount);

                payment.setStripePaymentIntentId(paymentIntent.getId());

                if ("succeeded".equals(paymentIntent.getStatus())) {
                    // Plata a reușit
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    payment.setPaymentDate(LocalDateTime.now());
                    if (paymentIntent.getLatestCharge() != null) {
                        payment.setStripeChargeId(paymentIntent.getLatestCharge());
                    }

                    Payment savedPayment = paymentRepository.save(payment);

                    logger.info("Plată automată reușită pentru profilul {} - suma: {} RON",
                            profile.getName(), totalAmount);

                    return PaymentResult.success(savedPayment, "card_auto", "Plată automată procesată cu succes");

                } else if ("requires_action".equals(paymentIntent.getStatus())) {
                    // Necesită acțiune suplimentară - nu este potrivit pentru plata automată
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("Plata automată necesită autentificare suplimentară");
                    paymentRepository.save(payment);

                    return PaymentResult.failure("Cardul necesită autentificare suplimentară - nu este potrivit pentru plata automată");

                } else {
                    // Plata a eșuat
                    payment.setStatus(PaymentStatus.FAILED);
                    if (paymentIntent.getLastPaymentError() != null) {
                        payment.setFailureReason(paymentIntent.getLastPaymentError().getMessage());
                    }
                    paymentRepository.save(payment);

                    return PaymentResult.failure("Plata automată a eșuat: " + paymentIntent.getStatus());
                }

            } catch (StripeException e) {
                logger.error("Eroare Stripe la plata automată pentru profilul {}: {}",
                        profile.getName(), e.getMessage(), e);

                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Eroare Stripe: " + e.getMessage());
                paymentRepository.save(payment);

                return PaymentResult.failure("Eroare la procesarea plății: " + e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Eroare generală la plata automată pentru profilul {}: {}",
                    profile.getName(), e.getMessage(), e);

            return PaymentResult.failure("Eroare generală: " + e.getMessage());
        }
    }

    /**
     * Creează un PaymentIntent cu Stripe pentru plata automată
     */
    private PaymentIntent createStripePaymentIntent(Payment payment, CardPaymentMethod card, BigDecimal amount)
            throws StripeException {

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount.doubleValue() * 100)) // convertește în cenți
                .setCurrency("RON")
                .setPaymentMethod(card.getStripePaymentMethodId())
                .setConfirm(true) // Confirmă imediat pentru plata automată
                .setOffSession(true) // Indică că este o plată fără prezența utilizatorului
                .putMetadata("payment_db_id", payment.getId().toString())
                .putMetadata("user_id", payment.getUserId().toString())
                .putMetadata("auto_payment", "true")
                .putMetadata("card_id", card.getId().toString())
                .build();

        return PaymentIntent.create(params);
    }

    /**
     * Creează un Payment în status PENDING
     */
    private Payment createPendingPayment(Long userId, BigDecimal amount, ReservationProfile profile) {
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setTotalAmount(amount);
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setCurrency("RON");
        payment.setDescription("Rezervare automată - profil: " + profile.getName());

        return paymentRepository.save(payment);
    }
}