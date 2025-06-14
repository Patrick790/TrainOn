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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/booking-prioritization")
public class BookingPrioritizationRestController {

    private static final Logger logger = LoggerFactory.getLogger(BookingPrioritizationRestController.class);

    private final IReservationProfileRepository reservationProfileRepository;
    private final IUserSpringRepository userRepository;
    private final ISportsHallSpringRepository sportsHallRepository;
    private final IReservationSpringRepository reservationRepository;
    private final IScheduleSpringRepository scheduleRepository;
    private final IPaymentSpringRepository paymentRepository;
    private final ICardPaymentMethodSpringRepository cardPaymentMethodRepository;
    private final AutoPaymentService autoPaymentService;

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
            IScheduleSpringRepository scheduleRepository,
            IPaymentSpringRepository paymentRepository,
            ICardPaymentMethodSpringRepository cardPaymentMethodRepository,
            AutoPaymentService autoPaymentService) {
        this.reservationProfileRepository = reservationProfileRepository;
        this.userRepository = userRepository;
        this.sportsHallRepository = sportsHallRepository;
        this.reservationRepository = reservationRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentRepository = paymentRepository;
        this.cardPaymentMethodRepository = cardPaymentMethodRepository;
        this.autoPaymentService = autoPaymentService;
    }

    /**
     * Endpoint pentru generarea automată a rezervărilor cu plăți automate
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateBookings() {
        try {
            logger.info("=== ÎNCEPUT DEBUG COMPLET - GENERARE REZERVĂRI ===");

            // ===== DEBUGGING: VERIFICARE PROFILURI =====
            List<ReservationProfile> allProfiles = StreamSupport.stream(
                            reservationProfileRepository.findAll().spliterator(), false)
                    .collect(Collectors.toList());

            logger.info("📊 TOTAL PROFILURI ÎN DB: {}", allProfiles.size());

            // Debug detaliat pentru fiecare profil
            for (ReservationProfile profile : allProfiles) {
                User user = profile.getUser();
                logger.info("🔍 === PROFIL {} ===", profile.getId());
                logger.info("  📝 Nume: {}", profile.getName());
                logger.info("  👤 User: {} (ID: {})",
                        user != null ? user.getEmail() : "NULL",
                        user != null ? user.getId() : "NULL");
                logger.info("  💳 AutoPayment: {}", profile.getAutoPaymentEnabled());
                logger.info("  🏢 SelectedHalls: {}",
                        profile.getSelectedHalls() != null ? profile.getSelectedHalls().size() : "NULL");

                if (user != null) {
                    logger.info("  🏷️ TeamType: '{}' (null: {})",
                            user.getTeamType(), user.getTeamType() == null);
                    logger.info("  ✅ AccountStatus: '{}' (active/verified: {})",
                            user.getAccountStatus(),
                            user.getAccountStatus() != null &&
                                    (user.getAccountStatus().equals("active") || user.getAccountStatus().equals("verified")));
                } else {
                    logger.warn("  ❌ USER ESTE NULL!");
                }

                // Testează eligibilitatea pas cu pas
                boolean userExists = user != null;
                boolean hasTeamType = user != null && user.getTeamType() != null;
                boolean teamTypeNotEmpty = user != null && user.getTeamType() != null && !user.getTeamType().isEmpty();
                boolean statusValid = user != null && user.getAccountStatus() != null &&
                        (user.getAccountStatus().equals("active") || user.getAccountStatus().equals("verified"));
                boolean hasHalls = profile.getSelectedHalls() != null;
                boolean hallsNotEmpty = profile.getSelectedHalls() != null && !profile.getSelectedHalls().isEmpty();

                logger.info("  🧪 TESTE ELIGIBILITATE:");
                logger.info("    - User exists: {}", userExists);
                logger.info("    - Has team_type: {}", hasTeamType);
                logger.info("    - Team_type not empty: {}", teamTypeNotEmpty);
                logger.info("    - Status valid: {}", statusValid);
                logger.info("    - Has halls: {}", hasHalls);
                logger.info("    - Halls not empty: {}", hallsNotEmpty);

                boolean eligible = userExists && hasTeamType && teamTypeNotEmpty && statusValid && hasHalls && hallsNotEmpty;
                logger.info("  🎯 REZULTAT FINAL - ELIGIBIL: {}", eligible);

                if (!eligible) {
                    logger.warn("  ⚠️ PROFIL NEELIGIBIL - MOTIVUL:");
                    if (!userExists) logger.warn("    - User lipsește");
                    if (!hasTeamType) logger.warn("    - team_type este null");
                    if (!teamTypeNotEmpty) logger.warn("    - team_type este gol");
                    if (!statusValid) logger.warn("    - account_status nu este active/verified");
                    if (!hasHalls) logger.warn("    - selectedHalls este null");
                    if (!hallsNotEmpty) logger.warn("    - selectedHalls este gol");
                }
            }

            // ===== DEBUGGING: VERIFICARE ELIGIBILITATE =====
            List<ReservationProfile> eligibleProfiles = getEligibleProfiles();
            logger.info("🎯 === REZULTAT ELIGIBILITATE ===");
            logger.info("Profiluri eligibile: {} din {}", eligibleProfiles.size(), allProfiles.size());

            if (eligibleProfiles.isEmpty()) {
                logger.error("❌ ZERO PROFILURI ELIGIBILE! Procesul se oprește aici.");
                logger.error("🔧 SOLUȚII POSIBILE:");
                logger.error("   1. UPDATE users SET team_type = 'standard' WHERE id IN (11, 13, 14);");
                logger.error("   2. UPDATE users SET account_status = 'active' WHERE id IN (11, 13, 14);");
                logger.error("   3. Verifică tabela profile_halls pentru asocieri");

                Map<String, Object> debugResponse = new HashMap<>();
                debugResponse.put("success", false);
                debugResponse.put("message", "Nu există profiluri eligibile pentru generare");
                debugResponse.put("totalProfiles", allProfiles.size());
                debugResponse.put("eligibleProfiles", 0);
                debugResponse.put("debug", "Verifică team_type, account_status și selected_halls");

                return ResponseEntity.ok(debugResponse);
            }

            // ===== DEBUGGING: VERIFICARE SĂLI ACTIVE =====
            List<SportsHall> activeHalls = getActiveHallsWithSchedules();
            logger.info("🏢 === SĂLI ACTIVE ===");
            logger.info("Săli active găsite: {}", activeHalls.size());

            if (activeHalls.isEmpty()) {
                logger.error("❌ ZERO SĂLI ACTIVE! Procesul se oprește aici.");

                Map<String, Object> debugResponse = new HashMap<>();
                debugResponse.put("success", false);
                debugResponse.put("message", "Nu există săli active disponibile");
                debugResponse.put("eligibleProfiles", eligibleProfiles.size());
                debugResponse.put("activeHalls", 0);

                return ResponseEntity.ok(debugResponse);
            }

            logger.info("Începerea procesului de generare automată a rezervărilor...");

            // Calculează data pentru săptămâna următoare
            Date nextMonday = calculateNextWeekStart();
            Date nextSunday = calculateWeekEnd(nextMonday);

            logger.info("Generare pentru săptămâna: {} - {}", formatDate(nextMonday), formatDate(nextSunday));

            // Șterge rezervările existente din săptămâna următoare
            deleteExistingReservationsForNextWeek(nextMonday, nextSunday);

            // Generează noi rezervări cu plăți automate
            GenerationResult result = generateAutomatedBookingsWithPayments(nextMonday);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rezervările au fost generate cu succes.");
            response.put("count", result.getTotalReservations());
            response.put("reservations", result.getReservations());
            response.put("payments", result.getPayments());
            response.put("paymentsCount", result.getPayments().size());
            response.put("autoPaymentsSuccessful", result.getAutoPaymentsSuccessful());
            response.put("autoPaymentsFailed", result.getAutoPaymentsFailed());

            logger.info("Generare completă: {} rezervări, {} plăți procesate",
                    result.getTotalReservations(), result.getPayments().size());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Eroare la generarea rezervărilor", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Eroare la generarea rezervărilor: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Calculează data de început pentru săptămâna următoare calendaristică
     */
    private Date calculateNextWeekStart() {
        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        logger.info("Ziua curentă: {} (1=Duminică, 2=Luni, etc.)", currentDayOfWeek);

        if (currentDayOfWeek == Calendar.SUNDAY) {
            // Dacă e duminică, săptămâna următoare începe mâine (luni)
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            logger.info("Este duminică - generez pentru săptămâna care începe mâine");
        } else {
            // Pentru orice altă zi, mergi la luni săptămâna viitoare
            int daysUntilNextMonday = Calendar.MONDAY - currentDayOfWeek + 7;
            calendar.add(Calendar.DAY_OF_YEAR, daysUntilNextMonday);
            logger.info("Nu este duminică - generez pentru luni săptămâna viitoare (peste {} zile)", daysUntilNextMonday);
        }

        // Setează la începutul zilei
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
        endCal.add(Calendar.DAY_OF_YEAR, 6); // Luni + 6 zile = Duminică
        return endCal.getTime();
    }

    /**
     * Șterge toate rezervările existente din săptămâna următoare pentru a permite regenerarea
     */
    private void deleteExistingReservationsForNextWeek(Date startDate, Date endDate) {
        logger.info("Ștergerea rezervărilor existente pentru săptămâna {} - {}", startDate, endDate);

        Iterable<Reservation> existingReservations = reservationRepository.findAll();
        List<Reservation> reservationsToDelete = new ArrayList<>();

        for (Reservation reservation : existingReservations) {
            Date reservationDate = reservation.getDate();

            if (reservationDate != null
                    && !reservationDate.before(startDate)
                    && !reservationDate.after(endDate)
                    && "reservation".equals(reservation.getType())) {
                reservationsToDelete.add(reservation);
            }
        }

        if (!reservationsToDelete.isEmpty()) {
            logger.info("Se șterg {} rezervări existente", reservationsToDelete.size());
            reservationRepository.deleteAll(reservationsToDelete);
        }
    }

    /**
     * Generează rezervări automate pentru săptămâna următoare cu plăți automate
     */
    private GenerationResult generateAutomatedBookingsWithPayments(Date startDate) {
        GenerationResult result = new GenerationResult();

        // Obține toate profilurile eligibile
        List<ReservationProfile> eligibleProfiles = getEligibleProfiles();
        logger.info("Profiluri eligibile găsite: {}", eligibleProfiles.size());

        // Sortează profilele după prioritate
        List<ReservationProfile> prioritizedProfiles = prioritizeProfiles(eligibleProfiles);

        // Obține toate sălile active cu programele lor
        List<SportsHall> allHalls = getActiveHallsWithSchedules();
        logger.info("Săli active găsite: {}", allHalls.size());

        // Creează programul disponibil pentru săptămâna următoare
        Map<Long, Map<String, Map<String, SlotAvailability>>> availabilitySchedule =
                createAvailabilityScheduleWithHallPrograms(allHalls, startDate);

        // Marchează sloturile deja rezervate și cele de mentenanță
        markExistingReservations(availabilitySchedule, startDate);

        // Generează rezervările automate cu plăți
        for (ReservationProfile profile : prioritizedProfiles) {
            try {
                ProfileReservationResult profileResult = generateReservationsForProfileWithPayment(
                        profile, availabilitySchedule, startDate);

                result.addProfileResult(profileResult);
                logger.info("Profil {}: {} rezervări generate",
                        profile.getName(), profileResult.getReservations().size());

            } catch (Exception e) {
                logger.error("Eroare la generarea rezervărilor pentru profilul {}: {}",
                        profile.getName(), e.getMessage(), e);
                result.addFailedProfile(profile, e.getMessage());
            }
        }

        return result;
    }

    /**
     * Obține profilurile eligibile pentru generarea automată
     */
    private List<ReservationProfile> getEligibleProfiles() {
        List<ReservationProfile> allProfiles = StreamSupport
                .stream(reservationProfileRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        return allProfiles.stream()
                .filter(profile -> {
                    User user = profile.getUser();
                    return user != null
                            && user.getTeamType() != null
                            && !user.getTeamType().isEmpty()
                            && (user.getAccountStatus().equals("active") || user.getAccountStatus().equals("verified"))
                            && profile.getSelectedHalls() != null
                            && !profile.getSelectedHalls().isEmpty();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obține sălile active cu programele lor
     */
    private List<SportsHall> getActiveHallsWithSchedules() {
        return StreamSupport
                .stream(sportsHallRepository.findAll().spliterator(), false)
                .filter(hall -> hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    /**
     * Obține programul unei săli din baza de date
     */
    private List<Schedule> fetchHallSchedule(Long hallId) {
        try {
            return scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(hallId, true);
        } catch (Exception e) {
            logger.error("Error fetching hall schedule for hall ID {}: {}", hallId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Creează programul de disponibilitate bazat pe programele sălilor
     */
    private Map<Long, Map<String, Map<String, SlotAvailability>>> createAvailabilityScheduleWithHallPrograms(
            List<SportsHall> halls, Date startDate) {

        Map<Long, Map<String, Map<String, SlotAvailability>>> schedule = new HashMap<>();
        List<String> timeSlots = generateTimeSlots();

        for (SportsHall hall : halls) {
            Map<String, Map<String, SlotAvailability>> hallSchedule = new HashMap<>();
            List<Schedule> hallSchedules = fetchHallSchedule(hall.getId());

            // Pentru fiecare zi din săptămâna următoare
            for (int i = 0; i < 7; i++) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                cal.add(Calendar.DAY_OF_YEAR, i);
                String dateKey = formatDate(cal.getTime());
                int dayOfWeek = getDayOfWeekForSchedule(cal.getTime());

                Map<String, SlotAvailability> daySchedule = new HashMap<>();

                // Găsește programul pentru această zi
                Optional<Schedule> dayScheduleOpt = hallSchedules.stream()
                        .filter(s -> s.getDayOfWeek().equals(dayOfWeek))
                        .findFirst();

                // Pentru fiecare slot orar
                for (String timeSlot : timeSlots) {
                    SlotAvailability availability = new SlotAvailability();
                    availability.setTimeSlot(timeSlot);
                    availability.setDate(dateKey);
                    availability.setHallId(hall.getId());

                    if (dayScheduleOpt.isPresent()) {
                        Schedule dayProgramSchedule = dayScheduleOpt.get();
                        if (isSlotInWorkingHours(timeSlot, dayProgramSchedule.getStartTime(), dayProgramSchedule.getEndTime())) {
                            availability.setStatus(SlotStatus.AVAILABLE);
                        } else {
                            availability.setStatus(SlotStatus.OUTSIDE_HOURS);
                        }
                    } else {
                        availability.setStatus(SlotStatus.HALL_CLOSED);
                    }

                    daySchedule.put(timeSlot, availability);
                }

                hallSchedule.put(dateKey, daySchedule);
            }

            schedule.put(hall.getId(), hallSchedule);
        }

        return schedule;
    }

    /**
     * Verifică dacă un slot este în orele de funcționare
     */
    private boolean isSlotInWorkingHours(String timeSlot, String workingStartTime, String workingEndTime) {
        try {
            String slotStartTime = timeSlot.split(" - ")[0];
            String slotEndTime = timeSlot.split(" - ")[1];

            int slotStart = timeToMinutes(slotStartTime);
            int slotEnd = timeToMinutes(slotEndTime);
            int workingStart = timeToMinutes(workingStartTime);
            int workingEnd = timeToMinutes(workingEndTime);

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
     * Obține ziua săptămânii pentru programul sălii (1=Luni, 7=Duminică)
     */
    private int getDayOfWeekForSchedule(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == 1 ? 7 : dayOfWeek - 1; // Convertește din format Calendar la format backend
    }

    /**
     * Generează rezervări pentru un profil cu plată automată
     */
    private ProfileReservationResult generateReservationsForProfileWithPayment(
            ReservationProfile profile,
            Map<Long, Map<String, Map<String, SlotAvailability>>> schedule,
            Date startDate) {

        ProfileReservationResult result = new ProfileReservationResult();
        result.setProfile(profile);

        // Determină numărul maxim de rezervări și bugetul
        int maxBookings = getMaxBookingsPerWeek(profile);
        BigDecimal weeklyBudget = profile.getWeeklyBudget();
        String timeInterval = profile.getTimeInterval();
        User user = profile.getUser();
        String city = profile.getCity();

        // Filtrează sălile în funcție de oraș și profilele selectate
        List<SportsHall> availableHalls = profile.getSelectedHalls().stream()
                .filter(hall -> hall.getCity().equals(city) &&
                        hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                .collect(Collectors.toList());

        if (availableHalls.isEmpty()) {
            logger.warn("Nu există săli disponibile pentru profilul {} în orașul {}",
                    profile.getName(), city);
            return result;
        }

        // Găsește sloturile disponibile pentru profil
        List<AvailableSlot> availableSlots = findAvailableSlotsForProfile(
                schedule, availableHalls, profile, startDate);

        // Selectează rezervările în funcție de buget și prioritate
        List<SelectedReservation> selectedReservations = selectReservationsWithinBudget(
                availableSlots, maxBookings, weeklyBudget);

        if (selectedReservations.isEmpty()) {
            logger.info("Nu s-au putut selecta rezervări pentru profilul {} din cauza bugetului sau disponibilității",
                    profile.getName());
            return result;
        }

        // Calculează costul total
        BigDecimal totalCost = selectedReservations.stream()
                .map(sr -> BigDecimal.valueOf(sr.getHall().getTariff()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Procesează plata
        PaymentResult paymentResult = processPaymentForProfile(profile, totalCost, user);
        result.setPaymentResult(paymentResult);

        if (paymentResult.isSuccessful()) {
            // Creează rezervările
            List<Reservation> reservations = createReservationsFromSelected(
                    selectedReservations, user, paymentResult.getPayment());
            result.setReservations(reservations);

            // Actualizează programul cu rezervările create
            updateScheduleWithReservations(schedule, reservations);

            // Trimite email de confirmare
            sendConfirmationEmail(user, reservations, paymentResult.getPayment());

            logger.info("Profilul {} - {} rezervări create cu plată de {} RON",
                    profile.getName(), reservations.size(), totalCost);
        }

        return result;
    }

    /**
     * Găsește sloturile disponibile pentru un profil
     */
    private List<AvailableSlot> findAvailableSlotsForProfile(
            Map<Long, Map<String, Map<String, SlotAvailability>>> schedule,
            List<SportsHall> halls,
            ReservationProfile profile,
            Date startDate) {

        List<AvailableSlot> availableSlots = new ArrayList<>();

        // Determină intervalul de ore preferat
        TimeInterval preferredInterval = getPreferredTimeInterval(profile.getTimeInterval());
        boolean isSenior = "senior".equals(profile.getAgeCategory()) || "seniori".equals(profile.getAgeCategory());

        // Generează toate combinațiile posibile
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DAY_OF_YEAR, dayOffset);
            String dateKey = formatDate(cal.getTime());

            for (SportsHall hall : halls) {
                if (!schedule.containsKey(hall.getId()) ||
                        !schedule.get(hall.getId()).containsKey(dateKey)) {
                    continue;
                }

                Map<String, SlotAvailability> daySchedule = schedule.get(hall.getId()).get(dateKey);

                for (Map.Entry<String, SlotAvailability> entry : daySchedule.entrySet()) {
                    String timeSlot = entry.getKey();
                    SlotAvailability availability = entry.getValue();

                    if (availability.getStatus() == SlotStatus.AVAILABLE &&
                            isSlotSuitableForProfile(timeSlot, preferredInterval, isSenior)) {

                        AvailableSlot slot = new AvailableSlot();
                        slot.setHall(hall);
                        slot.setDate(cal.getTime());
                        slot.setTimeSlot(timeSlot);
                        slot.setPrice(BigDecimal.valueOf(hall.getTariff()));
                        slot.setPriority(calculateSlotPriority(timeSlot, preferredInterval, isSenior));

                        availableSlots.add(slot);
                    }
                }
            }
        }

        // Sortează după prioritate și apoi randomizează în cadrul aceleiași priorități
        return availableSlots.stream()
                .collect(Collectors.groupingBy(AvailableSlot::getPriority))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, List<AvailableSlot>>comparingByKey().reversed())
                .flatMap(entry -> {
                    List<AvailableSlot> slots = entry.getValue();
                    Collections.shuffle(slots); // Randomizează pentru echitate
                    return slots.stream();
                })
                .collect(Collectors.toList());
    }

    /**
     * Verifică dacă un slot este potrivit pentru profil
     */
    private boolean isSlotSuitableForProfile(String timeSlot, TimeInterval preferredInterval, boolean isSenior) {
        try {
            String startTime = timeSlot.split(" - ")[0];
            int hour = Integer.parseInt(startTime.split(":")[0]);
            int minute = Integer.parseInt(startTime.split(":")[1]);

            // Verifică intervalul preferat
            if (!isInTimeInterval(hour, minute, preferredInterval)) {
                return false;
            }

            // Aplică regula pentru seniori/juniori la ora 14:30
            if (isSenior) {
                // Pentru seniori: până la ora 14:00 (excludem 14:30)
                return hour < 14 || (hour == 14 && minute == 0);
            } else {
                // Pentru juniori: începând cu ora 14:30
                return hour > 14 || (hour == 14 && minute == 30);
            }
        } catch (Exception e) {
            logger.warn("Eroare la verificarea slot-ului {}: {}", timeSlot, e.getMessage());
            return false;
        }
    }

    /**
     * Verifică dacă ora este în intervalul preferat
     */
    private boolean isInTimeInterval(int hour, int minute, TimeInterval interval) {
        int totalMinutes = hour * 60 + minute;
        return totalMinutes >= interval.getStartMinutes() && totalMinutes < interval.getEndMinutes();
    }

    /**
     * Calculează prioritatea unui slot
     */
    private int calculateSlotPriority(String timeSlot, TimeInterval preferredInterval, boolean isSenior) {
        try {
            String startTime = timeSlot.split(" - ")[0];
            int hour = Integer.parseInt(startTime.split(":")[0]);
            int minute = Integer.parseInt(startTime.split(":")[1]);
            int totalMinutes = hour * 60 + minute;

            // Prioritate bazată pe cât de aproape este de mijlocul intervalului preferat
            int intervalCenter = (preferredInterval.getStartMinutes() + preferredInterval.getEndMinutes()) / 2;
            int distance = Math.abs(totalMinutes - intervalCenter);

            // Prioritate inversă (distanță mai mică = prioritate mai mare)
            return 1000 - distance;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Selectează rezervările în funcție de buget
     */
    private List<SelectedReservation> selectReservationsWithinBudget(
            List<AvailableSlot> availableSlots, int maxBookings, BigDecimal weeklyBudget) {

        List<SelectedReservation> selected = new ArrayList<>();
        BigDecimal remainingBudget = weeklyBudget;
        Set<String> usedDateSlots = new HashSet<>(); // Pentru a evita suprapunerile

        for (AvailableSlot slot : availableSlots) {
            if (selected.size() >= maxBookings) {
                break;
            }

            String dateSlotKey = formatDate(slot.getDate()) + "_" + slot.getTimeSlot();
            if (usedDateSlots.contains(dateSlotKey)) {
                continue; // Evită suprapunerile
            }

            if (remainingBudget.compareTo(slot.getPrice()) >= 0) {
                SelectedReservation reservation = new SelectedReservation();
                reservation.setHall(slot.getHall());
                reservation.setDate(slot.getDate());
                reservation.setTimeSlot(slot.getTimeSlot());
                reservation.setPrice(slot.getPrice());

                selected.add(reservation);
                remainingBudget = remainingBudget.subtract(slot.getPrice());
                usedDateSlots.add(dateSlotKey);
            }
        }

        return selected;
    }

    /**
     * Procesează plata pentru un profil
     */
    private PaymentResult processPaymentForProfile(ReservationProfile profile, BigDecimal totalCost, User user) {
        try {
            // Verifică dacă profilul are plata automată activată
            if (profile.getAutoPaymentEnabled() && profile.canProcessAutoPayment(totalCost)) {
                // Procesează plata automată
                return autoPaymentService.processAutoPayment(profile, totalCost, user);
            } else {
                // Creează o plată cash (la fața locului)
                Payment payment = new Payment();
                payment.setUserId(user.getId());
                payment.setTotalAmount(totalCost);
                payment.setPaymentMethod(PaymentMethod.CASH);
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setCurrency("RON");
                payment.setDescription("Rezervare automată - profil: " + profile.getName());

                Payment savedPayment = paymentRepository.save(payment);

                return PaymentResult.success(savedPayment, "cash", "Plată cash programată");
            }
        } catch (Exception e) {
            logger.error("Eroare la procesarea plății pentru profilul {}: {}",
                    profile.getName(), e.getMessage(), e);
            return PaymentResult.failure("Eroare la procesarea plății: " + e.getMessage());
        }
    }

    /**
     * Creează rezervările din selecția făcută
     */
    private List<Reservation> createReservationsFromSelected(
            List<SelectedReservation> selectedReservations, User user, Payment payment) {

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

            // Creează legătura cu plata
            if (payment != null) {
                PaymentReservation paymentReservation = new PaymentReservation();
                paymentReservation.setPayment(payment);
                paymentReservation.setReservation(savedReservation);
                payment.getPaymentReservations().add(paymentReservation);
            }
        }

        if (payment != null && !payment.getPaymentReservations().isEmpty()) {
            paymentRepository.save(payment);
        }

        return reservations;
    }

    /**
     * Actualizează programul cu rezervările create
     */
    private void updateScheduleWithReservations(
            Map<Long, Map<String, Map<String, SlotAvailability>>> schedule,
            List<Reservation> reservations) {

        for (Reservation reservation : reservations) {
            String dateKey = formatDate(reservation.getDate());
            Long hallId = reservation.getHall().getId();
            String timeSlot = reservation.getTimeSlot();

            if (schedule.containsKey(hallId) &&
                    schedule.get(hallId).containsKey(dateKey) &&
                    schedule.get(hallId).get(dateKey).containsKey(timeSlot)) {

                schedule.get(hallId).get(dateKey).get(timeSlot).setStatus(SlotStatus.BOOKED);
            }
        }
    }

    /**
     * Trimite email de confirmare
     */
    private void sendConfirmationEmail(User user, List<Reservation> reservations, Payment payment) {
        try {
            ReservationEmailService.sendReservationConfirmationEmail(
                    mailSender, senderEmail, user, reservations, payment);
        } catch (Exception e) {
            logger.error("Eroare la trimiterea email-ului de confirmare pentru utilizatorul {}: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

    // Metode helper

    private List<ReservationProfile> prioritizeProfiles(List<ReservationProfile> profiles) {
        return profiles.stream()
                .sorted((p1, p2) -> {
                    String age1 = p1.getAgeCategory();
                    String age2 = p2.getAgeCategory();

                    // Verifică pentru seniori (atât "senior" cât și "seniori")
                    boolean isSenior1 = "senior".equals(age1) || "seniori".equals(age1);
                    boolean isSenior2 = "senior".equals(age2) || "seniori".equals(age2);

                    if (isSenior1 && !isSenior2) return -1;
                    if (!isSenior1 && isSenior2) return 1;

                    // Pentru categoriile de juniori (nu-seniori)
                    if (!isSenior1 && !isSenior2) {
                        if (age1.equals("17-18") && !age2.equals("17-18")) return -1;
                        if (!age1.equals("17-18") && age2.equals("17-18")) return 1;
                        if (age1.equals("15-16") && !age2.equals("15-16")) return -1;
                        if (!age1.equals("15-16") && age2.equals("15-16")) return 1;
                    }

                    return p2.getWeeklyBudget().compareTo(p1.getWeeklyBudget());
                })
                .collect(Collectors.toList());
    }

    private int getMaxBookingsPerWeek(ReservationProfile profile) {
        String ageCategory = profile.getAgeCategory();
        switch (ageCategory) {
            case "senior":
            case "seniori": return 6;
            case "17-18": return 5;
            case "15-16": return 4;
            case "0-14": return 3;
            default: return 3;
        }
    }

    private TimeInterval getPreferredTimeInterval(String timeInterval) {
        if (timeInterval == null || timeInterval.isEmpty()) {
            return new TimeInterval(8 * 60, 23 * 60); // 08:00 - 23:00
        }

        switch (timeInterval) {
            case "morning": return new TimeInterval(8 * 60, 12 * 60);   // 08:00 - 12:00
            case "afternoon": return new TimeInterval(12 * 60, 17 * 60); // 12:00 - 17:00
            case "evening": return new TimeInterval(17 * 60, 23 * 60);   // 17:00 - 23:00
            default: return new TimeInterval(8 * 60, 23 * 60);
        }
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
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return String.format("%d-%02d-%02d", year, month, day);
    }

    private void markExistingReservations(
            Map<Long, Map<String, Map<String, SlotAvailability>>> schedule, Date startDate) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DAY_OF_YEAR, 6);
        Date endDate = cal.getTime();

        Iterable<Reservation> existingReservations = reservationRepository.findAll();

        for (Reservation reservation : existingReservations) {
            Date reservationDate = reservation.getDate();

            if (reservationDate == null || reservationDate.before(startDate) || reservationDate.after(endDate)) {
                continue;
            }

            Long hallId = reservation.getHall().getId();
            String dateKey = formatDate(reservationDate);
            String timeSlot = reservation.getTimeSlot();

            if (schedule.containsKey(hallId) &&
                    schedule.get(hallId).containsKey(dateKey) &&
                    schedule.get(hallId).get(dateKey).containsKey(timeSlot)) {

                SlotAvailability availability = schedule.get(hallId).get(dateKey).get(timeSlot);
                if ("maintenance".equals(reservation.getType())) {
                    availability.setStatus(SlotStatus.MAINTENANCE);
                } else if ("reservation".equals(reservation.getType()) &&
                        !"CANCELLED".equals(reservation.getStatus())) {
                    availability.setStatus(SlotStatus.BOOKED);
                }
            }
        }
    }

    // Classes pentru rezultate și structuri de date

    private static class GenerationResult {
        private List<Reservation> reservations = new ArrayList<>();
        private List<Payment> payments = new ArrayList<>();
        private int autoPaymentsSuccessful = 0;
        private int autoPaymentsFailed = 0;
        private List<String> errors = new ArrayList<>();

        public void addProfileResult(ProfileReservationResult profileResult) {
            if (profileResult.getReservations() != null) {
                reservations.addAll(profileResult.getReservations());
            }
            if (profileResult.getPaymentResult() != null &&
                    profileResult.getPaymentResult().getPayment() != null) {
                payments.add(profileResult.getPaymentResult().getPayment());
                if (profileResult.getPaymentResult().isSuccessful()) {
                    autoPaymentsSuccessful++;
                } else {
                    autoPaymentsFailed++;
                }
            }
        }

        public void addFailedProfile(ReservationProfile profile, String error) {
            autoPaymentsFailed++;
            errors.add("Profil " + profile.getName() + ": " + error);
        }

        // Getters
        public List<Reservation> getReservations() { return reservations; }
        public List<Payment> getPayments() { return payments; }
        public int getTotalReservations() { return reservations.size(); }
        public int getAutoPaymentsSuccessful() { return autoPaymentsSuccessful; }
        public int getAutoPaymentsFailed() { return autoPaymentsFailed; }
        public List<String> getErrors() { return errors; }
    }

    private static class ProfileReservationResult {
        private ReservationProfile profile;
        private List<Reservation> reservations = new ArrayList<>();
        private PaymentResult paymentResult;

        // Getters și setters
        public ReservationProfile getProfile() { return profile; }
        public void setProfile(ReservationProfile profile) { this.profile = profile; }
        public List<Reservation> getReservations() { return reservations; }
        public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }
        public PaymentResult getPaymentResult() { return paymentResult; }
        public void setPaymentResult(PaymentResult paymentResult) { this.paymentResult = paymentResult; }
    }

    private static class SlotAvailability {
        private String timeSlot;
        private String date;
        private Long hallId;
        private SlotStatus status;

        // Getters și setters
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Long getHallId() { return hallId; }
        public void setHallId(Long hallId) { this.hallId = hallId; }
        public SlotStatus getStatus() { return status; }
        public void setStatus(SlotStatus status) { this.status = status; }
    }

    private enum SlotStatus {
        AVAILABLE, BOOKED, MAINTENANCE, OUTSIDE_HOURS, HALL_CLOSED
    }

    private static class AvailableSlot {
        private SportsHall hall;
        private Date date;
        private String timeSlot;
        private BigDecimal price;
        private int priority;

        // Getters și setters
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

    private static class SelectedReservation {
        private SportsHall hall;
        private Date date;
        private String timeSlot;
        private BigDecimal price;

        // Getters și setters
        public SportsHall getHall() { return hall; }
        public void setHall(SportsHall hall) { this.hall = hall; }
        public Date getDate() { return date; }
        public void setDate(Date date) { this.date = date; }
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    private static class TimeInterval {
        private int startMinutes;
        private int endMinutes;

        public TimeInterval(int startMinutes, int endMinutes) {
            this.startMinutes = startMinutes;
            this.endMinutes = endMinutes;
        }

        public int getStartMinutes() { return startMinutes; }
        public int getEndMinutes() { return endMinutes; }
    }
}