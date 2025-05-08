package licenta.service;

import licenta.model.Reservation;
import licenta.model.ReservationProfile;
import licenta.model.SportsHall;
import licenta.model.User;
import licenta.persistence.IReservationProfileRepository;
import licenta.persistence.IReservationSpringRepository;
import licenta.persistence.ISportsHallSpringRepository;
import licenta.persistence.IUserSpringRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/booking-prioritization")
public class BookingPrioritizationRestController {

    private final IReservationProfileRepository reservationProfileRepository;
    private final IUserSpringRepository userRepository;
    private final ISportsHallSpringRepository sportsHallRepository;
    private final IReservationSpringRepository reservationRepository;

    @Autowired
    public BookingPrioritizationRestController(
            IReservationProfileRepository reservationProfileRepository,
            IUserSpringRepository userRepository,
            ISportsHallSpringRepository sportsHallRepository,
            IReservationSpringRepository reservationRepository) {
        this.reservationProfileRepository = reservationProfileRepository;
        this.userRepository = userRepository;
        this.sportsHallRepository = sportsHallRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Endpoint pentru generarea automată a rezervărilor
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateBookings() {
        try {
            // Obținem data pentru săptămâna următoare
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
            Date nextMonday = calendar.getTime();

            // Calculăm sfârșitul săptămânii
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(nextMonday);
            endCal.add(Calendar.DAY_OF_YEAR, 6);
            Date nextSunday = endCal.getTime();

            // Ștergem toate rezervările anterioare din săptămâna următoare de tip "reservation"
            deleteExistingReservationsForNextWeek(nextMonday, nextSunday);

            // Generăm noi rezervări
            List<Reservation> generatedReservations = generateAutomatedBookings();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rezervările au fost generate cu succes.");
            response.put("count", generatedReservations.size());
            response.put("reservations", generatedReservations);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Eroare la generarea rezervărilor: " + e.getMessage());

            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Șterge toate rezervările existente din săptămâna următoare pentru a permite regenerarea
     */
    private void deleteExistingReservationsForNextWeek(Date startDate, Date endDate) {
        // Obținem toate rezervările existente pentru săptămâna următoare
        Iterable<Reservation> existingReservations = reservationRepository.findAll();
        List<Reservation> reservationsToDelete = new ArrayList<>();

        for (Reservation reservation : existingReservations) {
            Date reservationDate = reservation.getDate();

            // Verificăm dacă data rezervării este în săptămâna următoare și dacă este de tip "reservation"
            if (reservationDate != null
                    && !reservationDate.before(startDate)
                    && !reservationDate.after(endDate)
                    && "reservation".equals(reservation.getType())) {
                reservationsToDelete.add(reservation);
            }
        }

        // Ștergem rezervările găsite
        if (!reservationsToDelete.isEmpty()) {
            reservationRepository.deleteAll(reservationsToDelete);
        }
    }

    /**
     * Generează rezervări automate pentru săptămâna următoare
     * @return Lista de rezervări generate
     */
    private List<Reservation> generateAutomatedBookings() {
        // Obținem toate profilurile de rezervare
        List<ReservationProfile> allProfiles = StreamSupport
                .stream(reservationProfileRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        // Filtrăm profilurile pentru a include doar cele aparținând echipelor active sau verificate
        List<ReservationProfile> eligibleProfiles = allProfiles.stream()
                .filter(profile -> {
                    User user = profile.getUser();
                    // Verificăm dacă utilizatorul are team_type setat și status-ul contului este "active" sau "verified"
                    return user != null
                            && user.getTeamType() != null
                            && !user.getTeamType().isEmpty()
                            && (user.getAccountStatus().equals("active") || user.getAccountStatus().equals("verified"));
                })
                .collect(Collectors.toList());

        // Sortăm profilele după prioritate
        List<ReservationProfile> prioritizedProfiles = prioritizeProfiles(eligibleProfiles);

        // Obținem toate sălile disponibile
        List<SportsHall> allHalls = StreamSupport
                .stream(sportsHallRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        // Obținem data pentru săptămâna următoare
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        Date nextMonday = calendar.getTime();

        // Generăm un program gol pentru săptămâna următoare
        Map<Long, Map<String, Map<String, Boolean>>> availabilitySchedule = createEmptySchedule(allHalls, nextMonday);

        // Marcăm sloturile deja rezervate și cele de mentenanță
        markExistingReservations(availabilitySchedule, nextMonday);

        // Generăm rezervările automate
        List<Reservation> generatedReservations = new ArrayList<>();

        // Pentru fiecare profil, în ordinea priorității
        for (ReservationProfile profile : prioritizedProfiles) {
            // Determinăm numărul de rezervări permise pentru acest profil
            int maxBookings = getMaxBookingsPerWeek(profile);

            // Bugetul disponibil pentru săptămână
            BigDecimal weeklyBudget = profile.getWeeklyBudget();

            // Intervalul de timp preferat
            String timeInterval = profile.getTimeInterval();

            // Sălile selectate pentru acest profil
            List<SportsHall> selectedHalls = profile.getSelectedHalls();

            // Dacă nu sunt săli selectate, trecem la următorul profil
            if (selectedHalls.isEmpty()) continue;

            // Orașul din profil
            String city = profile.getCity();

            // Utilizatorul asociat profilului
            User user = profile.getUser();

            // Filtrăm sălile în funcție de oraș
            List<SportsHall> filteredHalls = selectedHalls.stream()
                    .filter(hall -> hall.getCity().equals(city))
                    .collect(Collectors.toList());

            // Generăm rezervările pentru acest profil
            List<Reservation> profileReservations = generateReservationsForProfile(
                    profile,
                    filteredHalls,
                    availabilitySchedule,
                    maxBookings,
                    weeklyBudget,
                    timeInterval,
                    user,
                    nextMonday
            );

            // Adăugăm rezervările generate la lista totală
            generatedReservations.addAll(profileReservations);

            // Actualizăm programul de disponibilitate
            updateAvailabilitySchedule(availabilitySchedule, profileReservations);
        }

        // Salvăm toate rezervările generate în baza de date
        Iterable<Reservation> savedReservations = reservationRepository.saveAll(generatedReservations);

        return StreamSupport
                .stream(savedReservations.spliterator(), false)
                .collect(Collectors.toList());
    }

    /**
     * Sortează profilele de rezervare după prioritate
     * Ordinea: Seniori > Juniori 17-18 > Juniori 15-16 > Juniori 0-14
     */
    private List<ReservationProfile> prioritizeProfiles(List<ReservationProfile> profiles) {
        return profiles.stream()
                .sorted((p1, p2) -> {
                    // Compară mai întâi categoria de vârstă
                    String age1 = p1.getAgeCategory();
                    String age2 = p2.getAgeCategory();

                    // Seniori au prioritate maximă
                    if (age1.equals("senior") && !age2.equals("senior")) {
                        return -1;
                    }
                    if (!age1.equals("senior") && age2.equals("senior")) {
                        return 1;
                    }

                    // Dacă ambele sunt juniori, sortăm după grupa de vârstă (descrescător)
                    if (!age1.equals("senior") && !age2.equals("senior")) {
                        if (age1.equals("17-18") && !age2.equals("17-18")) {
                            return -1;
                        }
                        if (!age1.equals("17-18") && age2.equals("17-18")) {
                            return 1;
                        }

                        if (age1.equals("15-16") && !age2.equals("15-16")) {
                            return -1;
                        }
                        if (!age1.equals("15-16") && age2.equals("15-16")) {
                            return 1;
                        }
                    }

                    // La egalitate, sortăm după buget (descrescător)
                    return p2.getWeeklyBudget().compareTo(p1.getWeeklyBudget());
                })
                .collect(Collectors.toList());
    }

    /**
     * Determină numărul maxim de rezervări pentru un profil bazat pe categoria de vârstă
     */
    private int getMaxBookingsPerWeek(ReservationProfile profile) {
        String ageCategory = profile.getAgeCategory();

        switch (ageCategory) {
            case "senior":
                return 6;
            case "17-18":
                return 5;
            case "15-16":
                return 4;
            case "0-14":
                return 3;
            default:
                return 3; // Valoare implicită
        }
    }

    /**
     * Creează un program gol pentru săptămâna următoare
     */
    private Map<Long, Map<String, Map<String, Boolean>>> createEmptySchedule(
            List<SportsHall> halls, Date startDate) {

        Map<Long, Map<String, Map<String, Boolean>>> schedule = new HashMap<>();

        // Lista de sloturi orare
        List<String> timeSlots = generateTimeSlots();

        for (SportsHall hall : halls) {
            Map<String, Map<String, Boolean>> hallSchedule = new HashMap<>();

            // Pentru fiecare zi din săptămâna următoare
            for (int i = 0; i < 7; i++) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                cal.add(Calendar.DAY_OF_YEAR, i);
                String dateKey = formatDate(cal.getTime());

                Map<String, Boolean> daySchedule = new HashMap<>();

                // Pentru fiecare slot orar
                for (String timeSlot : timeSlots) {
                    daySchedule.put(timeSlot, true); // true = disponibil
                }

                hallSchedule.put(dateKey, daySchedule);
            }

            schedule.put(hall.getId(), hallSchedule);
        }

        return schedule;
    }

    /**
     * Generează lista de sloturi orare conform tabelului din interfața
     * Format: "07:00 - 08:30", "08:30 - 10:00", etc.
     */
    private List<String> generateTimeSlots() {
        List<String> timeSlots = new ArrayList<>();

        // Adăugăm toate sloturile conform tabelului
        timeSlots.add("07:00 - 08:30");
        timeSlots.add("08:30 - 10:00");
        timeSlots.add("10:00 - 11:30");
        timeSlots.add("11:30 - 13:00");
        timeSlots.add("13:00 - 14:30");
        timeSlots.add("14:30 - 16:00");
        timeSlots.add("16:00 - 17:30");
        timeSlots.add("17:30 - 19:00");
        timeSlots.add("19:00 - 20:30");
        timeSlots.add("20:30 - 22:00");
        timeSlots.add("22:00 - 23:30");

        return timeSlots;
    }

    /**
     * Formatează data pentru a fi folosită ca cheie în program
     */
    private String formatDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Luna începe de la 0
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return String.format("%d-%02d-%02d", year, month, day);
    }

    /**
     * Marchează sloturile deja rezervate și cele de mentenanță în program
     */
    private void markExistingReservations(Map<Long, Map<String, Map<String, Boolean>>> schedule, Date startDate) {
        // Calculăm sfârșitul săptămânii
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DAY_OF_YEAR, 6);
        Date endDate = cal.getTime();

        // Obținem toate rezervările existente pentru săptămâna următoare
        Iterable<Reservation> existingReservations = reservationRepository.findAll();

        for (Reservation reservation : existingReservations) {
            Date reservationDate = reservation.getDate();

            // Verificăm dacă data rezervării este în săptămâna următoare
            if (reservationDate == null || reservationDate.before(startDate) || reservationDate.after(endDate)) {
                continue;
            }

            // ID-ul sălii
            Long hallId = reservation.getHall().getId();

            // Data rezervării formatată
            String dateKey = formatDate(reservationDate);

            // Slotul orar
            String timeSlot = reservation.getTimeSlot();

            // Marcăm slotul ca ocupat
            if (schedule.containsKey(hallId) &&
                    schedule.get(hallId).containsKey(dateKey) &&
                    schedule.get(hallId).get(dateKey).containsKey(timeSlot)) {

                schedule.get(hallId).get(dateKey).put(timeSlot, false);
            }
        }
    }

    /**
     * Actualizează programul cu noile rezervări generate
     */
    private void updateAvailabilitySchedule(
            Map<Long, Map<String, Map<String, Boolean>>> schedule, List<Reservation> reservations) {

        for (Reservation reservation : reservations) {
            Long hallId = reservation.getHall().getId();
            String dateKey = formatDate(reservation.getDate());
            String timeSlot = reservation.getTimeSlot();

            if (schedule.containsKey(hallId) &&
                    schedule.get(hallId).containsKey(dateKey) &&
                    schedule.get(hallId).get(dateKey).containsKey(timeSlot)) {

                schedule.get(hallId).get(dateKey).put(timeSlot, false);
            }
        }
    }

    /**
     * Generează rezervări pentru un profil specific
     */
    private List<Reservation> generateReservationsForProfile(
            ReservationProfile profile,
            List<SportsHall> halls,
            Map<Long, Map<String, Map<String, Boolean>>> schedule,
            int maxBookings,
            BigDecimal weeklyBudget,
            String timeInterval,
            User user,
            Date startDate) {

        List<Reservation> reservations = new ArrayList<>();

        // Dacă nu sunt săli disponibile, returnăm o listă goală
        if (halls.isEmpty()) {
            return reservations;
        }

        // Determinăm intervalul de ore preferat
        final int preferredStartHour;
        final int preferredEndHour;

        // Determinăm intervalul inițial în funcție de preferința utilizatorului
        if (timeInterval != null && !timeInterval.isEmpty()) {
            if (timeInterval.equals("morning")) {
                preferredStartHour = 8;
                preferredEndHour = 12;
            } else if (timeInterval.equals("afternoon")) {
                preferredStartHour = 12;
                preferredEndHour = 17;
            } else if (timeInterval.equals("evening")) {
                preferredStartHour = 17;
                preferredEndHour = 23;
            } else {
                preferredStartHour = 8;
                preferredEndHour = 23;
            }
        } else {
            preferredStartHour = 8;
            preferredEndHour = 23;
        }

        // Bugetul rămas
        BigDecimal remainingBudget = profile.getWeeklyBudget();

        // Numărul de rezervări generate până acum
        int generatedBookings = 0;

        // Generăm date aleatorii pentru săptămâna următoare
        List<Date> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DAY_OF_YEAR, i);
            weekDates.add(cal.getTime());
        }

        // Amestecăm datele pentru a evita favoritism pentru anumite zile
        Collections.shuffle(weekDates);

        // Pentru fiecare zi din săptămână
        for (Date date : weekDates) {
            // Dacă am generat deja numărul maxim de rezervări, ne oprim
            if (generatedBookings >= maxBookings) {
                break;
            }

            // Formatăm data pentru a fi folosită ca cheie în program
            String dateKey = formatDate(date);

            // Amestecăm sălile pentru a evita favoritism
            List<SportsHall> shuffledHalls = new ArrayList<>(halls);
            Collections.shuffle(shuffledHalls);

            // Pentru fiecare sală
            for (SportsHall hall : shuffledHalls) {
                // Dacă am generat deja numărul maxim de rezervări, ne oprim
                if (generatedBookings >= maxBookings) {
                    break;
                }

                // Dacă sala nu are un program, trecem la următoarea
                if (!schedule.containsKey(hall.getId()) || !schedule.get(hall.getId()).containsKey(dateKey)) {
                    continue;
                }

                // Verificăm dacă bugetul acoperă tariful
                if (remainingBudget.compareTo(BigDecimal.valueOf(hall.getTariff())) < 0) {
                    continue;
                }

                // Obținem sloturile orare disponibile pentru această zi și sală
                Map<String, Boolean> daySchedule = schedule.get(hall.getId()).get(dateKey);

                // Filtrăm sloturile în funcție de intervalul de ore preferat și categoria de vârstă
                List<String> availableSlots = daySchedule.entrySet().stream()
                        .filter(Map.Entry::getValue) // Doar sloturile disponibile
                        .map(Map.Entry::getKey)
                        .filter(slotKey -> {
                            // Extragem ora din slotul orar (format: "HH:MM - HH:MM")
                            String startTime = slotKey.split(" - ")[0];
                            int slotHour = Integer.parseInt(startTime.split(":")[0]);
                            int slotMinute = Integer.parseInt(startTime.split(":")[1]);

                            // Verificăm dacă slotul este în intervalul preferat al utilizatorului
                            boolean isInPreferredInterval = slotHour >= preferredStartHour && slotHour < preferredEndHour;

                            // Aplicăm regula pentru seniori/juniori în funcție de ora 14:30
                            if (profile.getAgeCategory().equals("senior")) {
                                // Pentru seniori: inclusiv până la ora 14, dar excludem 14:30
                                return isInPreferredInterval && (slotHour < 14 || (slotHour == 14 && slotMinute == 0));
                            } else {
                                // Pentru juniori: începând cu ora 14:30
                                return isInPreferredInterval && (slotHour > 14 || (slotHour == 14 && slotMinute == 30));
                            }
                        })
                        .collect(Collectors.toList());

                // Dacă nu sunt sloturi disponibile, trecem la următoarea sală
                if (availableSlots.isEmpty()) {
                    continue;
                }

                // Amestecăm sloturile pentru a evita favoritism
                Collections.shuffle(availableSlots);

                // Alegem primul slot disponibil
                String selectedSlot = availableSlots.get(0);

                // Creăm rezervarea
                Reservation reservation = new Reservation();
                reservation.setHall(hall);
                reservation.setUser(user);
                reservation.setDate(date);
                reservation.setTimeSlot(selectedSlot);
                reservation.setPrice(hall.getTariff());
                reservation.setType("reservation");

                // Adăugăm rezervarea la lista de rezervări generate
                reservations.add(reservation);

                // Actualizăm bugetul rămas
                remainingBudget = remainingBudget.subtract(BigDecimal.valueOf(hall.getTariff()));

                // Incrementăm contorul de rezervări generate
                generatedBookings++;

                // Marcăm slotul ca ocupat în program
                schedule.get(hall.getId()).get(dateKey).put(selectedSlot, false);

                // Trecem la următoarea zi
                break;
            }
        }

        return reservations;
    }
}