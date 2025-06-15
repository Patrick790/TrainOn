package licenta.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "reservation_profiles")
public class ReservationProfile extends BruteEntity<Long> {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "age_category", nullable = false)
    private String ageCategory;

    @Column(name = "time_interval", nullable = false)
    private String timeInterval;

    @Column(name = "weekly_budget", nullable = false, precision = 10, scale = 2)
    private BigDecimal weeklyBudget;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "sport", nullable = false)
    private String sport;

    // Suport pentru metoda de plata automata
    @Enumerated(EnumType.STRING)
    @Column(name = "auto_payment_method")
    private PaymentMethod autoPaymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_card_payment_method_id")
    @JsonIgnoreProperties({"user", "hibernateLazyInitializer", "handler"})
    private CardPaymentMethod defaultCardPaymentMethod;

    @Column(name = "auto_payment_enabled", nullable = false)
    private Boolean autoPaymentEnabled;

    @Column(name = "auto_payment_threshold", precision = 10, scale = 2)
    private BigDecimal autoPaymentThreshold;

    @Column(name = "max_weekly_auto_payment", precision = 10, scale = 2)
    private BigDecimal maxWeeklyAutoPayment;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-profiles")
    private User user;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @ManyToMany
    @JoinTable(
            name = "profile_halls",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "hall_id")
    )
    @JsonIgnoreProperties("reservationProfiles")
    private List<SportsHall> selectedHalls = new ArrayList<>();


    public ReservationProfile() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.autoPaymentEnabled = Boolean.FALSE;
    }

    public ReservationProfile(String name, String ageCategory, String timeInterval, BigDecimal weeklyBudget,
                              String city, String sport, User user, List<SportsHall> selectedHalls) {
        this(); // Apelează constructorul implicit
        this.name = name;
        this.ageCategory = ageCategory;
        this.timeInterval = timeInterval;
        this.weeklyBudget = weeklyBudget;
        this.city = city;
        this.sport = sport;
        this.user = user;
        this.selectedHalls = selectedHalls != null ? selectedHalls : new ArrayList<>();
    }

    public ReservationProfile(String name, String ageCategory, String timeInterval, BigDecimal weeklyBudget,
                              String city, String sport, User user, List<SportsHall> selectedHalls,
                              Boolean autoPaymentEnabled, PaymentMethod autoPaymentMethod,
                              CardPaymentMethod defaultCardPaymentMethod, BigDecimal autoPaymentThreshold,
                              BigDecimal maxWeeklyAutoPayment) {
        this(name, ageCategory, timeInterval, weeklyBudget, city, sport, user, selectedHalls);
        this.autoPaymentEnabled = autoPaymentEnabled != null ? autoPaymentEnabled : Boolean.FALSE;
        this.autoPaymentMethod = autoPaymentMethod;
        this.defaultCardPaymentMethod = defaultCardPaymentMethod;
        this.autoPaymentThreshold = autoPaymentThreshold;
        this.maxWeeklyAutoPayment = maxWeeklyAutoPayment;
    }

    public boolean canProcessAutoPayment(BigDecimal amount) {
        // Verificări de securitate
        if (autoPaymentEnabled == null || !autoPaymentEnabled) {
            return false;
        }

        if (defaultCardPaymentMethod == null ||
                defaultCardPaymentMethod.getIsActive() == null ||
                !defaultCardPaymentMethod.getIsActive()) {
            return false;
        }

        if (defaultCardPaymentMethod.isExpired()) {
            return false;
        }

        if (autoPaymentThreshold != null && amount.compareTo(autoPaymentThreshold) < 0) {
            return false;
        }

        if (maxWeeklyAutoPayment != null && amount.compareTo(maxWeeklyAutoPayment) > 0) {
            return false;
        }

        return true;
    }

    public void enableAutoPayment(CardPaymentMethod cardPaymentMethod) {
        this.autoPaymentEnabled = Boolean.TRUE;
        this.defaultCardPaymentMethod = cardPaymentMethod;
        this.autoPaymentMethod = PaymentMethod.CARD;
        this.updatedAt = new Date();
    }

    public void disableAutoPayment() {
        this.autoPaymentEnabled = Boolean.FALSE;
        this.defaultCardPaymentMethod = null;
        this.autoPaymentMethod = null;
        this.updatedAt = new Date();
    }

    public void updateAutoPaymentSettings(BigDecimal threshold, BigDecimal maxWeekly) {
        this.autoPaymentThreshold = threshold;
        this.maxWeeklyAutoPayment = maxWeekly;
        this.updatedAt = new Date();
    }

    public void addSportsHall(SportsHall sportsHall) {
        if (sportsHall != null) {
            this.selectedHalls.add(sportsHall);
        }
    }

    public void removeSportsHall(SportsHall sportsHall) {
        if (sportsHall != null) {
            this.selectedHalls.remove(sportsHall);
        }
    }

    public boolean isValidForAutoPayment() {
        return autoPaymentEnabled != null && autoPaymentEnabled &&
                autoPaymentMethod != null &&
                defaultCardPaymentMethod != null &&
                defaultCardPaymentMethod.getIsActive() != null &&
                defaultCardPaymentMethod.getIsActive() &&
                !defaultCardPaymentMethod.isExpired();
    }

    public String getAutoPaymentStatusDescription() {
        if (autoPaymentEnabled == null || !autoPaymentEnabled) {
            return "Plata automată dezactivată";
        }

        if (defaultCardPaymentMethod == null) {
            return "Nu există card asociat";
        }

        if (defaultCardPaymentMethod.getIsActive() == null || !defaultCardPaymentMethod.getIsActive()) {
            return "Cardul este inactiv";
        }

        if (defaultCardPaymentMethod.isExpired()) {
            return "Cardul a expirat";
        }

        return "Plata automată activă";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAgeCategory() { return ageCategory; }
    public void setAgeCategory(String ageCategory) { this.ageCategory = ageCategory; }

    public String getTimeInterval() { return timeInterval; }
    public void setTimeInterval(String timeInterval) { this.timeInterval = timeInterval; }

    public BigDecimal getWeeklyBudget() { return weeklyBudget; }
    public void setWeeklyBudget(BigDecimal weeklyBudget) { this.weeklyBudget = weeklyBudget; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public PaymentMethod getAutoPaymentMethod() { return autoPaymentMethod; }
    public void setAutoPaymentMethod(PaymentMethod autoPaymentMethod) { this.autoPaymentMethod = autoPaymentMethod; }

    public CardPaymentMethod getDefaultCardPaymentMethod() { return defaultCardPaymentMethod; }
    public void setDefaultCardPaymentMethod(CardPaymentMethod defaultCardPaymentMethod) {
        this.defaultCardPaymentMethod = defaultCardPaymentMethod;
    }

    public Boolean getAutoPaymentEnabled() { return autoPaymentEnabled; }
    public void setAutoPaymentEnabled(Boolean autoPaymentEnabled) {
        this.autoPaymentEnabled = autoPaymentEnabled != null ? autoPaymentEnabled : Boolean.FALSE;
    }

    public BigDecimal getAutoPaymentThreshold() { return autoPaymentThreshold; }
    public void setAutoPaymentThreshold(BigDecimal autoPaymentThreshold) {
        this.autoPaymentThreshold = autoPaymentThreshold;
    }

    public BigDecimal getMaxWeeklyAutoPayment() { return maxWeeklyAutoPayment; }
    public void setMaxWeeklyAutoPayment(BigDecimal maxWeeklyAutoPayment) {
        this.maxWeeklyAutoPayment = maxWeeklyAutoPayment;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public List<SportsHall> getSelectedHalls() { return selectedHalls; }
    public void setSelectedHalls(List<SportsHall> selectedHalls) {
        this.selectedHalls = selectedHalls != null ? selectedHalls : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ReservationProfile{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", ageCategory='" + ageCategory + '\'' +
                ", timeInterval='" + timeInterval + '\'' +
                ", weeklyBudget=" + weeklyBudget +
                ", city='" + city + '\'' +
                ", sport='" + sport + '\'' +
                ", autoPaymentEnabled=" + autoPaymentEnabled +
                ", autoPaymentMethod=" + autoPaymentMethod +
                ", hasDefaultCard=" + (defaultCardPaymentMethod != null) +
                ", user=" + (user != null ? user.getId() : null) +
                ", selectedHalls=" + (selectedHalls != null ? selectedHalls.size() : 0) +
                '}';
    }
}