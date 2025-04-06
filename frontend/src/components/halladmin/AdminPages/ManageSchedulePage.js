import React, { Component } from 'react';
import { ChevronLeft, ChevronRight, Calendar, Info, AlertCircle, Wrench } from 'lucide-react';
import './ManageSchedulePage.css';

class ManageSchedulePage extends Component {
    constructor(props) {
        super(props);

        // Definirea intervalelor orare pentru fiecare zi
        const timeSlots = [];
        for (let hour = 7; hour <= 22; hour += 1.5) {
            const startHour = Math.floor(hour);
            const startMinute = (hour - startHour) * 60;
            const endHour = Math.floor(hour + 1.5);
            const endMinute = ((hour + 1.5) - endHour) * 60;

            timeSlots.push({
                id: `${startHour}_${startMinute}`,
                startTime: `${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}`,
                endTime: `${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}`,
                display: `${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')} - ${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}`
            });
        }

        this.state = {
            halls: [],
            selectedHallId: null,
            currentWeekStart: this.getStartOfWeek(new Date()),
            timeSlots: timeSlots,
            schedule: {},
            maintenanceCount: 0,
            originalMaintenanceSlots: [], // Stocare pentru intervalele inițiale de mentenanță
            changedMaintenanceSlots: [], // Stocare pentru intervalele modificate de mentenanță
            loading: true,
            error: null,
            successMessage: '',
            errorMessage: '',
            showMessage: false,
            savingMaintenance: false
        };
    }

    async componentDidMount() {
        await this.fetchHalls();
    }

    fetchHalls = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                this.setState({
                    error: 'Nu sunteți autentificat corect',
                    loading: false,
                    halls: []
                });
                return;
            }

            const response = await fetch(`http://localhost:8080/sportsHalls/admin/${userId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();

            // Dacă avem cel puțin o sală, o selectăm automat
            const selectedHallId = halls.length > 0 ? halls[0].id : null;

            this.setState({
                halls,
                selectedHallId,
                loading: false
            }, () => {
                if (selectedHallId) {
                    this.fetchSchedule(selectedHallId, this.state.currentWeekStart);
                }
            });
        } catch (error) {
            console.error('Error fetching halls:', error);
            this.setState({
                error: error.message,
                loading: false,
                halls: []
            });
        }
    }

    fetchSchedule = async (hallId, weekStart) => {
        try {
            this.setState({ loading: true });

            const token = localStorage.getItem('jwtToken');

            if (!token) {
                throw new Error('Nu sunteți autentificat corect');
            }

            // Creăm un program gol în care toate sloturile sunt disponibile
            const emptySchedule = this.createEmptySchedule(weekStart);

            // Formatăm data de început a săptămânii pentru API
            const startDateStr = this.formatDate(weekStart);

            // Obținem toate rezervările de mentenanță pentru această săptămână
            const maintenanceResponse = await fetch(`http://localhost:8080/reservations/maintenance/week?hallId=${hallId}&weekStart=${startDateStr}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!maintenanceResponse.ok) {
                throw new Error(`Eroare la încărcarea intervalelor de mentenanță: ${maintenanceResponse.status}`);
            }

            const maintenanceReservations = await maintenanceResponse.json();

            // Obținem rezervările normale pentru fiecare zi a săptămânii
            // Creăm un array cu toate zilele săptămânii
            const weekDates = [];
            for (let i = 0; i < 7; i++) {
                const currentDate = new Date(weekStart);
                currentDate.setDate(currentDate.getDate() + i);
                weekDates.push(this.formatDate(currentDate));
            }

            // Obținem rezervările normale pentru fiecare zi
            const bookingPromises = weekDates.map(dateStr =>
                fetch(`http://localhost:8080/reservations?hallId=${hallId}&date=${dateStr}&type=reservation`, {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                }).then(response => {
                    if (!response.ok) {
                        throw new Error(`Eroare la încărcarea rezervărilor: ${response.status}`);
                    }
                    return response.json();
                })
            );

            const allBookingResponses = await Promise.all(bookingPromises);
            const bookingReservations = allBookingResponses.flat();

            // Actualizăm programul cu rezervările existente
            const updatedSchedule = { ...emptySchedule };
            const originalMaintenanceSlots = [];

            // Marcăm sloturile de mentenanță
            maintenanceReservations.forEach(reservation => {
                const dateKey = this.formatDate(new Date(reservation.date));
                const slotId = reservation.timeSlot;

                if (updatedSchedule[dateKey] && updatedSchedule[dateKey][slotId]) {
                    updatedSchedule[dateKey][slotId] = 'maintenance';

                    // Salvăm sloturile originale de mentenanță pentru a ține evidența modificărilor
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
                    if (status === 'maintenance') {
                        maintenanceCount++;
                    }
                });
            });

            this.setState({
                schedule: updatedSchedule,
                maintenanceCount,
                originalMaintenanceSlots,
                changedMaintenanceSlots: [], // Resetăm lista de modificări
                loading: false
            });
        } catch (error) {
            console.error('Error fetching schedule:', error);
            this.setState({
                error: 'Eroare la încărcarea programului',
                loading: false
            });
        }
    }

    createEmptySchedule = (weekStart) => {
        const schedule = {};

        // Pentru fiecare zi a săptămânii
        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(weekStart);
            currentDate.setDate(currentDate.getDate() + i);
            const dateKey = this.formatDate(currentDate);

            schedule[dateKey] = {};

            // Pentru fiecare interval orar, toate sunt disponibile inițial
            this.state.timeSlots.forEach(slot => {
                // Asigură-te că toate sloturile sunt 'available' la început
                schedule[dateKey][slot.id] = 'available';
            });
        }

        return schedule;
    }

    handleHallChange = (event) => {
        const hallId = event.target.value;
        this.setState({
            selectedHallId: hallId,
            loading: true,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        }, () => {
            this.fetchSchedule(hallId, this.state.currentWeekStart);
        });
    }

    handlePreviousWeek = () => {
        const { currentWeekStart } = this.state;
        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);

        // Verificăm dacă nu încercăm să mergem la o săptămână anterioară celei curente
        if (currentWeekStart.getTime() <= startOfCurrentWeek.getTime()) {
            return; // Nu permitem navigarea în trecut dincolo de săptămâna curentă
        }

        const previousWeekStart = new Date(currentWeekStart);
        previousWeekStart.setDate(previousWeekStart.getDate() - 7);

        this.setState({
            currentWeekStart: previousWeekStart,
            loading: true,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        }, () => {
            this.fetchSchedule(this.state.selectedHallId, previousWeekStart);
        });
    }

    handleNextWeek = () => {
        const { currentWeekStart } = this.state;
        const maxWeeks = 8; // Limită de 2 luni (aproximativ 8 săptămâni)

        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);

        // Calculăm diferența în milisecunde și apoi în săptămâni
        const diffTime = currentWeekStart.getTime() - startOfCurrentWeek.getTime();
        const diffWeeks = Math.ceil(diffTime / (1000 * 60 * 60 * 24 * 7));

        if (diffWeeks >= maxWeeks) {
            return; // Nu permitem navigarea mai departe de limita maximă
        }

        const nextWeekStart = new Date(currentWeekStart);
        nextWeekStart.setDate(nextWeekStart.getDate() + 7);

        this.setState({
            currentWeekStart: nextWeekStart,
            loading: true,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        }, () => {
            this.fetchSchedule(this.state.selectedHallId, nextWeekStart);
        });
    }

    toggleMaintenanceSlot = (date, slotId) => {
        this.setState(prevState => {
            const updatedSchedule = JSON.parse(JSON.stringify(prevState.schedule)); // deep copy
            const currentStatus = updatedSchedule[date][slotId];
            let updatedMaintenanceCount = prevState.maintenanceCount;
            let updatedChangedSlots = [...prevState.changedMaintenanceSlots];

            // Dacă slotul este deja în mentenanță, îl facem disponibil
            if (currentStatus === 'maintenance') {
                updatedSchedule[date][slotId] = 'available';
                updatedMaintenanceCount--;

                // Adăugăm la lista de modificări
                const existingIndex = updatedChangedSlots.findIndex(
                    slot => slot.date === date && slot.timeSlot === slotId
                );

                // Verificăm dacă acest slot era deja în lista originală
                const originalIndex = prevState.originalMaintenanceSlots.findIndex(
                    slot => slot.date === date && slot.timeSlot === slotId
                );

                if (existingIndex !== -1) {
                    // Dacă deja exista în lista de modificări, actualizăm acțiunea
                    if (originalIndex !== -1) {
                        updatedChangedSlots[existingIndex].action = 'delete';
                        updatedChangedSlots[existingIndex].id = prevState.originalMaintenanceSlots[originalIndex].id;
                    } else {
                        // Dacă a fost adăugat și apoi eliminat în aceeași sesiune, îl scoatem din lista de modificări
                        updatedChangedSlots.splice(existingIndex, 1);
                    }
                } else if (originalIndex !== -1) {
                    // Dacă era în lista originală, îl marcăm pentru ștergere
                    updatedChangedSlots.push({
                        date,
                        timeSlot: slotId,
                        id: prevState.originalMaintenanceSlots[originalIndex].id,
                        action: 'delete'
                    });
                }

                return {
                    schedule: updatedSchedule,
                    maintenanceCount: updatedMaintenanceCount,
                    changedMaintenanceSlots: updatedChangedSlots,
                    errorMessage: '',
                    showMessage: false
                };
            }

            // Dacă slotul este rezervat, nu permitem modificarea
            if (currentStatus === 'booked') {
                return {
                    errorMessage: 'Nu puteți marca pentru mentenanță un interval deja rezervat',
                    showMessage: true
                };
            }

            // Dacă încercăm să adăugăm un nou slot de mentenanță, verificăm limita de 3
            if (updatedMaintenanceCount >= 3) {
                return {
                    errorMessage: 'Nu puteți avea mai mult de 3 intervale de mentenanță pe săptămână',
                    showMessage: true
                };
            }

            // Altfel, îl setăm ca mentenanță și incrementăm contorul
            updatedSchedule[date][slotId] = 'maintenance';
            updatedMaintenanceCount++;

            // Adăugăm la lista de modificări
            const existingIndex = updatedChangedSlots.findIndex(
                slot => slot.date === date && slot.timeSlot === slotId
            );

            // Verificăm dacă acest slot era deja în lista originală
            const originalIndex = prevState.originalMaintenanceSlots.findIndex(
                slot => slot.date === date && slot.timeSlot === slotId
            );

            if (existingIndex !== -1) {
                // Dacă deja există în lista de modificări, actualizăm acțiunea
                if (originalIndex !== -1 && updatedChangedSlots[existingIndex].action === 'delete') {
                    // Dacă era marcat pentru ștergere, îl scoatem din lista de modificări
                    updatedChangedSlots.splice(existingIndex, 1);
                } else {
                    updatedChangedSlots[existingIndex].action = 'add';
                }
            } else if (originalIndex === -1) {
                // Dacă nu era în lista originală, îl adăugăm ca nou
                updatedChangedSlots.push({
                    date,
                    timeSlot: slotId,
                    action: 'add'
                });
            }

            return {
                schedule: updatedSchedule,
                maintenanceCount: updatedMaintenanceCount,
                changedMaintenanceSlots: updatedChangedSlots,
                errorMessage: '',
                showMessage: false
            };
        });
    }

    saveSchedule = async () => {
        try {
            // Aici ar trebui să fie apelul către backend pentru a salva programul
            // Pentru demo, simulăm un răspuns de succes

            this.setState({
                successMessage: 'Programul a fost salvat cu succes!',
                showMessage: true
            });

            // Ascunde mesajul după 3 secunde
            setTimeout(() => {
                this.setState({ showMessage: false });
            }, 3000);

        } catch (error) {
            console.error('Error saving schedule:', error);
            this.setState({
                errorMessage: 'A apărut o eroare la salvarea programului',
                showMessage: true
            });
        }
    }

    saveMaintenance = async () => {
        try {
            const { changedMaintenanceSlots, selectedHallId } = this.state;
            const token = localStorage.getItem('jwtToken');

            if (!token) {
                throw new Error('Nu sunteți autentificat corect');
            }

            if (changedMaintenanceSlots.length === 0) {
                this.setState({
                    successMessage: 'Nu există modificări de mentenanță de salvat',
                    showMessage: true
                });

                setTimeout(() => {
                    this.setState({ showMessage: false });
                }, 3000);

                return;
            }

            this.setState({ savingMaintenance: true });

            // Procesăm sloturile care trebuie adăugate
            const addPromises = changedMaintenanceSlots
                .filter(slot => slot.action === 'add')
                .map(slot => {
                    // Convertim data din string în obiect Date pentru a o trimite către backend
                    const dateParts = slot.date.split('-');
                    const dateObj = new Date(dateParts[0], dateParts[1] - 1, dateParts[2]);

                    // Creăm obiectul de rezervare pentru mentenanță
                    const maintenanceReservation = {
                        hall: { id: selectedHallId },
                        user: null, // Mentenanța nu are un utilizator asociat
                        date: dateObj,
                        timeSlot: slot.timeSlot,
                        price: null, // Mentenanța nu are preț
                        type: 'maintenance'
                    };

                    // Facem apelul POST către API
                    return fetch('http://localhost:8080/reservations', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${token}`
                        },
                        body: JSON.stringify(maintenanceReservation)
                    });
                });

            // Procesăm sloturile care trebuie șterse
            const deletePromises = changedMaintenanceSlots
                .filter(slot => slot.action === 'delete' && slot.id)
                .map(slot => {
                    // Facem apelul DELETE către API pentru a șterge rezervarea de mentenanță
                    return fetch(`http://localhost:8080/reservations/${slot.id}`, {
                        method: 'DELETE',
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    });
                });

            // Așteptăm ca toate operațiunile să fie finalizate
            await Promise.all([...addPromises, ...deletePromises]);

            // Reîncărcăm programul pentru a vedea modificările aplicate
            await this.fetchSchedule(selectedHallId, this.state.currentWeekStart);

            this.setState({
                successMessage: 'Intervalele de mentenanță au fost salvate cu succes!',
                showMessage: true,
                savingMaintenance: false
            });

            // Ascunde mesajul după 3 secunde
            setTimeout(() => {
                this.setState({ showMessage: false });
            }, 3000);

        } catch (error) {
            console.error('Error saving maintenance slots:', error);
            this.setState({
                errorMessage: `A apărut o eroare la salvarea intervalelor de mentenanță: ${error.message}`,
                showMessage: true,
                savingMaintenance: false
            });
        }
    }

    // Funcție pentru a obține prima zi a săptămânii (luni) pentru o dată dată
    getStartOfWeek = (date) => {
        const dayOfWeek = date.getDay(); // 0 pentru duminică, 1 pentru luni, etc.
        const diff = date.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1); // Ajustăm pentru a începe cu luni
        const startOfWeek = new Date(date);
        startOfWeek.setDate(diff);
        startOfWeek.setHours(0, 0, 0, 0);
        return startOfWeek;
    }

    // Formatare dată pentru cheie
    formatDate = (date) => {
        return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
    }

    // Verifică dacă o dată este azi
    isToday = (date) => {
        const today = new Date();
        return date.getDate() === today.getDate() &&
            date.getMonth() === today.getMonth() &&
            date.getFullYear() === today.getFullYear();
    }

    renderWeekDays = () => {
        const { currentWeekStart } = this.state;
        const weekDays = [];

        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(currentWeekStart);
            currentDate.setDate(currentDate.getDate() + i);
            const dateKey = this.formatDate(currentDate);
            const isToday = this.isToday(currentDate);

            weekDays.push(
                <th key={i} className={`schedule-day-header ${isToday ? 'today' : ''}`}>
                    <div className="day-name">{currentDate.toLocaleDateString('ro-RO', { weekday: 'long' })}</div>
                    <div className="day-date">{currentDate.toLocaleDateString('ro-RO', { day: 'numeric', month: 'long' })}</div>
                </th>
            );
        }

        return weekDays;
    }

    renderTimeSlots = () => {
        const { currentWeekStart, timeSlots, schedule } = this.state;

        return timeSlots.map(slot => (
            <tr key={slot.id} className="time-slot-row">
                <td className="time-slot-cell">{slot.display}</td>
                {[...Array(7)].map((_, i) => {
                    const currentDate = new Date(currentWeekStart);
                    currentDate.setDate(currentDate.getDate() + i);
                    const dateKey = this.formatDate(currentDate);

                    // Asigură-te că această valoare este setată corect (maintenance/available/booked)
                    const slotStatus = schedule[dateKey] && schedule[dateKey][slot.id]
                        ? schedule[dateKey][slot.id]
                        : 'available';

                    const isToday = this.isToday(currentDate);

                    return (
                        <td
                            key={`${dateKey}_${slot.id}`}
                            className={`schedule-slot ${slotStatus} ${isToday ? 'today' : ''}`}
                            onClick={() => this.toggleMaintenanceSlot(dateKey, slot.id)}
                        >
                            {slotStatus === 'maintenance' && (
                                <span className="slot-status maintenance">Mentenanță</span>
                            )}
                            {slotStatus === 'booked' && (
                                <span className="slot-status booked">Rezervat</span>
                            )}
                        </td>
                    );
                })}
            </tr>
        ));
    }

    render() {
        const {
            halls,
            selectedHallId,
            currentWeekStart,
            loading,
            error,
            maintenanceCount,
            changedMaintenanceSlots,
            successMessage,
            errorMessage,
            showMessage,
            savingMaintenance
        } = this.state;

        // Verificăm dacă suntem în săptămâna curentă
        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);
        const isCurrentWeekSelected = currentWeekStart.getTime() === startOfCurrentWeek.getTime();

        // Calculăm săptămâna maximă (peste 8 săptămâni față de cea curentă)
        const maxWeekStart = new Date(startOfCurrentWeek);
        maxWeekStart.setDate(maxWeekStart.getDate() + 7 * 8);
        const isMaxWeekSelected = currentWeekStart.getTime() >= maxWeekStart.getTime();

        if (loading && halls.length === 0) {
            return <div className="loading-container">Se încarcă sălile...</div>;
        }

        if (error && halls.length === 0) {
            return <div className="error-container">Eroare: {error}</div>;
        }

        // Găsim sala selectată
        const selectedHall = halls.find(hall => hall.id === parseInt(selectedHallId));
        const hasMaintenanceChanges = changedMaintenanceSlots.length > 0;

        return (
            <div className="manage-schedule-page">
                <h1 className="page-title">Gestionare Program</h1>

                {halls.length === 0 ? (
                    <div className="empty-state">
                        <p>Nu aveți săli de sport adăugate. Adăugați prima sală din secțiunea "Adaugă Sală Nouă".</p>
                    </div>
                ) : (
                    <>
                        {showMessage && (successMessage || errorMessage) && (
                            <div className={`notification-message ${successMessage ? 'success' : 'error'}`}>
                                {successMessage ? successMessage : errorMessage}
                            </div>
                        )}

                        <div className="schedule-controls">
                            <div className="hall-selector">
                                <label htmlFor="hall-select">Selectați sala:</label>
                                <select
                                    id="hall-select"
                                    value={selectedHallId || ''}
                                    onChange={this.handleHallChange}
                                    className="hall-select-input"
                                >
                                    {halls.map(hall => (
                                        <option key={hall.id} value={hall.id}>
                                            {hall.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {selectedHall && (
                                <div className="selected-hall-info">
                                    <span className="selected-hall-name">{selectedHall.name}</span>
                                    <span className="selected-hall-location">{selectedHall.city}, {selectedHall.county}</span>
                                </div>
                            )}
                        </div>

                        <div className="week-navigation">
                            <button
                                className={`week-nav-button prev ${isCurrentWeekSelected ? 'disabled' : ''}`}
                                onClick={this.handlePreviousWeek}
                                disabled={isCurrentWeekSelected}
                            >
                                <ChevronLeft size={20} />
                                Săptămâna anterioară
                            </button>

                            <div className="current-week">
                                <Calendar size={18} />
                                <span>
                                    {currentWeekStart.toLocaleDateString('ro-RO', { day: 'numeric', month: 'long' })} - {
                                    new Date(currentWeekStart.getTime() + 6 * 24 * 60 * 60 * 1000).toLocaleDateString('ro-RO', { day: 'numeric', month: 'long', year: 'numeric' })
                                }
                                </span>
                            </div>

                            <button
                                className={`week-nav-button next ${isMaxWeekSelected ? 'disabled' : ''}`}
                                onClick={this.handleNextWeek}
                                disabled={isMaxWeekSelected}
                            >
                                Săptămâna următoare
                                <ChevronRight size={20} />
                            </button>
                        </div>

                        <div className="schedule-legend">
                            <div className="legend-item">
                                <div className="legend-color available"></div>
                                <span>Disponibil</span>
                            </div>
                            <div className="legend-item">
                                <div className="legend-color booked"></div>
                                <span>Rezervat</span>
                            </div>
                            <div className="legend-item">
                                <div className="legend-color maintenance"></div>
                                <span>Mentenanță (<span className="maintenance-count">{maintenanceCount}</span>/3)</span>
                            </div>
                            <div className="legend-info">
                                <Info size={16} />
                                <span>Apăsați pe un interval pentru a marca/demarca mentenanță</span>
                            </div>
                        </div>

                        <div className="schedule-table-container">
                            {loading ? (
                                <div className="loading-overlay">
                                    <div className="loading-spinner"></div>
                                    <span>Se încarcă programul...</span>
                                </div>
                            ) : (
                                <table className="schedule-table">
                                    <thead>
                                    <tr>
                                        <th className="time-header">Interval Orar</th>
                                        {this.renderWeekDays()}
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {this.renderTimeSlots()}
                                    </tbody>
                                </table>
                            )}
                        </div>

                        <div className="schedule-actions">
                            <div className="maintenance-info">
                                {maintenanceCount >= 3 && (
                                    <div className="maintenance-limit-warning">
                                        <AlertCircle size={16} />
                                        <span>Ați atins limita maximă de 3 intervale de mentenanță pe săptămână</span>
                                    </div>
                                )}
                                {hasMaintenanceChanges && (
                                    <div className="maintenance-changes-pending">
                                        <Info size={16} />
                                        <span>Aveți modificări nesalvate la intervalele de mentenanță</span>
                                    </div>
                                )}
                            </div>
                            <div className="action-buttons">
                                <button
                                    className="maintenance-button"
                                    onClick={this.saveMaintenance}
                                    disabled={savingMaintenance || !hasMaintenanceChanges}
                                >
                                    {savingMaintenance ? (
                                        <>
                                            <div className="button-spinner"></div>
                                            Se salvează...
                                        </>
                                    ) : (
                                        <>
                                            <Wrench size={16} />
                                            Salvează mentenanță
                                        </>
                                    )}
                                </button>
                                <button className="save-button" onClick={this.saveSchedule}>
                                    Salvează programul
                                </button>
                            </div>
                        </div>
                    </>
                )}
            </div>
        );
    }
}

export default ManageSchedulePage;