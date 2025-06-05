import React, { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './StripePaymentSuccesPage.css';

const StripePaymentSuccessPage = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const {
        reservations: reservationData = [],
        totalAmount = 0,
        paymentMethod = 'N/A',
        paymentDetails = {} // Va conține paymentId, reservationIds, și pt card transactionId (stripePI)
    } = location.state || {};


    useEffect(() => {
        if (reservationData.length === 0) {
            // Dacă nu avem date în state, poate a fost un refresh sau acces direct
            // Considerați o strategie mai bună aici, poate stocare temporară sau un mesaj generic
            alert('Detaliile rezervării nu au fost găsite. Redirectare...');
            navigate('/');
        }
    }, [reservationData, navigate]);

    if (reservationData.length === 0) {
        // Poate afișa un spinner mic sau null până la redirectare
        return <div className="payment-success-page"><div className="success-container"><div className="loading-section">Se încarcă...</div></div></div>;
    }

    // Extragem ID-ul tranzacției Stripe dacă e plată cu cardul
    const displayTransactionId = paymentMethod === 'card' ? (paymentDetails?.stripePaymentIntentId || paymentDetails?.transactionId) : paymentDetails?.paymentId;


    return (
        <div className="payment-success-page">
            <div className="success-container">
                <div className="success-content">
                    <div className="success-icon">✅</div>
                    <h1>Rezervare Confirmată!</h1>
                    <p className="success-message">
                        {paymentMethod === 'cash' ?
                            'Rezervarea dumneavoastră a fost înregistrată cu succes.' :
                            'Plata a fost procesată cu succes.'
                        }
                    </p>

                    <div className="reservation-details">
                        <h3>Detalii Rezervare</h3>
                        <div className="details-card">
                            {reservationData.map((reservation, index) => (
                                <div key={index} className="reservation-item">
                                    <div className="item-row">
                                        <strong>Sala:</strong> {reservation.hallName}
                                    </div>
                                    <div className="item-row">
                                        <strong>Data:</strong> {reservation.dateFormatted}
                                    </div>
                                    <div className="item-row">
                                        <strong>Interval:</strong> {reservation.timeSlot}
                                    </div>
                                    {index < reservationData.length - 1 && <hr className="item-separator" />}
                                </div>
                            ))}
                            <div className="total-row">
                                <strong>Total plătit: {totalAmount} RON</strong>
                                {displayTransactionId && (
                                    <div className="transaction-id">
                                        <small>ID {paymentMethod === 'card' ? 'Tranzacție Stripe' : 'Plată Internă'}: {displayTransactionId}</small>
                                    </div>
                                )}
                                {paymentDetails?.reservationIds && (
                                    <div className="transaction-id">
                                        <small>ID-uri Rezervări: {paymentDetails.reservationIds.join(', ')}</small>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="important-info">
                        <h3>Informații Importante</h3>
                        <ul>
                            <li>Vă rugăm să ajungeți cu 15 minute înainte de ora rezervată.</li>
                            <li>Aveți nevoie de echipament sportiv adecvat.</li>
                            <li>În caz de întârziere mai mare de 15 minute, rezervarea poate fi anulată.</li>
                            {paymentMethod === 'cash' && (
                                <li><strong>Atenție:</strong> Plata se va efectua la fața locului înainte de activitate.</li>
                            )}
                        </ul>
                    </div>

                    <div className="action-buttons">
                        <button onClick={() => navigate('/')} className="primary-button">
                            Înapoi la Pagina Principală
                        </button>
                        <button onClick={() => window.print()} className="secondary-button">
                            Printează Confirmarea
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StripePaymentSuccessPage;