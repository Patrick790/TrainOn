import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { MapPin, ChevronDown, LogOut, Star, ArrowLeft, Users, DollarSign, Compass, Info } from 'lucide-react';
import axios from 'axios';
import LoginModal from '../login/LoginModal';
import RegisterModal from '../register/RegisterModal';
import SportsHallMap from './SportsHallMap';
import ReservationOptionsModal from './ReservationOptionsModal';
import './SportsHallDetailPage.css';

const SportsHallDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [sportsHall, setSportsHall] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentImageIndex, setCurrentImageIndex] = useState(0);

    // State pentru modalul de opțiuni de rezervare
    const [isReservationModalOpen, setIsReservationModalOpen] = useState(false);

    // State pentru header și search bar - similar cu MainPage
    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('isLoggedIn') === 'true');
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
    const [location, setLocation] = useState('Cluj-Napoca');
    const [activity, setActivity] = useState('Sport');
    const [searchQuery, setSearchQuery] = useState('');
    const [isLocationDropdownOpen, setIsLocationDropdownOpen] = useState(false);
    const [isActivityDropdownOpen, setIsActivityDropdownOpen] = useState(false);

    const API_URL = 'http://localhost:8080';

    useEffect(() => {
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

        fetchSportsHall();
    }, [id]);

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

    const toggleLocationDropdown = () => {
        setIsLocationDropdownOpen(!isLocationDropdownOpen);
        setIsActivityDropdownOpen(false);
    };

    const toggleActivityDropdown = () => {
        setIsActivityDropdownOpen(!isActivityDropdownOpen);
        setIsLocationDropdownOpen(false);
    };

    const handleLocationChange = (newLocation) => {
        setLocation(newLocation);
        setIsLocationDropdownOpen(false);
    };

    const handleActivityChange = (newActivity) => {
        setActivity(newActivity);
        setIsActivityDropdownOpen(false);
    };

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
    };

    const handleSearch = (e) => {
        e.preventDefault();
        // Implementare căutare
    };

    // Funcții pentru gestionarea modalului de rezervare
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

    const handleReserveRemaining = () => {
        // Aici vom implementa logica pentru rezervarea locurilor rămase
        // De exemplu, deschideți un alt modal pentru selectarea datei și orei
        closeReservationModal();
        alert('Funcționalitatea de rezervare a locurilor rămase va fi implementată în curând.');
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

    // Funcții pentru procesarea datelor sălii
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
            <div className="detail-stars">
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
        return `${price} RON/oră`;
    };

    const processFacilities = (facilitiesString) => {
        if (!facilitiesString) return [];
        return facilitiesString.split(',').map(facility => facility.trim());
    };

    const locations = ['Cluj-Napoca', 'Timisoara', 'Bucuresti', 'Iasi'];
    const activities = ['Fitness', 'Inot', 'Tenis', 'Fotbal', 'Baschet', 'Handbal'];

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
            {/* Header - similar cu MainPage */}
            <header className="header">
                <div className="logo-container">
                    <div className="logo"></div>
                    <span className="logo-text">Licenta</span>
                </div>
                <div className="auth-buttons">
                    {isLoggedIn ? (
                        <button onClick={handleLogout} className="auth-button logout-button">
                            <LogOut size={16} />
                            Deconectare
                        </button>
                    ) : (
                        <>
                            <button onClick={toggleLoginModal} className="auth-button">
                                Intra in cont
                            </button>
                            <button onClick={toggleRegisterModal} className="auth-button">
                                Inregistrare
                            </button>
                        </>
                    )}
                </div>
            </header>

            {/* Main content - combinăm search bar din MainPage cu conținutul specific paginii de detalii */}
            <main className="main-content">
                <div className="search-container">
                    <form onSubmit={handleSearch} className="search-bar">
                        <div className="input-group">
                            <MapPin className="icon" />
                            <div className="custom-select">
                                <div
                                    className="selected-option"
                                    onClick={toggleLocationDropdown}
                                >
                                    {location}
                                    <ChevronDown className="icon-small" />
                                </div>
                                {isLocationDropdownOpen && (
                                    <div className="options-list">
                                        {locations.map((loc) => (
                                            <div
                                                key={loc}
                                                className="option"
                                                onClick={() => handleLocationChange(loc)}
                                            >
                                                {loc}
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="input-group">
                            <div className="custom-select">
                                <div
                                    className="selected-option"
                                    onClick={toggleActivityDropdown}
                                >
                                    {activity}
                                    <ChevronDown className="icon-small" />
                                </div>
                                {isActivityDropdownOpen && (
                                    <div className="options-list">
                                        {activities.map((act) => (
                                            <div
                                                key={act}
                                                className="option"
                                                onClick={() => handleActivityChange(act)}
                                            >
                                                {act}
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>

                        <input
                            type="text"
                            value={searchQuery}
                            onChange={handleSearchChange}
                            placeholder="Denumire sala de sport"
                            className="search-input"
                        />

                        <button type="submit" className="search-button">
                            CAUTĂ
                        </button>
                    </form>
                </div>
            </main>

            {/* Conținutul paginii de detalii */}
            <div className="detail-page-wrapper">
                <div className="detail-page-container">
                    {/* Buton înapoi */}
                    <div className="detail-back-link">
                        <Link to="/" className="back-link">
                            <ArrowLeft size={16} />
                            Înapoi la lista de săli
                        </Link>
                    </div>

                    {/* Header sala */}
                    <div className="detail-page-header">
                        <h1 className="detail-page-title">{sportsHall.name}</h1>
                        <div className="detail-page-meta">
                            <div className="detail-page-location">
                                <MapPin size={16} />
                                <span>{sportsHall.address}, {sportsHall.city}</span>
                            </div>
                            <div className="detail-page-rating">
                                {renderStars(rating)}
                            </div>
                        </div>
                        {sportsHall.type && <div className="detail-page-badge">{sportsHall.type}</div>}
                    </div>

                    {/* Galerie imagini */}
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

                    {/* Conținut detalii */}
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
                                    </div>
                                </div>

                                <div className="detail-page-section">
                                    <h2 className="detail-section-title">Descriere</h2>
                                    <p className="detail-description">
                                        {sportsHall.description || 'Nu există descriere disponibilă pentru această sală de sport.'}
                                    </p>
                                </div>

                                {/* Harta cu locația sălii */}
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

            {/* Footer */}
            <footer className="app-footer">
                <div className="footer-content">
                    <div className="footer-logo">
                        <div className="footer-logo-icon"></div>
                        <span className="footer-logo-text">Licenta</span>
                    </div>
                    <div className="footer-links">
                        <a href="/terms" className="footer-link">Termeni și condiții</a>
                        <a href="/privacy" className="footer-link">Politica de confidențialitate</a>
                        <a href="/contact" className="footer-link">Contact</a>
                    </div>
                    <div className="footer-copyright">
                        &copy; {new Date().getFullYear()} Platforma de Administrare Săli Sport
                    </div>
                </div>
            </footer>

            {/* Modals */}
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

            {/* Modal pentru opțiuni de rezervare */}
            <ReservationOptionsModal
                isOpen={isReservationModalOpen}
                onClose={closeReservationModal}
                onReserveRemaining={handleReserveRemaining}
            />
        </div>
    );
};

export default SportsHallDetailPage;