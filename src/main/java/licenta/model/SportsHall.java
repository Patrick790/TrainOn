package licenta.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "phone_number")
    private String phoneNumber;

    // CÂMP NOU PENTRU STATUS
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HallStatus status = HallStatus.ACTIVE;

    @OneToMany(mappedBy = "sportsHall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<SportsHallImage> images = new ArrayList<>();

    @Column(name = "admin_id")
    private Long adminId;

    // Adăugare relație Many-to-Many către ReservationProfile
    @ManyToMany(mappedBy = "selectedHalls")
    @JsonIgnoreProperties("selectedHalls")
    private List<ReservationProfile> reservationProfiles = new ArrayList<>();

    // NOUĂ relație One-to-Many către Schedule
    @OneToMany(mappedBy = "sportsHall", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Schedule> schedules = new ArrayList<>();

    // ENUM PENTRU STATUS
    public enum HallStatus {
        ACTIVE("Activ"),
        INACTIVE("Inactiv");

        private final String displayName;

        HallStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public SportsHall() {
        this.status = HallStatus.ACTIVE; // Default status
    }

    public SportsHall(String name, String address, Integer capacity, String type, Float tariff, String facilities, String county, String city, String description, List<SportsHallImage> images) {
        this();
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

    // Metode helper pentru Schedule
    public void addSchedule(Schedule schedule) {
        schedules.add(schedule);
        schedule.setSportsHall(this);
    }

    public void removeSchedule(Schedule schedule) {
        schedules.remove(schedule);
        schedule.setSportsHall(null);
    }

    // Metoda pentru a activa sala
    public void activate() {
        this.status = HallStatus.ACTIVE;
    }

    // Metoda pentru a dezactiva sala
    public void deactivate() {
        this.status = HallStatus.INACTIVE;
    }

    // Metoda pentru a verifica dacă sala este activă
    public boolean isActive() {
        return this.status == HallStatus.ACTIVE;
    }

    // Metoda pentru a obține programul formatat
    public String getFormattedSchedule() {
        if (schedules == null || schedules.isEmpty()) {
            return "Program: Nu este definit";
        }

        StringBuilder sb = new StringBuilder("Program:\n");
        schedules.stream()
                .filter(Schedule::getIsActive)
                .sorted((s1, s2) -> s1.getDayOfWeek().compareTo(s2.getDayOfWeek()))
                .forEach(schedule ->
                        sb.append(schedule.getDayName())
                                .append(": ")
                                .append(schedule.getStartTime())
                                .append("-")
                                .append(schedule.getEndTime())
                                .append("\n")
                );

        return sb.toString();
    }

    // Getters și Setters
    public List<ReservationProfile> getReservationProfiles() {
        return reservationProfiles;
    }

    public void setReservationProfiles(List<ReservationProfile> reservationProfiles) {
        this.reservationProfiles = reservationProfiles;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public HallStatus getStatus() {
        return status;
    }

    public void setStatus(HallStatus status) {
        this.status = status;
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
                ", status=" + status +
                '}';
    }
}