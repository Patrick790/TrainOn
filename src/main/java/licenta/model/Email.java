package licenta.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "emails")
public class Email extends BruteEntity<Long> {

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sender", nullable = false)
    private String sender;

    @Column(name = "recipient_type", nullable = false)
    private String recipientType;  // all_users, all_admins, specific_user

    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "status", nullable = false)
    private String status;  // SENT, FAILED

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailRecipient> recipients = new ArrayList<>();

    public Email() {
        this.createdAt = new Date();
        this.status = "SENT";
    }

    public Email(String subject, String content, String sender, String recipientType, String createdBy) {
        this.subject = subject;
        this.content = content;
        this.sender = sender;
        this.recipientType = recipientType;
        this.createdBy = createdBy;
        this.createdAt = new Date();
        this.status = "SENT";
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

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<EmailRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<EmailRecipient> recipients) {
        this.recipients = recipients;
    }

    public void addRecipient(EmailRecipient recipient) {
        recipients.add(recipient);
        recipient.setEmail(this);
    }

    public void removeRecipient(EmailRecipient recipient) {
        recipients.remove(recipient);
        recipient.setEmail(null);
    }

    @Override
    public String toString() {
        return "Email{" +
                "id=" + getId() +
                ", subject='" + subject + '\'' +
                ", recipientType='" + recipientType + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}