import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Star, Users, DollarSign, Compass, Info, Phone } from 'lucide-react';
import axios from 'axios';
import Header from './Header';
import LoginModal from '../login/LoginModal';
import RegisterModal from '../register/RegisterModal';
import SportsHallMap from './SportsHallMap';
import SearchBar from './SearchBar';
import Footer from '../pageComponents/Footer';
import './SportsHallDetailPage.css';

const ReservationOptionsModal = ({ isOpen, onClose }) => {
    const [userType, setUserType] = useState(null);
    const [loading, setLoading] = useState(true);
    const API_URL = process.env.REACT_APP_API_URL || '';


    useEffect(() => {
        if (isOpen) {
            fetchUserInfo();
        }
    }, [isOpen]);

    const fetchUserInfo = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                setLoading(false);
                return;
            }

            const response = await fetch(`${process.env.REACT_APP_API_URL || ''}/users/${userId}`, {                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const userData = await response.json();
                setUserType(userData.teamType);
                setLoading(false);
            } else {
                console.error('Failed to fetch user data:', response.status);
                setLoading(false);
            }
        } catch (error) {
            console.error('Error fetching user data:', error);
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    const isTeamUser = userType && userType.trim() !== '';

    return (
        <div className="reservation-modal-backdrop">
            <div className="reservation-modal-content">
                <h2 className="reservation-modal-title">Opțiuni de rezervare</h2>
                <p className="reservation-modal-description">
                    {loading
                        ? 'Se încarcă opțiunile...'
                        : isTeamUser
                            ? 'Alege modul în care dorești să rezervi'
                            : 'Rezervă locurile disponibile în acest moment'
                    }
                </p>

                {!loading && (
                    <div className="reservation-options">
                        {isTeamUser && (
                            <div
                                className="reservation-option"
                                onClick={() => { window.location.href = '/profile-creation'; onClose(); }}
                            >
                                <div className="reservation-option-icon">
                                    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="12" cy="7" r="4"></circle>
                                    </svg>
                                </div>
                                <h3>Creează profil rezervări</h3>
                                <p>Personalizează opțiunile pentru algoritm de prioritizare echipe</p>
                            </div>
                        )}

                        <div
                            className={`reservation-option ${!isTeamUser ? 'single-option' : ''}`}
                            onClick={() => { window.location.href = '/fcfs-reservation'; onClose(); }}
                        >
                            <div className="reservation-option-icon">
                                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                                    <line x1="16" y1="2" x2="16" y2="6"></line>
                                    <line x1="8" y1="2" x2="8" y2="6"></line>
                                    <line x1="3" y1="10" x2="21" y2="10"></line>
                                </svg>
                            </div>
                            <h3>Rezervă locurile rămase</h3>
                            <p>
                                {isTeamUser
                                    ? 'Rezervare rapidă pentru locurile disponibile în acest moment'
                                    : 'Rezervare pentru locurile disponibile în acest moment'
                                }
                            </p>
                        </div>
                    </div>
                )}

                <div className="reservation-modal-footer">
                    <button className="reservation-modal-close-button" onClick={onClose}>
                        Anulează
                    </button>
                </div>
            </div>
        </div>
    );
};

const SportsHallDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [sportsHall, setSportsHall] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentImageIndex, setCurrentImageIndex] = useState(0);

    const [isReservationModalOpen, setIsReservationModalOpen] = useState(false);

    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('isLoggedIn') === 'true');
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);

    const API_URL = process.env.REACT_APP_API_URL || '';

    useEffect(() => {
        fetchSportsHall();
    }, [id]);

    const fetchSportsHall = async () => {
        try {
            const response = await axios.get(`${API_URL}/sportsHalls/${id}`);
            setSportsHall(response.data);
            setLoading(false);
        } catch (err) {
            console.error('Eroare la încărcarea sălii:', err);
            setError('Nu s-a putut încărca sala de sport.');
            setLoading(false);
        }
    };

    const checkLoginStatus = () => {
        setIsLoggedIn(localStorage.getItem('isLoggedIn') === 'true');
    };

    const handleLogout = () => {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userType');
        localStorage.removeItem('userId');
        localStorage.removeItem('jwtToken');
        setIsLoggedIn(false);
        window.location.href = '/';
    };

    const toggleLoginModal = () => {
        setIsLoginModalOpen(!isLoginModalOpen);
        setIsRegisterModalOpen(false);
    };

    const toggleRegisterModal = () => {
        setIsRegisterModalOpen(!isRegisterModalOpen);
        setIsLoginModalOpen(false);
    };

    const goToReviews = () => {
        navigate(`/sportsHalls/${id}/reviews`);
    };

    const openReservationModal = () => {
        if (isLoggedIn) {
            setIsReservationModalOpen(true);
        } else {
            toggleLoginModal();
        }
    };

    const closeReservationModal = () => {
        setIsReservationModalOpen(false);
    };

    // Funcții pentru imagini
    const getImageUrl = (image) => {
        if (!image) return 'https://via.placeholder.com/800x500?text=Imagine+Indisponibilă';
        return `${API_URL}/images/${image.id}`;
    };

    const nextImage = () => {
        if (sportsHall && sportsHall.images && sportsHall.images.length > 0) {
            setCurrentImageIndex((prevIndex) => (prevIndex + 1) % sportsHall.images.length);
        }
    };

    const prevImage = () => {
        if (sportsHall && sportsHall.images && sportsHall.images.length > 0) {
            setCurrentImageIndex((prevIndex) => (prevIndex - 1 + sportsHall.images.length) % sportsHall.images.length);
        }
    };

    const selectImage = (index) => {
        setCurrentImageIndex(index);
    };

    const calculateRating = (hall) => {
        if (!hall || !hall.capacity) return 3.0;

        const capacity = hall.capacity || 0;
        let baseRating = 3.5;

        if (capacity > 100) baseRating += 1.2;
        else if (capacity > 50) baseRating += 0.8;
        else if (capacity > 20) baseRating += 0.4;

        const rating = Math.min(5, Math.max(1, baseRating));
        return rating.toFixed(1);
    };

    const renderStars = (rating) => {
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.5;
        const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

        return (
            <div className="detail-stars" onClick={goToReviews} style={{ cursor: 'pointer' }} title="Vezi recenziile">
                {[...Array(fullStars)].map((_, i) => (
                    <Star key={`full-${i}`} className="star-full" fill="#FFD700" color="#FFD700" size={18} />
                ))}
                {hasHalfStar && <Star className="star-half" fill="#FFD700" color="#FFD700" size={18} />}
                {[...Array(emptyStars)].map((_, i) => (
                    <Star key={`empty-${i}`} className="star-empty" color="#FFD700" size={18} />
                ))}
                <span className="rating-value">{rating}</span>
            </div>
        );
    };

    const formatPrice = (price) => {
        if (!price) return 'Preț la cerere';
        return `${price} RON/1h30`;
    };

    const formatPhoneNumber = (phoneNumber) => {
        if (!phoneNumber) return 'Nu este disponibil';
        return phoneNumber;
    };

    const processFacilities = (facilitiesString) => {
        if (!facilitiesString) return [];
        return facilitiesString.split(',').map(facility => facility.trim());
    };

    const handleCityChange = (city) => {
        console.log('City changed to:', city);
    };

    const handlePhoneCall = (phoneNumber) => {
        if (phoneNumber && phoneNumber !== 'Nu este disponibil') {
            window.location.href = `tel:${phoneNumber.replace(/\s/g, '')}`;
        }
    };

    if (loading) {
        return <div className="detail-page-loading">Se încarcă...</div>;
    }

    if (error) {
        return <div className="detail-page-error">{error}</div>;
    }

    if (!sportsHall) {
        return <div className="detail-page-error">Nu s-a găsit sala de sport.</div>;
    }

    const rating = calculateRating(sportsHall);
    const facilities = processFacilities(sportsHall.facilities);

    return (
        <div className="main-container">
            {/* Header */}
            <Header
                isLoggedIn={isLoggedIn}
                onLoginClick={toggleLoginModal}
                onRegisterClick={toggleRegisterModal}
                onLogout={handleLogout}
            />

            <main className="main-content">
                <SearchBar onCityChange={handleCityChange} />
            </main>

            <div className="detail-page-wrapper">
                <div className="detail-page-container">
                    {/* Buton înapoi */}
                    <div className="detail-back-link">
                        <Link to="/" className="back-link">
                            <ArrowLeft size={16} />
                            Înapoi la lista de săli
                        </Link>
                    </div>

                    <div className="detail-page-header">
                        <h1 className="detail-page-title">{sportsHall.name}</h1>
                        <div className="detail-page-meta">
                            <div className="detail-page-location">
                                <Compass size={16} />
                                <span>{sportsHall.address}, {sportsHall.city}</span>
                            </div>
                            <div className="detail-page-rating">
                                {renderStars(rating)}
                                <Link to={`/sportsHalls/${id}/reviews`} className="view-reviews-link">
                                    Vezi toate recenziile
                                </Link>
                            </div>
                        </div>
                        {sportsHall.type && <div className="detail-page-badge">{sportsHall.type}</div>}
                    </div>

                    <div className="detail-image-gallery">
                        <div className="detail-main-image-container">
                            {sportsHall.images && sportsHall.images.length > 0 ? (
                                <>
                                    <img
                                        src={getImageUrl(sportsHall.images[currentImageIndex])}
                                        alt={sportsHall.name}
                                        className="detail-main-image"
                                        onError={(e) => {
                                            e.target.onerror = null;
                                            e.target.src = 'https://via.placeholder.com/800x500?text=Imagine+Indisponibilă';
                                        }}
                                    />
                                    {sportsHall.images.length > 1 && (
                                        <>
                                            <button onClick={prevImage} className="detail-image-nav detail-prev">❮</button>
                                            <button onClick={nextImage} className="detail-image-nav detail-next">❯</button>
                                        </>
                                    )}
                                </>
                            ) : (
                                <img
                                    src="https://via.placeholder.com/800x500?text=Imagine+Indisponibilă"
                                    alt="Imagine indisponibilă"
                                    className="detail-main-image"
                                />
                            )}
                        </div>
                        {sportsHall.images && sportsHall.images.length > 1 && (
                            <div className="detail-thumbnail-container">
                                {sportsHall.images.map((image, index) => (
                                    <img
                                        key={image.id}
                                        src={getImageUrl(image)}
                                        alt={`Thumbnail ${index + 1}`}
                                        className={`detail-thumbnail ${index === currentImageIndex ? 'active' : ''}`}
                                        onClick={() => selectImage(index)}
                                        onError={(e) => {
                                            e.target.onerror = null;
                                            e.target.src = 'https://via.placeholder.com/100x100?text=Imagine+Indisponibilă';
                                        }}
                                    />
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="detail-page-content">
                        <div className="detail-page-columns">
                            <div className="detail-page-left-column">
                                <div className="detail-page-section">
                                    <h2 className="detail-section-title">Informații generale</h2>
                                    <div className="detail-info-grid">
                                        <div className="detail-info-item">
                                            <DollarSign size={20} />
                                            <div className="detail-info-text">
                                                <h3>Tarif</h3>
                                                <p>{formatPrice(sportsHall.tariff)}</p>
                                            </div>
                                        </div>
                                        <div className="detail-info-item">
                                            <Users size={20} />
                                            <div className="detail-info-text">
                                                <h3>Capacitate</h3>
                                                <p>{sportsHall.capacity} persoane</p>
                                            </div>
                                        </div>
                                        <div className="detail-info-item">
                                            <Compass size={20} />
                                            <div className="detail-info-text">
                                                <h3>Locație</h3>
                                                <p>{sportsHall.city}, {sportsHall.county}</p>
                                            </div>
                                        </div>
                                        <div className="detail-info-item">
                                            <Info size={20} />
                                            <div className="detail-info-text">
                                                <h3>Tip sală</h3>
                                                <p>{sportsHall.type || 'Multifuncțională'}</p>
                                            </div>
                                        </div>
                                        <div
                                            className={`detail-info-item ${sportsHall.phoneNumber && sportsHall.phoneNumber !== 'Nu este disponibil' ? 'detail-phone-clickable' : ''}`}
                                            onClick={() => handlePhoneCall(sportsHall.phoneNumber)}
                                            title={sportsHall.phoneNumber && sportsHall.phoneNumber !== 'Nu este disponibil' ? 'Click pentru a apela' : ''}
                                        >
                                            <Phone size={20} />
                                            <div className="detail-info-text">
                                                <h3>Telefon</h3>
                                                <p>{formatPhoneNumber(sportsHall.phoneNumber)}</p>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="detail-page-section">
                                    <h2 className="detail-section-title">Descriere</h2>
                                    <p className="detail-description">
                                        {sportsHall.description || 'Nu există descriere disponibilă pentru această sală de sport.'}
                                    </p>
                                </div>

                                <SportsHallMap
                                    address={sportsHall.address}
                                    city={sportsHall.city}
                                    county={sportsHall.county}
                                />
                            </div>

                            <div className="detail-page-right-column">
                                <div className="detail-page-section">
                                    <h2 className="detail-section-title">Facilități</h2>
                                    {facilities.length > 0 ? (
                                        <ul className="detail-facilities-list">
                                            {facilities.map((facility, index) => (
                                                <li key={index} className="detail-facility-item">
                                                    <div className="detail-facility-icon">✓</div>
                                                    <span>{facility}</span>
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p className="detail-no-facilities">Nu există facilități specificate pentru această sală.</p>
                                    )}
                                </div>

                                <div className="detail-page-section detail-contact-section">
                                    <h2 className="detail-section-title">Rezervă acum</h2>
                                    <p className="detail-contact-info">
                                        Pentru rezervări și mai multe informații, vă rugăm să vă autentificați în cont sau să creați un cont nou.
                                    </p>
                                    <div className="detail-contact-buttons">
                                        {isLoggedIn ? (
                                            <button className="detail-book-button" onClick={openReservationModal}>Rezervă</button>
                                        ) : (
                                            <>
                                                <button onClick={toggleLoginModal} className="detail-login-button">
                                                    Intră în cont
                                                </button>
                                                <button onClick={toggleRegisterModal} className="detail-register-button">
                                                    Creează cont
                                                </button>
                                            </>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <Footer />

            {!isLoggedIn && (
                <>
                    <LoginModal
                        isOpen={isLoginModalOpen}
                        onClose={toggleLoginModal}
                        onRegisterClick={toggleRegisterModal}
                        onLoginSuccess={checkLoginStatus}
                    />
                    <RegisterModal
                        isOpen={isRegisterModalOpen}
                        onClose={toggleRegisterModal}
                    />
                </>
            )}

            <ReservationOptionsModal
                isOpen={isReservationModalOpen}
                onClose={closeReservationModal}
            />
        </div>
    );
};

export default SportsHallDetailPage;