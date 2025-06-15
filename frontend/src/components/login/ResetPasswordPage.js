import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import './ResetPasswordPage.css';

const ResetPasswordPage = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get('token');
    const API_BASE_URL = process.env.REACT_APP_API_URL || '';


    const [formData, setFormData] = useState({
        newPassword: '',
        confirmPassword: ''
    });
    const [errors, setErrors] = useState({});
    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [tokenValidating, setTokenValidating] = useState(true);
    const [tokenValid, setTokenValid] = useState(false);
    const [userEmail, setUserEmail] = useState('');
    const [userName, setUserName] = useState('');

    const newPasswordRef = useRef(null);

    // Validează token-ul când componenta se încarcă
    useEffect(() => {
        if (!token) {
            setErrorMessage('Link-ul de resetare este invalid sau lipsește.');
            setTokenValidating(false);
            return;
        }

        validateToken();
    }, [token]);

    useEffect(() => {
        if (tokenValid && newPasswordRef.current) {
            setTimeout(() => {
                newPasswordRef.current.focus();
            }, 100);
        }
    }, [tokenValid]);

    const validateToken = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/reset-password/validate?token=${token}`);
            const data = await response.json();

            if (response.ok && data.valid) {
                setTokenValid(true);
                setUserEmail(data.email);
                setUserName(data.userName);
            } else {
                setErrorMessage(data.message || 'Link-ul de resetare este invalid sau a expirat.');
                setTokenValid(false);
            }
        } catch (error) {
            setErrorMessage('Eroare la validarea link-ului. Te rugăm să încerci din nou.');
            setTokenValid(false);
        } finally {
            setTokenValidating(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        setErrors(prev => ({ ...prev, [name]: false }));
        setErrorMessage('');
        setSuccessMessage('');
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !isLoading) {
            e.preventDefault();
            handleResetPassword();
        }
    };

    const validatePasswords = () => {
        const newErrors = {};

        if (!formData.newPassword) {
            newErrors.newPassword = true;
            setErrorMessage('Parola nouă este obligatorie!');
            return false;
        }

        if (formData.newPassword.length < 6) {
            newErrors.newPassword = true;
            setErrorMessage('Parola nouă trebuie să aibă cel puțin 6 caractere!');
            return false;
        }

        if (formData.newPassword.length > 128) {
            newErrors.newPassword = true;
            setErrorMessage('Parola nouă este prea lungă (maxim 128 caractere)!');
            return false;
        }

        if (!formData.confirmPassword) {
            newErrors.confirmPassword = true;
            setErrorMessage('Confirmarea parolei este obligatorie!');
            return false;
        }

        if (formData.newPassword !== formData.confirmPassword) {
            newErrors.confirmPassword = true;
            setErrorMessage('Parolele nu se potrivesc!');
            return false;
        }

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            return false;
        }

        return true;
    };

    const handleResetPassword = async () => {
        if (!validatePasswords()) {
            return;
        }

        setIsLoading(true);
        setErrorMessage('');

        try {
            const response = await fetch(`${API_BASE_URL}/reset-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    token: token,
                    newPassword: formData.newPassword,
                    confirmPassword: formData.confirmPassword
                }),
            });

            const data = await response.json();

            if (response.ok) {
                setSuccessMessage(data.message);
                setFormData({ newPassword: '', confirmPassword: '' });

                // Redirecționează către pagina principală după 3 secunde
                setTimeout(() => {
                    navigate('/user-home', { replace: true });
                }, 3000);
            } else {
                throw new Error(data.message || 'Eroare la resetarea parolei');
            }

        } catch (error) {
            console.error('Reset password error:', error);
            setErrorMessage(error.message);
        } finally {
            setIsLoading(false);
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!isLoading) {
            handleResetPassword();
        }
    };

    const handleBackToLogin = () => {
        navigate('/user-home', { replace: true });
    };

    // Loading state pentru validarea token-ului
    if (tokenValidating) {
        return (
            <div className="reset-password-page">
                <div className="reset-password-container">
                    <div className="reset-password-card">
                        <div className="loading-spinner"></div>
                        <p>Validăm link-ul de resetare...</p>
                    </div>
                </div>
            </div>
        );
    }

    if (!tokenValid) {
        return (
            <div className="reset-password-page">
                <div className="reset-password-container">
                    <div className="reset-password-card">
                        <h1 className="reset-password-title">Link Invalid</h1>
                        <div className="error-icon">⚠️</div>
                        <p className="reset-password-error">{errorMessage}</p>
                        <p className="reset-password-description">
                            Te rugăm să soliciți din nou resetarea parolei sau să verifici că ai accesat link-ul corect din email.
                        </p>
                        <button
                            type="button"
                            className="reset-password-button"
                            onClick={handleBackToLogin}
                        >
                            Înapoi la pagina principală
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // formular de resetare parola
    return (
        <div className="reset-password-page">
            <div className="reset-password-container">
                <div className="reset-password-card">
                    <h1 className="reset-password-title">Resetare Parolă</h1>

                    <div className="user-info">
                        <p>Salut <strong>{userName}</strong>!</p>
                        <p className="user-email">Resetezi parola pentru: {userEmail}</p>
                    </div>

                    <form onSubmit={handleSubmit} className="reset-password-form" noValidate>
                        <div className={`reset-password-input-container ${errors.newPassword ? 'reset-password-input-error' : ''}`}>
                            <input
                                ref={newPasswordRef}
                                type="password"
                                name="newPassword"
                                value={formData.newPassword}
                                onChange={handleInputChange}
                                onKeyPress={handleKeyPress}
                                className="reset-password-input"
                                placeholder=" "
                                required
                                disabled={isLoading}
                                autoComplete="new-password"
                            />
                            <label className="reset-password-floating-label">Parola nouă</label>
                        </div>

                        <div className={`reset-password-input-container ${errors.confirmPassword ? 'reset-password-input-error' : ''}`}>
                            <input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleInputChange}
                                onKeyPress={handleKeyPress}
                                className="reset-password-input"
                                placeholder=" "
                                required
                                disabled={isLoading}
                                autoComplete="new-password"
                            />
                            <label className="reset-password-floating-label">Confirmă parola nouă</label>
                        </div>

                        <div className="password-requirements">
                            <p>Parola trebuie să aibă:</p>
                            <ul>
                                <li className={formData.newPassword.length >= 6 ? 'valid' : ''}>
                                    Cel puțin 6 caractere
                                </li>
                                <li className={formData.newPassword === formData.confirmPassword && formData.confirmPassword ? 'valid' : ''}>
                                    Parolele să se potrivească
                                </li>
                            </ul>
                        </div>

                        {errorMessage && (
                            <p className="reset-password-error-message" role="alert">
                                {errorMessage}
                            </p>
                        )}

                        {successMessage && (
                            <div className="reset-password-success-message" role="alert">
                                <div className="success-icon">✅</div>
                                <p>{successMessage}</p>
                                <p className="redirect-message">Vei fi redirecționat către pagina principală...</p>
                            </div>
                        )}

                        <button
                            type="submit"
                            className={`reset-password-button ${isLoading ? 'loading' : ''}`}
                            disabled={isLoading}
                        >
                            {isLoading ? 'Se resetează...' : 'Resetează parola'}
                        </button>

                        <button
                            type="button"
                            className="reset-password-back-btn"
                            onClick={handleBackToLogin}
                            disabled={isLoading}
                        >
                            Înapoi la pagina principală
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default ResetPasswordPage;