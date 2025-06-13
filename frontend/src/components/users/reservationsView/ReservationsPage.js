import React, { useState, useEffect } from 'react';
import { Calendar, Clock, MapPin, Users, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import Header from '../Header';
import './ReservationsPage.css';

const ReservationsPage = () => {
    const API_BASE_URL = process.env.REACT_APP_API_URL || '';

    const [activeSection, setActiveSection] = useState('upcoming');
    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('isLoggedIn') === 'true');
    const [userInfo, setUserInfo] = useState({
        firstName: '',
        lastName: '',
        email: ''
    });
    const [reservations, setReservations] = useState({
        upcoming: [],
        past: [],
        cancelled: []
    });
    const [loading, setLoading] = useState(false);

    // Funcție helper pentru a parsa corect data și ora rezervării
    const parseReservationDateTime = (dateString, timeSlot) => {
        try {
            // Extrage data (fără timp) din string-ul de la backend
            const dateOnly = dateString.split('T')[0]; // "2025-06-06T21:00:00.000+00:00" -> "2025-06-06"

            // Extrage ora de început din timeSlot (ex: "19:00 - 20:30" -> "19:00")
            const startTime = timeSlot ? timeSlot.split(' - ')[0] : '00:00';

            // Construiește data completă cu ora corectă în timezone-ul local
            const dateTimeString = `${dateOnly}T${startTime}:00`;

            // Creează data în timezone-ul local (România)
            const date = new Date(dateTimeString);

            console.log(`Parsing: ${dateString} + ${timeSlot} -> ${dateTimeString} -> ${date} (${date.toLocaleString('ro-RO')})`);

            return date;
        } catch (error) {
            console.error('Error parsing reservation date/time:', error);
            // Fallback la comportamentul vechi
            const dateOnly = dateString.split('T')[0];
            return new Date(dateOnly + 'T00:00:00');
        }
    };

    // Funcție pentru a obține informațiile de bază ale utilizatorului pentru sidebar
    const fetchBasicUserInfo = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                return;
            }

            const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const userData = await response.json();

                // Separăm numele complet în prenume și nume
                const nameParts = userData.name ? userData.name.split(' ') : ['', ''];
                const firstName = nameParts[0] || '';
                const lastName = nameParts.slice(1).join(' ') || '';

                setUserInfo({
                    firstName: firstName,
                    lastName: lastName,
                    email: userData.email || ''
                });
            }
        } catch (err) {
            console.error('Eroare la încărcarea informațiilor de bază ale utilizatorului:', err);
        }
    };

    // Funcție pentru a încărca rezervările
    const fetchReservations = async () => {
        console.log('fetchReservations called at:', new Date().toTimeString());
        setLoading(true);
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                return;
            }

            const response = await fetch(`${API_BASE_URL}/reservations/user/${userId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const reservationsData = await response.json();

                // Clasificăm rezervările pe categorii
                const now = new Date();
                console.log('Current time for comparison:', now.toLocaleString('ro-RO'));

                const upcoming = [];
                const past = [];
                const cancelled = [];

                reservationsData.forEach(reservation => {
                    // Construiește data rezervării CORECT cu ora din timeSlot
                    const reservationDateTime = parseReservationDateTime(reservation.date, reservation.timeSlot);

                    console.log(`Reservation: ${reservation.hall?.name || 'Unknown'} on ${reservationDateTime.toLocaleString('ro-RO')} - Status: ${reservation.status}`);
                    console.log(`Comparison: ${reservationDateTime.toLocaleString('ro-RO')} > ${now.toLocaleString('ro-RO')} = ${reservationDateTime > now}`);

                    if (reservation.status === 'CANCELLED') {
                        cancelled.push(reservation);
                    } else if (reservationDateTime > now) {
                        // Rezervarea este în viitor
                        upcoming.push(reservation);
                        console.log(`Added to upcoming: ${reservation.hall?.name} on ${reservationDateTime.toLocaleString('ro-RO')}`);
                    } else {
                        // Rezervarea este în trecut
                        past.push(reservation);
                        console.log(`Added to past: ${reservation.hall?.name} on ${reservationDateTime.toLocaleString('ro-RO')}`);
                    }
                });

                console.log('Final classification - Upcoming:', upcoming.length, 'Past:', past.length, 'Cancelled:', cancelled.length);

                setReservations({
                    upcoming: upcoming.sort((a, b) => {
                        const aDate = parseReservationDateTime(a.date, a.timeSlot);
                        const bDate = parseReservationDateTime(b.date, b.timeSlot);
                        return aDate - bDate;
                    }),
                    past: past.sort((a, b) => {
                        const aDate = parseReservationDateTime(a.date, a.timeSlot);
                        const bDate = parseReservationDateTime(b.date, b.timeSlot);
                        return bDate - aDate; // Descending order for past reservations
                    }),
                    cancelled: cancelled.sort((a, b) => {
                        const aDate = parseReservationDateTime(a.date, a.timeSlot);
                        const bDate = parseReservationDateTime(b.date, b.timeSlot);
                        return bDate - aDate; // Descending order for cancelled reservations
                    })
                });
            }
        } catch (err) {
            console.error('Eroare la încărcarea rezervărilor:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isLoggedIn) {
            fetchBasicUserInfo();
            fetchReservations();
        }
    }, []); // Elimină dependința de isLoggedIn care cauzează re-render-ul

    const handleLogout = () => {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userType');
        localStorage.removeItem('userId');
        localStorage.removeItem('jwtToken');
        setIsLoggedIn(false);
        window.location.href = '/';
    };

    const formatDate = (dateString) => {
        console.log('formatDate input:', dateString);
        // Folosește aceeași logică de parsing
        const dateOnly = dateString.split('T')[0];
        const date = new Date(dateOnly + 'T00:00:00');
        console.log('formatDate parsed:', date);
        const formatted = date.toLocaleDateString('ro-RO', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
        console.log('formatDate output:', formatted);
        return formatted;
    };

    const formatTime = (timeSlot) => {
        if (!timeSlot) return '';
        // timeSlot format: "10:00 - 12:00"
        return timeSlot;
    };

    const getStatusIcon = (status) => {
        switch (status) {
            case 'CONFIRMED':
                return <CheckCircle size={16} className="reservations-status-icon confirmed" />;
            case 'PENDING':
                return <Clock size={16} className="reservations-status-icon pending" />;
            case 'CANCELLED':
                return <XCircle size={16} className="reservations-status-icon cancelled" />;
            default:
                return <AlertCircle size={16} className="reservations-status-icon" />;
        }
    };

    const getStatusText = (status) => {
        switch (status) {
            case 'CONFIRMED':
                return 'Confirmată';
            case 'PENDING':
                return 'În așteptare';
            case 'CANCELLED':
                return 'Anulată';
            default:
                return status;
        }
    };

    const handleCancelReservation = async (reservationId) => {
        if (!window.confirm('Ești sigur că vrei să anulezi această rezervare?')) {
            return;
        }

        try {
            const token = localStorage.getItem('jwtToken');
            const response = await fetch(`${API_BASE_URL}/reservations/${reservationId}/cancel`, {                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                // Reîncărcăm rezervările pentru a reflecta schimbarea
                fetchReservations();
            } else {
                alert('Eroare la anularea rezervării. Te rugăm să încerci din nou.');
            }
        } catch (err) {
            console.error('Eroare la anularea rezervării:', err);
            alert('Eroare la anularea rezervării. Te rugăm să încerci din nou.');
        }
    };

    const renderReservationCard = (reservation) => {
        const canCancel = activeSection === 'upcoming' && reservation.status !== 'CANCELLED';

        return (
            <div key={reservation.id} className="reservations-card">
                <div className="reservations-card-header">
                    <div className="reservations-card-title">
                        <h3>{reservation.hall?.name || 'Sala necunoscută'}</h3>
                        <div className="reservations-status">
                            {getStatusIcon(reservation.status)}
                            <span>{getStatusText(reservation.status)}</span>
                        </div>
                    </div>
                </div>

                <div className="reservations-card-content">
                    <div className="reservations-info-row">
                        <Calendar size={16} />
                        <span>{formatDate(reservation.date)}</span>
                    </div>

                    <div className="reservations-info-row">
                        <Clock size={16} />
                        <span>{formatTime(reservation.timeSlot)}</span>
                    </div>

                    <div className="reservations-info-row">
                        <MapPin size={16} />
                        <span>{reservation.hall?.address || 'Adresă necunoscută'}</span>
                    </div>

                    {reservation.price && (
                        <div className="reservations-price">
                            <strong>{reservation.price} RON</strong>
                        </div>
                    )}
                </div>

                {canCancel && (
                    <div className="reservations-card-actions">
                        <button
                            className="reservations-cancel-btn"
                            onClick={() => handleCancelReservation(reservation.id)}
                        >
                            Anulează rezervarea
                        </button>
                    </div>
                )}
            </div>
        );
    };

    const renderContent = () => {
        if (loading) {
            return (
                <div className="reservations-loading">
                    <div className="reservations-loading-spinner"></div>
                    <p>Se încarcă rezervările...</p>
                </div>
            );
        }

        const currentReservations = reservations[activeSection];

        if (currentReservations.length === 0) {
            let emptyMessage = '';
            switch (activeSection) {
                case 'upcoming':
                    emptyMessage = 'Nu ai rezervări viitoare.';
                    break;
                case 'past':
                    emptyMessage = 'Nu ai rezervări trecute.';
                    break;
                case 'cancelled':
                    emptyMessage = 'Nu ai rezervări anulate.';
                    break;
                default:
                    emptyMessage = 'Nu ai rezervări.';
            }

            return (
                <div className="reservations-empty">
                    <Calendar size={48} />
                    <p>{emptyMessage}</p>
                </div>
            );
        }

        return (
            <div className="reservations-list">
                {currentReservations.map(renderReservationCard)}
            </div>
        );
    };

    const getSectionTitle = () => {
        switch (activeSection) {
            case 'upcoming':
                return 'Rezervări viitoare';
            case 'past':
                return 'Rezervări trecute';
            case 'cancelled':
                return 'Rezervări anulate';
            default:
                return 'Rezervări';
        }
    };

    const getReservationCount = (section) => {
        return reservations[section].length;
    };

    return (
        <div className="reservations-page">
            <Header
                isLoggedIn={isLoggedIn}
                onLoginClick={() => {}}
                onRegisterClick={() => {}}
                onLogout={handleLogout}
            />

            <div className="reservations-container">
                <nav className="reservations-sidebar">
                    <div className="reservations-user-info">
                        <div className="reservations-avatar">
                            <Calendar size={32} />
                        </div>
                        <h3>{userInfo.firstName} {userInfo.lastName}</h3>
                        <p>{userInfo.email}</p>
                    </div>

                    <div className="reservations-nav">
                        <button
                            className={`reservations-nav-item ${activeSection === 'upcoming' ? 'active' : ''}`}
                            onClick={() => setActiveSection('upcoming')}
                        >
                            <CheckCircle size={20} />
                            <span>Rezervări viitoare</span>
                            <span className="reservations-count">{getReservationCount('upcoming')}</span>
                        </button>

                        <button
                            className={`reservations-nav-item ${activeSection === 'past' ? 'active' : ''}`}
                            onClick={() => setActiveSection('past')}
                        >
                            <Clock size={20} />
                            <span>Rezervări trecute</span>
                            <span className="reservations-count">{getReservationCount('past')}</span>
                        </button>

                        <button
                            className={`reservations-nav-item ${activeSection === 'cancelled' ? 'active' : ''}`}
                            onClick={() => setActiveSection('cancelled')}
                        >
                            <XCircle size={20} />
                            <span>Rezervări anulate</span>
                            <span className="reservations-count">{getReservationCount('cancelled')}</span>
                        </button>
                    </div>
                </nav>

                <main className="reservations-content">
                    <div className="reservations-section">
                        <div className="reservations-section-header">
                            <h2>{getSectionTitle()}</h2>
                        </div>
                        {renderContent()}
                    </div>
                </main>
            </div>
        </div>
    );
};

export default ReservationsPage;