import React, { useState, useEffect } from 'react';
import { User, Shield, Settings } from 'lucide-react';
import Header from '../Header';
import PersonalData from './PersonalData';
import ReservationProfiles from './ReservationProfiles';
import SecuritySettings from './SecuritySettings';
import './ProfilePage.css';

const ProfilePage = () => {
    const [activeSection, setActiveSection] = useState('personal');
    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('isLoggedIn') === 'true');
    const [userInfo, setUserInfo] = useState({
        firstName: '',
        lastName: '',
        email: ''
    });

    // Funcție pentru a obține informațiile de bază ale utilizatorului pentru sidebar
    const fetchBasicUserInfo = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                return;
            }

            const response = await fetch(`http://localhost:8080/users/${userId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const userData = await response.json();

                // Separăm numele complet în prenume și nume
                const nameParts = userData.name ? userData.name.split(' ') : ['', ''];
                const firstName = nameParts[0] || '';
                const lastName = nameParts.slice(1).join(' ') || '';

                setUserInfo({
                    firstName: firstName,
                    lastName: lastName,
                    email: userData.email || ''
                });
            }
        } catch (err) {
            console.error('Eroare la încărcarea informațiilor de bază ale utilizatorului:', err);
        }
    };

    useEffect(() => {
        if (isLoggedIn) {
            fetchBasicUserInfo();
        }
    }, [isLoggedIn]);

    const handleLogout = () => {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userType');
        localStorage.removeItem('userId');
        localStorage.removeItem('jwtToken');
        setIsLoggedIn(false);
        window.location.href = '/';
    };

    const renderContent = () => {
        switch(activeSection) {
            case 'personal':
                return <PersonalData />;
            case 'profiles':
                return <ReservationProfiles />;
            case 'security':
                return <SecuritySettings />;
            default:
                return <PersonalData />;
        }
    };

    return (
        <div className="profile-page">
            <Header
                isLoggedIn={isLoggedIn}
                onLoginClick={() => {}}
                onRegisterClick={() => {}}
                onLogout={handleLogout}
            />

            <div className="profile-container">
                <nav className="profile-sidebar">
                    <div className="profile-user-info">
                        <div className="profile-avatar">
                            <User size={32} />
                        </div>
                        <h3>{userInfo.firstName} {userInfo.lastName}</h3>
                        <p>{userInfo.email}</p>
                    </div>

                    <div className="profile-nav">
                        <button
                            className={`profile-nav-item ${activeSection === 'personal' ? 'active' : ''}`}
                            onClick={() => setActiveSection('personal')}
                        >
                            <User size={20} />
                            Date personale
                        </button>

                        <button
                            className={`profile-nav-item ${activeSection === 'profiles' ? 'active' : ''}`}
                            onClick={() => setActiveSection('profiles')}
                        >
                            <Settings size={20} />
                            Profiluri de rezervare
                        </button>

                        <button
                            className={`profile-nav-item ${activeSection === 'security' ? 'active' : ''}`}
                            onClick={() => setActiveSection('security')}
                        >
                            <Shield size={20} />
                            Securitate
                        </button>
                    </div>
                </nav>

                <main className="profile-content">
                    {renderContent()}
                </main>
            </div>
        </div>
    );
};

export default ProfilePage;