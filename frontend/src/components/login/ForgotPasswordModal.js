import React, { useState, useRef, useEffect } from 'react';
import './ForgotPasswordModal.css';

const ForgotPasswordModal = ({ isOpen, onClose }) => {
    const [email, setEmail] = useState('');
    const [errors, setErrors] = useState({});
    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    // Refs pentru focus management
    const modalRef = useRef(null);
    const emailInputRef = useRef(null);

    // Focus pe input când modalul se deschide
    useEffect(() => {
        if (isOpen && emailInputRef.current) {
            setTimeout(() => {
                emailInputRef.current.focus();
            }, 100);
        }
    }, [isOpen]);

    // Gestionarea tastelor pentru accesibilitate
    useEffect(() => {
        const handleKeyDown = (e) => {
            if (!isOpen) return;

            if (e.key === 'Escape') {
                onClose();
                return;
            }

            // Tab trapping pentru focus
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

    // Reset form când modalul se deschide
    useEffect(() => {
        if (isOpen) {
            setEmail('');
            setErrors({});
            setErrorMessage('');
            setSuccessMessage('');
            setIsLoading(false);
        }
    }, [isOpen]);

    const validateEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };

    const handleInputChange = (e) => {
        setEmail(e.target.value);
        setErrors({});
        setErrorMessage('');
        setSuccessMessage('');
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !isLoading) {
            e.preventDefault();
            handleForgotPassword();
        }
    };

    const handleForgotPassword = async () => {
        const newErrors = {};

        if (!email) {
            newErrors.email = true;
            setErrorMessage('Email-ul este obligatoriu!');
        } else if (!validateEmail(email)) {
            newErrors.email = true;
            setErrorMessage('Email-ul nu este valid!');
        }

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            if (emailInputRef.current) {
                emailInputRef.current.focus();
            }
            return;
        }

        setIsLoading(true);
        setErrorMessage('');

        try {
            const response = await fetch('http://localhost:8080/forgot-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email }),
            });

            if (!response.ok) {
                if (response.status === 404) {
                    throw new Error('Nu există niciun cont cu această adresă de email!');
                } else if (response.status === 400) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Cererea nu este validă!');
                } else {
                    throw new Error('Eroare la trimiterea email-ului. Vă rugăm să încercați din nou.');
                }
            }

            // Succes
            setSuccessMessage('Un email cu instrucțiuni pentru resetarea parolei a fost trimis la adresa specificată!');
            setEmail('');

            // Închide modalul după 3 secunde
            setTimeout(() => {
                onClose();
            }, 3000);

        } catch (error) {
            console.error('Forgot password error:', error);
            setErrorMessage(error.message);

            if (emailInputRef.current) {
                setTimeout(() => emailInputRef.current.focus(), 100);
            }
        } finally {
            setIsLoading(false);
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!isLoading) {
            handleForgotPassword();
        }
    };

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    if (!isOpen) return null;

    return (
        <div
            className="forgot-password-modal-overlay"
            onClick={handleOverlayClick}
            role="dialog"
            aria-modal="true"
            aria-labelledby="forgot-password-modal-title"
        >
            <div
                className="forgot-password-modal-content"
                ref={modalRef}
                onClick={e => e.stopPropagation()}
            >
                <h1
                    id="forgot-password-modal-title"
                    className="forgot-password-modal-title"
                >
                    RESETARE PAROLĂ
                </h1>

                <p className="forgot-password-description">
                    Introdu adresa de email asociată contului tău și îți vom trimite instrucțiuni pentru resetarea parolei.
                </p>

                <form onSubmit={handleSubmit} className="forgot-password-form" noValidate>
                    <div className={`forgot-password-input-container ${errors.email ? 'forgot-password-input-error' : ''}`}>
                        <input
                            ref={emailInputRef}
                            type="email"
                            value={email}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                            className="forgot-password-input"
                            placeholder=" "
                            required
                            disabled={isLoading}
                            aria-label="Adresa de email"
                            aria-invalid={errors.email ? "true" : "false"}
                            autoComplete="email"
                        />
                        <label className="forgot-password-floating-label">E-mail</label>
                        {errors.email && (
                            <span className="sr-only">
                                Câmpul email este obligatoriu și trebuie să fie valid
                            </span>
                        )}
                    </div>

                    {errorMessage && (
                        <p
                            className="forgot-password-error-message"
                            role="alert"
                            aria-live="polite"
                        >
                            {errorMessage}
                        </p>
                    )}

                    {successMessage && (
                        <p
                            className="forgot-password-success-message"
                            role="alert"
                            aria-live="polite"
                        >
                            {successMessage}
                        </p>
                    )}

                    <button
                        type="submit"
                        className={`forgot-password-submit-button ${isLoading ? 'loading' : ''}`}
                        disabled={isLoading}
                    >
                        {isLoading ? 'Se trimite...' : 'Trimite instrucțiuni'}
                        {isLoading && (
                            <span className="sr-only">
                                Se trimite email-ul, vă rugăm să așteptați
                            </span>
                        )}
                    </button>

                    <button
                        type="button"
                        className="forgot-password-back-btn"
                        onClick={onClose}
                        disabled={isLoading}
                    >
                        Înapoi la autentificare
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ForgotPasswordModal;