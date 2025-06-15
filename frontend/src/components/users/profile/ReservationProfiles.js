import React, { useState, useEffect } from 'react';

const ReservationProfiles = () => {
    const [reservationProfiles, setReservationProfiles] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState('');
    const [editingProfile, setEditingProfile] = useState(null);
    const [editData, setEditData] = useState({});

    const romanianCities = [
        'Alba Iulia', 'Arad', 'Argeș', 'Bacău', 'Bihor', 'Bistrița-Năsăud',
        'Botoșani', 'Brăila', 'Brașov', 'București', 'Buzău', 'Călărași',
        'Caraș-Severin', 'Cluj-Napoca', 'Constanța', 'Covasna', 'Dâmbovița',
        'Dolj', 'Galați', 'Giurgiu', 'Gorj', 'Harghita', 'Hunedoara', 'Ialomița',
        'Iași', 'Ilfov', 'Maramureș', 'Mehedinți', 'Mureș', 'Neamț', 'Olt',
        'Prahova', 'Sălaj', 'Satu Mare', 'Sibiu', 'Suceava', 'Teleorman',
        'Timișoara', 'Tulcea', 'Vâlcea', 'Vaslui', 'Vrancea'
    ];

    const API_BASE_URL = process.env.REACT_APP_API_URL || '';


    const ageCategories = ['0-14', '15-16', '17-18', 'seniori'];
    const timeIntervals = ['7-14:30', '14:30-23', 'full-day'];

    const getAgeCategoryLabel = (value) => {
        const labels = { '0-14': '0-14 ani', '15-16': '15-16 ani', '17-18': '17-18 ani', 'seniori': 'Seniori' };
        return labels[value] || value;
    };

    const getTimeIntervalLabel = (value) => {
        const labels = { '7-14:30': '7:00 - 14:30', '14:30-23': '14:30 - 23:00', 'full-day': 'Toată ziua' };
        return labels[value] || value;
    };

    const fetchReservationProfiles = async () => {
        try {
            setLoading(true);
            setError(null);
            const token = localStorage.getItem('jwtToken');
            if (!token) throw new Error('Nu sunt disponibile informațiile de autentificare');

            const response = await fetch(`${API_BASE_URL}/reservationProfiles`, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
            });

            if (!response.ok) {
                if (response.status === 401) throw new Error('Sesiunea a expirat. Te rugăm să te autentifici din nou.');
                if (response.status === 403) throw new Error('Nu ai permisiunea să accesezi aceste date.');
                throw new Error('Nu s-au putut încărca profilurile de rezervare');
            }

            const profiles = await response.json();
            setReservationProfiles(profiles);
            setLoading(false);
        } catch (err) {
            console.error('Eroare la încărcarea profilurilor de rezervare:', err);
            setError(err.message);
            setLoading(false);
        }
    };

    const deleteReservationProfile = async (profileId) => {
        try {
            const token = localStorage.getItem('jwtToken');
            if (!token) throw new Error('Nu sunt disponibile informațiile de autentificare');

            const response = await fetch(`${API_BASE_URL}/reservationProfiles/${profileId}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
            });

            if (!response.ok) throw new Error('Nu s-a putut șterge profilul');

            await fetchReservationProfiles();
            setSuccessMessage('Profilul a fost șters cu succes!');
            setTimeout(() => setSuccessMessage(''), 3000);
        } catch (err) {
            console.error('Eroare la ștergerea profilului:', err);
            setError('Nu s-a putut șterge profilul: ' + err.message);
        }
    };

    const updateReservationProfile = async (profileId, updatedData) => {
        try {
            const token = localStorage.getItem('jwtToken');
            if (!token) throw new Error('Nu sunt disponibile informațiile de autentificare');

            const response = await fetch(`${API_BASE_URL}/reservationProfiles/${profileId}`, {
                method: 'PUT',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify(updatedData)
            });

            if (!response.ok) throw new Error('Nu s-a putut actualiza profilul');

            await fetchReservationProfiles();
            setEditingProfile(null);
            setEditData({});
            setSuccessMessage('Profilul a fost actualizat cu succes!');
            setTimeout(() => setSuccessMessage(''), 3000);
        } catch (err) {
            console.error('Eroare la actualizarea profilului:', err);
            setError('Nu s-a putut actualiza profilul: ' + err.message);
        }
    };

    const startEditing = (profile) => {
        setEditingProfile(profile.id);
        setEditData({
            name: profile.name,
            ageCategory: profile.ageCategory,
            timeInterval: profile.timeInterval,
            weeklyBudget: profile.weeklyBudget,
            city: profile.city
        });
        setError(null);
        setSuccessMessage('');
    };

    const cancelEditing = () => {
        setEditingProfile(null);
        setEditData({});
        setError(null);
        setSuccessMessage('');
    };

    const saveChanges = () => {
        if (!editData.name || !editData.ageCategory || !editData.timeInterval || !editData.weeklyBudget || !editData.city) {
            setError('Toate câmpurile sunt obligatorii');
            return;
        }
        if (editData.weeklyBudget <= 0) {
            setError('Bugetul săptămânal trebuie să fie mai mare decât 0');
            return;
        }
        updateReservationProfile(editingProfile, editData);
    };

    const handleInputChange = (field, value) => {
        setEditData(prev => ({ ...prev, [field]: value }));
    };

    useEffect(() => {
        fetchReservationProfiles();
    }, []);

    if (loading) {
        return (
            <div className="profile-section">
                <div className="section-header">
                    <h2>Profiluri de rezervare</h2>
                </div>
                <p style={{color: 'white', textAlign: 'center', textShadow: '0 2px 4px rgba(0, 0, 0, 0.8)'}}>
                    Se încarcă profilurile...
                </p>
            </div>
        );
    }

    if (error && !editingProfile) {
        return (
            <div className="profile-section">
                <div className="section-header">
                    <h2>Profiluri de rezervare</h2>
                </div>
                <p style={{color: '#dc2626', textAlign: 'center', textShadow: '0 2px 4px rgba(0, 0, 0, 0.8)'}}>
                    Eroare: {error}
                </p>
                <button onClick={fetchReservationProfiles} className="edit-button" style={{margin: '0 auto', display: 'block'}}>
                    Încearcă din nou
                </button>
            </div>
        );
    }

    return (
        <div className="profile-section">
            <div className="section-header">
                <h2>Profiluri de rezervare</h2>
            </div>

            {successMessage && (
                <div style={{
                    color: '#10b981', textAlign: 'center', marginBottom: '1rem', padding: '0.5rem',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)',
                    borderRadius: '6px', textShadow: '0 1px 2px rgba(0, 0, 0, 0.5)'
                }}>
                    {successMessage}
                </div>
            )}

            {error && editingProfile && (
                <div style={{
                    color: '#dc2626', textAlign: 'center', marginBottom: '1rem', padding: '0.5rem',
                    backgroundColor: 'rgba(220, 38, 38, 0.1)', border: '1px solid rgba(220, 38, 38, 0.3)',
                    borderRadius: '6px', textShadow: '0 1px 2px rgba(0, 0, 0, 0.5)'
                }}>
                    {error}
                </div>
            )}

            {reservationProfiles.length === 0 ? (
                <div style={{
                    textAlign: 'center', padding: '2rem', color: 'rgba(255, 255, 255, 0.8)',
                    textShadow: '0 1px 2px rgba(0, 0, 0, 0.5)'
                }}>
                    <p>Nu ai încă profiluri de rezervare create.</p>
                    <p>Creează primul tău profil pentru a începe să faci rezervări mai ușor!</p>
                </div>
            ) : (
                <div className="reservation-profiles">
                    {reservationProfiles.map((profile, index) => (
                        <div key={profile.id} className="profile-card">
                            <div className="profile-card-header">
                                {editingProfile === profile.id ? (
                                    <input
                                        type="text"
                                        value={editData.name}
                                        onChange={(e) => handleInputChange('name', e.target.value)}
                                        className="edit-input"
                                        style={{ fontSize: '1.1rem', fontWeight: '600', background: 'rgba(0, 0, 0, 0.3)' }}
                                    />
                                ) : (
                                    <h3>{profile.name}</h3>
                                )}
                                {index === 0 && <span className="profile-badge default">Implicit</span>}
                            </div>
                            <div className="profile-card-content">
                                <p>
                                    <strong>Categoria de vârstă:</strong>{' '}
                                    {editingProfile === profile.id ? (
                                        <select
                                            value={editData.ageCategory}
                                            onChange={(e) => handleInputChange('ageCategory', e.target.value)}
                                            className="edit-input"
                                            style={{ display: 'inline-block', width: 'auto', minWidth: '120px', marginLeft: '0.5rem' }}
                                        >
                                            {ageCategories.map(category => (
                                                <option key={category} value={category}>
                                                    {getAgeCategoryLabel(category)}
                                                </option>
                                            ))}
                                        </select>
                                    ) : (
                                        getAgeCategoryLabel(profile.ageCategory)
                                    )}
                                </p>
                                <p>
                                    <strong>Interval orar:</strong>{' '}
                                    {editingProfile === profile.id ? (
                                        <select
                                            value={editData.timeInterval}
                                            onChange={(e) => handleInputChange('timeInterval', e.target.value)}
                                            className="edit-input"
                                            style={{ display: 'inline-block', width: 'auto', minWidth: '140px', marginLeft: '0.5rem' }}
                                        >
                                            {timeIntervals.map(interval => (
                                                <option key={interval} value={interval}>
                                                    {getTimeIntervalLabel(interval)}
                                                </option>
                                            ))}
                                        </select>
                                    ) : (
                                        getTimeIntervalLabel(profile.timeInterval)
                                    )}
                                </p>
                                <p>
                                    <strong>Buget săptămânal:</strong>{' '}
                                    {editingProfile === profile.id ? (
                                        <>
                                            <input
                                                type="number"
                                                value={editData.weeklyBudget}
                                                onChange={(e) => handleInputChange('weeklyBudget', e.target.value)}
                                                className="edit-input"
                                                style={{ display: 'inline-block', width: '80px', marginLeft: '0.5rem', marginRight: '0.25rem' }}
                                            />
                                            RON
                                        </>
                                    ) : (
                                        `${profile.weeklyBudget} RON`
                                    )}
                                </p>
                                <p>
                                    <strong>Oraș:</strong>{' '}
                                    {editingProfile === profile.id ? (
                                        <select
                                            value={editData.city}
                                            onChange={(e) => handleInputChange('city', e.target.value)}
                                            className="edit-input"
                                            style={{ display: 'inline-block', width: 'auto', minWidth: '120px', marginLeft: '0.5rem' }}
                                        >
                                            {romanianCities.map(city => (
                                                <option key={city} value={city}>
                                                    {city}
                                                </option>
                                            ))}
                                        </select>
                                    ) : (
                                        profile.city
                                    )}
                                </p>
                                {profile.selectedHalls && profile.selectedHalls.length > 0 && (
                                    <p><strong>Săli preferate:</strong> {profile.selectedHalls.length} selectate</p>
                                )}
                            </div>
                            <div className="profile-card-actions">
                                {editingProfile === profile.id ? (
                                    <>
                                        <button className="edit-profile-btn" onClick={saveChanges}>Salvează</button>
                                        <button className="delete-profile-btn" onClick={cancelEditing}>Anulează</button>
                                    </>
                                ) : (
                                    <>
                                        <button className="edit-profile-btn" onClick={() => startEditing(profile)}>Editează</button>
                                        <button
                                            className="delete-profile-btn"
                                            onClick={() => {
                                                if (window.confirm('Ești sigur că vrei să ștergi acest profil?')) {
                                                    deleteReservationProfile(profile.id);
                                                }
                                            }}
                                        >
                                            Șterge
                                        </button>
                                    </>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ReservationProfiles;