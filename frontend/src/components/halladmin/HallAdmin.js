import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import './HallAdmin.css';
import MyHalls from './AdminPages/MyHallsPage';
import AddHallPage from './AdminPages/AddHallPage';
import ManageSchedulePage from './AdminPages/ManageSchedulePage';
import ViewReservationsPage from './AdminPages/ViewReservationsPage';
import ManageReviewsPage from './AdminPages/ManageReviewsPage';

class HallAdmin extends Component {
    constructor(props) {
        super(props);
        this.state = {
            activeSection: 'my-halls'
        };
    }

    setActiveSection = (section) => {
        this.setState({ activeSection: section });
    }

    handleLogout = () => {
        // Clear localStorage items related to authentication
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('authToken');
        localStorage.removeItem('userId');
        localStorage.removeItem('userType');

        // Redirect to home page
        window.location.href = '/';
    }

    renderContent() {
        const { activeSection } = this.state;

        switch (activeSection) {
            case 'my-halls':
                return <MyHalls />;
            case 'add-hall':
                return <AddHallPage />;
            case 'manage-schedule':
                return <ManageSchedulePage />;
            case 'view-reservations':
                return <ViewReservationsPage />;
            case 'manage-reviews':
                return <ManageReviewsPage />;
            default:
                return <MyHalls />;
        }
    }

    render() {
        const { activeSection } = this.state;
        const currentYear = new Date().getFullYear();

        return (
            <div className="admin-container">
                <header className="header">
                    <div className="logo-container">
                        <Link to="/hall-admin-dashboard" className="logo-link">
                            <div className="logo"></div>
                            <span className="logo-text">Licenta</span>
                        </Link>
                    </div>
                    <div className="auth-buttons">
                        <button onClick={this.handleLogout} className="auth-button">
                            Ieșire
                        </button>
                    </div>
                </header>

                <nav className="admin-nav">
                    <div className="admin-buttons-container">
                        <div
                            className={`admin-button-card ${activeSection === 'my-halls' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('my-halls')}
                        >
                            <div className="admin-button-icon my-halls-icon"></div>
                            <h3 className="admin-button-title">Sălile Mele</h3>
                            <p className="admin-button-description">
                                Vezi și gestionează sălile tale de sport
                            </p>
                        </div>

                        <div
                            className={`admin-button-card ${activeSection === 'add-hall' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('add-hall')}
                        >
                            <div className="admin-button-icon add-icon"></div>
                            <h3 className="admin-button-title">Adaugă Sală Nouă</h3>
                            <p className="admin-button-description">
                                Adaugă o sală de sport nouă și configurează detaliile
                            </p>
                        </div>

                        <div
                            className={`admin-button-card ${activeSection === 'manage-schedule' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('manage-schedule')}
                        >
                            <div className="admin-button-icon schedule-icon"></div>
                            <h3 className="admin-button-title">Gestionare Program</h3>
                            <p className="admin-button-description">
                                Configurează orele disponibile și zilele de funcționare
                            </p>
                        </div>

                        <div
                            className={`admin-button-card ${activeSection === 'view-reservations' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('view-reservations')}
                        >
                            <div className="admin-button-icon reservations-icon"></div>
                            <h3 className="admin-button-title">Vizualizare Rezervări</h3>
                            <p className="admin-button-description">
                                Consultă toate rezervările și gestionează cererile
                            </p>
                        </div>

                        <div
                            className={`admin-button-card ${activeSection === 'manage-reviews' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('manage-reviews')}
                        >
                            <div className="admin-button-icon reviews-icon"></div>
                            <h3 className="admin-button-title">Gestionare Recenzii</h3>
                            <p className="admin-button-description">
                                Vizualizează și răspunde la recenziile lăsate de utilizatori
                            </p>
                        </div>
                    </div>
                </nav>

                <main className="admin-content">
                    {this.renderContent()}
                </main>

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
                            &copy; {currentYear} Platforma de Administrare Săli Sport
                        </div>
                    </div>
                </footer>
            </div>
        );
    }
}

export default HallAdmin;