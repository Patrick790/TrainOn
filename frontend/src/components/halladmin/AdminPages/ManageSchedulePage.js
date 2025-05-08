import React, { Component } from 'react';
import { Info, AlertCircle, Wrench } from 'lucide-react';
import ScheduleTableComponent from './ScheduleTableComponent';
import './ManageSchedulePage.css';

class ManageSchedulePage extends Component {
    constructor(props) {
        super(props);

        this.state = {
            halls: [],
            selectedHallId: null,
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
            // Keep the 3 maintenance limit
            maxMaintenanceSlots: 3
        };
    }

    async componentDidMount() {
        await this.fetchHalls();
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

            this.setState({
                halls,
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

        // Get timeslots from the ScheduleTableComponent
        const timeSlots = [];
        for (let hour = 7; hour <= 22; hour += 1.5) {
            const startHour = Math.floor(hour);
            const startMinute = (hour - startHour) * 60;
            timeSlots.push({
                id: `${startHour}_${startMinute}`
            });
        }

        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(weekStart);
            currentDate.setDate(currentDate.getDate() + i);
            const dateKey = this.formatDate(currentDate);

            schedule[dateKey] = {};
            timeSlots.forEach(slot => {
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

            // Obtain all necessary data in parallel
            const [maintenanceReservations, bookingReservations] = await Promise.all([
                this.fetchMaintenanceReservations(hallId, startDateStr, token),
                this.fetchBookingReservations(hallId, weekStart, token)
            ]);

            // Update schedule with existing reservations
            const updatedSchedule = { ...emptySchedule };
            const originalMaintenanceSlots = [];

            // Mark maintenance slots
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

            // Mark booked slots
            bookingReservations.forEach(reservation => {
                const dateKey = this.formatDate(new Date(reservation.date));
                const slotId = reservation.timeSlot;

                if (updatedSchedule[dateKey] && updatedSchedule[dateKey][slotId]) {
                    updatedSchedule[dateKey][slotId] = 'booked';
                }
            });

            // Count maintenance intervals
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
        // Create an array with all days of the week
        const weekDates = [];
        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(weekStart);
            currentDate.setDate(currentDate.getDate() + i);
            weekDates.push(this.formatDate(currentDate));
        }

        // Get reservations for each day
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

    handleWeekChange = (newWeekStart) => {
        this.setState({
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

            // Check the limit of 3 maintenance slots per week
            if (updatedMaintenanceCount >= prevState.maxMaintenanceSlots) {
                return {
                    errorMessage: `Nu puteți avea mai mult de ${prevState.maxMaintenanceSlots} intervale de mentenanță pe săptămână`,
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

    render() {
        const {
            halls,
            selectedHallId,
            currentWeekStart,
            initialLoading,
            tableLoading,
            error,
            schedule,
            maintenanceCount,
            maxMaintenanceSlots,
            changedMaintenanceSlots,
            successMessage,
            errorMessage,
            showMessage,
            savingMaintenance
        } = this.state;

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

                        {/* Use the reusable ScheduleTableComponent */}
                        <ScheduleTableComponent
                            schedule={schedule}
                            tableLoading={tableLoading}
                            maintenanceCount={maintenanceCount}
                            maxMaintenanceSlots={maxMaintenanceSlots}
                            onToggleMaintenance={this.toggleMaintenanceSlot}
                            onWeekChange={this.handleWeekChange}
                            isPastDateBlocked={true}
                            initialWeekStart={currentWeekStart}
                            restrictPastNavigation={true}
                            restrictFutureNavigation={true}
                            maxFutureWeeks={8}
                            isReadOnly={false}
                            showWeekNavigation={true}
                        />

                        <div className="msp-schedule-actions">
                            <div className="msp-maintenance-info">
                                {maintenanceCount >= maxMaintenanceSlots && (
                                    <div className="msp-maintenance-limit-warning">
                                        <AlertCircle size={16} />
                                        <span>Ați atins limita maximă de {maxMaintenanceSlots} intervale de mentenanță pe săptămână</span>
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
                            </div>
                        </div>
                    </>
                )}
            </div>
        );
    }
}

export default ManageSchedulePage;