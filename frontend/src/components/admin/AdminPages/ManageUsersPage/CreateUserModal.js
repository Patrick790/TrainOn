import React, { useState } from 'react';
import './CreateUserModal.css';
import axios from 'axios';

const CreateUserModal = ({
                             isOpen,
                             onClose,
                             onUserCreated,
                             userType = 'user',
                             title = 'Creare utilizator nou',
                             API_URL,
                             getAuthHeader
                         }) => {
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        password: '',
        confirmPassword: '',
        address: '',
        city: '',
        county: '',
        birthDate: '',
        userType: userType // Tipul utilizatorului setat direct din props
    });
    const [errors, setErrors] = useState({});
    const [errorMessage, setErrorMessage] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

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
        // Șterge eroarea pentru câmpul modificat
        setErrors(prevErrors => ({ ...prevErrors, [name]: false }));
        setErrorMessage('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validare formular
        const newErrors = {};
        if (!formData.name) newErrors.name = true;
        if (!formData.email) {
            newErrors.email = true;
        } else if (!validateEmail(formData.email)) {
            newErrors.email = true;
            setErrorMessage('Email-ul nu este valid!');
        }
        if (!formData.password) newErrors.password = true;
        if (!formData.confirmPassword) newErrors.confirmPassword = true;
        if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = true;
            setErrorMessage('Parolele nu coincid!');
        }
        if (!formData.address) newErrors.address = true;
        if (!formData.city) newErrors.city = true;
        if (!formData.county) newErrors.county = true;
        if (!formData.birthDate) newErrors.birthDate = true;

        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            if (!errorMessage) setErrorMessage('Completați toate câmpurile obligatorii!');
            return;
        }

        // Pregătim datele pentru creare utilizator
        const userData = {
            name: formData.name,
            email: formData.email,
            password: formData.password,
            address: formData.address,
            county: formData.county,
            city: formData.city,
            userType: formData.userType // Utilizăm direct valoarea din state
        };

        // Adaugă data nașterii
        if (formData.birthDate) {
            userData.birthDate = new Date(formData.birthDate).toISOString();
        }

        setIsSubmitting(true);

        try {
            const response = await axios.post(
                `${API_URL}/users`,
                userData,
                getAuthHeader()
            );

            if (response.data) {
                // Anunță componenta părinte că a fost creat un utilizator nou
                onUserCreated(response.data);

                // Închide modalul
                onClose();

                // Notificare de succes
                alert(`${userType === 'hall_admin' ? 'Administratorul de sală' : 'Utilizatorul'} a fost creat cu succes!`);
            }
        } catch (err) {
            console.error('Error creating user:', err);

            // Mesaj de eroare personalizat în funcție de tipul erorii
            if (err.response?.status === 409) {
                setErrorMessage('Acest email este deja folosit de un alt utilizator!');
            } else if (err.response?.status === 400) {
                setErrorMessage(err.response.data.message || 'Date invalide. Verificați toate câmpurile!');
            } else if (err.response?.status === 403) {
                setErrorMessage('Nu aveți permisiunea de a crea utilizatori!');
            } else {
                setErrorMessage('A apărut o eroare la crearea utilizatorului. Încercați din nou.');
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="admin-modal-overlay" onClick={onClose}>
            <div className="admin-modal-content" onClick={e => e.stopPropagation()}>
                <div className="admin-modal-header">
                    <h3 className="admin-modal-title">{title}</h3>
                    <button className="admin-modal-close" onClick={onClose}>×</button>
                </div>

                <div className="admin-modal-body">
                    <form onSubmit={handleSubmit} className="admin-form">
                        <div className={`form-group ${errors.name ? 'form-group-error' : ''}`}>
                            <label htmlFor="name">Nume:</label>
                            <input
                                type="text"
                                id="name"
                                name="name"
                                value={formData.name}
                                onChange={handleInputChange}
                                required
                            />
                        </div>

                        <div className={`form-group ${errors.email ? 'form-group-error' : ''}`}>
                            <label htmlFor="email">Email:</label>
                            <input
                                type="email"
                                id="email"
                                name="email"
                                value={formData.email}
                                onChange={handleInputChange}
                                required
                            />
                        </div>

                        <div className="form-row">
                            <div className={`form-group ${errors.password ? 'form-group-error' : ''}`}>
                                <label htmlFor="password">Parolă:</label>
                                <input
                                    type="password"
                                    id="password"
                                    name="password"
                                    value={formData.password}
                                    onChange={handleInputChange}
                                    required
                                />
                            </div>

                            <div className={`form-group ${errors.confirmPassword ? 'form-group-error' : ''}`}>
                                <label htmlFor="confirmPassword">Confirmare parolă:</label>
                                <input
                                    type="password"
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    value={formData.confirmPassword}
                                    onChange={handleInputChange}
                                    required
                                />
                            </div>
                        </div>

                        <div className={`form-group ${errors.birthDate ? 'form-group-error' : ''}`}>
                            <label htmlFor="birthDate">Data nașterii:</label>
                            <input
                                type="date"
                                id="birthDate"
                                name="birthDate"
                                value={formData.birthDate}
                                onChange={handleInputChange}
                                className="date-input"
                                required
                            />
                        </div>

                        <div className={`form-group ${errors.address ? 'form-group-error' : ''}`}>
                            <label htmlFor="address">Adresă:</label>
                            <input
                                type="text"
                                id="address"
                                name="address"
                                value={formData.address}
                                onChange={handleInputChange}
                                required
                            />
                        </div>

                        <div className="form-row">
                            <div className={`form-group ${errors.city ? 'form-group-error' : ''}`}>
                                <label htmlFor="city">Oraș:</label>
                                <input
                                    type="text"
                                    id="city"
                                    name="city"
                                    value={formData.city}
                                    onChange={handleInputChange}
                                    required
                                />
                            </div>

                            <div className={`form-group ${errors.county ? 'form-group-error' : ''}`}>
                                <label htmlFor="county">Județ:</label>
                                <input
                                    type="text"
                                    id="county"
                                    name="county"
                                    value={formData.county}
                                    onChange={handleInputChange}
                                    required
                                />
                            </div>
                        </div>

                        {errorMessage && <div className="form-error-message">{errorMessage}</div>}

                        <div className="admin-modal-footer">
                            <button type="button" className="admin-button cancel" onClick={onClose}>
                                Anulează
                            </button>
                            <button
                                type="submit"
                                className="admin-button save"
                                disabled={isSubmitting}
                            >
                                {isSubmitting ? 'Se creează...' : 'Creează utilizator'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default CreateUserModal;