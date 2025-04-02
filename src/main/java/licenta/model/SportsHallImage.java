package licenta.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.awt.*;

@Entity
@Table(name = "sports_hall_images")
public class SportsHallImage extends BruteEntity<Long> {

    @Column(name = "image", nullable = false)
    private byte[] image;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sports_hall_id")
    @JsonBackReference
    private SportsHall sportsHall;

    public SportsHallImage() {
    }

    public SportsHallImage(byte[] image, String description) {
        this.image = image;
        this.description = description;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImagePath(byte[] image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SportsHall getSportsHall() {
        return sportsHall;
    }

    public void setSportsHall(SportsHall sportsHall) {
        this.sportsHall = sportsHall;
    }
}
