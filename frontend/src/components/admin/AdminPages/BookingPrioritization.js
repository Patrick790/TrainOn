import React, { Component } from 'react';
import './BookingPrioritization.css';
import { Play } from 'lucide-react';

class BookingPrioritization extends Component {
    constructor(props) {
        super(props);
        this.state = {
            isGenerating: false,
            generationComplete: false,
            generationSuccess: false,
            resultMessage: ''
        };
    }

    // Funcția care va fi apelată la click pe butonul de generare
    handleGenerateBookings = async () => {
        try {
            // Setăm starea de generare în curs
            this.setState({ isGenerating: true, generationComplete: false });

            // Obținem token-ul JWT din localStorage
            const token = localStorage.getItem('jwtToken');

            if (!token) {
                throw new Error('Nu sunteți autentificat. Vă rugăm să vă autentificați pentru a continua.');
            }

            // Facem apelul către API pentru generarea rezervărilor
            const response = await fetch('http://localhost:8080/booking-prioritization/generate', {
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
                    resultMessage: `${data.message} Au fost generate ${data.count} rezervări.`
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
                resultMessage: `Eroare la generarea rezervărilor: ${error.message}`
            });

            // Notificăm componenta părinte despre eșec
            if (this.props.onGenerationComplete) {
                this.props.onGenerationComplete(false, error.message);
            }
        }
    };

    // Resetăm starea după ce utilizatorul a văzut mesajul
    handleMessageDismiss = () => {
        this.setState({
            generationComplete: false,
            resultMessage: ''
        });
    };

    // Metoda de randare a componentei
    render() {
        const { isGenerating, generationComplete, generationSuccess, resultMessage } = this.state;

        return (
            <div className="bp-booking-prioritization">
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
                    <div
                        className={`bp-result-message ${generationSuccess ? 'bp-success' : 'bp-error'}`}
                        onClick={this.handleMessageDismiss}
                    >
                        {resultMessage}
                    </div>
                )}
            </div>
        );
    }
}

export default BookingPrioritization;