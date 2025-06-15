import React, { Component } from 'react';
import { Info, AlertCircle, Wrench, Filter, Eye, EyeOff, ToggleLeft, ToggleRight, CheckCircle, XCircle, Loader } from 'lucide-react';
import ScheduleTableComponent from '../../halladmin/AdminPages/ScheduleTableComponent';
import BookingPrioritization from './BookingPrioritization';
import './ViewHallsSchedulePage.css';

class ViewHallsSchedulePage extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';

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
            reservations: [],
            // FCFS control states
            fcfsEnabled: true,
            fcfsLoading: false,
            fcfsInitialLoading: true,
            fcfsMessage: '',
            fcfsError: '',
            fcfsToggling: false
        };
    }

    async componentDidMount() {
        await Promise.all([
            this.fetchAllHalls(),
            this.fetchFcfsStatus()
        ]);
    }

    fetchFcfsStatus = async () => {
        try {
            this.setState({ fcfsInitialLoading: true });
            const token = localStorage.getItem('jwtToken');

            if (!token) {
                this.setState({ fcfsError: 'Nu sunteți autentificat' });
                return;
            }

            const response = await fetch(`${this.API_BASE_URL}/admin/fcfs-status`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const data = await response.json();
                this.setState({
                    fcfsEnabled: data.fcfsEnabled,
                    fcfsMessage: data.message,
                    fcfsError: ''
                });
            } else {
                throw new Error(`Eroare la încărcarea statusului: ${response.status}`);
            }
        } catch (err) {
            console.error('Eroare la încărcarea statusului FCFS:', err);
            this.setState({ fcfsError: 'Eroare la încărcarea statusului FCFS' });
        } finally {
            this.setState({ fcfsInitialLoading: false });
        }
    };

    toggleFcfs = async () => {
        try {
            this.setState({ fcfsToggling: true, fcfsError: '' });

            const token = localStorage.getItem('jwtToken');

            if (!token) {
                this.setState({ fcfsError: 'Nu sunteți autentificat' });
                return;
            }

            const response = await fetch(`${this.API_BASE_URL}/admin/fcfs-toggle`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const data = await response.json();
                this.setState({
                    fcfsEnabled: data.fcfsEnabled,
                    fcfsMessage: data.message,
                    fcfsError: ''
                });

                setTimeout(() => {
                    this.setState({ fcfsMessage: '' });
                }, 4000);
            } else {
                const errorData = await response.json();
                throw new Error(errorData.error || `Eroare la actualizarea statusului: ${response.status}`);
            }
        } catch (err) {
            console.error('Eroare la schimbarea statusului FCFS:', err);
            this.setState({ fcfsError: err.message || 'Eroare la schimbarea statusului FCFS' });
        } finally {
            this.setState({ fcfsToggling: false });
        }
    };

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

            const response = await fetch(`${this.API_BASE_URL}/sportsHalls`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();

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

    fetchHallSchedule = async (hallId, token) => {
        try {
            const response = await fetch(`${this.API_BASE_URL}/schedules/hall/${hallId}`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea programului sălii: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching hall schedule:', error);
            return [];
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

    // Modifică pentru a include programul sălii
    createScheduleWithHallAvailability = (weekStart, hallSchedule) => {
        const schedule = {};

        // Generăm sloturile orare în formatul "HH:MM - HH:MM"
        const timeSlots = [
            { id: "07:00 - 08:30", startTime: "07:00", endTime: "08:30" },
            { id: "08:30 - 10:00", startTime: "08:30", endTime: "10:00" },
            { id: "10:00 - 11:30", startTime: "10:00", endTime: "11:30" },
            { id: "11:30 - 13:00", startTime: "11:30", endTime: "13:00" },
            { id: "13:00 - 14:30", startTime: "13:00", endTime: "14:30" },
            { id: "14:30 - 16:00", startTime: "14:30", endTime: "16:00" },
            { id: "16:00 - 17:30", startTime: "16:00", endTime: "17:30" },
            { id: "17:30 - 19:00", startTime: "17:30", endTime: "19:00" },
            { id: "19:00 - 20:30", startTime: "19:00", endTime: "20:30" },
            { id: "20:30 - 22:00", startTime: "20:30", endTime: "22:00" },
            { id: "22:00 - 23:30", startTime: "22:00", endTime: "23:30" }
        ];

        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(weekStart);
            currentDate.setDate(currentDate.getDate() + i);
            const dateKey = this.formatDate(currentDate);
            const dayOfWeek = currentDate.getDay(); // 0 = Sunday, 1 = Monday, etc.

            // Convertim la formatul folosit in backend (1 = Monday, 7 = Sunday)
            const backendDayOfWeek = dayOfWeek === 0 ? 7 : dayOfWeek;

            schedule[dateKey] = {};

            const daySchedule = hallSchedule.find(s => s.dayOfWeek === backendDayOfWeek);

            timeSlots.forEach(slot => {
                if (!daySchedule || !daySchedule.isActive) {
                    schedule[dateKey][slot.id] = 'unavailable';
                } else {
                    if (this.isSlotInWorkingHours(slot, daySchedule.startTime, daySchedule.endTime)) {
                        schedule[dateKey][slot.id] = 'available';
                    } else {
                        schedule[dateKey][slot.id] = 'unavailable';
                    }
                }
            });
        }

        return schedule;
    }

    isSlotInWorkingHours = (slot, workingStartTime, workingEndTime) => {
        const slotStart = this.timeToMinutes(slot.startTime);
        const slotEnd = this.timeToMinutes(slot.endTime);
        const workingStart = this.timeToMinutes(workingStartTime);
        const workingEnd = this.timeToMinutes(workingEndTime);

        return slotStart >= workingStart && slotEnd <= workingEnd;
    }

    timeToMinutes = (timeString) => {
        const [hours, minutes] = timeString.split(':').map(Number);
        return hours * 60 + minutes;
    }

    fetchSchedule = async (hallId, weekStart) => {
        try {
            this.setState({ tableLoading: true });

            const token = localStorage.getItem('jwtToken');
            if (!token) throw new Error('Nu sunteți autentificat corect');

            const startDateStr = this.formatDate(weekStart);
            const endDate = new Date(weekStart);
            endDate.setDate(endDate.getDate() + 6);
            const endDateStr = this.formatDate(endDate);

            const [hallSchedule, maintenanceReservations, bookingReservations] = await Promise.all([
                this.fetchHallSchedule(hallId, token),
                this.fetchMaintenanceReservations(hallId, startDateStr, token),
                this.fetchBookingReservations(hallId, weekStart, token)
            ]);

            const scheduleWithAvailability = this.createScheduleWithHallAvailability(weekStart, hallSchedule);
            const originalMaintenanceSlots = [];

            maintenanceReservations.forEach(reservation => {
                const dateKey = this.formatDate(new Date(reservation.date));
                const slotId = reservation.timeSlot;

                if (scheduleWithAvailability[dateKey] &&
                    scheduleWithAvailability[dateKey][slotId] === 'available') {
                    scheduleWithAvailability[dateKey][slotId] = 'maintenance';
                    originalMaintenanceSlots.push({
                        id: reservation.id,
                        date: dateKey,
                        timeSlot: slotId
                    });
                }
            });

            bookingReservations.forEach(reservation => {
                const dateKey = this.formatDate(new Date(reservation.date));
                const slotId = reservation.timeSlot;

                if (scheduleWithAvailability[dateKey] &&
                    scheduleWithAvailability[dateKey][slotId] === 'available') {
                    scheduleWithAvailability[dateKey][slotId] = 'booked';
                }
            });

            let maintenanceCount = 0;
            Object.values(scheduleWithAvailability).forEach(daySchedule => {
                Object.values(daySchedule).forEach(status => {
                    if (status === 'maintenance') maintenanceCount++;
                });
            });

            const enhancedReservations = await this.enhanceReservationsWithDetails(bookingReservations, token);

            this.setState({
                schedule: scheduleWithAvailability,
                maintenanceCount,
                originalMaintenanceSlots,
                changedMaintenanceSlots: [],
                tableLoading: false,
                reservations: enhancedReservations
            });
        } catch (error) {
            console.error('Error fetching schedule:', error);
            this.setState({
                error: 'Eroare la încărcarea programului',
                tableLoading: false
            });
        }
    }

    enhanceReservationsWithDetails = async (reservations, token) => {
        return reservations.map(reservation => {
            if (reservation.user) {
                reservation.user.teamName = reservation.user.teamName || reservation.user.name || 'Echipă nespecificată';
                reservation.profileName = 'Profil ' + (reservation.user.teamType || 'Standard');
            }
            return reservation;
        });
    }

    fetchMaintenanceReservations = async (hallId, startDateStr, token) => {
        const maintenanceResponse = await fetch(
            `${this.API_BASE_URL}/reservations/maintenance/week?hallId=${hallId}&weekStart=${startDateStr}`,
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
        const weekDates = [];
        for (let i = 0; i < 7; i++) {
            const currentDate = new Date(weekStart);
            currentDate.setDate(currentDate.getDate() + i);
            weekDates.push(this.formatDate(currentDate));
        }

        const bookingPromises = weekDates.map(dateStr =>
            fetch(`${this.API_BASE_URL}/reservations?hallId=${hallId}&date=${dateStr}&type=reservation`, {
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
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const slotDate = new Date(date);
        slotDate.setHours(0, 0, 0, 0);

        if (slotDate <= today) {
            this.setState({
                errorMessage: 'Nu puteți marca mentenanță pentru ziua curentă sau pentru zile din trecut',
                showMessage: true
            });
            return;
        }

        this.setState(prevState => {
            if (!prevState.schedule[date] || !prevState.schedule[date][slotId]) {
                return {
                    errorMessage: 'Eroare: intervalul orar nu a fost găsit în program',
                    showMessage: true
                };
            }

            const currentStatus = prevState.schedule[date][slotId];

            if (currentStatus === 'unavailable') {
                return {
                    errorMessage: 'Nu puteți marca pentru mentenanță un interval în afara orelor de funcționare ale sălii',
                    showMessage: true
                };
            }

            // Verificăm dacă slotul este deja rezervat
            if (currentStatus === 'booked') {
                return {
                    errorMessage: 'Nu puteți marca pentru mentenanță un interval deja rezervat',
                    showMessage: true
                };
            }

            const updatedSchedule = JSON.parse(JSON.stringify(prevState.schedule));
            let updatedMaintenanceCount = prevState.maintenanceCount;
            let updatedChangedSlots = [...prevState.changedMaintenanceSlots];

            if (currentStatus === 'maintenance') {
                return this.handleRemoveMaintenance(
                    updatedSchedule, updatedMaintenanceCount, updatedChangedSlots,
                    date, slotId, prevState.originalMaintenanceSlots
                );
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

            await Promise.all([
                ...this.processAddMaintenanceSlots(changedMaintenanceSlots, selectedHallId, token),
                ...this.processDeleteMaintenanceSlots(changedMaintenanceSlots, token)
            ]);

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

                return fetch(`${this.API_BASE_URL}/reservations`, {
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
                return fetch(`${this.API_BASE_URL}/reservations/${slot.id}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
            });
    }

    handleGenerationComplete = (success, errorMessage) => {
        if (success) {
            this.showTemporaryMessage('Rezervările au fost generate cu succes.', true);
            this.fetchSchedule(this.state.selectedHallId, this.state.currentWeekStart);
        } else {
            this.showTemporaryMessage(`Eroare la generarea rezervărilor: ${errorMessage || 'Eroare necunoscută'}`, false);
        }
    };

    renderFcfsControl = () => {
        const {
            fcfsEnabled,
            fcfsInitialLoading,
            fcfsMessage,
            fcfsError,
            fcfsToggling
        } = this.state;

        if (fcfsInitialLoading) {
            return (
                <div className="vhsp-fcfs-control loading">
                    <div className="vhsp-fcfs-loading">
                        <Loader className="vhsp-spinner" size={20} />
                        <span>Se încarcă...</span>
                    </div>
                </div>
            );
        }

        return (
            <div className="vhsp-fcfs-control">
                <div className="vhsp-fcfs-header">
                    <div className="vhsp-fcfs-icon-wrapper">
                        {fcfsEnabled ? (
                            <Eye className="vhsp-fcfs-icon active" size={20} />
                        ) : (
                            <EyeOff className="vhsp-fcfs-icon inactive" size={20} />
                        )}
                    </div>
                    <div className="vhsp-fcfs-info">
                        <h4>Control FCFS</h4>
                        <span className="vhsp-fcfs-subtitle">Rezervarea locurilor rămase</span>
                    </div>
                </div>

                <div className="vhsp-fcfs-status">
                    <div className={`vhsp-status-indicator ${fcfsEnabled ? 'active' : 'inactive'}`}>
                        {fcfsEnabled ? (
                            <>
                                <CheckCircle size={16} />
                                <span>Activat</span>
                            </>
                        ) : (
                            <>
                                <XCircle size={16} />
                                <span>Dezactivat</span>
                            </>
                        )}
                    </div>

                    <button
                        onClick={this.toggleFcfs}
                        disabled={fcfsToggling}
                        className={`vhsp-fcfs-toggle-button ${fcfsEnabled ? 'active' : 'inactive'}`}
                    >
                        {fcfsToggling ? (
                            <>
                                <Loader className="vhsp-spinner-small" size={14} />
                                <span>Se actualizează...</span>
                            </>
                        ) : (
                            <>
                                {fcfsEnabled ? (
                                    <ToggleRight size={14} />
                                ) : (
                                    <ToggleLeft size={14} />
                                )}
                                <span>{fcfsEnabled ? 'Dezactivează' : 'Activează'}</span>
                            </>
                        )}
                    </button>
                </div>

                {fcfsMessage && (
                    <div className="vhsp-fcfs-message success">
                        <CheckCircle size={14} />
                        <span>{fcfsMessage}</span>
                    </div>
                )}

                {fcfsError && (
                    <div className="vhsp-fcfs-message error">
                        <XCircle size={14} />
                        <span>{fcfsError}</span>
                    </div>
                )}
            </div>
        );
    }

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

                            <div className="vhsp-fcfs-section">
                                {this.renderFcfsControl()}
                            </div>

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

                            <ScheduleTableComponent
                                schedule={schedule}
                                tableLoading={tableLoading}
                                maintenanceCount={maintenanceCount}
                                maxMaintenanceSlots={null}
                                onToggleMaintenance={this.toggleMaintenanceSlot}
                                onWeekChange={this.handleWeekChange}
                                isPastDateBlocked={true}
                                initialWeekStart={currentWeekStart}
                                restrictPastNavigation={true}
                                restrictFutureNavigation={true}
                                maxFutureWeeks={8}
                                isReadOnly={false}
                                showWeekNavigation={true}
                                reservations={reservations}
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