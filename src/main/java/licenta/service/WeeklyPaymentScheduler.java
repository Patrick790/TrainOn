package licenta.service;

import licenta.model.*;
import licenta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviciu pentru gestionarea automată a plăților săptămânale
 * Se execută automat în fiecare duminică pentru a procesa plățile pentru săptămâna următoare
 */
@Service
public class WeeklyPaymentScheduler {

    private static final Logger logger = LoggerFactory.getLogger(WeeklyPaymentScheduler.class);

    @Autowired
    private BookingPrioritizationRestController bookingController;

    @Autowired
    private IReservationProfileRepository reservationProfileRepository;

    @Autowired
    private IPaymentSpringRepository paymentRepository;

    @Autowired
    private ICardPaymentMethodSpringRepository cardPaymentMethodRepository;

    @Autowired
    private AutoPaymentService autoPaymentService;


    /**
     * Se execută în fiecare duminică la ora 22:00 pentru a procesa plățile automate
     * Cron expression: "0 0 22 * * SUN" = la ora 22:00 în fiecare duminică
     */
    @Scheduled(cron = "0 0 22 * * SUN", zone = "Europe/Bucharest")
    public void processWeeklyAutomaticPayments() {
        logger.info("=== ÎNCEPUT PROCESARE PLĂȚI AUTOMATE SĂPTĂMÂNALE ===");

        try {
            // Apelează controllerul pentru generarea rezervărilor cu plăți automate
            var response = bookingController.generateBookings();

            if (response.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

                if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                    logger.info("Procesare automată reușită: {} rezervări generate",
                            responseBody.get("count"));

                    // Logăm statisticile plăților
                    logPaymentStatistics(responseBody);

                    // Notifică adminii despre rezultat
                    notifyAdminsAboutWeeklyGeneration(responseBody);
                } else {
                    logger.error("Generarea automată a rezervărilor a eșuat: {}",
                            responseBody != null ? responseBody.get("message") : "Răspuns null");
                }
            } else {
                logger.error("Eroare HTTP la generarea automată: status {}",
                        response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Eroare critică la procesarea automată săptămânală", e);

            // Încearcă să notifice adminii despre eroare
            try {
                notifyAdminsAboutError(e);
            } catch (Exception notificationError) {
                logger.error("Nu s-a putut trimite notificarea de eroare", notificationError);
            }
        }

        logger.info("=== SFÂRȘIT PROCESARE PLĂȚI AUTOMATE SĂPTĂMÂNALE ===");
    }

    /**
     * Task opțional care rulează în fiecare zi la 8:00 pentru verificarea cardurilor expirate
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Europe/Bucharest")
    public void checkExpiredCards() {
        try {
            logger.info("Verificare carduri expirate...");

            LocalDateTime now = LocalDateTime.now();
            int currentYear = now.getYear();
            int currentMonth = now.getMonthValue();

            // Dezactivează cardurile expirate
            int deactivatedCards = cardPaymentMethodRepository.deactivateExpiredCards(currentYear, currentMonth);

            if (deactivatedCards > 0) {
                logger.warn("Au fost dezactivate {} carduri expirate", deactivatedCards);

                // Găsește profilurile afectate și dezactivează plata automată
                disableAutoPaymentForExpiredCards();
            }

            // Verifică cardurile care urmează să expire în următoarele 2 luni
            checkCardsExpiringNextMonth(currentYear, currentMonth);

        } catch (Exception e) {
            logger.error("Eroare la verificarea cardurilor expirate", e);
        }
    }

    /**
     * Dezactivează plata automată pentru profilurile cu carduri expirate
     */
    private void disableAutoPaymentForExpiredCards() {
        try {
            List<ReservationProfile> allProfiles = (List<ReservationProfile>) reservationProfileRepository.findAll();

            int disabledProfiles = 0;
            for (ReservationProfile profile : allProfiles) {
                if (profile.getAutoPaymentEnabled() && profile.getDefaultCardPaymentMethod() != null) {
                    CardPaymentMethod card = profile.getDefaultCardPaymentMethod();

                    if (!card.getIsActive() || card.isExpired()) {
                        profile.disableAutoPayment();
                        reservationProfileRepository.save(profile);
                        disabledProfiles++;

                        logger.info("Dezactivată plata automată pentru profilul '{}' - card expirat",
                                profile.getName());
                    }
                }
            }

            if (disabledProfiles > 0) {
                logger.warn("Au fost dezactivate {} profiluri cu plăți automate din cauza cardurilor expirate",
                        disabledProfiles);
            }

        } catch (Exception e) {
            logger.error("Eroare la dezactivarea plăților automate pentru cardurile expirate", e);
        }
    }

    /**
     * Verifică cardurile care urmează să expire în următoarele luni
     */
    private void checkCardsExpiringNextMonth(int currentYear, int currentMonth) {
        try {
            int nextMonth = currentMonth + 1;
            int nextYear = currentYear;
            int nextMonthNextYear = nextMonth;

            if (nextMonth > 12) {
                nextMonth = 1;
                nextYear++;
                nextMonthNextYear = nextMonth;
            }

            // Găsește cardurile care expiră în următoarele 2 luni
            List<CardPaymentMethod> expiringSoon = cardPaymentMethodRepository
                    .findCardsExpiringInNextMonths(currentYear, nextMonth, nextYear, nextMonthNextYear);

            if (!expiringSoon.isEmpty()) {
                logger.info("Găsite {} carduri care expiră în următoarele luni", expiringSoon.size());

                // Aici poți adăuga logică pentru notificarea utilizatorilor
                // despre cardurile care urmează să expire
            }

        } catch (Exception e) {
            logger.error("Eroare la verificarea cardurilor care expiră curând", e);
        }
    }

    /**
     * Logăm statisticile plăților din generarea automată
     */
    private void logPaymentStatistics(Map<String, Object> responseBody) {
        try {
            Object paymentsCount = responseBody.get("paymentsCount");
            Object autoPaymentsSuccessful = responseBody.get("autoPaymentsSuccessful");
            Object autoPaymentsFailed = responseBody.get("autoPaymentsFailed");

            logger.info("=== STATISTICI PLĂȚI AUTOMATE ===");
            logger.info("Total plăți procesate: {}", paymentsCount);
            logger.info("Plăți automate reușite: {}", autoPaymentsSuccessful);
            logger.info("Plăți automate eșuate: {}", autoPaymentsFailed);

            // Calculează procentajul de succes pentru plățile automate
            if (autoPaymentsSuccessful instanceof Number && autoPaymentsFailed instanceof Number) {
                int successful = ((Number) autoPaymentsSuccessful).intValue();
                int failed = ((Number) autoPaymentsFailed).intValue();
                int total = successful + failed;

                if (total > 0) {
                    double successRate = (double) successful / total * 100;
                    logger.info("Rata de succes plăți automate: {:.1f}%", successRate);

                    if (successRate < 90.0) {
                        logger.warn("Rata de succes a plăților automate este sub 90%! Investigare necesară.");
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Eroare la logarea statisticilor plăților", e);
        }
    }

    /**
     * Notifică adminii despre rezultatul generării săptămânale
     */
    private void notifyAdminsAboutWeeklyGeneration(Map<String, Object> responseBody) {
        try {
            // Aici poți implementa logica de notificare a adminilor
            // De exemplu, prin email sau prin salvarea unei notificări în baza de date

            String summary = String.format(
                    "Generare automată rezervări completă: %s rezervări, %s plăți procesate",
                    responseBody.get("count"),
                    responseBody.get("paymentsCount")
            );

            logger.info("Notificare administratori: {}", summary);

            // TODO: Implementează trimiterea efectivă a notificării

        } catch (Exception e) {
            logger.error("Eroare la notificarea adminilor", e);
        }
    }

    /**
     * Notifică adminii despre erori critice
     */
    private void notifyAdminsAboutError(Exception error) {
        try {
            String errorMessage = String.format(
                    "EROARE CRITICĂ la procesarea automată săptămânală: %s",
                    error.getMessage()
            );

            logger.error("Notificare eroare către administratori: {}", errorMessage);

            // TODO: Implementează trimiterea unei notificări urgente către administratori

        } catch (Exception e) {
            logger.error("Eroare la notificarea de eroare către administratori", e);
        }
    }

    /**
     * Metodă manuală pentru testarea procesării (poate fi apelată prin endpoint de admin)
     */
    public Map<String, Object> manualWeeklyProcessing() {
        logger.info("=== PROCESARE MANUALĂ DECLANȘATĂ ===");

        try {
            var response = bookingController.generateBookings();

            if (response.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

                if (responseBody != null) {
                    logPaymentStatistics(responseBody);
                    return responseBody;
                }
            }

            // Dacă ajungem aici, ceva nu a mers bine
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Eroare la procesarea manuală");
            return errorResponse;

        } catch (Exception e) {
            logger.error("Eroare la procesarea manuală", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Eroare: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Obține statistici despre profilurile cu plăți automate
     */
    public Map<String, Object> getAutoPaymentStatistics() {
        try {
            List<ReservationProfile> allProfiles = (List<ReservationProfile>) reservationProfileRepository.findAll();

            long totalProfiles = allProfiles.size();
            long autoPaymentEnabled = allProfiles.stream()
                    .filter(ReservationProfile::getAutoPaymentEnabled)
                    .count();

            long validAutoPaymentProfiles = allProfiles.stream()
                    .filter(ReservationProfile::isValidForAutoPayment)
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProfiles", totalProfiles);
            stats.put("autoPaymentEnabled", autoPaymentEnabled);
            stats.put("validAutoPaymentProfiles", validAutoPaymentProfiles);
            stats.put("autoPaymentPercentage", totalProfiles > 0 ?
                    (double) autoPaymentEnabled / totalProfiles * 100 : 0.0);

            return stats;

        } catch (Exception e) {
            logger.error("Eroare la obținerea statisticilor plăților automate", e);
            return Map.of("error", e.getMessage());
        }
    }
}