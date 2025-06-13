import React, { useState, useRef, useEffect } from 'react';
import './RegisterModal.css';

const RegisterModal = ({ isOpen, onClose }) => {
    const [isAssociation, setIsAssociation] = useState(true);
    const [formData, setFormData] = useState({
        associationName: '',
        name: '',
        email: '',
        birthDate: '',
        address: '',
        county: '',
        city: '',
        password: '',
        confirmPassword: '',
        teamType: '',
        certificate: null
    });
    const [fileName, setFileName] = useState('');
    const [errors, setErrors] = useState({});
    const [errorMessage, setErrorMessage] = useState('');
    const API_BASE_URL = process.env.REACT_APP_API_URL || '';


    // Ref pentru primul input
    const firstInputRef = useRef(null);

    // Focus pe primul input când modalul se deschide
    useEffect(() => {
        if (isOpen && firstInputRef.current) {
            setTimeout(() => {
                firstInputRef.current.focus();
            }, 100);
        }
    }, [isOpen]);

    // Gestionarea tastei Escape pentru închiderea modalului
    useEffect(() => {
        const handleKeyDown = (e) => {
            if (!isOpen) return;

            if (e.key === 'Escape') {
                onClose();
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

    if (!isOpen) return null;

    const validateEmail = (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        setErrors(prevErrors => ({ ...prevErrors, [name]: false }));
        setErrorMessage('');
    };

    // Funcție pentru gestionarea tastei Enter
    const handleKeyPress = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleRegister();
        }
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            // Salvează numele fișierului pentru afișare
            setFileName(file.name);

            const reader = new FileReader();
            reader.onloadend = () => {
                // Convertește fișierul într-un array de bytes pentru a-l trimite la server
                const arrayBuffer = reader.result;
                const byteArray = new Uint8Array(arrayBuffer);
                setFormData(prev => ({
                    ...prev,
                    certificate: Array.from(byteArray)
                }));
            };
            reader.readAsArrayBuffer(file);
        }
    };

    const handleRegister = async () => {
        const newErrors = {};
        if (isAssociation && !formData.associationName) newErrors.associationName = true;
        if (!isAssociation && !formData.name) newErrors.name = true;
        if (!formData.email) {
            newErrors.email = true;
        } else if (!validateEmail(formData.email)) {
            newErrors.email = true;
            setErrorMessage('Email-ul nu este valid!');
        }
        if (!formData.password) newErrors.password = true;
        if (!formData.confirmPassword) newErrors.confirmPassword = true;
        if (formData.password !== formData.confirmPassword) newErrors.confirmPassword = true;

        // Verificăm dacă certificatul este furnizat pentru asociații
        if (isAssociation && !formData.certificate) {
            newErrors.certificate = true;
        }

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            if (!errorMessage) setErrorMessage('Toate câmpurile sunt obligatorii!');

            // Focus pe primul câmp cu eroare
            if (firstInputRef.current) {
                firstInputRef.current.focus();
            }
            return;
        }

        // Pregătire date pentru request
        const user = {
            name: isAssociation ? formData.associationName : formData.name,
            email: formData.email,
            password: formData.password,
            address: formData.address,
            county: formData.county,
            city: formData.city,
            teamType: isAssociation ? formData.teamType : null,
            certificate: isAssociation ? formData.certificate : null
        };

        // Adaugă data nașterii doar dacă nu este asociație
        if (!isAssociation && formData.birthDate) {
            // Data trebuie trimisă în format ISO pentru a fi corect deserializată de Java
            user.birthDate = new Date(formData.birthDate).toISOString();
        }

        console.log("Sending user data:", user);

        try {
            const response = await fetch(`${API_BASE_URL}/register/test`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(user),
            });

            if (response.ok) {
                alert('Cont creat cu succes!');
                onClose();
            } else {
                const errorText = await response.text();
                alert(`Eroare la crearea contului: ${errorText}`);
            }
        } catch (error) {
            console.error('Eroare la crearea contului:', error);
            alert('Eroare la crearea contului!');
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        handleRegister();
    };

    const handleTypeChange = (type) => {
        setIsAssociation(type === 'association');
    };

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    return (
        <div className="register-modal-overlay" onClick={handleOverlayClick}>
            <div className="register-modal-content" onClick={e => e.stopPropagation()}>
                <h1 className="register-modal-title">INREGISTRARE</h1>

                <div className="register-type-selector">
                    <label className="register-type-option">
                        <input
                            type="radio"
                            checked={isAssociation}
                            onChange={() => handleTypeChange('association')}
                        />
                        <span>Asociatie sportiva</span>
                    </label>
                    <label className="register-type-option">
                        <input
                            type="radio"
                            checked={!isAssociation}
                            onChange={() => handleTypeChange('person')}
                        />
                        <span>Persoana fizica</span>
                    </label>
                </div>

                <form onSubmit={handleSubmit} className="register-form">
                    {isAssociation ? (
                        <>
                            <div className={`register-input-container ${errors.associationName ? 'register-input-error' : ''}`}>
                                <input
                                    ref={firstInputRef}
                                    type="text"
                                    name="associationName"
                                    value={formData.associationName}
                                    onChange={handleInputChange}
                                    onKeyPress={handleKeyPress}
                                    placeholder=" "
                                    required
                                    className="register-input"
                                    autoComplete="organization"
                                />
                                <label className="register-floating-label">Numele asociatiei</label>
                            </div>
                            <div className={`register-input-container ${errors.teamType ? 'register-input-error' : ''}`}>
                                <select
                                    name="teamType"
                                    value={formData.teamType}
                                    onChange={handleInputChange}
                                    onKeyPress={handleKeyPress}
                                    required
                                    className="register-input register-select"
                                >
                                    <option value="">Selectează tip echipa</option>
                                    <option value="Juniori">Juniori</option>
                                    <option value="Seniori">Seniori</option>
                                </select>
                                <label className="register-floating-label">Tip echipa</label>
                            </div>
                            <div className={`register-certificate-upload ${errors.certificate ? 'register-certificate-error' : ''}`}>
                                <label className="register-file-label">
                                    <div className="register-upload-content">
                                        <span className="register-upload-icon">📎</span>
                                        <span className="register-upload-text">
                                            {fileName
                                                ? `Fișier selectat: ${fileName}`
                                                : 'Inserează certificatul de înregistrare la Ministerul Sportului'}
                                        </span>
                                    </div>
                                    <input
                                        type="file"
                                        onChange={handleFileChange}
                                        className="register-file-input"
                                        accept=".pdf,.jpg,.jpeg,.png"
                                    />
                                </label>
                            </div>
                        </>
                    ) : (
                        <div className={`register-input-container ${errors.name ? 'register-input-error' : ''}`}>
                            <input
                                ref={firstInputRef}
                                type="text"
                                name="name"
                                value={formData.name}
                                onChange={handleInputChange}
                                onKeyPress={handleKeyPress}
                                placeholder=" "
                                required
                                className="register-input"
                                autoComplete="name"
                            />
                            <label className="register-floating-label">Nume</label>
                        </div>
                    )}

                    <div className={`register-input-container ${errors.email ? 'register-input-error' : ''}`}>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                            placeholder=" "
                            required
                            className="register-input"
                            autoComplete="email"
                        />
                        <label className="register-floating-label">Email</label>
                    </div>

                    {!isAssociation && (
                        <div className={`register-input-container ${errors.birthDate ? 'register-input-error' : ''}`}>
                            <input
                                type="date"
                                name="birthDate"
                                value={formData.birthDate}
                                onChange={handleInputChange}
                                onKeyPress={handleKeyPress}
                                placeholder=" "
                                required
                                className="register-input register-date-input"
                                autoComplete="bday"
                            />
                            <label className="register-floating-label">Data nasterii</label>
                        </div>
                    )}

                    <div className={`register-input-container ${errors.address ? 'register-input-error' : ''}`}>
                        <input
                            type="text"
                            name="address"
                            value={formData.address}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                            placeholder=" "
                            required
                            className="register-input"
                            autoComplete="street-address"
                        />
                        <label className="register-floating-label">Adresa</label>
                    </div>

                    <div className={`register-input-container ${errors.county ? 'register-input-error' : ''}`}>
                        <input
                            type="text"
                            name="county"
                            value={formData.county}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                            placeholder=" "
                            required
                            className="register-input"
                            autoComplete="address-level1"
                        />
                        <label className="register-floating-label">Judet</label>
                    </div>

                    <div className={`register-input-container ${errors.city ? 'register-input-error' : ''}`}>
                        <input
                            type="text"
                            name="city"
                            value={formData.city}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                            placeholder=" "
                            required
                            className="register-input"
                            autoComplete="address-level2"
                        />
                        <label className="register-floating-label">Localitate</label>
                    </div>

                    <div className={`register-input-container ${errors.password ? 'register-input-error' : ''}`}>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                            placeholder=" "
                            required
                            className="register-input"
                            autoComplete="new-password"
                        />
                        <label className="register-floating-label">Parola</label>
                    </div>

                    <div className={`register-input-container ${errors.confirmPassword ? 'register-input-error' : ''}`}>
                        <input
                            type="password"
                            name="confirmPassword"
                            value={formData.confirmPassword}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                            placeholder=" "
                            required
                            className="register-input"
                            autoComplete="new-password"
                        />
                        <label className="register-floating-label">Confirmare parola</label>
                    </div>

                    {errorMessage && <p className="register-error-message">{errorMessage}</p>}

                    <button type="submit" className="register-submit-button">
                        Creează cont
                    </button>
                </form>
            </div>
        </div>
    );
};

export default RegisterModal;