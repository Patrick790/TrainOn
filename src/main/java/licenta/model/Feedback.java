package licenta.model;


import jakarta.persistence.*;

@Entity
@Table(name = "feedbacks")
public class Feedback extends BruteEntity<Long> {

    @ManyToOne
    @JoinColumn(name = "hall_id")
    private SportsHall hall;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment")
    private String comment;

    public Feedback() {
    }

    public Feedback(SportsHall hall, Team team, Integer rating, String comment) {
        this.hall = hall;
        this.team = team;
        this.rating = rating;
        this.comment = comment;
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "id=" + getId() +
                ", hall=" + hall +
                ", team=" + team +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                '}';
    }
}
