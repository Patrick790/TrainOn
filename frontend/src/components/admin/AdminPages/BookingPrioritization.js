import React, { Component } from 'react';
import './BookingPrioritization.css';
import { Play, CreditCard, Banknote, CheckCircle, XCircle, Info, AlertTriangle } from 'lucide-react';

class BookingPrioritization extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';
        this.state = {
            isGenerating: false,
            generationComplete: false,
            generationSuccess: false,
            resultMessage: '',
            generationResult: null,
            showDetailedResults: false
        };
    }

    // Funcția care va fi apelată la click pe butonul de generare
    handleGenerateBookings = async () => {
        try {
            // Setăm starea de generare în curs
            this.setState({
                isGenerating: true,
                generationComplete: false,
                showDetailedResults: false
            });

            // Obținem token-ul JWT din localStorage
            const token = localStorage.getItem('jwtToken');

            if (!token) {
                throw new Error('Nu sunteți autentificat. Vă rugăm să vă autentificați pentru a continua.');
            }

            // Facem apelul către API pentru generarea rezervărilor
            const response = await fetch(`${this.API_BASE_URL}/booking-prioritization/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            // Verificăm dacă răspunsul este de succes
            if (!response.ok) {
                throw new Error(`Serverul a răspuns cu codul: ${response.status}`);
            }

            // Parsăm răspunsul JSON
            const data = await response.json();

            // Verificăm dacă operația a fost de succes
            if (data.success) {
                // Actualizăm starea cu rezultatul
                this.setState({
                    isGenerating: false,
                    generationComplete: true,
                    generationSuccess: true,
                    resultMessage: this.buildSuccessMessage(data),
                    generationResult: data
                });

                // Notificăm componenta părinte despre generarea cu succes
                if (this.props.onGenerationComplete) {
                    this.props.onGenerationComplete(true);
                }
            } else {
                throw new Error(data.message || 'A apărut o eroare la generarea rezervărilor.');
            }
        } catch (error) {
            console.error('Eroare la generarea rezervărilor:', error);
            this.setState({
                isGenerating: false,
                generationComplete: true,
                generationSuccess: false,
                resultMessage: `Eroare la generarea rezervărilor: ${error.message}`,
                generationResult: null
            });

            // Notificăm componenta părinte despre eșec
            if (this.props.onGenerationComplete) {
                this.props.onGenerationComplete(false, error.message);
            }
        }
    };

    // Construiește mesajul de succes cu detalii despre plăți
    buildSuccessMessage = (data) => {
        const { count, paymentsCount, autoPaymentsSuccessful, autoPaymentsFailed } = data;

        let message = `${data.message} Au fost generate ${count} rezervări`;

        if (paymentsCount > 0) {
            message += ` cu ${paymentsCount} plăți procesate`;

            if (autoPaymentsSuccessful > 0 || autoPaymentsFailed > 0) {
                message += ` (${autoPaymentsSuccessful} plăți automate reușite`;
                if (autoPaymentsFailed > 0) {
                    message += `, ${autoPaymentsFailed} eșuate`;
                }
                message += ')';
            }
        }

        message += '.';
        return message;
    };

    // Toggle pentru afișarea rezultatelor detaliate
    toggleDetailedResults = () => {
        this.setState(prevState => ({
            showDetailedResults: !prevState.showDetailedResults
        }));
    };

    // Resetăm starea după ce utilizatorul a văzut mesajul
    handleMessageDismiss = () => {
        this.setState({
            generationComplete: false,
            resultMessage: '',
            generationResult: null,
            showDetailedResults: false
        });
    };

    // Render pentru rezultatele detaliate
    renderDetailedResults = () => {
        const { generationResult } = this.state;

        if (!generationResult) return null;

        const {
            count,
            paymentsCount,
            autoPaymentsSuccessful,
            autoPaymentsFailed,
            reservations,
            payments
        } = generationResult;

        return (
            <div className="bp-detailed-results">
                <div className="bp-results-header">
                    <h4>Rezultate detaliate</h4>
                    <button
                        className="bp-close-details"
                        onClick={this.toggleDetailedResults}
                        aria-label="Închide detaliile"
                    >
                        ×
                    </button>
                </div>

                <div className="bp-results-grid">
                    <div className="bp-result-card">
                        <div className="bp-result-icon">
                            <CheckCircle className="bp-icon-success" size={24} />
                        </div>
                        <div className="bp-result-content">
                            <h5>Rezervări generate</h5>
                            <span className="bp-result-value">{count}</span>
                        </div>
                    </div>

                    <div className="bp-result-card">
                        <div className="bp-result-icon">
                            <CreditCard className="bp-icon-info" size={24} />
                        </div>
                        <div className="bp-result-content">
                            <h5>Plăți procesate</h5>
                            <span className="bp-result-value">{paymentsCount}</span>
                        </div>
                    </div>

                    {(autoPaymentsSuccessful > 0 || autoPaymentsFailed > 0) && (
                        <>
                            <div className="bp-result-card">
                                <div className="bp-result-icon">
                                    <CheckCircle className="bp-icon-success" size={24} />
                                </div>
                                <div className="bp-result-content">
                                    <h5>Plăți automate reușite</h5>
                                    <span className="bp-result-value">{autoPaymentsSuccessful}</span>
                                </div>
                            </div>

                            {autoPaymentsFailed > 0 && (
                                <div className="bp-result-card">
                                    <div className="bp-result-icon">
                                        <XCircle className="bp-icon-error" size={24} />
                                    </div>
                                    <div className="bp-result-content">
                                        <h5>Plăți automate eșuate</h5>
                                        <span className="bp-result-value">{autoPaymentsFailed}</span>
                                    </div>
                                </div>
                            )}
                        </>
                    )}
                </div>

                {autoPaymentsFailed > 0 && (
                    <div className="bp-warning-notice">
                        <AlertTriangle size={18} />
                        <span>
                            Unele plăți automate au eșuat. Rezervările respective vor fi plătite la fața locului.
                        </span>
                    </div>
                )}

                {/* Opțional: Lista rezervărilor generate */}
                {reservations && reservations.length > 0 && (
                    <div className="bp-reservations-list">
                        <h5>Rezervări generate ({reservations.length})</h5>
                        <div className="bp-reservations-summary">
                            {this.renderReservationsSummary(reservations)}
                        </div>
                    </div>
                )}
            </div>
        );
    };

    // Render pentru rezumatul rezervărilor
    renderReservationsSummary = (reservations) => {
        // Grupează rezervările după sală
        const groupedByHall = reservations.reduce((acc, reservation) => {
            const hallName = reservation.hall?.name || 'Sală necunoscută';
            if (!acc[hallName]) {
                acc[hallName] = [];
            }
            acc[hallName].push(reservation);
            return acc;
        }, {});

        return Object.entries(groupedByHall).map(([hallName, hallReservations]) => (
            <div key={hallName} className="bp-hall-summary">
                <div className="bp-hall-name">{hallName}</div>
                <div className="bp-hall-count">{hallReservations.length} rezervări</div>
            </div>
        ));
    };

    // Metoda de randare a componentei
    render() {
        const {
            isGenerating,
            generationComplete,
            generationSuccess,
            resultMessage,
            generationResult,
            showDetailedResults
        } = this.state;

        return (
            <div className="bp-booking-prioritization">
                <div className="bp-header">
                    <div className="bp-info-card">
                        <div className="bp-info-icon">
                            <Info size={20} />
                        </div>
                        <div className="bp-info-content">
                            <h3>Generare automată rezervări</h3>
                            <p>
                                Sistemul va genera automat rezervările pentru săptămâna următoare
                                bazându-se pe profilurile de rezervare și prioritățile stabilite.
                                Plățile vor fi procesate automat pentru profilurile configurate.
                            </p>
                        </div>
                    </div>

                    <div className="bp-payment-info">
                        <div className="bp-payment-method">
                            <CreditCard size={16} />
                            <span>Plăți automate pentru profilurile configurate</span>
                        </div>
                        <div className="bp-payment-method">
                            <Banknote size={16} />
                            <span>Plăți cash pentru celelalte rezervări</span>
                        </div>
                    </div>
                </div>

                <div className="bp-generate-button-container">
                    <button
                        className={`bp-generate-button ${isGenerating ? 'bp-generating' : ''}`}
                        onClick={this.handleGenerateBookings}
                        disabled={isGenerating}
                    >
                        {isGenerating ? (
                            <>
                                <div className="bp-spinner"></div>
                                Se generează rezervările...
                            </>
                        ) : (
                            <>
                                <Play size={18} />
                                Generează rezervările
                            </>
                        )}
                    </button>
                </div>

                {generationComplete && (
                    <div className="bp-result-container">
                        <div
                            className={`bp-result-message ${generationSuccess ? 'bp-success' : 'bp-error'}`}
                        >
                            <div className="bp-result-main">
                                <div className="bp-result-icon">
                                    {generationSuccess ? (
                                        <CheckCircle size={20} />
                                    ) : (
                                        <XCircle size={20} />
                                    )}
                                </div>
                                <span className="bp-result-text">{resultMessage}</span>
                                <div className="bp-result-actions">
                                    {generationSuccess && generationResult && (
                                        <button
                                            className="bp-details-button"
                                            onClick={this.toggleDetailedResults}
                                        >
                                            {showDetailedResults ? 'Ascunde detaliile' : 'Vezi detaliile'}
                                        </button>
                                    )}
                                    <button
                                        className="bp-dismiss-button"
                                        onClick={this.handleMessageDismiss}
                                    >
                                        Închide
                                    </button>
                                </div>
                            </div>
                        </div>

                        {showDetailedResults && this.renderDetailedResults()}
                    </div>
                )}
            </div>
        );
    }
}

export default BookingPrioritization;