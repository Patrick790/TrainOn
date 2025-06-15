package licenta.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @Column(name = "certificate")
    private byte[] certificate;

    @Column(name = "birth_date")
    private Date birthDate;

    @Column(name = "account_status")
    private String accountStatus;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "fcfs_enabled", nullable = false)
    private Boolean fcfsEnabled = true;

    @OneToMany(mappedBy = "user")
    @JsonManagedReference("user-profiles")
    private List<ReservationProfile> reservationProfiles = new ArrayList<>();

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
        this.createdAt = new Date();
        this.fcfsEnabled = true;
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
        this.createdAt = new Date();
        this.fcfsEnabled = true;
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

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getFcfsEnabled() {
        return fcfsEnabled;
    }

    public void setFcfsEnabled(Boolean fcfsEnabled) {
        this.fcfsEnabled = fcfsEnabled;
    }

    public boolean isFcfsEnabled() {
        return fcfsEnabled != null && fcfsEnabled;
    }

    public List<ReservationProfile> getReservationProfiles() {
        return reservationProfiles;
    }

    public void setReservationProfiles(List<ReservationProfile> reservationProfiles) {
        this.reservationProfiles = reservationProfiles;
    }

    public void addReservationProfile(ReservationProfile profile) {
        reservationProfiles.add(profile);
        profile.setUser(this);
    }

    public void removeReservationProfile(ReservationProfile profile) {
        reservationProfiles.remove(profile);
        profile.setUser(null);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", userType='" + userType + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", fcfsEnabled=" + fcfsEnabled +
                '}';
    }
}