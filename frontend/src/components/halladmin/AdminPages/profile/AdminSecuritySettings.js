import React, { useState } from 'react';

const AdminSecuritySettings = ({ userId }) => {
    const [isChangingPassword, setIsChangingPassword] = useState(false);
    const [passwordData, setPasswordData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [passwordErrors, setPasswordErrors] = useState({});
    const [isLoading, setIsLoading] = useState(false);

    const handleChangePassword = () => {
        console.log('Butonul Schimbă parola a fost apăsat!');
        console.log('Starea anterioară isChangingPassword:', isChangingPassword);
        setIsChangingPassword(true);
        setPasswordData({
            currentPassword: '',
            newPassword: '',
            confirmPassword: ''
        });
        setPasswordErrors({});
        console.log('Starea setată la isChangingPassword: true');
    };

    const handleCancelPasswordChange = () => {
        setIsChangingPassword(false);
        setPasswordData({
            currentPassword: '',
            newPassword: '',
            confirmPassword: ''
        });
        setPasswordErrors({});
    };

    const handlePasswordInputChange = (field, value) => {
        setPasswordData(prev => ({
            ...prev,
            [field]: value
        }));

        // Clear specific error when user starts typing
        if (passwordErrors[field]) {
            setPasswordErrors(prev => ({
                ...prev,
                [field]: ''
            }));
        }
    };

    const validatePasswordForm = () => {
        const errors = {};

        if (!passwordData.currentPassword) {
            errors.currentPassword = 'Parola curentă este obligatorie';
        }

        if (!passwordData.newPassword) {
            errors.newPassword = 'Parola nouă este obligatorie';
        } else if (passwordData.newPassword.length < 6) {
            errors.newPassword = 'Parola nouă trebuie să aibă cel puțin 6 caractere';
        }

        if (!passwordData.confirmPassword) {
            errors.confirmPassword = 'Confirmarea parolei este obligatorie';
        } else if (passwordData.newPassword !== passwordData.confirmPassword) {
            errors.confirmPassword = 'Parolele nu se potrivesc';
        }

        if (passwordData.currentPassword === passwordData.newPassword) {
            errors.newPassword = 'Parola nouă trebuie să fie diferită de cea curentă';
        }

        setPasswordErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSubmitPasswordChange = async () => {
        if (!validatePasswordForm()) {
            return;
        }

        setIsLoading(true);

        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`http://localhost:8080/users/${userId}/change-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    currentPassword: passwordData.currentPassword,
                    newPassword: passwordData.newPassword
                })
            });

            const data = await response.json();

            if (response.ok) {
                alert('Parola a fost schimbată cu succes!');
                setIsChangingPassword(false);
                setPasswordData({
                    currentPassword: '',
                    newPassword: '',
                    confirmPassword: ''
                });
                setPasswordErrors({});
            } else {
                // Handle specific error from backend
                if (data.message) {
                    if (data.message.includes('incorectă')) {
                        setPasswordErrors({ currentPassword: data.message });
                    } else {
                        setPasswordErrors({ general: data.message });
                    }
                } else {
                    setPasswordErrors({ general: 'A apărut o eroare la schimbarea parolei' });
                }
            }
        } catch (error) {
            console.error('Error changing password:', error);
            setPasswordErrors({ general: 'A apărut o eroare de conectare' });
        } finally {
            setIsLoading(false);
        }
    };

    const handleTwoFactorAuth = () => {
        // TODO: Implementează logica pentru autentificarea în două pași
        console.log('Configurează autentificarea în două pași pentru administrator');
    };

    const handleDeleteAccount = () => {
        // TODO: Implementează logica pentru ștergerea contului de administrator
        if (window.confirm('Ești sigur că vrei să ștergi permanent contul de administrator? Această acțiune nu poate fi anulată și va afecta toate sălile administrate.')) {
            console.log('Șterge contul de administrator');
            // Aici ar trebui să faci un request către backend pentru ștergerea contului
        }
    };

    return (
        <div className="admin-profile-section">
            <div className="admin-section-header">
                <h2>Securitate</h2>
            </div>

            <div className="admin-security-section">
                <div className="admin-security-item">
                    <div className="admin-security-info">
                        <h3>Schimbă parola</h3>
                        <p>Actualizează-ți parola pentru a menține contul de administrator securizat</p>
                    </div>

                    {!isChangingPassword ? (
                        <button
                            className="admin-security-button"
                            onClick={handleChangePassword}
                        >
                            Schimbă parola
                        </button>
                    ) : (
                        <div style={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '1rem',
                            width: '100%',
                            maxWidth: '400px',
                            padding: '1.5rem',
                            background: 'rgba(0, 0, 0, 0.05)',
                            backdropFilter: 'blur(3px)',
                            borderRadius: '8px',
                            border: '1px solid rgba(255, 255, 255, 0.1)'
                        }}>
                            {passwordErrors.general && (
                                <div style={{
                                    color: '#ef4444',
                                    fontSize: '0.9rem',
                                    marginBottom: '1rem',
                                    padding: '0.5rem',
                                    background: 'rgba(239, 68, 68, 0.1)',
                                    borderRadius: '4px',
                                    border: '1px solid rgba(239, 68, 68, 0.3)',
                                    textAlign: 'center',
                                    textShadow: '0 1px 3px rgba(0, 0, 0, 0.5)'
                                }}>
                                    {passwordErrors.general}
                                </div>
                            )}

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                <label style={{
                                    fontSize: '0.9rem',
                                    fontWeight: '500',
                                    color: 'rgba(255, 255, 255, 0.95)',
                                    textShadow: '0 2px 6px rgba(0, 0, 0, 0.6)'
                                }}>Parola curentă</label>
                                <input
                                    type="password"
                                    className="admin-edit-input"
                                    value={passwordData.currentPassword}
                                    onChange={(e) => handlePasswordInputChange('currentPassword', e.target.value)}
                                    placeholder="Introdu parola curentă"
                                    disabled={isLoading}
                                />
                                {passwordErrors.currentPassword && (
                                    <span style={{
                                        color: '#ef4444',
                                        fontSize: '0.8rem',
                                        display: 'block',
                                        marginTop: '0.25rem',
                                        textShadow: '0 1px 3px rgba(0, 0, 0, 0.5)'
                                    }}>
                                        {passwordErrors.currentPassword}
                                    </span>
                                )}
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                <label style={{
                                    fontSize: '0.9rem',
                                    fontWeight: '500',
                                    color: 'rgba(255, 255, 255, 0.95)',
                                    textShadow: '0 2px 6px rgba(0, 0, 0, 0.6)'
                                }}>Parola nouă</label>
                                <input
                                    type="password"
                                    className="admin-edit-input"
                                    value={passwordData.newPassword}
                                    onChange={(e) => handlePasswordInputChange('newPassword', e.target.value)}
                                    placeholder="Introdu parola nouă (min. 6 caractere)"
                                    disabled={isLoading}
                                />
                                {passwordErrors.newPassword && (
                                    <span style={{
                                        color: '#ef4444',
                                        fontSize: '0.8rem',
                                        display: 'block',
                                        marginTop: '0.25rem',
                                        textShadow: '0 1px 3px rgba(0, 0, 0, 0.5)'
                                    }}>
                                        {passwordErrors.newPassword}
                                    </span>
                                )}
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                <label style={{
                                    fontSize: '0.9rem',
                                    fontWeight: '500',
                                    color: 'rgba(255, 255, 255, 0.95)',
                                    textShadow: '0 2px 6px rgba(0, 0, 0, 0.6)'
                                }}>Confirmă parola nouă</label>
                                <input
                                    type="password"
                                    className="admin-edit-input"
                                    value={passwordData.confirmPassword}
                                    onChange={(e) => handlePasswordInputChange('confirmPassword', e.target.value)}
                                    placeholder="Confirmă parola nouă"
                                    disabled={isLoading}
                                />
                                {passwordErrors.confirmPassword && (
                                    <span style={{
                                        color: '#ef4444',
                                        fontSize: '0.8rem',
                                        display: 'block',
                                        marginTop: '0.25rem',
                                        textShadow: '0 1px 3px rgba(0, 0, 0, 0.5)'
                                    }}>
                                        {passwordErrors.confirmPassword}
                                    </span>
                                )}
                            </div>

                            <div className="admin-edit-actions" style={{ marginTop: '1rem' }}>
                                <button
                                    className="admin-save-button"
                                    onClick={handleSubmitPasswordChange}
                                    disabled={isLoading}
                                    style={isLoading ? { opacity: 0.6, cursor: 'not-allowed' } : {}}
                                >
                                    {isLoading ? 'Se salvează...' : 'Salvează parola'}
                                </button>
                                <button
                                    className="admin-cancel-button"
                                    onClick={handleCancelPasswordChange}
                                    disabled={isLoading}
                                    style={isLoading ? { opacity: 0.6, cursor: 'not-allowed' } : {}}
                                >
                                    Anulează
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                <div className="admin-security-item">
                    <div className="admin-security-info">
                        <h3>Autentificare în două pași</h3>
                        <p>Adaugă un nivel suplimentar de securitate la contul tău de administrator</p>
                    </div>
                    <button
                        className="admin-security-button"
                        onClick={handleTwoFactorAuth}
                    >
                        Configurează
                    </button>
                </div>

                <div className="admin-security-item danger">
                    <div className="admin-security-info">
                        <h3>Șterge contul de administrator</h3>
                        <p>Șterge permanent contul și toate datele asociate, inclusiv sălile administrate</p>
                    </div>
                    <button
                        className="admin-security-button danger"
                        onClick={handleDeleteAccount}
                    >
                        Șterge contul
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AdminSecuritySettings;