import React, { useState, useEffect } from 'react';
import { Calendar, Clock, MapPin, Users, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import Header from '../Header';
import './ReservationsPage.css';

const ReservationsPage = () => {
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

    // Funcție pentru a obține informațiile de bază ale utilizatorului pentru sidebar
    const fetchBasicUserInfo = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                return;
            }

            const response = await fetch(`http://localhost:8080/users/${userId}`, {
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
        setLoading(true);
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                return;
            }

            const response = await fetch(`http://localhost:8080/reservations/user/${userId}`, {
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
                const upcoming = [];
                const past = [];
                const cancelled = [];

                reservationsData.forEach(reservation => {
                    const reservationDate = new Date(reservation.date + 'T' + reservation.startTime);

                    if (reservation.status === 'CANCELLED') {
                        cancelled.push(reservation);
                    } else if (reservationDate > now) {
                        upcoming.push(reservation);
                    } else {
                        past.push(reservation);
                    }
                });

                setReservations({
                    upcoming: upcoming.sort((a, b) => new Date(a.date + 'T' + a.startTime) - new Date(b.date + 'T' + b.startTime)),
                    past: past.sort((a, b) => new Date(b.date + 'T' + b.startTime) - new Date(a.date + 'T' + a.startTime)),
                    cancelled: cancelled.sort((a, b) => new Date(b.date + 'T' + b.startTime) - new Date(a.date + 'T' + a.startTime))
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
    }, [isLoggedIn]);

    const handleLogout = () => {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userType');
        localStorage.removeItem('userId');
        localStorage.removeItem('jwtToken');
        setIsLoggedIn(false);
        window.location.href = '/';
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ro-RO', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    const formatTime = (timeSlot) => {
        if (!timeSlot) return '';
        // timeSlot format: "10:00-12:00"
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
            const response = await fetch(`http://localhost:8080/reservations/${reservationId}/cancel`, {
                method: 'PUT',
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