import React, { Component } from 'react';
import { Star, User } from 'lucide-react';
import './ViewAllHallsPage.css';
import SportsHallDetailsModal from './SportsHallDetailsModal';
import HallManagementModal from './HallManagementModal';

class ViewAllHallsPage extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';
        this.state = {
            halls: [],
            cities: [],
            admins: {},
            hallRatings: {},
            selectedCity: 'all',
            loading: true,
            error: null,
            modalOpen: false,
            selectedHallId: null,
            managementModalOpen: false,
            managementHallId: null
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

            const response = await fetch(`${this.API_BASE_URL}/sportsHalls`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();

            const uniqueCities = [...new Set(halls.map(hall => hall.city))];

            this.setState({
                halls,
                cities: uniqueCities,
                loading: false
            });

            await this.fetchAdminsData(halls);
            await this.fetchRatingsForHalls(halls);

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
                    const response = await fetch(`${this.API_BASE_URL}/users/${adminId}`, {
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

    fetchRatingsForHalls = async (halls) => {
        const hallRatings = {};

        // Încărcăm rating-urile pentru fiecare sală în paralel
        await Promise.all(
            halls.map(async (hall) => {
                try {
                    const response = await fetch(`${this.API_BASE_URL}/feedbacks?hallId=${hall.id}`);
                    if (response.ok) {
                        const feedbacks = await response.json();
                        const ratingData = this.calculateRatingFromFeedbacks(feedbacks);
                        hallRatings[hall.id] = ratingData;
                    } else {
                        hallRatings[hall.id] = {
                            averageRating: 0,
                            totalReviews: 0
                        };
                    }
                } catch (error) {
                    console.error(`Error fetching ratings for hall ${hall.id}:`, error);
                    hallRatings[hall.id] = {
                        averageRating: 0,
                        totalReviews: 0
                    };
                }
            })
        );

        this.setState({ hallRatings });
    }

    calculateRatingFromFeedbacks = (feedbacks) => {
        if (!feedbacks || feedbacks.length === 0) {
            return {
                averageRating: 0,
                totalReviews: 0
            };
        }

        const totalRating = feedbacks.reduce((sum, feedback) => sum + (feedback.rating || 0), 0);
        const averageRating = totalRating / feedbacks.length;

        return {
            averageRating: Math.round(averageRating * 10) / 10, // Rotunjim la o zecimală
            totalReviews: feedbacks.length
        };
    }

    getCoverImage = (hall) => {
        if (hall.images && hall.images.length > 0) {
            const coverImage = hall.images.find(img => img.description === 'cover');
            if (coverImage) {
                return `${this.API_BASE_URL}/images/${coverImage.id}`;            }
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
        this.setState({
            managementModalOpen: true,
            managementHallId: hallId
        });
    }

    handleCloseManagementModal = () => {
        this.setState({
            managementModalOpen: false,
            managementHallId: null
        });
    }

    handleHallUpdated = (updatedHall) => {
        // Actualizează lista de săli cu datele noi
        this.setState(prevState => ({
            halls: prevState.halls.map(hall =>
                hall.id === updatedHall.id ? updatedHall : hall
            )
        }));
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
        const { halls, cities, selectedCity, loading, error, modalOpen, selectedHallId, hallRatings, managementModalOpen, managementHallId } = this.state;

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
                            const ratingData = hallRatings[hall.id] || { averageRating: 0, totalReviews: 0 };

                            return (
                                <div key={hall.id} className={`global-hall-card ${hall.status?.toLowerCase()}`}>
                                    <div className="global-hall-image-container">
                                        <div className={`global-hall-status-indicator ${hall.status?.toLowerCase()}`}>
                                            {hall.status === 'ACTIVE' ? 'Activ' : 'Inactiv'}
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

                                        <div className="global-hall-rating">
                                            {this.renderRatingStars(ratingData.averageRating)}
                                            <span className="global-rating-text">
                                                {ratingData.averageRating > 0
                                                    ? `${ratingData.averageRating} (${ratingData.totalReviews} ${ratingData.totalReviews === 1 ? 'recenzie' : 'recenzii'})`
                                                    : 'Fără recenzii'
                                                }
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

                {managementModalOpen && managementHallId && (
                    <HallManagementModal
                        isOpen={managementModalOpen}
                        hallId={managementHallId}
                        onClose={this.handleCloseManagementModal}
                        onHallUpdated={this.handleHallUpdated}
                    />
                )}
            </div>
        );
    }
}

export default ViewAllHallsPage;