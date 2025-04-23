package licenta.service;

import jakarta.transaction.Transactional;
import licenta.model.Email;
import licenta.model.EmailRecipient;
import licenta.model.User;
import licenta.persistence.IEmailRecipientRepository;
import licenta.persistence.IEmailRepository;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class EmailRestController {

    private final IEmailRepository emailRepository;
    private final IEmailRecipientRepository emailRecipientRepository;
    private final IUserSpringRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Autowired
    public EmailRestController(IEmailRepository emailRepository,
                           IEmailRecipientRepository emailRecipientRepository,
                           IUserSpringRepository userRepository,
                           JavaMailSender mailSender) {
        this.emailRepository = emailRepository;
        this.emailRecipientRepository = emailRecipientRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    /**
     * Obține statistici despre utilizatori pentru formularul de email
     */
    @GetMapping("/email-stats")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Map<String, Integer>> getEmailStats() {
        try {
            Iterable<User> allUsers = userRepository.findAll();
            List<User> usersList = StreamSupport.stream(allUsers.spliterator(), false).toList();

            int userCount = (int) usersList.stream()
                    .filter(user -> "user".equals(user.getUserType()))
                    .count();

            int adminCount = (int) usersList.stream()
                    .filter(user -> "admin".equals(user.getUserType()))
                    .count();

            Map<String, Integer> response = new HashMap<>();
            response.put("all_users", userCount);
            response.put("all_admins", adminCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Trimite email-uri către utilizatorii selectați
     */
    @PostMapping("/send-email")
    @PreAuthorize("hasAuthority('admin')")
    @Transactional
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequest emailRequest) {
        try {
            // Validăm cererea
            if (emailRequest.getSubject() == null || emailRequest.getSubject().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Subiectul email-ului este obligatoriu"));
            }

            if (emailRequest.getContent() == null || emailRequest.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Conținutul email-ului este obligatoriu"));
            }

            if ("specific_user".equals(emailRequest.getRecipientType()) &&
                    (emailRequest.getRecipient() == null || emailRequest.getRecipient().trim().isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Adresa de email a destinatarului este obligatorie"));
            }

            // Obține utilizatorul curent (administratorul)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            // Creează entitatea Email
            Email email = new Email();
            email.setSubject(emailRequest.getSubject());
            email.setContent(emailRequest.getContent());
            email.setSender(senderEmail);
            email.setRecipientType(emailRequest.getRecipientType());
            email.setCreatedBy(currentUserEmail);
            email.setStatus("SENT");

            // Determină destinatarii în funcție de tipul selectat
            List<String> recipientEmails = getRecipientEmails(
                    emailRequest.getRecipientType(),
                    emailRequest.getRecipient()
            );

            // Verifică dacă există destinatari
            if (recipientEmails.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nu există destinatari pentru acest email"));
            }

            // Salvează emailul în baza de date
            Email savedEmail = emailRepository.save(email);

            // Trimite email-urile și salvează destinatarii
            for (String recipientEmail : recipientEmails) {
                try {
                    // Trimite email
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(senderEmail);
                    message.setTo(recipientEmail);
                    message.setSubject(emailRequest.getSubject());
                    message.setText(emailRequest.getContent());
                    mailSender.send(message);

                    // Salvează destinatarul
                    EmailRecipient recipient = new EmailRecipient(recipientEmail);
                    recipient.setStatus("SENT");
                    email.addRecipient(recipient);
                    emailRecipientRepository.save(recipient);
                } catch (Exception e) {
                    // În caz de eroare, marchează statusul ca FAILED
                    EmailRecipient recipient = new EmailRecipient(recipientEmail);
                    recipient.setStatus("FAILED");
                    email.addRecipient(recipient);
                    emailRecipientRepository.save(recipient);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Email-urile au fost trimise cu succes");
            response.put("recipientCount", recipientEmails.size());
            response.put("emailId", savedEmail.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "A apărut o eroare la trimiterea email-urilor: " + e.getMessage()));
        }
    }

    /**
     * Determină lista de email-uri ale destinatarilor în funcție de tipul selectat
     */
    private List<String> getRecipientEmails(String recipientType, String specificRecipient) {
        return switch (recipientType) {
            case "all_users" -> getEmailsByUserType("user");
            case "all_admins" -> getEmailsByUserType("admin");
            case "specific_user" -> {
                if (specificRecipient != null && !specificRecipient.isEmpty()) {
                    yield List.of(specificRecipient);
                }
                yield new ArrayList<>();
            }
            default -> new ArrayList<>();
        };
    }

    /**
     * Obține email-urile utilizatorilor de un anumit tip
     */
    private List<String> getEmailsByUserType(String userType) {
        Iterable<User> allUsers = userRepository.findAll();

        return StreamSupport.stream(allUsers.spliterator(), false)
                .filter(user -> userType.equals(user.getUserType()))
                .map(User::getEmail)
                .collect(Collectors.toList());
    }

    /**
     * Obține toate email-urile trimise, ordonate după data creării (descendent)
     */
    @GetMapping("/emails")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<List<Email>> getAllEmails() {
        try {
            List<Email> emails = emailRepository.findAllByOrderByCreatedAtDesc();
            return ResponseEntity.ok(emails);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obține un email după ID, inclusiv destinatarii săi
     */
    @GetMapping("/emails/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> getEmailById(@PathVariable Long id) {
        try {
            Email email = emailRepository.findById(id).orElse(null);
            if (email == null) {
                return ResponseEntity.notFound().build();
            }

            List<EmailRecipient> recipients = emailRecipientRepository.findByEmailId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("recipients", recipients);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Clasa pentru cererea de trimitere a email-urilor
     */
    public static class EmailRequest {
        private String recipientType; // all_users, all_admins, specific_user
        private String recipient; // email-ul utilizatorului specific (folosit doar când recipientType este specific_user)
        private String subject;
        private String content;

        public String getRecipientType() {
            return recipientType;
        }

        public void setRecipientType(String recipientType) {
            this.recipientType = recipientType;
        }

        public String getRecipient() {
            return recipient;
        }

        public void setRecipient(String recipient) {
            this.recipient = recipient;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}