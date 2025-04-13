import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import './Admin.css';

import ViewAllHallsPage from './AdminPages/ViewAllHallsPage';
import ApproveRegistrationsPage from './AdminPages/ApproveRegistrationsPage';
import ViewHallsSchedulePage from './AdminPages/ViewHallsSchedulePage';
import ManageUsersPage from './AdminPages/ManageUsersPage';
import SendEmailsPage from './AdminPages/SendEmailsPage';

class Admin extends Component {
    constructor(props) {
        super(props);
        this.state = {
            activeSection: 'view-all-halls'
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
            case 'view-all-halls':
                return <ViewAllHallsPage />;
            case 'approve-registrations':
                return <ApproveRegistrationsPage />;
            case 'view-halls-schedule':
                return <ViewHallsSchedulePage />;
            case 'manage-users':
                return <ManageUsersPage />;
            case 'send-emails':
                return <SendEmailsPage />;
            default:
                return <ViewAllHallsPage />;
        }
    }

    render() {
        const { activeSection } = this.state;
        const currentYear = new Date().getFullYear();

        return (
            <div className="global-admin-container">
                <header className="global-admin-header">
                    <div className="global-admin-logo-container">
                        <Link to="/admin-dashboard" className="global-admin-logo-link">
                            <div className="global-admin-logo"></div>
                            <span className="global-admin-logo-text">TrainOn</span>
                        </Link>
                    </div>
                    <div className="global-admin-auth-buttons">
                        <button onClick={this.handleLogout} className="global-admin-auth-button">
                            Ieșire
                        </button>
                    </div>
                </header>

                <nav className="global-admin-nav">
                    <div className="global-admin-buttons-container">
                        <div
                            className={`global-admin-button-card ${activeSection === 'view-all-halls' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('view-all-halls')}
                        >
                            <div className="global-admin-button-icon view-halls-icon"></div>
                            <h3 className="global-admin-button-title">Vizualizarea tuturor sălilor</h3>
                            <p className="global-admin-button-description">
                                Lista + posibilitatea de gestiune
                            </p>
                        </div>

                        <div
                            className={`global-admin-button-card ${activeSection === 'approve-registrations' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('approve-registrations')}
                        >
                            <div className="global-admin-button-icon approve-reg-icon"></div>
                            <h3 className="global-admin-button-title">Aprobarea cererilor</h3>
                            <p className="global-admin-button-description">
                                Aprobarea cererilor de înregistrare a echipelor
                            </p>
                        </div>

                        <div
                            className={`global-admin-button-card ${activeSection === 'view-halls-schedule' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('view-halls-schedule')}
                        >
                            <div className="global-admin-button-icon schedule-view-icon"></div>
                            <h3 className="global-admin-button-title">Vizualizarea programului sălilor</h3>
                            <p className="global-admin-button-description">
                                Verifică disponibilitatea și programul sălilor
                            </p>
                        </div>

                        <div
                            className={`global-admin-button-card ${activeSection === 'manage-users' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('manage-users')}
                        >
                            <div className="global-admin-button-icon users-icon"></div>
                            <h3 className="global-admin-button-title">Gestiunea utilizatorilor</h3>
                            <p className="global-admin-button-description">
                                Vizualizarea și gestionarea utilizatorilor
                            </p>
                        </div>

                        <div
                            className={`global-admin-button-card ${activeSection === 'send-emails' ? 'active' : ''}`}
                            onClick={() => this.setActiveSection('send-emails')}
                        >
                            <div className="global-admin-button-icon emails-icon"></div>
                            <h3 className="global-admin-button-title">Trimiterea email-urilor</h3>
                            <p className="global-admin-button-description">
                                Trimite email-uri către utilizatori sau administratori
                            </p>
                        </div>
                    </div>
                </nav>

                <main className="global-admin-content">
                    {this.renderContent()}
                </main>

                <footer className="global-admin-footer">
                    <div className="global-admin-footer-content">
                        <div className="global-admin-footer-logo">
                            <div className="global-admin-footer-logo-icon"></div>
                            <span className="global-admin-footer-logo-text">TrainOn</span>
                        </div>
                        <div className="global-admin-footer-links">
                            <a href="/terms" className="global-admin-footer-link">Termeni și condiții</a>
                            <a href="/privacy" className="global-admin-footer-link">Politica de confidențialitate</a>
                            <a href="/contact" className="global-admin-footer-link">Contact</a>
                        </div>
                        <div className="global-admin-footer-copyright">
                            &copy; {currentYear} Platforma de Administrare
                        </div>
                    </div>
                </footer>
            </div>
        );
    }
}

export default Admin;