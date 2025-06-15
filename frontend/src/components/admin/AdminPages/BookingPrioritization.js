import React, { Component } from 'react';
import './BookingPrioritization.css';
import { Play, CheckCircle, XCircle, Info, AlertTriangle, Calendar, Users, MapPin } from 'lucide-react';

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
            showDetailedResults: false,
            systemStatus: null,
            isLoadingStatus: false
        };
    }

    componentDidMount() {
        this.loadSystemStatus();
    }

    // Încarcă statusul sistemului
    loadSystemStatus = async () => {
        try {
            this.setState({ isLoadingStatus: true });

            const response = await fetch(`${this.API_BASE_URL}/booking-prioritization/status`);

            if (response.ok) {
                const statusData = await response.json();
                this.setState({ systemStatus: statusData });
            }
        } catch (error) {
            console.error('Eroare la încărcarea statusului sistemului:', error);
        } finally {
            this.setState({ isLoadingStatus: false });
        }
    };

    // Funcția principală pentru generarea rezervărilor
    handleGenerateBookings = async () => {
        try {
            this.setState({
                isGenerating: true,
                generationComplete: false,
                showDetailedResults: false
            });

            // Nu mai este necesar token-ul JWT pentru că endpoint-ul este public
            const response = await fetch(`${this.API_BASE_URL}/booking-prioritization/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Serverul a răspuns cu codul: ${response.status}`);
            }

            const data = await response.json();

            if (data.success) {
                this.setState({
                    isGenerating: false,
                    generationComplete: true,
                    generationSuccess: true,
                    resultMessage: this.buildSuccessMessage(data),
                    generationResult: data
                });

                // Reîncarcă statusul sistemului după generare
                this.loadSystemStatus();

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

            if (this.props.onGenerationComplete) {
                this.props.onGenerationComplete(false, error.message);
            }
        }
    };

    // Construiește mesajul de succes simplificat (fără plăți)
    buildSuccessMessage = (data) => {
        const { count, profileResults } = data;
        const successfulProfiles = profileResults ? profileResults.filter(p => p.success).length : 0;

        let message = `${data.message} Au fost generate ${count} rezervări`;

        if (successfulProfiles > 0) {
            message += ` pentru ${successfulProfiles} profiluri de rezervare`;
        }

        message += '.';
        return message;
    };

    // Toggle pentru rezultatele detaliate
    toggleDetailedResults = () => {
        this.setState(prevState => ({
            showDetailedResults: !prevState.showDetailedResults
        }));
    };

    // Resetează starea
    handleMessageDismiss = () => {
        this.setState({
            generationComplete: false,
            resultMessage: '',
            generationResult: null,
            showDetailedResults: false
        });
    };

    // Render pentru statusul sistemului
    renderSystemStatus = () => {
        const { systemStatus, isLoadingStatus } = this.state;

        if (isLoadingStatus) {
            return (
                <div className="bp-status-card">
                    <div className="bp-status-loading">
                        <div className="bp-spinner-small"></div>
                        <span>Se încarcă statusul sistemului...</span>
                    </div>
                </div>
            );
        }

        if (!systemStatus) {
            return (
                <div className="bp-status-card bp-status-error">
                    <AlertTriangle size={16} />
                    <span>Nu s-a putut încărca statusul sistemului</span>
                </div>
            );
        }

        return (
            <div className="bp-status-card bp-status-ready">
                <div className="bp-status-header">
                    <CheckCircle size={16} className="bp-status-icon" />
                    <span>Sistem Pregătit</span>
                </div>
                <div className="bp-status-details">
                    <div className="bp-status-item">
                        <Users size={14} />
                        <span>{systemStatus.eligibleProfiles} / {systemStatus.totalProfiles} profiluri eligibile</span>
                    </div>
                    <div className="bp-status-item">
                        <MapPin size={14} />
                        <span>{systemStatus.activeHalls} săli active</span>
                    </div>
                    <div className="bp-status-item">
                        <Calendar size={14} />
                        <span>Săptămâna: {systemStatus.nextWeekStart} - {systemStatus.nextWeekEnd}</span>
                    </div>
                </div>
            </div>
        );
    };

    // Render pentru rezultatele detaliate
    renderDetailedResults = () => {
        const { generationResult } = this.state;

        if (!generationResult) return null;

        const { count, profileResults, reservations } = generationResult;
        const successfulProfiles = profileResults ? profileResults.filter(p => p.success).length : 0;
        const failedProfiles = profileResults ? profileResults.filter(p => !p.success).length : 0;

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
                            <Users className="bp-icon-info" size={24} />
                        </div>
                        <div className="bp-result-content">
                            <h5>Profiluri cu succes</h5>
                            <span className="bp-result-value">{successfulProfiles}</span>
                        </div>
                    </div>

                    {failedProfiles > 0 && (
                        <div className="bp-result-card">
                            <div className="bp-result-icon">
                                <XCircle className="bp-icon-error" size={24} />
                            </div>
                            <div className="bp-result-content">
                                <h5>Profiluri eșuate</h5>
                                <span className="bp-result-value">{failedProfiles}</span>
                            </div>
                        </div>
                    )}
                </div>

                {failedProfiles > 0 && (
                    <div className="bp-warning-notice">
                        <AlertTriangle size={18} />
                        <span>
                            {failedProfiles} profiluri nu au putut genera rezervări din cauza lipsei de disponibilitate sau buget insuficient.
                        </span>
                    </div>
                )}

                {/* Lista profilurilor procesate */}
                {profileResults && profileResults.length > 0 && (
                    <div className="bp-profiles-list">
                        <h5>Profiluri procesate ({profileResults.length})</h5>
                        <div className="bp-profiles-summary">
                            {this.renderProfilesSummary(profileResults)}
                        </div>
                    </div>
                )}

                {/* Lista rezervărilor generate */}
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

    // Render pentru rezumatul profilurilor
    renderProfilesSummary = (profileResults) => {
        return profileResults.map((result, index) => (
            <div key={index} className={`bp-profile-summary ${result.success ? 'bp-success' : 'bp-error'}`}>
                <div className="bp-profile-name">
                    {result.profile?.name || 'Profil necunoscut'}
                </div>
                <div className="bp-profile-result">
                    {result.success ? (
                        <span className="bp-success-text">
                            {result.reservations?.length || 0} rezervări
                        </span>
                    ) : (
                        <span className="bp-error-text">
                            {result.error || 'Eroare necunoscută'}
                        </span>
                    )}
                </div>
            </div>
        ));
    };

    // Render pentru rezumatul rezervărilor
    renderReservationsSummary = (reservations) => {
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

    // Metoda principală de render
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
                                Rezervările vor fi confirmate și utilizatorii vor primi email-uri de notificare.
                            </p>
                        </div>
                    </div>

                    {this.renderSystemStatus()}
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