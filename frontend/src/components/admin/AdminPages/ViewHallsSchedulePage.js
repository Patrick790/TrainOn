import React, { Component } from 'react';
import { Info, AlertCircle, Wrench, Filter } from 'lucide-react';
import ScheduleTableComponent from '../../halladmin/AdminPages/ScheduleTableComponent';
import BookingPrioritization from './BookingPrioritization';
import './ViewHallsSchedulePage.css';

class ViewHallsSchedulePage extends Component {
    constructor(props) {
        super(props);

        this.state = {
            halls: [],
            filteredHalls: [],
            cities: [],
            selectedHallId: null,
            selectedCity: 'all',
            currentWeekStart: null,
            schedule: {},
            maintenanceCount: 0,
            originalMaintenanceSlots: [],
            changedMaintenanceSlots: [],
            initialLoading: true,
            tableLoading: false,
            error: null,
            successMessage: '',
            errorMessage: '',
            showMessage: false,
            savingMaintenance: false,
            maxMaintenanceSlots: null,
            reservations: [] // Adăugăm starea pentru lista de rezervări
        };
    }

    async componentDidMount() {
        await this.fetchAllHalls();
    }

    // API calls
    fetchAllHalls = async () => {
        try {
            const token = localStorage.getItem('jwtToken');

            if (!token) {
                this.setState({
                    error: 'Nu sunteți autentificat corect',
                    initialLoading: false,
                    halls: []
                });
                return;
            }

            // Obținem toate sălile
            const response = await fetch(`http://localhost:8080/sportsHalls`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();

            // Extragem toate orașele unice
            const uniqueCities = [...new Set(halls.map(hall => hall.city))];
            uniqueCities.sort();

            const selectedHallId = halls.length > 0 ? halls[0].id : null;

            this.setState({
                halls,
                filteredHalls: halls,
                cities: uniqueCities,
                selectedHallId,
                initialLoading: false,
                currentWeekStart: this.getStartOfWeek(new Date())
            }, () => {
                if (selectedHallId) {
                    this.fetchSchedule(selectedHallId, this.state.currentWeekStart);
                }
            });
        } catch (error) {
            console.error('Error fetching halls:', error);
            this.setState({
                error: error.message,
                initialLoading: false,
                halls: []
            });
        }
    }

    getStartOfWeek = (date) => {
        const dayOfWeek = date.getDay();
        const diff = date.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
        const startOfWeek = new Date(date);
        startOfWeek.setDate(diff);
        startOfWeek.setHours(0, 0, 0, 0);
        return startOfWeek;
    }

    formatDate = (date) => {
        return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
    }

    createEmptySchedule = (weekStart) => {
        const schedule = {};

        // Generăm sloturile orare în formatul "HH:MM - HH:MM"
        const timeSlots = [
            "07:00 - 08:30",
            "08:30 - 10:00",
            "10:00 - 11:30",
            "11:30 - 13:00",
            "13:00 - 14:30",
            "14:30 - 16:00",
            "16:00 - 17:30",
            "17:30 - 19:00",
            "19:00 - 20:30",
            "20:30 - 22:00",
            "22:00 - 23:30"
        ];

        // Creăm programul gol pentru toată săptămâna
        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(weekStart);
            currentDate.setDate(currentDate.getDate() + i);
            const dateKey = this.formatDate(currentDate);

            schedule[dateKey] = {};
            timeSlots.forEach(slot => {
                schedule[dateKey][slot] = 'available';
            });
        }

        return schedule;
    }

    fetchSchedule = async (hallId, weekStart) => {
        try {
            this.setState({ tableLoading: true });

            const token = localStorage.getItem('jwtToken');
            if (!token) throw new Error('Nu sunteți autentificat corect');

            const emptySchedule = this.createEmptySchedule(weekStart);
            const startDateStr = this.formatDate(weekStart);

            // Calculăm sfârșitul săptămânii
            const endDate = new Date(weekStart);
            endDate.setDate(endDate.getDate() + 6);
            const endDateStr = this.formatDate(endDate);

            // Obținem toate datele necesare în paralel
            const [maintenanceReservations, bookingReservations] = await Promise.all([
                this.fetchMaintenanceReservations(hallId, startDateStr, token),
                this.fetchBookingReservations(hallId, weekStart, token)
            ]);

            // Actualizăm programul cu rezervările existente
            const updatedSchedule = { ...emptySchedule };
            const originalMaintenanceSlots = [];

            // Marcăm sloturile de mentenanță
            maintenanceReservations.forEach(reservation => {
                const dateKey = this.formatDate(new Date(reservation.date));
                const slotId = reservation.timeSlot;

                if (updatedSchedule[dateKey] && updatedSchedule[dateKey][slotId]) {
                    updatedSchedule[dateKey][slotId] = 'maintenance';
                    originalMaintenanceSlots.push({
                        id: reservation.id,
                        date: dateKey,
                        timeSlot: slotId
                    });
                }
            });

            // Marcăm sloturile rezervate
            bookingReservations.forEach(reservation => {
                const dateKey = this.formatDate(new Date(reservation.date));
                const slotId = reservation.timeSlot;

                if (updatedSchedule[dateKey] && updatedSchedule[dateKey][slotId]) {
                    updatedSchedule[dateKey][slotId] = 'booked';
                }
            });

            // Numărăm intervalele de mentenanță
            let maintenanceCount = 0;
            Object.values(updatedSchedule).forEach(daySchedule => {
                Object.values(daySchedule).forEach(status => {
                    if (status === 'maintenance') maintenanceCount++;
                });
            });

            // Pentru a afișa informațiile despre echipe și profiluri, trebuie să interogăm și detaliile rezervărilor
            // Obținem informațiile despre profilurile de rezervare și utilizatori pentru fiecare rezervare
            const enhancedReservations = await this.enhanceReservationsWithDetails(bookingReservations, token);

            this.setState({
                schedule: updatedSchedule,
                maintenanceCount,
                originalMaintenanceSlots,
                changedMaintenanceSlots: [],
                tableLoading: false,
                reservations: enhancedReservations // Adăugăm rezervările îmbogățite în state
            });
        } catch (error) {
            console.error('Error fetching schedule:', error);
            this.setState({
                error: 'Eroare la încărcarea programului',
                tableLoading: false
            });
        }
    }

    // Metodă pentru îmbogățirea rezervărilor cu informații despre echipe și profiluri
    enhanceReservationsWithDetails = async (reservations, token) => {
        // Adăugăm informații despre rezervare (nume echipă, nume profil, etc.)
        // Dacă este necesar, putem face cereri suplimentare pentru a obține aceste informații

        // Deoarece nu știm exact formatul datelor din backend, vom simula acest proces
        // În implementarea reală, s-ar putea face cereri API pentru a obține datele asociate

        return reservations.map(reservation => {
            // Verificăm dacă există utilizator și adăugăm informațiile
            if (reservation.user) {
                // Dacă utilizatorul are un nume de echipă, îl folosim, altfel folosim numele utilizatorului
                reservation.user.teamName = reservation.user.teamName || reservation.user.name || 'Echipă nespecificată';

                // Adăugăm numele profilului de rezervare (în implementarea reală ar putea proveni din alt endpoint)
                // Aici simulăm această informație
                reservation.profileName = 'Profil ' + (reservation.user.teamType || 'Standard');
            }

            return reservation;
        });
    }

    fetchMaintenanceReservations = async (hallId, startDateStr, token) => {
        const maintenanceResponse = await fetch(
            `http://localhost:8080/reservations/maintenance/week?hallId=${hallId}&weekStart=${startDateStr}`,
            {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            }
        );

        if (!maintenanceResponse.ok) {
            throw new Error(`Eroare la încărcarea intervalelor de mentenanță: ${maintenanceResponse.status}`);
        }

        return maintenanceResponse.json();
    }

    fetchBookingReservations = async (hallId, weekStart, token) => {
        // Creăm un array cu toate zilele săptămânii
        const weekDates = [];
        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(weekStart);
            currentDate.setDate(currentDate.getDate() + i);
            weekDates.push(this.formatDate(currentDate));
        }

        // Obținem rezervările pentru fiecare zi
        const bookingPromises = weekDates.map(dateStr =>
            fetch(`http://localhost:8080/reservations?hallId=${hallId}&date=${dateStr}&type=reservation`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            }).then(response => {
                if (!response.ok) {
                    throw new Error(`Eroare la încărcarea rezervărilor: ${response.status}`);
                }
                return response.json();
            })
        );

        const allBookingResponses = await Promise.all(bookingPromises);
        return allBookingResponses.flat();
    }

    // Event handlers
    handleHallChange = (event) => {
        const hallId = event.target.value;

        this.setState({
            selectedHallId: hallId,
            tableLoading: true,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        }, () => {
            this.fetchSchedule(hallId, this.state.currentWeekStart);
        });
    }

    handleCityChange = (event) => {
        const city = event.target.value;
        let filteredHalls;

        if (city === 'all') {
            filteredHalls = [...this.state.halls];
        } else {
            filteredHalls = this.state.halls.filter(hall => hall.city === city);
        }

        const selectedHallId = filteredHalls.length > 0 ? filteredHalls[0].id : null;

        this.setState({
            selectedCity: city,
            filteredHalls,
            selectedHallId,
            tableLoading: true,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        }, () => {
            if (selectedHallId) {
                this.fetchSchedule(selectedHallId, this.state.currentWeekStart);
            }
        });
    }

    handleWeekChange = (newWeekStart) => {
        this.setState({
            currentWeekStart: newWeekStart,
            tableLoading: true,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        }, () => {
            this.fetchSchedule(this.state.selectedHallId, newWeekStart);
        });
    }

    toggleMaintenanceSlot = (date, slotId) => {
        // Check if the date is in the past or today
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const slotDate = new Date(date);
        slotDate.setHours(0, 0, 0, 0);

        // Block both current day and past days
        if (slotDate <= today) {
            this.setState({
                errorMessage: 'Nu puteți marca mentenanță pentru ziua curentă sau pentru zile din trecut',
                showMessage: true
            });
            return;
        }

        this.setState(prevState => {
            // Verificăm dacă avem schedule[date] și schedule[date][slotId]
            if (!prevState.schedule[date]) {
                console.error(`Date ${date} not found in schedule`);
                return {
                    errorMessage: 'Eroare la modificarea slotului: data nu a fost găsită în program',
                    showMessage: true
                };
            }

            if (!prevState.schedule[date][slotId]) {
                console.error(`Slot ${slotId} not found in schedule for date ${date}`);
                return {
                    errorMessage: 'Eroare la modificarea slotului: intervalul orar nu a fost găsit în program',
                    showMessage: true
                };
            }

            const updatedSchedule = JSON.parse(JSON.stringify(prevState.schedule));
            const currentStatus = updatedSchedule[date][slotId];
            let updatedMaintenanceCount = prevState.maintenanceCount;
            let updatedChangedSlots = [...prevState.changedMaintenanceSlots];

            // Check if the slot is already in maintenance
            if (currentStatus === 'maintenance') {
                return this.handleRemoveMaintenance(
                    updatedSchedule, updatedMaintenanceCount, updatedChangedSlots,
                    date, slotId, prevState.originalMaintenanceSlots
                );
            }

            // Check if the slot is already booked
            if (currentStatus === 'booked') {
                return {
                    errorMessage: 'Nu puteți marca pentru mentenanță un interval deja rezervat',
                    showMessage: true
                };
            }

            // No maintenance slot limit check here - unlimited maintenance slots allowed
            return this.handleAddMaintenance(
                updatedSchedule, updatedMaintenanceCount, updatedChangedSlots,
                date, slotId, prevState.originalMaintenanceSlots
            );
        });
    }

    handleRemoveMaintenance = (schedule, maintenanceCount, changedSlots, date, slotId, originalSlots) => {
        schedule[date][slotId] = 'available';
        maintenanceCount--;

        const existingIndex = changedSlots.findIndex(
            slot => slot.date === date && slot.timeSlot === slotId
        );

        const originalIndex = originalSlots.findIndex(
            slot => slot.date === date && slot.timeSlot === slotId
        );

        if (existingIndex !== -1) {
            if (originalIndex !== -1) {
                changedSlots[existingIndex].action = 'delete';
                changedSlots[existingIndex].id = originalSlots[originalIndex].id;
            } else {
                changedSlots.splice(existingIndex, 1);
            }
        } else if (originalIndex !== -1) {
            changedSlots.push({
                date,
                timeSlot: slotId,
                id: originalSlots[originalIndex].id,
                action: 'delete'
            });
        }

        return {
            schedule,
            maintenanceCount,
            changedMaintenanceSlots: changedSlots,
            errorMessage: '',
            showMessage: false
        };
    }

    handleAddMaintenance = (schedule, maintenanceCount, changedSlots, date, slotId, originalSlots) => {
        schedule[date][slotId] = 'maintenance';
        maintenanceCount++;

        const existingIndex = changedSlots.findIndex(
            slot => slot.date === date && slot.timeSlot === slotId
        );

        const originalIndex = originalSlots.findIndex(
            slot => slot.date === date && slot.timeSlot === slotId
        );

        if (existingIndex !== -1) {
            if (originalIndex !== -1 && changedSlots[existingIndex].action === 'delete') {
                changedSlots.splice(existingIndex, 1);
            } else {
                changedSlots[existingIndex].action = 'add';
            }
        } else if (originalIndex === -1) {
            changedSlots.push({
                date,
                timeSlot: slotId,
                action: 'add'
            });
        }

        return {
            schedule,
            maintenanceCount,
            changedMaintenanceSlots: changedSlots,
            errorMessage: '',
            showMessage: false
        };
    }

    saveMaintenance = async () => {
        try {
            const { changedMaintenanceSlots, selectedHallId } = this.state;
            const token = localStorage.getItem('jwtToken');

            if (!token) {
                throw new Error('Nu sunteți autentificat corect');
            }

            if (changedMaintenanceSlots.length === 0) {
                this.showTemporaryMessage('Nu există modificări de mentenanță de salvat', true);
                return;
            }

            this.setState({ savingMaintenance: true });

            // Process slots that need to be added and deleted
            await Promise.all([
                ...this.processAddMaintenanceSlots(changedMaintenanceSlots, selectedHallId, token),
                ...this.processDeleteMaintenanceSlots(changedMaintenanceSlots, token)
            ]);

            // Reload schedule
            await this.fetchSchedule(selectedHallId, this.state.currentWeekStart);

            this.showTemporaryMessage('Intervalele de mentenanță au fost salvate cu succes!', true);
            this.setState({ savingMaintenance: false });
        } catch (error) {
            console.error('Error saving maintenance slots:', error);
            this.setState({
                errorMessage: `A apărut o eroare la salvarea intervalelor de mentenanță: ${error.message}`,
                showMessage: true,
                savingMaintenance: false
            });
        }
    }

    showTemporaryMessage = (message, isSuccess) => {
        this.setState({
            successMessage: isSuccess ? message : '',
            errorMessage: isSuccess ? '' : message,
            showMessage: true
        });

        setTimeout(() => {
            this.setState({ showMessage: false });
        }, 3000);
    }

    processAddMaintenanceSlots = (changedSlots, hallId, token) => {
        return changedSlots
            .filter(slot => slot.action === 'add')
            .map(slot => {
                const dateParts = slot.date.split('-');
                const dateObj = new Date(dateParts[0], dateParts[1] - 1, dateParts[2]);

                const maintenanceReservation = {
                    hall: { id: hallId },
                    user: null,
                    date: dateObj,
                    timeSlot: slot.timeSlot,
                    price: null,
                    type: 'maintenance'
                };

                return fetch('http://localhost:8080/reservations', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify(maintenanceReservation)
                });
            });
    }

    processDeleteMaintenanceSlots = (changedSlots, token) => {
        return changedSlots
            .filter(slot => slot.action === 'delete' && slot.id)
            .map(slot => {
                return fetch(`http://localhost:8080/reservations/${slot.id}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
            });
    }

    // Handler pentru finalizarea generării rezervărilor
    handleGenerationComplete = (success, errorMessage) => {
        if (success) {
            this.showTemporaryMessage('Rezervările au fost generate cu succes.', true);
            // Reîncărcăm programul pentru a afișa noile rezervări
            this.fetchSchedule(this.state.selectedHallId, this.state.currentWeekStart);
        } else {
            this.showTemporaryMessage(`Eroare la generarea rezervărilor: ${errorMessage || 'Eroare necunoscută'}`, false);
        }
    };

    render() {
        const {
            halls,
            filteredHalls,
            cities,
            selectedCity,
            selectedHallId,
            currentWeekStart,
            initialLoading,
            tableLoading,
            error,
            schedule,
            maintenanceCount,
            changedMaintenanceSlots,
            successMessage,
            errorMessage,
            showMessage,
            savingMaintenance,
            reservations
        } = this.state;

        if (initialLoading) {
            return <div className="global-admin-page">
                <h2 className="global-admin-page-title">Vizualizarea programului sălilor</h2>
                <div className="global-admin-page-content">
                    <div className="vhsp-loading-container">Se încarcă sălile...</div>
                </div>
            </div>;
        }

        if (error && halls.length === 0) {
            return <div className="global-admin-page">
                <h2 className="global-admin-page-title">Vizualizarea programului sălilor</h2>
                <div className="global-admin-page-content">
                    <div className="vhsp-error-container">Eroare: {error}</div>
                </div>
            </div>;
        }

        const selectedHall = filteredHalls.find(hall => hall.id === parseInt(selectedHallId));
        const hasMaintenanceChanges = changedMaintenanceSlots.length > 0;

        return (
            <div className="global-admin-page">
                <h2 className="global-admin-page-title">Vizualizarea programului sălilor</h2>
                <div className="global-admin-page-content">
                    {halls.length === 0 ? (
                        <div className="vhsp-empty-state">
                            <p>Nu există săli de sport în baza de date.</p>
                        </div>
                    ) : (
                        <>
                            {showMessage && (successMessage || errorMessage) && (
                                <div className={`vhsp-notification-message ${successMessage ? 'vhsp-success' : 'vhsp-error'}`}>
                                    {successMessage ? successMessage : errorMessage}
                                </div>
                            )}

                            {/* Componenta de prioritizare */}
                            <BookingPrioritization
                                onGenerationComplete={this.handleGenerationComplete}
                            />

                            <div className="vhsp-filters">
                                <div className="vhsp-city-filter">
                                    <label htmlFor="city-select">
                                        <Filter size={16} className="vhsp-filter-icon" />
                                        Filtrează după oraș:
                                    </label>
                                    <select
                                        id="city-select"
                                        value={selectedCity}
                                        onChange={this.handleCityChange}
                                        className="vhsp-city-select-input"
                                    >
                                        <option value="all">Toate orașele</option>
                                        {cities.map(city => (
                                            <option key={city} value={city}>
                                                {city}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <div className="vhsp-schedule-controls">
                                <div className="vhsp-hall-selector">
                                    <label htmlFor="hall-select">Selectați sala:</label>
                                    <select
                                        id="hall-select"
                                        value={selectedHallId || ''}
                                        onChange={this.handleHallChange}
                                        className="vhsp-hall-select-input"
                                    >
                                        {filteredHalls.map(hall => (
                                            <option key={hall.id} value={hall.id}>
                                                {hall.name}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {selectedHall && (
                                    <div className="vhsp-selected-hall-info">
                                        <span className="vhsp-selected-hall-name">{selectedHall.name}</span>
                                        <span className="vhsp-selected-hall-location">{selectedHall.city}, {selectedHall.county}</span>
                                    </div>
                                )}
                            </div>

                            {/* Transmitem și informațiile despre rezervări către ScheduleTableComponent */}
                            <ScheduleTableComponent
                                schedule={schedule}
                                tableLoading={tableLoading}
                                maintenanceCount={maintenanceCount}
                                maxMaintenanceSlots={null} // No limit
                                onToggleMaintenance={this.toggleMaintenanceSlot}
                                onWeekChange={this.handleWeekChange}
                                isPastDateBlocked={true}
                                initialWeekStart={currentWeekStart}
                                restrictPastNavigation={true}
                                restrictFutureNavigation={true}
                                maxFutureWeeks={8}
                                isReadOnly={false}
                                showWeekNavigation={true}
                                reservations={reservations} // Transmitem rezervările către componentă
                            />

                            <div className="vhsp-schedule-actions">
                                <div className="vhsp-maintenance-info">
                                    {hasMaintenanceChanges && (
                                        <div className="vhsp-maintenance-changes-pending">
                                            <Info size={16} />
                                            <span>Aveți modificări nesalvate la intervalele de mentenanță</span>
                                        </div>
                                    )}
                                </div>
                                <div className="vhsp-action-buttons">
                                    <button
                                        className="vhsp-maintenance-button"
                                        onClick={this.saveMaintenance}
                                        disabled={savingMaintenance || !hasMaintenanceChanges}
                                    >
                                        {savingMaintenance ? (
                                            <>
                                                <div className="vhsp-button-spinner"></div>
                                                Se salvează...
                                            </>
                                        ) : (
                                            <>
                                                <Wrench size={16} />
                                                Salvează mentenanță
                                            </>
                                        )}
                                    </button>
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </div>
        );
    }
}

export default ViewHallsSchedulePage;