package licenta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@Table(name = "users")
public class User extends BruteEntity<Long> {
    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "user_type")
    private String userType;

    @Column(name = "address")
    private String address;

    @Column(name = "county")
    private String county;

    @Column(name = "city")
    private String city;

    @Column(name = "team_type")
    private String teamType;

    // Înlocuim @Lob cu o simplă coloană de tip byte[] fără adnotări speciale
    // SQLite va trata acest câmp ca BLOB fără a folosi API-urile specifice care nu sunt implementate
    @Column(name = "certificate")
    private byte[] certificate;

    @Column(name = "birth_date")
    private Date birthDate;


    public User() {
    }

    public User(String name, String email, String password, String userType, String address, String county, String city, Date birthDate) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.userType = userType;
        this.address = address;
        this.county = county;
        this.city = city;
        this.birthDate = birthDate;
    }

    public User(String name, String email, String password, String userType, String address, String county, String city, String teamType, byte[] certificate) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.userType = userType;
        this.address = address;
        this.county = county;
        this.city = city;
        this.teamType = teamType;
        this.certificate = certificate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public void setCertificate(byte[] certificate) {
        this.certificate = certificate;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }
}