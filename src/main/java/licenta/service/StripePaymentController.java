package licenta.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;
import licenta.model.*;
import licenta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/payment")
@CrossOrigin(origins = "http://localhost:3000")
public class StripePaymentController {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentController.class);

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Autowired
    private IPaymentSpringRepository paymentRepository;

    @Autowired
    private IReservationSpringRepository reservationRepository;

    @Autowired
    private IUserSpringRepository userRepository;

    @Autowired
    private ISportsHallSpringRepository sportsHallRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @PostMapping("/stripe/create-payment-intent")
    public ResponseEntity<?> createStripePaymentIntent(@RequestBody StripePaymentIntentRequest request) {
        try {
            if (request.getAmount() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Suma trebuie să fie pozitivă"));
            }
            if (request.getReservations() == null || request.getReservations().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lista de rezervări nu poate fi goală"));
            }

            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Utilizator neautentificat."));
            }

            // Creează Payment în BD (inițial PENDING)
            Payment payment = new Payment();
            payment.setUserId(userId);
            payment.setTotalAmount(BigDecimal.valueOf(request.getAmount() / 100.0));
            payment.setPaymentMethod(licenta.model.PaymentMethod.CARD); // Folosește fully qualified name
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCurrency("RON");
            payment.setDescription("Rezervare sala de sport - " + request.getReservations().size() + " intervale");
            Payment savedPayment = paymentRepository.save(payment);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount())
                    .setCurrency("RON")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                    )
                    .putMetadata("payment_db_id", savedPayment.getId().toString())
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("reservations_count", String.valueOf(request.getReservations().size()))
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            savedPayment.setStripePaymentIntentId(paymentIntent.getId());
            paymentRepository.save(savedPayment);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentId", savedPayment.getId().toString());
            response.put("paymentIntentId", paymentIntent.getId());

            logger.info("Stripe PaymentIntent creat cu succes: {} pentru Payment DB ID: {}", paymentIntent.getId(), savedPayment.getId());
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            logger.error("Eroare Stripe la crearea PaymentIntent: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare Stripe: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Eroare generală la crearea PaymentIntent: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    @PostMapping("/stripe/confirm-payment-with-card")
    public ResponseEntity<?> confirmPaymentWithCard(@RequestBody StripeConfirmPaymentRequest request) {
        try {
            if (request.getClientSecret() == null || request.getCardDetails() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Client secret și detaliile cardului sunt obligatorii."));
            }

            // Creează PaymentMethod pe server (mai sigur) - folosește fully qualified name pentru Stripe PaymentMethod
            PaymentMethodCreateParams pmParams = PaymentMethodCreateParams.builder()
                    .setType(PaymentMethodCreateParams.Type.CARD)
                    .setCard(PaymentMethodCreateParams.CardDetails.builder()
                            .setNumber(request.getCardDetails().getNumber())
                            .setExpMonth(Long.valueOf(request.getCardDetails().getExp_month()))
                            .setExpYear(Long.valueOf(request.getCardDetails().getExp_year()))
                            .setCvc(request.getCardDetails().getCvc())
                            .build())
                    .setBillingDetails(PaymentMethodCreateParams.BillingDetails.builder()
                            .setName(request.getCardDetails().getCardholder_name())
                            .build())
                    .build();

            com.stripe.model.PaymentMethod stripePaymentMethod = com.stripe.model.PaymentMethod.create(pmParams);

            // Confirmă PaymentIntent cu PaymentMethod
            PaymentIntent paymentIntent = PaymentIntent.retrieve(extractPaymentIntentId(request.getClientSecret()));

            PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(stripePaymentMethod.getId())
                    .build();

            PaymentIntent confirmedPaymentIntent = paymentIntent.confirm(confirmParams);

            Map<String, Object> response = new HashMap<>();
            response.put("paymentIntent", Map.of(
                    "id", confirmedPaymentIntent.getId(),
                    "status", confirmedPaymentIntent.getStatus()
            ));

            logger.info("Payment confirmed successfully: {} with status: {}", confirmedPaymentIntent.getId(), confirmedPaymentIntent.getStatus());
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            logger.error("Stripe error during payment confirmation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("General error during payment confirmation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            logger.warn("Verificare semnătură webhook eșuată: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            logger.error("Eroare la construcția evenimentului webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            logger.warn("Nu s-a putut deserializa dataObject pentru event ID: {}", event.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook event data missing");
        }

        logger.info("Webhook Stripe primit - Tip: {}, ID: {}", event.getType(), event.getId());

        PaymentIntent paymentIntent;
        Optional<Payment> paymentOpt;

        switch (event.getType()) {
            case "payment_intent.succeeded":
                paymentIntent = (PaymentIntent) stripeObject;
                logger.info("PaymentIntent Succeeded: {}", paymentIntent.getId());
                paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());

                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
                        logger.info("Payment {} deja marcat ca SUCCEEDED.", payment.getId());
                        return ResponseEntity.ok("Webhook procesat (deja SUCCEEDED)");
                    }
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    payment.setPaymentDate(LocalDateTime.now());
                    if (paymentIntent.getLatestCharge() != null) {
                        payment.setStripeChargeId(paymentIntent.getLatestCharge());
                    }
                    paymentRepository.save(payment);
                    logger.info("Payment {} (DB ID) marcat ca SUCCEEDED via webhook pentru Stripe PI: {}", payment.getId(), paymentIntent.getId());
                } else {
                    logger.warn("Payment not found for successful Stripe PI: {}", paymentIntent.getId());
                }
                break;

            case "payment_intent.payment_failed":
                paymentIntent = (PaymentIntent) stripeObject;
                logger.info("PaymentIntent Failed: {}", paymentIntent.getId());
                paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());

                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    payment.setStatus(PaymentStatus.FAILED);
                    if (paymentIntent.getLastPaymentError() != null) {
                        payment.setFailureReason(paymentIntent.getLastPaymentError().getMessage());
                    }
                    paymentRepository.save(payment);
                    logger.info("Payment {} (DB ID) marcat ca FAILED via webhook pentru Stripe PI: {}", payment.getId(), paymentIntent.getId());
                } else {
                    logger.warn("Payment not found for failed Stripe PI: {}", paymentIntent.getId());
                }
                break;

            case "payment_intent.canceled":
                paymentIntent = (PaymentIntent) stripeObject;
                logger.info("PaymentIntent Canceled: {}", paymentIntent.getId());
                paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    payment.setStatus(PaymentStatus.CANCELLED);
                    if (paymentIntent.getCancellationReason() != null) {
                        payment.setFailureReason("Cancelled: " + paymentIntent.getCancellationReason());
                    }
                    paymentRepository.save(payment);
                    logger.info("Payment {} (DB ID) marcat ca CANCELLED via webhook pentru Stripe PI: {}", payment.getId(), paymentIntent.getId());
                } else {
                    logger.warn("Payment not found for cancelled Stripe PI: {}", paymentIntent.getId());
                }
                break;

            default:
                logger.info("Webhook Stripe - Tip eveniment neprocesat: {}", event.getType());
        }
        return ResponseEntity.ok("Webhook procesat cu succes");
    }

    @PostMapping("/stripe/finalize-payment-and-create-reservations")
    public ResponseEntity<?> finalizePaymentAndCreateReservations(@RequestBody StripeFinalizePaymentRequest request) {
        try {
            if (request.getStripePaymentIntentId() == null || request.getReservations() == null || request.getReservations().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "ID PaymentIntent și rezervările sunt obligatorii."));
            }

            Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(request.getStripePaymentIntentId());
            if (paymentOpt.isEmpty()) {
                logger.warn("Finalize payment: Payment not found for Stripe PI ID: {}", request.getStripePaymentIntentId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Plata negăsită."));
            }

            Payment payment = paymentOpt.get();

            // Verifică statusul PaymentIntent direct la Stripe pentru siguranță
            PaymentIntent stripePI = PaymentIntent.retrieve(request.getStripePaymentIntentId());

            if (!"succeeded".equals(stripePI.getStatus())) {
                logger.warn("Finalize payment: Stripe PI {} nu este 'succeeded'. Status actual: {}", stripePI.getId(), stripePI.getStatus());
                if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.PROCESSING) {
                    if ("requires_payment_method".equals(stripePI.getStatus()) || "requires_confirmation".equals(stripePI.getStatus()) || "requires_action".equals(stripePI.getStatus())) {
                        payment.setStatus(PaymentStatus.PENDING);
                    } else if("processing".equals(stripePI.getStatus())){
                        payment.setStatus(PaymentStatus.PROCESSING);
                    } else if ("canceled".equals(stripePI.getStatus())) {
                        payment.setStatus(PaymentStatus.CANCELLED);
                    } else {
                        payment.setStatus(PaymentStatus.FAILED);
                    }
                    if (stripePI.getLastPaymentError() != null) {
                        payment.setFailureReason(stripePI.getLastPaymentError().getMessage());
                    }
                    paymentRepository.save(payment);
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Plata Stripe nu este finalizată cu succes. Status: " + stripePI.getStatus()));
            }

            if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setPaymentDate(LocalDateTime.now());
                if (stripePI.getLatestCharge() != null) {
                    payment.setStripeChargeId(stripePI.getLatestCharge());
                }
            } else if (!payment.getPaymentReservations().isEmpty()) {
                logger.info("Payment {} (DB ID) deja marcat SUCCEEDED și rezervările create.", payment.getId());
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("paymentId", payment.getId());
                response.put("reservationIds", payment.getPaymentReservations().stream().map(pr -> pr.getReservation().getId()).toList());
                response.put("message", "Plata Stripe deja procesată cu succes.");
                return ResponseEntity.ok(response);
            }

            // Creează rezervările și le asociază cu plata
            List<Long> reservationIds = new ArrayList<>();
            User user = userRepository.findById(payment.getUserId())
                    .orElseThrow(() -> new RuntimeException("Utilizatorul plății nu a fost găsit: " + payment.getUserId()));

            for (ReservationData reservationData : request.getReservations()) {
                SportsHall hall = sportsHallRepository.findById(reservationData.getHallId())
                        .orElseThrow(() -> new RuntimeException("Sala de sport nu a fost găsită: " + reservationData.getHallId()));

                LocalDate localDate = LocalDate.parse(reservationData.getDate());
                Date utilDate = java.sql.Date.valueOf(localDate);

                Reservation reservation = new Reservation(hall, user, utilDate,
                        reservationData.getTimeSlot(), 50.0f, "reservation");

                Reservation savedReservation = reservationRepository.save(reservation);
                reservationIds.add(savedReservation.getId());

                PaymentReservation paymentReservation = new PaymentReservation(payment, savedReservation);
                payment.getPaymentReservations().add(paymentReservation);
            }

            paymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("paymentId", payment.getId());
            response.put("reservationIds", reservationIds);
            response.put("message", "Plata Stripe procesată cu succes și rezervările create.");

            logger.info("Stripe Payment {} (DB ID) marcat ca SUCCEEDED și {} rezervări create via finalize.", payment.getId(), reservationIds.size());
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            logger.error("Eroare Stripe la finalizarea plății: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Eroare Stripe: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Eroare generală la finalizarea plății Stripe: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm-payment-cash")
    public ResponseEntity<?> confirmCashPayment(@RequestBody PaymentConfirmationRequestCash request) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Utilizator neautentificat."));
            }
            List<Long> reservationIds = new ArrayList<>();

            Payment payment = new Payment();
            payment.setUserId(userId);
            payment.setTotalAmount(BigDecimal.valueOf(request.getTotalAmount()));
            payment.setPaymentMethod(licenta.model.PaymentMethod.CASH);
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setCurrency("RON");
            payment.setDescription("Rezervare sala de sport - plată cash (" + request.getReservations().size() + " intervale)");
            Payment savedPayment = paymentRepository.save(payment);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost găsit: " + userId));

            for (ReservationData reservationData : request.getReservations()) {
                SportsHall hall = sportsHallRepository.findById(reservationData.getHallId())
                        .orElseThrow(() -> new RuntimeException("Sala de sport nu a fost găsită: " + reservationData.getHallId()));

                LocalDate localDate = LocalDate.parse(reservationData.getDate());
                Date utilDate = java.sql.Date.valueOf(localDate);

                Reservation reservation = new Reservation(hall, user, utilDate,
                        reservationData.getTimeSlot(), 50.0f, "reservation");

                Reservation savedReservation = reservationRepository.save(reservation);
                reservationIds.add(savedReservation.getId());

                PaymentReservation paymentReservation = new PaymentReservation(savedPayment, savedReservation);
                savedPayment.getPaymentReservations().add(paymentReservation);
            }
            paymentRepository.save(savedPayment);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("paymentId", savedPayment.getId());
            response.put("reservationIds", reservationIds);
            response.put("message", "Rezervări (cash) create cu succes");

            logger.info("Cash Payment {} (DB ID) creat cu {} rezervări.", savedPayment.getId(), reservationIds.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Eroare la confirmarea rezervărilor cash", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare la crearea rezervărilor cash: " + e.getMessage()));
        }
    }

    private String extractPaymentIntentId(String clientSecret) {
        return clientSecret.split("_secret_")[0];
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                Optional<User> userOpt = userRepository.findByEmail(username);
                return userOpt.map(User::getId).orElse(null);
            } else if (principal instanceof licenta.model.User) {
                return ((licenta.model.User) principal).getId();
            }
        }
        logger.warn("getCurrentUserId: Utilizator neautentificat sau informații utilizator indisponibile.");
        return null;
    }

    // DTO Classes
    public static class StripePaymentIntentRequest {
        private Long amount;
        private List<ReservationData> reservations;

        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
        public List<ReservationData> getReservations() { return reservations; }
        public void setReservations(List<ReservationData> reservations) { this.reservations = reservations; }
    }

    public static class StripeFinalizePaymentRequest {
        private String stripePaymentIntentId;
        private List<ReservationData> reservations;

        public String getStripePaymentIntentId() { return stripePaymentIntentId; }
        public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
        public List<ReservationData> getReservations() { return reservations; }
        public void setReservations(List<ReservationData> reservations) { this.reservations = reservations; }
    }

    public static class StripeConfirmPaymentRequest {
        private String clientSecret;
        private CardDetailsRequest cardDetails;

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public CardDetailsRequest getCardDetails() { return cardDetails; }
        public void setCardDetails(CardDetailsRequest cardDetails) { this.cardDetails = cardDetails; }
    }

    public static class CardDetailsRequest {
        private String number;
        private Integer exp_month;
        private Integer exp_year;
        private String cvc;
        private String cardholder_name;

        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }
        public Integer getExp_month() { return exp_month; }
        public void setExp_month(Integer exp_month) { this.exp_month = exp_month; }
        public Integer getExp_year() { return exp_year; }
        public void setExp_year(Integer exp_year) { this.exp_year = exp_year; }
        public String getCvc() { return cvc; }
        public void setCvc(String cvc) { this.cvc = cvc; }
        public String getCardholder_name() { return cardholder_name; }
        public void setCardholder_name(String cardholder_name) { this.cardholder_name = cardholder_name; }
    }

    public static class ReservationData {
        private Long hallId;
        private String hallName;
        private String date;
        private String timeSlot;

        public Long getHallId() { return hallId; }
        public void setHallId(Long hallId) { this.hallId = hallId; }
        public String getHallName() { return hallName; }
        public void setHallName(String hallName) { this.hallName = hallName; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    }

    public static class PaymentConfirmationRequestCash {
        private List<ReservationData> reservations;
        private Double totalAmount;

        public List<ReservationData> getReservations() { return reservations; }
        public void setReservations(List<ReservationData> reservations) { this.reservations = reservations; }
        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    }
}