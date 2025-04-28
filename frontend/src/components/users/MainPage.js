import React from 'react';
import { Link } from 'react-router-dom';
import { MapPin, ChevronDown, LogOut } from 'lucide-react';
import LoginModal from '../login/LoginModal';
import RegisterModal from '../register/RegisterModal';
import SimpleFeaturedSportsHalls from './FeaturedSportsHalls';
import './MainPage.css';

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
            isLoggedIn: localStorage.getItem('isLoggedIn') === 'true'
        };
    }

    componentDidMount() {
        // Check login status when component mounts
        this.checkLoginStatus();

        // Add event listener for storage changes
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
        // Clear local storage
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userType');
        localStorage.removeItem('userId');
        localStorage.removeItem('jwtToken');

        // Update state
        this.setState({
            isLoggedIn: false
        });

        // Redirect to main page if needed
        window.location.href = '/';
    }

    render() {
        const locations = ['Cluj-Napoca', 'Timisoara', 'Bucuresti', 'Iasi'];
        const activities = ['Fitness', 'Inot', 'Tenis', 'Fotbal', 'Baschet', 'Handbal'];
        const { isLoggedIn } = this.state;

        return (
            <div className="main-container">
                <header className="header">
                    <div className="logo-container">
                        <div className="logo"></div>
                        <span className="logo-text">Licenta</span>
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