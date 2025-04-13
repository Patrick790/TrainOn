import React, { Component } from 'react';
import { Star, User } from 'lucide-react';
import './ViewAllHallsPage.css';
import SportsHallDetailsModal from './SportsHallDetailsModal';


class ViewAllHallsPage extends Component {
    constructor(props) {
        super(props);
        this.state = {
            halls: [],
            cities: [],
            admins: {},
            selectedCity: 'all',
            loading: true,
            error: null,
            modalOpen: false,
            selectedHallId: null
        };
    }

    async componentDidMount() {
        await this.fetchHalls();
    }

    fetchHalls = async () => {
        try {
            const token = localStorage.getItem('jwtToken');

            if (!token) {
                this.setState({
                    error: 'Nu sunteți autentificat corect',
                    loading: false,
                    halls: []
                });
                return;
            }

            const response = await fetch('http://localhost:8080/sportsHalls', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();

            // Extragem toate orașele unice pentru filtrare
            const uniqueCities = [...new Set(halls.map(hall => hall.city))];

            this.setState({
                halls,
                cities: uniqueCities,
                loading: false
            });

            // După ce avem sălile, obținem informațiile administratorilor
            await this.fetchAdminsData(halls);

        } catch (error) {
            console.error('Error fetching halls:', error);
            this.setState({
                error: error.message,
                loading: false,
                halls: []
            });
        }
    }

    fetchAdminsData = async (halls) => {
        try {
            const token = localStorage.getItem('jwtToken');
            const uniqueAdminIds = [...new Set(halls.map(hall => hall.adminId))].filter(id => id != null);
            const adminsData = {};

            // Pentru fiecare admin ID, facem un fetch pentru a obține datele
            for (const adminId of uniqueAdminIds) {
                try {
                    const response = await fetch(`http://localhost:8080/users/${adminId}`, {
                        method: 'GET',
                        headers: {
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json'
                        }
                    });

                    if (response.ok) {
                        const adminData = await response.json();
                        console.log('Admin data fetched:', adminData);
                        adminsData[adminId] = adminData;
                    } else {
                        console.warn(`Nu s-au putut obține datele pentru admin ID: ${adminId}`);
                        adminsData[adminId] = { name: 'Necunoscut' };
                    }
                } catch (err) {
                    console.error(`Eroare la obținerea datelor pentru admin ID: ${adminId}`, err);
                    adminsData[adminId] = { name: 'Necunoscut' };
                }
            }

            this.setState({ admins: adminsData });
            console.log('Admins data state:', adminsData);

        } catch (error) {
            console.error('Error fetching admins data:', error);
        }
    }

    getCoverImage = (hall) => {
        if (hall.images && hall.images.length > 0) {
            const coverImage = hall.images.find(img => img.description === 'cover');
            if (coverImage) {
                return `http://localhost:8080/images/${coverImage.id}`;
            }
        }
        return null;
    }

    getAdminName = (adminId) => {
        const { admins } = this.state;
        if (!adminId) return 'Necunoscut';

        if (admins[adminId]) {
            const admin = admins[adminId];
            return admin.name || 'Necunoscut';
        }
        return 'Necunoscut';
    }

    getAdminEmail = (adminId) => {
        const { admins } = this.state;
        if (!adminId || !admins[adminId]) return '';

        return admins[adminId].email || '';
    }

    handleCityChange = (e) => {
        this.setState({ selectedCity: e.target.value });
    }

    handleViewDetails = (hallId) => {
        this.setState({
            modalOpen: true,
            selectedHallId: hallId
        });
    }

    handleCloseModal = () => {
        this.setState({
            modalOpen: false,
            selectedHallId: null
        });
    }

    handleManage = (hallId) => {
        // Funcția pentru gestionarea sălii
        console.log(`Gestionare sală cu ID: ${hallId}`);
    }

    renderRatingStars = (rating) => {
        const totalStars = 5;
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.3 && rating % 1 <= 0.7;
        const hasAlmostFullStar = rating % 1 > 0.7;

        return (
            <div className="global-hall-rating-stars">
                {[...Array(totalStars)].map((_, index) => {
                    // Pentru stele complete
                    if (index < fullStars) {
                        return (
                            <Star
                                key={index}
                                size={18}
                                fill="#FFD700"
                                color="#FFD700"
                                className="global-star-icon"
                            />
                        );
                    }
                    // Pentru jumătate de stea
                    else if (index === fullStars && hasHalfStar) {
                        return (
                            <div className="global-half-star-container" key={index}>
                                <Star
                                    size={18}
                                    fill="#FFD700"
                                    color="#FFD700"
                                    className="global-half-star"
                                />
                                <Star
                                    size={18}
                                    fill="none"
                                    color="#D1D5DB"
                                    className="global-star-icon global-star-background"
                                />
                            </div>
                        );
                    }
                    // Pentru aproape o stea completa (peste 0.7)
                    else if (index === fullStars && hasAlmostFullStar) {
                        return (
                            <Star
                                key={index}
                                size={18}
                                fill="#FFD700"
                                color="#FFD700"
                                className="global-star-icon"
                            />
                        );
                    }
                    // Pentru stele goale
                    else {
                        return (
                            <Star
                                key={index}
                                size={18}
                                fill="none"
                                color="#D1D5DB"
                                className="global-star-icon"
                            />
                        );
                    }
                })}
            </div>
        );
    }

    render() {
        const { halls, cities, selectedCity, loading, error, modalOpen, selectedHallId } = this.state;

        if (loading) {
            return (
                <div className="global-admin-page">
                    <h2 className="global-admin-page-title">Vizualizarea tuturor sălilor</h2>
                    <div className="global-admin-loading">Se încarcă sălile...</div>
                </div>
            );
        }

        if (error) {
            return (
                <div className="global-admin-page">
                    <h2 className="global-admin-page-title">Vizualizarea tuturor sălilor</h2>
                    <div className="global-admin-error">{error}</div>
                </div>
            );
        }

        // Filtrăm sălile în funcție de orașul selectat
        const filteredHalls = selectedCity === 'all'
            ? halls
            : halls.filter(hall => hall.city === selectedCity);

        return (
            <div className="global-admin-page">
                <h2 className="global-admin-page-title">Vizualizarea tuturor sălilor</h2>

                <div className="global-admin-filters">
                    <div className="global-admin-filter-group">
                        <label htmlFor="city-filter">Filtrează după oraș:</label>
                        <select
                            id="city-filter"
                            className="global-admin-select"
                            value={selectedCity}
                            onChange={this.handleCityChange}
                        >
                            <option value="all">Toate orașele</option>
                            {cities.map(city => (
                                <option key={city} value={city}>{city}</option>
                            ))}
                        </select>
                    </div>
                </div>

                <div className="global-halls-grid">
                    {filteredHalls.length > 0 ? (
                        filteredHalls.map(hall => {
                            const coverImageUrl = this.getCoverImage(hall);
                            const adminName = this.getAdminName(hall.adminId);
                            const adminEmail = this.getAdminEmail(hall.adminId);

                            return (
                                <div key={hall.id} className="global-hall-card">
                                    <div className="global-hall-image-container">
                                        <div className="global-hall-status-indicator active">
                                            Activ
                                        </div>

                                        {coverImageUrl ? (
                                            <img
                                                src={coverImageUrl}
                                                alt={hall.name}
                                                className="global-hall-image"
                                            />
                                        ) : (
                                            <div className="global-hall-placeholder">
                                                <div className="global-hall-initial">{hall.name.charAt(0)}</div>
                                            </div>
                                        )}
                                    </div>

                                    <div className="global-hall-content">
                                        <h3 className="global-hall-name">{hall.name}</h3>
                                        <p className="global-hall-location">{hall.city}, {hall.county}</p>

                                        {/* Adăugăm numele administratorului cu email */}
                                        <div className="global-hall-admin-container">
                                            <div className="global-hall-admin-row">
                                                <User size={16} className="global-admin-icon" />
                                                <span className="global-admin-label">Administrator:</span>
                                                <span className="global-admin-name">{adminName}</span>
                                            </div>
                                            {adminEmail && (
                                                <div className="global-hall-admin-email">
                                                    {adminEmail}
                                                </div>
                                            )}
                                        </div>

                                        <p className="global-hall-description">{hall.description || 'Fără descriere'}</p>

                                        {/* Temporar, până când avem ratings reale */}
                                        <div className="global-hall-rating">
                                            {this.renderRatingStars(0)}
                                            <span className="global-rating-text">
                                                0 (0 recenzii)
                                            </span>
                                        </div>

                                        <div className="global-hall-actions">
                                            <button
                                                className="global-admin-action-button"
                                                onClick={() => this.handleViewDetails(hall.id)}
                                            >
                                                Detalii
                                            </button>
                                            <button
                                                className="global-admin-action-button primary"
                                                onClick={() => this.handleManage(hall.id)}
                                            >
                                                Gestionare
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            );
                        })
                    ) : (
                        <div className="global-admin-empty-state">
                            <p>Nu există săli de sport {selectedCity !== 'all' ? `în orașul ${selectedCity}` : 'înregistrate în platformă'}.</p>
                        </div>
                    )}
                </div>

                {modalOpen && selectedHallId && (
                    <SportsHallDetailsModal
                        isOpen={modalOpen}
                        hallId={selectedHallId}
                        onClose={this.handleCloseModal}
                    />
                )}
            </div>
        );
    }
}

export default ViewAllHallsPage;