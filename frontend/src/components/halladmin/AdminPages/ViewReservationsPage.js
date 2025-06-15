import React, { Component } from 'react';
import { Calendar, Clock, MapPin, CheckCircle } from 'lucide-react';
import './ViewReservationsPage.css';

class ViewReservationsPage extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';

        this.state = {
            halls: [],
            selectedHallId: '',
            reservations: [],
            loading: false,
            error: null
        };
    }

    componentDidMount() {
        this.loadHalls();
    }

    loadHalls = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                this.setState({ error: 'Nu sunteți autentificat corect' });
                return;
            }

            const response = await fetch(`${this.API_BASE_URL}/sportsHalls/admin/${userId}`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();
            console.log('Fetched halls:', halls); // Pentru debugging
            this.setState({ halls });
        } catch (error) {
            console.error('Eroare la încărcarea sălilor:', error);
            this.setState({ error: 'Eroare de conexiune la server' });
        }
    };

    loadReservations = async (hallId) => {
        if (!hallId) {
            this.setState({ reservations: [] });
            return;
        }

        this.setState({ loading: true, error: null });

        try {
            const response = await fetch(`${this.API_BASE_URL}/reservations?hallId=${hallId}&type=reservation`);
            if (response.ok) {
                const allReservations = await response.json();

                const now = new Date();
                const futureReservations = allReservations
                    .filter(reservation => {
                        const reservationDate = new Date(reservation.date);
                        return reservationDate >= now && reservation.status === 'CONFIRMED';
                    })
                    .sort((a, b) => new Date(a.date) - new Date(b.date)); // Sortează cronologic

                this.setState({ reservations: futureReservations });
            } else {
                this.setState({ error: 'Nu s-au putut încărca rezervările' });
            }
        } catch (error) {
            console.error('Eroare la încărcarea rezervărilor:', error);
            this.setState({ error: 'Eroare de conexiune la server' });
        } finally {
            this.setState({ loading: false });
        }
    };

    handleHallChange = (event) => {
        const hallId = event.target.value;
        this.setState({ selectedHallId: hallId });
        this.loadReservations(hallId);
    };

    formatDate = (dateString) => {
        const date = new Date(dateString);
        const options = {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        };
        return date.toLocaleDateString('ro-RO', options);
    };

    formatTimeSlot = (timeSlot) => {
        return timeSlot.replace('-', ' - ');
    };

    getDaysUntilReservation = (dateString) => {
        const reservationDate = new Date(dateString);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        reservationDate.setHours(0, 0, 0, 0);

        const diffTime = reservationDate - today;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays === 0) return 'Astăzi';
        if (diffDays === 1) return 'Mâine';
        return `În ${diffDays} zile`;
    };

    render() {
        const { halls, selectedHallId, reservations, loading, error } = this.state;

        return (
            <div className="admin-page">
                <h1 className="admin-page-title">Vizualizare Rezervări</h1>

                <div className="vrp-hall-selector-container">
                    <label htmlFor="vrp-hall-select" className="vrp-hall-selector-label">
                        Selectează sala de sport:
                    </label>
                    <select
                        id="vrp-hall-select"
                        value={selectedHallId}
                        onChange={this.handleHallChange}
                        className="vrp-hall-selector"
                    >
                        <option value="">-- Selectează o sală --</option>
                        {halls.map(hall => (
                            <option key={hall.id} value={hall.id}>
                                {hall.name} - {hall.city}, {hall.county}
                            </option>
                        ))}
                    </select>
                </div>

                {error && (
                    <div className="vrp-error-message">
                        {error}
                    </div>
                )}

                {loading && (
                    <div className="vrp-loading-container">
                        <div className="vrp-loading-spinner"></div>
                        <span>Se încarcă rezervările...</span>
                    </div>
                )}

                {!loading && selectedHallId && (
                    <div className="vrp-reservations-container">
                        <div className="vrp-reservations-header">
                            <h2 className="vrp-reservations-title">
                                Rezervări viitoare
                                <span className="vrp-reservations-count">({reservations.length})</span>
                            </h2>
                        </div>

                        {reservations.length === 0 ? (
                            <div className="vrp-empty-reservations">
                                <div className="vrp-empty-icon">
                                    <Calendar size={48} />
                                </div>
                                <h3>Nu există rezervări viitoare</h3>
                                <p>Nu sunt programate rezervări pentru această sală în perioada următoare.</p>
                            </div>
                        ) : (
                            <div className="vrp-reservations-list">
                                {reservations.map((reservation, index) => (
                                    <div key={reservation.id} className="vrp-reservation-item">
                                        <div className="vrp-reservation-number">
                                            {index + 1}
                                        </div>

                                        <div className="vrp-reservation-main-info">
                                            <div className="vrp-reservation-date">
                                                <Calendar size={16} className="vrp-date-icon" />
                                                {this.formatDate(reservation.date)}
                                            </div>
                                            <div className="vrp-reservation-time">
                                                <Clock size={16} className="vrp-time-icon" />
                                                {this.formatTimeSlot(reservation.timeSlot)}
                                            </div>
                                        </div>

                                        <div className="vrp-reservation-details">
                                            {reservation.user && (
                                                <div className="vrp-reservation-user">
                                                    {reservation.user.firstName} {reservation.user.lastName}
                                                </div>
                                            )}

                                            {reservation.price && (
                                                <div className="vrp-reservation-price">
                                                    {reservation.price} RON
                                                </div>
                                            )}
                                        </div>

                                        <div className="vrp-reservation-status">
                                            <span className="vrp-status-badge vrp-confirmed">
                                                <CheckCircle size={14} className="vrp-status-icon" />
                                                Confirmată
                                            </span>
                                            <div className="vrp-days-until">
                                                {this.getDaysUntilReservation(reservation.date)}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {!selectedHallId && !loading && (
                    <div className="vrp-no-hall-selected">
                        <div className="vrp-no-hall-icon">
                            <MapPin size={48} />
                        </div>
                        <h3>Selectează o sală de sport</h3>
                        <p>Pentru a vizualiza rezervările, te rog să selectezi mai întâi o sală din lista de mai sus.</p>
                    </div>
                )}
            </div>
        );
    }
}

export default ViewReservationsPage;