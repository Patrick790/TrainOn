import React, { useState, useEffect } from 'react';
import './ApproveRegistrationsPage.css';
import axios from 'axios';

const ApproveRegistrationsPage = () => {
    const [teamRegistrations, setTeamRegistrations] = useState([]);
    const [activeTab, setActiveTab] = useState('pending');
    const [selectedTeam, setSelectedTeam] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isImageFullscreen, setIsImageFullscreen] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [certificateUrl, setCertificateUrl] = useState(null);

    const API_URL = process.env.REACT_APP_API_URL || '';

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

    // Fetch team registrations based on active tab
    useEffect(() => {
        const fetchTeams = async () => {
            setIsLoading(true);
            try {
                const status = activeTab;
                console.log("Fetching teams with status:", status);

                const response = await axios.get(
                    `${API_URL}/admin/team-registrations?status=${status}`,
                    getAuthHeader()
                );

                console.log("Response data:", response.data);

                const filteredTeams = response.data.filter(team => {
                    if (activeTab === 'pending') {
                        return team.accountStatus === 'pending' || team.accountStatus === null;
                    } else if (activeTab === 'rejected') {
                        return team.accountStatus === 'rejected';
                    }
                    return true;
                });

                setTeamRegistrations(filteredTeams);
                setError(null);
            } catch (err) {
                console.error('Error fetching team registrations:', err);
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

        fetchTeams();
    }, [activeTab]);

    const openTeamDetails = async (team) => {
        setSelectedTeam(team);
        setIsModalOpen(true);

        try {
            // Folosește header-ul de autorizare standard
            const response = await axios.get(
                `${API_URL}/admin/team-registrations/${team.id}/certificate`,
                {
                    ...getAuthHeader(),
                    responseType: 'blob' // Important pentru binare
                }
            );

            // Creeaza un URL pentru blob
            const blob = new Blob([response.data], { type: response.headers['content-type'] });
            const url = URL.createObjectURL(blob);
            setCertificateUrl(url);
        } catch (error) {
            console.error("Eroare la încărcarea certificatului:", error);
            setCertificateUrl(null);
        }
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setSelectedTeam(null);
        if (certificateUrl) {
            URL.revokeObjectURL(certificateUrl);
        }
        setCertificateUrl(null);
    };

    const toggleImageFullscreen = (e) => {
        e.stopPropagation(); // Prevent modal from closing
        setIsImageFullscreen(!isImageFullscreen);
    };

    const closeImageFullscreen = () => {
        setIsImageFullscreen(false);
    };

    const handleApproveTeam = async (teamId) => {
        try {
            await axios.post(
                `${API_URL}/admin/team-registrations/${teamId}/approve`,
                {},
                getAuthHeader()
            );

            // Remove the approved team from the list
            setTeamRegistrations(teamRegistrations.filter(team => team.id !== teamId));
            alert('Echipa a fost aprobată cu succes!');
            closeModal();
        } catch (err) {
            console.error('Error approving team:', err);
            alert('A apărut o eroare la aprobarea echipei. Vă rugăm să încercați din nou.');
        }
    };

    const handleRejectTeam = async (teamId) => {
        const reason = prompt("Introduceți motivul respingerii:", "");
        if (reason && reason.trim() !== '') {
            try {
                await axios.post(
                    `${API_URL}/admin/team-registrations/${teamId}/reject`,
                    { rejectionReason: reason },
                    getAuthHeader()
                );

                // If we're on the pending tab, remove the team from the list
                if (activeTab === 'pending') {
                    setTeamRegistrations(teamRegistrations.filter(team => team.id !== teamId));
                }
                alert('Echipa a fost respinsă cu succes!');
                closeModal();
            } catch (err) {
                console.error('Error rejecting team:', err);
                alert('A apărut o eroare la respingerea echipei. Vă rugăm să încercați din nou.');
            }
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';

        // Verifica dacă dateString este valid
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

    const checkIsAdmin = () => {
        return localStorage.getItem('userType') === 'admin';
    };

    if (!checkIsAdmin()) {
        return (
            <div className="global-admin-page">
                <div className="error-message">
                    <h2>Acces restricționat</h2>
                    <p>Această pagină este disponibilă doar pentru administratori.</p>
                </div>
            </div>
        );
    }

    const getStatusText = (accountStatus) => {
        if (accountStatus === 'pending' || accountStatus === null) {
            return 'În așteptare';
        } else if (accountStatus === 'rejected') {
            return 'Respins';
        } else if (accountStatus === 'verified') {
            return 'Verificat';
        }
        return accountStatus;
    };

    return (
        <div className="global-admin-page">
            <h2 className="global-admin-page-title">Aprobarea cererilor de înregistrare</h2>

            <div className="registration-tabs">
                <button
                    className={`registration-tab ${activeTab === 'pending' ? 'active' : ''}`}
                    onClick={() => setActiveTab('pending')}
                >
                    Cereri în așteptare
                </button>
                <button
                    className={`registration-tab ${activeTab === 'rejected' ? 'active' : ''}`}
                    onClick={() => setActiveTab('rejected')}
                >
                    Cereri respinse
                </button>
            </div>

            <div className="registration-table-container">
                {isLoading ? (
                    <div className="loading-message">Se încarcă...</div>
                ) : error ? (
                    <div className="error-message">{error}</div>
                ) : (
                    <table className="registration-table">
                        <thead>
                        <tr>
                            <th>Nume echipă</th>
                            <th>Data cererii</th>
                            <th>Status</th>
                            <th>Acțiuni</th>
                        </tr>
                        </thead>
                        <tbody>
                        {teamRegistrations.length > 0 ? (
                            teamRegistrations.map(team => (
                                <tr key={team.id} className="registration-row">
                                    <td className="team-name-cell">{team.name}</td>
                                    <td className="team-date-cell">{formatDate(team.createdAt)}</td>
                                    <td className="team-status-cell">
                                        <span className={`table-status table-status-${team.accountStatus || 'pending'}`}>
                                            {getStatusText(team.accountStatus)}
                                        </span>
                                    </td>
                                    <td className="team-actions-cell">
                                        <button
                                            className="view-details-btn"
                                            onClick={() => openTeamDetails(team)}
                                        >
                                            Detalii
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan="4" className="registration-empty-row">
                                    Nu există cereri {activeTab === 'pending' ? 'în așteptare' : 'respinse'} în acest moment.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                )}
            </div>

            {isModalOpen && selectedTeam && (
                <div className="registration-modal-overlay" onClick={closeModal}>
                    <div className="registration-modal" onClick={e => e.stopPropagation()}>
                        <div className="registration-modal-header">
                            <h3 className="registration-modal-title">Detalii cerere: {selectedTeam.name}</h3>
                            <button className="registration-modal-close" onClick={closeModal}>×</button>
                        </div>

                        <div className="registration-modal-body">
                            <div className="registration-modal-columns">
                                <div className="registration-modal-left">
                                    <div className="registration-certificate-container">
                                        <h4>Certificat de înregistrare</h4>
                                        <div className="registration-certificate">
                                            {certificateUrl ? (
                                                <img
                                                    src={certificateUrl}
                                                    alt="Certificat"
                                                    onClick={toggleImageFullscreen}
                                                    className="certificate-image"
                                                    onError={(e) => {
                                                        console.error("Eroare la încărcarea imaginii");
                                                        e.target.src = "https://via.placeholder.com/400x500?text=Certificat+Lipsă";
                                                    }}
                                                />
                                            ) : (
                                                <div className="certificate-placeholder">
                                                    Certificat indisponibil
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>

                                <div className="registration-modal-right">
                                    <div className="registration-detail-section">
                                        <h4>Informații echipă</h4>
                                        <div className="registration-detail-grid">
                                            <div className="registration-detail-item">
                                                <span className="detail-label">Nume:</span>
                                                <span className="detail-value">{selectedTeam.name}</span>
                                            </div>
                                            <div className="registration-detail-item">
                                                <span className="detail-label">Tip echipă:</span>
                                                <span className="detail-value">{selectedTeam.teamType}</span>
                                            </div>
                                            <div className="registration-detail-item">
                                                <span className="detail-label">Email:</span>
                                                <span className="detail-value">{selectedTeam.email}</span>
                                            </div>
                                            <div className="registration-detail-item">
                                                <span className="detail-label">Oraș:</span>
                                                <span className="detail-value">{selectedTeam.city}</span>
                                            </div>
                                            <div className="registration-detail-item">
                                                <span className="detail-label">Județ:</span>
                                                <span className="detail-value">{selectedTeam.county}</span>
                                            </div>
                                            <div className="registration-detail-item">
                                                <span className="detail-label">Data cererii:</span>
                                                <span className="detail-value">{formatDate(selectedTeam.createdAt)}</span>
                                            </div>
                                            <div className="registration-detail-item">
                                                <span className="detail-label">Status:</span>
                                                <span className={`detail-value status-${selectedTeam.accountStatus || 'pending'}`}>
                                                    {getStatusText(selectedTeam.accountStatus)}
                                                </span>
                                            </div>
                                        </div>
                                    </div>

                                    {selectedTeam.address && (
                                        <div className="registration-detail-section">
                                            <h4>Adresă</h4>
                                            <p className="registration-description">{selectedTeam.address}</p>
                                        </div>
                                    )}

                                    {selectedTeam.accountStatus === 'rejected' && selectedTeam.rejectionReason && (
                                        <div className="registration-detail-section rejection-reason">
                                            <h4>Motiv respingere</h4>
                                            <p>{selectedTeam.rejectionReason}</p>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>

                        <div className="registration-modal-footer">
                            {(selectedTeam.accountStatus === 'pending' || selectedTeam.accountStatus === null) && (
                                <>
                                    <button
                                        className="registration-action-btn reject"
                                        onClick={() => handleRejectTeam(selectedTeam.id)}
                                    >
                                        Respinge
                                    </button>
                                    <button
                                        className="registration-action-btn approve"
                                        onClick={() => handleApproveTeam(selectedTeam.id)}
                                    >
                                        Aprobă
                                    </button>
                                </>
                            )}
                            {selectedTeam.accountStatus === 'rejected' && (
                                <button
                                    className="registration-action-btn approve"
                                    onClick={() => handleApproveTeam(selectedTeam.id)}
                                >
                                    Reconsideră și aprobă
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Fullscreen image view */}
            {isImageFullscreen && certificateUrl && (
                <div className="fullscreen-image-overlay" onClick={closeImageFullscreen}>
                    <div className="fullscreen-image-container">
                        <button className="fullscreen-close-btn" onClick={closeImageFullscreen}>×</button>
                        <img
                            src={certificateUrl}
                            alt="Certificat la dimensiune completă"
                            className="fullscreen-image"
                            onError={(e) => {
                                e.target.src = "https://via.placeholder.com/800x1000?text=Certificat+Lipsă";
                            }}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default ApproveRegistrationsPage;