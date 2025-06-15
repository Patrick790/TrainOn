import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './StripePaymentPage.css';

const STRIPE_PUBLISHABLE_KEY = 'pk_test_51RUSNJB90iMxhg7K13VbDV4YNESfylJS8mNsenCauFMwbo2t9rDEptGatnEwR2lzQrgb7liaFKyFTjfyMkq2ooT900FSp78Ooy';

const getAuthToken = () => {
    return localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
};

const API_BASE_URL = process.env.REACT_APP_API_URL || '';

const getAuthHeaders = () => {
    const token = getAuthToken();
    return {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` })
    };
};

const StripePaymentMethodCheckout = ({ amount, reservationData, onPaymentProcessing, onPaymentSuccess, onPaymentError }) => {
    const [stripe, setStripe] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [cardDetails, setCardDetails] = useState({
        number: '',
        expiry: '',
        cvc: '',
        name: ''
    });
    const [cardErrors, setCardErrors] = useState({});

    useEffect(() => {
        const loadStripe = async () => {
            try {
                if (window.Stripe) {
                    const stripeInstance = window.Stripe(STRIPE_PUBLISHABLE_KEY);
                    setStripe(stripeInstance);
                    return;
                }

                const existingScript = document.querySelector('script[src="https://js.stripe.com/v3/"]');
                if (existingScript) {
                    existingScript.onload = () => {
                        if (window.Stripe) {
                            const stripeInstance = window.Stripe(STRIPE_PUBLISHABLE_KEY);
                            setStripe(stripeInstance);
                        }
                    };
                    return;
                }

                const script = document.createElement('script');
                script.src = 'https://js.stripe.com/v3/';
                script.onload = () => {
                    if (window.Stripe) {
                        const stripeInstance = window.Stripe(STRIPE_PUBLISHABLE_KEY);
                        setStripe(stripeInstance);
                    } else {
                        onPaymentError('Stripe nu s-a încărcat corect.');
                    }
                };
                script.onerror = () => {
                    onPaymentError('Nu s-a putut încărca Stripe. Vă rugăm să verificați conexiunea la internet.');
                };
                document.head.appendChild(script);
            } catch (error) {
                onPaymentError('Eroare la încărcarea Stripe: ' + error.message);
            }
        };

        loadStripe();
    }, [onPaymentError]);

    const validateCard = () => {
        const errors = {};

        if (!cardDetails.number || cardDetails.number.replace(/\s/g, '').length < 13) {
            errors.number = 'Numărul cardului este invalid';
        }

        if (!cardDetails.expiry || !/^\d{2}\/\d{2}$/.test(cardDetails.expiry)) {
            errors.expiry = 'Data expirării trebuie să fie în formatul MM/YY';
        }

        if (!cardDetails.cvc || cardDetails.cvc.length < 3) {
            errors.cvc = 'CVC-ul trebuie să aibă cel puțin 3 cifre';
        }

        if (!cardDetails.name.trim()) {
            errors.name = 'Numele este obligatoriu';
        }

        setCardErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const formatCardNumber = (value) => {
        const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
        const matches = v.match(/\d{4,16}/g);
        const match = matches && matches[0] || '';
        const parts = [];
        for (let i = 0, len = match.length; i < len; i += 4) {
            parts.push(match.substring(i, i + 4));
        }
        if (parts.length) {
            return parts.join(' ');
        } else {
            return v;
        }
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

        setCardDetails(prev => ({
            ...prev,
            [field]: formattedValue
        }));

        if (cardErrors[field]) {
            setCardErrors(prev => ({
                ...prev,
                [field]: ''
            }));
        }
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!stripe) {
            onPaymentError("Stripe nu s-a încărcat corect.");
            return;
        }

        if (!validateCard()) {
            return;
        }

        // Verifică dacă utilizatorul este autentificat
        const token = getAuthToken();
        if (!token) {
            onPaymentError("Nu sunteți autentificat. Vă rugăm să vă logați din nou.");
            return;
        }

        setIsProcessing(true);
        onPaymentProcessing(true);

        try {
            // Pas 1: Creeaza PaymentIntent pe backend
            const intentResponse = await fetch(`${API_BASE_URL}/payment/stripe/create-payment-intent`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({
                    amount: amount,
                    reservations: reservationData
                })
            });

            if (intentResponse.status === 401 || intentResponse.status === 403) {
                throw new Error('Nu sunteți autorizat. Vă rugăm să vă logați din nou.');
            }

            const intentResult = await intentResponse.json();

            if (!intentResponse.ok || !intentResult.clientSecret) {
                throw new Error(intentResult.error || 'Eroare la crearea intenției de plată.');
            }

            const { clientSecret } = intentResult;

            // Pas 2: Foloseste test token in loc de date raw pentru siguranță
            const [month, year] = cardDetails.expiry.split('/');

            let paymentMethodId;

            const cardNumber = cardDetails.number.replace(/\s/g, '');

            if (cardNumber === '4242424242424242') {
                paymentMethodId = 'pm_card_visa';
            } else if (cardNumber === '5555555555554444') {
                paymentMethodId = 'pm_card_mastercard';
            } else {
                try {
                    const { error: pmError, paymentMethod } = await stripe.createPaymentMethod({
                        type: 'card',
                        card: {
                            number: cardNumber,
                            exp_month: parseInt(month, 10),
                            exp_year: parseInt('20' + year, 10),
                            cvc: cardDetails.cvc,
                        },
                        billing_details: {
                            name: cardDetails.name,
                        }
                    });

                    if (pmError) {
                        throw new Error(pmError.message);
                    }
                    paymentMethodId = paymentMethod.id;
                } catch (pmErr) {
                    const confirmResponse = await fetch(`${API_BASE_URL}/payment/stripe/confirm-payment-with-card`, {
                        method: 'POST',
                        headers: getAuthHeaders(),
                        body: JSON.stringify({
                            clientSecret: clientSecret,
                            cardDetails: {
                                number: cardNumber,
                                exp_month: parseInt(month, 10),
                                exp_year: parseInt('20' + year, 10),
                                cvc: cardDetails.cvc,
                                cardholder_name: cardDetails.name
                            }
                        })
                    });

                    if (confirmResponse.status === 401 || confirmResponse.status === 403) {
                        throw new Error('Nu sunteți autorizat. Vă rugăm să vă logați din nou.');
                    }

                    const confirmResult = await confirmResponse.json();

                    if (!confirmResponse.ok) {
                        throw new Error(confirmResult.error || 'Eroare la confirmarea plății.');
                    }

                    const paymentIntent = confirmResult.paymentIntent;

                    if (paymentIntent && paymentIntent.status === 'succeeded') {
                        const finalizeResponse = await fetch(`${API_BASE_URL}/payment/stripe/finalize-payment-and-create-reservations`, {
                            method: 'POST',
                            headers: getAuthHeaders(),
                            body: JSON.stringify({
                                stripePaymentIntentId: paymentIntent.id,
                                reservations: reservationData
                            })
                        });

                        if (finalizeResponse.status === 401 || finalizeResponse.status === 403) {
                            throw new Error('Nu sunteți autorizat. Vă rugăm să vă logați din nou.');
                        }

                        const finalizeResult = await finalizeResponse.json();

                        if (!finalizeResponse.ok || !finalizeResult.success) {
                            throw new Error(finalizeResult.error || 'Eroare la finalizarea rezervărilor pe server.');
                        }

                        onPaymentSuccess(finalizeResult, 'card');
                        return;
                    } else {
                        throw new Error('Plata Stripe nu a reușit. Status: ' + (paymentIntent ? paymentIntent.status : 'necunoscut'));
                    }
                }
            }

            const { error: stripePaymentError, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
                payment_method: paymentMethodId
            });

            if (stripePaymentError) {
                throw new Error(stripePaymentError.message);
            }

            if (paymentIntent && paymentIntent.status === 'succeeded') {
                const finalizeResponse = await fetch(`${API_BASE_URL}/payment/stripe/finalize-payment-and-create-reservations`, {
                    method: 'POST',
                    headers: getAuthHeaders(),
                    body: JSON.stringify({
                        stripePaymentIntentId: paymentIntent.id,
                        reservations: reservationData
                    })
                });

                if (finalizeResponse.status === 401 || finalizeResponse.status === 403) {
                    throw new Error('Nu sunteți autorizat. Vă rugăm să vă logați din nou.');
                }

                const finalizeResult = await finalizeResponse.json();

                if (!finalizeResponse.ok || !finalizeResult.success) {
                    throw new Error(finalizeResult.error || 'Eroare la finalizarea rezervărilor pe server.');
                }

                onPaymentSuccess(finalizeResult, 'card');
            } else {
                throw new Error('Plata Stripe nu a reușit. Status: ' + (paymentIntent ? paymentIntent.status : 'necunoscut'));
            }

        } catch (err) {
            console.error('Payment error:', err);
            onPaymentError(err.message || 'A apărut o eroare la procesarea plății.');
        } finally {
            setIsProcessing(false);
            onPaymentProcessing(false);
        }
    };

    if (!stripe) {
        return <div className="stripe-loading">Se încarcă Stripe...</div>;
    }

    return (
        <form onSubmit={handleSubmit} className="direct-stripe-form">
            <div className="card-form-group">
                <label htmlFor="card-name">Numele de pe card</label>
                <input
                    id="card-name"
                    type="text"
                    value={cardDetails.name}
                    onChange={(e) => handleInputChange('name', e.target.value)}
                    placeholder="Nume Prenume"
                    className={cardErrors.name ? 'error' : ''}
                />
                {cardErrors.name && <div className="error-text">{cardErrors.name}</div>}
            </div>

            <div className="card-form-group">
                <label htmlFor="card-number">Numărul cardului</label>
                <input
                    id="card-number"
                    type="text"
                    value={cardDetails.number}
                    onChange={(e) => handleInputChange('number', e.target.value)}
                    placeholder="1234 5678 9012 3456"
                    maxLength="19"
                    className={cardErrors.number ? 'error' : ''}
                />
                {cardErrors.number && <div className="error-text">{cardErrors.number}</div>}
            </div>

            <div className="card-form-row">
                <div className="card-form-group">
                    <label htmlFor="card-expiry">Data expirării</label>
                    <input
                        id="card-expiry"
                        type="text"
                        value={cardDetails.expiry}
                        onChange={(e) => handleInputChange('expiry', e.target.value)}
                        placeholder="MM/YY"
                        maxLength="5"
                        className={cardErrors.expiry ? 'error' : ''}
                    />
                    {cardErrors.expiry && <div className="error-text">{cardErrors.expiry}</div>}
                </div>

                <div className="card-form-group">
                    <label htmlFor="card-cvc">CVC</label>
                    <input
                        id="card-cvc"
                        type="text"
                        value={cardDetails.cvc}
                        onChange={(e) => handleInputChange('cvc', e.target.value)}
                        placeholder="123"
                        maxLength="4"
                        className={cardErrors.cvc ? 'error' : ''}
                    />
                    {cardErrors.cvc && <div className="error-text">{cardErrors.cvc}</div>}
                </div>
            </div>

            <button type="submit" disabled={isProcessing} className="finalize-button stripe-pay-button">
                {isProcessing ? 'Se procesează...' : `Plătește (${(amount / 100).toFixed(2)} RON)`}
            </button>
        </form>
    );
};

const StripePaymentPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [loading, setLoading] = useState(false);
    const [paymentMethod, setPaymentMethod] = useState('card');
    const [error, setError] = useState(null);
    const [hallDetails, setHallDetails] = useState({});

    const reservationData = location.state?.reservations || [];
    const city = location.state?.city || '';

    // Verifică autentificarea la încărcare
    useEffect(() => {
        const token = getAuthToken();
        if (!token) {
            alert('Nu sunteți autentificat. Vă rugăm să vă logați.');
            navigate('/login');
            return;
        }
    }, [navigate]);

    const fetchHallDetails = useCallback(async () => {
        if (reservationData.length === 0) return;

        try {
            const hallDetailsMap = {};

            for (const reservation of reservationData) {
                if (!hallDetailsMap[reservation.hallId]) {
                    const response = await fetch(`${API_BASE_URL}/sportsHalls/${reservation.hallId}`);
                    if (response.ok) {
                        const hallData = await response.json();
                        hallDetailsMap[reservation.hallId] = hallData;
                    } else {
                        console.error(`Eroare la obținerea detaliilor pentru sala ${reservation.hallId}`);
                        hallDetailsMap[reservation.hallId] = { tariff: 50 };
                    }
                }
            }

            setHallDetails(hallDetailsMap);
        } catch (error) {
            console.error('Eroare la obținerea detaliilor sălilor:', error);
            setError('Eroare la încărcarea detaliilor rezervării.');
        }
    }, [reservationData]);

    useEffect(() => {
        if (reservationData.length === 0 && !location.pathname.includes('/payment-success')) {
            alert('Nu există date de rezervare. Vă redirectăm la pagina principală.');
            navigate('/');
        } else {
            fetchHallDetails();
        }
    }, [reservationData, navigate, location.pathname, fetchHallDetails]);

    const calculateTotalPrice = () => {
        if (Object.keys(hallDetails).length === 0) {
            return 0;
        }

        let total = 0;
        reservationData.forEach(reservation => {
            const hall = hallDetails[reservation.hallId];
            if (hall && hall.tariff) {
                total += hall.tariff;
            } else {
                total += 50;
            }
        });

        return total;
    };

    const totalPrice = calculateTotalPrice();
    const totalPriceCents = totalPrice * 100;

    const handleCashPayment = async () => {
        const token = getAuthToken();
        if (!token) {
            setError('Nu sunteți autentificat. Vă rugăm să vă logați din nou.');
            return;
        }

        setLoading(true);
        setError(null);
        try {
            const response = await fetch(`${API_BASE_URL}/payment/confirm-payment-cash`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({
                    reservations: reservationData,
                    totalAmount: totalPrice
                })
            });

            if (response.status === 401 || response.status === 403) {
                setError('Nu sunteți autorizat. Vă rugăm să vă logați din nou.');
                return;
            }

            const result = await response.json();
            if (result.success) {
                navigate('/payment-success', {
                    state: {
                        reservations: reservationData,
                        totalAmount: totalPrice,
                        paymentMethod: 'cash',
                        paymentDetails: result
                    }
                });
            } else {
                setError(result.error || 'Eroare la crearea rezervării cu plată cash.');
            }
        } catch (err) {
            console.error('Cash payment error:', err);
            setError(err.message || 'A apărut o eroare la crearea rezervării cu plată cash.');
        } finally {
            setLoading(false);
        }
    };

    const handleStripePaymentSuccess = (paymentResult, method) => {
        navigate('/payment-success', {
            state: {
                reservations: reservationData,
                totalAmount: totalPrice,
                paymentMethod: method,
                paymentDetails: paymentResult
            }
        });
    };

    if (reservationData.length === 0 && !location.pathname.includes('/payment-success')) {
        return null;
    }

    if (Object.keys(hallDetails).length === 0 && reservationData.length > 0) {
        return (
            <div className="payment-page">
                <div className="payment-container">
                    <div className="stripe-loading">Se încarcă detaliile rezervării...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="payment-page">
            <div className="payment-container">
                <div className="payment-header">
                    <div className="light-line"></div>
                    <div className="light-line"></div>
                    <div className="light-line"></div>
                    <div className="light-line"></div>
                    <h1>Finalizare rezervare</h1>
                    <p>Completați plata pentru a confirma rezervarea</p>
                </div>

                <div className="payment-content">
                    <div className="reservation-summary">
                        <h2>Rezumat rezervare</h2>
                        <div className="summary-card">
                            <div className="summary-header">
                                <span><strong>Oraș:</strong> {city}</span>
                                <span><strong>Numărul de intervale:</strong> {reservationData.length}</span>
                            </div>
                            {reservationData.map((reservation, index) => {
                                const hall = hallDetails[reservation.hallId];
                                const price = hall?.tariff || 50;

                                return (
                                    <div key={index} className="reservation-item">
                                        <div className="reservation-details">
                                            <div><strong>Sala:</strong> {reservation.hallName}</div>
                                            <div><strong>Interval:</strong> {reservation.timeSlot}</div>
                                            <div className="full-width"><strong>Data:</strong> {reservation.dateFormatted}</div>
                                            <div className="full-width"><strong>Preț:</strong> {price.toFixed(2)} RON</div>
                                        </div>
                                    </div>
                                );
                            })}
                            <div className="total-section">
                                <span>Total de plată:</span>
                                <span className="total-amount">{totalPrice.toFixed(2)} RON</span>
                            </div>
                        </div>
                    </div>

                    <div className="payment-methods">
                        <h2>Metodă de plată</h2>
                        <div className="payment-options">
                            <label className={`payment-option ${paymentMethod === 'card' ? 'selected' : ''}`}>
                                <input
                                    type="radio"
                                    value="card"
                                    checked={paymentMethod === 'card'}
                                    onChange={() => setPaymentMethod('card')}
                                />
                                💳 Card de Credit/Debit (Stripe)
                            </label>
                            <label className={`payment-option ${paymentMethod === 'cash' ? 'selected' : ''}`}>
                                <input
                                    type="radio"
                                    value="cash"
                                    checked={paymentMethod === 'cash'}
                                    onChange={() => setPaymentMethod('cash')}
                                />
                                💵 Plată numerar
                            </label>
                        </div>

                        {error && <div className="error-message main-error">{error}</div>}

                        {paymentMethod === 'card' && (
                            <div className="stripe-form">
                                <div className="test-mode-notice">
                                    <strong>🧪 Mod Test Stripe:</strong> Folosiți cardurile de test furnizate de Stripe.
                                </div>
                                <StripePaymentMethodCheckout
                                    amount={totalPriceCents}
                                    reservationData={reservationData}
                                    onPaymentProcessing={setLoading}
                                    onPaymentSuccess={handleStripePaymentSuccess}
                                    onPaymentError={setError}
                                />
                                <div className="test-cards">
                                    <h4>Carduri de test Stripe (exemple comune):</h4>
                                    <ul>
                                        <li><strong>Visa (Recomandat):</strong> <code>4242 4242 4242 4242</code> - folosește token automat</li>
                                        <li><strong>Mastercard (Recomandat):</strong> <code>5555 5555 5555 4444</code> - folosește token automat</li>
                                        <li><strong>Visa Debit:</strong> <code>4000 0566 5566 5556</code></li>
                                        <li><strong>Mastercard Debit:</strong> <code>5200 8282 8282 8210</code></li>
                                        <li><strong>Exp:</strong> orice dată viitoare (ex: 12/25), <strong>CVC:</strong> orice 3 cifre (ex: 123)</li>
                                    </ul>
                                    <p><small>💡 Primele două carduri folosesc token-uri sigure și sunt recomandate pentru testare.</small></p>
                                </div>
                            </div>
                        )}

                        {paymentMethod === 'cash' && (
                            <div className="cash-payment-info">
                                <h3>Plată la Fața Locului</h3>
                                <p>
                                    Rezervarea va fi confirmată, iar plata se va efectua direct la sala de sport înainte de începerea activității.
                                    Vă rugăm să ajungeți cu 15 minute înainte de ora rezervată.
                                </p>
                                <button
                                    onClick={handleCashPayment}
                                    disabled={loading}
                                    className={`finalize-button cash-confirm-button ${loading ? 'loading' : ''}`}
                                >
                                    {loading ? 'Se procesează...' : `Confirmă Rezervarea (${totalPrice.toFixed(2)} RON)`}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StripePaymentPage;