import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginModal.css';

const LoginModal = ({ isOpen, onClose, onRegisterClick, onLoginSuccess }) => {
    const API_BASE_URL = process.env.REACT_APP_API_URL || '';

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState({});
    const [errorMessage, setErrorMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [showForgotPassword, setShowForgotPassword] = useState(false);

    const [forgotEmail, setForgotEmail] = useState('');
    const [forgotErrors, setForgotErrors] = useState({});
    const [forgotErrorMessage, setForgotErrorMessage] = useState('');
    const [forgotSuccessMessage, setForgotSuccessMessage] = useState('');
    const [forgotIsLoading, setForgotIsLoading] = useState(false);

    const navigate = useNavigate();

    const modalRef = useRef(null);
    const emailInputRef = useRef(null);
    const forgotEmailInputRef = useRef(null);

    useEffect(() => {
        if (isOpen) {
            if (showForgotPassword && forgotEmailInputRef.current) {
                setTimeout(() => {
                    forgotEmailInputRef.current.focus();
                }, 100);
            } else if (!showForgotPassword && emailInputRef.current) {
                setTimeout(() => {
                    emailInputRef.current.focus();
                }, 100);
            }
        }
    }, [isOpen, showForgotPassword]);

    useEffect(() => {
        if (isOpen) {
            setEmail('');
            setPassword('');
            setErrors({});
            setErrorMessage('');
            setIsLoading(false);
            setShowForgotPassword(false);
            setForgotEmail('');
            setForgotErrors({});
            setForgotErrorMessage('');
            setForgotSuccessMessage('');
            setForgotIsLoading(false);
        }
    }, [isOpen]);

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (!isOpen) return;

            // Escape pentru închiderea modalului
            if (e.key === 'Escape') {
                onClose();
                return;
            }

            if (e.key === 'Tab') {
                const focusableElements = modalRef.current?.querySelectorAll(
                    'input:not([disabled]), button:not([disabled]), [tabindex]:not([tabindex="-1"])'
                );

                if (focusableElements && focusableElements.length > 0) {
                    const firstElement = focusableElements[0];
                    const lastElement = focusableElements[focusableElements.length - 1];

                    if (e.shiftKey) {
                        if (document.activeElement === firstElement) {
                            e.preventDefault();
                            lastElement.focus();
                        }
                    } else {
                        if (document.activeElement === lastElement) {
                            e.preventDefault();
                            firstElement.focus();
                        }
                    }
                }
            }
        };

        if (isOpen) {
            document.addEventListener('keydown', handleKeyDown);
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
            document.body.style.overflow = 'unset';
        };
    }, [isOpen, onClose]);

    const handleInputChange = (setter, field) => (e) => {
        setter(e.target.value);
        setErrors((prevErrors) => ({ ...prevErrors, [field]: false }));
        setErrorMessage("");
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !isLoading) {
            e.preventDefault();
            login();
        }
    };

    async function login() {
        const newErrors = {};
        if (!email) newErrors.email = true;
        if (!password) newErrors.password = true;

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            setErrorMessage("Toate câmpurile sunt obligatorii!");

            if (newErrors.email && emailInputRef.current) {
                emailInputRef.current.focus();
            }
            return;
        }

        setIsLoading(true);
        setErrorMessage('');

        try {
            const loginResponse = await fetch(`${API_BASE_URL}/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });

            if (!loginResponse.ok) {
                if (loginResponse.status === 401) {
                    const errorData = await loginResponse.json();
                    throw new Error(errorData.message || 'Email sau parolă incorectă!');
                } else if (loginResponse.status === 403) {
                    const errorData = await loginResponse.json();
                    throw new Error(errorData.message || 'Contul nu este activ sau este restricționat!');
                } else {
                    throw new Error('Eroare la autentificare. Vă rugăm să încercați din nou.');
                }
            }

            const loginData = await loginResponse.json();

            if (!loginData.success) {
                throw new Error(loginData.message || 'Autentificare eșuată');
            }

            const tokenResponse = await fetch(`${API_BASE_URL}/login/generateToken`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });

            if (!tokenResponse.ok) {
                if (tokenResponse.status === 401) {
                    throw new Error('Email sau parolă incorectă!');
                } else if (tokenResponse.status === 403) {
                    const errorText = await tokenResponse.text();
                    throw new Error(errorText || 'Acces restricționat!');
                } else {
                    throw new Error('Eroare la generarea sesiunii. Vă rugăm să încercați din nou.');
                }
            }

            const token = await tokenResponse.text();

            localStorage.setItem('jwtToken', token);
            localStorage.setItem('userId', loginData.id);
            localStorage.setItem('userType', loginData.userType);
            localStorage.setItem('isLoggedIn', 'true');

            onClose();
            if (onLoginSuccess) {
                onLoginSuccess();
            }

            if (loginData.userType === "admin") {
                navigate('/admin-dashboard', { replace: true });
            } else if (loginData.userType === "hall_admin") {
                navigate('/hall-admin-dashboard', { replace: true });
            } else if (loginData.userType === "user") {
                navigate('/user-home', { replace: true });
            } else {
                window.location.reload();
            }

        } catch (error) {
            console.error('Login error:', error);
            setErrorMessage(error.message);

            if (emailInputRef.current) {
                setTimeout(() => emailInputRef.current.focus(), 100);
            }
        } finally {
            setIsLoading(false);
        }
    }

    const validateEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };

    const handleForgotInputChange = (e) => {
        setForgotEmail(e.target.value);
        setForgotErrors({});
        setForgotErrorMessage('');
        setForgotSuccessMessage('');
    };

    const handleForgotKeyPress = (e) => {
        if (e.key === 'Enter' && !forgotIsLoading) {
            e.preventDefault();
            handleSendResetEmail();
        }
    };

    const handleSendResetEmail = async () => {
        const newErrors = {};

        if (!forgotEmail) {
            newErrors.email = true;
            setForgotErrorMessage('Email-ul este obligatoriu!');
        } else if (!validateEmail(forgotEmail)) {
            newErrors.email = true;
            setForgotErrorMessage('Email-ul nu este valid!');
        }

        if (Object.keys(newErrors).length > 0) {
            setForgotErrors(newErrors);
            return;
        }

        setForgotIsLoading(true);
        setForgotErrorMessage('');

        try {
            const response = await fetch(`${API_BASE_URL}/forgot-password`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email: forgotEmail }),
            });

            if (!response.ok) {
                if (response.status === 404) {
                    throw new Error('Nu există niciun cont cu această adresă de email!');
                } else if (response.status === 400) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Cererea nu este validă!');
                } else if (response.status === 403) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Contul nu poate fi resetat!');
                } else {
                    throw new Error('Eroare la trimiterea email-ului. Vă rugăm să încercați din nou.');
                }
            }

            setForgotSuccessMessage('Un email cu instrucțiuni pentru resetarea parolei a fost trimis la adresa specificată!');
            setForgotEmail('');

            setTimeout(() => {
                setShowForgotPassword(false);
            }, 3000);

        } catch (error) {
            console.error('Forgot password error:', error);
            setForgotErrorMessage(error.message);
        } finally {
            setForgotIsLoading(false);
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!isLoading) {
            login();
        }
    };

    const handleForgotSubmit = (e) => {
        e.preventDefault();
        if (!forgotIsLoading) {
            handleSendResetEmail();
        }
    };

    const handleRegisterClick = (e) => {
        e.preventDefault();
        onClose();
        onRegisterClick();
    };

    const handleForgotPasswordClick = () => {
        setShowForgotPassword(true);
    };

    const handleBackToLogin = () => {
        setShowForgotPassword(false);
    };

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    if (!isOpen) return null;

    return (
        <div
            className="login-modal-overlay"
            onClick={handleOverlayClick}
        >
            <div
                className="login-modal-content"
                ref={modalRef}
                onClick={e => e.stopPropagation()}
            >
                {showForgotPassword ? (
                    <>
                        <h1 className="login-modal-title">RESETARE PAROLĂ</h1>

                        <p className="forgot-password-description">
                            Introdu adresa de email asociată contului tău și îți vom trimite instrucțiuni pentru resetarea parolei.
                        </p>

                        <form onSubmit={handleForgotSubmit} className="login-modal-form">
                            <div className={`login-input-container ${forgotErrors.email ? 'login-input-error' : ''}`}>
                                <input
                                    ref={forgotEmailInputRef}
                                    type="email"
                                    value={forgotEmail}
                                    onChange={handleForgotInputChange}
                                    onKeyPress={handleForgotKeyPress}
                                    className="login-modal-input"
                                    placeholder=" "
                                    required
                                    disabled={forgotIsLoading}
                                    autoComplete="email"
                                />
                                <label className="login-floating-label">E-mail</label>
                            </div>

                            {forgotErrorMessage && (
                                <p className="login-error-message">
                                    {forgotErrorMessage}
                                </p>
                            )}

                            {forgotSuccessMessage && (
                                <p className="login-success-message">
                                    {forgotSuccessMessage}
                                </p>
                            )}

                            <button
                                type="submit"
                                className={`login-submit-button ${forgotIsLoading ? 'loading' : ''}`}
                                disabled={forgotIsLoading}
                            >
                                {forgotIsLoading ? 'Se trimite...' : 'Trimite instrucțiuni'}
                            </button>

                            <button
                                type="button"
                                className="login-create-account-btn"
                                onClick={handleBackToLogin}
                                disabled={forgotIsLoading}
                            >
                                Înapoi la autentificare
                            </button>
                        </form>
                    </>
                ) : (
                    <>
                        <h1 className="login-modal-title">LOGIN</h1>

                        <form onSubmit={handleSubmit} className="login-modal-form">
                            <div className={`login-input-container ${errors.email ? 'login-input-error' : ''}`}>
                                <input
                                    ref={emailInputRef}
                                    type="email"
                                    value={email}
                                    onChange={handleInputChange(setEmail, 'email')}
                                    onKeyPress={handleKeyPress}
                                    className="login-modal-input"
                                    placeholder=" "
                                    required
                                    disabled={isLoading}
                                    autoComplete="email"
                                />
                                <label className="login-floating-label">E-mail</label>
                                {errors.email && (
                                    <span className="sr-only">
                                        Câmpul email este obligatoriu
                                    </span>
                                )}
                            </div>

                            <div className={`login-input-container ${errors.password ? 'login-input-error' : ''}`}>
                                <input
                                    type="password"
                                    value={password}
                                    onChange={handleInputChange(setPassword, 'password')}
                                    onKeyPress={handleKeyPress}
                                    className="login-modal-input"
                                    placeholder=" "
                                    required
                                    disabled={isLoading}
                                    autoComplete="current-password"
                                />
                                <label className="login-floating-label">Parola</label>
                                {errors.password && (
                                    <span className="sr-only">
                                        Câmpul parolă este obligatoriu
                                    </span>
                                )}
                            </div>

                            {errorMessage && (
                                <p className="login-error-message">
                                    {errorMessage}
                                </p>
                            )}

                            <button
                                type="button"
                                className="login-forgot-password-btn"
                                onClick={handleForgotPasswordClick}
                                disabled={isLoading}
                            >
                                Ti-ai uitat parola?
                            </button>

                            <button
                                type="submit"
                                className={`login-submit-button ${isLoading ? 'loading' : ''}`}
                                disabled={isLoading}
                            >
                                {isLoading ? 'Se conectează...' : 'Conectare'}
                            </button>

                            <button
                                type="button"
                                className="login-create-account-btn"
                                onClick={handleRegisterClick}
                                disabled={isLoading}
                            >
                                Nu ai cont? Creeaza-ti un cont nou
                            </button>
                        </form>
                    </>
                )}
            </div>
        </div>
    );
};

export default LoginModal;