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
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "date")
    private Date date;

    @Column(name = "price")
    private Float price;

    public Reservation() {
    }

    public Reservation(SportsHall hall, Team team, Date date, Float price) {
        this.hall = hall;
        this.team = team;
        this.date = date;
        this.price = price;
    }

    public SportsHall getHall() {
        return hall;
    }

    public void setHall(SportsHall hall) {
        this.hall = hall;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
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


}
