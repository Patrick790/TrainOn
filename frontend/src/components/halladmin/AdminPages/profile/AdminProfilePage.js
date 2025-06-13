import React, { useState, useEffect } from 'react';
import { User, Shield } from 'lucide-react';
import AdminHeader from '../Header';
import AdminPersonalData from './AdminPersonalData';
import AdminSecuritySettings from './AdminSecuritySettings';
import './AdminProfilePage.css';

const AdminProfilePage = () => {
    const [activeSection, setActiveSection] = useState('personal');
    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('isLoggedIn') === 'true');
    const [adminInfo, setAdminInfo] = useState({
        firstName: '',
        lastName: '',
        email: ''
    });
    const API_BASE_URL = process.env.REACT_APP_API_URL || '';


    // Funcție pentru a obține informațiile de bază ale administratorului pentru sidebar
    const fetchBasicAdminInfo = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                return;
            }

            const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
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

                setAdminInfo({
                    firstName: firstName,
                    lastName: lastName,
                    email: userData.email || ''
                });
            }
        } catch (err) {
            console.error('Eroare la încărcarea informațiilor de bază ale administratorului:', err);
        }
    };

    useEffect(() => {
        if (isLoggedIn) {
            fetchBasicAdminInfo();
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
                return <AdminPersonalData />;
            case 'security':
                return <AdminSecuritySettings />;
            default:
                return <AdminPersonalData />;
        }
    };

    return (
        <div className="admin-profile-page">
            <AdminHeader
                isLoggedIn={isLoggedIn}
                onLogout={handleLogout}
            />

            <div className="admin-profile-container">
                <nav className="admin-profile-sidebar">
                    <div className="admin-profile-user-info">
                        <div className="admin-profile-avatar">
                            <User size={32} />
                        </div>
                        <h3>{adminInfo.firstName} {adminInfo.lastName}</h3>
                        <p>{adminInfo.email}</p>
                        <span className="admin-profile-role">Administrator Sală</span>
                    </div>

                    <div className="admin-profile-nav">
                        <button
                            className={`admin-profile-nav-item ${activeSection === 'personal' ? 'active' : ''}`}
                            onClick={() => setActiveSection('personal')}
                        >
                            <User size={20} />
                            Date personale
                        </button>

                        <button
                            className={`admin-profile-nav-item ${activeSection === 'security' ? 'active' : ''}`}
                            onClick={() => setActiveSection('security')}
                        >
                            <Shield size={20} />
                            Securitate
                        </button>
                    </div>
                </nav>

                <main className="admin-profile-content">
                    {renderContent()}
                </main>
            </div>
        </div>
    );
};

export default AdminProfilePage;