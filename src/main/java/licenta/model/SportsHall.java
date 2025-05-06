package licenta.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sports_halls")
public class SportsHall extends BruteEntity<Long> {

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "type")
    private String type;

    @Column(name = "tariff")
    private Float tariff;

    @Column(name = "facilities")
    private String facilities;

    @Column(name = "county")
    private String county;

    @Column(name = "city")
    private String city;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "sportsHall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<SportsHallImage> images = new ArrayList<>();

    @Column(name = "admin_id")
    private Long adminId;

    // Adăugare relație Many-to-Many către ReservationProfile
    @ManyToMany(mappedBy = "selectedHalls")
    @JsonIgnoreProperties("selectedHalls")
    private List<ReservationProfile> reservationProfiles = new ArrayList<>();

    public SportsHall() {
    }

    public SportsHall(String name, String address, Integer capacity, String type, Float tariff, String facilities, String county, String city, String description, List<SportsHallImage> images) {
        this.name = name;
        this.address = address;
        this.capacity = capacity;
        this.type = type;
        this.tariff = tariff;
        this.facilities = facilities;
        this.county = county;
        this.city = city;
        this.description = description;
        this.images = images;
    }

    public SportsHall(String name, String address, Integer capacity, String type, Float tariff,
                      String facilities, String county, String city, String description,
                      List<SportsHallImage> images, Long adminId) {
        this(name, address, capacity, type, tariff, facilities, county, city, description, images);
        this.adminId = adminId;
    }

    public void addImage(SportsHallImage image) {
        images.add(image);
        image.setSportsHall(this);
    }

    public void removeImage(SportsHallImage image) {
        images.remove(image);
        image.setSportsHall(null);
    }

    // Getter și setter pentru reservationProfiles
    public List<ReservationProfile> getReservationProfiles() {
        return reservationProfiles;
    }

    public void setReservationProfiles(List<ReservationProfile> reservationProfiles) {
        this.reservationProfiles = reservationProfiles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Float getTariff() {
        return tariff;
    }

    public void setTariff(Float tariff) {
        this.tariff = tariff;
    }

    public String getFacilities() {
        return facilities;
    }

    public void setFacilities(String facilities) {
        this.facilities = facilities;
    }

    public List<SportsHallImage> getImages() {
        return images;
    }

    public void setImages(List<SportsHallImage> images) {
        this.images = images;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    @Override
    public String toString() {
        return "SportsHall{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", capacity=" + capacity +
                ", type='" + type + '\'' +
                ", tariff=" + tariff +
                ", facilities='" + facilities + '\'' +
                '}';
    }
}