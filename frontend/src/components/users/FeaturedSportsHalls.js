import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './FeaturedSportsHalls.css';

const FeaturedSportsHalls = ({ selectedCity }) => {
    const [sportsHalls, setSportsHalls] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [hallRatings, setHallRatings] = useState({});
    const navigate = useNavigate();

    const [isChanging, setIsChanging] = useState(false);

    const API_URL = process.env.REACT_APP_API_URL || '';
    const [allSportsHalls, setAllSportsHalls] = useState([]);
    const [initialLoading, setInitialLoading] = useState(true);

    useEffect(() => {
        const fetchAllSportsHalls = async () => {
            try {
                const response = await axios.get(`${API_URL}/sportsHalls/active`);
                console.log(`Loaded ${response.data.length} active sports halls for featured section`);
                setAllSportsHalls(response.data);
                setLoading(false);
                setInitialLoading(false);

                // Încarcă rating-urile pentru toate sălile
                await fetchRatingsForHalls(response.data);
            } catch (err) {
                console.error('Eroare la încărcarea sălilor active:', err);
                setError('Nu s-au putut încărca sălile de sport.');
                setLoading(false);
                setInitialLoading(false);
            }
        };

        fetchAllSportsHalls();
    }, []);

    const fetchRatingsForHalls = async (halls) => {
        const ratingsData = {};

        await Promise.all(
            halls.map(async (hall) => {
                try {
                    const response = await fetch(`${API_URL}/feedbacks?hallId=${hall.id}`);
                    if (response.ok) {
                        const feedbacks = await response.json();
                        const ratingData = calculateRatingFromFeedbacks(feedbacks);
                        ratingsData[hall.id] = ratingData;
                    } else {
                        ratingsData[hall.id] = {
                            averageRating: 0,
                            totalReviews: 0
                        };
                    }
                } catch (error) {
                    console.error(`Error fetching ratings for hall ${hall.id}:`, error);
                    ratingsData[hall.id] = {
                        averageRating: 0,
                        totalReviews: 0
                    };
                }
            })
        );

        setHallRatings(ratingsData);
    };

    const calculateRatingFromFeedbacks = (feedbacks) => {
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
    };

    useEffect(() => {
        if (!initialLoading) {
            setIsChanging(true);

            const timeoutId = setTimeout(() => {
                const filteredHalls = allSportsHalls
                    .filter(hall => hall.city === selectedCity)
                    .filter(hall => !hall.status || hall.status === 'ACTIVE') // Filtrare suplimentară pentru siguranță
                    .slice(0, 6);

                console.log(`Filtered ${filteredHalls.length} active halls for city: ${selectedCity}`);
                setSportsHalls(filteredHalls);

                setTimeout(() => {
                    setIsChanging(false);
                }, 300);
            }, 100);

            return () => clearTimeout(timeoutId);
        }
    }, [selectedCity, allSportsHalls, initialLoading]);

    const getCoverImage = (hall) => {
        if (hall.images && hall.images.length > 0) {
            const coverImage = hall.images.find(img => img.description === 'cover') || hall.images[0];
            return `${API_URL}/images/${coverImage.id}`;
        }
        return 'https://via.placeholder.com/400x250?text=Imagine+Indisponibilă';
    };

    const formatPrice = (price) => {
        if (!price) return 'Preț la cerere';
        return `${price} RON/1h30`;
    };

    const handleRatingClick = (hallId, e) => {
        e.preventDefault();
        e.stopPropagation();
        navigate(`/sportsHalls/${hallId}/reviews`);
    };

    const renderStars = (rating, totalReviews, hallId) => {
        if (rating === 0) {
            return (
                <div className="sports-hall-stars" onClick={(e) => handleRatingClick(hallId, e)}>
                    <span className="sports-hall-no-rating">Fără recenzii</span>
                </div>
            );
        }

        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.3 && rating % 1 <= 0.7;
        const hasAlmostFullStar = rating % 1 > 0.7;

        return (
            <div className="sports-hall-stars" onClick={(e) => handleRatingClick(hallId, e)}>
                {[...Array(5)].map((_, index) => {
                    // Pentru stele complete
                    if (index < fullStars) {
                        return (
                            <span key={`full-${index}`} className="sports-hall-star-full">★</span>
                        );
                    }
                    else if (index === fullStars && hasHalfStar) {
                        return (
                            <span key={`half-${index}`} className="sports-hall-star-half">★</span>
                        );
                    }
                    else if (index === fullStars && hasAlmostFullStar) {
                        return (
                            <span key={`almost-full-${index}`} className="sports-hall-star-full">★</span>
                        );
                    }
                    else {
                        return (
                            <span key={`empty-${index}`} className="sports-hall-star-empty">☆</span>
                        );
                    }
                })}
                <span className="sports-hall-rating-value">
                    {rating} ({totalReviews} {totalReviews === 1 ? 'recenzie' : 'recenzii'})
                </span>
            </div>
        );
    };

    if (initialLoading) {
        return <div className="sports-halls-loading">Se încarcă...</div>;
    }

    if (error) {
        return <div className="sports-halls-error">{error}</div>;
    }

    return (
        <div className="sports-halls-container">
            <h2 className="sports-halls-title">Săli de sport populare în {selectedCity}</h2>

            <div className={`sports-halls-grid ${isChanging ? 'fade-transition' : ''}`}>
                {sportsHalls.length > 0 ? (
                    sportsHalls.map(hall => {
                        const ratingData = hallRatings[hall.id] || { averageRating: 0, totalReviews: 0 };

                        return (
                            <div key={hall.id} className="sports-hall-card">
                                {hall.type && <span className="sports-hall-badge">{hall.type}</span>}
                                <div className="sports-hall-image-wrapper">
                                    <img
                                        src={getCoverImage(hall)}
                                        alt={hall.name}
                                        className="sports-hall-image"
                                        onError={(e) => {
                                            e.target.onerror = null;
                                            e.target.src = 'https://via.placeholder.com/400x250?text=Imagine+Indisponibilă';
                                        }}
                                    />
                                </div>
                                <div className="sports-hall-info">
                                    <h3 className="sports-hall-name">{hall.name}</h3>
                                    <p className="sports-hall-address">{hall.address}</p>

                                    <div className="sports-hall-details">
                                        <span className="sports-hall-price">
                                            {formatPrice(hall.tariff)}
                                        </span>
                                        <span className="sports-hall-rating">
                                            {renderStars(ratingData.averageRating, ratingData.totalReviews, hall.id)}
                                        </span>
                                    </div>

                                    <Link to={`/sportsHalls/${hall.id}`} className="sports-hall-link">
                                        Vezi detalii
                                    </Link>
                                </div>
                            </div>
                        );
                    })
                ) : (
                    <p className="sports-halls-empty">Nu există săli active disponibile în {selectedCity}.</p>
                )}
            </div>
        </div>
    );
};

export default FeaturedSportsHalls;