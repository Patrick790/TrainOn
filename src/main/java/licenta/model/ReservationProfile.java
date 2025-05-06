package licenta.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    }

    public ReservationProfile(String name, String ageCategory, String timeInterval, BigDecimal weeklyBudget,
                              String city, User user, List<SportsHall> selectedHalls) {
        this.name = name;
        this.ageCategory = ageCategory;
        this.timeInterval = timeInterval;
        this.weeklyBudget = weeklyBudget;
        this.city = city;
        this.user = user;
        this.selectedHalls = selectedHalls != null ? selectedHalls : new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public void addSportsHall(SportsHall sportsHall) {
        this.selectedHalls.add(sportsHall);
    }

    public void removeSportsHall(SportsHall sportsHall) {
        this.selectedHalls.remove(sportsHall);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgeCategory() {
        return ageCategory;
    }

    public void setAgeCategory(String ageCategory) {
        this.ageCategory = ageCategory;
    }

    public String getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
    }

    public BigDecimal getWeeklyBudget() {
        return weeklyBudget;
    }

    public void setWeeklyBudget(BigDecimal weeklyBudget) {
        this.weeklyBudget = weeklyBudget;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SportsHall> getSelectedHalls() {
        return selectedHalls;
    }

    public void setSelectedHalls(List<SportsHall> selectedHalls) {
        this.selectedHalls = selectedHalls;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
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
                ", user=" + (user != null ? user.getId() : null) +
                ", selectedHalls=" + selectedHalls.size() +
                '}';
    }

}
