import React, { useState } from 'react';
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
    const [errors, setErrors] = useState({});
    const [errorMessage, setErrorMessage] = useState('');

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

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
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

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            if (!errorMessage) setErrorMessage('Toate câmpurile sunt obligatorii!');
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
            const response = await fetch('http://localhost:8080/register/test', {
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

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content register-modal" onClick={e => e.stopPropagation()}>
                <h1 className="modal-title">INREGISTRARE</h1>

                <div className="register-type-selector">
                    <label className="type-option">
                        <input
                            type="radio"
                            checked={isAssociation}
                            onChange={() => handleTypeChange('association')}
                        />
                        <span>Asociatie sportiva</span>
                    </label>
                    <label className="type-option">
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
                            <div className={`input-container ${errors.associationName ? 'error' : ''}`}>
                                <input
                                    type="text"
                                    name="associationName"
                                    value={formData.associationName}
                                    onChange={handleInputChange}
                                    placeholder=" "
                                    required
                                />
                                <label className="floating-label">Numele asociatiei</label>
                            </div>
                            <div className={`input-container ${errors.teamType ? 'error' : ''}`}>
                                <input
                                    type="text"
                                    name="teamType"
                                    value={formData.teamType}
                                    onChange={handleInputChange}
                                    placeholder=" "
                                    required
                                />
                                <label className="floating-label">Tip echipa</label>
                            </div>
                            <div className="certificate-upload">
                                <label>
                                    <span>Insereaza certificatul de inregistrare la Ministerul Sportului</span>
                                    <input
                                        type="file"
                                        onChange={handleFileChange}
                                    />
                                </label>
                            </div>
                        </>
                    ) : (
                        <div className={`input-container ${errors.name ? 'error' : ''}`}>
                            <input
                                type="text"
                                name="name"
                                value={formData.name}
                                onChange={handleInputChange}
                                placeholder=" "
                                required
                            />
                            <label className="floating-label">Nume</label>
                        </div>
                    )}

                    <div className={`input-container ${errors.email ? 'error' : ''}`}>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleInputChange}
                            placeholder=" "
                            required
                        />
                        <label className="floating-label">Email</label>
                    </div>

                    {!isAssociation && (
                        <div className={`input-container ${errors.birthDate ? 'error' : ''}`}>
                            <input
                                type="date"
                                name="birthDate"
                                value={formData.birthDate}
                                onChange={handleInputChange}
                                placeholder=" "
                                required
                            />
                            <label className="floating-label">Data nasterii</label>
                        </div>
                    )}

                    <div className={`input-container ${errors.address ? 'error' : ''}`}>
                        <input
                            type="text"
                            name="address"
                            value={formData.address}
                            onChange={handleInputChange}
                            placeholder=" "
                            required
                        />
                        <label className="floating-label">Adresa</label>
                    </div>

                    <div className={`input-container ${errors.county ? 'error' : ''}`}>
                        <input
                            type="text"
                            name="county"
                            value={formData.county}
                            onChange={handleInputChange}
                            placeholder=" "
                            required
                        />
                        <label className="floating-label">Judet</label>
                    </div>

                    <div className={`input-container ${errors.city ? 'error' : ''}`}>
                        <input
                            type="text"
                            name="city"
                            value={formData.city}
                            onChange={handleInputChange}
                            placeholder=" "
                            required
                        />
                        <label className="floating-label">Localitate</label>
                    </div>

                    <div className={`input-container ${errors.password ? 'error' : ''}`}>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleInputChange}
                            placeholder=" "
                            required
                        />
                        <label className="floating-label">Parola</label>
                    </div>

                    <div className={`input-container ${errors.confirmPassword ? 'error' : ''}`}>
                        <input
                            type="password"
                            name="confirmPassword"
                            value={formData.confirmPassword}
                            onChange={handleInputChange}
                            placeholder=" "
                            required
                        />
                        <label className="floating-label">Confirmare parola</label>
                    </div>

                    {errorMessage && <p className="error-message">{errorMessage}</p>}

                    <button type="submit" className="register-button">
                        Creeaza cont
                    </button>
                </form>
            </div>
        </div>
    );
};

export default RegisterModal;