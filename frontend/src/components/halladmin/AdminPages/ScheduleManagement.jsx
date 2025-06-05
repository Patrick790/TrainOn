import React, { Component } from 'react';
import { Clock, Save, RotateCcw, Calendar, CheckCircle, AlertCircle } from 'lucide-react';
import './ScheduleManagement.css';

class ScheduleManagement extends Component {
    constructor(props) {
        super(props);

        this.state = {
            schedules: [],
            loading: false,
            saving: false,
            error: null,
            successMessage: ''
        };

        this.daysOfWeek = [
            { id: 1, name: 'Luni', short: 'LUN' },
            { id: 2, name: 'Marți', short: 'MAR' },
            { id: 3, name: 'Miercuri', short: 'MIE' },
            { id: 4, name: 'Joi', short: 'JOI' },
            { id: 5, name: 'Vineri', short: 'VIN' },
            { id: 6, name: 'Sâmbătă', short: 'SÂM' },
            { id: 7, name: 'Duminică', short: 'DUM' }
        ];

        this.timeSlots = this.generateTimeSlots();
    }

    generateTimeSlots = () => {
        const slots = [];
        for (let hour = 7; hour <= 24; hour += 1.5) {
            const wholeHours = Math.floor(hour);
            const minutes = (hour % 1) * 60;
            const timeString = `${wholeHours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
            slots.push(timeString);
        }
        return slots;
    }

    componentDidMount() {
        if (this.props.selectedHallId) {
            this.fetchSchedules();
        }
    }

    componentDidUpdate(prevProps) {
        if (prevProps.selectedHallId !== this.props.selectedHallId && this.props.selectedHallId) {
            this.fetchSchedules();
        }
    }

    fetchSchedules = async () => {
        try {
            this.setState({ loading: true, error: null });

            const token = localStorage.getItem('jwtToken');
            if (!token) {
                throw new Error('Nu sunteți autentificat corect');
            }

            const response = await fetch(`http://localhost:8080/schedules/hall/${this.props.selectedHallId}`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea programului: ${response.status}`);
            }

            const schedules = await response.json();

            if (schedules.length === 0) {
                this.initializeDefaultSchedule();
            } else {
                this.setState({ schedules });
            }

        } catch (error) {
            this.setState({ error: error.message });
        } finally {
            this.setState({ loading: false });
        }
    }

    initializeDefaultSchedule = () => {
        const defaultSchedules = this.daysOfWeek.map(day => ({
            dayOfWeek: day.id,
            startTime: '07:00',
            endTime: '23:30',
            isActive: true
        }));

        this.setState({ schedules: defaultSchedules });
    }

    handleTimeChange = (dayOfWeek, field, value) => {
        this.setState(prevState => ({
            schedules: prevState.schedules.map(schedule =>
                schedule.dayOfWeek === dayOfWeek
                    ? { ...schedule, [field]: value }
                    : schedule
            )
        }));
    }

    handleActiveToggle = (dayOfWeek) => {
        this.setState(prevState => ({
            schedules: prevState.schedules.map(schedule =>
                schedule.dayOfWeek === dayOfWeek
                    ? { ...schedule, isActive: !schedule.isActive }
                    : schedule
            )
        }));
    }

    saveSchedules = async () => {
        try {
            this.setState({ saving: true, error: null });

            const token = localStorage.getItem('jwtToken');
            if (!token) {
                throw new Error('Nu sunteți autentificat corect');
            }

            for (const schedule of this.state.schedules) {
                if (schedule.isActive && schedule.startTime >= schedule.endTime) {
                    throw new Error(`Ora de început trebuie să fie înainte de ora de sfârșit pentru ${this.getDayName(schedule.dayOfWeek)}`);
                }
            }

            const response = await fetch(`http://localhost:8080/schedules/hall/${this.props.selectedHallId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(this.state.schedules)
            });

            if (!response.ok) {
                throw new Error(`Eroare la salvarea programului: ${response.status}`);
            }

            const savedSchedules = await response.json();
            this.setState({
                schedules: savedSchedules,
                successMessage: 'Programul a fost salvat cu succes!'
            });

            setTimeout(() => {
                this.setState({ successMessage: '' });
            }, 4000);

        } catch (error) {
            this.setState({ error: error.message });
        } finally {
            this.setState({ saving: false });
        }
    }

    createDefaultSchedule = () => {
        this.initializeDefaultSchedule();
        this.setState({ successMessage: 'Program standard aplicat pentru toate zilele!' });
        setTimeout(() => {
            this.setState({ successMessage: '' });
        }, 4000);
    }

    getDayName = (dayOfWeek) => {
        const day = this.daysOfWeek.find(d => d.id === dayOfWeek);
        return day ? day.name : 'Unknown';
    }

    calculateSlots = (startTime, endTime) => {
        if (!startTime || !endTime) return 0;
        const [startHour, startMin] = startTime.split(':').map(Number);
        const [endHour, endMin] = endTime.split(':').map(Number);
        const startDecimal = startHour + startMin / 60;
        const endDecimal = endHour + endMin / 60;
        const duration = endDecimal - startDecimal;
        return Math.floor(duration / 1.5);
    }

    render() {
        const { loading, saving, error, successMessage, schedules } = this.state;
        const { selectedHallId } = this.props;

        if (!selectedHallId) {
            return (
                <div className="sm-empty-state">
                    <div className="sm-empty-icon">
                        <Calendar size={64} />
                    </div>
                    <h3>Selectați o sală</h3>
                    <p>Pentru a configura programul, selectați mai întâi o sală de sport din dropdown-ul de sus.</p>
                </div>
            );
        }

        const activeSchedules = schedules.filter(s => s.isActive);
        const totalSlots = activeSchedules.reduce((total, s) => total + this.calculateSlots(s.startTime, s.endTime), 0);

        return (
            <div className="sm-schedule-manager">
                {/* Header cu statistici */}
                <div className="sm-header">
                    <div className="sm-header-content">
                        <div className="sm-title-section">
                            <div className="sm-icon-wrapper">
                                <Clock size={24} />
                            </div>
                            <div>
                                <h2>Program de Funcționare</h2>
                                <p>Configurați orele disponibile pentru rezervări</p>
                            </div>
                        </div>
                        <div className="sm-stats">
                            <div className="sm-stat-card">
                                <span className="sm-stat-number">{activeSchedules.length}</span>
                                <span className="sm-stat-label">Zile Active</span>
                            </div>
                            <div className="sm-stat-card">
                                <span className="sm-stat-number">{totalSlots}</span>
                                <span className="sm-stat-label">Sloturi/Săptămână</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Notificări */}
                {successMessage && (
                    <div className="sm-notification sm-success">
                        <CheckCircle size={20} />
                        <span>{successMessage}</span>
                    </div>
                )}

                {error && (
                    <div className="sm-notification sm-error">
                        <AlertCircle size={20} />
                        <span>{error}</span>
                    </div>
                )}

                {/* Controale */}
                <div className="sm-controls">
                    <button
                        onClick={this.createDefaultSchedule}
                        className="sm-btn sm-btn-secondary"
                        disabled={saving}
                    >
                        <RotateCcw size={18} />
                        Program Standard
                    </button>
                    <button
                        onClick={this.saveSchedules}
                        disabled={saving || loading}
                        className="sm-btn sm-btn-primary"
                    >
                        {saving ? (
                            <>
                                <div className="sm-spinner-small"></div>
                                Se salvează...
                            </>
                        ) : (
                            <>
                                <Save size={18} />
                                Salvează Program
                            </>
                        )}
                    </button>
                </div>

                {/* Content principal */}
                <div className="sm-content">
                    {loading ? (
                        <div className="sm-loading">
                            <div className="sm-spinner"></div>
                            <p>Se încarcă programul...</p>
                        </div>
                    ) : (
                        <div className="sm-days-container">
                            {this.daysOfWeek.map(day => {
                                const schedule = schedules.find(s => s.dayOfWeek === day.id) || {
                                    dayOfWeek: day.id,
                                    startTime: '07:00',
                                    endTime: '22:00',
                                    isActive: true
                                };

                                const slots = this.calculateSlots(schedule.startTime, schedule.endTime);

                                return (
                                    <div key={day.id} className={`sm-day-card ${schedule.isActive ? 'active' : 'inactive'}`}>
                                        <div className="sm-day-header">
                                            <div className="sm-day-toggle">
                                                <input
                                                    type="checkbox"
                                                    id={`day-${day.id}`}
                                                    checked={schedule.isActive}
                                                    onChange={() => this.handleActiveToggle(day.id)}
                                                    className="sm-toggle"
                                                />
                                                <label htmlFor={`day-${day.id}`} className="sm-toggle-label">
                                                    <div className="sm-day-info">
                                                        <span className="sm-day-short">{day.short}</span>
                                                        <span className="sm-day-name">{day.name}</span>
                                                    </div>
                                                </label>
                                            </div>
                                            {schedule.isActive && (
                                                <div className="sm-slots-badge">
                                                    {slots} sloturi
                                                </div>
                                            )}
                                        </div>

                                        <div className="sm-day-content">
                                            {schedule.isActive ? (
                                                <div className="sm-time-selector">
                                                    <div className="sm-time-group">
                                                        <label>Deschidere</label>
                                                        <select
                                                            value={schedule.startTime}
                                                            onChange={(e) => this.handleTimeChange(day.id, 'startTime', e.target.value)}
                                                            className="sm-select"
                                                        >
                                                            {this.timeSlots.map(time => (
                                                                <option key={time} value={time}>{time}</option>
                                                            ))}
                                                        </select>
                                                    </div>
                                                    <div className="sm-time-separator">→</div>
                                                    <div className="sm-time-group">
                                                        <label>Închidere</label>
                                                        <select
                                                            value={schedule.endTime}
                                                            onChange={(e) => this.handleTimeChange(day.id, 'endTime', e.target.value)}
                                                            className="sm-select"
                                                        >
                                                            {this.timeSlots.filter(time => time > schedule.startTime).map(time => (
                                                                <option key={time} value={time}>{time}</option>
                                                            ))}
                                                        </select>
                                                    </div>
                                                </div>
                                            ) : (
                                                <div className="sm-closed-state">
                                                    <span>Sala este închisă</span>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Footer info */}
                <div className="sm-footer">
                    <div className="sm-info-card">
                        <div className="sm-info-icon">
                            <Clock size={20} />
                        </div>
                        <div className="sm-info-content">
                            <h4>Despre intervalele de rezervare</h4>
                            <p>Fiecare slot de rezervare durează <strong>1.5 ore (90 minute)</strong>. Sistemul va genera automat toate intervalele disponibile pe baza programului configurat.</p>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

export default ScheduleManagement;