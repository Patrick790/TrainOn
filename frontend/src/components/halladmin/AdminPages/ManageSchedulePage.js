import React, { Component } from 'react';
import { Info, Settings, Calendar } from 'lucide-react';
import ScheduleManagement from './ScheduleManagement';
import './ManageSchedulePage.css';

class ManageSchedulePage extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';
        this.state = {
            halls: [],
            selectedHallId: null,
            activeTab: 'schedule', // 'schedule' sau 'reservations'
            initialLoading: true,
            error: null,
            successMessage: '',
            errorMessage: '',
            showMessage: false
        };
    }

    async componentDidMount() {
        await this.fetchHalls();
    }

    // API calls
    fetchHalls = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                this.setState({
                    error: 'Nu sunteți autentificat corect',
                    initialLoading: false,
                    halls: []
                });
                return;
            }

            const response = await fetch(`${this.API_BASE_URL}/sportsHalls/admin/${userId}`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();
            console.log('Fetched halls:', halls); // Pentru debugging
            const selectedHallId = halls.length > 0 ? halls[0].id : null;
            console.log('Auto-selected hall ID:', selectedHallId); // Pentru debugging

            this.setState({
                halls,
                selectedHallId,
                initialLoading: false
            });
        } catch (error) {
            console.error('Error fetching halls:', error);
            this.setState({
                error: error.message,
                initialLoading: false,
                halls: []
            });
        }
    }

    // Event handlers
    handleHallChange = (event) => {
        const hallId = parseInt(event.target.value);
        console.log('Selected hall ID:', hallId); // Pentru debugging
        this.setState({
            selectedHallId: hallId,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        });
    }

    handleTabChange = (tab) => {
        this.setState({
            activeTab: tab,
            errorMessage: '',
            successMessage: '',
            showMessage: false
        });
    }

    render() {
        const {
            halls,
            selectedHallId,
            activeTab,
            initialLoading,
            error,
            successMessage,
            errorMessage,
            showMessage
        } = this.state;

        if (initialLoading) {
            return <div className="msp-loading-container">Se încarcă sălile...</div>;
        }

        if (error && halls.length === 0) {
            return <div className="msp-error-container">Eroare: {error}</div>;
        }

        const selectedHall = halls.find(hall => hall.id === parseInt(selectedHallId));

        return (
            <div className="msp-manage-schedule-page">
                <h1 className="msp-page-title">Gestionare Săli de Sport</h1>

                {halls.length === 0 ? (
                    <div className="msp-empty-state">
                        <p>Nu aveți săli de sport adăugate. Adăugați prima sală din secțiunea "Adaugă Sală Nouă".</p>
                    </div>
                ) : (
                    <>
                        {showMessage && (successMessage || errorMessage) && (
                            <div className={`msp-notification-message ${successMessage ? 'msp-success' : 'msp-error'}`}>
                                {successMessage ? successMessage : errorMessage}
                            </div>
                        )}

                        <div className="msp-schedule-controls">
                            <div className="msp-hall-selector">
                                <label htmlFor="hall-select">Selectați sala:</label>
                                <select
                                    id="hall-select"
                                    value={selectedHallId || ''}
                                    onChange={this.handleHallChange}
                                    className="msp-hall-select-input"
                                >
                                    {halls.map(hall => (
                                        <option key={hall.id} value={hall.id}>
                                            {hall.name}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {selectedHall && (
                                <div className="msp-selected-hall-info">
                                    <span className="msp-selected-hall-name">{selectedHall.name}</span>
                                    <span className="msp-selected-hall-location">{selectedHall.city}, {selectedHall.county}</span>
                                </div>
                            )}
                        </div>

                        {/* Tabs pentru navigare */}
                        <div className="msp-tabs-container">
                            <div className="msp-tabs">
                                <button
                                    className={`msp-tab ${activeTab === 'schedule' ? 'msp-tab-active' : ''}`}
                                    onClick={() => this.handleTabChange('schedule')}
                                >
                                    <Settings size={18} />
                                    Program Sală
                                </button>
                                <button
                                    className={`msp-tab ${activeTab === 'reservations' ? 'msp-tab-active' : ''}`}
                                    onClick={() => this.handleTabChange('reservations')}
                                >
                                    <Calendar size={18} />
                                    Rezervări & Mentenanță
                                </button>
                            </div>
                        </div>

                        {/* Content bazat pe tab-ul activ */}
                        <div className="msp-tab-content">
                            {activeTab === 'schedule' && (
                                <ScheduleManagement
                                    selectedHallId={selectedHallId ? parseInt(selectedHallId) : null}
                                />
                            )}

                            {activeTab === 'reservations' && (
                                <div className="msp-reservations-placeholder">
                                    <div className="bg-white rounded-lg p-8 shadow-sm border text-center">
                                        <Calendar size={48} className="text-gray-400 mx-auto mb-4" />
                                        <h3 className="text-lg font-semibold text-gray-900 mb-2">
                                            Managementul Rezervărilor
                                        </h3>
                                        <p className="text-gray-600 mb-4">
                                            Aici veți putea vizualiza și gestiona rezervările, precum și să programați intervale de mentenanță.
                                        </p>
                                        <div className="bg-blue-50 p-4 rounded-lg">
                                            <p className="text-sm text-blue-800">
                                                <Info size={16} className="inline mr-1" />
                                                Pentru a vedea rezervările, mai întâi definiți programul sălii în tab-ul "Program Sală".
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    </>
                )}
            </div>
        );
    }
}

export default ManageSchedulePage;