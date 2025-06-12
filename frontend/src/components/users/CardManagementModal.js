import React, { useState } from 'react';
import { CreditCard, Plus, Trash } from 'lucide-react';
import './CardManagementModal.css';

const CardManagementModal = ({ userCards, onClose, onCardsUpdated }) => {
    const [isAdding, setIsAdding] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [newCard, setNewCard] = useState({
        number: '',
        expiry: '',
        cvc: '',
        name: '',
        setAsDefault: false
    });

    const getAuthHeaders = () => {
        const token = localStorage.getItem('jwtToken');
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        };
    };

    const formatCardNumber = (value) => {
        const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
        const matches = v.match(/\d{4,16}/g);
        const match = matches && matches[0] || '';
        const parts = [];
        for (let i = 0, len = match.length; i < len; i += 4) {
            parts.push(match.substring(i, i + 4));
        }
        return parts.length ? parts.join(' ') : v;
    };

    const formatExpiry = (value) => {
        const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
        if (v.length >= 2) {
            return v.substring(0, 2) + '/' + v.substring(2, 4);
        }
        return v;
    };

    const handleInputChange = (field, value) => {
        let formattedValue = value;
        if (field === 'number') {
            formattedValue = formatCardNumber(value);
        } else if (field === 'expiry') {
            formattedValue = formatExpiry(value);
        } else if (field === 'cvc') {
            formattedValue = value.replace(/[^0-9]/g, '').substring(0, 4);
        }

        setNewCard(prev => ({
            ...prev,
            [field]: formattedValue
        }));
    };

    const validateCard = () => {
        const errors = {};

        // Validare număr card
        if (!newCard.number || newCard.number.replace(/\s/g, '').length < 13) {
            errors.number = 'Numărul cardului este invalid';
        }

        // Validare expiry
        if (!newCard.expiry || !/^\d{2}\/\d{2}$/.test(newCard.expiry)) {
            errors.expiry = 'Data expirării trebuie să fie în formatul MM/YY';
        }

        // Validare CVC
        if (!newCard.cvc || newCard.cvc.length < 3) {
            errors.cvc = 'CVC-ul trebuie să aibă cel puțin 3 cifre';
        }

        // Validare nume
        if (!newCard.name.trim()) {
            errors.name = 'Numele este obligatoriu';
        }

        return Object.keys(errors).length === 0 ? null : errors;
    };

    const handleAddCard = async (e) => {
        e.preventDefault();

        const validationErrors = validateCard();
        if (validationErrors) {
            setError('Vă rugăm să completați corect toate câmpurile.');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const [month, year] = newCard.expiry.split('/');
            const cardNumber = newCard.number.replace(/\s/g, '');

            // Log pentru debugging
            console.log('Token din localStorage:', localStorage.getItem('jwtToken'));
            console.log('Card number being sent:', cardNumber);

            const response = await fetch('http://localhost:8080/card-payment-methods/save-card', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({
                    cardDetails: {
                        number: cardNumber,
                        exp_month: parseInt(month, 10),
                        exp_year: parseInt('20' + year, 10),
                        cvc: newCard.cvc,
                        cardholder_name: newCard.name
                    },
                    setAsDefault: newCard.setAsDefault
                })
            });

            console.log('Response status:', response.status);

            // Pentru 403, să nu redirectăm automat
            if (response.status === 403) {
                console.error('403 Forbidden - posibil cardul este deja salvat');
                setError('Nu aveți permisiunea să adăugați acest card. Posibil să fie deja salvat.');
                return;
            }

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Eroare la salvarea cardului');
            }

            // Reset form
            setNewCard({
                number: '',
                expiry: '',
                cvc: '',
                name: '',
                setAsDefault: false
            });
            setIsAdding(false);

            // Refresh cards list
            await onCardsUpdated();

        } catch (err) {
            console.error('Eroare la adăugarea cardului:', err);
            setError(err.message || 'A apărut o eroare la salvarea cardului.');
        } finally {
            setLoading(false);
        }
    };

    const handleRemoveCard = async (cardId) => {
        if (!window.confirm('Ești sigur că vrei să ștergi acest card?')) {
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const response = await fetch(`http://localhost:8080/card-payment-methods/${cardId}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Eroare la ștergerea cardului');
            }

            await onCardsUpdated();
        } catch (err) {
            console.error('Eroare la ștergerea cardului:', err);
            setError(err.message || 'A apărut o eroare la ștergerea cardului.');
        } finally {
            setLoading(false);
        }
    };

    const handleSetDefault = async (cardId) => {
        setLoading(true);
        setError(null);

        try {
            const response = await fetch(`http://localhost:8080/card-payment-methods/${cardId}/set-default`, {
                method: 'PUT',
                headers: getAuthHeaders()
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.error || 'Eroare la setarea cardului ca default');
            }

            await onCardsUpdated();
        } catch (err) {
            console.error('Eroare la setarea cardului ca default:', err);
            setError(err.message || 'A apărut o eroare la setarea cardului ca default.');
        } finally {
            setLoading(false);
        }
    };

    const cancelAdding = () => {
        setIsAdding(false);
        setNewCard({
            number: '',
            expiry: '',
            cvc: '',
            name: '',
            setAsDefault: false
        });
        setError(null);
    };

    // Handle click outside modal
    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    return (
        <div className="card-modal-overlay" onClick={handleOverlayClick}>
            <div className="card-modal-content">
                <div className="card-modal-header">
                    <h3>Gestionează cardurile tale</h3>
                    <button
                        className="card-modal-close"
                        onClick={onClose}
                        aria-label="Închide modal"
                    >
                        ×
                    </button>
                </div>

                <div className="card-modal-body">
                    {error && (
                        <div className="card-error-message">
                            {error}
                        </div>
                    )}

                    <div className="cards-list">
                        {userCards.length === 0 ? (
                            <div className="no-cards-message">
                                <CreditCard size={48} />
                                <p>Nu aveți carduri salvate</p>
                                <p>Adăugați primul card pentru plăți automate</p>
                            </div>
                        ) : (
                            userCards.map(card => (
                                <div key={card.id} className="card-item">
                                    <div className="card-info">
                                        <CreditCard size={20} />
                                        <div className="card-details">
                                            <span className="card-name">{card.displayName}</span>
                                            <div className="card-badges">
                                                {card.isDefault && (
                                                    <span className="card-badge card-badge-default">Default</span>
                                                )}
                                                {card.isExpired && (
                                                    <span className="card-badge card-badge-expired">Expirat</span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                    <div className="card-actions">
                                        {!card.isDefault && !card.isExpired && (
                                            <button
                                                onClick={() => handleSetDefault(card.id)}
                                                disabled={loading}
                                                className="card-action-button"
                                            >
                                                Setează ca default
                                            </button>
                                        )}
                                        <button
                                            onClick={() => handleRemoveCard(card.id)}
                                            disabled={loading}
                                            className="card-remove-button"
                                            aria-label="Șterge card"
                                        >
                                            <Trash size={16} />
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>

                    {!isAdding ? (
                        <button
                            onClick={() => setIsAdding(true)}
                            className="add-card-button"
                            disabled={loading}
                        >
                            <Plus size={16} />
                            Adaugă un card nou
                        </button>
                    ) : (
                        <form onSubmit={handleAddCard} className="add-card-form">
                            <h4>Adaugă un card nou</h4>

                            <div className="form-group">
                                <label htmlFor="cardName">Numele de pe card</label>
                                <input
                                    id="cardName"
                                    type="text"
                                    value={newCard.name}
                                    onChange={(e) => handleInputChange('name', e.target.value)}
                                    placeholder="Nume Prenume"
                                    required
                                    disabled={loading}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="cardNumber">Numărul cardului</label>
                                <input
                                    id="cardNumber"
                                    type="text"
                                    value={newCard.number}
                                    onChange={(e) => handleInputChange('number', e.target.value)}
                                    placeholder="4242 4242 4242 4242"
                                    maxLength="19"
                                    required
                                    disabled={loading}
                                />
                                <small>Folosiți carduri de test Stripe pentru dezvoltare</small>
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label htmlFor="cardExpiry">Data expirării</label>
                                    <input
                                        id="cardExpiry"
                                        type="text"
                                        value={newCard.expiry}
                                        onChange={(e) => handleInputChange('expiry', e.target.value)}
                                        placeholder="MM/YY"
                                        maxLength="5"
                                        required
                                        disabled={loading}
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="cardCvc">CVC</label>
                                    <input
                                        id="cardCvc"
                                        type="text"
                                        value={newCard.cvc}
                                        onChange={(e) => handleInputChange('cvc', e.target.value)}
                                        placeholder="123"
                                        maxLength="4"
                                        required
                                        disabled={loading}
                                    />
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="checkbox-label">
                                    <input
                                        type="checkbox"
                                        checked={newCard.setAsDefault}
                                        onChange={(e) => setNewCard(prev => ({
                                            ...prev,
                                            setAsDefault: e.target.checked
                                        }))}
                                        disabled={loading}
                                    />
                                    <span>Setează ca card default</span>
                                </label>
                            </div>

                            <div className="test-cards-info">
                                <h5>💡 Carduri de test recomandate:</h5>
                                <ul>
                                    <li><strong>Visa:</strong> <code>4242 4242 4242 4242</code></li>
                                    <li><strong>Mastercard:</strong> <code>5555 5555 5555 4444</code></li>
                                    <li><strong>Exp:</strong> orice dată viitoare, <strong>CVC:</strong> orice 3 cifre</li>
                                </ul>
                            </div>

                            <div className="form-actions">
                                <button
                                    type="button"
                                    onClick={cancelAdding}
                                    disabled={loading}
                                    className="cancel-button"
                                >
                                    Anulează
                                </button>
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="submit-button"
                                >
                                    {loading ? 'Se salvează...' : 'Salvează cardul'}
                                </button>
                            </div>
                        </form>
                    )}

                    {loading && (
                        <div className="loading-overlay">
                            <div className="loading-spinner"></div>
                            <p>Se procesează...</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default CardManagementModal;