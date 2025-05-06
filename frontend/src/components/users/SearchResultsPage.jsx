import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { MapPin, ChevronDown, LogOut, Star, Search } from 'lucide-react';
import axios from 'axios';
import LoginModal from '../login/LoginModal';
import RegisterModal from '../register/RegisterModal';
import './SearchResultsPage.css';

const SearchResultsPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const queryParams = new URLSearchParams(location.search);
    const mapRef = useRef(null);
    const mapContainerRef = useRef(null);
    const markersRef = useRef([]); // Referință pentru markere
    const mapInitializedRef = useRef(false); // Flag pentru a urmări dacă harta a fost inițializată

    // Stare pentru toate datele necesare paginii
    const [sportsHalls, setSportsHalls] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [hoveredMarker, setHoveredMarker] = useState(null);
    const [mapReady, setMapReady] = useState(false); // Stare pentru a urmări dacă harta este gata

    // State pentru header și search bar - similar cu MainPage
    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('isLoggedIn') === 'true');
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
    const [selectedCity, setSelectedCity] = useState(queryParams.get('city') || 'Cluj-Napoca');
    const [selectedActivity, setSelectedActivity] = useState(queryParams.get('activity') || 'Sport');
    const [searchQuery, setSearchQuery] = useState(queryParams.get('query') || '');
    const [isLocationDropdownOpen, setIsLocationDropdownOpen] = useState(false);
    const [isActivityDropdownOpen, setIsActivityDropdownOpen] = useState(false);

    const API_URL = 'http://localhost:8080';

    // Funcție pentru geocodarea adreselor (obținerea coordonatelor din adresă)
    const geocodeAddress = async (address, city, id) => {
        if (!address || !city) {
            console.log(`Sala cu ID ${id} nu are adresă sau oraș complet.`);
            return null;
        }

        try {
            // Folosim Nominatim OpenStreetMap pentru geocodare
            const fullAddress = `${address}, ${city}, Romania`;
            const encodedAddress = encodeURIComponent(fullAddress);
            const response = await axios.get(
                `https://nominatim.openstreetmap.org/search?format=json&q=${encodedAddress}&limit=1`
            );

            // Respectăm limitele de utilizare ale API-ului Nominatim
            await new Promise(resolve => setTimeout(resolve, 1000)); // 1 second delay

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

    // Încarcă sălile de sport în funcție de parametrii de căutare
    useEffect(() => {
        const fetchSportsHalls = async () => {
            try {
                setLoading(true);
                // Ar trebui să ai un endpoint care să permită filtrarea după oraș și tip de activitate
                const response = await axios.get(`${API_URL}/sportsHalls`);

                // Filtrăm rezultatele în funcție de parametrii de căutare
                let filteredHalls = response.data;

                // Filtrare după oraș
                if (selectedCity) {
                    filteredHalls = filteredHalls.filter(hall =>
                        hall.city.toLowerCase() === selectedCity.toLowerCase()
                    );
                }

                // Filtrare după activitate (presupunând că există un câmp 'type' sau similar)
                if (selectedActivity && selectedActivity !== 'Sport') {
                    filteredHalls = filteredHalls.filter(hall =>
                        hall.type && hall.type.toLowerCase().includes(selectedActivity.toLowerCase())
                    );
                }

                // Filtrare după termenul de căutare
                if (searchQuery) {
                    filteredHalls = filteredHalls.filter(hall =>
                        hall.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                        (hall.description && hall.description.toLowerCase().includes(searchQuery.toLowerCase()))
                    );
                }

                // Verificăm și geocodăm adresele care nu au coordonate
                const hallsWithCoordinates = await Promise.all(
                    filteredHalls.map(async (hall) => {
                        // Dacă sala are deja coordonate valide, le folosim
                        if (hall.latitude && hall.longitude &&
                            !isNaN(parseFloat(hall.latitude)) &&
                            !isNaN(parseFloat(hall.longitude))) {
                            return hall;
                        }

                        // Dacă nu are coordonate dar are adresă, încercăm să geocodăm
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

                        // Dacă geocodarea eșuează sau nu există adresă, returnăm sala fără coordonate
                        return hall;
                    })
                );

                setSportsHalls(hallsWithCoordinates);
                setLoading(false);
            } catch (err) {
                console.error('Eroare la încărcarea sălilor:', err);
                setError('Nu s-au putut încărca sălile de sport.');
                setLoading(false);
            }
        };

        fetchSportsHalls();
    }, [selectedCity, selectedActivity, searchQuery, API_URL]);

    // Funcție separată pentru inițializarea hărții
    const initializeMap = () => {
        if (!mapContainerRef.current || !window.L || mapInitializedRef.current) {
            return;
        }

        try {
            // Curățăm harta anterioară dacă există
            if (mapRef.current) {
                mapRef.current.remove();
            }

            console.log('Inițializez harta...');

            // Creăm harta și adăugăm tile-urile OpenStreetMap
            const map = window.L.map(mapContainerRef.current, {
                // Opțiuni suplimentare pentru stabilitate
                fadeAnimation: false,      // Dezactivăm animațiile fade pentru stabilitate
                zoomAnimation: false,      // Dezactivăm animațiile zoom pentru stabilitate
                markerZoomAnimation: false // Dezactivăm animațiile zoom pentru markere
            }).setView([46.7712, 23.6236], 13); // Centru Cluj-Napoca

            window.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(map);

            mapRef.current = map;
            mapInitializedRef.current = true;

            // Invalidăm dimensiunea pentru a forța o recalculare corectă
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

    // Funcție separată pentru adăugarea markerelor
    const addMarkersToMap = () => {
        if (!mapRef.current || !sportsHalls.length || !mapReady) {
            return;
        }

        try {
            console.log('Adaug markere pe hartă...');

            // Curățăm markerele existente
            markersRef.current.forEach(marker => {
                if (marker && marker.remove) {
                    marker.remove();
                }
            });
            markersRef.current = [];

            // Adăugăm markeri noi
            const bounds = [];
            let validMarkers = 0;

            sportsHalls.forEach(hall => {
                // Verificăm dacă sala are coordonate valide
                if (!hall.latitude || !hall.longitude) {
                    console.log(`Sala ${hall.name} nu are coordonate valide. Nu se adaugă marker.`);
                    return; // Skip la această sală
                }

                const lat = parseFloat(hall.latitude);
                const lng = parseFloat(hall.longitude);

                // Verificăm dacă coordonatele sunt numere valide
                if (isNaN(lat) || isNaN(lng)) {
                    console.log(`Sala ${hall.name} are coordonate invalide. Nu se adaugă marker.`);
                    return; // Skip la această sală
                }

                bounds.push([lat, lng]);
                validMarkers++;

                try {
                    // Creăm și adăugăm markerul la hartă
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

            // Ajustăm vizualizarea hărții doar dacă avem markere valide
            if (bounds.length > 0) {
                try {
                    mapRef.current.fitBounds(bounds, { padding: [30, 30] });
                    console.log(`Au fost adăugate ${validMarkers} markere pe hartă.`);
                } catch (boundsError) {
                    console.error('Eroare la fitBounds:', boundsError);
                    // Plan de rezervă: centrăm pe Cluj-Napoca dacă fitBounds eșuează
                    mapRef.current.setView([46.7712, 23.6236], 13);
                }
            } else {
                console.log('Nu există săli cu coordonate valide pentru afișare pe hartă.');
                // Dacă nu există markere, centrăm harta pe Cluj-Napoca
                mapRef.current.setView([46.7712, 23.6236], 13);
            }
        } catch (error) {
            console.error('Eroare la adăugarea markerelor:', error);
        }
    };

    // Inițializarea hărții când componenta se montează
    useEffect(() => {
        // Verificăm dacă DOM este gata
        if (mapContainerRef.current && typeof window !== 'undefined' && window.L) {
            initializeMap();
        }

        // Cleanup când componenta se demontează
        return () => {
            if (mapRef.current) {
                // Curățăm markerele
                markersRef.current.forEach(marker => {
                    if (marker && marker.remove) {
                        marker.remove();
                    }
                });
                markersRef.current = [];

                // Apoi eliminăm harta
                mapRef.current.remove();
                mapRef.current = null;
                mapInitializedRef.current = false;
                setMapReady(false);
            }
        };
    }, []);

    // Adăugăm markere când harta este gata și datele sunt disponibile
    useEffect(() => {
        if (!loading && !error && sportsHalls.length > 0 && mapReady) {
            addMarkersToMap();
        }
    }, [sportsHalls, loading, error, mapReady]);

    // Actualizăm dimensiunea hărții când fereastra se redimensionează
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

    const toggleLocationDropdown = () => {
        setIsLocationDropdownOpen(!isLocationDropdownOpen);
        setIsActivityDropdownOpen(false);
    };

    const toggleActivityDropdown = () => {
        setIsActivityDropdownOpen(!isActivityDropdownOpen);
        setIsLocationDropdownOpen(false);
    };

    const handleLocationChange = (city) => {
        setSelectedCity(city);
        setIsLocationDropdownOpen(false);
        updateURLParams('city', city);
    };

    const handleActivityChange = (activity) => {
        setSelectedActivity(activity);
        setIsActivityDropdownOpen(false);
        updateURLParams('activity', activity);
    };

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
    };

    const handleSearch = (e) => {
        e.preventDefault();
        updateURLParams('query', searchQuery);
    };

    // Actualizează parametrii URL fără a reîncărca pagina
    const updateURLParams = (key, value) => {
        const searchParams = new URLSearchParams(window.location.search);
        searchParams.set(key, value);
        const newUrl = `${window.location.pathname}?${searchParams.toString()}`;
        window.history.pushState({ path: newUrl }, '', newUrl);
    };

    // Funcție pentru calcularea ratingului
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

    // Funcție pentru afișarea stelelor de rating
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

    // Funcție pentru obținerea URL-ului imaginii
    const getImageUrl = (hall) => {
        if (hall.images && hall.images.length > 0) {
            const coverImage = hall.images.find(img => img.description === 'cover') || hall.images[0];
            return `${API_URL}/images/${coverImage.id}`;
        }
        return 'https://via.placeholder.com/400x250?text=Imagine+Indisponibilă';
    };

    // Formatare preț
    const formatPrice = (price) => {
        if (!price) return 'Preț la cerere';
        return `${price} RON/oră`;
    };

    const locations = ['Cluj-Napoca', 'Timisoara', 'Bucuresti', 'Iasi'];
    const activities = ['Sport', 'Fitness', 'Inot', 'Tenis', 'Fotbal', 'Baschet', 'Handbal'];

    return (
        <div className="main-container">
            {/* Header - identic cu MainPage */}
            <header className="header">
                <div className="logo-container">
                    <Link to="/" className="logo-link">
                        <div className="logo"></div>
                        <span className="logo-text">Licenta</span>
                    </Link>
                </div>
                <div className="auth-buttons">
                    {isLoggedIn ? (
                        <button onClick={handleLogout} className="auth-button logout-button">
                            <LogOut size={16} />
                            Deconectare
                        </button>
                    ) : (
                        <>
                            <button onClick={toggleLoginModal} className="auth-button">
                                Intra in cont
                            </button>
                            <button onClick={toggleRegisterModal} className="auth-button">
                                Inregistrare
                            </button>
                        </>
                    )}
                </div>
            </header>

            {/* Search Bar - similar cu MainPage dar fără background image */}
            <div className="srp-search-container">
                <form onSubmit={handleSearch} className="srp-search-bar">
                    <div className="srp-input-group">
                        <MapPin className="srp-icon" />
                        <div className="srp-custom-select">
                            <div
                                className="srp-selected-option"
                                onClick={toggleLocationDropdown}
                            >
                                {selectedCity}
                                <ChevronDown className="srp-icon-small" />
                            </div>
                            {isLocationDropdownOpen && (
                                <div className="srp-options-list">
                                    {locations.map((loc) => (
                                        <div
                                            key={loc}
                                            className="srp-option"
                                            onClick={() => handleLocationChange(loc)}
                                        >
                                            {loc}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="srp-input-group">
                        <div className="srp-custom-select">
                            <div
                                className="srp-selected-option"
                                onClick={toggleActivityDropdown}
                            >
                                {selectedActivity}
                                <ChevronDown className="srp-icon-small" />
                            </div>
                            {isActivityDropdownOpen && (
                                <div className="srp-options-list">
                                    {activities.map((act) => (
                                        <div
                                            key={act}
                                            className="srp-option"
                                            onClick={() => handleActivityChange(act)}
                                        >
                                            {act}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    <input
                        type="text"
                        value={searchQuery}
                        onChange={handleSearchChange}
                        placeholder="Denumire sala de sport"
                        className="srp-search-input"
                    />

                    <button type="submit" className="srp-search-button">
                        <Search size={18} />
                        CAUTĂ
                    </button>
                </form>
            </div>

            {/* Conținut rezultate căutare */}
            <div className="srp-results-content">
                <h1 className="srp-results-title">
                    Săli de sport în {selectedCity}
                    {selectedActivity !== 'Sport' && ` - ${selectedActivity}`}
                    {searchQuery && ` - "${searchQuery}"`}
                </h1>

                <div className="srp-results-layout">
                    {/* Lista de săli */}
                    <div className="srp-results-list">
                        {loading ? (
                            <div className="srp-loading">Se încarcă...</div>
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
                                                {hall.facilities.split(',').slice(0, 3).map((facility, idx) => (
                                                    <span key={idx} className="srp-facility-badge">{facility.trim()}</span>
                                                ))}
                                                {hall.facilities.split(',').length > 3 && (
                                                    <span className="srp-facility-more">+ {hall.facilities.split(',').length - 3} mai multe</span>
                                                )}
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

            {/* Footer */}
            <footer className="app-footer">
                <div className="footer-content">
                    <div className="footer-logo">
                        <div className="footer-logo-icon"></div>
                        <span className="footer-logo-text">Licenta</span>
                    </div>
                    <div className="footer-links">
                        <a href="/terms" className="footer-link">Termeni și condiții</a>
                        <a href="/privacy" className="footer-link">Politica de confidențialitate</a>
                        <a href="/contact" className="footer-link">Contact</a>
                    </div>
                    <div className="footer-copyright">
                        &copy; {new Date().getFullYear()} Platforma de Administrare Săli Sport
                    </div>
                </div>
            </footer>

            {/* Modals */}
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

            {/* Stiluri inline pentru a rezolva problema cu hover-ul cardurilor */}
            <style>
                {`
                .srp-result-card {
                    display: flex;
                    background-color: white;
                    border-radius: 10px;
                    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
                    overflow: hidden;
                    height: 180px;
                    cursor: pointer;
                    /* Adăugăm un border transparent implicit pentru a rezerva spațiul */
                    border: 2px solid transparent;
                    /* Adăugăm o tranziție subtilă doar pentru box-shadow */
                    transition: box-shadow 0.2s ease;
                    /* Asigurăm că poziția rămâne fixă */
                    position: relative;
                }

                .srp-result-card:hover {
                    /* Păstrăm același box-shadow, dar cu intensitate crescută */
                    box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
                    /* Nu modificăm alte proprietăți care ar putea cauza mișcare */
                }

                .srp-result-highlighted {
                    border: 2px solid #6d28d9;
                    box-shadow: 0 6px 12px rgba(109, 40, 217, 0.2);
                    /* Nu mai este nevoie să modificăm alte proprietăți */
                }
                `}
            </style>
        </div>
    );
};

export default SearchResultsPage;