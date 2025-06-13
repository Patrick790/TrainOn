import React, { Component } from 'react';
import { Star, User, MapPin, DollarSign, X, ChevronLeft, ChevronRight, Phone, Mail, Tag, Users, Calendar } from 'lucide-react';
import './SportsHallDetailsModal.css';

class SportsHallDetailsModal extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';

        this.state = {
            hall: null,
            admin: null,
            hallRating: { averageRating: 0, totalReviews: 0 }, // Adăugat pentru rating-uri din DB
            loading: true,
            error: null,
            currentImageIndex: 0,
            fadeIn: false
        };
    }

    componentDidMount() {
        this.fetchHallDetails();
        // Adăugăm un mic delay pentru animația de fade-in
        setTimeout(() => {
            this.setState({ fadeIn: true });
        }, 50);
    }

    fetchHallDetails = async () => {
        try {
            const { hallId } = this.props;
            const token = localStorage.getItem('jwtToken');

            // Obținem datele sălii
            const hallResponse = await fetch(`${this.API_BASE_URL}/sportsHalls/${hallId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!hallResponse.ok) {
                throw new Error(`Eroare la încărcarea detaliilor sălii`);
            }

            const hall = await hallResponse.json();
            this.setState({ hall });

            // Obținem datele administratorului și rating-urile în paralel
            const promises = [];

            if (hall.adminId) {
                promises.push(this.fetchAdminData(hall.adminId));
            }

            // Adăugat: încărcăm rating-urile din baza de date
            promises.push(this.fetchHallRating(hallId));

            await Promise.all(promises);
            this.setState({ loading: false });

        } catch (error) {
            console.error('Error fetching hall details:', error);
            this.setState({
                error: error.message,
                loading: false
            });
        }
    }

    fetchAdminData = async (adminId) => {
        try {
            const token = localStorage.getItem('jwtToken');
            const response = await fetch(`${this.API_BASE_URL}/users/${adminId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const admin = await response.json();
                this.setState({ admin });
            }
        } catch (err) {
            console.error(`Eroare la obținerea datelor pentru admin:`, err);
        }
    }

    // Nouă metodă pentru încărcarea rating-urilor din baza de date
    fetchHallRating = async (hallId) => {
        try {
            const response = await fetch(`${this.API_BASE_URL}/feedbacks?hallId=${hallId}`);

            if (response.ok) {
                const feedbacks = await response.json();
                const ratingData = this.calculateRatingFromFeedbacks(feedbacks);
                this.setState({ hallRating: ratingData });
            } else {
                // Setăm valori default dacă nu putem încărca feedback-urile
                this.setState({ hallRating: { averageRating: 0, totalReviews: 0 } });
            }
        } catch (error) {
            console.error(`Error fetching hall rating:`, error);
            this.setState({ hallRating: { averageRating: 0, totalReviews: 0 } });
        }
    }

    // Nouă metodă pentru calcularea rating-ului din feedback-uri
    calculateRatingFromFeedbacks = (feedbacks) => {
        if (!feedbacks || feedbacks.length === 0) {
            return { averageRating: 0, totalReviews: 0 };
        }

        const totalRating = feedbacks.reduce((sum, feedback) => sum + (feedback.rating || 0), 0);
        const averageRating = totalRating / feedbacks.length;

        return {
            averageRating: Math.round(averageRating * 10) / 10,
            totalReviews: feedbacks.length
        };
    }

    handleImageNav = (direction) => {
        const { hall } = this.state;
        if (!hall || !hall.images || hall.images.length <= 1) return;

        this.setState(prevState => {
            let newIndex = prevState.currentImageIndex;
            if (direction === 'next') {
                newIndex = (prevState.currentImageIndex + 1) % hall.images.length;
            } else {
                newIndex = (prevState.currentImageIndex - 1 + hall.images.length) % hall.images.length;
            }
            return { currentImageIndex: newIndex };
        });
    }

    handleClose = () => {
        // Animație de fade-out la închidere
        this.setState({ fadeIn: false });
        setTimeout(() => {
            if (this.props.onClose) {
                this.props.onClose();
            }
        }, 200);
    }

    renderRatingStars = (rating) => {
        const totalStars = 5;
        return (
            <div className="modal-rating-stars">
                {[...Array(totalStars)].map((_, index) => (
                    <Star
                        key={index}
                        size={18}
                        fill={index < Math.floor(rating) ? "#FFD700" : "none"}
                        color={index < Math.floor(rating) ? "#FFD700" : "#D1D5DB"}
                        className="modal-star-icon"
                    />
                ))}
            </div>
        );
    }

    render() {
        const { isOpen } = this.props;
        const { hall, admin, hallRating, loading, error, currentImageIndex, fadeIn } = this.state;

        if (!isOpen) return null;

        return (
            <div className={`modal-overlay ${fadeIn ? 'fade-in' : 'fade-out'}`} onClick={this.handleClose}>
                <div className={`modal-container ${fadeIn ? 'slide-in' : 'slide-out'}`} onClick={e => e.stopPropagation()}>
                    <button className="modal-close-btn" onClick={this.handleClose} aria-label="Închide">
                        <X size={24} />
                    </button>

                    {loading ? (
                        <div className="modal-loading">
                            <div className="loading-spinner"></div>
                            <p>Se încarcă detaliile sălii...</p>
                        </div>
                    ) : error ? (
                        <div className="modal-error">
                            <div className="error-icon">!</div>
                            <p>{error}</p>
                        </div>
                    ) : hall ? (
                        <>
                            <div className="modal-header">
                                <h2 className="modal-title">{hall.name}</h2>
                                <div className="modal-location">
                                    <MapPin size={18} className="modal-icon" />
                                    <span>{hall.address}, {hall.city}, {hall.county}</span>
                                </div>
                            </div>

                            <div className="modal-body">
                                <div className="modal-columns">
                                    <div className="modal-left">
                                        {hall.images && hall.images.length > 0 ? (
                                            <div className="modal-image-gallery">
                                                <div className="modal-main-image-container">
                                                    <img
                                                        src={`${this.API_BASE_URL}/images/${hall.images[currentImageIndex].id}`}
                                                        alt={hall.name}
                                                        className="modal-main-image"
                                                    />
                                                    {hall.images.length > 1 && (
                                                        <>
                                                            <button
                                                                className="modal-nav-button prev"
                                                                onClick={() => this.handleImageNav('prev')}
                                                                aria-label="Imaginea anterioară"
                                                            >
                                                                <ChevronLeft size={24} />
                                                            </button>
                                                            <button
                                                                className="modal-nav-button next"
                                                                onClick={() => this.handleImageNav('next')}
                                                                aria-label="Imaginea următoare"
                                                            >
                                                                <ChevronRight size={24} />
                                                            </button>
                                                        </>
                                                    )}
                                                </div>

                                                {hall.images.length > 1 && (
                                                    <div className="modal-thumbnails">
                                                        {hall.images.map((image, index) => (
                                                            <div
                                                                key={image.id}
                                                                className={`modal-thumbnail ${index === currentImageIndex ? 'active' : ''}`}
                                                                onClick={() => this.setState({ currentImageIndex: index })}
                                                            >
                                                                <img
                                                                    src={`${this.API_BASE_URL}/images/${image.id}`}
                                                                    alt={`${hall.name} - ${index + 1}`}
                                                                />
                                                            </div>
                                                        ))}
                                                    </div>
                                                )}
                                            </div>
                                        ) : (
                                            <div className="modal-no-image">
                                                <div className="modal-initial-container">
                                                    <div className="modal-initial">{hall.name.charAt(0)}</div>
                                                </div>
                                                <p className="modal-no-image-text">Nu există imagini disponibile</p>
                                            </div>
                                        )}
                                    </div>

                                    <div className="modal-right">
                                        <div className="modal-info-card">
                                            <div className="modal-info-header">
                                                <h3>Detalii generale</h3>
                                            </div>
                                            <div className="modal-info-grid">
                                                <div className="modal-info-item">
                                                    <Tag size={16} className="modal-info-icon" />
                                                    <div>
                                                        <span className="modal-info-label">Tip</span>
                                                        <span className="modal-info-value">{hall.type || 'Nespecificat'}</span>
                                                    </div>
                                                </div>
                                                <div className="modal-info-item">
                                                    <Users size={16} className="modal-info-icon" />
                                                    <div>
                                                        <span className="modal-info-label">Capacitate</span>
                                                        <span className="modal-info-value">{hall.capacity || 0} persoane</span>
                                                    </div>
                                                </div>
                                                <div className="modal-info-item">
                                                    <DollarSign size={16} className="modal-info-icon" />
                                                    <div>
                                                        <span className="modal-info-label">Tarif</span>
                                                        <span className="modal-info-value">{hall.tariff || 0} lei/1h30</span>
                                                    </div>
                                                </div>
                                                <div className="modal-info-item">
                                                    <Calendar size={16} className="modal-info-icon" />
                                                    <div>
                                                        <span className="modal-info-label">Disponibilitate</span>
                                                        <span className="modal-info-value">Verificați programul</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        {hall.description && (
                                            <div className="modal-info-card">
                                                <div className="modal-info-header">
                                                    <h3>Descriere</h3>
                                                </div>
                                                <p className="modal-description">{hall.description}</p>
                                            </div>
                                        )}

                                        {hall.facilities && (
                                            <div className="modal-info-card">
                                                <div className="modal-info-header">
                                                    <h3>Facilități</h3>
                                                </div>
                                                <div className="modal-facilities">
                                                    {hall.facilities.split(',').map((facility, index) => (
                                                        <div key={index} className="modal-facility-tag">
                                                            {facility.trim()}
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        )}

                                        {admin && (
                                            <div className="modal-info-card">
                                                <div className="modal-info-header">
                                                    <h3>Contact administrator</h3>
                                                </div>
                                                <div className="modal-admin-info">
                                                    <div className="modal-admin-avatar">
                                                        <User size={22} />
                                                    </div>
                                                    <div className="modal-admin-details">
                                                        <div className="modal-admin-name">{admin.name || 'Necunoscut'}</div>
                                                        {admin.email && (
                                                            <div className="modal-admin-contact">
                                                                <Mail size={14} className="modal-contact-icon" />
                                                                <span>{admin.email}</span>
                                                            </div>
                                                        )}
                                                        {admin.phone && (
                                                            <div className="modal-admin-contact">
                                                                <Phone size={14} className="modal-contact-icon" />
                                                                <span>{admin.phone}</span>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        )}

                                        <div className="modal-info-card">
                                            <div className="modal-info-header">
                                                <h3>Rating și recenzii</h3>
                                            </div>
                                            <div className="modal-rating">
                                                {/* Folosește rating-ul real din baza de date */}
                                                {this.renderRatingStars(hallRating.averageRating)}
                                                <span className="modal-rating-text">
                                                    {hallRating.averageRating > 0
                                                        ? `${hallRating.averageRating} (${hallRating.totalReviews} ${hallRating.totalReviews === 1 ? 'recenzie' : 'recenzii'})`
                                                        : '0 (0 recenzii)'
                                                    }
                                                </span>
                                            </div>
                                            <p className="modal-no-reviews">
                                                {hallRating.totalReviews > 0
                                                    ? `Această sală are ${hallRating.totalReviews} ${hallRating.totalReviews === 1 ? 'recenzie' : 'recenzii'}.`
                                                    : 'Nu există recenzii încă pentru această sală.'
                                                }
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="modal-footer">
                                <button className="modal-btn primary" onClick={this.handleClose}>
                                    Închide
                                </button>
                            </div>
                        </>
                    ) : (
                        <div className="modal-error">
                            <div className="error-icon">!</div>
                            <p>Nu s-au putut încărca detaliile sălii</p>
                        </div>
                    )}
                </div>
            </div>
        );
    }
}

export default SportsHallDetailsModal;