import React from 'react';
import { Link } from 'react-router-dom';
import { LogOut, User } from 'lucide-react';
import './Header.css';

class Header extends React.Component {
    render() {
        const { isLoggedIn, onLoginClick, onRegisterClick, onLogout } = this.props;

        return (
            <header className="main-header">
                <div className="light-line"></div>
                <div className="light-line"></div>
                <div className="light-line"></div>
                <div className="light-line"></div>

                <div className="main-logo-container">
                    <div className="main-logo">
                        <div className="main-logo-inner">
                            <div className="main-logo-face front"></div>
                            <div className="main-logo-face back"></div>
                            <div className="main-logo-face right"></div>
                            <div className="main-logo-face left"></div>
                            <div className="main-logo-face top"></div>
                            <div className="main-logo-face bottom"></div>
                        </div>
                    </div>
                    <div className="main-logo-text">
                        TrainOn
                        <div className="main-logo-flash"></div>
                        <div className="main-logo-flash"></div>
                        <div className="main-logo-flash"></div>
                        <div className="main-logo-flash"></div>
                        <div className="main-logo-flash"></div>
                        <div className="main-logo-flash"></div>
                    </div>
                </div>

                <div className="main-header-right">
                    {isLoggedIn && (
                        <div className="main-header-user">
                            <div className="main-user-avatar">
                                <div className="main-user-initial">A</div>
                            </div>
                            <div className="main-user-info">
                                <span className="main-user-name">Administrator sala</span>
                            </div>
                        </div>
                    )}

                    <div className="main-auth-buttons">
                        {isLoggedIn ? (
                            <>
                                <Link to="/admin-profile" className="main-auth-button profile-button">
                                    <User size={16} className="button-icon" />
                                    <span className="button-text">Profilul meu</span>
                                </Link>
                                <button onClick={onLogout} className="main-auth-button">
                                    <span className="button-text">Deconectare</span>
                                    <LogOut size={16} className="button-icon" />
                                </button>
                            </>
                        ) : (
                            <>
                                <button onClick={onLoginClick} className="main-auth-button login-button">
                                    <span className="button-text">Intra in cont</span>
                                </button>
                                <button onClick={onRegisterClick} className="main-auth-button register-button">
                                    <span className="button-text">Inregistrare</span>
                                </button>
                            </>
                        )}
                    </div>
                </div>
            </header>
        );
    }
}

export default Header;