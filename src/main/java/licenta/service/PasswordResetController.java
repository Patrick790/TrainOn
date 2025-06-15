package licenta.service;

import licenta.jwtservice.JwtService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class PasswordResetController {

    private final IUserSpringRepository userRepository;
    private final IEmailRepository emailRepository;
    private final IEmailRecipientRepository emailRecipientRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private final Map<String, TokenData> resetTokens = new HashMap<>();

    @Autowired
    public PasswordResetController(IUserSpringRepository userRepository,
                                   IEmailRepository emailRepository,
                                   IEmailRecipientRepository emailRecipientRepository,
                                   JavaMailSender mailSender,
                                   PasswordEncoder passwordEncoder,
                                   JwtService jwtService) {
        this.userRepository = userRepository;
        this.emailRepository = emailRepository;
        this.emailRecipientRepository = emailRecipientRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Endpoint pentru cererea de resetare a parolei
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            // Validează email-ul
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Email-ul este obligatoriu"));
            }

            // Verifică dacă utilizatorul există
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("message", "Nu există niciun cont cu această adresă de email"));
            }

            User user = userOptional.get();

            if ("suspended".equals(user.getAccountStatus())) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Contul este suspendat și nu poate fi resetat"));
            }

            if ("rejected".equals(user.getAccountStatus())) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Contul a fost respins și nu poate fi resetat"));
            }

            String resetToken = generateSecureToken();

            // Stocheaza token-ul cu timestamp (expira in 1 ora)
            TokenData tokenData = new TokenData(user.getEmail(), System.currentTimeMillis() + 3600000); // 1 oră
            resetTokens.put(resetToken, tokenData);

            // Creeaza URL-ul de resetare
            String resetUrl = "http://localhost:3000/reset-password?token=" + resetToken;

            String emailContent = String.format(
                    "Salut %s,\n\n" +
                            "Ai solicitat resetarea parolei pentru contul tău de pe platforma TrainOn.\n\n" +
                            "Pentru a-ți reseta parola, te rugăm să accesezi următorul link:\n" +
                            "%s\n\n" +
                            "IMPORTANT:\n" +
                            "• Acest link va expira în 1 oră din momentul primirii acestui email\n" +
                            "• Dacă nu ai solicitat resetarea parolei, te rugăm să ignori acest email\n" +
                            "• Pentru securitatea contului tău, nu împărtăși acest link cu nimeni\n\n" +
                            "După accesarea link-ului, vei putea să introduci o parolă nouă și să confirmi modificarea.\n\n" +
                            "Dacă întâmpini probleme, te rugăm să ne contactezi.\n\n" +
                            "Cu stimă,\n" +
                            "Echipa TrainOn\n\n" +
                            "---\n" +
                            "Acest email a fost generat automat. Te rugăm să nu răspunzi la acest mesaj.",
                    user.getName(),
                    resetUrl
            );

            // Trimite email-ul
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(user.getEmail());
                message.setSubject("Resetare parolă - TrainOn");
                message.setText(emailContent);
                mailSender.send(message);
            } catch (Exception emailException) {
                return ResponseEntity.status(500)
                        .body(Map.of("message", "Eroare la trimiterea email-ului. Te rugăm să încerci din nou."));
            }

            // Salveaza email-ul in baza de date pentru istoric
            try {
                Email emailEntity = new Email();
                emailEntity.setSubject("Resetare parolă - TrainOn");
                emailEntity.setContent(emailContent);
                emailEntity.setSender(senderEmail);
                emailEntity.setRecipientType("specific_user");
                emailEntity.setCreatedBy("system");
                emailEntity.setStatus("SENT");

                Email savedEmail = emailRepository.save(emailEntity);

                EmailRecipient recipient = new EmailRecipient(user.getEmail());
                recipient.setStatus("SENT");
                savedEmail.addRecipient(recipient);
                emailRecipientRepository.save(recipient);
            } catch (Exception dbException) {
                System.err.println("Eroare la salvarea email-ului în BD: " + dbException.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Email-ul de resetare a fost trimis cu succes",
                    "email", user.getEmail()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Eroare internă. Te rugăm să încerci din nou."));
        }
    }

    /**
     * Endpoint pentru validarea token-ului de resetare
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        try {
            TokenData tokenData = resetTokens.get(token);

            if (tokenData == null) {
                return ResponseEntity.status(400)
                        .body(Map.of("message", "Link-ul de resetare este invalid", "valid", false));
            }

            if (System.currentTimeMillis() > tokenData.getExpirationTime()) {
                resetTokens.remove(token);
                return ResponseEntity.status(400)
                        .body(Map.of("message", "Link-ul de resetare a expirat. Te rugăm să soliciți din nou resetarea parolei.", "valid", false));
            }

            Optional<User> userOptional = userRepository.findByEmail(tokenData.getEmail());
            if (userOptional.isEmpty()) {
                resetTokens.remove(token);
                return ResponseEntity.status(400)
                        .body(Map.of("message", "Contul asociat nu mai există", "valid", false));
            }

            User user = userOptional.get();

            if ("suspended".equals(user.getAccountStatus()) || "rejected".equals(user.getAccountStatus())) {
                resetTokens.remove(token);
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Contul nu poate fi resetat din cauza statusului său", "valid", false));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Link valid",
                    "valid", true,
                    "email", tokenData.getEmail(),
                    "userName", user.getName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Eroare la validarea link-ului", "valid", false));
        }
    }

    /**
     * Endpoint pentru resetarea efectivă a parolei
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // Validează cererea
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Token-ul de resetare este obligatoriu"));
            }

            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Parola nouă este obligatorie"));
            }

            if (request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Parola nouă trebuie să aibă cel puțin 6 caractere"));
            }

            if (request.getNewPassword().length() > 128) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Parola nouă este prea lungă (maxim 128 caractere)"));
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Parolele nu se potrivesc"));
            }

            // Verifică token-ul
            TokenData tokenData = resetTokens.get(request.getToken());
            if (tokenData == null) {
                return ResponseEntity.status(400)
                        .body(Map.of("message", "Link-ul de resetare este invalid"));
            }

            // Verifică dacă token-ul a expirat
            if (System.currentTimeMillis() > tokenData.getExpirationTime()) {
                resetTokens.remove(request.getToken());
                return ResponseEntity.status(400)
                        .body(Map.of("message", "Link-ul de resetare a expirat"));
            }

            // Găsește utilizatorul
            Optional<User> userOptional = userRepository.findByEmail(tokenData.getEmail());
            if (userOptional.isEmpty()) {
                resetTokens.remove(request.getToken());
                return ResponseEntity.status(400)
                        .body(Map.of("message", "Utilizatorul nu mai există"));
            }

            User user = userOptional.get();

            if ("suspended".equals(user.getAccountStatus()) || "rejected".equals(user.getAccountStatus())) {
                resetTokens.remove(request.getToken());
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Contul nu poate fi resetat din cauza statusului său"));
            }

            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Parola nouă trebuie să fie diferită de cea curentă"));
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            resetTokens.remove(request.getToken());

            System.out.println("Password reset successful for user: " + user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Parola a fost resetată cu succes! Poți să te autentifici acum cu noua parolă."
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Eroare la resetarea parolei. Te rugăm să încerci din nou."));
        }
    }

    /**
     * Generează un token sigur pentru resetarea parolei
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String baseToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        String timestamp = String.valueOf(System.currentTimeMillis());
        return baseToken + "_" + timestamp.substring(timestamp.length() - 6);
    }

    /**
     * Clasa pentru cererea de forgot password
     */
    public static class ForgotPasswordRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * Clasa pentru cererea de resetare a parolei
     */
    public static class ResetPasswordRequest {
        private String token;
        private String newPassword;
        private String confirmPassword;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }

    /**
     * Clasa pentru stocarea datelor token-ului
     */
    private static class TokenData {
        private final String email;
        private final long expirationTime;

        public TokenData(String email, long expirationTime) {
            this.email = email;
            this.expirationTime = expirationTime;
        }

        public String getEmail() {
            return email;
        }

        public long getExpirationTime() {
            return expirationTime;
        }
    }
}