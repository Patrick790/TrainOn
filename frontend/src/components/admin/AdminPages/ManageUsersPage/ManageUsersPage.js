import React, { useState, useEffect } from 'react';
import './ManageUsersPage.css';
import axios from 'axios';
import CreateUserModal from './CreateUserModal';
import ReservationProfilesModal from './ReservationProfilesModal';

const ManageUsersPage = () => {
    const [users, setUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedUser, setSelectedUser] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [cityFilter, setCityFilter] = useState('');
    const [statusFilter, setStatusFilter] = useState('active');
    const [availableCities, setAvailableCities] = useState([]);
    const [isEditing, setIsEditing] = useState(false);
    const [editFormData, setEditFormData] = useState({
        name: '',
        email: '',
        address: '',
        city: '',
        county: '',
        userType: ''
    });
    // New state for create modals
    const [isCreateUserModalOpen, setIsCreateUserModalOpen] = useState(false);
    const [isCreateHallAdminModalOpen, setIsCreateHallAdminModalOpen] = useState(false);

    // Nou state pentru modalul de profiluri de rezervare
    const [isReservationProfilesModalOpen, setIsReservationProfilesModalOpen] = useState(false);

    const API_URL = 'http://localhost:8080';

    // Auth header helper function
    const getAuthHeader = () => {
        const token = localStorage.getItem('jwtToken');

        if (token) {
            console.log("Token găsit:", token.substring(0, 15) + "...");
            return {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            };
        } else {
            console.log("Nu s-a găsit token-ul JWT");
            return {};
        }
    };

    // Fetch users
    useEffect(() => {
        const fetchUsers = async () => {
            setIsLoading(true);
            try {
                console.log("Încarcă lista de utilizatori...");

                const response = await axios.get(
                    `${API_URL}/users`,
                    getAuthHeader()
                );

                console.log("Response data:", response.data);

                // Păstrăm toți utilizatorii, filtrarea se face la afișare
                setUsers(response.data);

                // Filtrăm inițial doar utilizatorii activi (null sau 'verified')
                const activeUsers = response.data.filter(user =>
                    user.accountStatus === null || user.accountStatus === 'verified'
                );
                setFilteredUsers(activeUsers);

                // Extrage lista de orașe disponibile pentru filtrare din toți utilizatorii
                const cities = [...new Set(response.data.map(user => user.city).filter(city => city))];
                setAvailableCities(cities.sort());

                setError(null);
            } catch (err) {
                console.error('Error fetching users:', err);
                if (err.response) {
                    console.error('Response status:', err.response.status);
                    console.error('Response data:', err.response.data);
                }

                // Mesaj de eroare personalizat în funcție de tipul erorii
                if (err.response?.status === 403) {
                    setError('Acces interzis. Vă rugăm să verificați că aveți drepturi de administrator.');
                } else if (err.response?.status === 401) {
                    setError('Sesiunea a expirat. Vă rugăm să vă autentificați din nou.');
                } else {
                    setError('Nu s-au putut încărca datele. Vă rugăm să încercați din nou.');
                }
            } finally {
                setIsLoading(false);
            }
        };

        fetchUsers();
    }, []);

    // Filter users by status and city
    useEffect(() => {
        let filtered = [...users];

        // Filtrare după status
        if (statusFilter === 'active') {
            filtered = filtered.filter(user =>
                user.accountStatus === null || user.accountStatus === 'verified'
            );
        } else if (statusFilter === 'suspended') {
            filtered = filtered.filter(user => user.accountStatus === 'suspended');
        }
        // Dacă statusFilter === 'all', nu filtrăm după status

        // Filtrare după oraș
        if (cityFilter) {
            filtered = filtered.filter(user => user.city === cityFilter);
        }

        setFilteredUsers(filtered);
    }, [statusFilter, cityFilter, users]);

    const openUserDetails = (user) => {
        setSelectedUser(user);
        setIsModalOpen(true);
        setIsEditing(false);

        // Pregătește datele pentru editare (dar nu activează editarea încă)
        setEditFormData({
            name: user.name || '',
            email: user.email || '',
            address: user.address || '',
            city: user.city || '',
            county: user.county || '',
            userType: user.userType || ''
        });
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setSelectedUser(null);
        setIsEditing(false);
    };

    const handleEditUser = () => {
        setIsEditing(true);
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
        // Resetează datele formularului la valorile utilizatorului selectat
        if (selectedUser) {
            setEditFormData({
                name: selectedUser.name || '',
                email: selectedUser.email || '',
                address: selectedUser.address || '',
                city: selectedUser.city || '',
                county: selectedUser.county || '',
                userType: selectedUser.userType || ''
            });
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setEditFormData(prevFormData => ({
            ...prevFormData,
            [name]: value
        }));
    };

    const handleSaveChanges = async () => {
        if (!selectedUser) return;

        try {
            await axios.put(
                `${API_URL}/users/${selectedUser.id}`,
                editFormData,
                getAuthHeader()
            );

            // Actualizează starea utilizatorului în lista locală
            const updatedUsers = users.map(user => {
                if (user.id === selectedUser.id) {
                    return { ...user, ...editFormData };
                }
                return user;
            });

            setUsers(updatedUsers);

            // Actualizează și utilizatorul selectat
            setSelectedUser({ ...selectedUser, ...editFormData });

            // Dezactivează modul de editare
            setIsEditing(false);

            alert('Utilizatorul a fost actualizat cu succes!');
        } catch (err) {
            console.error('Error updating user:', err);
            alert('A apărut o eroare la actualizarea utilizatorului. Vă rugăm să încercați din nou.');
        }
    };

    const handleSuspendUser = async (userId) => {
        try {
            // Confirmă că administratorul dorește să suspende utilizatorul
            if (!window.confirm('Sunteți sigur că doriți să suspendați acest utilizator?')) {
                return;
            }

            // Apelează endpoint-ul corect pentru suspendarea utilizatorului
            await axios.post(
                `${API_URL}/users/${userId}/suspend`,
                {},
                getAuthHeader()
            );

            // Actualizează starea utilizatorului în lista locală
            const updatedUsers = users.map(user => {
                if (user.id === userId) {
                    return { ...user, accountStatus: 'suspended' };
                }
                return user;
            });

            setUsers(updatedUsers);

            // Dacă suntem în filtrul 'active', eliminăm utilizatorul suspendat din vizualizare
            if (statusFilter === 'active') {
                setFilteredUsers(prevFiltered => prevFiltered.filter(user => user.id !== userId));
            }

            closeModal();
            alert('Utilizatorul a fost suspendat cu succes!');
        } catch (err) {
            console.error('Error suspending user:', err);
            alert('A aparut o eroare la suspendarea utilizatorului. Va rugam sa incercati din nou.');
        }
    };

    const handleActivateUser = async (userId) => {
        try {
            // Confirmă că administratorul dorește să reactiveze utilizatorul
            if (!window.confirm('Sunteți sigur că doriți să reactivați acest utilizator?')) {
                return;
            }

            // Actualizăm utilizatorul pentru a-i schimba statusul în 'verified'
            await axios.put(
                `${API_URL}/users/${userId}`,
                { accountStatus: 'verified' },
                getAuthHeader()
            );

            // Actualizează starea utilizatorului în lista locală
            const updatedUsers = users.map(user => {
                if (user.id === userId) {
                    return { ...user, accountStatus: 'verified' };
                }
                return user;
            });

            setUsers(updatedUsers);

            // Dacă suntem în filtrul 'suspended', eliminăm utilizatorul reactivat din vizualizare
            if (statusFilter === 'suspended') {
                setFilteredUsers(prevFiltered => prevFiltered.filter(user => user.id !== userId));
            }

            closeModal();
            alert('Utilizatorul a fost reactivat cu succes!');
        } catch (err) {
            console.error('Error activating user:', err);
            alert('A aparut o eroare la reactivarea utilizatorului. Va rugam sa incercati din nou.');
        }
    };

    // Handler to add a new user to the list after creation
    const handleUserCreated = (newUser) => {
        setUsers(prevUsers => [...prevUsers, newUser]);

        // If the new user meets current filter criteria, add to filtered users as well
        const isActive = newUser.accountStatus === null || newUser.accountStatus === 'verified';
        const matchesStatusFilter =
            (statusFilter === 'active' && isActive) ||
            (statusFilter === 'suspended' && newUser.accountStatus === 'suspended') ||
            statusFilter === 'all';

        const matchesCityFilter = !cityFilter || newUser.city === cityFilter;

        if (matchesStatusFilter && matchesCityFilter) {
            setFilteredUsers(prevFiltered => [...prevFiltered, newUser]);
        }

        // Add city to available cities if new
        if (newUser.city && !availableCities.includes(newUser.city)) {
            setAvailableCities(prev => [...prev, newUser.city].sort());
        }
    };

    // Funcție pentru a deschide modalul profilurilor de rezervare
    const handleOpenReservationProfiles = () => {
        setIsReservationProfilesModalOpen(true);
    };

    // Funcție pentru a închide modalul profilurilor de rezervare
    const handleCloseReservationProfiles = () => {
        setIsReservationProfilesModalOpen(false);
    };

    // Verifică dacă utilizatorul are teamType (este în echipă)
    const hasTeamType = (user) => {
        return user.teamType && user.teamType.trim() !== '';
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';

        try {
            const date = new Date(dateString);
            if (isNaN(date.getTime())) {
                return 'N/A';
            }

            const options = { year: 'numeric', month: 'long', day: 'numeric' };
            return date.toLocaleDateString('ro-RO', options);
        } catch (e) {
            console.error("Error formatting date:", e);
            return 'N/A';
        }
    };

    // Determinarea textului pentru roluri de utilizator
    const getUserTypeText = (userType) => {
        switch (userType) {
            case 'admin':
                return 'Administrator';
            case 'hall_admin':
                return 'Administrator sală';
            case 'user':
                return 'Utilizator';
            default:
                return userType || 'Nedefinit';
        }
    };

    // Determinarea textului pentru status cont
    const getAccountStatusText = (accountStatus) => {
        switch (accountStatus) {
            case 'verified':
                return 'Verificat';
            case 'suspended':
                return 'Suspendat';
            case 'pending':
                return 'În așteptare';
            case 'rejected':
                return 'Respins';
            default:
                return 'Activ';
        }
    };

    return (
        <div className="global-admin-page">
            <h2 className="global-admin-page-title">Gestiunea utilizatorilor</h2>

            <div className="admin-actions-bar">
                <button
                    className="admin-action-button create-user"
                    onClick={() => setIsCreateUserModalOpen(true)}
                >
                    <span className="action-icon">+</span> Creează utilizator nou
                </button>
                <button
                    className="admin-action-button create-hall-admin"
                    onClick={() => setIsCreateHallAdminModalOpen(true)}
                >
                    <span className="action-icon">+</span> Creează administrator sală
                </button>
            </div>

            <div className="user-filters">
                <div className="filters-left">
                    <div className="status-filter">
                        <label htmlFor="status-select">Status:</label>
                        <select
                            id="status-select"
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                            className="status-select"
                        >
                            <option value="active">Utilizatori activi</option>
                            <option value="suspended">Utilizatori suspendați</option>
                            <option value="all">Toți utilizatorii</option>
                        </select>
                    </div>

                    <div className="city-filter">
                        <label htmlFor="city-select">Oraș:</label>
                        <select
                            id="city-select"
                            value={cityFilter}
                            onChange={(e) => setCityFilter(e.target.value)}
                            className="city-select"
                        >
                            <option value="">Toate orașele</option>
                            {availableCities.map((city, index) => (
                                <option key={index} value={city}>{city}</option>
                            ))}
                        </select>
                    </div>
                </div>

                <div className="filter-stats">
                    Afișare {filteredUsers.length} din {users.length} utilizatori
                </div>
            </div>

            <div className="users-table-container">
                {isLoading ? (
                    <div className="loading-message">Se încarcă...</div>
                ) : error ? (
                    <div className="error-message">{error}</div>
                ) : (
                    <table className="users-table">
                        <thead>
                        <tr>
                            <th>Nume</th>
                            <th>Email</th>
                            <th>Rol</th>
                            <th>Oraș</th>
                            <th>Status</th>
                            <th>Acțiuni</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredUsers.length > 0 ? (
                            filteredUsers.map(user => (
                                <tr key={user.id} className="user-row">
                                    <td className="user-name-cell">{user.name}</td>
                                    <td className="user-email-cell">{user.email}</td>
                                    <td className="user-type-cell">
                                        <span className={`user-type-badge user-type-${user.userType || 'user'}`}>
                                            {getUserTypeText(user.userType)}
                                        </span>
                                    </td>
                                    <td className="user-city-cell">{user.city || 'N/A'}</td>
                                    <td className="user-status-cell">
                                        <span className={`user-status-badge user-status-${user.accountStatus || 'active'}`}>
                                            {getAccountStatusText(user.accountStatus)}
                                        </span>
                                    </td>
                                    <td className="user-actions-cell">
                                        <button
                                            className="view-details-btn"
                                            onClick={() => openUserDetails(user)}
                                        >
                                            Detalii
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan="6" className="users-empty-row">
                                    Nu există utilizatori care să corespundă criteriilor de filtrare.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Modal for viewing/editing existing user */}
            {isModalOpen && selectedUser && (
                <div className="user-modal-overlay" onClick={closeModal}>
                    <div className="user-modal" onClick={e => e.stopPropagation()}>
                        <div className="user-modal-header">
                            <h3 className="user-modal-title">
                                {isEditing ? 'Editare utilizator' : 'Detalii utilizator'}: {selectedUser.name}
                            </h3>
                            <button className="user-modal-close" onClick={closeModal}>×</button>
                        </div>

                        <div className="user-modal-body">
                            {isEditing ? (
                                <div className="user-edit-form">
                                    <div className="form-group">
                                        <label htmlFor="name">Nume:</label>
                                        <input
                                            type="text"
                                            id="name"
                                            name="name"
                                            value={editFormData.name}
                                            onChange={handleInputChange}
                                            required
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label htmlFor="email">Email:</label>
                                        <input
                                            type="email"
                                            id="email"
                                            name="email"
                                            value={editFormData.email}
                                            onChange={handleInputChange}
                                            required
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label htmlFor="userType">Rol:</label>
                                        <select
                                            id="userType"
                                            name="userType"
                                            value={editFormData.userType}
                                            onChange={handleInputChange}
                                        >
                                            <option value="user">Utilizator</option>
                                            <option value="hall_admin">Administrator sală</option>
                                            <option value="admin">Administrator</option>
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label htmlFor="address">Adresă:</label>
                                        <input
                                            type="text"
                                            id="address"
                                            name="address"
                                            value={editFormData.address}
                                            onChange={handleInputChange}
                                        />
                                    </div>
                                    <div className="form-row">
                                        <div className="form-group">
                                            <label htmlFor="city">Oraș:</label>
                                            <input
                                                type="text"
                                                id="city"
                                                name="city"
                                                value={editFormData.city}
                                                onChange={handleInputChange}
                                            />
                                        </div>
                                        <div className="form-group">
                                            <label htmlFor="county">Județ:</label>
                                            <input
                                                type="text"
                                                id="county"
                                                name="county"
                                                value={editFormData.county}
                                                onChange={handleInputChange}
                                            />
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <div className="user-detail-sections">
                                    <div className="user-detail-section">
                                        <h4>Informații generale</h4>
                                        <div className="user-detail-grid">
                                            <div className="user-detail-item">
                                                <span className="detail-label">Nume:</span>
                                                <span className="detail-value">{selectedUser.name}</span>
                                            </div>
                                            <div className="user-detail-item">
                                                <span className="detail-label">Email:</span>
                                                <span className="detail-value">{selectedUser.email}</span>
                                            </div>
                                            <div className="user-detail-item">
                                                <span className="detail-label">Rol:</span>
                                                <span className={`detail-value user-type-text-${selectedUser.userType || 'user'}`}>
                                                    {getUserTypeText(selectedUser.userType)}
                                                </span>
                                            </div>
                                            <div className="user-detail-item">
                                                <span className="detail-label">Status cont:</span>
                                                <span className={`detail-value user-status-text-${selectedUser.accountStatus || 'active'}`}>
                                                    {getAccountStatusText(selectedUser.accountStatus)}
                                                </span>
                                            </div>
                                            <div className="user-detail-item">
                                                <span className="detail-label">Data înregistrării:</span>
                                                <span className="detail-value">{formatDate(selectedUser.createdAt)}</span>
                                            </div>
                                            {selectedUser.teamType && (
                                                <div className="user-detail-item">
                                                    <span className="detail-label">Tip echipă:</span>
                                                    <span className="detail-value">{selectedUser.teamType}</span>
                                                </div>
                                            )}
                                        </div>
                                    </div>

                                    {(selectedUser.address || selectedUser.city || selectedUser.county) && (
                                        <div className="user-detail-section">
                                            <h4>Adresă</h4>
                                            <div className="user-detail-grid">
                                                {selectedUser.address && (
                                                    <div className="user-detail-item full-width">
                                                        <span className="detail-label">Adresă completă:</span>
                                                        <span className="detail-value">{selectedUser.address}</span>
                                                    </div>
                                                )}
                                                {selectedUser.city && (
                                                    <div className="user-detail-item">
                                                        <span className="detail-label">Oraș:</span>
                                                        <span className="detail-value">{selectedUser.city}</span>
                                                    </div>
                                                )}
                                                {selectedUser.county && (
                                                    <div className="user-detail-item">
                                                        <span className="detail-label">Județ:</span>
                                                        <span className="detail-value">{selectedUser.county}</span>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        <div className="user-modal-footer">
                            {isEditing ? (
                                <>
                                    <button className="user-action-btn cancel" onClick={handleCancelEdit}>
                                        Anulează
                                    </button>
                                    <button className="user-action-btn save" onClick={handleSaveChanges}>
                                        Salvează modificările
                                    </button>
                                </>
                            ) : (
                                <>
                                    {/* Butonul pentru profiluri de rezervare - doar pentru utilizatori cu teamType */}
                                    {hasTeamType(selectedUser) && (
                                        <button
                                            className="user-action-btn reservation-profiles"
                                            onClick={handleOpenReservationProfiles}
                                        >
                                            Profiluri rezervare
                                        </button>
                                    )}

                                    {selectedUser.accountStatus === 'suspended' ? (
                                        <button
                                            className="user-action-btn activate"
                                            onClick={() => handleActivateUser(selectedUser.id)}
                                        >
                                            Reactivează utilizatorul
                                        </button>
                                    ) : (
                                        <button
                                            className="user-action-btn suspend"
                                            onClick={() => handleSuspendUser(selectedUser.id)}
                                        >
                                            Suspendă utilizatorul
                                        </button>
                                    )}
                                    <button className="user-action-btn edit" onClick={handleEditUser}>
                                        Editează
                                    </button>
                                </>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Modal for creating new regular user */}
            <CreateUserModal
                isOpen={isCreateUserModalOpen}
                onClose={() => setIsCreateUserModalOpen(false)}
                onUserCreated={handleUserCreated}
                userType="user"
                title="Creare utilizator nou"
                API_URL={API_URL}
                getAuthHeader={getAuthHeader}
            />

            {/* Modal for creating new hall admin */}
            <CreateUserModal
                isOpen={isCreateHallAdminModalOpen}
                onClose={() => setIsCreateHallAdminModalOpen(false)}
                onUserCreated={handleUserCreated}
                userType="hall_admin"
                title="Creare administrator sală nou"
                API_URL={API_URL}
                getAuthHeader={getAuthHeader}
            />

            {/* Modal pentru profiluri de rezervare */}
            <ReservationProfilesModal
                isOpen={isReservationProfilesModalOpen}
                onClose={handleCloseReservationProfiles}
                userId={selectedUser?.id}
                userName={selectedUser?.name}
                API_URL={API_URL}
                getAuthHeader={getAuthHeader}
            />
        </div>
    );
};

export default ManageUsersPage;