import React, { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './StripePaymentSuccessPage.css';

const StripePaymentSuccessPage = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const {
        reservations: reservationData = [],
        totalAmount = 0,
        paymentMethod = 'N/A',
        paymentDetails = {}
    } = location.state || {};

    useEffect(() => {
        if (reservationData.length === 0) {
            alert('Detaliile rezervării nu au fost găsite. Redirectare...');
            navigate('/');
        }
    }, [reservationData, navigate]);

    if (reservationData.length === 0) {
        return (
            <div className="payment-success-page">
                <div className="success-container">
                    <div className="loading-section">Se încarcă...</div>
                </div>
            </div>
        );
    }

    const displayTransactionId = paymentMethod === 'card' ?
        (paymentDetails?.stripePaymentIntentId || paymentDetails?.transactionId) :
        paymentDetails?.paymentId;

    const currentDate = new Date().toLocaleDateString('ro-RO', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });

    return (
        <div className="payment-success-page">
            <div className="success-container">
                <div className="document-header">
                    <div className="company-header">
                        <h1 className="company-name">TRAINON</h1>
                        <p className="company-subtitle">Platformă Rezervări Sportive</p>
                    </div>
                    <div className="document-info">
                        <p className="document-type">CONFIRMAREA REZERVĂRII</p>
                        <p className="document-date">Data emiterii: {currentDate}</p>
                    </div>
                </div>

                <div className="status-section">
                    <div className="status-indicator success">
                        <div className="status-icon"></div>
                        <div className="status-content">
                            <h2 className="status-title">Rezervare Confirmată</h2>
                            <p className="status-message">
                                {paymentMethod === 'cash' ?
                                    'Rezervarea dumneavoastră a fost înregistrată cu succes în sistemul nostru.' :
                                    'Plata a fost procesată cu succes și rezervarea este confirmată.'
                                }
                            </p>
                        </div>
                    </div>
                </div>

                <div className="content-section">
                    <div className="section-block">
                        <h3 className="section-title">Detalii Rezervare</h3>
                        <div className="details-table">
                            <div className="table-header">
                                <div className="col-header">Sala de Sport</div>
                                <div className="col-header">Data</div>
                                <div className="col-header">Interval Orar</div>
                                <div className="col-header">Tarif</div>
                            </div>
                            {reservationData.map((reservation, index) => (
                                <div key={index} className="table-row">
                                    <div className="table-cell">{reservation.hallName}</div>
                                    <div className="table-cell">{reservation.dateFormatted}</div>
                                    <div className="table-cell">{reservation.timeSlot}</div>
                                    <div className="table-cell">{(totalAmount / reservationData.length).toFixed(2)} RON</div>
                                </div>
                            ))}
                            <div className="table-footer">
                                <div className="total-label">Total de plată:</div>
                                <div className="total-amount">{totalAmount.toFixed(2)} RON</div>
                            </div>
                        </div>
                    </div>

                    <div className="section-block">
                        <h3 className="section-title">Informații Tranzacție</h3>
                        <div className="info-grid">
                            <div className="info-row">
                                <span className="info-label">Metodă de plată:</span>
                                <span className="info-value">
                                    {paymentMethod === 'card' ? 'Card de Credit/Debit' :
                                        paymentMethod === 'cash' ? 'Plată Numerar' : paymentMethod}
                                </span>
                            </div>
                            {displayTransactionId && (
                                <div className="info-row">
                                    <span className="info-label">ID Tranzacție:</span>
                                    <span className="info-value transaction-id">{displayTransactionId}</span>
                                </div>
                            )}
                            {paymentDetails?.reservationIds && (
                                <div className="info-row">
                                    <span className="info-label">ID-uri Rezervări:</span>
                                    <span className="info-value">{paymentDetails.reservationIds.join(', ')}</span>
                                </div>
                            )}
                            <div className="info-row">
                                <span className="info-label">Status:</span>
                                <span className="info-value status-confirmed">Confirmat</span>
                            </div>
                        </div>
                    </div>

                    <div className="section-block">
                        <h3 className="section-title">Instrucțiuni Importante</h3>
                        <div className="instructions-list">
                            <div className="instruction-item">
                                <span className="instruction-number">1</span>
                                <span className="instruction-text">Prezentați-vă cu 15 minute înainte de ora rezervată</span>
                            </div>
                            <div className="instruction-item">
                                <span className="instruction-number">2</span>
                                <span className="instruction-text">Asigurați-vă că aveți echipamentul sportiv necesar</span>
                            </div>
                            <div className="instruction-item">
                                <span className="instruction-number">3</span>
                                <span className="instruction-text">Întârzierile mai mari de 15 minute pot rezulta în anularea rezervării</span>
                            </div>
                            {paymentMethod === 'cash' && (
                                <div className="instruction-item cash-note">
                                    <span className="instruction-number">!</span>
                                    <span className="instruction-text">
                                        <strong>Atenție:</strong> Plata se va efectua la fața locului înainte de începerea activității
                                    </span>
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="section-block">
                        <h3 className="section-title">Contact și Suport</h3>
                        <div className="contact-info">
                            <p>Pentru modificări, anulări sau întrebări, vă rugăm să ne contactați prin:</p>
                            <ul className="contact-list">
                                <li>Platforma online TrainOn</li>
                                <li>Secțiunea "Contul Meu" → "Rezervările Mele"</li>
                                <li>Program de funcționare: Luni - Duminică, 08:00 - 22:00</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div className="action-section no-print">
                    <button onClick={() => navigate('/')} className="btn btn-primary">
                        Înapoi la Pagina Principală
                    </button>
                    <button onClick={() => window.print()} className="btn btn-secondary">
                        Printează Confirmarea
                    </button>
                </div>

                <div className="document-footer">
                    <div className="footer-line"></div>
                    <p className="footer-text">
                        Acest document a fost generat automat de sistemul TrainOn în data de {currentDate}
                    </p>
                    <p className="footer-note">
                        Păstrați această confirmare pentru evidențele dumneavoastră
                    </p>
                </div>
            </div>
        </div>
    );
};

export default StripePaymentSuccessPage;