import React, { Component } from 'react';
import { X, Edit, Save, AlertTriangle, MapPin, Phone, Clock, Star, User } from 'lucide-react';
import './HallManagementModal.css';

class HallManagementModal extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';
        this.state = {
            hall: null,
            loading: true,
            error: null,
            isEditing: false,
            isSaving: false,
            editData: {},
            showDeactivateConfirm: false,
            adminData: null,
            hallRating: { averageRating: 0, totalReviews: 0 }
        };
    }

    componentDidMount() {
        if (this.props.isOpen && this.props.hallId) {
            this.fetchHallDetails();
        }
    }

    componentDidUpdate(prevProps) {
        if (this.props.isOpen && !prevProps.isOpen && this.props.hallId) {
            this.fetchHallDetails();
        }
    }

    fetchHallDetails = async () => {
        try {
            this.setState({ loading: true, error: null });
            const token = localStorage.getItem('jwtToken');

            // Fetch hall details
            const hallResponse = await fetch(`${this.API_BASE_URL}/sportsHalls/${this.props.hallId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!hallResponse.ok) {
                throw new Error('Nu s-au putut încărca detaliile sălii');
            }

            const hall = await hallResponse.json();

            // Fetch admin data
            let adminData = null;
            if (hall.adminId) {
                try {
                    const adminResponse = await fetch(`${this.API_BASE_URL}/users/${hall.adminId}`, {
                        headers: { 'Authorization': `Bearer ${token}` }
                    });
                    if (adminResponse.ok) {
                        adminData = await adminResponse.json();
                    }
                } catch (err) {
                    console.warn('Nu s-au putut încărca datele administratorului');
                }
            }

            // Fetch ratings
            let hallRating = { averageRating: 0, totalReviews: 0 };
            try {
                const ratingsResponse = await fetch(`${this.API_BASE_URL}/feedbacks?hallId=${hall.id}`);
                if (ratingsResponse.ok) {
                    const feedbacks = await ratingsResponse.json();
                    hallRating = this.calculateRatingFromFeedbacks(feedbacks);
                }
            } catch (err) {
                console.warn('Nu s-au putut încărca rating-urile');
            }

            this.setState({
                hall,
                adminData,
                hallRating,
                editData: { ...hall },
                loading: false
            });

        } catch (error) {
            console.error('Error fetching hall details:', error);
            this.setState({
                error: error.message,
                loading: false
            });
        }
    }

    calculateRatingFromFeedbacks = (feedbacks) => {
        if (!feedbacks || feedbacks.length === 0) {
            return { averageRating: 0, totalReviews: 0 };
        }

        const totalRating = feedbacks.reduce((sum, feedback) => sum + (feedback.rating || 0), 0);
        const averageRating = totalRating / feedbacks.length;

        return {
            averageRating: Math.round(averageRating * 10) / 10,
            totalReviews: feedbacks.length
        };
    }

    handleEdit = () => {
        this.setState({
            isEditing: true,
            editData: { ...this.state.hall }
        });
    }

    handleCancelEdit = () => {
        this.setState({
            isEditing: false,
            editData: { ...this.state.hall }
        });
    }

    handleInputChange = (field, value) => {
        this.setState(prevState => ({
            editData: {
                ...prevState.editData,
                [field]: value
            }
        }));
    }

    handleSave = async () => {
        try {
            this.setState({ isSaving: true });
            const token = localStorage.getItem('jwtToken');

            const formData = new FormData();

            formData.append('sportsHall', JSON.stringify(this.state.editData));

            const response = await fetch(`${this.API_BASE_URL}/sportsHalls/${this.state.hall.id}`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                body: formData
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'Nu s-au putut salva modificarile');
            }

            const updatedHall = await response.json();

            this.setState({
                hall: updatedHall,
                editData: { ...updatedHall },
                isEditing: false,
                isSaving: false,
                error: null
            });

            if (this.props.onHallUpdated) {
                this.props.onHallUpdated(updatedHall);
            }

        } catch (error) {
            console.error('Error saving hall:', error);
            this.setState({
                error: error.message,
                isSaving: false
            });
        }
    }

    handleDeactivate = async () => {
        try {
            const token = localStorage.getItem('jwtToken');
            const currentStatus = this.state.hall.status;
            const isCurrentlyActive = currentStatus === 'ACTIVE';

            // Folosește endpoint-urile specifice pentru activare/dezactivare
            const endpoint = isCurrentlyActive
                ? `${this.API_BASE_URL}/sportsHalls/${this.state.hall.id}/deactivate`
                : `${this.API_BASE_URL}/sportsHalls/${this.state.hall.id}/activate`;

            console.log('=== DEBUG INFO ===');
            console.log('Token exists:', !!token);
            console.log('Hall ID:', this.state.hall.id);
            console.log('Current status:', this.state.hall.status);
            console.log('Endpoint:', endpoint);
            console.log('Is currently active:', isCurrentlyActive);

            const response = await fetch(endpoint, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            console.log('Response status:', response.status);
            console.log('Response ok:', response.ok);

            if (!response.ok) {
                const errorText = await response.text();
                console.error('Error response:', errorText);
                throw new Error(errorText || `Nu s-a putut ${isCurrentlyActive ? 'dezactiva' : 'activa'} sala`);
            }

            const updatedHall = await response.json();
            console.log('Updated hall:', updatedHall);

            this.setState({
                hall: updatedHall,
                editData: { ...updatedHall },
                showDeactivateConfirm: false,
                error: null
            });

            if (this.props.onHallUpdated) {
                this.props.onHallUpdated(updatedHall);
            }

            console.log(`Sala ${isCurrentlyActive ? 'dezactivată' : 'activată'} cu succes`);

        } catch (error) {
            console.error('Error changing hall status:', error);
            this.setState({
                error: error.message,
                showDeactivateConfirm: false
            });
        }
    }

    getCoverImage = (hall) => {
        if (hall?.images && hall.images.length > 0) {
            const coverImage = hall.images.find(img => img.description === 'cover');
            if (coverImage) {
                return `${this.API_BASE_URL}/images/${coverImage.id}`;            }
        }
        return null;
    }

    renderRatingStars = (rating) => {
        const totalStars = 5;
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.3 && rating % 1 <= 0.7;
        const hasAlmostFullStar = rating % 1 > 0.7;

        return (
            <div className="modal-rating-stars">
                {[...Array(totalStars)].map((_, index) => {
                    if (index < fullStars) {
                        return <Star key={index} size={16} fill="#FFD700" color="#FFD700" />;
                    } else if (index === fullStars && hasHalfStar) {
                        return (
                            <div className="modal-half-star-container" key={index}>
                                <Star size={16} fill="#FFD700" color="#FFD700" className="modal-half-star" />
                                <Star size={16} fill="none" color="#D1D5DB" className="modal-star-background" />
                            </div>
                        );
                    } else if (index === fullStars && hasAlmostFullStar) {
                        return <Star key={index} size={16} fill="#FFD700" color="#FFD700" />;
                    } else {
                        return <Star key={index} size={16} fill="none" color="#D1D5DB" />;
                    }
                })}
            </div>
        );
    }

    render() {
        if (!this.props.isOpen) return null;

        const { hall, loading, error, isEditing, isSaving, editData, showDeactivateConfirm, adminData, hallRating } = this.state;

        return (
            <div className="hall-management-modal-overlay" onClick={this.props.onClose}>
                <div className="hall-management-modal" onClick={(e) => e.stopPropagation()}>
                    <div className="hall-management-modal-header">
                        <h2>Gestionare Sală</h2>
                        <button className="hall-management-close-btn" onClick={this.props.onClose}>
                            <X size={24} />
                        </button>
                    </div>

                    <div className="hall-management-modal-content">
                        {loading && (
                            <div className="hall-management-loading">
                                Se încarcă detaliile sălii...
                            </div>
                        )}

                        {error && (
                            <div className="hall-management-error">
                                Eroare: {error}
                            </div>
                        )}

                        {hall && (
                            <>
                                <div className="hall-management-main-info">
                                    <div className="hall-management-image-section">
                                        {this.getCoverImage(hall) ? (
                                            <img
                                                src={this.getCoverImage(hall)}
                                                alt={hall.name}
                                                className="hall-management-cover-image"
                                            />
                                        ) : (
                                            <div className="hall-management-placeholder">
                                                <div className="hall-management-initial">
                                                    {hall.name.charAt(0)}
                                                </div>
                                            </div>
                                        )}

                                        <div className="hall-management-rating-section">
                                            {this.renderRatingStars(hallRating.averageRating)}
                                            <span className="hall-management-rating-text">
                                                {hallRating.averageRating > 0
                                                    ? `${hallRating.averageRating} (${hallRating.totalReviews} ${hallRating.totalReviews === 1 ? 'recenzie' : 'recenzii'})`
                                                    : 'Fără recenzii'
                                                }
                                            </span>
                                        </div>
                                    </div>

                                    <div className="hall-management-details-section">
                                        <div className="hall-management-field">
                                            <label>Nume sală:</label>
                                            {isEditing ? (
                                                <input
                                                    type="text"
                                                    value={editData.name || ''}
                                                    onChange={(e) => this.handleInputChange('name', e.target.value)}
                                                    className="hall-management-input"
                                                />
                                            ) : (
                                                <span>{hall.name}</span>
                                            )}
                                        </div>

                                        <div className="hall-management-field">
                                            <label>Descriere:</label>
                                            {isEditing ? (
                                                <textarea
                                                    value={editData.description || ''}
                                                    onChange={(e) => this.handleInputChange('description', e.target.value)}
                                                    className="hall-management-textarea"
                                                    rows="3"
                                                />
                                            ) : (
                                                <span>{hall.description || 'Fără descriere'}</span>
                                            )}
                                        </div>

                                        <div className="hall-management-location">
                                            <div className="hall-management-field">
                                                <label><MapPin size={16} /> Oraș:</label>
                                                {isEditing ? (
                                                    <input
                                                        type="text"
                                                        value={editData.city || ''}
                                                        onChange={(e) => this.handleInputChange('city', e.target.value)}
                                                        className="hall-management-input"
                                                    />
                                                ) : (
                                                    <span>{hall.city}</span>
                                                )}
                                            </div>

                                            <div className="hall-management-field">
                                                <label>Județ:</label>
                                                {isEditing ? (
                                                    <input
                                                        type="text"
                                                        value={editData.county || ''}
                                                        onChange={(e) => this.handleInputChange('county', e.target.value)}
                                                        className="hall-management-input"
                                                    />
                                                ) : (
                                                    <span>{hall.county}</span>
                                                )}
                                            </div>
                                        </div>

                                        <div className="hall-management-field">
                                            <label>Adresă:</label>
                                            {isEditing ? (
                                                <input
                                                    type="text"
                                                    value={editData.address || ''}
                                                    onChange={(e) => this.handleInputChange('address', e.target.value)}
                                                    className="hall-management-input"
                                                />
                                            ) : (
                                                <span>{hall.address}</span>
                                            )}
                                        </div>

                                        <div className="hall-management-field">
                                            <label>Tarif (RON/1h30):</label>
                                            {isEditing ? (
                                                <input
                                                    type="number"
                                                    step="0.01"
                                                    min="0"
                                                    value={editData.tariff || ''}
                                                    onChange={(e) => this.handleInputChange('tariff', parseFloat(e.target.value) || 0)}
                                                    className="hall-management-input"
                                                />
                                            ) : (
                                                <span>{hall.tariff ? `${hall.tariff} RON/1h30` : 'Nu este specificat'}</span>
                                            )}
                                        </div>

                                        <div className="hall-management-field">
                                            <label>Capacitate:</label>
                                            {isEditing ? (
                                                <input
                                                    type="number"
                                                    min="1"
                                                    value={editData.capacity || ''}
                                                    onChange={(e) => this.handleInputChange('capacity', parseInt(e.target.value) || 1)}
                                                    className="hall-management-input"
                                                />
                                            ) : (
                                                <span>{hall.capacity ? `${hall.capacity} persoane` : 'Nu este specificată'}</span>
                                            )}
                                        </div>

                                        <div className="hall-management-contact">
                                            <div className="hall-management-field">
                                                <label><Phone size={16} /> Telefon:</label>
                                                {isEditing ? (
                                                    <input
                                                        type="tel"
                                                        value={editData.phoneNumber || ''}
                                                        onChange={(e) => this.handleInputChange('phoneNumber', e.target.value)}
                                                        className="hall-management-input"
                                                    />
                                                ) : (
                                                    <span>{hall.phoneNumber || 'Nu este specificat'}</span>
                                                )}
                                            </div>

                                            <div className="hall-management-field">
                                                <label>Status:</label>
                                                <span className={`hall-status-badge ${hall.status?.toLowerCase()}`}>
                                                    {hall.status === 'ACTIVE' ? '🟢 Activ' : '🔴 Inactiv'}
                                                </span>
                                            </div>
                                        </div>

                                        {adminData && (
                                            <div className="hall-management-admin-info">
                                                <label><User size={16} /> Administrator:</label>
                                                <div className="hall-management-admin-details">
                                                    <span className="admin-name">{adminData.name}</span>
                                                    <span className="admin-email">{adminData.email}</span>
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                <div className="hall-management-actions">
                                    {!isEditing ? (
                                        <>
                                            <button
                                                className="hall-management-btn edit-btn"
                                                onClick={this.handleEdit}
                                            >
                                                <Edit size={16} />
                                                Editează
                                            </button>
                                            {hall.status === 'ACTIVE' ? (
                                                <button
                                                    className="hall-management-btn deactivate-btn"
                                                    onClick={() => this.setState({ showDeactivateConfirm: true })}
                                                >
                                                    <AlertTriangle size={16} />
                                                    Dezactivează
                                                </button>
                                            ) : (
                                                <button
                                                    className="hall-management-btn activate-btn"
                                                    onClick={() => this.setState({ showDeactivateConfirm: true })}
                                                >
                                                    <AlertTriangle size={16} />
                                                    Activează
                                                </button>
                                            )}
                                        </>
                                    ) : (
                                        <>
                                            <button
                                                className="hall-management-btn save-btn"
                                                onClick={this.handleSave}
                                                disabled={isSaving}
                                            >
                                                <Save size={16} />
                                                {isSaving ? 'Se salvează...' : 'Salvează'}
                                            </button>
                                            <button
                                                className="hall-management-btn cancel-btn"
                                                onClick={this.handleCancelEdit}
                                                disabled={isSaving}
                                            >
                                                Anulează
                                            </button>
                                        </>
                                    )}
                                </div>

                                {showDeactivateConfirm && (
                                    <div className="hall-management-confirm-overlay">
                                        <div className="hall-management-confirm-dialog">
                                            <h3>
                                                {hall.status === 'ACTIVE' ? 'Confirmare dezactivare' : 'Confirmare activare'}
                                            </h3>
                                            <p>
                                                Ești sigur că vrei să {hall.status === 'ACTIVE' ? 'dezactivezi' : 'activezi'} sala "{hall.name}"?
                                            </p>
                                            <p className="warning-text">
                                                <AlertTriangle size={16} />
                                                {hall.status === 'ACTIVE'
                                                    ? 'Această acțiune va schimba status-ul sălii în "Inactiv"!'
                                                    : 'Această acțiune va schimba status-ul sălii în "Activ"!'
                                                }
                                            </p>
                                            <div className="hall-management-confirm-actions">
                                                <button
                                                    className={`hall-management-btn ${hall.status === 'ACTIVE' ? 'confirm-deactivate-btn' : 'confirm-activate-btn'}`}
                                                    onClick={this.handleDeactivate}
                                                >
                                                    Da, {hall.status === 'ACTIVE' ? 'dezactivează' : 'activează'}
                                                </button>
                                                <button
                                                    className="hall-management-btn cancel-btn"
                                                    onClick={() => this.setState({ showDeactivateConfirm: false })}
                                                >
                                                    Anulează
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>
        );
    }
}

export default HallManagementModal;