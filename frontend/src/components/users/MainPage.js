import React from 'react';
import { Link } from 'react-router-dom';
import { MapPin, ChevronDown, LogOut, Calendar } from 'lucide-react';
import LoginModal from '../login/LoginModal';
import RegisterModal from '../register/RegisterModal';
import SimpleFeaturedSportsHalls from './FeaturedSportsHalls';
import './MainPage.css';

// Adăugăm doar această componentă în fișierul MainPage.js
class ReservationOptionsModal extends React.Component {
    render() {
        const { isOpen, onClose } = this.props;
        if (!isOpen) return null;

        return (
            <div className="reservation-modal-backdrop">
                <div className="reservation-modal-content">
                    <h2 className="reservation-modal-title">Opțiuni de rezervare</h2>
                    <p className="reservation-modal-description">
                        Alege modul în care dorești să faci rezervare
                    </p>

                    <div className="reservation-options">
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
                            <p>Personalizează opțiunile pentru algoritm de prioritizare</p>
                        </div>

                        <div
                            className="reservation-option"
                            onClick={() => { window.location.href = '/reserve-remaining'; onClose(); }}
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
                            <p>Rezervare rapidă pentru locurile disponibile în acest moment</p>
                        </div>
                    </div>

                    <div className="reservation-modal-footer">
                        <button className="reservation-modal-close-button" onClick={onClose}>
                            Anulează
                        </button>
                    </div>
                </div>
            </div>
        );
    }
}

class MainPage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            location: 'Cluj-Napoca', // Presetat pentru Cluj-Napoca
            activity: 'Sport',
            searchQuery: '',
            isLocationDropdownOpen: false,
            isActivityDropdownOpen: false,
            isLoginModalOpen: false,
            isRegisterModalOpen: false,
            isReservationModalOpen: false,  // Adăugăm această stare pentru modalul de rezervare
            isLoggedIn: localStorage.getItem('isLoggedIn') === 'true'
        };
    }

    // Toate metodele existente rămân neschimbate
    componentDidMount() {
        this.checkLoginStatus();
        window.addEventListener('storage', this.checkLoginStatus);
    }

    componentWillUnmount() {
        window.removeEventListener('storage', this.checkLoginStatus);
    }

    checkLoginStatus = () => {
        const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
        this.setState({ isLoggedIn });
    }

    handleLocationChange = (location) => {
        this.setState({
            location: location,
            isLocationDropdownOpen: false
        });
    }

    handleActivityChange = (activity) => {
        this.setState({
            activity: activity,
            isActivityDropdownOpen: false
        });
    }

    toggleLocationDropdown = () => {
        this.setState(prevState => ({
            isLocationDropdownOpen: !prevState.isLocationDropdownOpen,
            isActivityDropdownOpen: false
        }));
    }

    toggleActivityDropdown = () => {
        this.setState(prevState => ({
            isActivityDropdownOpen: !prevState.isActivityDropdownOpen,
            isLocationDropdownOpen: false
        }));
    }

    handleSearchChange = (e) => {
        this.setState({ searchQuery: e.target.value });
    }

    handleSearch = (e) => {
        e.preventDefault();
        const searchParams = new URLSearchParams();
        searchParams.append('city', this.state.location);
        searchParams.append('activity', this.state.activity);
        if (this.state.searchQuery) {
            searchParams.append('query', this.state.searchQuery);
        }
        window.location.href = `/search?${searchParams.toString()}`;
    }

    toggleLoginModal = () => {
        this.setState(prevState => ({
            isLoginModalOpen: !prevState.isLoginModalOpen,
            isRegisterModalOpen: false
        }));
    }

    toggleRegisterModal = () => {
        this.setState(prevState => ({
            isRegisterModalOpen: !prevState.isRegisterModalOpen,
            isLoginModalOpen: false
        }));
    }

    handleLogout = () => {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userType');
        localStorage.removeItem('userId');
        localStorage.removeItem('jwtToken');
        this.setState({
            isLoggedIn: false
        });
        window.location.href = '/';
    }

    // Adăugăm doar aceste două metode
    toggleReservationModal = () => {
        // Deschide modalul doar dacă utilizatorul este autentificat
        if (this.state.isLoggedIn) {
            this.setState(prevState => ({
                isReservationModalOpen: !prevState.isReservationModalOpen
            }));
        } else {
            // Dacă nu este autentificat, deschide modalul de login
            this.toggleLoginModal();
        }
    }

    closeReservationModal = () => {
        this.setState({ isReservationModalOpen: false });
    }

    render() {
        const locations = ['Cluj-Napoca', 'Timisoara', 'Bucuresti', 'Iasi'];
        const activities = ['Sport', 'Fitness', 'Inot', 'Tenis', 'Fotbal', 'Baschet', 'Handbal'];
        const { isLoggedIn } = this.state;

        return (
            <div className="main-container">
                <header className="header">
                    <div className="logo-container">
                        <Link to="/" className="logo-link">
                            <div className="logo"></div>
                            <span className="logo-text">Licenta</span>
                        </Link>
                    </div>
                    <div className="auth-buttons">
                        {isLoggedIn ? (
                            <button onClick={this.handleLogout} className="auth-button logout-button">
                                <LogOut size={16} />
                                Deconectare
                            </button>
                        ) : (
                            <>
                                <button onClick={this.toggleLoginModal} className="auth-button">
                                    Intra in cont
                                </button>
                                <button onClick={this.toggleRegisterModal} className="auth-button">
                                    Inregistrare
                                </button>
                            </>
                        )}
                    </div>
                </header>

                <main className="main-content">
                    <div className="search-container">
                        <form onSubmit={this.handleSearch} className="search-bar">
                            <div className="input-group">
                                <MapPin className="icon" />
                                <div className="custom-select">
                                    <div
                                        className="selected-option"
                                        onClick={this.toggleLocationDropdown}
                                    >
                                        {this.state.location}
                                        <ChevronDown className="icon-small" />
                                    </div>
                                    {this.state.isLocationDropdownOpen && (
                                        <div className="options-list">
                                            {locations.map((location) => (
                                                <div
                                                    key={location}
                                                    className="option"
                                                    onClick={() => this.handleLocationChange(location)}
                                                >
                                                    {location}
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
                                        onClick={this.toggleActivityDropdown}
                                    >
                                        {this.state.activity}
                                        <ChevronDown className="icon-small" />
                                    </div>
                                    {this.state.isActivityDropdownOpen && (
                                        <div className="options-list">
                                            {activities.map((activity) => (
                                                <div
                                                    key={activity}
                                                    className="option"
                                                    onClick={() => this.handleActivityChange(activity)}
                                                >
                                                    {activity}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>

                            <input
                                type="text"
                                value={this.state.searchQuery}
                                onChange={this.handleSearchChange}
                                placeholder="Denumire sala de sport"
                                className="search-input"
                            />

                            <button type="submit" className="search-button">
                                CAUTĂ
                            </button>
                        </form>
                    </div>
                </main>

                {/* Transmitem orașul selectat la componenta SimpleFeaturedSportsHalls */}
                <SimpleFeaturedSportsHalls selectedCity={this.state.location} />

                {/* Adăugăm doar acest div pentru butonul de rezervare */}
                <div className="reservation-cta">
                    <p className="reservation-text">
                        Dorești să efectuezi o rezervare rapidă? Folosește sistemul nostru inteligent
                        pentru a găsi cea mai bună opțiune pentru tine și echipa ta.
                    </p>
                    <button onClick={this.toggleReservationModal} className="reservation-button">
                        <Calendar size={18} />
                        Rezervă acum
                    </button>
                </div>

                {/* Adăugăm modalul */}
                <ReservationOptionsModal
                    isOpen={this.state.isReservationModalOpen}
                    onClose={this.closeReservationModal}
                />

                {/* Only render modals when not logged in */}
                {!isLoggedIn && (
                    <>
                        <LoginModal
                            isOpen={this.state.isLoginModalOpen}
                            onClose={this.toggleLoginModal}
                            onRegisterClick={this.toggleRegisterModal}
                            onLoginSuccess={this.checkLoginStatus}
                        />
                        <RegisterModal
                            isOpen={this.state.isRegisterModalOpen}
                            onClose={this.toggleRegisterModal}
                        />
                    </>
                )}
            </div>
        );
    }
}

export default MainPage;