import React, { Component } from 'react';
import { ChevronLeft, ChevronRight, Calendar, Info, AlertCircle, Wrench } from 'lucide-react';
import './ManageSchedulePage.css';

class ManageSchedulePage extends Component {
    constructor(props) {
        super(props);

        // Generarea intervalelor orare
        const timeSlots = [];
        for (let hour = 7; hour <= 22; hour += 1.5) {
            const startHour = Math.floor(hour);
            const startMinute = (hour - startHour) * 60;
            const endHour = Math.floor(hour + 1.5);
            const endMinute = ((hour + 1.5) - endHour) * 60;

            const formattedStartHour = startHour.toString().padStart(2, '0');
            const formattedStartMinute = startMinute.toString().padStart(2, '0');
            const formattedEndHour = endHour.toString().padStart(2, '0');
            const formattedEndMinute = endMinute.toString().padStart(2, '0');

            timeSlots.push({
                id: `${startHour}_${startMinute}`,
                display: `${formattedStartHour}:${formattedStartMinute} - ${formattedEndHour}:${formattedEndMinute}`
            });
        }

        this.state = {
            halls: [],
            selectedHallId: null,
            currentWeekStart: this.getStartOfWeek(new Date()),
            timeSlots,
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
            savingMaintenance: false
        };
    }

    async componentDidMount() {
        await this.fetchHalls();
    }

    // Funcții de utilitate
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

    isToday = (date) => {
        const today = new Date();
        return date.getDate() === today.getDate() &&
            date.getMonth() === today.getMonth() &&
            date.getFullYear() === today.getFullYear();
    }

    // API calls
    fetchHalls = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                this.setState({
                    error: 'Nu sunteți autentificat corect',
                    initialLoading: false,
                    halls: []
                });
                return;
            }

            const response = await fetch(`http://localhost:8080/sportsHalls/admin/${userId}`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();
            const selectedHallId = halls.length > 0 ? halls[0].id : null;

            this.setState({ halls, selectedHallId, initialLoading: false }, () => {
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

    createEmptySchedule = (weekStart) => {
        const schedule = {};

        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(weekStart);
            currentDate.setDate(currentDate.getDate() + i);
            const dateKey = this.formatDate(currentDate);

            schedule[dateKey] = {};
            this.state.timeSlots.forEach(slot => {
                schedule[dateKey][slot.id] = 'available';
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

            this.setState({
                schedule: updatedSchedule,
                maintenanceCount,
                originalMaintenanceSlots,
                changedMaintenanceSlots: [],
                tableLoading: false
            });
        } catch (error) {
            console.error('Error fetching schedule:', error);
            this.setState({
                error: 'Eroare la încărcarea programului',
                tableLoading: false
            });
        }
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

    handlePreviousWeek = () => {
        const { currentWeekStart } = this.state;
        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);

        if (currentWeekStart.getTime() <= startOfCurrentWeek.getTime()) {
            return; // Nu permitem navigarea în trecut
        }

        const previousWeekStart = new Date(currentWeekStart);
        previousWeekStart.setDate(previousWeekStart.getDate() - 7);

        this.setState({
            currentWeekStart: previousWeekStart,
            tableLoading: true,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        }, () => {
            this.fetchSchedule(this.state.selectedHallId, previousWeekStart);
        });
    }

    handleNextWeek = () => {
        const { currentWeekStart } = this.state;
        const maxWeeks = 8;
        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);

        const diffTime = currentWeekStart.getTime() - startOfCurrentWeek.getTime();
        const diffWeeks = Math.ceil(diffTime / (1000 * 60 * 60 * 24 * 7));

        if (diffWeeks >= maxWeeks) {
            return; // Nu permitem navigarea prea departe în viitor
        }

        const nextWeekStart = new Date(currentWeekStart);
        nextWeekStart.setDate(nextWeekStart.getDate() + 7);

        this.setState({
            currentWeekStart: nextWeekStart,
            tableLoading: true,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        }, () => {
            this.fetchSchedule(this.state.selectedHallId, nextWeekStart);
        });
    }

    toggleMaintenanceSlot = (date, slotId) => {
        // Verificăm dacă data este în trecut sau astăzi
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const slotDate = new Date(date);
        slotDate.setHours(0, 0, 0, 0);

        // Blocăm atât ziua curentă cât și zilele din trecut
        if (slotDate <= today) {
            this.setState({
                errorMessage: 'Nu puteți marca mentenanță pentru ziua curentă sau pentru zile din trecut',
                showMessage: true
            });
            return;
        }

        this.setState(prevState => {
            const updatedSchedule = JSON.parse(JSON.stringify(prevState.schedule));
            const currentStatus = updatedSchedule[date][slotId];
            let updatedMaintenanceCount = prevState.maintenanceCount;
            let updatedChangedSlots = [...prevState.changedMaintenanceSlots];

            // Verificăm dacă slotul este deja în mentenanță
            if (currentStatus === 'maintenance') {
                return this.handleRemoveMaintenance(
                    updatedSchedule, updatedMaintenanceCount, updatedChangedSlots,
                    date, slotId, prevState.originalMaintenanceSlots
                );
            }

            // Verificăm dacă slotul este rezervat
            if (currentStatus === 'booked') {
                return {
                    errorMessage: 'Nu puteți marca pentru mentenanță un interval deja rezervat',
                    showMessage: true
                };
            }

            // Verificăm limita de 3 mentenanțe pe săptămână
            if (updatedMaintenanceCount >= 3) {
                return {
                    errorMessage: 'Nu puteți avea mai mult de 3 intervale de mentenanță pe săptămână',
                    showMessage: true
                };
            }

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

    saveSchedule = async () => {
        try {
            this.setState({
                successMessage: 'Programul a fost salvat cu succes!',
                showMessage: true
            });

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
                this.showTemporaryMessage('Nu există modificări de mentenanță de salvat', true);
                return;
            }

            this.setState({ savingMaintenance: true });

            // Procesăm sloturile care trebuie adăugate și șterse
            await Promise.all([
                ...this.processAddMaintenanceSlots(changedMaintenanceSlots, selectedHallId, token),
                ...this.processDeleteMaintenanceSlots(changedMaintenanceSlots, token)
            ]);

            // Reîncărcăm programul
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

    // Render methods
    renderWeekDays = () => {
        const { currentWeekStart } = this.state;
        const weekDays = [];

        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(currentWeekStart);
            currentDate.setDate(currentDate.getDate() + i);
            const isToday = this.isToday(currentDate);

            weekDays.push(
                <th key={i} className={`msp-schedule-day-header ${isToday ? 'msp-today' : ''}`}>
                    <div className="msp-day-name">{currentDate.toLocaleDateString('ro-RO', { weekday: 'long' })}</div>
                    <div className="msp-day-date">{currentDate.toLocaleDateString('ro-RO', { day: 'numeric', month: 'long' })}</div>
                </th>
            );
        }

        return weekDays;
    }

    renderTimeSlots = () => {
        const { currentWeekStart, timeSlots, schedule } = this.state;
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        return timeSlots.map(slot => (
            <tr key={slot.id} className="msp-time-slot-row">
                <td className="msp-time-slot-cell">{slot.display}</td>
                {[...Array(7)].map((_, i) => {
                    const currentDate = new Date(currentWeekStart);
                    currentDate.setDate(currentDate.getDate() + i);
                    const dateKey = this.formatDate(currentDate);
                    // Marchează atât ziua curentă cât și zilele din trecut ca indisponibile pentru mentenanță
                    const isPastDate = currentDate <= today;
                    const slotStatus = schedule[dateKey] && schedule[dateKey][slot.id]
                        ? schedule[dateKey][slot.id]
                        : 'available';
                    const isToday = this.isToday(currentDate);

                    return (
                        <td
                            key={`${dateKey}_${slot.id}`}
                            className={`msp-schedule-slot msp-${slotStatus} ${isToday ? 'msp-today' : ''} ${isPastDate ? 'msp-past-date' : ''}`}
                            onClick={() => !isPastDate && this.toggleMaintenanceSlot(dateKey, slot.id)}
                        >
                            {slotStatus === 'maintenance' && (
                                <span className="msp-slot-status msp-maintenance">Mentenanță</span>
                            )}
                            {slotStatus === 'booked' && (
                                <span className="msp-slot-status msp-booked">Rezervat</span>
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
            initialLoading,
            tableLoading,
            error,
            maintenanceCount,
            changedMaintenanceSlots,
            successMessage,
            errorMessage,
            showMessage,
            savingMaintenance
        } = this.state;

        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);
        const isCurrentWeekSelected = currentWeekStart.getTime() === startOfCurrentWeek.getTime();

        const maxWeekStart = new Date(startOfCurrentWeek);
        maxWeekStart.setDate(maxWeekStart.getDate() + 7 * 8);
        const isMaxWeekSelected = currentWeekStart.getTime() >= maxWeekStart.getTime();

        if (initialLoading) {
            return <div className="msp-loading-container">Se încarcă sălile...</div>;
        }

        if (error && halls.length === 0) {
            return <div className="msp-error-container">Eroare: {error}</div>;
        }

        const selectedHall = halls.find(hall => hall.id === parseInt(selectedHallId));
        const hasMaintenanceChanges = changedMaintenanceSlots.length > 0;

        return (
            <div className="msp-manage-schedule-page">
                <h1 className="msp-page-title">Gestionare Program</h1>

                {halls.length === 0 ? (
                    <div className="msp-empty-state">
                        <p>Nu aveți săli de sport adăugate. Adăugați prima sală din secțiunea "Adaugă Sală Nouă".</p>
                    </div>
                ) : (
                    <>
                        {showMessage && (successMessage || errorMessage) && (
                            <div className={`msp-notification-message ${successMessage ? 'msp-success' : 'msp-error'}`}>
                                {successMessage ? successMessage : errorMessage}
                            </div>
                        )}

                        <div className="msp-schedule-controls">
                            <div className="msp-hall-selector">
                                <label htmlFor="hall-select">Selectați sala:</label>
                                <select
                                    id="hall-select"
                                    value={selectedHallId || ''}
                                    onChange={this.handleHallChange}
                                    className="msp-hall-select-input"
                                >
                                    {halls.map(hall => (
                                        <option key={hall.id} value={hall.id}>
                                            {hall.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {selectedHall && (
                                <div className="msp-selected-hall-info">
                                    <span className="msp-selected-hall-name">{selectedHall.name}</span>
                                    <span className="msp-selected-hall-location">{selectedHall.city}, {selectedHall.county}</span>
                                </div>
                            )}
                        </div>

                        <div className="msp-week-navigation">
                            <button
                                className={`msp-week-nav-button msp-prev ${isCurrentWeekSelected ? 'msp-disabled' : ''}`}
                                onClick={this.handlePreviousWeek}
                                disabled={isCurrentWeekSelected}
                            >
                                <ChevronLeft size={20} />
                                Săptămâna anterioară
                            </button>

                            <div className="msp-current-week">
                                <Calendar size={18} />
                                <span>
                                    {currentWeekStart.toLocaleDateString('ro-RO', { day: 'numeric', month: 'long' })} - {
                                    new Date(currentWeekStart.getTime() + 6 * 24 * 60 * 60 * 1000).toLocaleDateString('ro-RO', { day: 'numeric', month: 'long', year: 'numeric' })
                                }
                                </span>
                            </div>

                            <button
                                className={`msp-week-nav-button msp-next ${isMaxWeekSelected ? 'msp-disabled' : ''}`}
                                onClick={this.handleNextWeek}
                                disabled={isMaxWeekSelected}
                            >
                                Săptămâna următoare
                                <ChevronRight size={20} />
                            </button>
                        </div>

                        <div className="msp-schedule-legend">
                            <div className="msp-legend-item">
                                <div className="msp-legend-color msp-available"></div>
                                <span>Disponibil</span>
                            </div>
                            <div className="msp-legend-item">
                                <div className="msp-legend-color msp-booked"></div>
                                <span>Rezervat</span>
                            </div>
                            <div className="msp-legend-item">
                                <div className="msp-legend-color msp-maintenance"></div>
                                <span>Mentenanță (<span className="msp-maintenance-count">{maintenanceCount}</span>/3)</span>
                            </div>
                            <div className="msp-legend-info">
                                <Info size={16} />
                                <span>Apăsați pe un interval pentru a marca/demarca mentenanță</span>
                            </div>
                        </div>

                        <div className="msp-schedule-table-container">
                            {tableLoading && (
                                <div className="msp-loading-overlay">
                                    <div className="msp-loading-spinner"></div>
                                    <span>Se încarcă programul...</span>
                                </div>
                            )}
                            <table className="msp-schedule-table">
                                <thead>
                                <tr>
                                    <th className="msp-time-header">Interval Orar</th>
                                    {this.renderWeekDays()}
                                </tr>
                                </thead>
                                <tbody>
                                {this.renderTimeSlots()}
                                </tbody>
                            </table>
                        </div>

                        <div className="msp-schedule-actions">
                            <div className="msp-maintenance-info">
                                {maintenanceCount >= 3 && (
                                    <div className="msp-maintenance-limit-warning">
                                        <AlertCircle size={16} />
                                        <span>Ați atins limita maximă de 3 intervale de mentenanță pe săptămână</span>
                                    </div>
                                )}
                                {hasMaintenanceChanges && (
                                    <div className="msp-maintenance-changes-pending">
                                        <Info size={16} />
                                        <span>Aveți modificări nesalvate la intervalele de mentenanță</span>
                                    </div>
                                )}
                            </div>
                            <div className="msp-action-buttons">
                                <button
                                    className="msp-maintenance-button"
                                    onClick={this.saveMaintenance}
                                    disabled={savingMaintenance || !hasMaintenanceChanges}
                                >
                                    {savingMaintenance ? (
                                        <>
                                            <div className="msp-button-spinner"></div>
                                            Se salvează...
                                        </>
                                    ) : (
                                        <>
                                            <Wrench size={16} />
                                            Salvează mentenanță
                                        </>
                                    )}
                                </button>
                                <button className="msp-save-button" onClick={this.saveSchedule}>
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