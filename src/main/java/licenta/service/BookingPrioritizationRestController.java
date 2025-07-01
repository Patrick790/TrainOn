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
     * Endpoint principal pentru generarea automata a rezervărilor
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
            response.put("successfulProfiles", result.getSuccessfulProfiles());

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
     * Metoda principală de generare a rezervărilor
     */
    private GenerationResult generateAutomatedBookings() {
        GenerationResult result = new GenerationResult();
        long startTime = System.currentTimeMillis();

        try {
            logger.info("=== ÎNCEPEREA GENERĂRII AUTOMATE CU ALOCARE ECHITABILĂ ===");

            // Calculeaza perioada pentru saptamana urmatoare
            Date nextMonday = calculateNextWeekStart();
            Date nextSunday = calculateWeekEnd(nextMonday);

            logger.info("=== GENERARE PENTRU SĂPTĂMÂNA: {} - {} ===",
                    formatDate(nextMonday), formatDate(nextSunday));

            deleteExistingReservationsForNextWeek(nextMonday, nextSunday);

            List<ReservationProfile> eligibleProfiles = getEligibleProfiles();

            if (eligibleProfiles.isEmpty()) {
                logger.warn("Nu există profiluri eligibile pentru generarea rezervărilor!");
                return result;
            }

            // Max 30 de profiluri pentru a evita timeout-ul
            if (eligibleProfiles.size() > 30) {
                logger.warn("Se limitează la 30 profiluri din {} disponibile", eligibleProfiles.size());
                eligibleProfiles = eligibleProfiles.subList(0, 30);
            }

            List<ReservationProfile> prioritizedProfiles = prioritizeProfiles(eligibleProfiles);
            logger.info("Profiluri eligibile procesate: {}", prioritizedProfiles.size());

            // 4. Creeaza programul disponibil
            WeeklySchedule weeklySchedule = createWeeklySchedule(nextMonday);

            // 5. ALOCARE ECHITABILA IN 3 FAZE
            result = allocateReservationsFairly(prioritizedProfiles, weeklySchedule, nextMonday, startTime);

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("=== GENERARE COMPLETĂ: {} rezervări pentru {} profiluri în {}ms ===",
                    result.getTotalReservations(), result.getSuccessfulProfiles(), totalTime);

        } catch (Exception e) {
            logger.error("Eroare critică în procesul de generare", e);
            throw new RuntimeException("Eroare la generarea rezervărilor: " + e.getMessage());
        }

        return result;
    }

    /**
     * ALOCARE ECHITABILĂ ÎN 3 FAZE
     */
    private GenerationResult allocateReservationsFairly(
            List<ReservationProfile> profiles,
            WeeklySchedule schedule,
            Date startDate,
            long globalStartTime) {

        GenerationResult result = new GenerationResult();

        try {
            // FAZA 1: Alocare minima garantata (1 rezervare per echipa)
            logger.info("=== FAZA 1: Alocare minimă garantată pentru {} echipe ===", profiles.size());
            result = allocateMinimumReservations(profiles, schedule, startDate, result, globalStartTime);

            if (System.currentTimeMillis() - globalStartTime > 75000) { // 75s limit
                logger.warn("Timeout global - se oprește după faza 1");
                return result;
            }

            // FAZA 2: Distributie suplimentara in functie de prioritate si buget
            logger.info("=== FAZA 2: Distribuție suplimentară ===");
            result = allocateAdditionalReservations(profiles, schedule, startDate, result, globalStartTime);

            if (System.currentTimeMillis() - globalStartTime > 85000) { // 85s limit
                logger.warn("Timeout global - se oprește după faza 2");
                return result;
            }

            // FAZA 3: Optimizare finala cu slot-urile ramase
            logger.info("=== FAZA 3: Optimizare finală ===");
            result = optimizeFinalAllocation(profiles, schedule, startDate, result, globalStartTime);

        } catch (Exception e) {
            logger.error("Eroare în alocarea echitabilă: {}", e.getMessage());
        }

        return result;
    }

    /**
     * FAZA 1: Asigura ca fiecare echipa primeste macar o rezervare
     */
    private GenerationResult allocateMinimumReservations(
            List<ReservationProfile> profiles,
            WeeklySchedule schedule,
            Date startDate,
            GenerationResult result,
            long globalStartTime) {

        logger.info("Alocand minimum 1 rezervare pentru {} echipe", profiles.size());
        int successCount = 0;

        for (ReservationProfile profile : profiles) {
            try {
                // Verifică timeout
                if (System.currentTimeMillis() - globalStartTime > 60000) { // 60s pentru faza 1
                    logger.warn("Timeout în faza 1 după {} profiluri procesate", successCount);
                    break;
                }

                // Găsește cel mai bun slot disponibil pentru această echipă
                AvailableSlot bestSlot = findBestAvailableSlotForProfile(profile, schedule, startDate);

                if (bestSlot != null && profile.getWeeklyBudget().compareTo(bestSlot.getPrice()) >= 0) {
                    // Creează rezervarea
                    List<SelectedReservation> minReservation = Arrays.asList(
                            new SelectedReservation(bestSlot.getHall(), bestSlot.getDate(),
                                    bestSlot.getTimeSlot(), bestSlot.getPrice())
                    );

                    List<Reservation> reservations = createReservationsFromSelected(minReservation, profile.getUser());

                    if (!reservations.isEmpty()) {
                        ProfileResult profileResult = new ProfileResult(profile);
                        profileResult.setReservations(reservations);
                        result.addProfileResult(profileResult);

                        // Actualizează programul pentru a marca slot-ul ca ocupat
                        updateScheduleWithReservations(schedule, reservations);

                        successCount++;
                        logger.debug("Echipa '{}' a primit rezervarea minimă garantată", profile.getName());

                        // Trimite email asyncron
                        try {
                            sendConfirmationEmail(profile.getUser(), reservations);
                        } catch (Exception e) {
                            logger.debug("Eroare email pentru {}: {}", profile.getName(), e.getMessage());
                        }
                    } else {
                        logger.warn("Nu s-a putut crea rezervarea minimă pentru '{}'", profile.getName());
                        result.addFailedProfile(profile, "Eroare la crearea rezervării minime");
                    }
                } else {
                    String reason = bestSlot == null ? "Nu există slot-uri disponibile" : "Buget insuficient";
                    logger.warn("Nu s-a putut aloca slot pentru echipa '{}': {}", profile.getName(), reason);
                    result.addFailedProfile(profile, reason);
                }

            } catch (Exception e) {
                logger.error("Eroare la alocarea minimă pentru '{}': {}", profile.getName(), e.getMessage());
                result.addFailedProfile(profile, "Eroare la alocare: " + e.getMessage());
            }
        }

        logger.info("Faza 1 completă: {}/{} echipe au primit rezervarea minimă", successCount, profiles.size());
        return result;
    }

    /**
     * FAZA 2: Distribuie rezervări suplimentare în funcție de prioritate și buget
     */
    private GenerationResult allocateAdditionalReservations(
            List<ReservationProfile> profiles,
            WeeklySchedule schedule,
            Date startDate,
            GenerationResult result,
            long globalStartTime) {

        logger.info("Alocând rezervări suplimentare pentru echipele cu buget și prioritate mare");

        // Sorteaza echipele care au deja o rezervare dupa prioritate si buget ramas
        List<ProfileResult> successfulProfiles = result.getProfileResults().stream()
                .filter(ProfileResult::isSuccess)
                .sorted((p1, p2) -> {
                    // Calculeaza bugetul ramas
                    BigDecimal budget1 = calculateRemainingBudget(p1.getProfile(), p1.getReservations());
                    BigDecimal budget2 = calculateRemainingBudget(p2.getProfile(), p2.getReservations());

                    // Prioritate dupa categoria de varsta, apoi dupa buget ramas
                    int agePriority1 = getAgePriority(p1.getProfile().getAgeCategory());
                    int agePriority2 = getAgePriority(p2.getProfile().getAgeCategory());

                    if (agePriority1 != agePriority2) {
                        return Integer.compare(agePriority2, agePriority1);
                    }

                    return budget2.compareTo(budget1);
                })
                .collect(Collectors.toList());

        int additionalCount = 0;

        for (ProfileResult profileResult : successfulProfiles) {
            try {
                if (System.currentTimeMillis() - globalStartTime > 75000) { // 75s pentru faza 2
                    logger.warn("Timeout în faza 2 după {} rezervări suplimentare", additionalCount);
                    break;
                }

                ReservationProfile profile = profileResult.getProfile();
                int maxBookings = getMaxBookingsForProfile(profile);
                int currentBookings = profileResult.getReservations().size();

                if (currentBookings < maxBookings) {
                    BigDecimal remainingBudget = calculateRemainingBudget(profile, profileResult.getReservations());

                    if (remainingBudget.compareTo(BigDecimal.ZERO) > 0) {
                        List<AvailableSlot> additionalSlots = findAdditionalSlotsForProfile(
                                profile, schedule, startDate, maxBookings - currentBookings);

                        List<SelectedReservation> selectedAdditional = selectReservationsWithinBudgetLimited(
                                additionalSlots, maxBookings - currentBookings, remainingBudget);

                        if (!selectedAdditional.isEmpty()) {
                            List<Reservation> additionalReservations = createReservationsFromSelected(
                                    selectedAdditional, profile.getUser());

                            // Adauga la rezervarile existente
                            List<Reservation> allReservations = new ArrayList<>(profileResult.getReservations());
                            allReservations.addAll(additionalReservations);
                            profileResult.setReservations(allReservations);

                            // Actualizeaza programul
                            updateScheduleWithReservations(schedule, additionalReservations);

                            additionalCount += additionalReservations.size();
                            logger.debug("Echipa '{}' a primit {} rezervări suplimentare",
                                    profile.getName(), additionalReservations.size());
                        }
                    }
                }

            } catch (Exception e) {
                logger.warn("Eroare la alocarea suplimentară pentru '{}': {}",
                        profileResult.getProfile().getName(), e.getMessage());
            }
        }

        logger.info("Faza 2 completă: {} rezervări suplimentare alocate", additionalCount);
        return result;
    }

    /**
     * FAZA 3: Optimizare finală - distribuie slot-urile rămase echitabil
     */
    private GenerationResult optimizeFinalAllocation(
            List<ReservationProfile> profiles,
            WeeklySchedule schedule,
            Date startDate,
            GenerationResult result,
            long globalStartTime) {

        logger.info("Optimizare finală - distribuind slot-urile rămase");

        // Gaseste toate slot-urile inca disponibile
        List<AvailableSlot> remainingSlots = findAllRemainingSlots(schedule, startDate);

        if (remainingSlots.isEmpty()) {
            logger.info("Nu mai există slot-uri disponibile pentru optimizare");
            return result;
        }

        // Limiteaza pentru performanta
        if (remainingSlots.size() > 50) {
            Collections.shuffle(remainingSlots, new Random());
            remainingSlots = remainingSlots.subList(0, 50);
        }

        logger.info("Slot-uri ramase pentru optimizare: {}", remainingSlots.size());

        // Distribuie slot-urile ramase echipelor care inca au buget
        List<ProfileResult> profilesWithBudget = result.getProfileResults().stream()
                .filter(ProfileResult::isSuccess)
                .filter(pr -> {
                    BigDecimal remaining = calculateRemainingBudget(pr.getProfile(), pr.getReservations());
                    int maxBookings = getMaxBookingsForProfile(pr.getProfile());
                    return remaining.compareTo(BigDecimal.ZERO) > 0 &&
                            pr.getReservations().size() < maxBookings;
                })
                .sorted((p1, p2) -> {
                    // Prioritate pentru echipele cu mai putine rezervari
                    int bookings1 = p1.getReservations().size();
                    int bookings2 = p2.getReservations().size();
                    return Integer.compare(bookings1, bookings2);
                })
                .collect(Collectors.toList());

        // Distribuie in mod rotativ
        int slotIndex = 0;
        int optimizedCount = 0;
        boolean anyAllocation = true;

        while (anyAllocation && slotIndex < remainingSlots.size()) {
            // Verifică timeout
            if (System.currentTimeMillis() - globalStartTime > 88000) { // 88s pentru faza 3
                logger.warn("Timeout în faza 3 după {} optimizări", optimizedCount);
                break;
            }

            anyAllocation = false;

            for (ProfileResult profileResult : profilesWithBudget) {
                if (slotIndex >= remainingSlots.size()) break;

                ReservationProfile profile = profileResult.getProfile();
                BigDecimal remainingBudget = calculateRemainingBudget(profile, profileResult.getReservations());
                AvailableSlot slot = remainingSlots.get(slotIndex);

                // Verifică dacă echipa poate să își permită slot-ul și dacă îi convine
                if (remainingBudget.compareTo(slot.getPrice()) >= 0 &&
                        isSlotSuitableForProfile(profile, slot) &&
                        profileResult.getReservations().size() < getMaxBookingsForProfile(profile)) {

                    try {
                        SelectedReservation newReservation = new SelectedReservation(
                                slot.getHall(), slot.getDate(), slot.getTimeSlot(), slot.getPrice());

                        List<Reservation> newReservations = createReservationsFromSelected(
                                Arrays.asList(newReservation), profile.getUser());

                        if (!newReservations.isEmpty()) {
                            List<Reservation> allReservations = new ArrayList<>(profileResult.getReservations());
                            allReservations.addAll(newReservations);
                            profileResult.setReservations(allReservations);

                            // Marchează slot-ul ca ocupat
                            updateScheduleWithReservations(schedule, newReservations);

                            anyAllocation = true;
                            optimizedCount++;
                            logger.debug("Optimizare: Echipa '{}' a primit slot suplimentar", profile.getName());
                        }

                    } catch (Exception e) {
                        logger.debug("Eroare la optimizarea pentru '{}': {}", profile.getName(), e.getMessage());
                    }
                }

                slotIndex++;
            }
        }

        logger.info("Faza 3 completă: {} optimizări realizate", optimizedCount);
        return result;
    }

    /**
     * Gaseste cel mai bun slot disponibil pentru o echipa (pentru rezervarea minima)
     */
    private AvailableSlot findBestAvailableSlotForProfile(
            ReservationProfile profile, WeeklySchedule schedule, Date startDate) {

        TimeInterval preferredInterval = getPreferredTimeInterval(profile.getTimeInterval());
        boolean isSenior = isSeniorCategory(profile.getAgeCategory());
        List<SportsHall> profileHalls = getProfileHalls(profile);

        if (profileHalls.isEmpty()) {
            return null;
        }

        List<AvailableSlot> candidateSlots = new ArrayList<>();

        // Cauta in toate zilele saptamanii (cu limitari pentru performanta)
        int slotsFound = 0;
        for (Map.Entry<String, DaySchedule> dayEntry : schedule.getDaySchedules().entrySet()) {
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
                        candidateSlots.add(availableSlot);

                        slotsFound++;
                        if (slotsFound >= 30) break;
                    }
                }
                if (slotsFound >= 30) break;
            }
            if (slotsFound >= 30) break;
        }

        if (candidateSlots.isEmpty()) {
            return null;
        }

        // Returnează slot-ul cu cea mai mare prioritate si care incape in buget
        return candidateSlots.stream()
                .filter(slot -> profile.getWeeklyBudget().compareTo(slot.getPrice()) >= 0)
                .max(Comparator.comparingInt(AvailableSlot::getPriority))
                .orElse(candidateSlots.get(0)); // Fallback la primul slot disponibil
    }

    /**
     * Calculează bugetul rămas pentru o echipă
     */
    private BigDecimal calculateRemainingBudget(ReservationProfile profile, List<Reservation> existingReservations) {
        BigDecimal totalSpent = existingReservations.stream()
                .map(r -> BigDecimal.valueOf(r.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return profile.getWeeklyBudget().subtract(totalSpent);
    }

    /**
     * Găsește slot-uri suplimentare pentru o echipă (evitând duplicatele)
     */
    private List<AvailableSlot> findAdditionalSlotsForProfile(
            ReservationProfile profile, WeeklySchedule schedule, Date startDate, int maxAdditional) {

        TimeInterval preferredInterval = getPreferredTimeInterval(profile.getTimeInterval());
        boolean isSenior = isSeniorCategory(profile.getAgeCategory());
        List<SportsHall> profileHalls = getProfileHalls(profile);

        List<AvailableSlot> availableSlots = new ArrayList<>();

        int slotsFound = 0;
        for (Map.Entry<String, DaySchedule> dayEntry : schedule.getDaySchedules().entrySet()) {
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

                        slotsFound++;
                        if (slotsFound >= maxAdditional * 8) break;
                    }
                }
                if (slotsFound >= maxAdditional * 8) break;
            }
            if (slotsFound >= maxAdditional * 8) break;
        }

        // Sorteaza si limiteaza
        Collections.shuffle(availableSlots, new Random());
        return availableSlots.stream()
                .limit(maxAdditional * 3)
                .collect(Collectors.toList());
    }

    /**
     * Găsește toate slot-urile încă disponibile în program
     */
    private List<AvailableSlot> findAllRemainingSlots(WeeklySchedule schedule, Date startDate) {
        List<AvailableSlot> remainingSlots = new ArrayList<>();
        List<SportsHall> activeHalls = getActiveHalls();

        int slotsFound = 0;
        for (Map.Entry<String, DaySchedule> dayEntry : schedule.getDaySchedules().entrySet()) {
            Date currentDate = parseDate(dayEntry.getKey());
            DaySchedule daySchedule = dayEntry.getValue();

            for (SportsHall hall : activeHalls) {
                HallDaySchedule hallSchedule = daySchedule.getHallSchedule(hall.getId());
                if (hallSchedule == null) continue;

                for (Map.Entry<String, TimeSlot> slotEntry : hallSchedule.getSlots().entrySet()) {
                    String timeSlot = slotEntry.getKey();
                    TimeSlot slot = slotEntry.getValue();

                    if (slot.getStatus() == SlotStatus.AVAILABLE) {
                        AvailableSlot availableSlot = new AvailableSlot(
                                hall, currentDate, timeSlot, BigDecimal.valueOf(hall.getTariff()));
                        remainingSlots.add(availableSlot);

                        slotsFound++;
                        // Limitează pentru performanță
                        if (slotsFound >= 100) break;
                    }
                }
                if (slotsFound >= 100) break;
            }
            if (slotsFound >= 100) break;
        }

        return remainingSlots;
    }

    /**
     * Verifică dacă un slot este potrivit pentru o echipă (versiune simplificată)
     */
    private boolean isSlotSuitableForProfile(ReservationProfile profile, AvailableSlot slot) {
        TimeInterval preferredInterval = getPreferredTimeInterval(profile.getTimeInterval());
        boolean isSenior = isSeniorCategory(profile.getAgeCategory());

        // Verifica daca sala este in lista echipei
        boolean hallMatch = getProfileHalls(profile).stream()
                .anyMatch(h -> h.getId().equals(slot.getHall().getId()));

        if (!hallMatch) return false;

        // Creeaza un TimeSlot temporar pentru verificare
        TimeSlot tempSlot = new TimeSlot(slot.getTimeSlot(), SlotStatus.AVAILABLE, slot.getHall().getTariff());

        return isSlotSuitableForProfile(tempSlot, preferredInterval, isSenior);
    }

    /**
     * Versiune limitată a selecției pentru rezervări suplimentare
     */
    private List<SelectedReservation> selectReservationsWithinBudgetLimited(
            List<AvailableSlot> availableSlots, int maxBookings, BigDecimal budget) {

        // Pentru rezervari suplimentare, foloseste o versiune simplificata
        List<SelectedReservation> selected = new ArrayList<>();
        BigDecimal remainingBudget = budget;

        // Limiteaza lista pentru performanta
        List<AvailableSlot> limitedSlots = availableSlots.stream()
                .limit(maxBookings * 5)
                .collect(Collectors.toList());

        Collections.shuffle(limitedSlots, new Random());

        for (AvailableSlot slot : limitedSlots) {
            if (selected.size() >= maxBookings) break;

            // Verifica daca bugetul permite
            if (remainingBudget.compareTo(slot.getPrice()) >= 0) {
                SelectedReservation reservation = new SelectedReservation(
                        slot.getHall(), slot.getDate(), slot.getTimeSlot(), slot.getPrice());
                selected.add(reservation);
                remainingBudget = remainingBudget.subtract(slot.getPrice());
            }
        }

        return selected;
    }

    /**
     * Calculează data de început pentru săptămâna următoare (luni)
     */
    private Date calculateNextWeekStart() {
        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (currentDayOfWeek == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
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
            List<Reservation> toDelete = new ArrayList<>();

            for (Reservation reservation : allReservations) {
                if (reservation.getDate() != null &&
                        "reservation".equals(reservation.getType()) &&
                        !reservation.getDate().before(startDate) &&
                        !reservation.getDate().after(endDate)) {
                    toDelete.add(reservation);
                }
            }

            if (!toDelete.isEmpty()) {
                logger.info("Se șterg {} rezervări din săptămâna următoare", toDelete.size());
                reservationRepository.deleteAll(toDelete);

                // Verificare
                Thread.sleep(200);
                long remaining = ((List<Reservation>) reservationRepository.findAll()).stream()
                        .filter(r -> r.getDate() != null &&
                                "reservation".equals(r.getType()) &&
                                !r.getDate().before(startDate) &&
                                !r.getDate().after(endDate))
                        .count();

                if (remaining > 0) {
                    logger.warn("Încă există {} rezervări după ștergere!", remaining);
                } else {
                    logger.info("Toate rezervările au fost șterse cu succes");
                }
            }
        } catch (Exception e) {
            logger.error("Eroare la ștergerea rezervărilor: {}", e.getMessage());
            throw new RuntimeException("Eroare la ștergerea rezervărilor: " + e.getMessage());
        }
    }

    /**
     * Obține profilurile eligibile pentru generare - OPTIMIZAT
     */
    private List<ReservationProfile> getEligibleProfiles() {
        List<ReservationProfile> allProfiles = (List<ReservationProfile>) reservationProfileRepository.findAll();

        return allProfiles.stream()
                .filter(this::isProfileEligible)
                .limit(30) // Limiteaza din start
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
                        return Integer.compare(agePriority2, agePriority1);
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

        // Limiteaza nr de sali pentru performanta
        if (activeHalls.size() > 15) {
            activeHalls = activeHalls.subList(0, 15);
        }

        // Pentru fiecare zi din saptamana (luni - duminica)
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DAY_OF_YEAR, dayOffset);
            Date currentDate = cal.getTime();

            DaySchedule daySchedule = createDaySchedule(currentDate, activeHalls);
            schedule.addDaySchedule(formatDate(currentDate), daySchedule);
        }

        // Marcheaza sloturile deja ocupate
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
            // Gaseste programul salii pentru aceasta zi
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
     * Determina statusul unui slot
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
     * Verifică dacă un slot este în orele de funcționare
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
     * Convertește ora în minute
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
     * Generează rezervări pentru un profil
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
     * Găsește sloturile disponibile pentru un profil
     */
    private List<AvailableSlot> findAvailableSlotsForProfile(
            ReservationProfile profile, WeeklySchedule weeklySchedule, Date startDate,
            TimeInterval preferredInterval, boolean isSenior) {

        List<AvailableSlot> availableSlots = new ArrayList<>();
        List<SportsHall> profileHalls = getProfileHalls(profile);

        if (profileHalls.isEmpty()) {
            return availableSlots;
        }

        int maxSlots = getMaxBookingsForProfile(profile) * 15; // Limiteaza cautarea

        // Colecteaza toate slot-urile disponibile
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

                        // EARLY EXIT pentru performanta
                        if (availableSlots.size() >= maxSlots) {
                            break outerLoop;
                        }
                    }
                }
            }
        }

        // Aplica diversificarea doar daca avem suficiente slot-uri
        if (availableSlots.size() > getMaxBookingsForProfile(profile) * 4) {
            return diversifySlotSelection(availableSlots);
        } else {
            // Pentru liste mici, doar amesteca
            Collections.shuffle(availableSlots, new Random());
            return availableSlots;
        }
    }

    /**
     * Diversifică selecția de slot-uri
     */
    private List<AvailableSlot> diversifySlotSelection(List<AvailableSlot> availableSlots) {
        if (availableSlots.isEmpty()) {
            return availableSlots;
        }

        if (availableSlots.size() > 200) {
            Collections.shuffle(availableSlots, new Random());
            return availableSlots.subList(0, 200);
        }

        try {
            // Grupeaza slot-urile dupa prioritate
            Map<Integer, List<AvailableSlot>> priorityGroups = availableSlots.stream()
                    .collect(Collectors.groupingBy(AvailableSlot::getPriority));

            List<AvailableSlot> diversifiedSlots = new ArrayList<>();
            Random random = new Random();

            // Limiteaza nr de grupuri procesate
            List<Integer> sortedPriorities = priorityGroups.keySet().stream()
                    .sorted((a, b) -> Integer.compare(b, a))
                    .limit(8) // LIMITARE: Proceseaza doar top 8 grupuri de prioritate
                    .collect(Collectors.toList());

            // Pentru fiecare nivel de prioritate, amesteca slot-urile
            for (Integer priority : sortedPriorities) {
                List<AvailableSlot> groupSlots = new ArrayList<>(priorityGroups.get(priority));

                if (groupSlots.size() > 30) {
                    Collections.shuffle(groupSlots, random);
                    groupSlots = groupSlots.subList(0, 30);
                }

                Collections.shuffle(groupSlots, random);

                // Adauga o parte din slot-uri pentru diversitate
                int maxSlotsFromGroup = Math.min(15, Math.max(1, groupSlots.size() / 2));
                diversifiedSlots.addAll(groupSlots.subList(0, Math.min(maxSlotsFromGroup, groupSlots.size())));

                // LIMITARE CRITICA: Nu depăși 100 de slot-uri
                if (diversifiedSlots.size() > 100) {
                    break;
                }
            }

            // Amesteca rezultatul final
            Collections.shuffle(diversifiedSlots, random);

            // LIMITARE FINALA
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

            // Aplica regula pentru seniori/juniori la ora 14:30
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

            // Calculeaza prioritatea de baza
            int basePriority = 1000 - distance;

            // Adauga o componenta aleatorie pentru diversitate (±10% din prioritate)
            Random random = new Random(timeSlot.hashCode()); // Seed consistent pentru același slot
            int randomVariation = (int)(basePriority * 0.1 * (random.nextDouble() - 0.5) * 2);

            return basePriority + randomVariation;
        } catch (Exception e) {
            return new Random().nextInt(100); // Prioritate aleatorie in caz de eroare
        }
    }

    /**
     * Selectează rezervările în funcție de buget - OPTIMIZAT
     */
    private List<SelectedReservation> selectReservationsWithinBudget(
            List<AvailableSlot> availableSlots, int maxBookings, BigDecimal budget) {

        List<SelectedReservation> selected = new ArrayList<>();
        BigDecimal remainingBudget = budget;

        // Constrangeri pentru diversitate
        Set<String> usedDateSlots = new HashSet<>();
        Map<Long, Integer> hallUsageCount = new HashMap<>();  // Limiteaza rezervarile per sala
        Map<String, Integer> timeSlotUsageCount = new HashMap<>();  // Limiteaza slot-urile identice

        Random random = new Random();

        // Amesteca slot-urile pentru selectie aleatorie
        List<AvailableSlot> shuffledSlots = new ArrayList<>(availableSlots);
        Collections.shuffle(shuffledSlots, random);

        for (AvailableSlot slot : shuffledSlots) {
            if (selected.size() >= maxBookings) break;

            String dateSlotKey = formatDate(slot.getDate()) + "_" + slot.getTimeSlot();
            if (usedDateSlots.contains(dateSlotKey)) continue;

            // Verifica dacă bugetul permite
            if (remainingBudget.compareTo(slot.getPrice()) < 0) continue;

            // CONSTRANGERI DE DIVERSITATE

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
     * Creează rezervările din selecția făcută (fara plata)
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
     * Trimite email de confirmare (fara plata)
     */
    private void sendConfirmationEmail(User user, List<Reservation> reservations) {
        try {
            ReservationEmailService.sendReservationConfirmationEmail(
                    mailSender, senderEmail, user, reservations, null);
        } catch (Exception e) {
            logger.debug("Eroare la trimiterea email-ului de confirmare pentru {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // Metode helper

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
     * Distribuție echilibrată
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

            // Încearca sa distribuie uniform pe zile
            int slotsPerDay = Math.max(1, maxBookings / 7);

            for (Map.Entry<String, List<AvailableSlot>> dayEntry : slotsByDay.entrySet()) {
                List<AvailableSlot> daySlots = dayEntry.getValue();
                Collections.shuffle(daySlots, random);

                int slotsToTake = Math.min(slotsPerDay, daySlots.size());
                balancedSlots.addAll(daySlots.subList(0, slotsToTake));

                // LIMITARE: Nu depasi capacitatea
                if (balancedSlots.size() >= maxBookings * 2) {
                    break;
                }
            }

            // Completeaza cu slot-uri ramase DOAR daca e necesar
            if (balancedSlots.size() < maxBookings && balancedSlots.size() < availableSlots.size()) {
                List<AvailableSlot> remainingSlots = availableSlots.stream()
                        .filter(slot -> !balancedSlots.contains(slot))
                        .limit(maxBookings)
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

    // Clase pentru gestionarea programului si rezultatelor

    public static class WeeklySchedule {
        private final Map<String, DaySchedule> daySchedules = new HashMap<>();

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
        private final Map<Long, HallDaySchedule> hallSchedules = new HashMap<>();

        public void addHallSchedule(Long hallId, HallDaySchedule schedule) {
            hallSchedules.put(hallId, schedule);
        }

        public HallDaySchedule getHallSchedule(Long hallId) {
            return hallSchedules.get(hallId);
        }
    }

    public static class HallDaySchedule {
        private final Map<String, TimeSlot> slots = new HashMap<>();

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

        public List<Reservation> getReservations() { return reservations; }
        public List<ProfileResult> getProfileResults() { return profileResults; }
        public int getTotalReservations() {
            return profileResults.stream()
                    .filter(ProfileResult::isSuccess)
                    .mapToInt(pr -> pr.getReservations() != null ? pr.getReservations().size() : 0)
                    .sum();
        }
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