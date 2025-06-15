package licenta.service;

import jakarta.transaction.Transactional;
import licenta.model.*;
import licenta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private IReservationProfileRepository reservationProfileRepository;
    @Autowired
    private IUserSpringRepository userRepository;
    @Autowired
    private ISportsHallSpringRepository sportsHallRepository;
    @Autowired
    private IReservationSpringRepository reservationRepository;
    @Autowired
    private IScheduleSpringRepository scheduleRepository;
    @Autowired
    private IPaymentSpringRepository paymentRepository;

    /**
     * Endpoint pentru generarea automată a rezervărilor (versiune simplificată)
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateBookings() {
        try {
            logger.info("Începerea procesului simplificat de generare rezervări...");

            // 1. Calculează perioada pentru săptămâna următoare
            Calendar nextWeekStart = getNextWeekStart();
            Calendar nextWeekEnd = getNextWeekEnd(nextWeekStart);

            logger.info("Generare pentru perioada: {} - {}",
                    formatDate(nextWeekStart.getTime()),
                    formatDate(nextWeekEnd.getTime()));

            // 2. Șterge rezervările existente pentru săptămâna următoare
            deleteExistingReservationsForNextWeek(nextWeekStart.getTime(), nextWeekEnd.getTime());

            // 3. Obține toate profilurile eligibile
            List<ReservationProfile> eligibleProfiles = getEligibleProfiles();
            logger.info("Profiluri eligibile găsite: {}", eligibleProfiles.size());

            if (eligibleProfiles.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Nu există profiluri eligibile pentru generarea rezervărilor");
                response.put("count", 0);
                response.put("reservations", Collections.emptyList());
                response.put("payments", Collections.emptyList());
                return ResponseEntity.ok(response);
            }

            // 4. Sortează profilurile după prioritate
            List<ReservationProfile> prioritizedProfiles = prioritizeProfiles(eligibleProfiles);

            // 5. Obține toate sălile active
            List<SportsHall> activeHalls = getActiveHalls();
            logger.info("Săli active găsite: {}", activeHalls.size());

            if (activeHalls.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Nu există săli active pentru generarea rezervărilor");
                response.put("count", 0);
                response.put("reservations", Collections.emptyList());
                response.put("payments", Collections.emptyList());
                return ResponseEntity.ok(response);
            }

            // 6. Creează matricea de disponibilitate pentru săptămâna următoare
            Map<String, Map<String, Map<String, Boolean>>> availabilityMatrix =
                    createAvailabilityMatrix(activeHalls, nextWeekStart);

            // 7. Generează rezervările pentru fiecare profil
            List<Reservation> allReservations = new ArrayList<>();
            List<Payment> allPayments = new ArrayList<>();

            for (ReservationProfile profile : prioritizedProfiles) {
                try {
                    GenerationResult profileResult = generateReservationsForProfile(
                            profile, availabilityMatrix, nextWeekStart);

                    allReservations.addAll(profileResult.getReservations());
                    allPayments.addAll(profileResult.getPayments());

                    logger.info("Profil '{}' - {} rezervări generate",
                            profile.getName(), profileResult.getReservations().size());

                } catch (Exception e) {
                    logger.error("Eroare la generarea rezervărilor pentru profilul '{}': {}",
                            profile.getName(), e.getMessage(), e);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rezervările au fost generate cu succes");
            response.put("count", allReservations.size());
            response.put("reservations", allReservations);
            response.put("payments", allPayments);
            response.put("paymentsCount", allPayments.size());

            logger.info("Generare completă: {} rezervări, {} plăți",
                    allReservations.size(), allPayments.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Eroare la generarea rezervărilor", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Eroare la generarea rezervărilor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Calculează începutul săptămânii următoare (luni)
     */
    private Calendar getNextWeekStart() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Bucharest"));

        // Găsește următoarea luni
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysUntilNextMonday;

        if (currentDayOfWeek == Calendar.SUNDAY) {
            daysUntilNextMonday = 1; // Mâine este luni
        } else {
            daysUntilNextMonday = Calendar.MONDAY - currentDayOfWeek + 7;
        }

        cal.add(Calendar.DAY_OF_YEAR, daysUntilNextMonday);

        // Setează la începutul zilei
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }

    /**
     * Calculează sfârșitul săptămânii (duminică)
     */
    private Calendar getNextWeekEnd(Calendar weekStart) {
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6); // Luni + 6 zile = Duminică
        weekEnd.set(Calendar.HOUR_OF_DAY, 23);
        weekEnd.set(Calendar.MINUTE, 59);
        weekEnd.set(Calendar.SECOND, 59);
        return weekEnd;
    }

    /**
     * Șterge rezervările existente din săptămâna următoare
     */
    private void deleteExistingReservationsForNextWeek(Date startDate, Date endDate) {
        try {
            List<Reservation> existingReservations = StreamSupport
                    .stream(reservationRepository.findAll().spliterator(), false)
                    .filter(res -> res.getDate() != null &&
                            !res.getDate().before(startDate) &&
                            !res.getDate().after(endDate) &&
                            "reservation".equals(res.getType()) &&
                            !"CANCELLED".equals(res.getStatus()))
                    .collect(Collectors.toList());

            if (!existingReservations.isEmpty()) {
                logger.info("Șterg {} rezervări existente pentru regenerare", existingReservations.size());

                // Pentru fiecare rezervare, șterge mai întâi legăturile cu plățile
                for (Reservation reservation : existingReservations) {
                    try {
                        // Găsește plățile asociate acestei rezervări
                        List<Payment> associatedPayments = StreamSupport
                                .stream(paymentRepository.findAll().spliterator(), false)
                                .filter(payment -> payment.getPaymentReservations() != null &&
                                        payment.getPaymentReservations().stream()
                                                .anyMatch(pr -> pr.getReservation() != null &&
                                                        pr.getReservation().getId().equals(reservation.getId())))
                                .collect(Collectors.toList());

                        // Șterge legăturile payment_reservations
                        for (Payment payment : associatedPayments) {
                            payment.getPaymentReservations().removeIf(pr ->
                                    pr.getReservation() != null &&
                                            pr.getReservation().getId().equals(reservation.getId()));
                            paymentRepository.save(payment);
                        }

                        logger.debug("Șterse legăturile pentru rezervarea {}", reservation.getId());
                    } catch (Exception e) {
                        logger.warn("Nu s-au putut șterge legăturile pentru rezervarea {}: {}",
                                reservation.getId(), e.getMessage());
                    }
                }

                // Acum șterge rezervările
                reservationRepository.deleteAll(existingReservations);
                logger.info("Rezervări șterse cu succes");
            }
        } catch (Exception e) {
            logger.error("Eroare la ștergerea rezervărilor existente", e);
            throw new RuntimeException("Eroare la ștergerea rezervărilor existente: " + e.getMessage());
        }
    }

    /**
     * Obține profilurile eligibile pentru generarea rezervărilor
     */
    private List<ReservationProfile> getEligibleProfiles() {
        return StreamSupport
                .stream(reservationProfileRepository.findAll().spliterator(), false)
                .filter(profile -> {
                    User user = profile.getUser();
                    return user != null &&
                            user.getTeamType() != null && !user.getTeamType().isEmpty() &&
                            ("active".equals(user.getAccountStatus()) || "verified".equals(user.getAccountStatus())) &&
                            profile.getSelectedHalls() != null && !profile.getSelectedHalls().isEmpty();
                })
                .collect(Collectors.toList());
    }

    /**
     * Sortează profilurile după prioritate (seniori > juniori mari > juniori mici)
     */
    private List<ReservationProfile> prioritizeProfiles(List<ReservationProfile> profiles) {
        return profiles.stream()
                .sorted((p1, p2) -> {
                    int priority1 = getAgePriority(p1.getAgeCategory());
                    int priority2 = getAgePriority(p2.getAgeCategory());

                    if (priority1 != priority2) {
                        return Integer.compare(priority2, priority1); // Prioritate mai mare = mai întâi
                    }

                    // La aceeași prioritate, sortează după buget (mai mare = mai întâi)
                    return p2.getWeeklyBudget().compareTo(p1.getWeeklyBudget());
                })
                .collect(Collectors.toList());
    }

    /**
     * Returnează prioritatea pe baza categoriei de vârstă
     */
    private int getAgePriority(String ageCategory) {
        if (ageCategory == null) return 0;

        switch (ageCategory.toLowerCase()) {
            case "senior":
            case "seniori": return 100;
            case "17-18": return 80;
            case "15-16": return 60;
            case "13-14": return 40;
            case "0-14": return 20;
            default: return 10;
        }
    }

    /**
     * Obține toate sălile active
     */
    private List<SportsHall> getActiveHalls() {
        return StreamSupport
                .stream(sportsHallRepository.findAll().spliterator(), false)
                .filter(hall -> hall.getStatus() == SportsHall.HallStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    /**
     * Creează matricea de disponibilitate pentru toate sălile și zilele săptămânii
     */
    private Map<String, Map<String, Map<String, Boolean>>> createAvailabilityMatrix(
            List<SportsHall> halls, Calendar weekStart) {

        Map<String, Map<String, Map<String, Boolean>>> matrix = new HashMap<>();
        List<String> timeSlots = generateTimeSlots();

        for (SportsHall hall : halls) {
            Map<String, Map<String, Boolean>> hallSchedule = new HashMap<>();

            // Pentru fiecare zi a săptămânii
            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                Calendar currentDay = (Calendar) weekStart.clone();
                currentDay.add(Calendar.DAY_OF_YEAR, dayOffset);
                String dateKey = formatDate(currentDay.getTime());

                Map<String, Boolean> daySlots = new HashMap<>();

                // Verifică dacă sala are program în această zi
                int dayOfWeek = getDayOfWeekForSchedule(currentDay.get(Calendar.DAY_OF_WEEK));
                Schedule daySchedule = getHallScheduleForDay(hall.getId(), dayOfWeek);

                for (String timeSlot : timeSlots) {
                    boolean isAvailable = daySchedule != null &&
                            isSlotInWorkingHours(timeSlot, daySchedule);
                    daySlots.put(timeSlot, isAvailable);
                }

                hallSchedule.put(dateKey, daySlots);
            }

            matrix.put(hall.getId().toString(), hallSchedule);
        }

        // Marchează sloturile deja rezervate
        markExistingReservations(matrix, weekStart);

        return matrix;
    }

    /**
     * Obține programul unei săli pentru o anumită zi
     */
    private Schedule getHallScheduleForDay(Long hallId, int dayOfWeek) {
        List<Schedule> schedules = scheduleRepository.findBySportsHallIdAndIsActiveOrderByDayOfWeek(hallId, true);
        return schedules.stream()
                .filter(schedule -> schedule.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElse(null);
    }

    /**
     * Verifică dacă un slot este în orele de funcționare
     */
    private boolean isSlotInWorkingHours(String timeSlot, Schedule schedule) {
        try {
            String[] slotParts = timeSlot.split(" - ");
            String slotStart = slotParts[0];
            String slotEnd = slotParts[1];

            int slotStartMinutes = timeToMinutes(slotStart);
            int slotEndMinutes = timeToMinutes(slotEnd);
            int workingStartMinutes = timeToMinutes(schedule.getStartTime());
            int workingEndMinutes = timeToMinutes(schedule.getEndTime());

            return slotStartMinutes >= workingStartMinutes && slotEndMinutes <= workingEndMinutes;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convertește timpul în minute
     */
    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    /**
     * Convertește Calendar DAY_OF_WEEK la formatul backend (1=Luni, 7=Duminică)
     */
    private int getDayOfWeekForSchedule(int calendarDayOfWeek) {
        return calendarDayOfWeek == Calendar.SUNDAY ? 7 : calendarDayOfWeek - 1;
    }

    /**
     * Marchează sloturile deja rezervate în matrice
     */
    private void markExistingReservations(Map<String, Map<String, Map<String, Boolean>>> matrix,
                                          Calendar weekStart) {
        Calendar weekEnd = getNextWeekEnd(weekStart);

        StreamSupport
                .stream(reservationRepository.findAll().spliterator(), false)
                .filter(res -> res.getDate() != null &&
                        !res.getDate().before(weekStart.getTime()) &&
                        !res.getDate().after(weekEnd.getTime()) &&
                        !"CANCELLED".equals(res.getStatus()))
                .forEach(reservation -> {
                    String hallId = reservation.getHall().getId().toString();
                    String dateKey = formatDate(reservation.getDate());
                    String timeSlot = reservation.getTimeSlot();

                    if (matrix.containsKey(hallId) &&
                            matrix.get(hallId).containsKey(dateKey) &&
                            matrix.get(hallId).get(dateKey).containsKey(timeSlot)) {
                        matrix.get(hallId).get(dateKey).put(timeSlot, false);
                    }
                });
    }

    /**
     * Generează rezervări pentru un profil specific
     */
    private GenerationResult generateReservationsForProfile(
            ReservationProfile profile,
            Map<String, Map<String, Map<String, Boolean>>> availabilityMatrix,
            Calendar weekStart) {

        GenerationResult result = new GenerationResult();

        // Determină numărul maxim de rezervări pentru profil
        int maxBookings = getMaxBookingsForProfile(profile);
        BigDecimal weeklyBudget = profile.getWeeklyBudget();

        // Filtrează sălile din profil care sunt active și în orașul corect
        List<SportsHall> profileHalls = profile.getSelectedHalls().stream()
                .filter(hall -> hall.getStatus() == SportsHall.HallStatus.ACTIVE &&
                        hall.getCity().equals(profile.getCity()))
                .collect(Collectors.toList());

        if (profileHalls.isEmpty()) {
            logger.warn("Profilul '{}' nu are săli valide în orașul {}",
                    profile.getName(), profile.getCity());
            return result;
        }

        // Găsește toate sloturile disponibile pentru acest profil
        List<AvailableSlot> availableSlots = findAvailableSlots(
                profile, profileHalls, availabilityMatrix, weekStart);

        // Selectează rezervările în funcție de buget și prioritate
        List<AvailableSlot> selectedSlots = selectSlotsWithinBudget(
                availableSlots, maxBookings, weeklyBudget);

        if (selectedSlots.isEmpty()) {
            logger.info("Nu s-au putut selecta sloturile pentru profilul '{}'", profile.getName());
            return result;
        }

        // Creează rezervările și plata cash
        BigDecimal totalCost = BigDecimal.ZERO;
        List<Reservation> reservations = new ArrayList<>();

        for (AvailableSlot slot : selectedSlots) {
            Reservation reservation = new Reservation();
            reservation.setHall(slot.getHall());
            reservation.setUser(profile.getUser());
            reservation.setDate(slot.getDate());
            reservation.setTimeSlot(slot.getTimeSlot());
            reservation.setPrice(slot.getHall().getTariff());
            reservation.setType("reservation");
            reservation.setStatus("CONFIRMED");

            Reservation savedReservation = reservationRepository.save(reservation);
            reservations.add(savedReservation);
            totalCost = totalCost.add(BigDecimal.valueOf(slot.getHall().getTariff()));

            // Marchează slotul ca ocupat în matrice
            String hallId = slot.getHall().getId().toString();
            String dateKey = formatDate(slot.getDate());
            availabilityMatrix.get(hallId).get(dateKey).put(slot.getTimeSlot(), false);
        }

        // Creează plata cash
        if (!reservations.isEmpty()) {
            Payment payment = new Payment();
            payment.setUserId(profile.getUser().getId());
            payment.setTotalAmount(totalCost);
            payment.setPaymentMethod(PaymentMethod.CASH);
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setCurrency("RON");
            payment.setDescription("Rezervare automată - profil: " + profile.getName());

            Payment savedPayment = paymentRepository.save(payment);
            result.addPayment(savedPayment);

            // Creează legăturile payment-reservation
            for (Reservation reservation : reservations) {
                PaymentReservation paymentReservation = new PaymentReservation(savedPayment, reservation);
                savedPayment.getPaymentReservations().add(paymentReservation);
            }
            paymentRepository.save(savedPayment);
        }

        result.setReservations(reservations);
        return result;
    }

    /**
     * Găsește toate sloturile disponibile pentru un profil
     */
    private List<AvailableSlot> findAvailableSlots(
            ReservationProfile profile,
            List<SportsHall> halls,
            Map<String, Map<String, Map<String, Boolean>>> availabilityMatrix,
            Calendar weekStart) {

        List<AvailableSlot> availableSlots = new ArrayList<>();
        TimeInterval preferredInterval = getPreferredTimeInterval(profile.getTimeInterval());
        boolean isSenior = "senior".equals(profile.getAgeCategory()) || "seniori".equals(profile.getAgeCategory());

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            Calendar currentDay = (Calendar) weekStart.clone();
            currentDay.add(Calendar.DAY_OF_YEAR, dayOffset);
            String dateKey = formatDate(currentDay.getTime());

            for (SportsHall hall : halls) {
                String hallId = hall.getId().toString();

                if (!availabilityMatrix.containsKey(hallId) ||
                        !availabilityMatrix.get(hallId).containsKey(dateKey)) {
                    continue;
                }

                Map<String, Boolean> daySlots = availabilityMatrix.get(hallId).get(dateKey);

                for (Map.Entry<String, Boolean> entry : daySlots.entrySet()) {
                    String timeSlot = entry.getKey();
                    Boolean isAvailable = entry.getValue();

                    if (isAvailable && isSlotSuitableForProfile(timeSlot, preferredInterval, isSenior)) {
                        AvailableSlot slot = new AvailableSlot();
                        slot.setHall(hall);
                        slot.setDate(currentDay.getTime());
                        slot.setTimeSlot(timeSlot);
                        slot.setPriority(calculateSlotPriority(timeSlot, preferredInterval));

                        availableSlots.add(slot);
                    }
                }
            }
        }

        // Sortează după prioritate și randomizează pentru echitate
        return availableSlots.stream()
                .sorted((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority()))
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
            int totalMinutes = hour * 60 + minute;

            // Verifică intervalul preferat
            if (totalMinutes < preferredInterval.startMinutes || totalMinutes >= preferredInterval.endMinutes) {
                return false;
            }

            // Regula pentru seniori vs juniori la 14:30
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
     * Calculează prioritatea unui slot
     */
    private int calculateSlotPriority(String timeSlot, TimeInterval preferredInterval) {
        try {
            String startTime = timeSlot.split(" - ")[0];
            int hour = Integer.parseInt(startTime.split(":")[0]);
            int minute = Integer.parseInt(startTime.split(":")[1]);
            int totalMinutes = hour * 60 + minute;

            int intervalCenter = (preferredInterval.startMinutes + preferredInterval.endMinutes) / 2;
            int distance = Math.abs(totalMinutes - intervalCenter);

            return 1000 - distance; // Prioritate mai mare pentru sloturile mai apropiate de centru
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Selectează sloturile în funcție de buget
     */
    private List<AvailableSlot> selectSlotsWithinBudget(
            List<AvailableSlot> availableSlots, int maxBookings, BigDecimal weeklyBudget) {

        List<AvailableSlot> selected = new ArrayList<>();
        BigDecimal remainingBudget = weeklyBudget;
        Set<String> usedDateSlots = new HashSet<>();

        for (AvailableSlot slot : availableSlots) {
            if (selected.size() >= maxBookings) {
                break;
            }

            String dateSlotKey = formatDate(slot.getDate()) + "_" + slot.getTimeSlot();
            if (usedDateSlots.contains(dateSlotKey)) {
                continue;
            }

            BigDecimal slotPrice = BigDecimal.valueOf(slot.getHall().getTariff());
            if (remainingBudget.compareTo(slotPrice) >= 0) {
                selected.add(slot);
                remainingBudget = remainingBudget.subtract(slotPrice);
                usedDateSlots.add(dateSlotKey);
            }
        }

        return selected;
    }

    // Helper methods

    private int getMaxBookingsForProfile(ReservationProfile profile) {
        String ageCategory = profile.getAgeCategory();
        if (ageCategory == null) return 3;

        switch (ageCategory.toLowerCase()) {
            case "senior":
            case "seniori": return 6;
            case "17-18": return 5;
            case "15-16": return 4;
            case "13-14": return 3;
            case "0-14": return 3;
            default: return 3;
        }
    }

    private TimeInterval getPreferredTimeInterval(String timeInterval) {
        if (timeInterval == null) return new TimeInterval(8 * 60, 23 * 60);

        switch (timeInterval.toLowerCase()) {
            case "morning": return new TimeInterval(8 * 60, 12 * 60);
            case "afternoon": return new TimeInterval(12 * 60, 17 * 60);
            case "evening": return new TimeInterval(17 * 60, 23 * 60);
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Bucharest"));
        return sdf.format(date);
    }

    // Inner classes

    private static class GenerationResult {
        private List<Reservation> reservations = new ArrayList<>();
        private List<Payment> payments = new ArrayList<>();

        public List<Reservation> getReservations() {
            return reservations;
        }

        public void setReservations(List<Reservation> reservations) {
            this.reservations = reservations;
        }

        public List<Payment> getPayments() {
            return payments;
        }

        public void addPayment(Payment payment) {
            this.payments.add(payment);
        }
    }

    private static class AvailableSlot {
        private SportsHall hall;
        private Date date;
        private String timeSlot;
        private int priority;

        // Getters și setters
        public SportsHall getHall() {
            return hall;
        }

        public void setHall(SportsHall hall) {
            this.hall = hall;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getTimeSlot() {
            return timeSlot;
        }

        public void setTimeSlot(String timeSlot) {
            this.timeSlot = timeSlot;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    private static class TimeInterval {
        private final int startMinutes;
        private final int endMinutes;

        public TimeInterval(int startMinutes, int endMinutes) {
            this.startMinutes = startMinutes;
            this.endMinutes = endMinutes;
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Controller works! Current time: " + new Date());
    }
}