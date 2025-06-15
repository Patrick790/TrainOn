import React from 'react';
import { Calendar } from 'lucide-react';
import LoginModal from '../login/LoginModal';
import RegisterModal from '../register/RegisterModal';
import Header from './Header';
import SearchBar from './SearchBar'; // Import componenta SearchBar
import SimpleFeaturedSportsHalls from './FeaturedSportsHalls';
import Footer from '../pageComponents/Footer';
import './MainPage.css';

class ReservationOptionsModal extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            userType: null,
            loading: true
        };
    }

    componentDidMount() {
        if (this.props.isOpen) {
            this.fetchUserInfo();
        }
    }

    componentDidUpdate(prevProps) {
        if (this.props.isOpen && !prevProps.isOpen) {
            this.fetchUserInfo();
        }
    }

    fetchUserInfo = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                this.setState({ loading: false });
                return;
            }

            const API_BASE_URL = process.env.REACT_APP_API_URL || '';
            const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const userData = await response.json();
                this.setState({
                    userType: userData.teamType,
                    loading: false
                });
            } else {
                console.error('Failed to fetch user data:', response.status);
                this.setState({ loading: false });
            }
        } catch (error) {
            console.error('Error fetching user data:', error);
            this.setState({ loading: false });
        }
    };

    render() {
        const { isOpen, onClose } = this.props;
        const { userType, loading } = this.state;

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
    }
}

class MainPage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedCity: '',
            isLoginModalOpen: false,
            isRegisterModalOpen: false,
            isReservationModalOpen: false,
            isLoggedIn: localStorage.getItem('isLoggedIn') === 'true'
        };
    }

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

    handleCityChange = (city) => {
        this.setState({ selectedCity: city });
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

    toggleReservationModal = () => {
        if (this.state.isLoggedIn) {
            this.setState(prevState => ({
                isReservationModalOpen: !prevState.isReservationModalOpen
            }));
        } else {
            this.toggleLoginModal();
        }
    }

    closeReservationModal = () => {
        this.setState({ isReservationModalOpen: false });
    }

    render() {
        const { isLoggedIn, selectedCity } = this.state;

        return (
            <div className="main-container">
                <Header
                    isLoggedIn={isLoggedIn}
                    onLoginClick={this.toggleLoginModal}
                    onRegisterClick={this.toggleRegisterModal}
                    onLogout={this.handleLogout}
                />

                <main className="main-content">
                    <SearchBar onCityChange={this.handleCityChange} />
                </main>

                {selectedCity && (
                    <SimpleFeaturedSportsHalls selectedCity={selectedCity} />
                )}

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

                <ReservationOptionsModal
                    isOpen={this.state.isReservationModalOpen}
                    onClose={this.closeReservationModal}
                />

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

                <Footer />
            </div>
        );
    }
}

export default MainPage;