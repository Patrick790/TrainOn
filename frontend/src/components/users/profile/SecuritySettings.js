import React, { useState } from 'react';

const SecuritySettings = () => {
    const [isChangingPassword, setIsChangingPassword] = useState(false);
    const [passwordData, setPasswordData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [errors, setErrors] = useState({});
    const [loading, setLoading] = useState(false);

    const handleChangePassword = () => {
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
            // Get the current user ID from localStorage or context
            const token = localStorage.getItem('jwtToken');
            if (!token) {
                throw new Error('Nu ești autentificat');
            }

            // Decode the token to get user info (you might need to implement this differently based on your auth setup)
            const tokenPayload = JSON.parse(atob(token.split('.')[1]));
            const userEmail = tokenPayload.sub;

            // Find user by email to get the ID
            const usersResponse = await fetch('http://localhost:8080/users', {
                headers: {
                    'Authorization': `Bearer ${token}`,
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
            const response = await fetch(`http://localhost:8080/users/${currentUser.id}/change-password`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
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

            // Success
            alert('Parola a fost schimbată cu succes!');
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
        console.log('Configurează autentificarea în două pași');
    };

    const handleDeleteAccount = () => {
        // TODO: Implementează logica pentru ștergerea contului
        if (window.confirm('Ești sigur că vrei să ștergi permanent contul? Această acțiune nu poate fi anulată.')) {
            console.log('Șterge contul');
            // Aici ar trebui să faci un request către backend pentru ștergerea contului
        }
    };

    return (
        <div className="profile-section">
            <div className="section-header">
                <h2>Securitate</h2>
            </div>

            <div className="security-section">
                <div className="security-item">
                    <div className="security-info">
                        <h3>Schimbă parola</h3>
                        <p>Actualizează-ți parola pentru a menține contul securizat</p>
                    </div>
                    {!isChangingPassword ? (
                        <button
                            className="security-button"
                            onClick={handleChangePassword}
                        >
                            Schimbă parola
                        </button>
                    ) : (
                        <div style={{ width: '100%' }}>
                            <div className="personal-data-grid" style={{ marginTop: '1rem' }}>
                                <div className="data-field">
                                    <label>Parola curentă</label>
                                    <input
                                        type="password"
                                        name="currentPassword"
                                        className={`edit-input ${errors.currentPassword ? 'error' : ''}`}
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

                                <div className="data-field">
                                    <label>Parola nouă</label>
                                    <input
                                        type="password"
                                        name="newPassword"
                                        className={`edit-input ${errors.newPassword ? 'error' : ''}`}
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

                                <div className="data-field" style={{ gridColumn: 'span 2' }}>
                                    <label>Confirmă parola nouă</label>
                                    <input
                                        type="password"
                                        name="confirmPassword"
                                        className={`edit-input ${errors.confirmPassword ? 'error' : ''}`}
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

                            <div className="edit-actions" style={{ marginTop: '1rem' }}>
                                <button
                                    className="save-button"
                                    onClick={handleSavePassword}
                                    disabled={loading}
                                >
                                    {loading ? 'Se salvează...' : 'Salvează parola'}
                                </button>
                                <button
                                    className="cancel-button"
                                    onClick={handleCancelPasswordChange}
                                    disabled={loading}
                                >
                                    Anulează
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                <div className="security-item">
                    <div className="security-info">
                        <h3>Autentificare în două pași</h3>
                        <p>Adaugă un nivel suplimentar de securitate la contul tău</p>
                    </div>
                    <button
                        className="security-button"
                        onClick={handleTwoFactorAuth}
                    >
                        Configurează
                    </button>
                </div>

                <div className="security-item danger">
                    <div className="security-info">
                        <h3>Șterge contul</h3>
                        <p>Șterge permanent contul și toate datele asociate</p>
                    </div>
                    <button
                        className="security-button danger"
                        onClick={handleDeleteAccount}
                    >
                        Șterge contul
                    </button>
                </div>
            </div>
        </div>
    );
};

export default SecuritySettings;