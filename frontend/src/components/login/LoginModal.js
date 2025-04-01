import React, { useState, useEffect } from 'react';
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

            // Show success alert
            alert('Login successful!');

            // Navigate based on user type
            if (json.userType === "hall_admin") {
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
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <h1 className="modal-title">LOGIN</h1>

                <form onSubmit={handleSubmit} className="login-form">
                    <div className={`input-container ${errors.email ? 'error' : ''}`}>
                        <input
                            type="email"
                            value={email}
                            onChange={handleInputChange(setEmail, 'email')}
                            className="login-input"
                            placeholder=" "
                            required
                        />
                        <label className="floating-label">E-mail</label>
                    </div>

                    <div className={`input-container ${errors.password ? 'error' : ''}`}>
                        <input
                            type="password"
                            value={password}
                            onChange={handleInputChange(setPassword, 'password')}
                            className="login-input"
                            placeholder=" "
                            required
                        />
                        <label className="floating-label">Parola</label>
                    </div>

                    {errorMessage && <p className="error-message">{errorMessage}</p>}

                    <a href="#" className="forgot-password">Ti-ai uitat parola?</a>

                    <button type="submit" className="login-button">
                        Conectare
                    </button>

                    <a
                        href="#"
                        className="create-account"
                        onClick={handleRegisterClick}
                    >
                        Nu ai cont? Creeaza-ti un cont nou
                    </a>
                </form>
            </div>
        </div>
    );
};

export default LoginModal;