package licenta.model;


import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "feedbacks")
public class Feedback extends BruteEntity<Long> {

    @ManyToOne
    @JoinColumn(name = "hall_id")
    private SportsHall hall;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment")
    private String comment;

    @Column(name = "date")
    private Date date;

    public Feedback() {
    }

    public Feedback(SportsHall hall, User user, Integer rating, String comment, Date date) {
        this.hall = hall;
        this.user = user;
        this.rating = rating;
        this.comment = comment;
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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
                ", user=" + user +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                '}';
    }
}
