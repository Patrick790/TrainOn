import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { MapPin, ChevronDown, Star, Search } from 'lucide-react';
import axios from 'axios';
import Header from './Header';
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
    const markersRef = useRef([]); // Referință pentru markere
    const mapInitializedRef = useRef(false); // Flag pentru a urmări dacă harta a fost inițializată

    // Stare pentru toate datele necesare paginii
    const [sportsHalls, setSportsHalls] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [hoveredMarker, setHoveredMarker] = useState(null);
    const [mapReady, setMapReady] = useState(false); // Stare pentru a urmări dacă harta este gata

    // State pentru header și search bar - cu orașe dinamice
    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('isLoggedIn') === 'true');
    const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
    const [selectedCity, setSelectedCity] = useState(queryParams.get('city') || '');
    const [selectedActivity, setSelectedActivity] = useState(queryParams.get('activity') || 'Sport');
    const [searchQuery, setSearchQuery] = useState(queryParams.get('query') || '');
    const [cities, setCities] = useState([]); // Array pentru orașele încărcate din baza de date
    const [loadingCities, setLoadingCities] = useState(true); // Loading state pentru orașe
    const [isLocationDropdownOpen, setIsLocationDropdownOpen] = useState(false);
    const [isActivityDropdownOpen, setIsActivityDropdownOpen] = useState(false);

    const API_URL = 'http://localhost:8080';

    // Nouă funcție pentru încărcarea orașelor din baza de date
    const fetchCities = async () => {
        setLoadingCities(true);
        try {
            const response = await fetch(`${API_URL}/sportsHalls/cities`);
            if (response.ok) {
                const citiesData = await response.json();
                const cities = Array.isArray(citiesData) ? citiesData : [];

                setCities(cities);

                // Dacă nu avem un oraș selectat din query params și avem orașe disponibile
                if (!selectedCity && cities.length > 0) {
                    setSelectedCity(cities[0]);
                }

                setLoadingCities(false);
            } else {
                console.error('Eroare la încărcarea orașelor:', response.status);
                setCities([]);
                setLoadingCities(false);
            }
        } catch (error) {
            console.error('Eroare la încărcarea orașelor:', error);
            setCities([]);
            setLoadingCities(false);
        }
    };

    // Încărcăm orașele când componenta se montează
    useEffect(() => {
        fetchCities();
    }, []);

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
        // Nu încărcăm sălile până nu avem orașele încărcate și un oraș selectat
        if (loadingCities || !selectedCity) {
            return;
        }

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
    }, [selectedCity, selectedActivity, searchQuery, loadingCities, API_URL]);

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

        // Verificăm dacă au fost selectate orașul
        if (!selectedCity) {
            alert('Vă rugăm să selectați un oraș pentru căutare.');
            return;
        }

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

    // Folosim orașele din baza de date în loc de array-ul hardcodat
    const activities = ['Sport', 'Fitness', 'Inot', 'Tenis', 'Fotbal', 'Baschet', 'Handbal'];

    return (
        <div className="main-container">
            {/* Header global */}
            <Header
                isLoggedIn={isLoggedIn}
                onLoginClick={toggleLoginModal}
                onRegisterClick={toggleRegisterModal}
                onLogout={handleLogout}
            />

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
                                {loadingCities
                                    ? 'Se încarcă orașele...'
                                    : selectedCity || 'Selectează orașul'
                                }
                                <ChevronDown className="srp-icon-small" />
                            </div>
                            {isLocationDropdownOpen && !loadingCities && (
                                <div className="srp-options-list">
                                    {cities.length > 0 ? (
                                        cities.map((city) => (
                                            <div
                                                key={city}
                                                className="srp-option"
                                                onClick={() => handleLocationChange(city)}
                                            >
                                                {city}
                                            </div>
                                        ))
                                    ) : (
                                        <div className="srp-option disabled">
                                            Nu există orașe disponibile
                                        </div>
                                    )}
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

                    <button
                        type="submit"
                        className="srp-search-button"
                        disabled={loadingCities || !selectedCity}
                    >
                        <Search size={18} />
                        CAUTĂ
                    </button>
                </form>
            </div>

            {/* Conținut rezultate căutare */}
            <div className="srp-results-content">
                <h1 className="srp-results-title">
                    {loadingCities ? (
                        'Se încarcă...'
                    ) : selectedCity ? (
                        <>
                            Săli de sport în {selectedCity}
                            {selectedActivity !== 'Sport' && ` - ${selectedActivity}`}
                            {searchQuery && ` - "${searchQuery}"`}
                        </>
                    ) : (
                        'Selectați un oraș pentru a vedea sălile disponibile'
                    )}
                </h1>

                <div className="srp-results-layout">
                    {/* Lista de săli */}
                    <div className="srp-results-list">
                        {loadingCities ? (
                            <div className="srp-loading">Se încarcă orașele...</div>
                        ) : !selectedCity ? (
                            <div className="srp-no-results">Vă rugăm să selectați un oraș pentru a vedea sălile disponibile.</div>
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
                                                    // Calculăm câte facilități încap într-un spațiu limitat
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

            {/* Footer Global */}
            <Footer />

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
        </div>
    );
};

export default SearchResultsPage;