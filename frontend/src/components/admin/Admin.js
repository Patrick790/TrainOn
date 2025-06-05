import React, { Component } from 'react';
import './Admin.css';

import AdminHeader from './AdminPages/AdminHeader';
import ViewAllHallsPage from './AdminPages/ViewAllHallsPage';
import ApproveRegistrationsPage from './AdminPages/ApproveRegistrationsPage';
import ViewHallsSchedulePage from './AdminPages/ViewHallsSchedulePage';
import ManageUsersPage from './AdminPages/ManageUsersPage/ManageUsersPage';
import SendEmailsPage from './AdminPages/SendEmailsPage';
import Footer from '../pageComponents/Footer';

class Admin extends Component {
    constructor(props) {
        super(props);
        this.state = {
            activeSection: 'view-all-halls',
            isLoggedIn: localStorage.getItem('isLoggedIn') === 'true'
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

        // Update state
        this.setState({ isLoggedIn: false });

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
        const { activeSection, isLoggedIn } = this.state;

        return (
            <div className="global-admin-container">
                <AdminHeader
                    isLoggedIn={isLoggedIn}
                    onLogout={this.handleLogout}
                />

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

                <Footer />
            </div>
        );
    }
}

export default Admin;