package licenta.model;

import jakarta.persistence.*;

@Entity
@Table(name = "schedules")
public class Schedule extends BruteEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sports_hall_id", nullable = false)
    private SportsHall sportsHall;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1=Luni, 2=Marți, ..., 7=Duminică

    @Column(name = "start_time", nullable = false)
    private String startTime; // Format: "07:00"

    @Column(name = "end_time", nullable = false)
    private String endTime;   // Format: "23:00"

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Pentru a putea dezactiva temporar o zi

    public Schedule() {
    }

    public Schedule(SportsHall sportsHall, Integer dayOfWeek, String startTime, String endTime) {
        this.sportsHall = sportsHall;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = true;
    }

    public String getDayName() {
        switch (dayOfWeek) {
            case 1: return "Luni";
            case 2: return "Marți";
            case 3: return "Miercuri";
            case 4: return "Joi";
            case 5: return "Vineri";
            case 6: return "Sâmbătă";
            case 7: return "Duminică";
            default: return "Unknown";
        }
    }

    // Metoda pentru a genera intervalele de timp de 1.5 ore
    public java.util.List<String> generateTimeSlots() {
        java.util.List<String> slots = new java.util.ArrayList<>();

        try {
            int startHour = Integer.parseInt(startTime.split(":")[0]);
            int startMinute = Integer.parseInt(startTime.split(":")[1]);
            int endHour = Integer.parseInt(endTime.split(":")[0]);
            int endMinute = Integer.parseInt(endTime.split(":")[1]);

            int currentHour = startHour;
            int currentMinute = startMinute;

            while (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute)) {
                String slotStart = String.format("%02d:%02d", currentHour, currentMinute);

                // Adauga 1.5 ore
                currentMinute += 90;
                if (currentMinute >= 60) {
                    currentHour += currentMinute / 60;
                    currentMinute = currentMinute % 60;
                }

                String slotEnd = String.format("%02d:%02d", currentHour, currentMinute);

                if (currentHour < endHour || (currentHour == endHour && currentMinute <= endMinute)) {
                    slots.add(slotStart + "-" + slotEnd);
                }
            }
        } catch (Exception e) {
            // În caz de eroare, returnează o listă goală
        }

        return slots;
    }

    public SportsHall getSportsHall() {
        return sportsHall;
    }

    public void setSportsHall(SportsHall sportsHall) {
        this.sportsHall = sportsHall;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "id=" + getId() +
                ", dayOfWeek=" + dayOfWeek +
                ", dayName='" + getDayName() + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}