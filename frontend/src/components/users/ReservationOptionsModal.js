import React from 'react';
import { useNavigate } from 'react-router-dom';
import './ReservationOptionsModal.css';

const ReservationOptionsModal = ({ isOpen, onClose }) => {
    const navigate = useNavigate();

    if (!isOpen) return null;

    const handleCreateProfile = () => {
        navigate('/profile-creation');
        onClose();
    };

    const handleReserveRemaining = () => {
        navigate('/fcfs-reservation');
        onClose();
    };

    return (
        <div className="reservation-modal-backdrop">
            <div className="reservation-modal-content">
                <h2 className="reservation-modal-title">Opțiuni de rezervare</h2>
                <p className="reservation-modal-description">
                    Alege modul în care dorești să faci rezervare
                </p>

                <div className="reservation-options">
                    <div className="reservation-option" onClick={handleCreateProfile}>
                        <div className="reservation-option-icon">
                            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                <circle cx="12" cy="7" r="4"></circle>
                            </svg>
                        </div>
                        <h3>Creează profil rezervări</h3>
                        <p>Personalizează opțiunile pentru algoritm de prioritizare</p>
                    </div>

                    <div className="reservation-option" onClick={handleReserveRemaining}>
                        <div className="reservation-option-icon">
                            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                                <line x1="16" y1="2" x2="16" y2="6"></line>
                                <line x1="8" y1="2" x2="8" y2="6"></line>
                                <line x1="3" y1="10" x2="21" y2="10"></line>
                            </svg>
                        </div>
                        <h3>Rezervă locurile rămase</h3>
                        <p>Rezervare rapidă pentru locurile disponibile în acest moment</p>
                    </div>
                </div>

                <div className="reservation-modal-footer">
                    <button className="reservation-modal-close-button" onClick={onClose}>
                        Anulează
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ReservationOptionsModal;