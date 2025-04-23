import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginModal.css';

const LoginModal = ({ isOpen, onClose, onRegisterClick, onLoginSuccess }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState({});
    const [errorMessage, setErrorMessage] = useState('');
    const navigate = useNavigate();

    const handleInputChange = (setter, field) => (e) => {
        setter(e.target.value);
        setErrors((prevErrors) => ({ ...prevErrors, [field]: false }));
        setErrorMessage("");
    };

    async function login() {
        const newErrors = {};
        if (!email) newErrors.email = true;
        if (!password) newErrors.password = true;

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            setErrorMessage("Toate câmpurile sunt obligatorii!");
            return;
        }

        const url = 'http://localhost:8080/login';
        try {
            const response = await fetch(url, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });
            if (!response.ok) {
                throw new Error(`Response status: ${response.status}`);
            }
            const json = await response.json();

            const urlToken = 'http://localhost:8080/login/generateToken';
            const genToken = await fetch(urlToken, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password })
            });

            if (genToken.ok) {
                const token = await genToken.text();
                localStorage.setItem('jwtToken', token);
            } else {
                console.error('Failed to generate token:', await genToken.text());
            }

            localStorage.setItem('userId', json.id);
            localStorage.setItem('userType', json.userType);
            localStorage.setItem('isLoggedIn', 'true');

            // Close modal first
            onClose();

            // Trigger parent component update
            if (onLoginSuccess) {
                onLoginSuccess();
            }

            alert('Login successful!');

            // Navigate based on user type
            if (json.userType === "admin") {
                navigate('/admin-dashboard', { replace: true });
            } else if (json.userType === "hall_admin") {
                navigate('/hall-admin-dashboard', { replace: true });
            } else if (json.userType === "user") {
                navigate('/user-home', { replace: true });
            } else {
                // Force page refresh to update UI if not navigating away
                window.location.reload();
            }
        } catch (err) {
            setErrorMessage('Email sau parola incorectă!');
            console.log(err);
        }
    }

    const handleSubmit = (e) => {
        e.preventDefault();
        login();
    };

    const handleRegisterClick = (e) => {
        e.preventDefault();
        onClose();
        onRegisterClick();
    };

    if (!isOpen) return null;

    return (
        <div className="login-modal-overlay" onClick={onClose}>
            <div className="login-modal-content" onClick={e => e.stopPropagation()}>
                <h1 className="login-modal-title">LOGIN</h1>

                <form onSubmit={handleSubmit} className="login-modal-form">
                    <div className={`login-input-container ${errors.email ? 'login-input-error' : ''}`}>
                        <input
                            type="email"
                            value={email}
                            onChange={handleInputChange(setEmail, 'email')}
                            className="login-modal-input"
                            placeholder=" "
                            required
                        />
                        <label className="login-floating-label">E-mail</label>
                    </div>

                    <div className={`login-input-container ${errors.password ? 'login-input-error' : ''}`}>
                        <input
                            type="password"
                            value={password}
                            onChange={handleInputChange(setPassword, 'password')}
                            className="login-modal-input"
                            placeholder=" "
                            required
                        />
                        <label className="login-floating-label">Parola</label>
                    </div>

                    {errorMessage && <p className="login-error-message">{errorMessage}</p>}

                    <button
                        type="button"
                        className="login-forgot-password-btn"
                        onClick={() => alert('Funcționalitate recuperare parolă')}
                    >
                        Ti-ai uitat parola?
                    </button>

                    <button type="submit" className="login-submit-button">
                        Conectare
                    </button>

                    <button
                        type="button"
                        className="login-create-account-btn"
                        onClick={handleRegisterClick}
                    >
                        Nu ai cont? Creeaza-ti un cont nou
                    </button>
                </form>
            </div>
        </div>
    );
};

export default LoginModal;