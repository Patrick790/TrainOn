import React, { Component } from 'react';
import { Star } from 'lucide-react';
import { Link, Navigate } from 'react-router-dom';
import './MyHallsPage.css';

class MyHallsPage extends Component {
    constructor(props) {
        super(props);
        this.state = {
            halls: [],
            hallRatings: {}, // Obiect pentru a stoca rating-urile fiecărei săli
            loading: true,
            error: null,
            redirectToEdit: null
        };
    }

    async componentDidMount() {
        await this.fetchHalls();
    }

    fetchHalls = async () => {
        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                this.setState({
                    error: 'Nu sunteți autentificat corect',
                    loading: false,
                    halls: []
                });
                return;
            }

            const response = await fetch(`http://localhost:8080/sportsHalls/admin/${userId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălilor: ${response.status}`);
            }

            const halls = await response.json();
            console.log('Halls loaded:', halls);

            // După încărcarea sălilor, încărcăm rating-urile pentru fiecare
            await this.fetchRatingsForHalls(halls);

            this.setState({ halls, loading: false });
        } catch (error) {
            console.error('Error fetching halls:', error);
            this.setState({
                error: error.message,
                loading: false,
                halls: []
            });
        }
    }

    fetchRatingsForHalls = async (halls) => {
        const hallRatings = {};

        // Încărcăm rating-urile pentru fiecare sală în paralel
        await Promise.all(
            halls.map(async (hall) => {
                try {
                    const response = await fetch(`http://localhost:8080/feedbacks?hallId=${hall.id}`);
                    if (response.ok) {
                        const feedbacks = await response.json();
                        const ratingData = this.calculateRatingFromFeedbacks(feedbacks);
                        hallRatings[hall.id] = ratingData;
                    } else {
                        // Dacă nu putem încărca feedback-urile, setăm valori default
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

        // Calculăm media rating-urilor
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
                return `http://localhost:8080/images/${coverImage.id}`;
            }
        }
        return null;
    }

    handleEditClick = (hallId) => {
        this.setState({ redirectToEdit: `/edit-hall/${hallId}` });
    }

    renderRatingStars = (rating) => {
        const totalStars = 5;
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.3 && rating % 1 <= 0.7;
        const hasAlmostFullStar = rating % 1 > 0.7;

        return (
            <div className="hall-rating-stars">
                {[...Array(totalStars)].map((_, index) => {
                    // Pentru stele complete
                    if (index < fullStars) {
                        return (
                            <Star
                                key={index}
                                size={18}
                                fill="#FFD700"
                                color="#FFD700"
                                className="star-icon"
                            />
                        );
                    }
                    // Pentru jumătate de stea
                    else if (index === fullStars && hasHalfStar) {
                        return (
                            <div className="half-star-container" key={index}>
                                <Star
                                    size={18}
                                    fill="#FFD700"
                                    color="#FFD700"
                                    className="half-star"
                                />
                                <Star
                                    size={18}
                                    fill="none"
                                    color="#D1D5DB"
                                    className="star-icon star-background"
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
                                className="star-icon"
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
                                className="star-icon"
                            />
                        );
                    }
                })}
            </div>
        );
    }

    render() {
        const { halls, hallRatings, loading, error, redirectToEdit } = this.state;

        if (redirectToEdit) {
            return <Navigate to={redirectToEdit} />;
        }

        if (loading) {
            return <div className="loading-container">Se încarcă sălile...</div>;
        }

        if (error) {
            return <div className="error-container">Eroare: {error}</div>;
        }

        return (
            <div className="admin-page">
                <h1 className="admin-page-title">Sălile Mele</h1>

                <div className="halls-grid">
                    {halls.length > 0 ? (
                        halls.map(hall => {
                            const coverImageUrl = this.getCoverImage(hall);
                            const ratingData = hallRatings[hall.id] || { averageRating: 0, totalReviews: 0 };

                            return (
                                <div key={hall.id} className="hall-card">
                                    <div className="hall-image-container">
                                        <div className="hall-status-indicator active">
                                            Activ
                                        </div>

                                        {coverImageUrl ? (
                                            <img
                                                src={coverImageUrl}
                                                alt={hall.name}
                                                className="hall-image"
                                            />
                                        ) : (
                                            <div className="hall-placeholder">
                                                <div className="hall-initial">{hall.name.charAt(0)}</div>
                                            </div>
                                        )}
                                    </div>

                                    <div className="hall-content">
                                        <h3 className="hall-name">{hall.name}</h3>
                                        <p className="hall-location">{hall.city}, {hall.county}</p>
                                        <p className="hall-description">{hall.description || 'Fără descriere'}</p>

                                        {/* Rating real din baza de date */}
                                        <div className="hall-rating">
                                            {this.renderRatingStars(ratingData.averageRating)}
                                            <span className="rating-text">
                                                {ratingData.averageRating > 0
                                                    ? `${ratingData.averageRating} (${ratingData.totalReviews} ${ratingData.totalReviews === 1 ? 'recenzie' : 'recenzii'})`
                                                    : 'Fără recenzii'
                                                }
                                            </span>
                                        </div>

                                        <div className="hall-actions">
                                            <button
                                                className="hall-action-button edit"
                                                onClick={() => this.handleEditClick(hall.id)}
                                            >
                                                Editare
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            );
                        })
                    ) : (
                        <div className="empty-state">
                            <p>Nu aveți săli de sport adăugate. Adăugați prima sală folosind butonul "Adăugare".</p>
                        </div>
                    )}
                </div>
            </div>
        );
    }
}

export default MyHallsPage;