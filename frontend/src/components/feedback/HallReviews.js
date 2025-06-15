import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import axios from 'axios';
import './HallReviews.css';
import StarRating from './StarRating';

const HallReviews = () => {
    const { hallId } = useParams();
    const [hall, setHall] = useState(null);
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [userData, setUserData] = useState(null);
    const [newReview, setNewReview] = useState({
        rating: 0,
        comment: '',
    });
    const [showAddReviewForm, setShowAddReviewForm] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');
    const [isAuthenticated, setIsAuthenticated] = useState(false);

    const API_URL = process.env.REACT_APP_API_URL || '';

    useEffect(() => {
        const token = localStorage.getItem('jwtToken');
        const userId = localStorage.getItem('userId');
        const userType = localStorage.getItem('userType');

        const isLoggedIn = !!token && !!userId && localStorage.getItem('isLoggedIn') === 'true';
        setIsAuthenticated(isLoggedIn);

        console.log("Auth status:", {
            isLoggedIn: localStorage.getItem('isLoggedIn'),
            userId: userId,
            token: token ? "exists" : "missing",
            userType: userType
        });

        if (isLoggedIn && userId) {
            const fetchUserData = async () => {
                try {
                    const response = await axios.get(`${API_URL}/users/${userId}`, {
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    });
                    setUserData(response.data);
                } catch (err) {
                    console.error("Eroare la încărcarea datelor utilizatorului:", err);
                }
            };
            fetchUserData();
        }
    }, []);

    useEffect(() => {
        const fetchHallDetails = async () => {
            try {
                const hallResponse = await axios.get(`${API_URL}/sportsHalls/${hallId}`);
                setHall(hallResponse.data);
            } catch (err) {
                console.error('Eroare la încărcarea detaliilor sălii:', err);
                setError('Nu s-au putut încărca detaliile sălii de sport.');
            }
        };

        fetchHallDetails();
    }, [hallId]);

    const loadReviews = async () => {
        try {
            const feedbacksResponse = await axios.get(`${API_URL}/feedbacks`);

            const hallReviews = feedbacksResponse.data.filter(feedback =>
                feedback.hall && feedback.hall.id === parseInt(hallId)
            );

            hallReviews.sort((a, b) => {
                return new Date(b.date || b.createdAt || 0) - new Date(a.date || a.createdAt || 0);
            });

            console.log("Recenzii încărcate:", hallReviews);

            setReviews(hallReviews);
            setLoading(false);
        } catch (err) {
            console.error('Eroare la încărcarea recenziilor:', err);
            setError('Nu s-au putut încărca recenziile pentru această sală.');
            setLoading(false);
        }
    };

    useEffect(() => {
        if (hallId) {
            loadReviews();
        }
    }, [hallId]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setNewReview({
            ...newReview,
            [name]: value
        });
    };

    const handleRatingChange = (rating) => {
        setNewReview({
            ...newReview,
            rating: rating
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!newReview.rating) {
            alert('Vă rugăm să acordați un rating înainte de a trimite recenzia.');
            return;
        }

        try {
            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                alert('Trebuie să fiți autentificat pentru a adăuga o recenzie.');
                return;
            }

            // Creăm obiectul conform modelului Feedback
            const reviewData = {
                hall: { id: parseInt(hallId) },
                user: { id: parseInt(userId) },
                rating: newReview.rating,
                comment: newReview.comment,
                date: new Date()
            };

            console.log("Se trimite recenzia:", reviewData);

            const config = {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            };

            const response = await axios.post(`${API_URL}/feedbacks`, reviewData, config);
            console.log("Răspuns de la server:", response.data);

            setNewReview({
                rating: 0,
                comment: '',
            });
            setShowAddReviewForm(false);

            setSuccessMessage('Recenzia dvs. a fost adăugată cu succes!');
            setTimeout(() => {
                setSuccessMessage('');
            }, 3000);

            loadReviews();

        } catch (err) {
            console.error('Eroare la trimiterea recenziei:', err);
            alert('A apărut o eroare la trimiterea recenziei. Vă rugăm să încercați din nou.');
        }
    };

    const calculateAverageRating = () => {
        if (reviews.length === 0) return 0;

        const sum = reviews.reduce((total, review) => total + review.rating, 0);
        return (sum / reviews.length).toFixed(1);
    };

    const calculateRatingDistribution = () => {
        const distribution = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };

        reviews.forEach(review => {
            if (review.rating >= 1 && review.rating <= 5) {
                distribution[review.rating]++;
            }
        });

        return distribution;
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'Dată necunoscută';

        const date = new Date(dateString);
        const day = date.getDate().toString().padStart(2, '0');
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const year = date.getFullYear();

        return `${day}.${month}.${year}`;
    };

    const formatUsername = (review) => {
        // Verifică dacă există utilizator în review
        if (review.user) {
            return review.user.name || review.user.username || `Utilizator ${review.user.id}`;
        }
        if (review.team) {
            return review.team.name || `Utilizator ${review.team.id}`;
        }
        return 'Utilizator anonim';
    };

    const getUserInitial = (review) => {
        const username = formatUsername(review);
        return username.charAt(0).toUpperCase();
    };

    if (loading) {
        return <div className="reviews-loading">Se încarcă recenziile...</div>;
    }

    if (error) {
        return <div className="reviews-error">{error}</div>;
    }

    const ratingDistribution = calculateRatingDistribution();
    const averageRating = calculateAverageRating();

    return (
        <div className="reviews-page">
            <div className="reviews-header">
                <div className="reviews-header-content">
                    <div className="reviews-header-left">
                        <Link to={`/sportsHalls/${hallId}`} className="back-to-hall">
                            ← Înapoi la sala de sport
                        </Link>
                        <h1 className="reviews-title">Recenzii pentru {hall ? hall.name : 'Sală de sport'}</h1>
                    </div>

                    {isAuthenticated && (
                        <button
                            className="add-review-button"
                            onClick={() => setShowAddReviewForm(!showAddReviewForm)}
                        >
                            {showAddReviewForm ? 'Anulează' : 'Adaugă recenzie'}
                        </button>
                    )}
                </div>
            </div>

            <div className="reviews-content">
                <div className="reviews-summary">
                    <div className="reviews-average">
                        <div className="average-rating">{averageRating}</div>
                        <div className="average-stars">
                            <StarRating defaultRating={parseFloat(averageRating)} readOnly={true} size="large" />
                        </div>
                        <div className="reviews-count">{reviews.length} recenzii</div>
                    </div>

                    <div className="rating-distribution">
                        {[5, 4, 3, 2, 1].map(rating => (
                            <div key={rating} className="rating-bar-container">
                                <span className="rating-label">{rating}</span>
                                <div className="rating-bar-wrapper">
                                    <div
                                        className="rating-bar"
                                        style={{
                                            width: `${reviews.length ? (ratingDistribution[rating] / reviews.length) * 100 : 0}%`
                                        }}
                                    ></div>
                                </div>
                                <span className="rating-count">{ratingDistribution[rating]}</span>
                            </div>
                        ))}
                    </div>
                </div>

                {successMessage && (
                    <div className="success-message">{successMessage}</div>
                )}

                {isAuthenticated ? (
                    showAddReviewForm && (
                        <div className="add-review-form">
                            <h2>Adaugă recenzia ta</h2>
                            <form onSubmit={handleSubmit}>
                                <div className="form-group">
                                    <label>Evaluează:</label>
                                    <div className="rating-stars">
                                        <StarRating
                                            defaultRating={newReview.rating}
                                            onChange={handleRatingChange}
                                            size="large"
                                        />
                                    </div>
                                </div>
                                <div className="form-group">
                                    <label htmlFor="comment">Comentariu:</label>
                                    <textarea
                                        id="comment"
                                        name="comment"
                                        value={newReview.comment}
                                        onChange={handleInputChange}
                                        placeholder="Scrie recenzia ta aici..."
                                        rows="4"
                                        required
                                    ></textarea>
                                </div>
                                <button type="submit" className="submit-review-button">Trimite recenzia</button>
                            </form>
                        </div>
                    )
                ) : (
                    <div className="login-prompt">
                        <p>Trebuie să fii autentificat pentru a adăuga o recenzie.</p>
                        <Link to="/login" className="login-button">Autentifică-te</Link>
                    </div>
                )}

                <div className="reviews-list">
                    <h2>Toate recenziile</h2>

                    {reviews.length === 0 ? (
                        <div className="no-reviews">
                            Nu există recenzii pentru această sală de sport. Fii primul care adaugă o recenzie!
                        </div>
                    ) : (
                        reviews.map(review => (
                            <div key={review.id} className="review-card">
                                <div className="review-header">
                                    <div className="review-user">
                                        <div className="user-avatar">{getUserInitial(review)}</div>
                                        <div className="user-info">
                                            <div className="user-name">{formatUsername(review)}</div>
                                            <div className="review-date">{formatDate(review.date || review.createdAt)}</div>
                                        </div>
                                    </div>
                                    <div className="review-rating">
                                        <StarRating defaultRating={review.rating} readOnly={true} size="small" />
                                    </div>
                                </div>
                                <div className="review-content">
                                    {review.comment || "Niciun comentariu."}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};

export default HallReviews;