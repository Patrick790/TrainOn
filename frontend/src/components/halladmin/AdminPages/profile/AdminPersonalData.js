import React, { useState, useEffect } from 'react';
import { Edit3, Save, X } from 'lucide-react';

const AppAdminPersonalData = () => {
    const [isEditing, setIsEditing] = useState(false);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState('');

    const [personalData, setPersonalData] = useState({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        address: '',
        county: '',
        city: '',
        birthDate: ''
    });
    const API_BASE_URL = process.env.REACT_APP_API_URL || '';


    const [tempPersonalData, setTempPersonalData] = useState({...personalData});

    // Orașele din România pentru dropdown
    const romanianCities = [
        'Alba Iulia', 'Arad', 'Argeș', 'Bacău', 'Bihor', 'Bistrița-Năsăud',
        'Botoșani', 'Brăila', 'Brașov', 'București', 'Buzău', 'Călărași',
        'Caraș-Severin', 'Cluj-Napoca', 'Constanța', 'Covasna', 'Dâmbovița',
        'Dolj', 'Galați', 'Giurgiu', 'Gorj', 'Harghita', 'Hunedoara', 'Ialomița',
        'Iași', 'Ilfov', 'Maramureș', 'Mehedinți', 'Mureș', 'Neamț', 'Olt',
        'Prahova', 'Sălaj', 'Satu Mare', 'Sibiu', 'Suceava', 'Teleorman',
        'Timișoara', 'Tulcea', 'Vâlcea', 'Vaslui', 'Vrancea'
    ];

    // Funcție pentru a obține datele administratorului din baza de date
    const fetchAdminData = async () => {
        try {
            setLoading(true);
            setError(null);

            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                throw new Error('Nu sunt disponibile informațiile de autentificare');
            }

            // SCHIMBAT: folosim /users în loc de /app-admins
            const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 401) {
                    throw new Error('Sesiunea a expirat. Te rugăm să te autentifici din nou.');
                } else if (response.status === 403) {
                    throw new Error('Nu ai permisiunea să accesezi aceste date.');
                } else {
                    throw new Error('Nu s-au putut încărca datele administratorului');
                }
            }

            const userData = await response.json();

            // Separăm numele complet în prenume și nume
            const nameParts = userData.name ? userData.name.split(' ') : ['', ''];
            const firstName = nameParts[0] || '';
            const lastName = nameParts.slice(1).join(' ') || '';

            // Formatăm data nașterii pentru input-ul de tip date
            const formattedBirthDate = userData.birthDate
                ? new Date(userData.birthDate).toISOString().split('T')[0]
                : '';

            const formattedUserData = {
                firstName: firstName,
                lastName: lastName,
                email: userData.email || '',
                phone: userData.phone || '',
                address: userData.address || '',
                county: userData.county || '',
                city: userData.city || '',
                birthDate: formattedBirthDate
            };

            setPersonalData(formattedUserData);
            setTempPersonalData(formattedUserData);
            setLoading(false);
        } catch (err) {
            console.error('Eroare la încărcarea datelor administratorului:', err);
            setError(err.message);
            setLoading(false);
        }
    };

    // Funcție pentru a salva datele actualizate în baza de date
    const saveAdminData = async (updatedData) => {
        try {
            setSaving(true);
            setError(null);
            setSuccessMessage('');

            const userId = localStorage.getItem('userId');
            const token = localStorage.getItem('jwtToken');

            if (!userId || !token) {
                throw new Error('Nu sunt disponibile informațiile de autentificare');
            }

            // Combinăm prenumele și numele înapoi
            const fullName = `${updatedData.firstName} ${updatedData.lastName}`.trim();

            // Formatăm data pentru trimitere la backend
            const birthDate = updatedData.birthDate ? new Date(updatedData.birthDate) : null;

            const dataToSend = {
                name: fullName,
                email: updatedData.email,
                address: updatedData.address,
                county: updatedData.county,
                city: updatedData.city,
                birthDate: birthDate
            };

            // SCHIMBAT: folosim /users în loc de /app-admins
            const response = await fetch(`${API_BASE_URL}/users/${userId}`, {                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(dataToSend)
            });

            if (!response.ok) {
                if (response.status === 401) {
                    throw new Error('Sesiunea a expirat. Te rugăm să te autentifici din nou.');
                } else if (response.status === 403) {
                    throw new Error('Nu ai permisiunea să modifici aceste date.');
                } else {
                    throw new Error('Nu s-au putut salva modificările');
                }
            }

            const savedData = await response.json();
            setSuccessMessage('Datele au fost salvate cu succes!');
            setSaving(false);

            // Ascundem mesajul de succes după 3 secunde
            setTimeout(() => {
                setSuccessMessage('');
            }, 3000);

            return savedData;
        } catch (err) {
            console.error('Eroare la salvarea datelor:', err);
            setSaving(false);
            throw err;
        }
    };

    useEffect(() => {
        fetchAdminData();
    }, []);

    const handleEdit = () => {
        setTempPersonalData({...personalData});
        setIsEditing(true);
        setError(null);
        setSuccessMessage('');
    };

    const handleSave = async () => {
        try {
            await saveAdminData(tempPersonalData);
            setPersonalData({...tempPersonalData});
            setIsEditing(false);
        } catch (err) {
            setError('Nu s-au putut salva modificările: ' + err.message);
        }
    };

    const handleCancel = () => {
        setTempPersonalData({...personalData});
        setIsEditing(false);
        setError(null);
        setSuccessMessage('');
    };

    const handleInputChange = (field, value) => {
        setTempPersonalData(prev => ({
            ...prev,
            [field]: value
        }));
    };

    if (loading) {
        return (
            <div className="app-admin-profile-section">
                <div className="app-admin-section-header">
                    <h2>Date personale</h2>
                </div>
                <p style={{color: 'white', textAlign: 'center', textShadow: '0 2px 4px rgba(0, 0, 0, 0.8)'}}>
                    Se încarcă datele...
                </p>
            </div>
        );
    }

    if (error && !isEditing) {
        return (
            <div className="app-admin-profile-section">
                <div className="app-admin-section-header">
                    <h2>Date personale</h2>
                </div>
                <p style={{color: '#dc2626', textAlign: 'center', textShadow: '0 2px 4px rgba(0, 0, 0, 0.8)'}}>
                    Eroare: {error}
                </p>
                <button
                    onClick={fetchAdminData}
                    className="app-admin-edit-button"
                    style={{margin: '0 auto', display: 'block'}}
                >
                    Încearcă din nou
                </button>
            </div>
        );
    }

    return (
        <div className="app-admin-profile-section">
            <div className="app-admin-section-header">
                <h2>Date personale</h2>
                {!isEditing ? (
                    <button onClick={handleEdit} className="app-admin-edit-button">
                        <Edit3 size={16} />
                        Editează
                    </button>
                ) : (
                    <div className="app-admin-edit-actions">
                        <button
                            onClick={handleSave}
                            className="app-admin-save-button"
                            disabled={saving}
                        >
                            <Save size={16} />
                            {saving ? 'Se salvează...' : 'Salvează'}
                        </button>
                        <button
                            onClick={handleCancel}
                            className="app-admin-cancel-button"
                            disabled={saving}
                        >
                            <X size={16} />
                            Anulează
                        </button>
                    </div>
                )}
            </div>

            {/* Mesaje de feedback */}
            {successMessage && (
                <div style={{
                    color: '#10b981',
                    textAlign: 'center',
                    marginBottom: '1rem',
                    padding: '0.5rem',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    border: '1px solid rgba(16, 185, 129, 0.3)',
                    borderRadius: '6px',
                    textShadow: '0 1px 2px rgba(0, 0, 0, 0.5)'
                }}>
                    {successMessage}
                </div>
            )}

            {error && isEditing && (
                <div style={{
                    color: '#dc2626',
                    textAlign: 'center',
                    marginBottom: '1rem',
                    padding: '0.5rem',
                    backgroundColor: 'rgba(220, 38, 38, 0.1)',
                    border: '1px solid rgba(220, 38, 38, 0.3)',
                    borderRadius: '6px',
                    textShadow: '0 1px 2px rgba(0, 0, 0, 0.5)'
                }}>
                    {error}
                </div>
            )}

            <div className="app-admin-personal-data-grid">
                <div className="app-admin-data-field">
                    <label>Prenume</label>
                    {isEditing ? (
                        <input
                            type="text"
                            value={tempPersonalData.firstName}
                            onChange={(e) => handleInputChange('firstName', e.target.value)}
                            className="app-admin-edit-input"
                            placeholder="Introdu prenumele"
                        />
                    ) : (
                        <span className="app-admin-data-value">{personalData.firstName || 'Nu este specificat'}</span>
                    )}
                </div>

                <div className="app-admin-data-field">
                    <label>Nume</label>
                    {isEditing ? (
                        <input
                            type="text"
                            value={tempPersonalData.lastName}
                            onChange={(e) => handleInputChange('lastName', e.target.value)}
                            className="app-admin-edit-input"
                            placeholder="Introdu numele"
                        />
                    ) : (
                        <span className="app-admin-data-value">{personalData.lastName || 'Nu este specificat'}</span>
                    )}
                </div>

                <div className="app-admin-data-field">
                    <label>Email</label>
                    {isEditing ? (
                        <input
                            type="email"
                            value={tempPersonalData.email}
                            onChange={(e) => handleInputChange('email', e.target.value)}
                            className="app-admin-edit-input"
                            placeholder="Introdu adresa de email"
                        />
                    ) : (
                        <span className="app-admin-data-value">{personalData.email || 'Nu este specificat'}</span>
                    )}
                </div>

                <div className="app-admin-data-field">
                    <label>Telefon</label>
                    {isEditing ? (
                        <input
                            type="tel"
                            value={tempPersonalData.phone}
                            onChange={(e) => handleInputChange('phone', e.target.value)}
                            className="app-admin-edit-input"
                            placeholder="Adaugă numărul de telefon"
                        />
                    ) : (
                        <span className="app-admin-data-value">{personalData.phone || 'Nu este specificat'}</span>
                    )}
                </div>

                <div className="app-admin-data-field">
                    <label>Adresă</label>
                    {isEditing ? (
                        <input
                            type="text"
                            value={tempPersonalData.address}
                            onChange={(e) => handleInputChange('address', e.target.value)}
                            className="app-admin-edit-input"
                            placeholder="Introdu adresa"
                        />
                    ) : (
                        <span className="app-admin-data-value">{personalData.address || 'Nu este specificată'}</span>
                    )}
                </div>

                <div className="app-admin-data-field">
                    <label>Județ</label>
                    {isEditing ? (
                        <input
                            type="text"
                            value={tempPersonalData.county}
                            onChange={(e) => handleInputChange('county', e.target.value)}
                            className="app-admin-edit-input"
                            placeholder="Introdu județul"
                        />
                    ) : (
                        <span className="app-admin-data-value">{personalData.county || 'Nu este specificat'}</span>
                    )}
                </div>

                <div className="app-admin-data-field">
                    <label>Oraș</label>
                    {isEditing ? (
                        <select
                            value={tempPersonalData.city}
                            onChange={(e) => handleInputChange('city', e.target.value)}
                            className="app-admin-edit-input"
                        >
                            <option value="">Selectează orașul</option>
                            {romanianCities.map(city => (
                                <option key={city} value={city}>{city}</option>
                            ))}
                        </select>
                    ) : (
                        <span className="app-admin-data-value">{personalData.city || 'Nu este specificat'}</span>
                    )}
                </div>

                <div className="app-admin-data-field">
                    <label>Data nașterii</label>
                    {isEditing ? (
                        <input
                            type="date"
                            value={tempPersonalData.birthDate}
                            onChange={(e) => handleInputChange('birthDate', e.target.value)}
                            className="app-admin-edit-input"
                        />
                    ) : (
                        <span className="app-admin-data-value">
                            {personalData.birthDate
                                ? new Date(personalData.birthDate).toLocaleDateString('ro-RO')
                                : 'Nu este specificată'
                            }
                        </span>
                    )}
                </div>
            </div>
        </div>
    );
};

export default AppAdminPersonalData;