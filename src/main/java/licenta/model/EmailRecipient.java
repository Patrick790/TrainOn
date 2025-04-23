package licenta.model;

import jakarta.persistence.*;

@Entity
@Table(name = "email_recipients")
public class EmailRecipient extends BruteEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "status")
    private String status;  // SENT, DELIVERED, READ, FAILED

    public EmailRecipient() {
        this.status = "SENT";
    }

    public EmailRecipient(String recipientEmail) {
        this.recipientEmail = recipientEmail;
        this.status = "SENT";
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "EmailRecipient{" +
                "id=" + getId() +
                ", recipientEmail='" + recipientEmail + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}