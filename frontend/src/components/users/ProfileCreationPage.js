import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, User, Clock, DollarSign, MapPin, Plus, Edit, Trash, Save, CreditCard, Settings, AlertCircle, CheckCircle, Trophy } from 'lucide-react';
import axios from 'axios';
import CardManagementModal from './CardManagementModal';
import './ProfileCreationPage.css';

const STRIPE_PUBLISHABLE_KEY = 'pk_test_51RUSNJB90iMxhg7K13VbDV4YNESfylJS8mNsenCauFMwbo2t9rDEptGatnEwR2lzQrgb7liaFKyFTjfyMkq2ooT900FSp78Ooy';

const ProfileCreationPage = () => {
    const navigate = useNavigate();
    const [profiles, setProfiles] = useState([]);
    const [currentProfile, setCurrentProfile] = useState({
        name: '',
        ageCategory: '0-14',
        timeInterval: '7-14:30',
        weeklyBudget: '',
        city: '',
        sport: 'fotbal', // ADĂUGAT: câmpul sport cu valoare default
        selectedHalls: [],
        autoPaymentEnabled: false,
        autoPaymentMethod: null,
        autoPaymentThreshold: '',
        maxWeeklyAutoPayment: '',
        defaultCardPaymentMethodId: null
    });
    const [availableHalls, setAvailableHalls] = useState([]);
    const [userCards, setUserCards] = useState([]);
    const [activeStep, setActiveStep] = useState(1);
    const [isAddingProfile, setIsAddingProfile] = useState(true);
    const [editingProfileId, setEditingProfileId] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [showCardModal, setShowCardModal] = useState(false);
    const [stripe, setStripe] = useState(null);

    // URL-ul API-ului
    const API_URL = process.env.REACT_APP_API_URL || '';

    // Constante pentru formulare
    const ageCategories = [
        { value: '0-14', label: '0-14 ani' },
        { value: '15-16', label: '15-16 ani' },
        { value: '17-18', label: '17-18 ani' },
        { value: 'seniori', label: 'Seniori' }
    ];

    const timeIntervals = [
        { value: '7-14:30', label: '7:00 - 14:30' },
        { value: '14:30-23', label: '14:30 - 23:00' },
        { value: 'full-day', label: 'Toată ziua' }
    ];

    const paymentMethods = [
        { value: 'CARD', label: 'Card de Credit/Debit' },
        { value: 'CASH', label: 'Plată la Fața Locului' }
    ];

    // ADĂUGAT: Lista extinsă de sporturi disponibile
    const availableSports = [
        // Sporturi de echipă
        { value: 'fotbal', label: 'Fotbal' },
        { value: 'fotbal-sala', label: 'Fotbal în Sală (Futsal)' },
        { value: 'baschet', label: 'Baschet' },
        { value: 'volei', label: 'Volei' },
        { value: 'handbal', label: 'Handbal' },
        { value: 'rugby', label: 'Rugby' },
        { value: 'hochei', label: 'Hochei pe Gheață' },
        { value: 'polo-apa', label: 'Polo pe Apă' },

        // Sporturi cu rachetă
        { value: 'tenis', label: 'Tenis' },
        { value: 'tenis-masa', label: 'Tenis de Masă' },
        { value: 'badminton', label: 'Badminton' },
        { value: 'squash', label: 'Squash' },
        { value: 'padel', label: 'Padel' },

        // Sporturi de luptă
        { value: 'box', label: 'Box' },
        { value: 'kickbox', label: 'Kickbox' },
        { value: 'karate', label: 'Karate' },
        { value: 'judo', label: 'Judo' },
        { value: 'taekwondo', label: 'Taekwondo' },
        { value: 'mma', label: 'MMA (Arte Marțiale Mixte)' },
        { value: 'wrestling', label: 'Wrestling' },
        { value: 'aikido', label: 'Aikido' },
        { value: 'brazilian-jiu-jitsu', label: 'Brazilian Jiu-Jitsu' },

        // Înot și sporturi acvatice
        { value: 'inot', label: 'Înot' },
        { value: 'aqua-aerobic', label: 'Aqua Aerobic' },
        { value: 'sincron', label: 'Înot Sincron' },
        { value: 'sarituri-apa', label: 'Sărituri în Apă' },
        { value: 'waterpolo', label: 'Waterpolo' },

        // Gimnastică și fitness
        { value: 'gimnastica', label: 'Gimnastică Artistică' },
        { value: 'gimnastica-ritmica', label: 'Gimnastică Ritmică' },
        { value: 'aerobic', label: 'Aerobic Sportiv' },
        { value: 'fitness', label: 'Fitness / Culturism' },
        { value: 'crossfit', label: 'CrossFit' },
        { value: 'pilates', label: 'Pilates' },
        { value: 'yoga', label: 'Yoga' },
        { value: 'zumba', label: 'Zumba' },
        { value: 'spinning', label: 'Spinning / Ciclism Indoor' },

        // Sporturi individuale
        { value: 'atletism', label: 'Atletism' },
        { value: 'haltere', label: 'Haltere' },
        { value: 'escalada', label: 'Escaladă' },
        { value: 'dans-sportiv', label: 'Dans Sportiv' },
        { value: 'scrima', label: 'Scrimă' },
        { value: 'tir-cu-arcul', label: 'Tir cu Arcul' },

        // Sporturi cu mingea
        { value: 'bowling', label: 'Bowling' },
        { value: 'biliard', label: 'Biliard' },

        // Sporturi pentru copii
        { value: 'sport-copii', label: 'Sport pentru Copii (Multisport)' },
        { value: 'baby-swimming', label: 'Înot pentru Bebeluși' },

        // Activități de grup
        { value: 'aqua-fitness', label: 'Aqua Fitness' },
        { value: 'step-aerobic', label: 'Step Aerobic' },
        { value: 'body-pump', label: 'Body Pump' },
        { value: 'tabata', label: 'Tabata' },
        { value: 'hiit', label: 'HIIT (High Intensity Interval Training)' },

        // Opțiune generală
        { value: 'multipla', label: 'Sală Multiplă (Toate Sporturile)' }
    ];

    const [availableCities, setAvailableCities] = useState([]);

    // Configurare axios cu autentificare
    const api = axios.create();
    api.interceptors.request.use(function (config) {
        const token = localStorage.getItem('jwtToken');
        config.headers.Authorization = `Bearer ${token}`;
        return config;
    }, function (error) {
        return Promise.reject(error);
    });

    // Încarcă Stripe
    useEffect(() => {
        const loadStripe = async () => {
            if (window.Stripe) {
                setStripe(window.Stripe(STRIPE_PUBLISHABLE_KEY));
                return;
            }

            const script = document.createElement('script');
            script.src = 'https://js.stripe.com/v3/';
            script.onload = () => {
                if (window.Stripe) {
                    setStripe(window.Stripe(STRIPE_PUBLISHABLE_KEY));
                }
            };
            document.head.appendChild(script);
        };

        loadStripe();
    }, []);

    // Funcții pentru încărcarea datelor
    const fetchProfiles = async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await api.get(`${API_URL}/reservationProfiles`);
            setProfiles(response.data);
        } catch (err) {
            console.error('Eroare la încărcarea profilurilor:', err);
            setError('Nu s-au putut încărca profilurile. Verificați conexiunea la internet sau autentificarea.');
        } finally {
            setLoading(false);
        }
    };

    const fetchUserCards = async () => {
        try {
            const response = await api.get(`${API_URL}/card-payment-methods`);
            if (response.data.success) {
                setUserCards(response.data.cards);
            }
        } catch (err) {
            console.error('Eroare la încărcarea cardurilor:', err);
        }
    };

    const fetchCities = async () => {
        try {
            const response = await fetch(`${API_URL}/sportsHalls/cities`);            if (response.ok) {
                const citiesData = await response.json();
                const cities = Array.isArray(citiesData) ? citiesData : [];
                console.log(`Loaded ${cities.length} cities for profile creation`);
                setAvailableCities(cities);
            } else {
                console.error('Eroare la încărcarea orașelor:', response.status);
                setAvailableCities([]);
            }
        } catch (error) {
            console.error('Eroare la încărcarea orașelor:', error);
            setAvailableCities([]);
        }
    };

    useEffect(() => {
        fetchProfiles();
        fetchUserCards();
        fetchCities();
    }, []);

    // Listener pentru actualizările de carduri din modal
    useEffect(() => {
        const handleCardsUpdated = () => {
            fetchUserCards();
            // Dacă editezi un profil, reîncarcă și profilurile pentru a avea datele actualizate
            if (editingProfileId) {
                fetchProfiles();
            }
        };

        window.addEventListener('cardsUpdated', handleCardsUpdated);

        return () => {
            window.removeEventListener('cardsUpdated', handleCardsUpdated);
        };
    }, [editingProfileId]);

    // Încarcă sălile pe baza orașului selectat (fără filtrare pe sport)
    // Înlocuiește useEffect-ul pentru încărcarea sălilor (în jurul liniei 180-200) cu acest cod:

// Încarcă sălile pe baza orașului selectat (doar sălile ACTIVE)
    useEffect(() => {
        if (currentProfile.city) {
            setLoading(true);
            setError(null);

            // SCHIMBAREA PRINCIPALĂ: folosim endpoint-ul /active în loc de cel general
            api.get(`${API_URL}/sportsHalls/active`)
                .then(response => {
                    // Filtrăm sălile ACTIVE din orașul selectat
                    const filteredHalls = response.data.filter(hall =>
                        hall.city && hall.city.toLowerCase() === currentProfile.city.toLowerCase()
                    );
                    console.log(`Loaded ${filteredHalls.length} active sports halls for city: ${currentProfile.city}`);
                    setAvailableHalls(filteredHalls);
                    setLoading(false);
                })
                .catch(err => {
                    console.error(`Eroare la obținerea sălilor active din ${currentProfile.city}:`, err);
                    setError(`Nu s-au putut încărca sălile active din ${currentProfile.city}. Verificați conexiunea la internet.`);
                    setLoading(false);
                    setAvailableHalls([]);
                });
        } else {
            setAvailableHalls([]);
        }
    }, [currentProfile.city]);

    // Event handlers
    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setCurrentProfile({
            ...currentProfile,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleHallSelection = (hallId) => {
        const isSelected = currentProfile.selectedHalls.includes(hallId);

        if (isSelected) {
            setCurrentProfile({
                ...currentProfile,
                selectedHalls: currentProfile.selectedHalls.filter(id => id !== hallId)
            });
        } else {
            setCurrentProfile({
                ...currentProfile,
                selectedHalls: [...currentProfile.selectedHalls, hallId]
            });
        }
    };

    const handleNextStep = () => {
        setActiveStep(prevStep => prevStep + 1);
    };

    const handlePrevStep = () => {
        setActiveStep(prevStep => prevStep - 1);
    };

    const resetCurrentProfile = () => {
        setCurrentProfile({
            name: '',
            ageCategory: '0-14',
            timeInterval: '7-14:30',
            weeklyBudget: '',
            city: '',
            sport: 'fotbal', // ADĂUGAT: resetare sport
            selectedHalls: [],
            autoPaymentEnabled: false,
            autoPaymentMethod: null,
            autoPaymentThreshold: '',
            maxWeeklyAutoPayment: '',
            defaultCardPaymentMethodId: null
        });
    };

    const handleAddProfile = async () => {
        if (currentProfile.name && currentProfile.weeklyBudget && currentProfile.city && currentProfile.sport) {
            setLoading(true);
            setError(null);

            try {
                const profileData = {
                    name: currentProfile.name,
                    ageCategory: currentProfile.ageCategory,
                    timeInterval: currentProfile.timeInterval,
                    weeklyBudget: currentProfile.weeklyBudget,
                    city: currentProfile.city,
                    sport: currentProfile.sport, // ADĂUGAT: includere sport în datele trimise
                    selectedHalls: currentProfile.selectedHalls,
                    autoPaymentEnabled: currentProfile.autoPaymentEnabled,
                    autoPaymentMethod: currentProfile.autoPaymentEnabled ? currentProfile.autoPaymentMethod : null,
                    autoPaymentThreshold: currentProfile.autoPaymentThreshold || null,
                    maxWeeklyAutoPayment: currentProfile.maxWeeklyAutoPayment || null,
                    defaultCardPaymentMethodId: currentProfile.defaultCardPaymentMethodId || null
                };

                if (editingProfileId) {
                    // Pentru editare, salvează și apoi reîncarcă datele
                    await api.put(`${API_URL}/reservationProfiles/${editingProfileId}`, profileData);

                    // Reîncarcă profilurile pentru a obține datele actualizate
                    await fetchProfiles();

                    // Găsește profilul actualizat și setează-l în formular
                    const updatedProfilesResponse = await api.get(`${API_URL}/reservationProfiles`);
                    const updatedProfile = updatedProfilesResponse.data.find(p => p.id === editingProfileId);

                    if (updatedProfile) {
                        const hallIds = updatedProfile.selectedHalls ? updatedProfile.selectedHalls.map(hall => hall.id) : [];
                        setCurrentProfile({
                            ...updatedProfile,
                            sport: updatedProfile.sport || 'fotbal', // FIXAT: asigură-te că sportul are o valoare default
                            selectedHalls: hallIds,
                            autoPaymentEnabled: updatedProfile.autoPaymentEnabled || false,
                            autoPaymentMethod: updatedProfile.autoPaymentMethod || null,
                            autoPaymentThreshold: updatedProfile.autoPaymentThreshold || '',
                            maxWeeklyAutoPayment: updatedProfile.maxWeeklyAutoPayment || '',
                            defaultCardPaymentMethodId: updatedProfile.defaultCardPaymentMethod?.id || null
                        });
                    }
                } else {
                    // Pentru profil nou
                    await api.post(`${API_URL}/reservationProfiles`, profileData);
                    await fetchProfiles();
                    resetCurrentProfile();
                    setActiveStep(1);
                    setIsAddingProfile(true);
                    setEditingProfileId(null);
                }

            } catch (err) {
                console.error('Eroare la salvarea profilului:', err);
                setError('Nu s-a putut salva profilul. Verificați conexiunea la internet sau autentificarea.');
            } finally {
                setLoading(false);
            }
        }
    };

    const handleRemoveProfile = async (profileId) => {
        const confirmDelete = window.confirm('Ești sigur că vrei să ștergi acest profil?');
        if (confirmDelete) {
            setLoading(true);
            setError(null);

            try {
                await api.delete(`${API_URL}/reservationProfiles/${profileId}`);
                await fetchProfiles();

                if (editingProfileId === profileId) {
                    resetCurrentProfile();
                    setEditingProfileId(null);
                    setActiveStep(1);
                    setIsAddingProfile(true);
                }
            } catch (err) {
                console.error('Eroare la ștergerea profilului:', err);
                setError('Nu s-a putut șterge profilul. Verificați conexiunea la internet sau autentificarea.');
            } finally {
                setLoading(false);
            }
        }
    };

    const handleEditProfile = (profile) => {
        const hallIds = profile.selectedHalls ? profile.selectedHalls.map(hall => hall.id) : [];

        setCurrentProfile({
            ...profile,
            sport: profile.sport || 'fotbal', // FIXAT: asigură-te că sportul are o valoare default
            selectedHalls: hallIds,
            autoPaymentEnabled: profile.autoPaymentEnabled || false,
            autoPaymentMethod: profile.autoPaymentMethod || null,
            autoPaymentThreshold: profile.autoPaymentThreshold || '',
            maxWeeklyAutoPayment: profile.maxWeeklyAutoPayment || '',
            defaultCardPaymentMethodId: profile.defaultCardPaymentMethod?.id || null
        });
        setEditingProfileId(profile.id);
        setIsAddingProfile(false);
        setActiveStep(1);

        // Refresh cardurile când editezi un profil
        fetchUserCards();
    };

    const handleFinish = () => {
        navigate(-1);
    };

    const handleCancel = () => {
        if (currentProfile.name || currentProfile.city || currentProfile.weeklyBudget || currentProfile.selectedHalls.length > 0) {
            const confirmCancel = window.confirm('Ai modificări nesalvate. Ești sigur că vrei să ieși fără a salva?');
            if (confirmCancel) {
                navigate(-1);
            }
        } else {
            navigate(-1);
        }
    };

    const handleNewProfile = () => {
        resetCurrentProfile();
        setEditingProfileId(null);
        setIsAddingProfile(true);
        setActiveStep(1);
    };

    // Funcții helper
    const formatPrice = (price) => {
        if (!price) return 'Preț la cerere';
        return `${price} RON/1h30`;
    };

    const selectedCard = userCards.find(card => card.id === currentProfile.defaultCardPaymentMethodId);

    const isFormValid = () => {
        if (!currentProfile.city || currentProfile.selectedHalls.length === 0) {
            return false;
        }

        if (currentProfile.autoPaymentEnabled &&
            currentProfile.autoPaymentMethod === 'CARD' &&
            !currentProfile.defaultCardPaymentMethodId) {
            return false;
        }

        return true;
    };

    return (
        <div className="reserv-profile-page-container">
            <header className="reserv-profile-page-header">
                <div className="reserv-profile-header-content">
                    <Link to="/" className="reserv-profile-back-link">
                        <ArrowLeft size={18} />
                        <span>Înapoi</span>
                    </Link>
                    <h1 className="reserv-profile-page-title">Profiluri de rezervare</h1>
                </div>
            </header>

            <div className="reserv-profile-page-content">
                {/* Sidebar cu lista de profiluri */}
                <aside className="reserv-profile-page-sidebar">
                    <div className="reserv-profile-sidebar-header">
                        <h2>Profiluri create</h2>
                        <button
                            className="reserv-add-profile-button"
                            onClick={handleNewProfile}
                        >
                            <Plus size={18} />
                            <span>Profil nou</span>
                        </button>
                    </div>

                    <div className="reserv-profile-list">
                        {loading && profiles.length === 0 ? (
                            <div className="reserv-loading-message">
                                <p>Se încarcă profilurile...</p>
                            </div>
                        ) : error ? (
                            <div className="reserv-error-message">
                                <p>{error}</p>
                            </div>
                        ) : profiles.length === 0 ? (
                            <div className="reserv-no-profiles">
                                <p>Nu ai creat încă niciun profil de rezervare.</p>
                                <p>Creează primul tău profil pentru a începe.</p>
                            </div>
                        ) : (
                            profiles.map(profile => (
                                <div
                                    key={profile.id}
                                    className={`reserv-profile-list-item ${editingProfileId === profile.id ? 'active' : ''}`}
                                >
                                    <div className="reserv-profile-list-info" onClick={() => handleEditProfile(profile)}>
                                        <h3>{profile.name}</h3>
                                        <div className="reserv-profile-list-details">
                                            <span>
                                                <User size={14} />
                                                {ageCategories.find(cat => cat.value === profile.ageCategory)?.label}
                                            </span>
                                            <span>
                                                <Clock size={14} />
                                                {timeIntervals.find(t => t.value === profile.timeInterval)?.label}
                                            </span>
                                            <span>
                                                <MapPin size={14} />
                                                {profile.city}
                                            </span>
                                            <span>
                                                <Trophy size={14} />
                                                {availableSports.find(s => s.value === profile.sport)?.label}
                                            </span>
                                            <span>
                                                <DollarSign size={14} />
                                                {profile.weeklyBudget} RON/săptămână
                                            </span>
                                            {profile.autoPaymentEnabled && (
                                                <span>
                                                    <CheckCircle size={14} />
                                                    Plată automată activă
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <div className="reserv-profile-list-actions">
                                        <button
                                            className="reserv-profile-edit-button"
                                            onClick={() => handleEditProfile(profile)}
                                            title="Editează profil"
                                        >
                                            <Edit size={16} />
                                        </button>
                                        <button
                                            className="reserv-profile-delete-button"
                                            onClick={() => handleRemoveProfile(profile.id)}
                                            title="Șterge profil"
                                        >
                                            <Trash size={16} />
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </aside>

                {/* Formularul principal */}
                <main className="reserv-profile-form-container">
                    <h2 className="reserv-profile-form-title">
                        {editingProfileId ? 'Editare profil' : 'Creare profil nou'}
                    </h2>

                    {/* Indicator pentru pași */}
                    <div className="reserv-profile-steps-indicator">
                        <div className={`reserv-step ${activeStep >= 1 ? 'active' : ''}`}>
                            <div className="reserv-step-number">1</div>
                            <div className="reserv-step-label">Informații de bază</div>
                        </div>
                        <div className="reserv-step-connector"></div>
                        <div className={`reserv-step ${activeStep >= 2 ? 'active' : ''}`}>
                            <div className="reserv-step-number">2</div>
                            <div className="reserv-step-label">Locație și săli</div>
                        </div>
                        <div className="reserv-step-connector"></div>
                        <div className={`reserv-step ${activeStep >= 3 ? 'active' : ''}`}>
                            <div className="reserv-step-number">3</div>
                            <div className="reserv-step-label">Plăți automate</div>
                        </div>
                    </div>

                    <div className="reserv-profile-form">
                        {/* Pasul 1: Informații de bază */}
                        {activeStep === 1 && (
                            <div className="reserv-form-step">
                                <div className="reserv-form-group">
                                    <label htmlFor="name">Nume profil / echipă</label>
                                    <input
                                        type="text"
                                        id="name"
                                        name="name"
                                        value={currentProfile.name}
                                        onChange={handleChange}
                                        placeholder="Introduceți numele echipei"
                                        required
                                    />
                                </div>

                                <div className="reserv-form-group">
                                    <label htmlFor="ageCategory">Categorie de vârstă</label>
                                    <select
                                        id="ageCategory"
                                        name="ageCategory"
                                        value={currentProfile.ageCategory}
                                        onChange={handleChange}
                                    >
                                        {ageCategories.map(category => (
                                            <option key={category.value} value={category.value}>
                                                {category.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="reserv-form-group">
                                    <label htmlFor="sport">Sport</label>
                                    <select
                                        id="sport"
                                        name="sport"
                                        value={currentProfile.sport}
                                        onChange={handleChange}
                                        required
                                    >
                                        {availableSports.map(sport => (
                                            <option key={sport.value} value={sport.value}>
                                                {sport.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="reserv-form-group">
                                    <label htmlFor="timeInterval">Interval orar</label>
                                    <select
                                        id="timeInterval"
                                        name="timeInterval"
                                        value={currentProfile.timeInterval}
                                        onChange={handleChange}
                                    >
                                        {timeIntervals.map(interval => (
                                            <option key={interval.value} value={interval.value}>
                                                {interval.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="reserv-form-group">
                                    <label htmlFor="weeklyBudget">Buget săptămânal (RON)</label>
                                    <input
                                        type="number"
                                        id="weeklyBudget"
                                        name="weeklyBudget"
                                        value={currentProfile.weeklyBudget}
                                        onChange={handleChange}
                                        placeholder="Introduceți bugetul săptămânal"
                                        required
                                    />
                                </div>

                                <div className="reserv-form-buttons">
                                    <button
                                        type="button"
                                        className="reserv-form-button-cancel"
                                        onClick={handleCancel}
                                    >
                                        Anulează
                                    </button>
                                    <button
                                        type="button"
                                        className="reserv-form-button-next"
                                        onClick={handleNextStep}
                                        disabled={!currentProfile.name || !currentProfile.weeklyBudget || !currentProfile.sport}
                                    >
                                        Continuare
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* Pasul 2: Locație și săli */}
                        {activeStep === 2 && (
                            <div className="reserv-form-step">
                                <div className="reserv-form-group">
                                    <label htmlFor="city">Oraș</label>
                                    <select
                                        id="city"
                                        name="city"
                                        value={currentProfile.city}
                                        onChange={handleChange}
                                        required
                                    >
                                        <option value="">Selectează orașul</option>
                                        {availableCities.map(city => (
                                            <option key={city} value={city}>
                                                {city}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {currentProfile.city && (
                                    <div className="reserv-form-group">
                                        <label>Săli disponibile în {currentProfile.city}</label>
                                        {loading ? (
                                            <div className="reserv-loading-message">Se încarcă sălile disponibile...</div>
                                        ) : error ? (
                                            <div className="reserv-error-message">{error}</div>
                                        ) : (
                                            <div className="reserv-halls-selector">
                                                {availableHalls.length === 0 ? (
                                                    <p className="reserv-no-halls-message">Nu există săli disponibile în orașul selectat.</p>
                                                ) : (
                                                    availableHalls.map(hall => (
                                                        <div
                                                            key={hall.id}
                                                            className={`reserv-hall-option ${currentProfile.selectedHalls.includes(hall.id) ? 'selected' : ''}`}
                                                            onClick={() => handleHallSelection(hall.id)}
                                                        >
                                                            <div className="reserv-hall-checkbox">
                                                                {currentProfile.selectedHalls.includes(hall.id) && <span>✓</span>}
                                                            </div>
                                                            <div className="reserv-hall-info">
                                                                <div className="reserv-hall-name">{hall.name}</div>
                                                                <div className="reserv-hall-price">{formatPrice(hall.tariff)}</div>
                                                                <div className="reserv-hall-type">Tip: {hall.type}</div>
                                                            </div>
                                                        </div>
                                                    ))
                                                )}
                                            </div>
                                        )}
                                    </div>
                                )}

                                <div className="reserv-form-buttons">
                                    <button
                                        type="button"
                                        className="reserv-form-button-prev"
                                        onClick={handlePrevStep}
                                    >
                                        Înapoi
                                    </button>
                                    <button
                                        type="button"
                                        className="reserv-form-button-next"
                                        onClick={handleNextStep}
                                        disabled={!currentProfile.city || currentProfile.selectedHalls.length === 0}
                                    >
                                        Continuare
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* Pasul 3: Plăți automate */}
                        {activeStep === 3 && (
                            <div className="reserv-form-step">
                                <div className="reserv-form-group">
                                    <label className="reserv-checkbox-label">
                                        <input
                                            type="checkbox"
                                            name="autoPaymentEnabled"
                                            checked={currentProfile.autoPaymentEnabled}
                                            onChange={handleChange}
                                        />
                                        <span>Activează plăți automate</span>
                                    </label>
                                    <small>Permite debitarea automată a cardului pentru rezervări</small>
                                </div>

                                {currentProfile.autoPaymentEnabled && (
                                    <>
                                        <div className="reserv-form-group">
                                            <label htmlFor="autoPaymentMethod">Metoda de plată automată</label>
                                            <select
                                                id="autoPaymentMethod"
                                                name="autoPaymentMethod"
                                                value={currentProfile.autoPaymentMethod || ''}
                                                onChange={handleChange}
                                                required
                                            >
                                                <option value="">Selectează metoda de plată</option>
                                                {paymentMethods.map(method => (
                                                    <option key={method.value} value={method.value}>
                                                        {method.label}
                                                    </option>
                                                ))}
                                            </select>
                                        </div>

                                        {currentProfile.autoPaymentMethod === 'CARD' && (
                                            <div className="reserv-form-group">
                                                <label>Card de plată</label>
                                                <div className="reserv-card-selection">
                                                    {userCards.length === 0 ? (
                                                        <div className="reserv-no-cards">
                                                            <p>Nu aveți carduri salvate</p>
                                                            <button
                                                                type="button"
                                                                className="reserv-add-card-button"
                                                                onClick={() => setShowCardModal(true)}
                                                            >
                                                                <CreditCard size={16} />
                                                                Adaugă un card
                                                            </button>
                                                        </div>
                                                    ) : (
                                                        <>
                                                            <select
                                                                name="defaultCardPaymentMethodId"
                                                                value={currentProfile.defaultCardPaymentMethodId || ''}
                                                                onChange={handleChange}
                                                                required
                                                            >
                                                                <option value="">Selectează cardul</option>
                                                                {userCards.map(card => (
                                                                    <option key={card.id} value={card.id}>
                                                                        {card.displayName}
                                                                    </option>
                                                                ))}
                                                            </select>
                                                            <button
                                                                type="button"
                                                                className="reserv-manage-cards-button"
                                                                onClick={() => setShowCardModal(true)}
                                                            >
                                                                <Settings size={16} />
                                                                Gestionează carduri
                                                            </button>
                                                        </>
                                                    )}
                                                </div>
                                                {selectedCard && (
                                                    <div className="reserv-selected-card-info">
                                                        <CreditCard size={16} />
                                                        <span>{selectedCard.displayName}</span>
                                                        {selectedCard.isExpired && (
                                                            <span className="reserv-card-expired">
                                                                <AlertCircle size={14} />
                                                                Expirat
                                                            </span>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        )}

                                        <div className="reserv-form-group">
                                            <label htmlFor="autoPaymentThreshold">Prag minim pentru plată automată (RON)</label>
                                            <input
                                                type="number"
                                                id="autoPaymentThreshold"
                                                name="autoPaymentThreshold"
                                                value={currentProfile.autoPaymentThreshold}
                                                onChange={handleChange}
                                                placeholder="Ex: 10 (opțional)"
                                                min="0"
                                                step="0.01"
                                            />
                                            <small>Suma minimă pentru care se activează plata automată</small>
                                        </div>

                                        <div className="reserv-form-group">
                                            <label htmlFor="maxWeeklyAutoPayment">Limita maximă săptămânală (RON)</label>
                                            <input
                                                type="number"
                                                id="maxWeeklyAutoPayment"
                                                name="maxWeeklyAutoPayment"
                                                value={currentProfile.maxWeeklyAutoPayment}
                                                onChange={handleChange}
                                                placeholder="Ex: 200 (opțional)"
                                                min="0"
                                                step="0.01"
                                            />
                                            <small>Suma maximă care poate fi debitată automat într-o săptămână</small>
                                        </div>
                                    </>
                                )}

                                <div className="reserv-form-buttons">
                                    <button
                                        type="button"
                                        className="reserv-form-button-prev"
                                        onClick={handlePrevStep}
                                    >
                                        Înapoi
                                    </button>
                                    <button
                                        type="button"
                                        className="reserv-form-button-save"
                                        onClick={handleAddProfile}
                                        disabled={!isFormValid() || loading}
                                    >
                                        <Save size={16} />
                                        {editingProfileId ? 'Actualizează profil' : 'Salvează profil'}
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </main>
            </div>

            {/* Modal pentru gestionarea cardurilor */}
            {showCardModal && (
                <CardManagementModal
                    userCards={userCards}
                    onClose={() => setShowCardModal(false)}
                    onCardsUpdated={fetchUserCards}
                />
            )}

            {/* Mesaje de eroare și încărcare globale */}
            {error && (
                <div className="reserv-profile-error">
                    <AlertCircle size={16} />
                    <p>{error}</p>
                </div>
            )}

            {loading && (
                <div className="reserv-profile-loading">
                    <div className="reserv-loading-spinner"></div>
                    <p>Se procesează datele...</p>
                </div>
            )}

            {/* Footer cu acțiuni finale */}
            <footer className="reserv-profile-page-actions">
                <button
                    type="button"
                    className="reserv-profile-page-button-cancel"
                    onClick={handleCancel}
                >
                    Anulează
                </button>
                <button
                    type="button"
                    className="reserv-profile-page-button-finish"
                    onClick={handleFinish}
                    disabled={profiles.length === 0}
                >
                    Finalizează
                </button>
            </footer>
        </div>
    );
};

export default ProfileCreationPage;