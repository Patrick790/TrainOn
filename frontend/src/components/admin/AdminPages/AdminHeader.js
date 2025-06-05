import React from 'react';
import { Link } from 'react-router-dom';
import { LogOut, User } from 'lucide-react';
import './AdminHeader.css';

class AdminHeader extends React.Component {
    render() {
        const { isLoggedIn, onLogout } = this.props;

        return (
            <header className="admin-main-header">
                {/* Linii de lumină animate pentru fundal */}
                <div className="light-line"></div>
                <div className="light-line"></div>
                <div className="light-line"></div>
                <div className="light-line"></div>

                <div className="admin-main-logo-container">
                    <Link to="/admin-dashboard" className="admin-main-logo-link">
                        <div className="admin-main-logo">
                            <div className="admin-main-logo-inner">
                                <div className="admin-main-logo-face front"></div>
                                <div className="admin-main-logo-face back"></div>
                                <div className="admin-main-logo-face right"></div>
                                <div className="admin-main-logo-face left"></div>
                                <div className="admin-main-logo-face top"></div>
                                <div className="admin-main-logo-face bottom"></div>
                            </div>
                        </div>
                        <div className="admin-main-logo-text">
                            TrainOn
                            <div className="admin-main-logo-flash"></div>
                            <div className="admin-main-logo-flash"></div>
                            <div className="admin-main-logo-flash"></div>
                            <div className="admin-main-logo-flash"></div>
                            <div className="admin-main-logo-flash"></div>
                            <div className="admin-main-logo-flash"></div>
                        </div>
                    </Link>
                </div>

                <div className="admin-main-header-right">
                    {isLoggedIn && (
                        <div className="admin-main-header-user">
                            <div className="admin-main-user-avatar">
                                <div className="admin-main-user-initial">A</div>
                            </div>
                            <div className="admin-main-user-info">
                                <span className="admin-main-user-name">Administrator aplicație</span>
                            </div>
                        </div>
                    )}

                    <div className="admin-main-auth-buttons">
                        {isLoggedIn ? (
                            <>
                                <Link to="/app-admin-profile" className="admin-main-auth-button profile-button">
                                    <User size={16} className="button-icon" />
                                    <span className="button-text">Profilul meu</span>
                                </Link>
                                <button onClick={onLogout} className="admin-main-auth-button">
                                    <span className="button-text">Deconectare</span>
                                    <LogOut size={16} className="button-icon" />
                                </button>
                            </>
                        ) : (
                            <>
                                <button className="admin-main-auth-button login-button">
                                    <span className="button-text">Autentificare</span>
                                </button>
                            </>
                        )}
                    </div>
                </div>
            </header>
        );
    }
}

export default AdminHeader;