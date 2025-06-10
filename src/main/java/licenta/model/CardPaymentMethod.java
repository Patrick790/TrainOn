package licenta.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_payment_methods")
public class CardPaymentMethod extends BruteEntity<Long> {

    @Column(name = "stripe_payment_method_id", nullable = false)
    private String stripePaymentMethodId;

    @Column(name = "card_last_four", length = 4, nullable = false)
    private String cardLastFour;

    @Column(name = "card_brand", length = 20, nullable = false)
    private String cardBrand; // visa, mastercard, etc.

    @Column(name = "card_exp_month", nullable = false)
    private Integer cardExpMonth;

    @Column(name = "card_exp_year", nullable = false)
    private Integer cardExpYear;

    @Column(name = "cardholder_name", nullable = false)
    private String cardholderName;

    @Column(name = "billing_country", length = 2)
    private String billingCountry;

    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // Constructors
    public CardPaymentMethod() {
        this.createdAt = LocalDateTime.now();
        this.isDefault = false;
        this.isActive = true;
    }

    public CardPaymentMethod(String stripePaymentMethodId, String cardLastFour, String cardBrand,
                             Integer cardExpMonth, Integer cardExpYear, String cardholderName, User user) {
        this();
        this.stripePaymentMethodId = stripePaymentMethodId;
        this.cardLastFour = cardLastFour;
        this.cardBrand = cardBrand;
        this.cardExpMonth = cardExpMonth;
        this.cardExpYear = cardExpYear;
        this.cardholderName = cardholderName;
        this.user = user;
    }

    // Helper methods
    public String getDisplayName() {
        return String.format("%s ****%s (%02d/%d)",
                capitalize(cardBrand), cardLastFour, cardExpMonth, cardExpYear);
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.getYear() > cardExpYear ||
                (now.getYear() == cardExpYear && now.getMonthValue() > cardExpMonth);
    }

    public void markAsDefault() {
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters și Setters
    public String getStripePaymentMethodId() { return stripePaymentMethodId; }
    public void setStripePaymentMethodId(String stripePaymentMethodId) { this.stripePaymentMethodId = stripePaymentMethodId; }

    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }

    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }

    public Integer getCardExpMonth() { return cardExpMonth; }
    public void setCardExpMonth(Integer cardExpMonth) { this.cardExpMonth = cardExpMonth; }

    public Integer getCardExpYear() { return cardExpYear; }
    public void setCardExpYear(Integer cardExpYear) { this.cardExpYear = cardExpYear; }

    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }

    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }

    public String getBillingPostalCode() { return billingPostalCode; }
    public void setBillingPostalCode(String billingPostalCode) { this.billingPostalCode = billingPostalCode; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Override
    public String toString() {
        return "CardPaymentMethod{" +
                "id=" + getId() +
                ", cardBrand='" + cardBrand + '\'' +
                ", cardLastFour='" + cardLastFour + '\'' +
                ", cardholderName='" + cardholderName + '\'' +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                '}';
    }
}