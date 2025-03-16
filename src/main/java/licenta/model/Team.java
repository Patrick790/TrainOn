package licenta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team extends BruteEntity<Long> {

    @Column(name = "name")
    private String name;

    @Column(name = "sport")
    private String sport;

    @Column(name = "type")
    private String type;

    @Column(name = "coach")
    private String coach;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    public Team() {
    }

    public Team(String name, String sport, String type, String coach, String email, String address) {
        this.name = name;
        this.sport = sport;
        this.type = type;
        this.coach = coach;
        this.email = email;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", sport='" + sport + '\'' +
                ", type='" + type + '\'' +
                ", coach='" + coach + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
