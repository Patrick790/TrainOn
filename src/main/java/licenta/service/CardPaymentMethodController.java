package licenta.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodCreateParams;
import jakarta.transaction.Transactional;
import licenta.model.CardPaymentMethod;
import licenta.model.User;
import licenta.persistence.ICardPaymentMethodSpringRepository;
import licenta.persistence.IUserSpringRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/card-payment-methods")
@CrossOrigin(origins = "http://localhost:3000")
@Transactional
public class CardPaymentMethodController {

    private static final Logger logger = LoggerFactory.getLogger(CardPaymentMethodController.class);

    @Autowired
    private ICardPaymentMethodSpringRepository cardPaymentMethodRepository;

    @Autowired
    private IUserSpringRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getUserCardPaymentMethods() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Utilizator neautentificat"));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilizator negăsit"));

            List<CardPaymentMethod> cards = cardPaymentMethodRepository
                    .findByUserAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(user);

            List<Map<String, Object>> cardDTOs = cards.stream().map(this::convertToDTO).toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cards", cardDTOs
            ));

        } catch (Exception e) {
            logger.error("Eroare la obținerea cardurilor utilizatorului", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    @PostMapping("/save-from-stripe")
    public ResponseEntity<?> saveCardFromStripe(@RequestBody SaveCardFromStripeRequest request) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Utilizator neautentificat"));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilizator negăsit"));

            // Verifică dacă cardul nu există deja
            if (cardPaymentMethodRepository.existsByUserAndStripePaymentMethodId(user, request.getStripePaymentMethodId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Acest card este deja salvat"));
            }

            // Obține detaliile cardului de la Stripe
            PaymentMethod stripePaymentMethod = PaymentMethod.retrieve(request.getStripePaymentMethodId());
            PaymentMethod.Card cardDetails = stripePaymentMethod.getCard();

            if (cardDetails == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Detaliile cardului nu sunt disponibile"));
            }

            // Creează entitatea CardPaymentMethod
            CardPaymentMethod cardPaymentMethod = new CardPaymentMethod();
            cardPaymentMethod.setStripePaymentMethodId(request.getStripePaymentMethodId());
            cardPaymentMethod.setCardLastFour(cardDetails.getLast4());
            cardPaymentMethod.setCardBrand(cardDetails.getBrand());
            cardPaymentMethod.setCardExpMonth(Math.toIntExact(cardDetails.getExpMonth()));
            cardPaymentMethod.setCardExpYear(Math.toIntExact(cardDetails.getExpYear()));
            cardPaymentMethod.setUser(user);

            // Setează numele titularului de card
            if (stripePaymentMethod.getBillingDetails() != null &&
                    stripePaymentMethod.getBillingDetails().getName() != null) {
                cardPaymentMethod.setCardholderName(stripePaymentMethod.getBillingDetails().getName());
            } else {
                cardPaymentMethod.setCardholderName(request.getCardholderName() != null ?
                        request.getCardholderName() : "Titular necunoscut");
            }

            // Setează detaliile de facturare dacă sunt disponibile
            if (stripePaymentMethod.getBillingDetails() != null) {
                if (stripePaymentMethod.getBillingDetails().getAddress() != null) {
                    cardPaymentMethod.setBillingCountry(stripePaymentMethod.getBillingDetails().getAddress().getCountry());
                    cardPaymentMethod.setBillingPostalCode(stripePaymentMethod.getBillingDetails().getAddress().getPostalCode());
                }
            }

            // Dacă este primul card sau este marcat ca default, fă-l default
            boolean isFirstCard = cardPaymentMethodRepository.countByUserAndIsActiveTrue(user) == 0;
            if (isFirstCard || Boolean.TRUE.equals(request.getSetAsDefault())) {
                // Marchează toate cardurile existente ca non-default
                cardPaymentMethodRepository.unmarkAllAsDefaultForUser(user);
                cardPaymentMethod.setIsDefault(true);
            }

            CardPaymentMethod savedCard = cardPaymentMethodRepository.save(cardPaymentMethod);

            logger.info("Card salvat cu succes pentru utilizatorul {}: {}", userId, savedCard.getDisplayName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Card salvat cu succes",
                    "card", convertToDTO(savedCard)
            ));

        } catch (StripeException e) {
            logger.error("Eroare Stripe la salvarea cardului", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Eroare Stripe: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Eroare la salvarea cardului", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    @PostMapping("/save-card")
    public ResponseEntity<Map<String, Object>> saveCard(@RequestBody SaveCardRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // LOG: Verifică utilizatorul
            User currentUser = getCurrentUser();
            logger.info("=== SAVE CARD DEBUG ===");
            logger.info("Current user: {}", currentUser != null ? currentUser.getEmail() : "NULL");

            if (currentUser == null) {
                logger.error("No current user found!");
                response.put("success", false);
                response.put("error", "Utilizator neautentificat");
                return ResponseEntity.status(401).body(response);
            }

            String cardNumber = request.getCardDetails().getNumber().replace(" ", "");
            logger.info("Card number received: {}", cardNumber);

            String paymentMethodId;
            String cardBrand;
            String lastFour;

            // Pentru cardurile de test, generează ID-uri unice
            if ("4242424242424242".equals(cardNumber)) {
                paymentMethodId = "pm_card_visa_" + System.currentTimeMillis() + "_" + currentUser.getId();
                cardBrand = "visa";
                lastFour = "4242";
            } else if ("5555555555554444".equals(cardNumber)) {
                paymentMethodId = "pm_card_mastercard_" + System.currentTimeMillis() + "_" + currentUser.getId();
                cardBrand = "mastercard";
                lastFour = "4444";
            } else if ("4000056655665556".equals(cardNumber)) {
                paymentMethodId = "pm_card_visa_debit_" + System.currentTimeMillis() + "_" + currentUser.getId();
                cardBrand = "visa";
                lastFour = "5556";
            } else if ("5200828282828210".equals(cardNumber)) {
                paymentMethodId = "pm_card_mastercard_debit_" + System.currentTimeMillis() + "_" + currentUser.getId();
                cardBrand = "mastercard";
                lastFour = "8210";
            } else {
                response.put("success", false);
                response.put("error", "Pentru testare, vă rugăm să folosiți cardurile recomandate: 4242 4242 4242 4242 (Visa) sau 5555 5555 5555 4444 (Mastercard)");
                return ResponseEntity.status(400).body(response);
            }

            logger.info("Generated payment method ID: {}", paymentMethodId);

            // Verifică dacă cardul nu există deja (nu ar trebui să existe cu ID-ul generat)
            boolean cardExists = cardPaymentMethodRepository.existsByUserAndStripePaymentMethodId(currentUser, paymentMethodId);
            logger.info("Card already exists: {}", cardExists);

            if (cardExists) {
                response.put("success", false);
                response.put("error", "Acest card este deja salvat");
                return ResponseEntity.status(400).body(response);
            }

            // Dacă va fi cardul default, resetează alte carduri
            if (request.isSetAsDefault()) {
                List<CardPaymentMethod> userCards = cardPaymentMethodRepository.findByUserAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(currentUser);
                for (CardPaymentMethod existingCard : userCards) {
                    if (existingCard.getIsDefault()) {
                        existingCard.setIsDefault(false);
                        cardPaymentMethodRepository.save(existingCard);
                    }
                }
            }

            // Creează și salvează cardul în baza de date
            CardPaymentMethod cardPaymentMethod = new CardPaymentMethod();
            cardPaymentMethod.setStripePaymentMethodId(paymentMethodId);
            cardPaymentMethod.setCardholderName(request.getCardDetails().getCardholder_name());
            cardPaymentMethod.setUser(currentUser);
            cardPaymentMethod.setIsDefault(request.isSetAsDefault());
            cardPaymentMethod.setCardLastFour(lastFour);
            cardPaymentMethod.setCardBrand(cardBrand);
            cardPaymentMethod.setCardExpMonth(request.getCardDetails().getExp_month());
            cardPaymentMethod.setCardExpYear(request.getCardDetails().getExp_year());

            CardPaymentMethod savedCard = cardPaymentMethodRepository.save(cardPaymentMethod);
            logger.info("Card saved successfully with ID: {}", savedCard.getId());

            response.put("success", true);
            response.put("cardId", savedCard.getId());
            response.put("message", "Cardul a fost salvat cu succes");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Exception in saveCard:", e);
            response.put("success", false);
            response.put("error", "Eroare la salvarea cardului: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/{cardId}/set-default")
    public ResponseEntity<?> setCardAsDefault(@PathVariable Long cardId) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Utilizator neautentificat"));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilizator negăsit"));

            CardPaymentMethod card = cardPaymentMethodRepository.findById(cardId)
                    .orElseThrow(() -> new RuntimeException("Card negăsit"));

            // Verifică dacă cardul aparține utilizatorului
            if (!card.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Nu aveți permisiunea să modificați acest card"));
            }

            if (!card.getIsActive()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Nu puteți seta ca default un card inactiv"));
            }

            // Marchează toate cardurile ca non-default
            List<CardPaymentMethod> userCards = cardPaymentMethodRepository.findByUserAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(user);
            for (CardPaymentMethod existingCard : userCards) {
                if (existingCard.getIsDefault()) {
                    existingCard.setIsDefault(false);
                    cardPaymentMethodRepository.save(existingCard);
                }
            }

            // Marchează cardul curent ca default
            card.markAsDefault();
            cardPaymentMethodRepository.save(card);

            logger.info("Card setat ca default pentru utilizatorul {}: {}", userId, card.getDisplayName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Card setat ca default",
                    "card", convertToDTO(card)
            ));

        } catch (Exception e) {
            logger.error("Eroare la setarea cardului ca default", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<?> removeCard(@PathVariable Long cardId) {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Utilizator neautentificat"));
            }

            CardPaymentMethod card = cardPaymentMethodRepository.findById(cardId)
                    .orElseThrow(() -> new RuntimeException("Card negăsit"));

            // Verifică dacă cardul aparține utilizatorului
            if (!card.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Nu aveți permisiunea să ștergeți acest card"));
            }

            // Dezactivează cardul (nu îl ștergem complet pentru istoricul plăților)
            card.deactivate();
            cardPaymentMethodRepository.save(card);

            // Încearcă să ștergi PaymentMethod-ul și de la Stripe (opțional)
            try {
                PaymentMethod stripePaymentMethod = PaymentMethod.retrieve(card.getStripePaymentMethodId());
                stripePaymentMethod.detach();
                logger.info("PaymentMethod detached from Stripe: {}", card.getStripePaymentMethodId());
            } catch (StripeException e) {
                logger.warn("Nu s-a putut detach PaymentMethod de la Stripe: {}", e.getMessage());
                // Nu returnăm eroare, cardul este dezactivat în BD oricum
            }

            logger.info("Card dezactivat pentru utilizatorul {}: {}", userId, card.getDisplayName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Card înlăturat cu succes"
            ));

        } catch (Exception e) {
            logger.error("Eroare la înlăturarea cardului", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    @GetMapping("/default")
    public ResponseEntity<?> getDefaultCard() {
        try {
            Long userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Utilizator neautentificat"));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilizator negăsit"));

            Optional<CardPaymentMethod> defaultCard = cardPaymentMethodRepository
                    .findByUserAndIsDefaultTrueAndIsActiveTrue(user);

            if (defaultCard.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "hasDefaultCard", false,
                        "message", "Nu există un card default setat"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasDefaultCard", true,
                    "card", convertToDTO(defaultCard.get())
            ));

        } catch (Exception e) {
            logger.error("Eroare la obținerea cardului default", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    @PostMapping("/cleanup-expired")
    public ResponseEntity<?> cleanupExpiredCards() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deactivatedCount = cardPaymentMethodRepository.deactivateExpiredCards(
                    now.getYear(), now.getMonthValue());

            logger.info("Au fost dezactivate {} carduri expirate", deactivatedCount);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cardurile expirate au fost dezactivate",
                    "deactivatedCount", deactivatedCount
            ));

        } catch (Exception e) {
            logger.error("Eroare la curățarea cardurilor expirate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Eroare server: " + e.getMessage()));
        }
    }

    // Helper methods
    private Map<String, Object> convertToDTO(CardPaymentMethod card) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", card.getId());
        dto.put("displayName", card.getDisplayName());
        dto.put("cardBrand", card.getCardBrand());
        dto.put("cardLastFour", card.getCardLastFour());
        dto.put("cardExpMonth", card.getCardExpMonth());
        dto.put("cardExpYear", card.getCardExpYear());
        dto.put("cardholderName", card.getCardholderName());
        dto.put("isDefault", card.getIsDefault());
        dto.put("isActive", card.getIsActive());
        dto.put("isExpired", card.isExpired());
        dto.put("createdAt", card.getCreatedAt());
        dto.put("stripePaymentMethodId", card.getStripePaymentMethodId());
        return dto;
    }

    private User getCurrentUser() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
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
    public static class SaveCardFromStripeRequest {
        private String stripePaymentMethodId;
        private String cardholderName;
        private Boolean setAsDefault;

        public String getStripePaymentMethodId() { return stripePaymentMethodId; }
        public void setStripePaymentMethodId(String stripePaymentMethodId) { this.stripePaymentMethodId = stripePaymentMethodId; }

        public String getCardholderName() { return cardholderName; }
        public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }

        public Boolean getSetAsDefault() { return setAsDefault; }
        public void setSetAsDefault(Boolean setAsDefault) { this.setAsDefault = setAsDefault; }
    }

    public static class SaveCardRequest {
        private CardDetails cardDetails;
        private boolean setAsDefault;

        // Getters și setters
        public CardDetails getCardDetails() { return cardDetails; }
        public void setCardDetails(CardDetails cardDetails) { this.cardDetails = cardDetails; }
        public boolean isSetAsDefault() { return setAsDefault; }
        public void setSetAsDefault(boolean setAsDefault) { this.setAsDefault = setAsDefault; }

        public static class CardDetails {
            private String number;
            private int exp_month;
            private int exp_year;
            private String cvc;
            private String cardholder_name;

            // Getters și setters
            public String getNumber() { return number; }
            public void setNumber(String number) { this.number = number; }
            public int getExp_month() { return exp_month; }
            public void setExp_month(int exp_month) { this.exp_month = exp_month; }
            public int getExp_year() { return exp_year; }
            public void setExp_year(int exp_year) { this.exp_year = exp_year; }
            public String getCvc() { return cvc; }
            public void setCvc(String cvc) { this.cvc = cvc; }
            public String getCardholder_name() { return cardholder_name; }
            public void setCardholder_name(String cardholder_name) { this.cardholder_name = cardholder_name; }
        }
    }
}