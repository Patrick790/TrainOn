package licenta.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "reservations")
public class Reservation extends BruteEntity<Long> {

    @ManyToOne
    @JoinColumn(name = "hall_id")
    private SportsHall hall;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "date")
    private Date date;

    @Column(name = "price", nullable = true)
    private Float price;

    @Column(name = "time_slot", nullable = false)
    private String timeSlot;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "status")
    private String status = "CONFIRMED";

    // NOU: Adăugăm câmpul created_at pentru a ști când s-a făcut rezervarea
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Reservation() {
    }

    public Reservation(SportsHall hall, User user, Date date, String timeSlot, Float price, String type) {
        this.hall = hall;
        this.user = user;
        this.date = date;
        this.timeSlot = timeSlot;
        this.price = price;
        this.type = type;
        this.status = "CONFIRMED";

        // Setează timpul curent în timezone-ul României
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Bucharest"));
        this.createdAt = cal.getTime();
    }

    // Metodă JPA care se execută înainte de persistarea entității
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            // Setează timpul curent în timezone-ul României
            java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Bucharest"));
            createdAt = cal.getTime();
        }
    }

    // Getteri și setteri existenți
    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SportsHall getHall() {
        return hall;
    }

    public void setHall(SportsHall hall) {
        this.hall = hall;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // NOU: Getter și setter pentru createdAt
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    // Metodele tale existente
    public boolean isMaintenance() {
        return "maintenance".equals(this.type);
    }

    public boolean isReservation() {
        return "reservation".equals(this.type);
    }
}