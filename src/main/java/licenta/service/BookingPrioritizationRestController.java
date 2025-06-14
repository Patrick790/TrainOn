package licenta.service;

import jakarta.transaction.Transactional;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/booking-prioritization")
@Transactional
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

    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> diagnosticCheck() {
        Map<String, Object> diagnostic = new HashMap<>();

        try {
            // 1. Verifică profilurile
            List<ReservationProfile> allProfiles = (List<ReservationProfile>) reservationProfileRepository.findAll();
            diagnostic.put("totalProfiles", allProfiles.size());

            List<Map<String, Object>> profilesInfo = new ArrayList<>();
            for (ReservationProfile profile : allProfiles) {
                Map<String, Object> profileInfo = new HashMap<>();
                profileInfo.put("id", profile.getId());
                profileInfo.put("name", profile.getName());
                profileInfo.put("hasUser", profile.getUser() != null);

                if (profile.getUser() != null) {
                    profileInfo.put("userEmail", profile.getUser().getEmail());
                    profileInfo.put("userTeamType", profile.getUser().getTeamType());
                    profileInfo.put("userAccountStatus", profile.getUser().getAccountStatus());
                }

                profileInfo.put("selectedHallsCount",
                        profile.getSelectedHalls() != null ? profile.getSelectedHalls().size() : 0);
                profileInfo.put("weeklyBudget", profile.getWeeklyBudget());
                profileInfo.put("city", profile.getCity());
                profileInfo.put("sport", profile.getSport());

                profilesInfo.add(profileInfo);
            }
            diagnostic.put("profiles", profilesInfo);

            // 2. Verifică profilurile eligibile
            List<ReservationProfile> eligibleProfiles = getEligibleProfiles();
            diagnostic.put("eligibleProfilesCount", eligibleProfiles.size());

            // 3. Verifică sălile active
            List<SportsHall> activeHalls = getActiveHallsWithSchedules();
            diagnostic.put("activeHallsCount", activeHalls.size());

            List<Map<String, Object>> hallsInfo = new ArrayList<>();
            for (SportsHall hall : activeHalls) {
                Map<String, Object> hallInfo = new HashMap<>();
                hallInfo.put("id", hall.getId());
                hallInfo.put("name", hall.getName());
                hallInfo.put("city", hall.getCity());
                hallInfo.put("status", hall.getStatus());

                List<Schedule> schedules = fetchHallSchedule(hall.getId());
                hallInfo.put("schedulesCount", schedules.size());

                hallsInfo.add(hallInfo);
            }
            diagnostic.put("halls", hallsInfo);

            // 4. Verifică datele pentru generare
            Date nextWeekStart = calculateNextWeekStart();
            Date nextWeekEnd = calculateWeekEnd(nextWeekStart);

            diagnostic.put("nextWeekStart", nextWeekStart.toString());
            diagnostic.put("nextWeekEnd", nextWeekEnd.toString());
            diagnostic.put("currentTime", new Date().toString());
            diagnostic.put("timezone", TimeZone.getDefault().getID());

            // 5. Test de generare pentru primul profil eligibil
            if (!eligibleProfiles.isEmpty()) {
                ReservationProfile testProfile = eligibleProfiles.get(0);
                diagnostic.put("testProfileName", testProfile.getName());

                // Verifică sălile selectate pentru acest profil
                List<String> selectedHallNames = testProfile.getSelectedHalls().stream()
                        .map(SportsHall::getName)
                        .collect(Collectors.toList());
                diagnostic.put("testProfileSelectedHalls", selectedHallNames);

                // Verifică disponibilitatea
                Map<Long, Map<String, Map<String, SlotAvailability>>> schedule =
                        createAvailabilityScheduleWithHallPrograms(activeHalls, nextWeekStart);

                List<AvailableSlot> availableSlots = findAvailableSlotsForProfile(
                        schedule,
                        testProfile.getSelectedHalls().stream()
                                .filter(hall -> hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                                .collect(Collectors.toList()),
                        testProfile,
                        nextWeekStart
                );

                diagnostic.put("testProfileAvailableSlots", availableSlots.size());
            }

            diagnostic.put("success", true);

        } catch (Exception e) {
            diagnostic.put("success", false);
            diagnostic.put("error", e.getMessage());
            diagnostic.put("stackTrace", Arrays.toString(e.getStackTrace()));
            logger.error("Diagnostic check failed", e);
        }

        return ResponseEntity.ok(diagnostic);
    }

    /**
     * Endpoint pentru generarea automată a rezervărilor cu plăți automate
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateBookings() {
        try {
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
        // IMPORTANT: Folosește timezone-ul României explicit
        TimeZone romaniaTimeZone = TimeZone.getTimeZone("Europe/Bucharest");
        Calendar calendar = Calendar.getInstance(romaniaTimeZone);

        // Log pentru debug
        logger.info("DEBUG: Current date/time in Romania: {}", calendar.getTime());
        logger.info("DEBUG: Current day of week: {} (1=Sunday, 2=Monday, etc.)", calendar.get(Calendar.DAY_OF_WEEK));

        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (currentDayOfWeek == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            logger.info("DEBUG: Este duminică - generez pentru săptămâna care începe mâine");
        } else {
            int daysUntilNextMonday = Calendar.MONDAY - currentDayOfWeek + 7;
            calendar.add(Calendar.DAY_OF_YEAR, daysUntilNextMonday);
            logger.info("DEBUG: Nu este duminică - generez pentru luni săptămâna viitoare (peste {} zile)", daysUntilNextMonday);
        }

        // Setează la începutul zilei
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date nextWeekStart = calendar.getTime();
        logger.info("DEBUG: Next week start date: {}", nextWeekStart);

        return nextWeekStart;
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
        logger.info("DEBUG: Ștergerea rezervărilor existente pentru săptămâna {} - {}", startDate, endDate);

        try {
            // Folosește @Query pentru ștergere mai eficientă
            List<Reservation> reservationsToDelete = StreamSupport
                    .stream(reservationRepository.findAll().spliterator(), false)
                    .filter(reservation -> {
                        Date reservationDate = reservation.getDate();
                        return reservationDate != null
                                && !reservationDate.before(startDate)
                                && !reservationDate.after(endDate)
                                && "reservation".equals(reservation.getType())
                                && !"CANCELLED".equals(reservation.getStatus());
                    })
                    .collect(Collectors.toList());

            if (!reservationsToDelete.isEmpty()) {
                logger.info("DEBUG: Se șterg {} rezervări existente", reservationsToDelete.size());

                // Log primele câteva pentru debug
                reservationsToDelete.stream().limit(5).forEach(r ->
                        logger.info("DEBUG: Șterg rezervarea: Hall {}, Date {}, TimeSlot {}",
                                r.getHall().getName(), r.getDate(), r.getTimeSlot())
                );

                reservationRepository.deleteAll(reservationsToDelete);
            } else {
                logger.info("DEBUG: Nu există rezervări de șters pentru perioada specificată");
            }
        } catch (Exception e) {
            logger.error("ERROR: Eroare la ștergerea rezervărilor existente", e);
            throw new RuntimeException("Eroare la ștergerea rezervărilor existente: " + e.getMessage());
        }
    }

    /**
     * Generează rezervări automate pentru săptămâna următoare cu plăți automate
     */
    private GenerationResult generateAutomatedBookingsWithPayments(Date startDate) {
        GenerationResult result = new GenerationResult();

        // Obține toate profilurile eligibile
        List<ReservationProfile> eligibleProfiles = getEligibleProfiles();
        logger.info("DEBUG: Profiluri eligibile găsite: {}", eligibleProfiles.size());

        // Log detaliat pentru fiecare profil
        for (ReservationProfile profile : eligibleProfiles) {
            logger.info("DEBUG: Profil {} - User: {}, TeamType: {}, AccountStatus: {}, SelectedHalls: {}",
                    profile.getName(),
                    profile.getUser() != null ? profile.getUser().getEmail() : "NULL",
                    profile.getUser() != null ? profile.getUser().getTeamType() : "NULL",
                    profile.getUser() != null ? profile.getUser().getAccountStatus() : "NULL",
                    profile.getSelectedHalls() != null ? profile.getSelectedHalls().size() : 0
            );
        }

        // Sortează profilele după prioritate
        List<ReservationProfile> prioritizedProfiles = prioritizeProfiles(eligibleProfiles);
        logger.info("DEBUG: Profiluri după prioritizare: {}", prioritizedProfiles.size());

        // Obține toate sălile active cu programele lor
        List<SportsHall> allHalls = getActiveHallsWithSchedules();
        logger.info("DEBUG: Săli active găsite: {}", allHalls.size());

        // Log pentru fiecare sală
        for (SportsHall hall : allHalls) {
            logger.info("DEBUG: Sală {} - Status: {}, City: {}",
                    hall.getName(),
                    hall.getStatus(),
                    hall.getCity()
            );
        }

        // Creează programul disponibil pentru săptămâna următoare
        Map<Long, Map<String, Map<String, SlotAvailability>>> availabilitySchedule =
                createAvailabilityScheduleWithHallPrograms(allHalls, startDate);

        logger.info("DEBUG: Availability schedule created for {} halls", availabilitySchedule.size());

        // Marchează sloturile deja rezervate și cele de mentenanță
        markExistingReservations(availabilitySchedule, startDate);

        // Generează rezervările automate cu plăți
        for (ReservationProfile profile : prioritizedProfiles) {
            try {
                logger.info("DEBUG: Procesare profil: {}", profile.getName());

                ProfileReservationResult profileResult = generateReservationsForProfileWithPayment(
                        profile, availabilitySchedule, startDate);

                result.addProfileResult(profileResult);

                logger.info("DEBUG: Profil {} - Rezervări generate: {}, Payment success: {}",
                        profile.getName(),
                        profileResult.getReservations().size(),
                        profileResult.getPaymentResult() != null ? profileResult.getPaymentResult().isSuccessful() : "NULL"
                );

            } catch (Exception e) {
                logger.error("DEBUG: Eroare la generarea rezervărilor pentru profilul {}: {}",
                        profile.getName(), e.getMessage(), e);
                result.addFailedProfile(profile, e.getMessage());
            }
        }

        logger.info("DEBUG: Total rezervări generate: {}", result.getTotalReservations());
        return result;
    }

    // În metoda getEligibleProfiles - adaugă debugging
    private List<ReservationProfile> getEligibleProfiles() {
        List<ReservationProfile> allProfiles = StreamSupport
                .stream(reservationProfileRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        logger.info("DEBUG: Total profiluri în DB: {}", allProfiles.size());

        for (ReservationProfile profile : allProfiles) {
            if (profile.getUser() != null) {
                // Accesează câmpurile pentru a forța încărcarea
                profile.getUser().getEmail();
                profile.getUser().getTeamType();
                profile.getUser().getAccountStatus();
            }
            if (profile.getSelectedHalls() != null) {
                profile.getSelectedHalls().size(); // Forțează încărcarea colecției
            }
        }

        return allProfiles.stream()
                .filter(profile -> {
                    User user = profile.getUser();
                    boolean hasUser = user != null;
                    boolean hasTeamType = hasUser && user.getTeamType() != null && !user.getTeamType().isEmpty();
                    boolean hasValidStatus = hasUser && (user.getAccountStatus().equals("active") || user.getAccountStatus().equals("verified"));
                    boolean hasSelectedHalls = profile.getSelectedHalls() != null && !profile.getSelectedHalls().isEmpty();

                    logger.info("DEBUG: Profil {} - hasUser: {}, hasTeamType: {}, hasValidStatus: {}, hasSelectedHalls: {}",
                            profile.getName(), hasUser, hasTeamType, hasValidStatus, hasSelectedHalls);

                    return hasUser && hasTeamType && hasValidStatus && hasSelectedHalls;
                })
                .collect(Collectors.toList());
    }

    /**
     * Obține sălile active cu programele lor
     */
    private List<SportsHall> getActiveHallsWithSchedules() {
        List<SportsHall> activeHalls = StreamSupport
                .stream(sportsHallRepository.findAll().spliterator(), false)
                .filter(hall -> hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                .collect(Collectors.toList());

        logger.info("DEBUG: Total active halls: {}", activeHalls.size());

        // Verifică că fiecare sală are program
        for (SportsHall hall : activeHalls) {
            List<Schedule> schedules = fetchHallSchedule(hall.getId());
            if (schedules.isEmpty()) {
                logger.warn("WARNING: Hall {} ({}) has no schedules defined!",
                        hall.getName(), hall.getId());
            } else {
                logger.info("DEBUG: Hall {} has {} schedule entries",
                        hall.getName(), schedules.size());
            }
        }

        return activeHalls;
    }

    /**
     * Obține programul unei săli din baza de date
     */
    private List<Schedule> fetchHallSchedule(Long hallId) {
        try {
            List<Schedule> schedules = scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(hallId, true);
            logger.info("DEBUG: Hall {} - Found {} schedules", hallId, schedules.size());
            return schedules;
        } catch (Exception e) {
            logger.error("DEBUG: Error fetching hall schedule for hall ID {}: {}", hallId, e.getMessage(), e);
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

        logger.info("DEBUG: Finding slots for profile {} - Checking {} halls", profile.getName(), halls.size());

        // Determină intervalul de ore preferat
        TimeInterval preferredInterval = getPreferredTimeInterval(profile.getTimeInterval());
        boolean isSenior = "senior".equals(profile.getAgeCategory()) || "seniori".equals(profile.getAgeCategory());

        logger.info("DEBUG: Profile {} - TimeInterval: {}, IsSenior: {}",
                profile.getName(), profile.getTimeInterval(), isSenior);

        // Generează toate combinațiile posibile
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DAY_OF_YEAR, dayOffset);
            String dateKey = formatDate(cal.getTime());

            for (SportsHall hall : halls) {
                if (!schedule.containsKey(hall.getId()) ||
                        !schedule.get(hall.getId()).containsKey(dateKey)) {
                    logger.warn("DEBUG: Schedule missing for hall {} on date {}", hall.getId(), dateKey);
                    continue;
                }

                Map<String, SlotAvailability> daySchedule = schedule.get(hall.getId()).get(dateKey);
                int availableSlotsForDay = 0;

                for (Map.Entry<String, SlotAvailability> entry : daySchedule.entrySet()) {
                    String timeSlot = entry.getKey();
                    SlotAvailability availability = entry.getValue();

                    if (availability.getStatus() == SlotStatus.AVAILABLE &&
                            isSlotSuitableForProfile(timeSlot, preferredInterval, isSenior)) {
                        availableSlotsForDay++;
                        AvailableSlot slot = new AvailableSlot();
                        slot.setHall(hall);
                        slot.setDate(cal.getTime());
                        slot.setTimeSlot(timeSlot);
                        slot.setPrice(BigDecimal.valueOf(hall.getTariff()));
                        slot.setPriority(calculateSlotPriority(timeSlot, preferredInterval, isSenior));
                        availableSlots.add(slot);
                    }
                }

                if (availableSlotsForDay > 0) {
                    logger.info("DEBUG: Hall {} on {} - {} available slots",
                            hall.getName(), dateKey, availableSlotsForDay);
                }
            }
        }

        logger.info("DEBUG: Profile {} - Total available slots found: {}",
                profile.getName(), availableSlots.size());

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
        // Folosește SimpleDateFormat cu timezone explicit
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Bucharest"));
        String formatted = sdf.format(date);
        logger.debug("DEBUG: Formatted date {} to {}", date, formatted);
        return formatted;
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