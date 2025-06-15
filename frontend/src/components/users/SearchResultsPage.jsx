import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { MapPin, Star } from 'lucide-react';
import axios from 'axios';
import Header from './Header';
import SearchBar from './SearchBar';
import LoginModal from '../login/LoginModal';
import RegisterModal from '../register/RegisterModal';
import Footer from '../pageComponents/Footer';
import './SearchResultsPage.css';

const SearchResultsPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const queryParams = new URLSearchParams(location.search);
    const mapRef = useRef(null);
    const mapContainerRef = useRef(null);
    const markersRef = useRef([]);
    const mapInitializedRef = useRef(false);

    // Stare pentru toate datele necesare paginii
    const [sportsHalls, setSportsHalls] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [hoveredMarker, setHoveredMarker] = useState(null);
    const [mapReady, setMapReady] = useState(false);

    // State pentru header și autentificare
    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('isLoggedIn') === 'true');
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);

    // Parametrii de căutare din URL
    const [searchParams, setSearchParams] = useState({
        city: queryParams.get('city') || '',
        sport: queryParams.get('sport') || '',
        query: queryParams.get('query') || ''
    });


    const API_URL = process.env.REACT_APP_API_URL || '';

    const geocodeAddress = async (address, city, id) => {
        if (!address || !city) {
            console.log(`Sala cu ID ${id} nu are adresă sau oraș complet.`);
            return null;
        }

        try {
            const fullAddress = `${address}, ${city}, Romania`;
            const encodedAddress = encodeURIComponent(fullAddress);
            const response = await axios.get(
                `https://nominatim.openstreetmap.org/search?format=json&q=${encodedAddress}&limit=1`
            );

            await new Promise(resolve => setTimeout(resolve, 1000));

            if (response.data && response.data.length > 0) {
                const result = response.data[0];
                return {
                    latitude: parseFloat(result.lat),
                    longitude: parseFloat(result.lon)
                };
            } else {
                console.log(`Nu s-au găsit coordonate pentru adresa: ${fullAddress}`);
                return null;
            }
        } catch (error) {
            console.error(`Eroare la geocodarea adresei ${address}, ${city}:`, error);
            return null;
        }
    };

    useEffect(() => {
        const fetchSportsHalls = async () => {
            try {
                setLoading(true);

                const searchURLParams = new URLSearchParams();

                if (searchParams.city) {
                    searchURLParams.append('city', searchParams.city);
                }
                if (searchParams.sport && searchParams.sport !== 'Sport') {
                    searchURLParams.append('sport', searchParams.sport);
                }
                if (searchParams.query) {
                    searchURLParams.append('query', searchParams.query);
                }

                const searchURL = `${API_URL}/sportsHalls/search?${searchURLParams.toString()}`;
                console.log('Fetching from URL:', searchURL);
                console.log('Search parameters:', searchParams);

                const response = await axios.get(searchURL);

                console.log(`Found ${response.data.length} results for search:`, searchParams);
                console.log('Response data:', response.data);

                const hallsWithCoordinates = await Promise.all(
                    response.data.map(async (hall) => {
                        // Dacă sala are deja coordonate valide, le folosim
                        if (hall.latitude && hall.longitude &&
                            !isNaN(parseFloat(hall.latitude)) &&
                            !isNaN(parseFloat(hall.longitude))) {
                            return hall;
                        }

                        if (hall.address && hall.city) {
                            const coordinates = await geocodeAddress(hall.address, hall.city, hall.id);
                            if (coordinates) {
                                return {
                                    ...hall,
                                    latitude: coordinates.latitude,
                                    longitude: coordinates.longitude
                                };
                            }
                        }

                        return hall;
                    })
                );

                setSportsHalls(hallsWithCoordinates);
                setLoading(false);
            } catch (err) {
                console.error('Eroare la încărcarea sălilor:', err);
                console.error('Error details:', err.response?.data || err.message);
                setError('Nu s-au putut încărca sălile de sport.');
                setLoading(false);
            }
        };

        if (searchParams.city || searchParams.sport || searchParams.query) {
            console.log('Starting search with params:', searchParams);
            fetchSportsHalls();
        } else {
            console.log('No search criteria provided');
            setLoading(false);
            setSportsHalls([]);
        }
    }, [searchParams, API_URL]);

    useEffect(() => {
        const currentParams = new URLSearchParams(location.search);
        const newSearchParams = {
            city: currentParams.get('city') || '',
            sport: currentParams.get('sport') || '',
            query: currentParams.get('query') || ''
        };

        console.log('URL changed, new params:', newSearchParams);
        setSearchParams(newSearchParams);
    }, [location.search]);

    const initializeMap = () => {
        if (!mapContainerRef.current || !window.L || mapInitializedRef.current) {
            return;
        }

        try {
            if (mapRef.current) {
                mapRef.current.remove();
            }

            console.log('Inițializez harta...');

            const map = window.L.map(mapContainerRef.current, {
                // Opțiuni suplimentare pentru stabilitate
                fadeAnimation: false,
                zoomAnimation: false,
                markerZoomAnimation: false
            }).setView([46.7712, 23.6236], 13);

            window.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(map);

            mapRef.current = map;
            mapInitializedRef.current = true;

            setTimeout(() => {
                if (mapRef.current) {
                    mapRef.current.invalidateSize();
                    setMapReady(true);
                }
            }, 300);

            console.log('Harta a fost inițializată cu succes');
        } catch (error) {
            console.error('Eroare la inițializarea hărții:', error);
            setError('Nu s-a putut inițializa harta.');
        }
    };

    const addMarkersToMap = () => {
        if (!mapRef.current || !sportsHalls.length || !mapReady) {
            return;
        }

        try {
            console.log('Adaug markere pe hartă...');

            markersRef.current.forEach(marker => {
                if (marker && marker.remove) {
                    marker.remove();
                }
            });
            markersRef.current = [];

            const bounds = [];
            let validMarkers = 0;

            sportsHalls.forEach(hall => {
                if (!hall.latitude || !hall.longitude) {
                    console.log(`Sala ${hall.name} nu are coordonate valide. Nu se adaugă marker.`);
                    return; // Skip la această sală
                }

                const lat = parseFloat(hall.latitude);
                const lng = parseFloat(hall.longitude);

                if (isNaN(lat) || isNaN(lng)) {
                    console.log(`Sala ${hall.name} are coordonate invalide. Nu se adaugă marker.`);
                    return;
                }

                bounds.push([lat, lng]);
                validMarkers++;

                try {
                    const marker = window.L.marker([lat, lng])
                        .addTo(mapRef.current)
                        .bindTooltip(hall.name, { permanent: false, direction: 'top' });

                    marker.hallId = hall.id;

                    marker.on('mouseover', () => {
                        setHoveredMarker(hall.id);
                        marker.openTooltip();
                    });

                    marker.on('mouseout', () => {
                        setHoveredMarker(null);
                        marker.closeTooltip();
                    });

                    marker.on('click', () => {
                        navigate(`/sportsHalls/${hall.id}`);
                    });

                    markersRef.current.push(marker);
                } catch (markerError) {
                    console.error('Eroare la adăugarea markerului:', markerError);
                }
            });

            if (bounds.length > 0) {
                try {
                    mapRef.current.fitBounds(bounds, { padding: [30, 30] });
                    console.log(`Au fost adăugate ${validMarkers} markere pe hartă.`);
                } catch (boundsError) {
                    console.error('Eroare la fitBounds:', boundsError);
                    mapRef.current.setView([46.7712, 23.6236], 13);
                }
            } else {
                console.log('Nu există săli cu coordonate valide pentru afișare pe hartă.');
                mapRef.current.setView([46.7712, 23.6236], 13);
            }
        } catch (error) {
            console.error('Eroare la adăugarea markerelor:', error);
        }
    };

    useEffect(() => {
        // Verificăm dacă DOM este gata
        if (mapContainerRef.current && typeof window !== 'undefined' && window.L) {
            initializeMap();
        }

        return () => {
            if (mapRef.current) {
                // Curățăm markerele
                markersRef.current.forEach(marker => {
                    if (marker && marker.remove) {
                        marker.remove();
                    }
                });
                markersRef.current = [];

                mapRef.current.remove();
                mapRef.current = null;
                mapInitializedRef.current = false;
                setMapReady(false);
            }
        };
    }, []);

    useEffect(() => {
        if (!loading && !error && sportsHalls.length > 0 && mapReady) {
            addMarkersToMap();
        }
    }, [sportsHalls, loading, error, mapReady]);

    useEffect(() => {
        const handleResize = () => {
            if (mapRef.current) {
                mapRef.current.invalidateSize();
            }
        };

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
        };
    }, []);

    const checkLoginStatus = () => {
        setIsLoggedIn(localStorage.getItem('isLoggedIn') === 'true');
    };

    const handleLogout = () => {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userType');
        localStorage.removeItem('userId');
        localStorage.removeItem('jwtToken');
        setIsLoggedIn(false);
        window.location.href = '/';
    };

    const toggleLoginModal = () => {
        setIsLoginModalOpen(!isLoginModalOpen);
        setIsRegisterModalOpen(false);
    };

    const toggleRegisterModal = () => {
        setIsRegisterModalOpen(!isRegisterModalOpen);
        setIsLoginModalOpen(false);
    };

    const calculateRating = (hall) => {
        if (!hall || !hall.capacity) return 3.0;

        const capacity = hall.capacity || 0;
        let baseRating = 3.5;

        if (capacity > 100) baseRating += 1.2;
        else if (capacity > 50) baseRating += 0.8;
        else if (capacity > 20) baseRating += 0.4;

        const rating = Math.min(5, Math.max(1, baseRating));
        return rating.toFixed(1);
    };

    const renderStars = (rating) => {
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.5;
        const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

        return (
            <div className="srp-result-stars">
                {[...Array(fullStars)].map((_, i) => (
                    <Star key={`full-${i}`} className="srp-star-full" fill="#FFD700" color="#FFD700" size={14} />
                ))}
                {hasHalfStar && <Star className="srp-star-half" fill="#FFD700" color="#FFD700" size={14} />}
                {[...Array(emptyStars)].map((_, i) => (
                    <Star key={`empty-${i}`} className="srp-star-empty" color="#FFD700" size={14} />
                ))}
                <span className="srp-rating-value">{rating}</span>
            </div>
        );
    };

    const getImageUrl = (hall) => {
        if (hall.images && hall.images.length > 0) {
            const coverImage = hall.images.find(img => img.description === 'cover') || hall.images[0];
            return `${API_URL}/images/${coverImage.id}`;
        }
        return 'https://via.placeholder.com/400x250?text=Imagine+Indisponibilă';
    };

    const formatPrice = (price) => {
        if (!price) return 'Preț la cerere';
        return `${price} RON/oră`;
    };

    const getResultsTitle = () => {
        if (!searchParams.city && !searchParams.sport && !searchParams.query) {
            return 'Introduceți criterii de căutare pentru a vedea rezultatele';
        }

        let title = 'Săli de sport';

        if (searchParams.query) {
            title += ` "${searchParams.query}"`;
        }

        if (searchParams.city) {
            title += ` în ${searchParams.city}`;
        }

        if (searchParams.sport && searchParams.sport !== 'Sport') {
            title += ` - ${searchParams.sport}`;
        }

        return title;
    };

    return (
        <div className="main-container">
            <Header
                isLoggedIn={isLoggedIn}
                onLoginClick={toggleLoginModal}
                onRegisterClick={toggleRegisterModal}
                onLogout={handleLogout}
            />

            <div className="srp-search-container">
                <div className="srp-search-wrapper">
                    <SearchBar />
                </div>
            </div>

            <div className="srp-results-content">
                <h1 className="srp-results-title">
                    {getResultsTitle()}
                </h1>

                <div className="srp-results-layout">
                    <div className="srp-results-list">
                        {!searchParams.city && !searchParams.sport && !searchParams.query ? (
                            <div className="srp-no-results">
                                Vă rugăm să introduceți criterii de căutare: orașul, sportul sau numele sălii.
                            </div>
                        ) : loading ? (
                            <div className="srp-loading">Se încarcă sălile...</div>
                        ) : error ? (
                            <div className="srp-error">{error}</div>
                        ) : sportsHalls.length === 0 ? (
                            <div className="srp-no-results">Nu s-au găsit săli care să corespundă criteriilor de căutare.</div>
                        ) : (
                            sportsHalls.map(hall => (
                                <div
                                    key={hall.id}
                                    className={`srp-result-card ${hoveredMarker === hall.id ? 'srp-result-highlighted' : ''}`}
                                    onMouseEnter={() => setHoveredMarker(hall.id)}
                                    onMouseLeave={() => setHoveredMarker(null)}
                                >
                                    <div className="srp-result-image-container">
                                        <img
                                            src={getImageUrl(hall)}
                                            alt={hall.name}
                                            className="srp-result-image"
                                            onError={(e) => {
                                                e.target.onerror = null;
                                                e.target.src = 'https://via.placeholder.com/400x250?text=Imagine+Indisponibilă';
                                            }}
                                        />
                                        {hall.type && (
                                            <div className="srp-result-badge">{hall.type}</div>
                                        )}
                                    </div>
                                    <div className="srp-result-details">
                                        <h2 className="srp-result-name">{hall.name}</h2>
                                        <div className="srp-result-location">
                                            <MapPin size={14} />
                                            <span>{hall.address}, {hall.city}</span>
                                        </div>
                                        <div className="srp-result-rating">
                                            {renderStars(calculateRating(hall))}
                                        </div>
                                        <div className="srp-result-price">
                                            {formatPrice(hall.tariff)}
                                        </div>
                                        {hall.facilities && (
                                            <div className="srp-result-facilities">
                                                {(() => {
                                                    const facilitiesArray = hall.facilities.split(',').map(f => f.trim());

                                                    const maxVisible = 2; // Reducem la 2 pentru a fi siguri că nu se suprapun
                                                    const visibleFacilities = facilitiesArray.slice(0, maxVisible);
                                                    const remainingCount = facilitiesArray.length - maxVisible;

                                                    return (
                                                        <>
                                                            {visibleFacilities.map((facility, idx) => (
                                                                <span key={idx} className="srp-facility-badge">{facility}</span>
                                                            ))}
                                                            {remainingCount > 0 && (
                                                                <span className="srp-facility-more">+ {remainingCount} mai multe</span>
                                                            )}
                                                        </>
                                                    );
                                                })()}
                                            </div>
                                        )}
                                        <Link to={`/sportsHalls/${hall.id}`} className="srp-result-link">
                                            Vezi detalii
                                        </Link>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>

                    {/* Harta */}
                    <div className="srp-results-map">
                        <div ref={mapContainerRef} className="srp-map-container"></div>
                    </div>
                </div>
            </div>

            <Footer />

            {!isLoggedIn && (
                <>
                    <LoginModal
                        isOpen={isLoginModalOpen}
                        onClose={toggleLoginModal}
                        onRegisterClick={toggleRegisterModal}
                        onLoginSuccess={checkLoginStatus}
                    />
                    <RegisterModal
                        isOpen={isRegisterModalOpen}
                        onClose={toggleRegisterModal}
                    />
                </>
            )}
        </div>
    );
};

export default SearchResultsPage;