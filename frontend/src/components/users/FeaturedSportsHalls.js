import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './FeaturedSportsHalls.css';

const FeaturedSportsHalls = ({ selectedCity }) => {
    const [sportsHalls, setSportsHalls] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    // Adăugăm stare pentru animație și tranziție fluidă
    const [isChanging, setIsChanging] = useState(false);

    const API_URL = process.env.REACT_APP_API_URL || '';
    // State pentru toate sălile de sport ACTIVE
    const [allSportsHalls, setAllSportsHalls] = useState([]);
    const [initialLoading, setInitialLoading] = useState(true);

    // Primul effect pentru încărcarea tuturor sălilor ACTIVE
    useEffect(() => {
        const fetchAllSportsHalls = async () => {
            try {
                // SCHIMBAREA PRINCIPALĂ: folosim endpoint-ul /active în loc de cel general
                const response = await axios.get(`${API_URL}/sportsHalls/active`);
                console.log(`Loaded ${response.data.length} active sports halls for featured section`);
                setAllSportsHalls(response.data);
                setLoading(false);
                setInitialLoading(false);
            } catch (err) {
                console.error('Eroare la încărcarea sălilor active:', err);
                setError('Nu s-au putut încărca sălile de sport.');
                setLoading(false);
                setInitialLoading(false);
            }
        };

        fetchAllSportsHalls();
    }, []); // Rulează doar la montarea componentei

    // Al doilea effect pentru filtrarea sălilor când se schimbă orașul
    useEffect(() => {
        if (!initialLoading) {
            // Activăm starea de tranziție
            setIsChanging(true);

            // Folosim un timeout scurt pentru a permite animației să înceapă
            const timeoutId = setTimeout(() => {
                // Filtrăm sălile din orașul selectat și luăm primele 6
                // PLUS: verificăm dublu că sala este activă (chiar dacă endpoint-ul /active ar trebui să returneze doar active)
                const filteredHalls = allSportsHalls
                    .filter(hall => hall.city === selectedCity)
                    .filter(hall => !hall.status || hall.status === 'ACTIVE') // Filtrare suplimentară pentru siguranță
                    .slice(0, 6);

                console.log(`Filtered ${filteredHalls.length} active halls for city: ${selectedCity}`);
                setSportsHalls(filteredHalls);

                // Dezactivăm starea de tranziție după un mic delay
                setTimeout(() => {
                    setIsChanging(false);
                }, 300);
            }, 100);

            return () => clearTimeout(timeoutId);
        }
    }, [selectedCity, allSportsHalls, initialLoading]); // Re-filtrăm când se schimbă orașul

    // Funcție pentru a obține imaginea de copertă
    const getCoverImage = (hall) => {
        if (hall.images && hall.images.length > 0) {
            const coverImage = hall.images.find(img => img.description === 'cover') || hall.images[0];
            return `${API_URL}/images/${coverImage.id}`;
        }
        return 'https://via.placeholder.com/400x250?text=Imagine+Indisponibilă';
    };

    // Funcție pentru a formata prețul
    const formatPrice = (price) => {
        if (!price) return 'Preț la cerere';
        return `${price} RON/1h30`;
    };

    // Funcție pentru a calcula un rating demonstrativ
    const calculateRating = (hall) => {
        // Pentru demonstrație folosim capacitatea ca input pentru calculul unui rating
        // În realitate, rating-ul ar veni din backend
        const capacity = hall.capacity || 0;
        let baseRating = 3.5; // Rating de bază

        // Adăugăm variație bazată pe capacitate
        if (capacity > 100) baseRating += 1.2;
        else if (capacity > 50) baseRating += 0.8;
        else if (capacity > 20) baseRating += 0.4;

        // Asigurăm-ne că ratingul este între 1 și 5
        const rating = Math.min(5, Math.max(1, baseRating));

        // Returnăm rating-ul cu o singură zecimală
        return rating.toFixed(1);
    };

    const handleRatingClick = (hallId, e) => {
        e.preventDefault(); // Prevenim comportamentul implicit al link-ului
        e.stopPropagation(); // Oprim propagarea evenimentului
        navigate(`/sportsHalls/${hallId}/reviews`);
    };

    // Funcție pentru a genera stelele de rating
    const renderStars = (rating, hallId) => {
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.5;
        const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

        return (
            <div className="sports-hall-stars" onClick={(e) => handleRatingClick(hallId, e)}>
                {[...Array(fullStars)].map((_, i) => (
                    <span key={`full-${i}`} className="sports-hall-star-full">★</span>
                ))}
                {hasHalfStar && <span className="sports-hall-star-half">★</span>}
                {[...Array(emptyStars)].map((_, i) => (
                    <span key={`empty-${i}`} className="sports-hall-star-empty">☆</span>
                ))}
                <span className="sports-hall-rating-value">{rating}</span>
            </div>
        );
    };

    // Afișăm indicator de încărcare doar la încărcarea inițială
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
                        const rating = calculateRating(hall);

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
                                            {renderStars(rating, hall.id)}
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