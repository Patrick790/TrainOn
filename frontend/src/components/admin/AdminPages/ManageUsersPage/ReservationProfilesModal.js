import React, { useState, useEffect } from 'react';
import './ReservationProfilesModal.css';

const ReservationProfilesModal = ({ isOpen, onClose, userId, userName, API_URL, getAuthHeader }) => {
    const [profiles, setProfiles] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [editingProfile, setEditingProfile] = useState(null);
    const [isEditing, setIsEditing] = useState(false);
    const [editFormData, setEditFormData] = useState({
        name: '',
        ageCategory: '',
        timeInterval: '',
        weeklyBudget: '',
        city: ''
    });

    // Încarcă profilurile de rezervare ale utilizatorului
    const fetchReservationProfiles = async () => {
        if (!userId) return;

        setLoading(true);
        setError(null);

        try {
            const response = await fetch(
                `${API_URL}/reservationProfiles?userId=${userId}`,
                {
                    method: 'GET',
                    ...getAuthHeader()
                }
            );

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea profilurilor: ${response.status}`);
            }

            const data = await response.json();
            setProfiles(data);
        } catch (err) {
            console.error('Error fetching reservation profiles:', err);
            setError('Nu s-au putut încărca profilurile de rezervare');
        } finally {
            setLoading(false);
        }
    };

    // Încarcă profilurile când se deschide modalul
    useEffect(() => {
        if (isOpen && userId) {
            fetchReservationProfiles();
        }
    }, [isOpen, userId]);

    // Resetează starea când se închide modalul
    useEffect(() => {
        if (!isOpen) {
            setProfiles([]);
            setEditingProfile(null);
            setIsEditing(false);
            setError(null);
        }
    }, [isOpen]);

    const handleEditProfile = (profile) => {
        setEditingProfile(profile);
        setIsEditing(true);
        setEditFormData({
            name: profile.name || '',
            ageCategory: profile.ageCategory || '',
            timeInterval: profile.timeInterval || '',
            weeklyBudget: profile.weeklyBudget || '',
            city: profile.city || ''
        });
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
        setEditingProfile(null);
        setEditFormData({
            name: '',
            ageCategory: '',
            timeInterval: '',
            weeklyBudget: '',
            city: ''
        });
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setEditFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSaveProfile = async () => {
        if (!editingProfile) return;

        try {
            const response = await fetch(
                `${API_URL}/reservationProfiles/${editingProfile.id}`,
                {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        ...getAuthHeader().headers
                    },
                    body: JSON.stringify(editFormData)
                }
            );

            if (!response.ok) {
                throw new Error(`Eroare la salvarea profilului: ${response.status}`);
            }

            // Reîncarcă profilurile
            await fetchReservationProfiles();
            handleCancelEdit();

        } catch (err) {
            console.error('Error saving profile:', err);
            setError('Nu s-a putut salva profilul');
        }
    };

    const handleDeleteProfile = async (profileId, profileName) => {
        if (!window.confirm(`Sunteți sigur că doriți să ștergeți profilul "${profileName}"?`)) {
            return;
        }

        try {
            const response = await fetch(
                `${API_URL}/reservationProfiles/${profileId}`,
                {
                    method: 'DELETE',
                    ...getAuthHeader()
                }
            );

            if (!response.ok) {
                throw new Error(`Eroare la ștergerea profilului: ${response.status}`);
            }

            // Reîncarcă profilurile
            await fetchReservationProfiles();

        } catch (err) {
            console.error('Error deleting profile:', err);
            setError('Nu s-a putut șterge profilul');
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        try {
            return new Date(dateString).toLocaleDateString('ro-RO');
        } catch {
            return 'N/A';
        }
    };

    if (!isOpen) return null;

    return (
        <div className="rp-modal-overlay" onClick={onClose}>
            <div className="rp-modal" onClick={e => e.stopPropagation()}>
                <div className="rp-modal-header">
                    <h3 className="rp-modal-title">
                        Profiluri de rezervare - {userName}
                    </h3>
                    <button className="rp-modal-close" onClick={onClose}>×</button>
                </div>

                <div className="rp-modal-body">
                    {loading && (
                        <div className="rp-loading">
                            Se încarcă profilurile de rezervare...
                        </div>
                    )}

                    {error && (
                        <div className="rp-error">
                            {error}
                        </div>
                    )}

                    {!loading && !error && profiles.length === 0 && (
                        <div className="rp-empty">
                            <p>Acest utilizator nu are profiluri de rezervare create.</p>
                        </div>
                    )}

                    {!loading && !error && profiles.length > 0 && (
                        <div className="rp-profiles-container">
                            {isEditing ? (
                                <div className="rp-edit-form">
                                    <h4>Editare profil: {editingProfile.name}</h4>

                                    <div className="rp-form-grid">
                                        <div className="rp-form-group">
                                            <label htmlFor="name">Nume profil:</label>
                                            <input
                                                type="text"
                                                id="name"
                                                name="name"
                                                value={editFormData.name}
                                                onChange={handleInputChange}
                                                required
                                                placeholder="Ex: Echipa Juniori"
                                            />
                                        </div>

                                        <div className="rp-form-group">
                                            <label htmlFor="ageCategory">Categorie vârstă:</label>
                                            <select
                                                id="ageCategory"
                                                name="ageCategory"
                                                value={editFormData.ageCategory}
                                                onChange={handleInputChange}
                                                required
                                                className="rp-select"
                                            >
                                                <option value="">Selectează categoria</option>
                                                <option value="0-14">0-14 ani</option>
                                                <option value="15-16">15-16 ani</option>
                                                <option value="17-18">17-18 ani</option>
                                                <option value="seniori">Seniori</option>
                                            </select>
                                        </div>

                                        <div className="rp-form-group">
                                            <label htmlFor="timeInterval">Interval orar preferat:</label>
                                            <select
                                                id="timeInterval"
                                                name="timeInterval"
                                                value={editFormData.timeInterval}
                                                onChange={handleInputChange}
                                                required
                                                className="rp-select"
                                            >
                                                <option value="">Selectează intervalul</option>
                                                <option value="7-14:30">7:00 - 14:30</option>
                                                <option value="14:30-23:30">14:30 - 23:30</option>
                                                <option value="toata_ziua">Toată ziua</option>
                                            </select>
                                        </div>

                                        <div className="rp-form-group">
                                            <label htmlFor="weeklyBudget">Buget săptămânal (RON):</label>
                                            <input
                                                type="number"
                                                id="weeklyBudget"
                                                name="weeklyBudget"
                                                value={editFormData.weeklyBudget}
                                                onChange={handleInputChange}
                                                min="0"
                                                step="0.01"
                                                placeholder="Ex: 200.00"
                                            />
                                        </div>

                                        <div className="rp-form-group rp-full-width">
                                            <label htmlFor="city">Oraș preferat:</label>
                                            <input
                                                type="text"
                                                id="city"
                                                name="city"
                                                value={editFormData.city}
                                                onChange={handleInputChange}
                                                required
                                                placeholder="Ex: Cluj-Napoca"
                                            />
                                        </div>
                                    </div>

                                    <div className="rp-edit-actions">
                                        <button
                                            className="rp-btn rp-btn-cancel"
                                            onClick={handleCancelEdit}
                                        >
                                            Anulează
                                        </button>
                                        <button
                                            className="rp-btn rp-btn-save"
                                            onClick={handleSaveProfile}
                                        >
                                            Salvează modificările
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div className="rp-profiles-list">
                                    {profiles.map(profile => (
                                        <div key={profile.id} className="rp-profile-card">
                                            <div className="rp-profile-header">
                                                <h4 className="rp-profile-name">
                                                    {profile.name}
                                                </h4>
                                                <div className="rp-profile-actions">
                                                    <button
                                                        className="rp-btn rp-btn-edit"
                                                        onClick={() => handleEditProfile(profile)}
                                                    >
                                                        Editează
                                                    </button>
                                                    <button
                                                        className="rp-btn rp-btn-delete"
                                                        onClick={() => handleDeleteProfile(profile.id, profile.name)}
                                                    >
                                                        Șterge
                                                    </button>
                                                </div>
                                            </div>

                                            <div className="rp-profile-details">
                                                <div className="rp-profile-row">
                                                    <span className="rp-label">Categorie vârstă:</span>
                                                    <span className="rp-value">{profile.ageCategory || 'N/A'}</span>
                                                </div>

                                                <div className="rp-profile-row">
                                                    <span className="rp-label">Interval orar:</span>
                                                    <span className="rp-value">{profile.timeInterval || 'N/A'}</span>
                                                </div>

                                                <div className="rp-profile-row">
                                                    <span className="rp-label">Buget săptămânal:</span>
                                                    <span className="rp-value">
                                                        {profile.weeklyBudget ? `${profile.weeklyBudget} RON` : 'N/A'}
                                                    </span>
                                                </div>

                                                <div className="rp-profile-row">
                                                    <span className="rp-label">Oraș:</span>
                                                    <span className="rp-value">{profile.city || 'N/A'}</span>
                                                </div>

                                                <div className="rp-profile-row">
                                                    <span className="rp-label">Săli selectate:</span>
                                                    <span className="rp-value">
                                                        {profile.selectedHalls && profile.selectedHalls.length > 0
                                                            ? `${profile.selectedHalls.length} săli`
                                                            : 'Nicio sală selectată'}
                                                    </span>
                                                </div>

                                                <div className="rp-profile-row">
                                                    <span className="rp-label">Creat la:</span>
                                                    <span className="rp-value">{formatDate(profile.createdAt)}</span>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}
                </div>

                <div className="rp-modal-footer">
                    <button className="rp-btn rp-btn-close" onClick={onClose}>
                        Închide
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ReservationProfilesModal;