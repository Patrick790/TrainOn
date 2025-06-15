package licenta.service;

import licenta.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class pentru trimiterea email-urilor de rezervare
 */
public class ReservationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationEmailService.class);

    /**
     * Trimite email de confirmare pentru rezervări
     */
    public static void sendReservationConfirmationEmail(
            JavaMailSender mailSender,
            String senderEmail,
            User user,
            List<Reservation> reservations,
            Payment payment) {

        try {
            if (mailSender == null || senderEmail == null || user == null ||
                    user.getEmail() == null || reservations == null || reservations.isEmpty()) {
                logger.warn("Date incomplete pentru trimiterea email-ului de confirmare rezervare");
                return;
            }

            String subject = "Confirmare rezervare TrainOn - " + reservations.size() + " interval" +
                    (reservations.size() > 1 ? "e" : "") + " rezervat" +
                    (reservations.size() > 1 ? "e" : "");

            String emailContent = buildReservationEmailContent(user, reservations, payment);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(emailContent);

            mailSender.send(message);
            logger.info("Email de confirmare rezervare trimis cu succes către: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Eroare la trimiterea email-ului de confirmare rezervare pentru utilizatorul {}: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Construiește conținutul email-ului de confirmare
     */
    private static String buildReservationEmailContent(User user, List<Reservation> reservations, Payment payment) {
        StringBuilder content = new StringBuilder();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("ro", "RO"));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'la' HH:mm", new Locale("ro", "RO"));

        // Header elegant
        content.append("================================================================================\n");
        content.append("                                  TRAINON\n");
        content.append("                           PLATFORMĂ REZERVĂRI SPORTIVE\n");
        content.append("                             CONFIRMARE REZERVARE\n");
        content.append("================================================================================\n\n");

        content.append("Stimate/Stimată ").append(user.getName()).append(",\n\n");
        content.append("Vă confirmăm cu plăcere că rezervarea dumneavoastră a fost înregistrată cu succes!\n");
        content.append("Vă mulțumim că ați ales TrainOn pentru nevoile dumneavoastră sportive.\n\n");

        // Separator pentru detalii rezervare
        content.append("================================================================================\n");
        content.append("                               DETALII REZERVARE\n");
        content.append("================================================================================\n\n");

        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);

            if (reservations.size() > 1) {
                content.append("REZERVAREA ").append(i + 1).append(" din ").append(reservations.size()).append(":\n");
                content.append("--------------------------------------------------------------------------------\n");
            }

            content.append("Sala de sport:      ").append(reservation.getHall().getName()).append("\n");
            content.append("Adresa:             ").append(reservation.getHall().getAddress()).append("\n");
            content.append("Data rezervării:    ").append(dateFormatter.format(reservation.getDate())).append("\n");
            content.append("Intervalul orar:    ").append(reservation.getTimeSlot()).append("\n");
            content.append("Tariful:            ").append(String.format("%.2f RON", reservation.getPrice())).append("\n");

            if (reservation.getHall().getDescription() != null && !reservation.getHall().getDescription().trim().isEmpty()) {
                content.append("Facilități:         ").append(reservation.getHall().getDescription()).append("\n");
            }

            if (reservation.getHall().getPhoneNumber() != null) {
                content.append("Contact sala:       ").append(reservation.getHall().getPhoneNumber()).append("\n");
            }

            if (i < reservations.size() - 1) {
                content.append("\n");
            }
        }
        content.append("\n");

        // Detalii plată
        if (payment != null) {
            content.append("================================================================================\n");
            content.append("                               DETALII PLATĂ\n");
            content.append("================================================================================\n\n");

            content.append("ID Tranzacție:      #").append(payment.getId()).append("\n");
            content.append("Suma totală:        ").append(String.format("%.2f %s", payment.getTotalAmount(), payment.getCurrency())).append("\n");
            content.append("Metodă de plată:    ").append(getPaymentMethodDisplayName(payment.getPaymentMethod())).append("\n");
            content.append("Status plată:       ").append(getPaymentStatusDisplayName(payment.getStatus())).append("\n");

            if (payment.getPaymentDate() != null) {
                content.append("Data procesării:    ").append(payment.getPaymentDate().format(timeFormatter)).append("\n");
            }
            content.append("\n");
        }

        // Instrucțiuni importante
        content.append("================================================================================\n");
        content.append("                          INSTRUCȚIUNI IMPORTANTE\n");
        content.append("================================================================================\n\n");

        if (payment != null && payment.getPaymentMethod() == PaymentMethod.CASH) {
            content.append("PLATĂ LA FAȚA LOCULUI:\n");
            content.append("• Plata se va efectua direct la sala de sport înainte de începerea activității\n");
            content.append("• Vă recomandăm să ajungeți cu 15 minute înainte de ora rezervată\n");
            content.append("• Aveți la dumneavoastră suma exactă pentru a facilita procesul\n\n");
        } else {
            content.append("PLATĂ PROCESATĂ:\n");
            content.append("• Plata a fost confirmată cu succes în sistemul nostru\n");
            content.append("• Vă recomandăm să ajungeți cu 10 minute înainte de ora rezervată\n\n");
        }

        content.append("PREGĂTIRE PENTRU ACTIVITATE:\n");
        content.append("• Aduceți echipamentul sportiv necesar (încălțăminte, îmbrăcăminte adecvată)\n");
        content.append("• Este recomandat să aveți la dumneavoastră o sticlă cu apă\n");
        content.append("• Verificați condițiile meteorologice dacă activitatea este în aer liber\n\n");

        content.append("PUNCTUALITATE:\n");
        content.append("• În caz de întârziere, vă rugăm să contactați urgent sala de sport\n");
        content.append("• Rezervările au o toleranță de maximum 15 minute\n");
        content.append("• Respectarea programului este esențială pentru buna funcționare\n\n");

        // Politica de anulare
        content.append("================================================================================\n");
        content.append("                            POLITICA DE ANULARE\n");
        content.append("================================================================================\n\n");

        content.append("CONDIȚII DE ANULARE:\n");
        content.append("• Rezervările pot fi anulate cu minimum 2 ore înainte de ora programată\n");
        content.append("• Anulările se realizează prin contul dumneavoastră de pe platforma TrainOn\n");
        content.append("• Pentru plățile online, rambursarea se procesează în 3-5 zile lucrătoare\n");
        content.append("• Anulările tardive pot fi supuse unor penalizări conform termenilor și condițiilor\n\n");

        // Contact și suport
        content.append("================================================================================\n");
        content.append("                          CONTACT ȘI ASISTENȚĂ\n");
        content.append("================================================================================\n\n");

        content.append("Pentru orice întrebări sau asistență tehnică:\n");
        content.append("• Accesați contul dumneavoastră pe platforma TrainOn\n");
        content.append("• Utilizați sistemul de mesagerie integrat\n");
        content.append("• Programul de asistență: Luni - Duminică, 08:00 - 22:00\n");
        content.append("• Echipa noastră răspunde în cel mai scurt timp posibil\n\n");

        // Footer elegant
        content.append("================================================================================\n");
        content.append("                           VĂ MULȚUMIM PENTRU ÎNCREDERE!\n");
        content.append("                                                                                \n");
        content.append("               Vă dorim o experiență sportivă plăcută și reușită!             \n");
        content.append("                                                                                \n");
        content.append("                         Cu respect, Echipa TrainOn                           \n");
        content.append("================================================================================\n\n");

        content.append("--------------------------------------------------------------------------------\n");
        content.append("Acest email a fost generat automat de sistemul TrainOn.\n");
        content.append("Vă rugăm să nu răspundeți direct la acest mesaj.\n");
        content.append("Pentru comunicare, utilizați platforma online TrainOn.\n");
        content.append("--------------------------------------------------------------------------------");

        return content.toString();
    }

    /**
     * Trimite email pentru anularea rezervării
     */
    public static void sendReservationCancellationEmail(
            JavaMailSender mailSender,
            String senderEmail,
            User user,
            Reservation reservation) {

        try {
            if (mailSender == null || senderEmail == null || user == null ||
                    user.getEmail() == null || reservation == null) {
                logger.warn("Date incomplete pentru trimiterea email-ului de anulare rezervare");
                return;
            }

            SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("ro", "RO"));

            String subject = "Anulare rezervare TrainOn - " + reservation.getHall().getName();

            StringBuilder content = new StringBuilder();

            content.append("================================================================================\n");
            content.append("                                  TRAINON\n");
            content.append("                           PLATFORMĂ REZERVĂRI SPORTIVE\n");
            content.append("                              ANULARE REZERVARE\n");
            content.append("================================================================================\n\n");

            content.append("Stimate/Stimată ").append(user.getName()).append(",\n\n");
            content.append("Vă confirmăm că rezervarea dumneavoastră a fost anulată cu succes.\n");
            content.append("Înțelegem că planurile se pot schimba și sperăm să vă revedem curând pe TrainOn.\n\n");

            content.append("================================================================================\n");
            content.append("                           REZERVARE ANULATĂ\n");
            content.append("================================================================================\n\n");

            content.append("Sala de sport:      ").append(reservation.getHall().getName()).append("\n");
            content.append("Adresa:             ").append(reservation.getHall().getAddress()).append("\n");
            content.append("Data rezervării:    ").append(dateFormatter.format(reservation.getDate())).append("\n");
            content.append("Intervalul orar:    ").append(reservation.getTimeSlot()).append("\n");
            if (reservation.getPrice() != null) {
                content.append("Suma rezervării:    ").append(String.format("%.2f RON", reservation.getPrice())).append("\n");
            }
            content.append("\n");

            if (reservation.getPrice() != null && reservation.getPrice() > 0) {
                content.append("================================================================================\n");
                content.append("                         INFORMAȚII RAMBURSARE\n");
                content.append("================================================================================\n\n");

                content.append("PROCESAREA RAMBURSĂRII:\n");
                content.append("• Rambursarea va fi procesată în termen de 3-5 zile lucrătoare\n");
                content.append("• Suma va fi creditată pe metoda de plată utilizată inițial\n");
                content.append("• Veți primi o notificare când rambursarea este finalizată\n");
                content.append("• Pentru întrebări despre rambursare, contactați-ne prin platformă\n\n");
            }

            // Informații utile
            content.append("================================================================================\n");
            content.append("                            INFORMAȚII UTILE\n");
            content.append("================================================================================\n\n");

            content.append("REZERVĂRI VIITOARE:\n");
            content.append("• Puteți efectua o nouă rezervare oricând prin platforma TrainOn\n");
            content.append("• Verificați disponibilitatea pentru alte date și intervale orare\n");
            content.append("• Explorați ofertele speciale și promoțiile disponibile\n\n");

            content.append("CONTACT ȘI ASISTENȚĂ:\n");
            content.append("• Pentru întrebări, accesați contul dumneavoastră pe TrainOn\n");
            content.append("• Echipa noastră de suport este disponibilă zilnic între 08:00 - 22:00\n");
            content.append("• Răspundem prompt la toate solicitările prin platforma online\n\n");

            content.append("================================================================================\n");
            content.append("                        NE PARE RĂU PENTRU INCONVENIENT!\n");
            content.append("                                                                                \n");
            content.append("              Sperăm să vă revedem curând pentru o nouă rezervare!            \n");
            content.append("                                                                                \n");
            content.append("                         Cu respect, Echipa TrainOn                           \n");
            content.append("================================================================================\n\n");

            content.append("--------------------------------------------------------------------------------\n");
            content.append("Acest email a fost generat automat de sistemul TrainOn.\n");
            content.append("Pentru comunicare, utilizați platforma online TrainOn.\n");
            content.append("--------------------------------------------------------------------------------");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(content.toString());

            mailSender.send(message);
            logger.info("Email de anulare rezervare trimis cu succes către: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Eroare la trimiterea email-ului de anulare rezervare pentru utilizatorul {}: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Metodă helper pentru a obține numele afișat al metodei de plată
     */
    private static String getPaymentMethodDisplayName(PaymentMethod method) {
        if (method == null) return "Necunoscut";
        return method.getDisplayName();
    }

    /**
     * Metodă helper pentru a obține numele afișat al statusului plății
     */
    private static String getPaymentStatusDisplayName(PaymentStatus status) {
        if (status == null) return "Necunoscut";
        return status.getDisplayName();
    }
}