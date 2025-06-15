import React, { useState } from 'react';

const AppAdminSecuritySettings = () => {
    const [isChangingPassword, setIsChangingPassword] = useState(false);
    const [passwordData, setPasswordData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [errors, setErrors] = useState({});
    const [loading, setLoading] = useState(false);
    const API_BASE_URL = process.env.REACT_APP_API_URL || '';


    const handleChangePassword = () => {
        console.log('Butonul schimbă parola a fost apăsat!'); // Debug
        setIsChangingPassword(true);
        setPasswordData({
            currentPassword: '',
            newPassword: '',
            confirmPassword: ''
        });
        setErrors({});
    };

    const handlePasswordInputChange = (e) => {
        const { name, value } = e.target;
        setPasswordData(prev => ({
            ...prev,
            [name]: value
        }));

        // Clear error for this field when user starts typing
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const validatePasswordForm = () => {
        const newErrors = {};

        if (!passwordData.currentPassword) {
            newErrors.currentPassword = 'Parola curentă este obligatorie';
        }

        if (!passwordData.newPassword) {
            newErrors.newPassword = 'Parola nouă este obligatorie';
        } else if (passwordData.newPassword.length < 6) {
            newErrors.newPassword = 'Parola nouă trebuie să aibă cel puțin 6 caractere';
        }

        if (!passwordData.confirmPassword) {
            newErrors.confirmPassword = 'Confirmarea parolei este obligatorie';
        } else if (passwordData.newPassword !== passwordData.confirmPassword) {
            newErrors.confirmPassword = 'Parolele nu se potrivesc';
        }

        if (passwordData.currentPassword === passwordData.newPassword) {
            newErrors.newPassword = 'Parola nouă trebuie să fie diferită de cea curentă';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSavePassword = async () => {
        if (!validatePasswordForm()) {
            return;
        }

        setLoading(true);

        try {
            // Get the JWT token from localStorage
            const jwtToken = localStorage.getItem('token');
            if (!jwtToken) {
                throw new Error('Nu ești autentificat');
            }

            // Decode the token to get user info
            const tokenPayload = JSON.parse(atob(jwtToken.split('.')[1]));
            const userEmail = tokenPayload.sub;

            // Find user by email to get the ID
            const usersResponse = await fetch(`${API_BASE_URL}/users`, {
                headers: {
                    'Authorization': `Bearer ${jwtToken}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!usersResponse.ok) {
                throw new Error('Eroare la obținerea datelor utilizatorului');
            }

            const users = await usersResponse.json();
            const currentUser = users.find(user => user.email === userEmail);

            if (!currentUser) {
                throw new Error('Utilizatorul nu a fost găsit');
            }

            // Call the change password endpoint
            const response = await fetch(`${API_BASE_URL}/users/${currentUser.id}/change-password`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${jwtToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    currentPassword: passwordData.currentPassword,
                    newPassword: passwordData.newPassword
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Eroare la schimbarea parolei');
            }

            alert('Parola administratorului aplicației a fost schimbată cu succes!');
            setIsChangingPassword(false);
            setPasswordData({
                currentPassword: '',
                newPassword: '',
                confirmPassword: ''
            });

        } catch (error) {
            console.error('Eroare la schimbarea parolei:', error);
            setErrors({
                general: error.message || 'A apărut o eroare la schimbarea parolei'
            });
        } finally {
            setLoading(false);
        }
    };

    const handleCancelPasswordChange = () => {
        setIsChangingPassword(false);
        setPasswordData({
            currentPassword: '',
            newPassword: '',
            confirmPassword: ''
        });
        setErrors({});
    };

    const handleTwoFactorAuth = () => {
        // TODO: Implementează logica pentru autentificarea în două pași
        console.log('Configurează autentificarea în două pași pentru administratorul aplicației');
    };

    const handleDeleteAccount = () => {
        // TODO: Implementează logica pentru ștergerea contului de administrator aplicație
        if (window.confirm('Ești sigur că vrei să ștergi permanent contul de administrator aplicație? Această acțiune nu poate fi anulată și va afecta întreaga platformă.')) {
            console.log('Șterge contul de administrator aplicație');
            // Aici ar trebui să faci un request către backend pentru ștergerea contului
        }
    };

    console.log('AppAdminSecuritySettings render - isChangingPassword:', isChangingPassword); // Debug

    return (
        <div className="app-admin-profile-section">
            <div className="app-admin-section-header">
                <h2>Securitate</h2>
            </div>

            <div className="app-admin-security-section">
                <div className="app-admin-security-item">
                    <div className="app-admin-security-info">
                        <h3>Schimbă parola</h3>
                        <p>Actualizează-ți parola pentru a menține contul de administrator aplicație securizat</p>
                    </div>
                    {!isChangingPassword ? (
                        <button
                            className="app-admin-security-button"
                            onClick={handleChangePassword}
                            style={{ cursor: 'pointer' }}
                        >
                            Schimbă parola
                        </button>
                    ) : (
                        <div style={{ width: '100%' }}>
                            <div className="app-admin-personal-data-grid" style={{ marginTop: '1rem' }}>
                                <div className="app-admin-data-field">
                                    <label>Parola curentă</label>
                                    <input
                                        type="password"
                                        name="currentPassword"
                                        className={`app-admin-edit-input ${errors.currentPassword ? 'error' : ''}`}
                                        value={passwordData.currentPassword}
                                        onChange={handlePasswordInputChange}
                                        placeholder="Introdu parola curentă"
                                    />
                                    {errors.currentPassword && (
                                        <span style={{ color: '#ef4444', fontSize: '0.8rem' }}>
                                            {errors.currentPassword}
                                        </span>
                                    )}
                                </div>

                                <div className="app-admin-data-field">
                                    <label>Parola nouă</label>
                                    <input
                                        type="password"
                                        name="newPassword"
                                        className={`app-admin-edit-input ${errors.newPassword ? 'error' : ''}`}
                                        value={passwordData.newPassword}
                                        onChange={handlePasswordInputChange}
                                        placeholder="Introdu parola nouă"
                                    />
                                    {errors.newPassword && (
                                        <span style={{ color: '#ef4444', fontSize: '0.8rem' }}>
                                            {errors.newPassword}
                                        </span>
                                    )}
                                </div>

                                <div className="app-admin-data-field" style={{ gridColumn: 'span 2' }}>
                                    <label>Confirmă parola nouă</label>
                                    <input
                                        type="password"
                                        name="confirmPassword"
                                        className={`app-admin-edit-input ${errors.confirmPassword ? 'error' : ''}`}
                                        value={passwordData.confirmPassword}
                                        onChange={handlePasswordInputChange}
                                        placeholder="Confirmă parola nouă"
                                    />
                                    {errors.confirmPassword && (
                                        <span style={{ color: '#ef4444', fontSize: '0.8rem' }}>
                                            {errors.confirmPassword}
                                        </span>
                                    )}
                                </div>
                            </div>

                            {errors.general && (
                                <div style={{
                                    color: '#ef4444',
                                    fontSize: '0.9rem',
                                    marginTop: '1rem',
                                    textAlign: 'center'
                                }}>
                                    {errors.general}
                                </div>
                            )}

                            <div className="app-admin-edit-actions" style={{ marginTop: '1rem' }}>
                                <button
                                    className="app-admin-save-button"
                                    onClick={handleSavePassword}
                                    disabled={loading}
                                >
                                    {loading ? 'Se salvează...' : 'Salvează parola'}
                                </button>
                                <button
                                    className="app-admin-cancel-button"
                                    onClick={handleCancelPasswordChange}
                                    disabled={loading}
                                >
                                    Anulează
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                <div className="app-admin-security-item">
                    <div className="app-admin-security-info">
                        <h3>Autentificare în două pași</h3>
                        <p>Adaugă un nivel suplimentar de securitate la contul tău de administrator aplicație</p>
                    </div>
                    <button
                        className="app-admin-security-button"
                        onClick={handleTwoFactorAuth}
                    >
                        Configurează
                    </button>
                </div>

                <div className="app-admin-security-item danger">
                    <div className="app-admin-security-info">
                        <h3>Șterge contul de administrator aplicație</h3>
                        <p>Șterge permanent contul și toate datele asociate, inclusiv configurările platformei</p>
                    </div>
                    <button
                        className="app-admin-security-button danger"
                        onClick={handleDeleteAccount}
                    >
                        Șterge contul
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AppAdminSecuritySettings;