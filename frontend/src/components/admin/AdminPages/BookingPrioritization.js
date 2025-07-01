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
            isLoadingStatus: false,
            generationProgress: '',
            timeoutWarning: false
        };

        // Nu mai avem generateTimeout
    }

    componentDidMount() {
        this.loadSystemStatus();
    }

    componentWillUnmount() {
    }

    // Încarcă statusul sistemului
    loadSystemStatus = async () => {
        try {
            this.setState({ isLoadingStatus: true });

            const response = await fetch(`${this.API_BASE_URL}/booking-prioritization/status`, {
                timeout: 10000 // 10 secunde timeout
            });

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

    handleGenerateBookings = async () => {
        try {
            this.setState({
                isGenerating: true,
                generationComplete: false,
                showDetailedResults: false,
                generationProgress: 'Se inițializează...',
                timeoutWarning: false
            });

            const progressUpdates = [
                { time: 10000, message: 'Se procesează profilurile...' },
                { time: 30000, message: 'Se generează rezervările...' },
                { time: 60000, message: 'Procesul continuă... încă se lucrează...' },
                { time: 90000, message: 'Aproape gata... se finalizează procesul...' }
            ];

            const progressTimeouts = progressUpdates.map(update =>
                setTimeout(() => {
                    if (this.state.isGenerating) {
                        this.setState({
                            generationProgress: update.message,
                            timeoutWarning: update.time >= 30000 // Show warning după 30s
                        });
                    }
                }, update.time)
            );

            const response = await fetch(`${this.API_BASE_URL}/booking-prioritization/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            progressTimeouts.forEach(timeout => clearTimeout(timeout));

            if (!response.ok) {
                throw new Error(`Serverul a răspuns cu codul: ${response.status}`);
            }

            let data;
            try {
                const text = await response.text();
                data = JSON.parse(text);
            } catch (parseError) {
                throw new Error('Response-ul serverului nu poate fi procesat');
            }

            if (data.success) {
                this.setState({
                    isGenerating: false,
                    generationComplete: true,
                    generationSuccess: true,
                    resultMessage: this.buildSuccessMessage(data),
                    generationResult: data,
                    generationProgress: '',
                    timeoutWarning: false
                });

                setTimeout(() => {
                    this.loadSystemStatus();
                }, 1000);

                if (this.props.onGenerationComplete) {
                    this.props.onGenerationComplete(true);
                }
            } else {
                throw new Error(data.message || 'A apărut o eroare la generarea rezervărilor.');
            }
        } catch (error) {
            console.error('Eroare la generarea rezervărilor:', error);

            let errorMessage = error.message;

            this.setState({
                isGenerating: false,
                generationComplete: true,
                generationSuccess: false,
                resultMessage: `Eroare la generarea rezervărilor: ${errorMessage}`,
                generationResult: null,
                generationProgress: '',
                timeoutWarning: false
            });

            if (this.props.onGenerationComplete) {
                this.props.onGenerationComplete(false, errorMessage);
            }
        }
    };

    buildSuccessMessage = (data) => {
        const { count, successfulProfiles } = data;

        let message = `${data.message} Au fost generate ${count} rezervări`;

        if (successfulProfiles > 0) {
            message += ` pentru ${successfulProfiles} profiluri de rezervare`;
        }

        message += '.';
        return message;
    };

    toggleDetailedResults = () => {
        this.setState(prevState => ({
            showDetailedResults: !prevState.showDetailedResults
        }));
    };

    handleMessageDismiss = () => {
        this.setState({
            generationComplete: false,
            resultMessage: '',
            generationResult: null,
            showDetailedResults: false,
            generationProgress: '',
            timeoutWarning: false
        });
    };

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

    renderGenerationProgress = () => {
        const { generationProgress, timeoutWarning } = this.state;

        if (!generationProgress) return null;

        return (
            <div className={`bp-progress-container ${timeoutWarning ? 'bp-warning' : ''}`}>
                <div className="bp-progress-content">
                    {timeoutWarning && <AlertTriangle size={16} />}
                    <span>{generationProgress}</span>
                </div>
                {timeoutWarning && (
                    <div className="bp-timeout-help">
                        <p>Procesul durează mai mult pentru că:</p>
                        <ul>
                            <li>Se procesează multe profiluri</li>
                            <li>Se caută disponibilități complexe</li>
                            <li>Se optimizează distribuția rezervărilor</li>
                        </ul>
                        <p><strong>Vă rugăm să așteptați până se termină.</strong></p>
                    </div>
                )}
            </div>
        );
    };

    renderDetailedResults = () => {
        const { generationResult } = this.state;

        if (!generationResult) return null;

        const { count, profileResults, successfulProfiles } = generationResult;
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
            </div>
        );
    };

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

                {/* Progress indicator */}
                {isGenerating && this.renderGenerationProgress()}

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