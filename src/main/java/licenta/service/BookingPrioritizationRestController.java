package licenta.service;

import licenta.model.*;
import licenta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/booking-prioritization")
public class BookingPrioritizationRestController {

    private static final Logger logger = LoggerFactory.getLogger(BookingPrioritizationRestController.class);

    private final IReservationProfileRepository reservationProfileRepository;
    private final IUserSpringRepository userRepository;
    private final ISportsHallSpringRepository sportsHallRepository;
    private final IReservationSpringRepository reservationRepository;
    private final IScheduleSpringRepository scheduleRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    public BookingPrioritizationRestController(
            IReservationProfileRepository reservationProfileRepository,
            IUserSpringRepository userRepository,
            ISportsHallSpringRepository sportsHallRepository,
            IReservationSpringRepository reservationRepository,
            IScheduleSpringRepository scheduleRepository) {
        this.reservationProfileRepository = reservationProfileRepository;
        this.userRepository = userRepository;
        this.sportsHallRepository = sportsHallRepository;
        this.reservationRepository = reservationRepository;
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Endpoint principal pentru generarea automată a rezervărilor
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateBookings() {
        try {
            logger.info("=== ÎNCEPEREA GENERĂRII AUTOMATE A REZERVĂRILOR ===");

            GenerationResult result = generateAutomatedBookings();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rezervările au fost generate cu succes.");
            response.put("count", result.getTotalReservations());
            response.put("profileResults", result.getProfileResults());
            response.put("reservations", result.getReservations());

            logger.info("=== GENERARE COMPLETĂ: {} rezervări pentru {} profiluri ===",
                    result.getTotalReservations(), result.getSuccessfulProfiles());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("EROARE CRITICĂ la generarea rezervărilor", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Eroare la generarea rezervărilor: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint pentru verificarea statusului sistemului
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            List<ReservationProfile> allProfiles = (List<ReservationProfile>) reservationProfileRepository.findAll();
            List<ReservationProfile> eligibleProfiles = getEligibleProfiles();
            List<SportsHall> activeHalls = getActiveHalls();

            Date nextMonday = calculateNextWeekStart();
            Date nextSunday = calculateWeekEnd(nextMonday);

            status.put("systemReady", true);
            status.put("totalProfiles", allProfiles.size());
            status.put("eligibleProfiles", eligibleProfiles.size());
            status.put("activeHalls", activeHalls.size());
            status.put("nextWeekStart", formatDate(nextMonday));
            status.put("nextWeekEnd", formatDate(nextSunday));
            status.put("timestamp", new Date());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Eroare la verificarea statusului sistemului", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("systemReady", false, "error", e.getMessage()));
        }
    }

    /**
     * Metoda principală de generare a rezervărilor
     */
    private GenerationResult generateAutomatedBookings() {
        GenerationResult result = new GenerationResult();

        // 1. Calculează perioada pentru săptămâna următoare
        Date nextMonday = calculateNextWeekStart();
        Date nextSunday = calculateWeekEnd(nextMonday);

        logger.info("Generare pentru săptămâna: {} - {}", formatDate(nextMonday), formatDate(nextSunday));

        // 2. Șterge rezervările existente din săptămâna următoare
        deleteExistingReservationsForNextWeek(nextMonday, nextSunday);

        // 3. Obține și prioritizează profilurile
        List<ReservationProfile> eligibleProfiles = getEligibleProfiles();
        List<ReservationProfile> prioritizedProfiles = prioritizeProfiles(eligibleProfiles);

        logger.info("Profiluri eligibile: {}", prioritizedProfiles.size());

        // 4. Creează programul disponibil pentru săptămâna următoare
        WeeklySchedule weeklySchedule = createWeeklySchedule(nextMonday);

        // 5. Generează rezervările pentru fiecare profil în ordine de prioritate
        for (ReservationProfile profile : prioritizedProfiles) {
            try {
                ProfileResult profileResult = generateReservationsForProfile(profile, weeklySchedule, nextMonday);
                result.addProfileResult(profileResult);

                logger.info("Profil '{}': {} rezervări generate",
                        profile.getName(), profileResult.getReservations().size());

            } catch (Exception e) {
                logger.error("Eroare la generarea rezervărilor pentru profilul '{}': {}",
                        profile.getName(), e.getMessage());
                result.addFailedProfile(profile, e.getMessage());
            }
        }

        return result;
    }

    /**
     * Calculează data de început pentru săptămâna următoare (luni)
     */
    private Date calculateNextWeekStart() {
        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (currentDayOfWeek == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Dacă e duminică, următoarea zi e luni
        } else {
            int daysUntilNextMonday = Calendar.MONDAY - currentDayOfWeek + 7;
            calendar.add(Calendar.DAY_OF_YEAR, daysUntilNextMonday);
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    /**
     * Calculează sfârșitul săptămânii (duminică)
     */
    private Date calculateWeekEnd(Date weekStart) {
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(weekStart);
        endCal.add(Calendar.DAY_OF_YEAR, 6);
        return endCal.getTime();
    }

    /**
     * Șterge rezervările existente din săptămâna următoare
     */
    private void deleteExistingReservationsForNextWeek(Date startDate, Date endDate) {
        logger.info("Ștergerea rezervărilor existente pentru săptămâna {} - {}", startDate, endDate);

        List<Reservation> existingReservations = (List<Reservation>) reservationRepository.findAll();
        List<Reservation> reservationsToDelete = existingReservations.stream()
                .filter(reservation -> isReservationInWeek(reservation, startDate, endDate))
                .collect(Collectors.toList());

        if (!reservationsToDelete.isEmpty()) {
            logger.info("Se șterg {} rezervări existente", reservationsToDelete.size());
            reservationRepository.deleteAll(reservationsToDelete);
        }
    }

    /**
     * Verifică dacă o rezervare este în săptămâna specificată
     */
    private boolean isReservationInWeek(Reservation reservation, Date startDate, Date endDate) {
        Date reservationDate = reservation.getDate();
        return reservationDate != null
                && !reservationDate.before(startDate)
                && !reservationDate.after(endDate)
                && "reservation".equals(reservation.getType());
    }

    /**
     * Obține profilurile eligibile pentru generare
     */
    private List<ReservationProfile> getEligibleProfiles() {
        List<ReservationProfile> allProfiles = (List<ReservationProfile>) reservationProfileRepository.findAll();

        return allProfiles.stream()
                .filter(this::isProfileEligible)
                .collect(Collectors.toList());
    }

    /**
     * Verifică dacă un profil este eligibil pentru generarea automată
     */
    private boolean isProfileEligible(ReservationProfile profile) {
        User user = profile.getUser();
        return user != null
                && user.getTeamType() != null
                && !user.getTeamType().isEmpty()
                && ("active".equals(user.getAccountStatus()) || "verified".equals(user.getAccountStatus()))
                && profile.getSelectedHalls() != null
                && !profile.getSelectedHalls().isEmpty()
                && profile.getWeeklyBudget() != null
                && profile.getWeeklyBudget().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Sortează profilurile după prioritate
     */
    private List<ReservationProfile> prioritizeProfiles(List<ReservationProfile> profiles) {
        return profiles.stream()
                .sorted((p1, p2) -> {
                    // 1. Prioritate după categoria de vârstă (seniori > 17-18 > 15-16 > 0-14)
                    int agePriority1 = getAgePriority(p1.getAgeCategory());
                    int agePriority2 = getAgePriority(p2.getAgeCategory());

                    if (agePriority1 != agePriority2) {
                        return Integer.compare(agePriority2, agePriority1); // Descendent
                    }

                    // 2. Prioritate după bugetul săptămânal (buget mai mare = prioritate mai mare)
                    return p2.getWeeklyBudget().compareTo(p1.getWeeklyBudget());
                })
                .collect(Collectors.toList());
    }

    /**
     * Returnează prioritatea numerică pentru categoria de vârstă
     */
    private int getAgePriority(String ageCategory) {
        if (ageCategory == null) return 0;

        return switch (ageCategory.toLowerCase()) {
            case "seniori", "senior" -> 4;
            case "17-18" -> 3;
            case "15-16" -> 2;
            case "0-14" -> 1;
            default -> 0;
        };
    }

    /**
     * Creează programul săptămânal disponibil
     */
    private WeeklySchedule createWeeklySchedule(Date startDate) {
        WeeklySchedule schedule = new WeeklySchedule();
        List<SportsHall> activeHalls = getActiveHalls();

        // Pentru fiecare zi din săptămână (luni - duminică)
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DAY_OF_YEAR, dayOffset);
            Date currentDate = cal.getTime();

            DaySchedule daySchedule = createDaySchedule(currentDate, activeHalls);
            schedule.addDaySchedule(formatDate(currentDate), daySchedule);
        }

        // Marchează sloturile deja ocupate
        markExistingReservations(schedule, startDate);

        return schedule;
    }

    /**
     * Creează programul pentru o zi
     */
    private DaySchedule createDaySchedule(Date date, List<SportsHall> halls) {
        DaySchedule daySchedule = new DaySchedule();
        int dayOfWeek = getDayOfWeekForSchedule(date);

        for (SportsHall hall : halls) {
            HallDaySchedule hallSchedule = createHallDaySchedule(hall, dayOfWeek);
            daySchedule.addHallSchedule(hall.getId(), hallSchedule);
        }

        return daySchedule;
    }

    /**
     * Creează programul unei săli pentru o zi
     */
    private HallDaySchedule createHallDaySchedule(SportsHall hall, int dayOfWeek) {
        HallDaySchedule hallSchedule = new HallDaySchedule();

        // Găsește programul sălii pentru această zi
        List<Schedule> hallSchedules = scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(hall.getId(), true);
        Optional<Schedule> dayScheduleOpt = hallSchedules.stream()
                .filter(s -> s.getDayOfWeek().equals(dayOfWeek))
                .findFirst();

        List<String> timeSlots = generateTimeSlots();

        for (String timeSlot : timeSlots) {
            SlotStatus status = determineSlotStatus(timeSlot, dayScheduleOpt);
            hallSchedule.addSlot(timeSlot, new TimeSlot(timeSlot, status, hall.getTariff()));
        }

        return hallSchedule;
    }

    /**
     * Determină statusul unui slot
     */
    private SlotStatus determineSlotStatus(String timeSlot, Optional<Schedule> daySchedule) {
        if (daySchedule.isEmpty()) {
            return SlotStatus.HALL_CLOSED;
        }

        Schedule schedule = daySchedule.get();
        if (isSlotInWorkingHours(timeSlot, schedule.getStartTime(), schedule.getEndTime())) {
            return SlotStatus.AVAILABLE;
        } else {
            return SlotStatus.OUTSIDE_HOURS;
        }
    }

    /**
     * Verifică dacă un slot este în orele de funcționare
     */
    private boolean isSlotInWorkingHours(String timeSlot, String startTime, String endTime) {
        try {
            String slotStartTime = timeSlot.split(" - ")[0];
            String slotEndTime = timeSlot.split(" - ")[1];

            int slotStart = timeToMinutes(slotStartTime);
            int slotEnd = timeToMinutes(slotEndTime);
            int workingStart = timeToMinutes(startTime);
            int workingEnd = timeToMinutes(endTime);

            return slotStart >= workingStart && slotEnd <= workingEnd;
        } catch (Exception e) {
            logger.warn("Eroare la verificarea orelor de funcționare pentru slot {}: {}", timeSlot, e.getMessage());
            return false;
        }
    }

    /**
     * Convertește ora în minute
     */
    private int timeToMinutes(String timeString) {
        String[] parts = timeString.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    /**
     * Generează rezervări pentru un profil
     */
    private ProfileResult generateReservationsForProfile(ReservationProfile profile, WeeklySchedule weeklySchedule, Date startDate) {
        ProfileResult result = new ProfileResult(profile);

        // Parametrii pentru profil
        int maxBookings = getMaxBookingsForProfile(profile);
        BigDecimal budget = profile.getWeeklyBudget();
        TimeInterval preferredInterval = getPreferredTimeInterval(profile.getTimeInterval());
        boolean isSenior = isSeniorCategory(profile.getAgeCategory());

        // Găsește sloturile disponibile pentru profil
        List<AvailableSlot> availableSlots = findAvailableSlotsForProfile(
                profile, weeklySchedule, startDate, preferredInterval, isSenior);

        // Selectează rezervările în funcție de buget și numărul maxim
        List<SelectedReservation> selectedReservations = selectReservationsWithinBudget(
                availableSlots, maxBookings, budget);

        if (selectedReservations.isEmpty()) {
            logger.info("Nu s-au putut selecta rezervări pentru profilul '{}'", profile.getName());
            return result;
        }

        // Creează rezervările
        List<Reservation> reservations = createReservationsFromSelected(selectedReservations, profile.getUser());
        result.setReservations(reservations);

        // Actualizează programul cu rezervările create
        updateScheduleWithReservations(weeklySchedule, reservations);

        // Trimite email de confirmare
        sendConfirmationEmail(profile.getUser(), reservations);

        return result;
    }

    /**
     * Găsește sloturile disponibile pentru un profil
     */
    private List<AvailableSlot> findAvailableSlotsForProfile(
            ReservationProfile profile, WeeklySchedule weeklySchedule, Date startDate,
            TimeInterval preferredInterval, boolean isSenior) {

        List<AvailableSlot> availableSlots = new ArrayList<>();
        List<SportsHall> profileHalls = getProfileHalls(profile);

        for (Map.Entry<String, DaySchedule> dayEntry : weeklySchedule.getDaySchedules().entrySet()) {
            Date currentDate = parseDate(dayEntry.getKey());
            DaySchedule daySchedule = dayEntry.getValue();

            for (SportsHall hall : profileHalls) {
                HallDaySchedule hallSchedule = daySchedule.getHallSchedule(hall.getId());
                if (hallSchedule == null) continue;

                for (Map.Entry<String, TimeSlot> slotEntry : hallSchedule.getSlots().entrySet()) {
                    String timeSlot = slotEntry.getKey();
                    TimeSlot slot = slotEntry.getValue();

                    if (isSlotSuitableForProfile(slot, preferredInterval, isSenior)) {
                        AvailableSlot availableSlot = new AvailableSlot(
                                hall, currentDate, timeSlot, BigDecimal.valueOf(hall.getTariff()));
                        availableSlot.setPriority(calculateSlotPriority(timeSlot, preferredInterval));
                        availableSlots.add(availableSlot);
                    }
                }
            }
        }

        // Sortează după prioritate și randomizează pentru echitate
        return availableSlots.stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * Verifică dacă un slot este potrivit pentru profil
     */
    private boolean isSlotSuitableForProfile(TimeSlot slot, TimeInterval preferredInterval, boolean isSenior) {
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            return false;
        }

        try {
            String startTime = slot.getTimeSlot().split(" - ")[0];
            int hour = Integer.parseInt(startTime.split(":")[0]);
            int minute = Integer.parseInt(startTime.split(":")[1]);
            int totalMinutes = hour * 60 + minute;

            // Verifică intervalul preferat
            if (totalMinutes < preferredInterval.getStartMinutes() ||
                    totalMinutes >= preferredInterval.getEndMinutes()) {
                return false;
            }

            // Aplică regula pentru seniori/juniori la ora 14:30
            if (isSenior) {
                return hour < 14 || (hour == 14 && minute == 0);
            } else {
                return hour > 14 || (hour == 14 && minute == 30);
            }
        } catch (Exception e) {
            logger.warn("Eroare la verificarea slot-ului {}: {}", slot.getTimeSlot(), e.getMessage());
            return false;
        }
    }

    /**
     * Calculează prioritatea unui slot
     */
    private int calculateSlotPriority(String timeSlot, TimeInterval preferredInterval) {
        try {
            String startTime = timeSlot.split(" - ")[0];
            int hour = Integer.parseInt(startTime.split(":")[0]);
            int minute = Integer.parseInt(startTime.split(":")[1]);
            int totalMinutes = hour * 60 + minute;

            int intervalCenter = (preferredInterval.getStartMinutes() + preferredInterval.getEndMinutes()) / 2;
            int distance = Math.abs(totalMinutes - intervalCenter);

            return 1000 - distance; // Prioritate inversă
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Selectează rezervările în funcție de buget
     */
    private List<SelectedReservation> selectReservationsWithinBudget(
            List<AvailableSlot> availableSlots, int maxBookings, BigDecimal budget) {

        List<SelectedReservation> selected = new ArrayList<>();
        BigDecimal remainingBudget = budget;
        Set<String> usedDateSlots = new HashSet<>();

        for (AvailableSlot slot : availableSlots) {
            if (selected.size() >= maxBookings) break;

            String dateSlotKey = formatDate(slot.getDate()) + "_" + slot.getTimeSlot();
            if (usedDateSlots.contains(dateSlotKey)) continue;

            if (remainingBudget.compareTo(slot.getPrice()) >= 0) {
                SelectedReservation reservation = new SelectedReservation(
                        slot.getHall(), slot.getDate(), slot.getTimeSlot(), slot.getPrice());
                selected.add(reservation);
                remainingBudget = remainingBudget.subtract(slot.getPrice());
                usedDateSlots.add(dateSlotKey);
            }
        }

        return selected;
    }

    /**
     * Creează rezervările din selecția făcută (fără plăți)
     */
    private List<Reservation> createReservationsFromSelected(List<SelectedReservation> selectedReservations, User user) {
        List<Reservation> reservations = new ArrayList<>();

        for (SelectedReservation selected : selectedReservations) {
            Reservation reservation = new Reservation();
            reservation.setHall(selected.getHall());
            reservation.setUser(user);
            reservation.setDate(selected.getDate());
            reservation.setTimeSlot(selected.getTimeSlot());
            reservation.setPrice(selected.getPrice().floatValue());
            reservation.setType("reservation");
            reservation.setStatus("CONFIRMED");

            Reservation savedReservation = reservationRepository.save(reservation);
            reservations.add(savedReservation);
        }

        return reservations;
    }

    /**
     * Trimite email de confirmare (fără plăți)
     */
    private void sendConfirmationEmail(User user, List<Reservation> reservations) {
        try {
            ReservationEmailService.sendReservationConfirmationEmail(
                    mailSender, senderEmail, user, reservations, null);
        } catch (Exception e) {
            logger.error("Eroare la trimiterea email-ului de confirmare pentru utilizatorul {}: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    // Metode helper

    private List<SportsHall> getActiveHalls() {
        return ((List<SportsHall>) sportsHallRepository.findAll()).stream()
                .filter(hall -> hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    private List<SportsHall> getProfileHalls(ReservationProfile profile) {
        return profile.getSelectedHalls().stream()
                .filter(hall -> hall.getCity().equals(profile.getCity()) &&
                        hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    private int getMaxBookingsForProfile(ReservationProfile profile) {
        String ageCategory = profile.getAgeCategory();
        return switch (ageCategory.toLowerCase()) {
            case "seniori", "senior" -> 6;
            case "17-18" -> 5;
            case "15-16" -> 4;
            case "0-14" -> 3;
            default -> 3;
        };
    }

    private TimeInterval getPreferredTimeInterval(String timeInterval) {
        if (timeInterval == null) return new TimeInterval(8 * 60, 23 * 60);

        return switch (timeInterval) {
            case "7-14:30" -> new TimeInterval(7 * 60, 14 * 60 + 30);
            case "14:30-23" -> new TimeInterval(14 * 60 + 30, 23 * 60);
            case "full-day" -> new TimeInterval(7 * 60, 23 * 60);
            default -> new TimeInterval(8 * 60, 23 * 60);
        };
    }

    private boolean isSeniorCategory(String ageCategory) {
        return "seniori".equals(ageCategory) || "senior".equals(ageCategory);
    }

    private int getDayOfWeekForSchedule(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == 1 ? 7 : dayOfWeek - 1; // Convertește din format Calendar la format backend
    }

    private List<String> generateTimeSlots() {
        return Arrays.asList(
                "07:00 - 08:30", "08:30 - 10:00", "10:00 - 11:30", "11:30 - 13:00",
                "13:00 - 14:30", "14:30 - 16:00", "16:00 - 17:30", "17:30 - 19:00",
                "19:00 - 20:30", "20:30 - 22:00", "22:00 - 23:30"
        );
    }

    private String formatDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.format("%d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private Date parseDate(String dateStr) {
        try {
            String[] parts = dateStr.split("-");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
            return cal.getTime();
        } catch (Exception e) {
            return new Date();
        }
    }

    private void markExistingReservations(WeeklySchedule schedule, Date startDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DAY_OF_YEAR, 6);
        Date endDate = cal.getTime();

        List<Reservation> existingReservations = (List<Reservation>) reservationRepository.findAll();

        for (Reservation reservation : existingReservations) {
            if (isReservationInCurrentWeek(reservation, startDate, endDate)) {
                markSlotAsOccupied(schedule, reservation);
            }
        }
    }

    private boolean isReservationInCurrentWeek(Reservation reservation, Date startDate, Date endDate) {
        Date reservationDate = reservation.getDate();
        return reservationDate != null
                && !reservationDate.before(startDate)
                && !reservationDate.after(endDate);
    }

    private void markSlotAsOccupied(WeeklySchedule schedule, Reservation reservation) {
        String dateKey = formatDate(reservation.getDate());
        Long hallId = reservation.getHall().getId();
        String timeSlot = reservation.getTimeSlot();

        DaySchedule daySchedule = schedule.getDaySchedule(dateKey);
        if (daySchedule != null) {
            HallDaySchedule hallSchedule = daySchedule.getHallSchedule(hallId);
            if (hallSchedule != null) {
                TimeSlot slot = hallSchedule.getSlot(timeSlot);
                if (slot != null) {
                    if ("maintenance".equals(reservation.getType())) {
                        slot.setStatus(SlotStatus.MAINTENANCE);
                    } else if ("reservation".equals(reservation.getType()) &&
                            !"CANCELLED".equals(reservation.getStatus())) {
                        slot.setStatus(SlotStatus.BOOKED);
                    }
                }
            }
        }
    }

    private void updateScheduleWithReservations(WeeklySchedule schedule, List<Reservation> reservations) {
        for (Reservation reservation : reservations) {
            markSlotAsOccupied(schedule, reservation);
        }
    }

    // Clase pentru gestionarea programului și rezultatelor

    public static class WeeklySchedule {
        private Map<String, DaySchedule> daySchedules = new HashMap<>();

        public void addDaySchedule(String date, DaySchedule daySchedule) {
            daySchedules.put(date, daySchedule);
        }

        public DaySchedule getDaySchedule(String date) {
            return daySchedules.get(date);
        }

        public Map<String, DaySchedule> getDaySchedules() {
            return daySchedules;
        }
    }

    public static class DaySchedule {
        private Map<Long, HallDaySchedule> hallSchedules = new HashMap<>();

        public void addHallSchedule(Long hallId, HallDaySchedule schedule) {
            hallSchedules.put(hallId, schedule);
        }

        public HallDaySchedule getHallSchedule(Long hallId) {
            return hallSchedules.get(hallId);
        }
    }

    public static class HallDaySchedule {
        private Map<String, TimeSlot> slots = new HashMap<>();

        public void addSlot(String timeSlot, TimeSlot slot) {
            slots.put(timeSlot, slot);
        }

        public TimeSlot getSlot(String timeSlot) {
            return slots.get(timeSlot);
        }

        public Map<String, TimeSlot> getSlots() {
            return slots;
        }
    }

    public static class TimeSlot {
        private String timeSlot;
        private SlotStatus status;
        private float tariff;

        public TimeSlot(String timeSlot, SlotStatus status, float tariff) {
            this.timeSlot = timeSlot;
            this.status = status;
            this.tariff = tariff;
        }

        // Getters and setters
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public SlotStatus getStatus() { return status; }
        public void setStatus(SlotStatus status) { this.status = status; }
        public float getTariff() { return tariff; }
        public void setTariff(float tariff) { this.tariff = tariff; }
    }

    public enum SlotStatus {
        AVAILABLE, BOOKED, MAINTENANCE, OUTSIDE_HOURS, HALL_CLOSED
    }

    public static class AvailableSlot {
        private SportsHall hall;
        private Date date;
        private String timeSlot;
        private BigDecimal price;
        private int priority;

        public AvailableSlot(SportsHall hall, Date date, String timeSlot, BigDecimal price) {
            this.hall = hall;
            this.date = date;
            this.timeSlot = timeSlot;
            this.price = price;
        }

        // Getters and setters
        public SportsHall getHall() { return hall; }
        public void setHall(SportsHall hall) { this.hall = hall; }
        public Date getDate() { return date; }
        public void setDate(Date date) { this.date = date; }
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }

    public static class SelectedReservation {
        private SportsHall hall;
        private Date date;
        private String timeSlot;
        private BigDecimal price;

        public SelectedReservation(SportsHall hall, Date date, String timeSlot, BigDecimal price) {
            this.hall = hall;
            this.date = date;
            this.timeSlot = timeSlot;
            this.price = price;
        }

        // Getters and setters
        public SportsHall getHall() { return hall; }
        public void setHall(SportsHall hall) { this.hall = hall; }
        public Date getDate() { return date; }
        public void setDate(Date date) { this.date = date; }
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public static class TimeInterval {
        private int startMinutes;
        private int endMinutes;

        public TimeInterval(int startMinutes, int endMinutes) {
            this.startMinutes = startMinutes;
            this.endMinutes = endMinutes;
        }

        public int getStartMinutes() { return startMinutes; }
        public int getEndMinutes() { return endMinutes; }
    }

    public static class GenerationResult {
        private List<Reservation> reservations = new ArrayList<>();
        private List<ProfileResult> profileResults = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public void addProfileResult(ProfileResult profileResult) {
            profileResults.add(profileResult);
            if (profileResult.getReservations() != null) {
                reservations.addAll(profileResult.getReservations());
            }
        }

        public void addFailedProfile(ReservationProfile profile, String error) {
            ProfileResult failedResult = new ProfileResult(profile);
            failedResult.setError(error);
            profileResults.add(failedResult);
            errors.add("Profil '" + profile.getName() + "': " + error);
        }

        // Getters
        public List<Reservation> getReservations() { return reservations; }
        public List<ProfileResult> getProfileResults() { return profileResults; }
        public int getTotalReservations() { return reservations.size(); }
        public int getSuccessfulProfiles() {
            return (int) profileResults.stream()
                    .filter(ProfileResult::isSuccess)
                    .count();
        }
        public List<String> getErrors() { return errors; }
    }

    public static class ProfileResult {
        private ReservationProfile profile;
        private List<Reservation> reservations = new ArrayList<>();
        private String error;
        private boolean success = true;

        public ProfileResult(ReservationProfile profile) {
            this.profile = profile;
        }

        // Getters and setters
        public ReservationProfile getProfile() { return profile; }
        public void setProfile(ReservationProfile profile) { this.profile = profile; }
        public List<Reservation> getReservations() { return reservations; }
        public void setReservations(List<Reservation> reservations) {
            this.reservations = reservations;
            this.success = reservations != null && !reservations.isEmpty();
        }
        public String getError() { return error; }
        public void setError(String error) {
            this.error = error;
            this.success = false;
        }
        public boolean isSuccess() { return success; }
    }
}