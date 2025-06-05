import React from 'react';
import { Info, AlertCircle, Wrench, Calendar, ChevronLeft, ChevronRight } from 'lucide-react';
import './ScheduleTableComponent.css';

class ScheduleTableComponent extends React.Component {
    constructor(props) {
        super(props);

        // Generate time slots according to the format in the table
        const timeSlots = [
            { id: "07:00 - 08:30", display: "07:00 - 08:30" },
            { id: "08:30 - 10:00", display: "08:30 - 10:00" },
            { id: "10:00 - 11:30", display: "10:00 - 11:30" },
            { id: "11:30 - 13:00", display: "11:30 - 13:00" },
            { id: "13:00 - 14:30", display: "13:00 - 14:30" },
            { id: "14:30 - 16:00", display: "14:30 - 16:00" },
            { id: "16:00 - 17:30", display: "16:00 - 17:30" },
            { id: "17:30 - 19:00", display: "17:30 - 19:00" },
            { id: "19:00 - 20:30", display: "19:00 - 20:30" },
            { id: "20:30 - 22:00", display: "20:30 - 22:00" },
            { id: "22:00 - 23:30", display: "22:00 - 23:30" }
        ];

        this.state = {
            timeSlots,
            currentWeekStart: this.getStartOfWeek(new Date()),
            tableLoading: false
        };
    }

    componentDidMount() {
        // If a different start week is provided, use it
        if (this.props.initialWeekStart) {
            this.setState({ currentWeekStart: this.props.initialWeekStart });
        }
    }

    // Utility functions
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

    handlePreviousWeek = () => {
        const { currentWeekStart } = this.state;
        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);

        // Check if navigation is restricted to current week onward
        if (this.props.restrictPastNavigation &&
            currentWeekStart.getTime() <= startOfCurrentWeek.getTime()) {
            return; // Prevent navigation to past
        }

        const previousWeekStart = new Date(currentWeekStart);
        previousWeekStart.setDate(previousWeekStart.getDate() - 7);

        this.setState({ currentWeekStart: previousWeekStart }, () => {
            if (this.props.onWeekChange) {
                this.props.onWeekChange(previousWeekStart);
            }
        });
    }

    handleNextWeek = () => {
        const { currentWeekStart } = this.state;
        const maxWeeks = this.props.maxFutureWeeks || 8;
        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);

        // Check if we've reached the maximum weeks in the future
        if (this.props.restrictFutureNavigation) {
            const diffTime = currentWeekStart.getTime() - startOfCurrentWeek.getTime();
            const diffWeeks = Math.ceil(diffTime / (1000 * 60 * 60 * 24 * 7));

            if (diffWeeks >= maxWeeks) {
                return; // Prevent navigation too far into the future
            }
        }

        const nextWeekStart = new Date(currentWeekStart);
        nextWeekStart.setDate(nextWeekStart.getDate() + 7);

        this.setState({ currentWeekStart: nextWeekStart }, () => {
            if (this.props.onWeekChange) {
                this.props.onWeekChange(nextWeekStart);
            }
        });
    }

    toggleMaintenanceSlot = (date, slotId) => {
        if (this.props.onToggleMaintenance) {
            this.props.onToggleMaintenance(date, slotId);
        }
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
                <th key={i} className={`stc-schedule-day-header ${isToday ? 'stc-today' : ''}`}>
                    <div className="stc-day-name">{currentDate.toLocaleDateString('ro-RO', { weekday: 'long' })}</div>
                    <div className="stc-day-date">{currentDate.toLocaleDateString('ro-RO', { day: 'numeric', month: 'long' })}</div>
                </th>
            );
        }

        return weekDays;
    }

    // În ScheduleTableComponent.js, modifică metoda renderTimeSlots
    renderTimeSlots = () => {
        const { currentWeekStart, timeSlots } = this.state;
        const { schedule, isPastDateBlocked, isReadOnly, reservations } = this.props;

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        return timeSlots.map(slot => (
            <tr key={slot.id} className="stc-time-slot-row">
                <td className="stc-time-slot-cell">{slot.display}</td>
                {[...Array(7)].map((_, i) => {
                    const currentDate = new Date(currentWeekStart);
                    currentDate.setDate(currentDate.getDate() + i);
                    const dateKey = this.formatDate(currentDate);

                    const isPastDate = isPastDateBlocked && currentDate <= today;
                    const slotStatus = schedule && schedule[dateKey] && schedule[dateKey][slot.id]
                        ? schedule[dateKey][slot.id]
                        : 'available';

                    let reservationInfo = null;
                    if (slotStatus === 'booked' && reservations) {
                        reservationInfo = reservations.find(r =>
                            this.formatDate(new Date(r.date)) === dateKey &&
                            r.timeSlot === slot.id
                        );
                    }

                    const isToday = this.isToday(currentDate);
                    const isClickable = !isPastDate && !isReadOnly &&
                        slotStatus !== 'booked' && slotStatus !== 'unavailable';

                    return (
                        <td
                            key={`${dateKey}_${slot.id}`}
                            className={`stc-schedule-slot stc-${slotStatus} ${isToday ? 'stc-today' : ''} ${isPastDate ? 'stc-past-date' : ''}`}
                            onClick={() => isClickable && this.toggleMaintenanceSlot(dateKey, slot.id)}
                            style={{ cursor: isClickable ? 'pointer' : 'not-allowed' }}
                        >
                            {slotStatus === 'unavailable' && (
                                <span className="stc-slot-status stc-unavailable">Închis</span>
                            )}
                            {slotStatus === 'maintenance' && (
                                <span className="stc-slot-status stc-maintenance">Mentenanță</span>
                            )}
                            {slotStatus === 'booked' && (
                                <div className="stc-slot-status stc-booked">
                                    <span className="stc-reservation-title">Rezervat</span>
                                    {reservationInfo && reservationInfo.user && (
                                        <div className="stc-reservation-details">
                                            <span className="stc-team-name">{reservationInfo.user.teamName || reservationInfo.user.name}</span>
                                            {reservationInfo.profileName && (
                                                <span className="stc-profile-name">{reservationInfo.profileName}</span>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )}
                        </td>
                    );
                })}
            </tr>
        ));
    }

    render() {
        const { currentWeekStart } = this.state;
        const {
            tableLoading,
            maintenanceCount,
            isReadOnly,
            showWeekNavigation = true,
            restrictPastNavigation = true,
            restrictFutureNavigation = true,
            maxFutureWeeks = 8,
            maxMaintenanceSlots
        } = this.props;

        const today = new Date();
        const startOfCurrentWeek = this.getStartOfWeek(today);

        // Determine if navigation buttons should be disabled
        const isCurrentWeekSelected = restrictPastNavigation &&
            currentWeekStart.getTime() === startOfCurrentWeek.getTime();

        const maxWeekStart = new Date(startOfCurrentWeek);
        maxWeekStart.setDate(maxWeekStart.getDate() + 7 * (maxFutureWeeks || 8));

        const isMaxWeekSelected = restrictFutureNavigation &&
            currentWeekStart.getTime() >= maxWeekStart.getTime();

        return (
            <div className="stc-schedule-component">
                {showWeekNavigation && (
                    <div className="stc-week-navigation">
                        <button
                            className={`stc-week-nav-button stc-prev ${isCurrentWeekSelected ? 'stc-disabled' : ''}`}
                            onClick={this.handlePreviousWeek}
                            disabled={isCurrentWeekSelected}
                        >
                            <ChevronLeft size={20} />
                            Săptămâna anterioară
                        </button>

                        <div className="stc-current-week">
                            <Calendar size={18} />
                            <span>
                                {currentWeekStart.toLocaleDateString('ro-RO', { day: 'numeric', month: 'long' })} - {
                                new Date(currentWeekStart.getTime() + 6 * 24 * 60 * 60 * 1000).toLocaleDateString('ro-RO', { day: 'numeric', month: 'long', year: 'numeric' })
                            }
                            </span>
                        </div>

                        <button
                            className={`stc-week-nav-button stc-next ${isMaxWeekSelected ? 'stc-disabled' : ''}`}
                            onClick={this.handleNextWeek}
                            disabled={isMaxWeekSelected}
                        >
                            Săptămâna următoare
                            <ChevronRight size={20} />
                        </button>
                    </div>
                )}

                <div className="stc-schedule-legend">
                    <div className="stc-legend-item">
                        <div className="stc-legend-color stc-available"></div>
                        <span>Disponibil</span>
                    </div>
                    <div className="stc-legend-item">
                        <div className="stc-legend-color stc-unavailable"></div>
                        <span>Închis</span>
                    </div>
                    <div className="stc-legend-item">
                        <div className="stc-legend-color stc-booked"></div>
                        <span>Rezervat</span>
                    </div>
                    <div className="stc-legend-item">
                        <div className="stc-legend-color stc-maintenance"></div>
                        <span>Mentenanță
                            {maxMaintenanceSlots ? (
                                <span className="stc-maintenance-count">({maintenanceCount || 0}/{maxMaintenanceSlots})</span>
                            ) : (
                                <span className="stc-maintenance-count">({maintenanceCount || 0})</span>
                            )}
        </span>
                    </div>
                    {!isReadOnly && (
                        <div className="stc-legend-info">
                            <Info size={16} />
                            <span>Apăsați pe un interval disponibil pentru a marca/demarca mentenanță</span>
                        </div>
                    )}
                </div>

                <div className="stc-schedule-table-container">
                    {tableLoading && (
                        <div className="stc-loading-overlay">
                            <div className="stc-loading-spinner"></div>
                            <span>Se încarcă programul...</span>
                        </div>
                    )}
                    <table className="stc-schedule-table">
                        <thead>
                        <tr>
                            <th className="stc-time-header">Interval Orar</th>
                            {this.renderWeekDays()}
                        </tr>
                        </thead>
                        <tbody>
                        {this.renderTimeSlots()}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }
}

export default ScheduleTableComponent;