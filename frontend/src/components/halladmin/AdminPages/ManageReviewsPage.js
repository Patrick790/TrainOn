import React, { Component } from 'react';
import axios from 'axios';
import { Star, ChevronDown } from 'lucide-react';
import './ManageReviewsPage.css';

class ManageReviewsPage extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';
        this.state = {
            isLoading: true,
            error: null,
            myHalls: [],
            selectedHall: null,
            reviews: [],
            filter: 'all',
        };
    }

    componentDidMount() {
        this.fetchMyHalls();
    }

    fetchMyHalls = async () => {
        try {
            const userId = localStorage.getItem('userId');
            if (!userId) {
                this.setState({
                    error: "Nu sunteți autentificat sau nu aveți permisiuni de administrator.",
                    isLoading: false
                });
                return;
            }

            const response = await axios.get(`${this.API_BASE_URL}/sportsHalls/admin/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('jwtToken')}`
                }
            });

            if (response.data.length === 0) {
                this.setState({
                    myHalls: [],
                    isLoading: false,
                    error: "Nu aveți săli de sport înregistrate."
                });
            } else {
                this.setState({
                    myHalls: response.data,
                    isLoading: false,
                    selectedHall: response.data[0],
                }, () => {
                    this.fetchReviews(response.data[0].id);
                });
            }
        } catch (error) {
            console.error('Eroare la obținerea sălilor:', error);
            this.setState({
                error: "A apărut o eroare la încărcarea sălilor de sport.",
                isLoading: false
            });
        }
    };

    fetchReviews = async (hallId) => {
        this.setState({ isLoading: true });
        try {
            const response = await axios.get(`${this.API_BASE_URL}/feedbacks?hallId=${hallId}`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('jwtToken')}`
                }
            });

            this.setState({
                reviews: response.data,
                isLoading: false
            });
        } catch (error) {
            console.error('Eroare la obținerea recenziilor:', error);
            this.setState({
                error: "A apărut o eroare la încărcarea recenziilor.",
                isLoading: false
            });
        }
    };

    handleHallChange = (hall) => {
        this.setState({ selectedHall: hall, reviews: [] }, () => {
            this.fetchReviews(hall.id);
        });
    };

    handleFilterChange = (filter) => {
        this.setState({ filter });
    };

    getFilteredReviews = () => {
        const { reviews, filter } = this.state;

        switch (filter) {
            case 'highest':
                return [...reviews].sort((a, b) => b.rating - a.rating);
            case 'lowest':
                return [...reviews].sort((a, b) => a.rating - b.rating);
            case 'recent':
                return [...reviews].sort((a, b) => new Date(b.date) - new Date(a.date));
            default:
                return reviews;
        }
    };

    renderStars = (rating) => {
        const stars = [];
        for (let i = 1; i <= 5; i++) {
            stars.push(
                <Star
                    key={i}
                    size={18}
                    fill={i <= rating ? "#FFD700" : "none"}
                    color={i <= rating ? "#FFD700" : "#d1d5db"}
                />
            );
        }
        return stars;
    };

    formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('ro-RO', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    render() {
        const { isLoading, error, myHalls, selectedHall } = this.state;
        const filteredReviews = this.getFilteredReviews();

        if (isLoading && myHalls.length === 0) {
            return (
                <div className="admin-page">
                    <h1 className="admin-page-title">Vizualizare Recenzii</h1>
                    <div className="reviews-loading">Se încarcă...</div>
                </div>
            );
        }

        if (error && myHalls.length === 0) {
            return (
                <div className="admin-page">
                    <h1 className="admin-page-title">Vizualizare Recenzii</h1>
                    <div className="reviews-error">{error}</div>
                </div>
            );
        }

        return (
            <div className="admin-page">
                <h1 className="admin-page-title">Vizualizare Recenzii</h1>

                {/* Selectarea sălii */}
                <div className="hall-selector">
                    <label htmlFor="hall-select">Alege sala pentru vizualizarea recenziilor:</label>
                    <div className="select-wrapper">
                        <select
                            id="hall-select"
                            value={selectedHall ? selectedHall.id : ''}
                            onChange={(e) => {
                                const hallId = e.target.value;
                                const hall = myHalls.find(h => h.id.toString() === hallId);
                                if (hall) this.handleHallChange(hall);
                            }}
                        >
                            {myHalls.map(hall => (
                                <option key={hall.id} value={hall.id}>
                                    {hall.name}
                                </option>
                            ))}
                        </select>
                        <ChevronDown size={18} className="select-icon" />
                    </div>
                </div>

                {/* Filtre */}
                <div className="reviews-filters">
                    <div className="filters-group">
                        <span>Filtrează recenziile: </span>
                        <div className="filter-buttons">
                            <button
                                className={this.state.filter === 'all' ? 'active' : ''}
                                onClick={() => this.handleFilterChange('all')}
                            >
                                Toate
                            </button>
                            <button
                                className={this.state.filter === 'highest' ? 'active' : ''}
                                onClick={() => this.handleFilterChange('highest')}
                            >
                                Cele mai bune
                            </button>
                            <button
                                className={this.state.filter === 'lowest' ? 'active' : ''}
                                onClick={() => this.handleFilterChange('lowest')}
                            >
                                Cele mai slabe
                            </button>
                            <button
                                className={this.state.filter === 'recent' ? 'active' : ''}
                                onClick={() => this.handleFilterChange('recent')}
                            >
                                Recente
                            </button>
                        </div>
                    </div>
                </div>

                {isLoading ? (
                    <div className="reviews-loading">Se încarcă recenziile...</div>
                ) : error ? (
                    <div className="reviews-error">{error}</div>
                ) : filteredReviews.length === 0 ? (
                    <div className="reviews-empty">
                        <p>Nu există recenzii pentru această sală de sport.</p>
                    </div>
                ) : (
                    <div className="reviews-list">
                        {filteredReviews.map(review => (
                            <div key={review.id} className="review-card">
                                <div className="review-header">
                                    <div className="review-user-info">
                                        <div className="user-avatar">
                                            {review.user && review.user.name ? review.user.name.charAt(0).toUpperCase() : '?'}
                                        </div>
                                        <div className="user-details">
                                            <h3 className="user-name">{review.user ? review.user.name : 'Utilizator necunoscut'}</h3>
                                            <div className="review-meta">
                                                <div className="review-rating">
                                                    {this.renderStars(review.rating)}
                                                </div>
                                                <span className="review-date">{this.formatDate(review.date)}</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div className="review-content">
                                    <p>{review.comment}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }
}

export default ManageReviewsPage;