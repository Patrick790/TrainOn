import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, User, Clock, DollarSign, MapPin, Plus, Edit, Trash, Save } from 'lucide-react';
import axios from 'axios';
import './ProfileCreationPage.css';

const ProfileCreationPage = () => {
    const navigate = useNavigate();
    const [profiles, setProfiles] = useState([]);
    const [currentProfile, setCurrentProfile] = useState({
        name: '',
        ageCategory: '0-14',
        timeInterval: '7-14:30',
        weeklyBudget: '',
        city: '',
        selectedHalls: []
    });
    const [availableHalls, setAvailableHalls] = useState([]);
    const [activeStep, setActiveStep] = useState(1);
    const [isAddingProfile, setIsAddingProfile] = useState(true);
    const [editingProfileId, setEditingProfileId] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // URL-ul API-ului
    const API_URL = 'http://localhost:8080';

    // Age categories
    const ageCategories = [
        { value: '0-14', label: '0-14 ani' },
        { value: '15-16', label: '15-16 ani' },
        { value: '17-18', label: '17-18 ani' },
        { value: 'seniori', label: 'Seniori' }
    ];

    // Time intervals
    const timeIntervals = [
        { value: '7-14:30', label: '7:00 - 14:30' },
        { value: '14:30-23', label: '14:30 - 23:00' },
        { value: 'full-day', label: 'Toată ziua' }
    ];

    // Available cities - hardcodat pentru moment, dar ar putea fi obținut dintr-un endpoint specific
    const availableCities = ['Cluj-Napoca', 'București', 'Timișoara', 'Iași', 'Brașov'];

    // Crearea unei instanțe axios cu headerul de autorizare inclus
    const api = axios.create();
    api.interceptors.request.use(function (config) {
        const token = localStorage.getItem('jwtToken');
        config.headers.Authorization = `Bearer ${token}`;
        return config;
    }, function (error) {
        return Promise.reject(error);
    });

    // Încarcă profilurile de la server la încărcarea paginii
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

    useEffect(() => {
        fetchProfiles();
    }, []);

    // Fetch sports halls based on selected city
    useEffect(() => {
        if (currentProfile.city) {
            setLoading(true);
            setError(null);

            // Obținem toate sălile și filtrăm după oraș
            api.get(`${API_URL}/sportsHalls`)
                .then(response => {
                    // Filtrăm sălile în funcție de orașul selectat
                    const filteredHalls = response.data.filter(hall =>
                        hall.city && hall.city.toLowerCase() === currentProfile.city.toLowerCase()
                    );
                    setAvailableHalls(filteredHalls);
                    setLoading(false);
                })
                .catch(err => {
                    console.error(`Eroare la obținerea sălilor din ${currentProfile.city}:`, err);
                    setError(`Nu s-au putut încărca sălile din ${currentProfile.city}. Verificați conexiunea la internet.`);
                    setLoading(false);
                    setAvailableHalls([]);
                });
        } else {
            setAvailableHalls([]);
        }
    }, [currentProfile.city]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setCurrentProfile({
            ...currentProfile,
            [name]: value
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

    const handleAddProfile = async () => {
        if (currentProfile.name && currentProfile.weeklyBudget && currentProfile.city) {
            setLoading(true);
            setError(null);

            try {
                // Pregătim datele pentru trimitere la server
                const profileData = {
                    name: currentProfile.name,
                    ageCategory: currentProfile.ageCategory,
                    timeInterval: currentProfile.timeInterval,
                    weeklyBudget: currentProfile.weeklyBudget,
                    city: currentProfile.city,
                    selectedHalls: currentProfile.selectedHalls
                };

                if (editingProfileId) {
                    // Actualizare profil existent
                    await api.put(`${API_URL}/reservationProfiles/${editingProfileId}`, profileData);
                } else {
                    // Adăugare profil nou
                    await api.post(`${API_URL}/reservationProfiles`, profileData);
                }

                // Reîncărcăm profilurile pentru a afișa datele actualizate
                await fetchProfiles();

                // Resetare formular
                setCurrentProfile({
                    name: '',
                    ageCategory: '0-14',
                    timeInterval: '7-14:30',
                    weeklyBudget: '',
                    city: '',
                    selectedHalls: []
                });
                setActiveStep(1);
                setIsAddingProfile(true);
                setEditingProfileId(null);
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
                // Ștergem profilul de la server
                await api.delete(`${API_URL}/reservationProfiles/${profileId}`);

                // Reîncărcăm profilurile
                await fetchProfiles();

                // Dacă profilul care se șterge este cel care se editează, resetăm formularul
                if (editingProfileId === profileId) {
                    setCurrentProfile({
                        name: '',
                        ageCategory: '0-14',
                        timeInterval: '7-14:30',
                        weeklyBudget: '',
                        city: '',
                        selectedHalls: []
                    });
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
        // Pregătim profilul pentru editare
        // Transformăm obiectele sală în id-uri pentru editare
        const hallIds = profile.selectedHalls ? profile.selectedHalls.map(hall => hall.id) : [];

        setCurrentProfile({
            ...profile,
            selectedHalls: hallIds
        });
        setEditingProfileId(profile.id);
        setIsAddingProfile(false);
        setActiveStep(1);
    };

    const handleFinish = () => {
        // Navigăm înapoi la pagina de detalii sau unde este necesar
        navigate(-1);
    };

    const handleCancel = () => {
        // Verificăm dacă avem modificări nesalvate
        if (currentProfile.name || currentProfile.city || currentProfile.weeklyBudget || currentProfile.selectedHalls.length > 0) {
            const confirmCancel = window.confirm('Ai modificări nesalvate. Ești sigur că vrei să ieși fără a salva?');
            if (confirmCancel) {
                navigate(-1);
            }
        } else {
            navigate(-1);
        }
    };

    // Formatare preț
    const formatPrice = (price) => {
        if (!price) return 'Preț la cerere';
        return `${price} RON/1h30`;
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
                <div className="reserv-profile-page-sidebar">
                    <div className="reserv-profile-sidebar-header">
                        <h2>Profiluri create</h2>
                        <button
                            className="reserv-add-profile-button"
                            onClick={() => {
                                setCurrentProfile({
                                    name: '',
                                    ageCategory: '0-14',
                                    timeInterval: '7-14:30',
                                    weeklyBudget: '',
                                    city: '',
                                    selectedHalls: []
                                });
                                setEditingProfileId(null);
                                setIsAddingProfile(true);
                                setActiveStep(1);
                            }}
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
                                                <DollarSign size={14} />
                                                {profile.weeklyBudget} RON/săptămână
                                            </span>
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
                </div>

                <div className="reserv-profile-form-container">
                    <h2 className="reserv-profile-form-title">
                        {editingProfileId ? 'Editare profil' : 'Creare profil nou'}
                    </h2>

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
                    </div>

                    <div className="reserv-profile-form">
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
                                        disabled={!currentProfile.name || !currentProfile.weeklyBudget}
                                    >
                                        Continuare
                                    </button>
                                </div>
                            </div>
                        )}

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
                                                                <span className="reserv-hall-name">{hall.name}</span>
                                                                <span className="reserv-hall-price">{formatPrice(hall.tariff)}</span>
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
                                        className="reserv-form-button-save"
                                        onClick={handleAddProfile}
                                        disabled={!currentProfile.city || currentProfile.selectedHalls.length === 0 || loading}
                                    >
                                        <Save size={16} />
                                        {editingProfileId ? 'Actualizează profil' : 'Salvează profil'}
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Afișează un mesaj de eroare general dacă există */}
            {error && (
                <div className="reserv-profile-error">
                    <p>{error}</p>
                </div>
            )}

            {/* Afișează un indicator de încărcare global */}
            {loading && (
                <div className="reserv-profile-loading">
                    <p>Se procesează datele...</p>
                </div>
            )}

            <div className="reserv-profile-page-actions">
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
            </div>
        </div>
    );
};

export default ProfileCreationPage;