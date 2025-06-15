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

            // OPTIMIZARE CRITICĂ: Response minimalist pentru a evita crash-ul browser-ului
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rezervările au fost generate cu succes.");
            response.put("count", result.getTotalReservations());
            response.put("successfulProfiles", result.getSuccessfulProfiles());

            // NU trimite lista completă de rezervări - prea mare pentru browser
            // response.put("reservations", result.getReservations()); // COMENTAT

            // Trimite doar un sumar compact
            List<Map<String, Object>> profileSummary = result.getProfileResults().stream()
                    .map(this::createProfileSummary)
                    .collect(Collectors.toList());

            response.put("profileResults", profileSummary);

            logger.info("=== GENERARE COMPLETĂ: {} rezervări pentru {} profiluri ===",
                    result.getTotalReservations(), result.getSuccessfulProfiles());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("EROARE CRITICĂ la generarea rezervărilor", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Eroare la generarea rezervărilor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private Map<String, Object> createProfileSummary(ProfileResult profileResult) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("profileName", profileResult.getProfile().getName());
        summary.put("reservationCount", profileResult.getReservations().size());
        summary.put("success", profileResult.isSuccess());

        if (!profileResult.isSuccess() && profileResult.getError() != null) {
            summary.put("error", profileResult.getError());
        }

        // Doar primele 3 rezervări pentru sumar
        if (!profileResult.getReservations().isEmpty()) {
            List<Map<String, Object>> reservationSummary = profileResult.getReservations().stream()
                    .limit(3) // Doar primele 3 pentru sumar
                    .map(this::createReservationSummary)
                    .collect(Collectors.toList());
            summary.put("sampleReservations", reservationSummary);
        }

        return summary;
    }

    private Map<String, Object> createReservationSummary(Reservation reservation) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("date", formatDate(reservation.getDate()));
        summary.put("timeSlot", reservation.getTimeSlot());
        summary.put("hallName", reservation.getHall().getName());
        summary.put("price", reservation.getPrice());
        return summary;
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
     * Metoda principală de generare a rezervărilor - OPTIMIZATĂ
     */
    private GenerationResult generateAutomatedBookings() {
        GenerationResult result = new GenerationResult();
        long startTime = System.currentTimeMillis();

        try {
            logger.info("=== ÎNCEPEREA GENERĂRII AUTOMATE A REZERVĂRILOR ===");

            // 1. Calculează perioada pentru săptămâna următoare
            Date nextMonday = calculateNextWeekStart();
            Date nextSunday = calculateWeekEnd(nextMonday);

            logger.info("=== GENERARE PENTRU SĂPTĂMÂNA: {} - {} ===",
                    formatDate(nextMonday), formatDate(nextSunday));

            // 2. Șterge rezervările existente - OPTIMIZAT
            deleteExistingReservationsForNextWeek(nextMonday, nextSunday);

            // 3. Obține și prioritizează profilurile - LIMITAT
            List<ReservationProfile> eligibleProfiles = getEligibleProfiles();

            if (eligibleProfiles.isEmpty()) {
                logger.warn("Nu există profiluri eligibile pentru generarea rezervărilor!");
                return result;
            }

            // LIMITARE CRITICĂ: Max 20 de profiluri pentru a evita timeout-ul
            if (eligibleProfiles.size() > 20) {
                logger.warn("Se limitează la 20 profiluri din {} disponibile", eligibleProfiles.size());
                eligibleProfiles = eligibleProfiles.subList(0, 20);
            }

            List<ReservationProfile> prioritizedProfiles = prioritizeProfiles(eligibleProfiles);
            logger.info("Profiluri eligibile procesate: {}", prioritizedProfiles.size());

            // 4. Creează programul disponibil - OPTIMIZAT
            WeeklySchedule weeklySchedule = createWeeklySchedule(nextMonday);

            // 5. Procesează profilurile cu timeout strict
            int processedCount = 0;
            for (ReservationProfile profile : prioritizedProfiles) {
                try {
                    long profileStart = System.currentTimeMillis();

                    ProfileResult profileResult = generateReservationsForProfile(profile, weeklySchedule, nextMonday);
                    result.addProfileResult(profileResult);

                    long profileDuration = System.currentTimeMillis() - profileStart;
                    processedCount++;

                    logger.info("Profil {}/{}: '{}' - {} rezervări în {}ms",
                            processedCount, prioritizedProfiles.size(),
                            profile.getName(), profileResult.getReservations().size(), profileDuration);

                    // TIMEOUT PER PROFIL: 8 secunde maxim
                    if (profileDuration > 8000) {
                        logger.warn("Profil '{}' a luat prea mult timp, se oprește", profile.getName());
                        break;
                    }

                    // TIMEOUT GLOBAL: 90 secunde maxim
                    long totalDuration = System.currentTimeMillis() - startTime;
                    if (totalDuration > 90000) {
                        logger.warn("Procesul a luat {}ms, se oprește pentru a evita timeout-ul", totalDuration);
                        break;
                    }

                } catch (Exception e) {
                    logger.error("Eroare la profilul '{}': {}", profile.getName(), e.getMessage());
                    result.addFailedProfile(profile, e.getMessage());
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("=== GENERARE COMPLETĂ: {} rezervări în {}ms ===", result.getTotalReservations(), totalTime);

        } catch (Exception e) {
            logger.error("Eroare critică în procesul de generare", e);
            throw new RuntimeException("Eroare la generarea rezervărilor: " + e.getMessage());
        }

        return result;
    }

    /**
     * Verifică că nu mai există rezervări în săptămâna specificată
     */
    private void verifyNoReservationsInWeek(Date startDate, Date endDate) {
        try {
            List<Reservation> allReservations = (List<Reservation>) reservationRepository.findAll();
            long reservationsInWeek = allReservations.stream()
                    .filter(r -> isReservationInTargetWeek(r, startDate, endDate))
                    .count();

            if (reservationsInWeek > 0) {
                logger.warn("Încă există {} rezervări în săptămâna țintă după ștergere", reservationsInWeek);
            } else {
                logger.info("✓ Verificare completă: Nu există rezervări în săptămâna țintă");
            }
        } catch (Exception e) {
            logger.warn("Eroare la verificarea rezervărilor: {}", e.getMessage());
        }
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
     * Șterge rezervările existente din săptămâna următoare - OPTIMIZAT
     */
    private void deleteExistingReservationsForNextWeek(Date startDate, Date endDate) {
        logger.info("Ștergerea rezervărilor pentru săptămâna {} - {}", formatDate(startDate), formatDate(endDate));

        try {
            List<Reservation> allReservations = (List<Reservation>) reservationRepository.findAll();
            List<Long> idsToDelete = allReservations.stream()
                    .filter(r -> isReservationInTargetWeek(r, startDate, endDate))
                    .map(Reservation::getId)
                    .limit(500) // Limitează pentru siguranță
                    .collect(Collectors.toList());

            if (!idsToDelete.isEmpty()) {
                logger.info("Se șterg {} rezervări", idsToDelete.size());

                // Șterge în batch-uri mici pentru performanță
                int batchSize = 20;
                for (int i = 0; i < idsToDelete.size(); i += batchSize) {
                    List<Long> batch = idsToDelete.subList(i, Math.min(i + batchSize, idsToDelete.size()));
                    for (Long id : batch) {
                        try {
                            reservationRepository.deleteById(id);
                        } catch (Exception e) {
                            logger.debug("Nu s-a putut șterge rezervarea {}: {}", id, e.getMessage());
                        }
                    }
                }

                logger.info("Ștergerea completă pentru {} rezervări", idsToDelete.size());
            }
        } catch (Exception e) {
            logger.error("Eroare la ștergerea rezervărilor", e);
            // Nu aruncă excepție, continuă procesul
        }
    }

    /**
     * Verifică dacă o rezervare este în săptămâna specificată
     */
    private boolean isReservationInTargetWeek(Reservation reservation, Date startDate, Date endDate) {
        if (reservation == null || reservation.getDate() == null) {
            return false;
        }

        Date reservationDate = reservation.getDate();

        // Verifică dacă rezervarea este de tip "reservation" (nu maintenance)
        if (!"reservation".equals(reservation.getType())) {
            return false;
        }

        // Verifică dacă data rezervării este în intervalul specificat
        return !reservationDate.before(startDate) && !reservationDate.after(endDate);
    }

    /**
     * Obține profilurile eligibile pentru generare - OPTIMIZAT
     */
    private List<ReservationProfile> getEligibleProfiles() {
        List<ReservationProfile> allProfiles = (List<ReservationProfile>) reservationProfileRepository.findAll();

        return allProfiles.stream()
                .filter(this::isProfileEligible)
                .limit(30) // Limitează din start
                .collect(Collectors.toList());
    }

    /**
     * Verifică dacă un profil este eligibil pentru generarea automată - OPTIMIZAT
     */
    private boolean isProfileEligible(ReservationProfile profile) {
        try {
            User user = profile.getUser();
            return user != null
                    && user.getTeamType() != null
                    && !user.getTeamType().trim().isEmpty()
                    && ("active".equals(user.getAccountStatus()) || "verified".equals(user.getAccountStatus()))
                    && profile.getSelectedHalls() != null
                    && !profile.getSelectedHalls().isEmpty()
                    && profile.getWeeklyBudget() != null
                    && profile.getWeeklyBudget().compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            return false;
        }
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

        return switch (ageCategory.toLowerCase().trim()) {
            case "seniori", "senior" -> 4;
            case "17-18" -> 3;
            case "15-16" -> 2;
            case "0-14" -> 1;
            default -> 0;
        };
    }

    /**
     * Creează programul săptămânal disponibil - OPTIMIZAT
     */
    private WeeklySchedule createWeeklySchedule(Date startDate) {
        WeeklySchedule schedule = new WeeklySchedule();
        List<SportsHall> activeHalls = getActiveHalls();

        // Limitează numărul de săli pentru performanță
        if (activeHalls.size() > 15) {
            activeHalls = activeHalls.subList(0, 15);
        }

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
     * Creează programul pentru o zi - OPTIMIZAT
     */
    private DaySchedule createDaySchedule(Date date, List<SportsHall> halls) {
        DaySchedule daySchedule = new DaySchedule();
        int dayOfWeek = getDayOfWeekForSchedule(date);

        for (SportsHall hall : halls) {
            try {
                HallDaySchedule hallSchedule = createHallDaySchedule(hall, dayOfWeek);
                daySchedule.addHallSchedule(hall.getId(), hallSchedule);
            } catch (Exception e) {
                logger.debug("Eroare la crearea programului pentru sala {}: {}", hall.getId(), e.getMessage());
            }
        }

        return daySchedule;
    }

    /**
     * Creează programul unei săli pentru o zi - OPTIMIZAT
     */
    private HallDaySchedule createHallDaySchedule(SportsHall hall, int dayOfWeek) {
        HallDaySchedule hallSchedule = new HallDaySchedule();

        try {
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
        } catch (Exception e) {
            logger.debug("Eroare la crearea programului sălii {}: {}", hall.getId(), e.getMessage());
        }

        return hallSchedule;
    }

    /**
     * Determină statusul unui slot - OPTIMIZAT
     */
    private SlotStatus determineSlotStatus(String timeSlot, Optional<Schedule> daySchedule) {
        if (daySchedule.isEmpty()) {
            return SlotStatus.HALL_CLOSED;
        }

        try {
            Schedule schedule = daySchedule.get();
            if (isSlotInWorkingHours(timeSlot, schedule.getStartTime(), schedule.getEndTime())) {
                return SlotStatus.AVAILABLE;
            } else {
                return SlotStatus.OUTSIDE_HOURS;
            }
        } catch (Exception e) {
            return SlotStatus.HALL_CLOSED;
        }
    }

    /**
     * Verifică dacă un slot este în orele de funcționare - OPTIMIZAT
     */
    private boolean isSlotInWorkingHours(String timeSlot, String startTime, String endTime) {
        try {
            String slotStartTime = timeSlot.split(" - ")[0];
            int slotStart = timeToMinutes(slotStartTime);
            int workingStart = timeToMinutes(startTime);
            int workingEnd = timeToMinutes(endTime);

            return slotStart >= workingStart && slotStart < workingEnd;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convertește ora în minute - OPTIMIZAT
     */
    private int timeToMinutes(String timeString) {
        try {
            String[] parts = timeString.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Generează rezervări pentru un profil - OPTIMIZAT
     */
    private ProfileResult generateReservationsForProfile(ReservationProfile profile, WeeklySchedule weeklySchedule, Date startDate) {
        ProfileResult result = new ProfileResult(profile);

        try {
            // Parametrii pentru profil
            int maxBookings = getMaxBookingsForProfile(profile);
            BigDecimal budget = profile.getWeeklyBudget();
            TimeInterval preferredInterval = getPreferredTimeInterval(profile.getTimeInterval());
            boolean isSenior = isSeniorCategory(profile.getAgeCategory());

            // Găsește sloturile disponibile pentru profil - OPTIMIZAT
            List<AvailableSlot> availableSlots = findAvailableSlotsForProfile(
                    profile, weeklySchedule, startDate, preferredInterval, isSenior);

            if (availableSlots.isEmpty()) {
                logger.debug("Nu există slot-uri disponibile pentru profilul '{}'", profile.getName());
                return result;
            }

            // Aplică distribuția echilibrată doar dacă avem multe opțiuni
            if (availableSlots.size() > maxBookings * 3) {
                availableSlots = balanceDistribution(availableSlots, maxBookings * 2);
            }

            // Selectează rezervările în funcție de buget și numărul maxim
            List<SelectedReservation> selectedReservations = selectReservationsWithinBudget(
                    availableSlots, maxBookings, budget);

            if (selectedReservations.isEmpty()) {
                logger.debug("Nu s-au putut selecta rezervări pentru profilul '{}'", profile.getName());
                return result;
            }

            // Creează rezervările
            List<Reservation> reservations = createReservationsFromSelected(selectedReservations, profile.getUser());
            result.setReservations(reservations);

            // Actualizează programul cu rezervările create
            updateScheduleWithReservations(weeklySchedule, reservations);

            // Trimite email de confirmare (fără a bloca procesul)
            try {
                sendConfirmationEmail(profile.getUser(), reservations);
            } catch (Exception e) {
                logger.debug("Eroare la trimiterea email-ului pentru {}: {}", profile.getName(), e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Eroare la generarea rezervărilor pentru profilul '{}': {}", profile.getName(), e.getMessage());
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * Găsește sloturile disponibile pentru un profil - ULTRA OPTIMIZAT
     */
    private List<AvailableSlot> findAvailableSlotsForProfile(
            ReservationProfile profile, WeeklySchedule weeklySchedule, Date startDate,
            TimeInterval preferredInterval, boolean isSenior) {

        List<AvailableSlot> availableSlots = new ArrayList<>();
        List<SportsHall> profileHalls = getProfileHalls(profile);

        if (profileHalls.isEmpty()) {
            return availableSlots;
        }

        int maxSlots = getMaxBookingsForProfile(profile) * 15; // Limitează căutarea

        // Colectează toate slot-urile disponibile
        outerLoop:
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

                        // EARLY EXIT pentru performanță
                        if (availableSlots.size() >= maxSlots) {
                            break outerLoop;
                        }
                    }
                }
            }
        }

        // Aplică diversificarea DOAR dacă avem suficiente slot-uri
        if (availableSlots.size() > getMaxBookingsForProfile(profile) * 4) {
            return diversifySlotSelection(availableSlots);
        } else {
            // Pentru liste mici, doar amestecă
            Collections.shuffle(availableSlots, new Random());
            return availableSlots;
        }
    }

    /**
     * Diversifică selecția de slot-uri - OPTIMIZAT
     */
    private List<AvailableSlot> diversifySlotSelection(List<AvailableSlot> availableSlots) {
        if (availableSlots.isEmpty()) {
            return availableSlots;
        }

        // LIMITARE CRITICĂ: Nu procesa liste foarte mari
        if (availableSlots.size() > 200) {
            Collections.shuffle(availableSlots, new Random());
            return availableSlots.subList(0, 200);
        }

        try {
            // Grupează slot-urile după prioritate
            Map<Integer, List<AvailableSlot>> priorityGroups = availableSlots.stream()
                    .collect(Collectors.groupingBy(AvailableSlot::getPriority));

            List<AvailableSlot> diversifiedSlots = new ArrayList<>();
            Random random = new Random();

            // Limitează numărul de grupuri procesate
            List<Integer> sortedPriorities = priorityGroups.keySet().stream()
                    .sorted((a, b) -> Integer.compare(b, a))
                    .limit(8) // LIMITARE: Procesează doar top 8 grupuri de prioritate
                    .collect(Collectors.toList());

            // Pentru fiecare nivel de prioritate, amestecă slot-urile
            for (Integer priority : sortedPriorities) {
                List<AvailableSlot> groupSlots = new ArrayList<>(priorityGroups.get(priority));

                // OPTIMIZARE: Limitează mărimea grupului
                if (groupSlots.size() > 30) {
                    Collections.shuffle(groupSlots, random);
                    groupSlots = groupSlots.subList(0, 30);
                }

                Collections.shuffle(groupSlots, random);

                // Adaugă o parte din slot-uri pentru diversitate
                int maxSlotsFromGroup = Math.min(15, Math.max(1, groupSlots.size() / 2));
                diversifiedSlots.addAll(groupSlots.subList(0, Math.min(maxSlotsFromGroup, groupSlots.size())));

                // LIMITARE CRITICĂ: Nu depăși 100 de slot-uri
                if (diversifiedSlots.size() > 100) {
                    break;
                }
            }

            // Amestecă rezultatul final
            Collections.shuffle(diversifiedSlots, random);

            // LIMITARE FINALĂ
            if (diversifiedSlots.size() > 80) {
                diversifiedSlots = diversifiedSlots.subList(0, 80);
            }

            return diversifiedSlots;

        } catch (Exception e) {
            logger.debug("Eroare în diversifySlotSelection: {}", e.getMessage());
            Collections.shuffle(availableSlots, new Random());
            return availableSlots.subList(0, Math.min(50, availableSlots.size()));
        }
    }

    /**
     * Verifică dacă un slot este potrivit pentru profil - OPTIMIZAT
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
            return false;
        }
    }

    /**
     * Calculează prioritatea unui slot - OPTIMIZAT
     */
    private int calculateSlotPriority(String timeSlot, TimeInterval preferredInterval) {
        try {
            String startTime = timeSlot.split(" - ")[0];
            int hour = Integer.parseInt(startTime.split(":")[0]);
            int minute = Integer.parseInt(startTime.split(":")[1]);
            int totalMinutes = hour * 60 + minute;

            int intervalCenter = (preferredInterval.getStartMinutes() + preferredInterval.getEndMinutes()) / 2;
            int distance = Math.abs(totalMinutes - intervalCenter);

            // Calculează prioritatea de bază
            int basePriority = 1000 - distance;

            // Adaugă o componentă aleatorie pentru diversitate (±10% din prioritate)
            Random random = new Random(timeSlot.hashCode()); // Seed consistent pentru același slot
            int randomVariation = (int)(basePriority * 0.1 * (random.nextDouble() - 0.5) * 2);

            return basePriority + randomVariation;
        } catch (Exception e) {
            return new Random().nextInt(100); // Prioritate aleatorie în caz de eroare
        }
    }

    /**
     * Selectează rezervările în funcție de buget - OPTIMIZAT
     */
    private List<SelectedReservation> selectReservationsWithinBudget(
            List<AvailableSlot> availableSlots, int maxBookings, BigDecimal budget) {

        List<SelectedReservation> selected = new ArrayList<>();
        BigDecimal remainingBudget = budget;

        // Constrângeri pentru diversitate
        Set<String> usedDateSlots = new HashSet<>();
        Map<Long, Integer> hallUsageCount = new HashMap<>();  // Limitează rezervările per sală
        Map<String, Integer> timeSlotUsageCount = new HashMap<>();  // Limitează slot-urile identice

        Random random = new Random();

        // Amestecă slot-urile pentru selecție aleatorie
        List<AvailableSlot> shuffledSlots = new ArrayList<>(availableSlots);
        Collections.shuffle(shuffledSlots, random);

        for (AvailableSlot slot : shuffledSlots) {
            if (selected.size() >= maxBookings) break;

            String dateSlotKey = formatDate(slot.getDate()) + "_" + slot.getTimeSlot();
            if (usedDateSlots.contains(dateSlotKey)) continue;

            // Verifică dacă bugetul permite
            if (remainingBudget.compareTo(slot.getPrice()) < 0) continue;

            // CONSTRÂNGERI DE DIVERSITATE

            // 1. Limitează numărul de rezervări per sală (max 60% din total)
            int maxPerHall = Math.max(1, maxBookings * 60 / 100);
            if (hallUsageCount.getOrDefault(slot.getHall().getId(), 0) >= maxPerHall) {
                continue;
            }

            // 2. Limitează slot-urile identice (max 50% din total)
            int maxPerTimeSlot = Math.max(1, maxBookings / 2);
            if (timeSlotUsageCount.getOrDefault(slot.getTimeSlot(), 0) >= maxPerTimeSlot) {
                continue;
            }

            // 3. Adaugă o șansă de a sări peste slot-uri pentru mai multă diversitate
            if (selected.size() > 0 && random.nextDouble() < 0.12) { // 12% șansă să sară
                continue;
            }

            // Selectează slot-ul
            SelectedReservation reservation = new SelectedReservation(
                    slot.getHall(), slot.getDate(), slot.getTimeSlot(), slot.getPrice());
            selected.add(reservation);

            // Actualizează constrângerile
            remainingBudget = remainingBudget.subtract(slot.getPrice());
            usedDateSlots.add(dateSlotKey);
            hallUsageCount.merge(slot.getHall().getId(), 1, Integer::sum);
            timeSlotUsageCount.merge(slot.getTimeSlot(), 1, Integer::sum);
        }

        return selected;
    }

    /**
     * Creează rezervările din selecția făcută (fără plăți)
     */
    private List<Reservation> createReservationsFromSelected(List<SelectedReservation> selectedReservations, User user) {
        List<Reservation> reservations = new ArrayList<>();

        for (SelectedReservation selected : selectedReservations) {
            try {
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
            } catch (Exception e) {
                logger.debug("Eroare la salvarea rezervării pentru {}: {}", user.getEmail(), e.getMessage());
            }
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
            logger.debug("Eroare la trimiterea email-ului de confirmare pentru {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // Metode helper - OPTIMIZATE

    private List<SportsHall> getActiveHalls() {
        return ((List<SportsHall>) sportsHallRepository.findAll()).stream()
                .filter(hall -> hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                .limit(15) // Limitează numărul de săli
                .collect(Collectors.toList());
    }

    private List<SportsHall> getProfileHalls(ReservationProfile profile) {
        return profile.getSelectedHalls().stream()
                .filter(hall -> hall.getCity().equals(profile.getCity()) &&
                        hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                .limit(10) // Limitează și aici
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
        // Slot-uri optimizate pentru performanță
        return Arrays.asList(
                "08:30 - 10:00", "10:00 - 11:30", "11:30 - 13:00",
                "13:00 - 14:30", "14:30 - 16:00", "16:00 - 17:30",
                "17:30 - 19:00", "19:00 - 20:30", "20:30 - 22:00"
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
        try {
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
        } catch (Exception e) {
            logger.debug("Eroare la marcarea rezervărilor existente: {}", e.getMessage());
        }
    }

    private boolean isReservationInCurrentWeek(Reservation reservation, Date startDate, Date endDate) {
        try {
            Date reservationDate = reservation.getDate();
            return reservationDate != null
                    && !reservationDate.before(startDate)
                    && !reservationDate.after(endDate);
        } catch (Exception e) {
            return false;
        }
    }

    private void markSlotAsOccupied(WeeklySchedule schedule, Reservation reservation) {
        try {
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
        } catch (Exception e) {
            logger.debug("Eroare la marcarea slot-ului ocupat: {}", e.getMessage());
        }
    }

    private void updateScheduleWithReservations(WeeklySchedule schedule, List<Reservation> reservations) {
        for (Reservation reservation : reservations) {
            markSlotAsOccupied(schedule, reservation);
        }
    }

    /**
     * Distribuție echilibrată - OPTIMIZATĂ
     */
    private List<AvailableSlot> balanceDistribution(List<AvailableSlot> availableSlots, int maxBookings) {
        if (availableSlots.isEmpty() || availableSlots.size() <= maxBookings) {
            return availableSlots;
        }

        // LIMITARE CRITICĂ pentru performanță
        if (availableSlots.size() > 150) {
            Collections.shuffle(availableSlots, new Random());
            availableSlots = availableSlots.subList(0, 150);
        }

        try {
            Map<String, List<AvailableSlot>> slotsByDay = availableSlots.stream()
                    .collect(Collectors.groupingBy(slot -> formatDate(slot.getDate())));

            List<AvailableSlot> balancedSlots = new ArrayList<>();
            Random random = new Random();

            // Încearcă să distribuie uniform pe zile
            int slotsPerDay = Math.max(1, maxBookings / 7);

            for (Map.Entry<String, List<AvailableSlot>> dayEntry : slotsByDay.entrySet()) {
                List<AvailableSlot> daySlots = dayEntry.getValue();
                Collections.shuffle(daySlots, random);

                int slotsToTake = Math.min(slotsPerDay, daySlots.size());
                balancedSlots.addAll(daySlots.subList(0, slotsToTake));

                // LIMITARE: Nu depăși capacitatea
                if (balancedSlots.size() >= maxBookings * 2) {
                    break;
                }
            }

            // Completează cu slot-uri rămase DOAR dacă e necesar
            if (balancedSlots.size() < maxBookings && balancedSlots.size() < availableSlots.size()) {
                List<AvailableSlot> remainingSlots = availableSlots.stream()
                        .filter(slot -> !balancedSlots.contains(slot))
                        .limit(maxBookings) // LIMITARE
                        .collect(Collectors.toList());

                Collections.shuffle(remainingSlots, random);
                int needed = Math.min(maxBookings - balancedSlots.size(), remainingSlots.size());
                if (needed > 0) {
                    balancedSlots.addAll(remainingSlots.subList(0, needed));
                }
            }

            Collections.shuffle(balancedSlots, random);
            return balancedSlots.subList(0, Math.min(maxBookings * 2, balancedSlots.size()));

        } catch (Exception e) {
            logger.debug("Eroare în balanceDistribution: {}", e.getMessage());
            Collections.shuffle(availableSlots, new Random());
            return availableSlots.subList(0, Math.min(maxBookings, availableSlots.size()));
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